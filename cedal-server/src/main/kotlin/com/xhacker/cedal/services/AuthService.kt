package com.xhacker.cedal.services

import com.xhacker.cedal.db.LockoutState
import com.xhacker.cedal.db.RefreshTokens
import com.xhacker.cedal.db.Users
import com.xhacker.cedal.db.VerificationCodes
import com.xhacker.cedal.models.*
import com.xhacker.cedal.plugins.JwtConfig
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.neq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.mindrot.jbcrypt.BCrypt
import java.security.SecureRandom
import java.util.UUID

class AuthException(message: String) : Exception(message)

object AuthService {
    private val random = SecureRandom()
    private const val DEV_KEY_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#\$%^&*"
    private const val REFRESH_TOKEN_TTL_MS = 30L * 24 * 60 * 60 * 1000 // 30 days
    private const val CODE_TTL_MS = 15L * 60 * 1000 // 15 minutes

    // Bump whenever the terms text changes (must match cedal-android's
    // TermsConfig.CURRENT_VERSION) — forces re-acceptance on next app open.
    const val CURRENT_TERMS_VERSION = "2026-07-24"

    // The app owner's own fixed Developer-mode passcode - see
    // verifyNodePassword's isOwnerAccount branch. Never rotates, unaffected
    // by the delegated-developer key system (Users.developerKey).
    private const val OWNER_DEVELOPER_PASSCODE = "cedalstar"

    // Excludes ambiguous chars (0/O, 1/I/L) so IDs are easy to read/type aloud.
    private const val PUBLIC_ID_CHARS = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"
    private val HANDLE_REGEX = Regex("^[a-z0-9_]{3,20}$")

    private fun generateDevKey(): String =
        (1..7).map { DEV_KEY_CHARS[random.nextInt(DEV_KEY_CHARS.length)] }.joinToString("")

    private fun generatePublicId(): String {
        repeat(10) {
            val candidate = (1..8).map { PUBLIC_ID_CHARS[random.nextInt(PUBLIC_ID_CHARS.length)] }.joinToString("")
            val exists = Users.selectAll().where { Users.publicId eq candidate }.firstOrNull() != null
            if (!exists) return candidate
        }
        throw IllegalStateException("Could not generate a unique public ID")
    }

    private fun generateCode(): String = (100000 + random.nextInt(900000)).toString()

    private fun generateOpaqueToken(): String {
        val bytes = ByteArray(32)
        random.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun hashToken(token: String): String = BCrypt.hashpw(token, BCrypt.gensalt(10))

    // --- Signup / login ---

    // Marker for the non-guest branch's pending verification code - carried
    // out of the transaction{} block so the actual SMS/email send (a
    // suspend call) can happen outside it, same split SecurityService uses.
    private data class SignupPendingVerify(val userId: String, val code: String, val verifyVia: String, val email: String, val phone: String)

    suspend fun signup(req: SignupRequest): SignupResponse {
        val (guestResponse: SignupResponse?, pending: SignupPendingVerify?) = transaction {
        if (req.acceptedTermsVersion != CURRENT_TERMS_VERSION) {
            throw AuthException("Terms must be accepted")
        }
        val trimmedDeviceId = req.deviceId?.trim()
        var trimmedPhone: String? = null
        if (!req.guest) {
            if (req.email.isNullOrBlank() || req.password.isNullOrBlank()) {
                throw AuthException("Email and password are required")
            }
            if (req.password.length < 6) {
                throw AuthException("Password must be at least 6 characters")
            }
            val existing = Users.selectAll().where { Users.email eq req.email }.firstOrNull()
            if (existing != null) {
                throw AuthException("Email already registered")
            }
            // Godmode "Ban" permanently blocks this email from ever signing
            // up again, even after the original account is deleted - see
            // BannedIdentities' own doc comment. Deliberately the SAME
            // generic message as the "already registered" check above -
            // signup/login never reveal ban status to someone not already
            // in a live session when it happened (see AccountGateBody
            // client-side for where the real explanation actually shows).
            val bannedEmail = com.xhacker.cedal.db.BannedIdentities.selectAll()
                .where { com.xhacker.cedal.db.BannedIdentities.email eq req.email }
                .any()
            if (bannedEmail) throw AuthException("Account already exists")

            // Phone number is now required for every real (non-guest)
            // account - "one account per phone number", enforced right at
            // creation instead of as an optional later Settings add-on (see
            // Users.phoneNumber's own doc comment). Written straight onto
            // the new row below, same as email - no separate staging table
            // needed here since, unlike a later Settings phone CHANGE, this
            // is the one and only number a brand-new account is claiming.
            trimmedPhone = req.phoneNumber?.trim()
            if (trimmedPhone.isNullOrBlank()) throw AuthException("Phone number is required")
            if (!SecurityService.isValidPhoneFormat(trimmedPhone)) {
                throw AuthException("Enter a valid phone number, e.g. +14155551234")
            }
            val phoneTaken = Users.selectAll().where { Users.phoneNumber eq trimmedPhone }.any()
            if (phoneTaken) throw AuthException("This phone number is already linked to another account")
            val bannedPhone = com.xhacker.cedal.db.BannedIdentities.selectAll()
                .where { com.xhacker.cedal.db.BannedIdentities.phoneNumber eq trimmedPhone }
                .any()
            if (bannedPhone) throw AuthException("This phone number has been banned and can't be used on Cedal again")

            if (req.verifyVia != "sms" && req.verifyVia != "email") {
                throw AuthException("Choose sms or email to verify")
            }
        } else {
            if (trimmedDeviceId.isNullOrBlank()) throw AuthException("Missing device id")
            // Only rows still marked isGuest count against the limit — once a
            // guest links an email it stops being "the device's guest node",
            // freeing this device up to spawn a new one.
            val existingGuest = Users.selectAll()
                .where { (Users.deviceId eq trimmedDeviceId) and (Users.isGuest eq true) }
                .firstOrNull()
            if (existingGuest != null) {
                throw AuthException("This device already has a guest node. Sign in with it, or link it to an email to free up a new one.")
            }
        }

        val now = System.currentTimeMillis()
        val userId = Users.insert {
            it[email] = req.email
            it[passwordHash] = req.password?.let { p -> BCrypt.hashpw(p, BCrypt.gensalt(10)) }
            it[isGuest] = req.guest
            it[emailVerified] = req.guest // guests skip verification
            it[acceptedTermsVersion] = req.acceptedTermsVersion
            it[acceptedTermsAt] = now
            it[role] = "user"
            it[devKey] = generateDevKey()
            it[publicId] = generatePublicId()
            it[deviceId] = if (req.guest) trimmedDeviceId else null
            // Reserved immediately (not staged like a later Settings phone
            // CHANGE) - the uniqueness check above already ran synchronously
            // in this same transaction, same pattern as email above it.
            it[phoneNumber] = trimmedPhone
            it[phoneVerified] = !req.guest
            it[createdAt] = now
            it[lastSeen] = now
        } get Users.id

        LockoutState.insert { it[LockoutState.userId] = userId.value }

        if (req.guest) {
            val tokens = issueTokens(userId.value, "user")
            AchievementService.unlock(userId.value, "welcome")
            SignupResponse(userId.value.toString(), emailVerificationRequired = false, tokens = tokens) to null
        } else {
            val code = generateCode()
            VerificationCodes.insert {
                it[VerificationCodes.userId] = userId.value
                it[purpose] = "verify_email"
                it[VerificationCodes.code] = code
                it[expiresAt] = now + CODE_TTL_MS
            }
            null to SignupPendingVerify(userId.value.toString(), code, req.verifyVia!!, req.email!!, trimmedPhone!!)
        }
        }

        if (guestResponse != null) return guestResponse

        val p = pending!!
        // Real delivery via SmsService/EmailService when configured; falls
        // back to logging + echoing the code in the response for the app to
        // display when it isn't (see EmailService/SmsService's own doc
        // comments) - same dev-mode pattern either channel.
        val sent = if (p.verifyVia == "sms") {
            SmsService.sendSms(p.phone, "Your Cedal verification code is ${p.code}. It expires in 10 minutes.")
        } else {
            EmailService.send(p.email, "Your Cedal verification code", "Your Cedal verification code is ${p.code}. It expires in 10 minutes.")
        }
        val destination = if (p.verifyVia == "sms") p.phone else p.email
        println("[cedal-server] Verification code for $destination via ${p.verifyVia}: ${p.code} (sent: $sent)")
        return SignupResponse(p.userId, emailVerificationRequired = true, tokens = null, devVerificationCode = if (sent) null else p.code)
    }

    fun verifyEmail(req: VerifyEmailRequest): Unit = transaction {
        val uid = UUID.fromString(req.userId)
        val row = VerificationCodes
            .selectAll()
            .where { (VerificationCodes.userId eq uid) and (VerificationCodes.purpose eq "verify_email") }
            .firstOrNull() ?: throw AuthException("No pending verification")

        if (row[VerificationCodes.code] != req.code) throw AuthException("Invalid code")
        if (row[VerificationCodes.expiresAt] < System.currentTimeMillis()) throw AuthException("Code expired")

        Users.update({ Users.id eq uid }) { it[emailVerified] = true }
        VerificationCodes.deleteWhere { (VerificationCodes.userId eq uid) and (VerificationCodes.purpose eq "verify_email") }
    }

    fun login(req: LoginRequest): LoginResponse = transaction {
        val row = Users.selectAll().where { Users.email eq req.email }.firstOrNull()
        if (row == null) {
            // A fresh login attempt never reveals WHY - "Invalid account"
            // covers both a never-registered email and one whose data was
            // cleared by Godmode (see AdminClearedIdentities). The real
            // explanation only ever shows to a user who was actually in a
            // LIVE session when it happened - see AccountGateBody /
            // MemberScaffold's live ban-check poll client-side.
            throw AuthException("Invalid account")
        }
        val hash = row[Users.passwordHash] ?: throw AuthException("Invalid email or password")
        if (!BCrypt.checkpw(req.password, hash)) throw AuthException("Invalid email or password")
        if (!row[Users.emailVerified]) throw AuthException("Email not verified")
        // Same non-revealing message as the row==null branch above, whether
        // this is a temporary or a permanent ban - see this fun's own doc
        // comment for why fresh login attempts never say "banned".
        if (row[Users.banned]) throw AuthException("Invalid account")

        val uid = row[Users.id].value
        Users.update({ Users.id eq uid }) { it[lastSeen] = System.currentTimeMillis() }

        if (!row[Users.twoFactorEnabled]) {
            val tokens = issueTokens(uid, row[Users.role])
            AchievementService.unlock(uid, "welcome")
            return@transaction LoginResponse(requiresTwoFactor = false, tokens = tokens)
        }

        val code = generateCode()
        VerificationCodes.deleteWhere { (VerificationCodes.userId eq uid) and (VerificationCodes.purpose eq "login_2fa") }
        VerificationCodes.insert {
            it[VerificationCodes.userId] = uid
            it[purpose] = "login_2fa"
            it[VerificationCodes.code] = code
            it[expiresAt] = System.currentTimeMillis() + CODE_TTL_MS
        }
        val sent = EmailService.send(req.email, "Your Cedal two-factor code", "Your Cedal two-factor login code is $code. It expires in 10 minutes.")
        println("[cedal-server] Two-factor login code for ${req.email}: $code (email sent: $sent)")
        LoginResponse(requiresTwoFactor = true, userId = uid.toString(), devVerificationCode = if (sent) null else code)
    }

    fun confirmLoginTwoFactor(req: TwoFactorLoginConfirmRequest): AuthTokens = transaction {
        val uid = UUID.fromString(req.userId)
        val codeRow = VerificationCodes
            .selectAll()
            .where { (VerificationCodes.userId eq uid) and (VerificationCodes.purpose eq "login_2fa") }
            .firstOrNull() ?: throw AuthException("No pending two-factor login")
        if (codeRow[VerificationCodes.code] != req.code) throw AuthException("Invalid code")
        if (codeRow[VerificationCodes.expiresAt] < System.currentTimeMillis()) throw AuthException("Code expired")
        VerificationCodes.deleteWhere { (VerificationCodes.userId eq uid) and (VerificationCodes.purpose eq "login_2fa") }

        val userRow = Users.selectAll().where { Users.id eq uid }.first()
        val tokens = issueTokens(uid, userRow[Users.role])
        AchievementService.unlock(uid, "welcome")
        tokens
    }

    // --- Two-factor (security settings) ---

    // Enabling requires proving control of the linked email first (same
    // in-app-code pattern as signup verification / password reset).
    fun requestTwoFactorSetup(userId: String): String? = transaction {
        val uid = UUID.fromString(userId)
        val row = Users.selectAll().where { Users.id eq uid }.firstOrNull() ?: throw AuthException("User not found")
        if (row[Users.isGuest] || row[Users.email].isNullOrBlank()) {
            throw AuthException("Link an email to this node before enabling two-factor verification")
        }
        val code = generateCode()
        VerificationCodes.deleteWhere { (VerificationCodes.userId eq uid) and (VerificationCodes.purpose eq "two_factor_setup") }
        VerificationCodes.insert {
            it[VerificationCodes.userId] = uid
            it[purpose] = "two_factor_setup"
            it[VerificationCodes.code] = code
            it[expiresAt] = System.currentTimeMillis() + CODE_TTL_MS
        }
        val email = row[Users.email]!!
        val sent = EmailService.send(email, "Your Cedal two-factor setup code", "Your Cedal two-factor setup code is $code. It expires in 10 minutes.")
        println("[cedal-server] Two-factor setup code for $email: $code (email sent: $sent)")
        if (sent) null else code
    }

    fun confirmTwoFactorSetup(userId: String, code: String): UserProfile = transaction {
        val uid = UUID.fromString(userId)
        val codeRow = VerificationCodes
            .selectAll()
            .where { (VerificationCodes.userId eq uid) and (VerificationCodes.purpose eq "two_factor_setup") }
            .firstOrNull() ?: throw AuthException("No pending two-factor setup")
        if (codeRow[VerificationCodes.code] != code) throw AuthException("Invalid code")
        if (codeRow[VerificationCodes.expiresAt] < System.currentTimeMillis()) throw AuthException("Code expired")

        Users.update({ Users.id eq uid }) { it[twoFactorEnabled] = true }
        VerificationCodes.deleteWhere { (VerificationCodes.userId eq uid) and (VerificationCodes.purpose eq "two_factor_setup") }
        AchievementService.checkSecure(userId)
        getProfile(userId)
    }

    // Disabling doesn't need a code — you're already inside an authenticated
    // session, same trust level as any other settings change.
    fun disableTwoFactor(userId: String): UserProfile = transaction {
        val uid = UUID.fromString(userId)
        Users.update({ Users.id eq uid }) { it[twoFactorEnabled] = false }
        getProfile(userId)
    }

    // Shared by forgotPassword/resetPassword - the user identifies their
    // account by EITHER email or phone number, whichever they remember/
    // prefer (see ForgotPasswordScreen's two-field form). Must be called
    // from inside a transaction{} block.
    private fun findByEmailOrPhone(email: String?, phoneNumber: String?): org.jetbrains.exposed.sql.ResultRow? {
        val trimmedEmail = email?.trim()?.takeIf { it.isNotBlank() }
        val trimmedPhone = phoneNumber?.trim()?.takeIf { it.isNotBlank() }
        if (trimmedEmail != null) {
            Users.selectAll().where { Users.email eq trimmedEmail }.firstOrNull()?.let { return it }
        }
        if (trimmedPhone != null) {
            Users.selectAll().where { Users.phoneNumber eq trimmedPhone }.firstOrNull()?.let { return it }
        }
        return null
    }

    // Two-factor-style identity check before ANY reset code goes out: the
    // account's own node passcode, on top of choosing sms/email - not just
    // "prove you own this inbox/phone" like a plain forgot-password code
    // alone would. Silently no-ops (same non-revealing principle as
    // login()'s "Invalid account") on a wrong email/phone, wrong passcode,
    // or picking "sms" for an account with no phone on file - never tells a
    // caller WHICH of those was wrong.
    suspend fun forgotPassword(req: ForgotPasswordRequest) {
        val pending: Pair<String, String>? = transaction {
            val row = findByEmailOrPhone(req.email, req.phoneNumber) ?: return@transaction null
            if (row[Users.passcode] != req.passcode) return@transaction null
            val destination = if (req.verifyVia == "sms") row[Users.phoneNumber] else row[Users.email]
            if (destination.isNullOrBlank()) return@transaction null

            val uid = row[Users.id].value
            val code = generateCode()
            VerificationCodes.deleteWhere { (VerificationCodes.userId eq uid) and (VerificationCodes.purpose eq "reset_password") }
            VerificationCodes.insert {
                it[userId] = uid
                it[purpose] = "reset_password"
                it[VerificationCodes.code] = code
                it[expiresAt] = System.currentTimeMillis() + CODE_TTL_MS
            }
            code to destination
        }
        val (code, destination) = pending ?: return

        val sent = if (req.verifyVia == "sms") {
            SmsService.sendSms(destination, "Your Cedal password reset code is $code. It expires in 10 minutes.")
        } else {
            EmailService.send(destination, "Your Cedal password reset code", "Your Cedal password reset code is $code. It expires in 10 minutes.")
        }
        println("[cedal-server] Password reset code for $destination via ${req.verifyVia}: $code (sent: $sent)")
    }

    fun resetPassword(req: ResetPasswordRequest): Unit = transaction {
        val userRow = findByEmailOrPhone(req.email, req.phoneNumber)
            ?: throw AuthException("Invalid email or code")
        val uid = userRow[Users.id].value
        val codeRow = VerificationCodes
            .selectAll()
            .where { (VerificationCodes.userId eq uid) and (VerificationCodes.purpose eq "reset_password") }
            .firstOrNull() ?: throw AuthException("Invalid email or code")

        if (codeRow[VerificationCodes.code] != req.code) throw AuthException("Invalid email or code")
        if (codeRow[VerificationCodes.expiresAt] < System.currentTimeMillis()) throw AuthException("Code expired")

        Users.update({ Users.id eq uid }) { it[passwordHash] = BCrypt.hashpw(req.newPassword, BCrypt.gensalt(10)) }
        VerificationCodes.deleteWhere { (VerificationCodes.userId eq uid) and (VerificationCodes.purpose eq "reset_password") }
    }

    // --- Node password (passcode / dev key) with server-side lockout ---

    fun createPasscode(req: CreatePasscodeRequest): Unit = transaction {
        val uid = UUID.fromString(req.userId)
        Users.update({ Users.id eq uid }) {
            it[passcode] = req.code
            it[age] = req.age
            it[favoriteColor] = req.favoriteColor
        }
    }

    // Settings-screen passcode change — unlike createPasscode (first-time
    // setup, also collects recovery info), this only touches the code itself.
    fun updatePasscode(userId: String, code: String): Unit = transaction {
        if (code.length !in 4..6) throw AuthException("Passcode must be 4-6 digits")
        val uid = UUID.fromString(userId)
        Users.update({ Users.id eq uid }) { it[passcode] = code }
        AchievementService.checkSecure(userId)
    }

    // "Link guest node" (Settings > Navigation in cedal-mobile) — upgrades a
    // guest account to a real email+password one it can sign in with later.
    // Skips the email-verification step for simplicity at this dev stage
    // (unlike normal signup); revisit before this is public-facing.
    fun linkGuestToEmail(userId: String, email: String, password: String): UserProfile = transaction {
        val uid = UUID.fromString(userId)
        val row = Users.selectAll().where { Users.id eq uid }.firstOrNull() ?: throw AuthException("User not found")
        if (!row[Users.isGuest]) throw AuthException("Only guest nodes can be linked")
        if (password.length < 6) throw AuthException("Password must be at least 6 characters")
        val taken = Users.selectAll().where { (Users.email eq email) and (Users.id neq uid) }.firstOrNull() != null
        if (taken) throw AuthException("Email already registered")

        Users.update({ Users.id eq uid }) {
            it[Users.email] = email
            it[passwordHash] = BCrypt.hashpw(password, BCrypt.gensalt(10))
            it[isGuest] = false
            it[emailVerified] = true
        }
        getProfile(userId)
    }

    fun verifyNodePassword(req: NodePasswordVerifyRequest): NodePasswordVerifyResponse = transaction {
        val uid = UUID.fromString(req.userId)
        val userRow = Users.selectAll().where { Users.id eq uid }.firstOrNull()
            ?: throw AuthException("User not found")
        val lockRow = LockoutState.selectAll().where { LockoutState.userId eq uid }.firstOrNull()
        val now = System.currentTimeMillis()
        val lockUntil = lockRow?.get(LockoutState.lockUntil)
        val failCount = lockRow?.get(LockoutState.failCount) ?: 0

        if (lockUntil != null && lockUntil > now) {
            return@transaction NodePasswordVerifyResponse(
                success = false, locked = true, lockUntil = lockUntil, failCount = failCount,
                message = "Locked out",
            )
        }

        // The app owner has a fixed passcode for their own developer entry
        // (never rotates, unaffected by the delegation system below) -
        // anyone else needs Users.developerAccess PLUS a currently-live,
        // admin-issued developerKey (see DeveloperAccessService).
        val isOwnerAccount = userRow[Users.email]?.equals(AdminService.ADMIN_EMAIL, ignoreCase = true) == true
        val expected = when {
            req.mode == "developer" && isOwnerAccount -> OWNER_DEVELOPER_PASSCODE
            req.mode == "developer" -> userRow[Users.developerKey]
            else -> userRow[Users.passcode]
        }
        val matches = expected != null && expected == req.code

        if (matches) {
            // One-time - spent the instant it's used, so a delegated
            // account always has to ask the owner for a fresh key before
            // their NEXT developer-mode entry too.
            if (req.mode == "developer" && !isOwnerAccount) {
                Users.update({ Users.id eq uid }) { it[developerKey] = null }
            }
            LockoutState.update({ LockoutState.userId eq uid }) {
                it[LockoutState.failCount] = 0
                it[LockoutState.lockUntil] = null
            }
            val currentRole = userRow[Users.role]
            val newRole = when {
                currentRole == "owner" -> "owner"
                req.mode == "developer" -> "developer"
                else -> "user"
            }
            if (newRole != currentRole) {
                Users.update({ Users.id eq uid }) { it[role] = newRole }
            }
            NodePasswordVerifyResponse(success = true, role = newRole, failCount = 0)
        } else {
            val nextFail = failCount + 1
            // Mirrors the RN enter-password.tsx schedule: 1st = warn only,
            // 2nd = 15min, 3rd = 30min, 4th+ = 2hr.
            val newLockUntil = when {
                nextFail <= 1 -> null
                nextFail == 2 -> now + 15 * 60 * 1000
                nextFail == 3 -> now + 30 * 60 * 1000
                else -> now + 2 * 60 * 60 * 1000
            }
            LockoutState.update({ LockoutState.userId eq uid }) {
                it[LockoutState.failCount] = nextFail
                it[LockoutState.lockUntil] = newLockUntil
            }
            NodePasswordVerifyResponse(
                success = false, locked = newLockUntil != null, lockUntil = newLockUntil,
                failCount = nextFail, message = "Incorrect code",
            )
        }
    }

    // --- Tokens ---

    fun issueTokens(userId: UUID, role: String): AuthTokens = transaction {
        val refreshToken = generateOpaqueToken()
        RefreshTokens.insert {
            it[RefreshTokens.userId] = userId
            it[tokenHash] = hashToken(refreshToken)
            it[expiresAt] = System.currentTimeMillis() + REFRESH_TOKEN_TTL_MS
        }
        AuthTokens(
            accessToken = JwtConfig.generateAccessToken(userId.toString(), role),
            refreshToken = refreshToken,
            userId = userId.toString(),
            role = role,
        )
    }

    fun refresh(rawToken: String): AuthTokens = transaction {
        val candidates = RefreshTokens.selectAll().where { RefreshTokens.revoked eq false }
        val match = candidates.firstOrNull { BCrypt.checkpw(rawToken, it[RefreshTokens.tokenHash]) }
            ?: throw AuthException("Invalid refresh token")
        if (match[RefreshTokens.expiresAt] < System.currentTimeMillis()) throw AuthException("Refresh token expired")

        val uid = match[RefreshTokens.userId].value
        val userRow = Users.selectAll().where { Users.id eq uid }.first()
        RefreshTokens.update({ RefreshTokens.id eq match[RefreshTokens.id] }) { it[revoked] = true }
        // A ban takes effect immediately, not just on the next fresh
        // login - an already-signed-in session can't silently keep
        // refreshing past it (see Users.banned's doc comment).
        if (userRow[Users.banned]) throw AuthException("This account has been banned")
        issueTokens(uid, userRow[Users.role])
    }

    fun logout(rawToken: String): Unit = transaction {
        val candidates = RefreshTokens.selectAll().where { RefreshTokens.revoked eq false }
        val match = candidates.firstOrNull { BCrypt.checkpw(rawToken, it[RefreshTokens.tokenHash]) } ?: return@transaction
        RefreshTokens.update({ RefreshTokens.id eq match[RefreshTokens.id] }) { it[revoked] = true }
    }

    // --- Terms ---

    fun updateTermsAcceptance(userId: String, version: String): UserProfile = transaction {
        if (version != CURRENT_TERMS_VERSION) throw AuthException("Unknown terms version")
        val uid = UUID.fromString(userId)
        Users.update({ Users.id eq uid }) {
            it[acceptedTermsVersion] = version
            it[acceptedTermsAt] = System.currentTimeMillis()
        }
        getProfile(userId)
    }

    // --- Profile ---

    // viewerId identifies who's ASKING - null/equal-to-userId means "viewing
    // your own profile", always fully visible. Any other viewerId applies
    // Settings > Security > Popularity redaction (see PopularityService).
    fun getProfile(userId: String, viewerId: String? = null): UserProfile = transaction {
        val uid = UUID.fromString(userId)
        val row = Users.selectAll().where { Users.id eq uid }.firstOrNull() ?: throw AuthException("User not found")
        val profile = row.toProfile()
        if (viewerId == null || viewerId == userId) return@transaction profile

        val effective = PopularityService.effectiveFor(uid, UUID.fromString(viewerId))
        val canCall = CallService.canCall(uid, UUID.fromString(viewerId))
        profile.copy(
            nickname = if (effective.showName) profile.nickname else null,
            handle = if (effective.showName) profile.handle else null,
            avatarUrl = if (effective.showPfp) profile.avatarUrl else null,
            age = if (effective.showAge) profile.age else null,
            exp = if (effective.showRank) profile.exp else 0L,
            occupation = if (effective.showOccupation) profile.occupation else null,
            hobby = if (effective.showHobby) profile.hobby else null,
            bio = if (effective.showBio) profile.bio else null,
            gender = if (effective.showGender) profile.gender else null,
            nameVisible = effective.showName,
            pfpVisible = effective.showPfp,
            ageVisible = effective.showAge,
            rankVisible = effective.showRank,
            occupationVisible = effective.showOccupation,
            hobbyVisible = effective.showHobby,
            bioVisible = effective.showBio,
            genderVisible = effective.showGender,
            canCall = canCall,
            phoneNumber = if (canCall) profile.phoneNumber else null,
        )
    }

    fun updateProfile(userId: String, req: UpdateProfileRequest): UserProfile = transaction {
        val uid = UUID.fromString(userId)

        // Handle is display text like any other profile field — not unique.
        // publicId is the only identifier guaranteed unique on a node.
        val normalizedHandle = req.handle?.trim()?.removePrefix("@")?.lowercase()
        if (normalizedHandle != null && !HANDLE_REGEX.matches(normalizedHandle)) {
            throw AuthException("Handle can only use English letters, numbers, and underscore (3-20 of them) — this is a fixed @handle, not your display name. Want Chinese or another language? Use Nickname instead, which supports any text.")
        }

        Users.update({ Users.id eq uid }) {
            req.nickname?.let { v -> it[nickname] = v }
            normalizedHandle?.let { v -> it[handle] = v }
            req.age?.let { v -> it[age] = v }
            req.occupation?.let { v -> it[occupation] = v }
            req.hobby?.let { v -> it[hobby] = v }
            req.bio?.let { v -> it[bio] = v }
            req.gender?.let { v -> it[gender] = v }
            req.avatarUrl?.let { v -> it[avatarUrl] = v }
            req.hideFromSearch?.let { v -> it[hideFromSearch] = v }
            req.preferredLanguage?.let { v -> it[preferredLanguage] = v }
            // Group chat DM/tag privacy toggles (Round 2/4) - see their own
            // doc comments in Tables.kt.
            req.dmClosed?.let { v -> it[dmClosed] = v }
            req.noTag?.let { v -> it[noTag] = v }
            req.hiderEnabled?.let { v -> it[hiderEnabled] = v }
            req.shareNumberDefault?.let { v -> it[shareNumberDefault] = v }
            req.denyAllCalls?.let { v -> it[denyAllCalls] = v }
            req.denyNonFriendCalls?.let { v -> it[denyNonFriendCalls] = v }
            req.denyUnknownCallers?.let { v -> it[denyUnknownCallers] = v }
            req.autoMuteNewGroups?.let { v -> it[autoMuteNewGroups] = v }
            req.mentionsOnlyDefault?.let { v -> it[mentionsOnlyDefault] = v }
            req.autoPinOwnedGroups?.let { v -> it[autoPinOwnedGroups] = v }
            req.requireGroupAddApproval?.let { v -> it[requireGroupAddApproval] = v }
        }
        getProfile(userId)
    }

    private fun org.jetbrains.exposed.sql.ResultRow.toProfile() = UserProfile(
        id = this[Users.id].value.toString(),
        email = this[Users.email],
        isGuest = this[Users.isGuest],
        emailVerified = this[Users.emailVerified],
        role = this[Users.role],
        nickname = this[Users.nickname],
        publicId = this[Users.publicId],
        handle = this[Users.handle],
        occupation = this[Users.occupation],
        hobby = this[Users.hobby],
        bio = this[Users.bio],
        gender = this[Users.gender],
        avatarUrl = this[Users.avatarUrl],
        age = this[Users.age],
        createdAt = this[Users.createdAt],
        acceptedTermsVersion = this[Users.acceptedTermsVersion],
        twoFactorEnabled = this[Users.twoFactorEnabled],
        hideFromSearch = this[Users.hideFromSearch],
        preferredLanguage = this[Users.preferredLanguage],
        dmClosed = this[Users.dmClosed],
        noTag = this[Users.noTag],
        hiderEnabled = this[Users.hiderEnabled],
        shareNumberDefault = this[Users.shareNumberDefault],
        denyAllCalls = this[Users.denyAllCalls],
        denyNonFriendCalls = this[Users.denyNonFriendCalls],
        denyUnknownCallers = this[Users.denyUnknownCallers],
        autoMuteNewGroups = this[Users.autoMuteNewGroups],
        mentionsOnlyDefault = this[Users.mentionsOnlyDefault],
        autoPinOwnedGroups = this[Users.autoPinOwnedGroups],
        requireGroupAddApproval = this[Users.requireGroupAddApproval],
        phoneNumber = this[Users.phoneNumber],
        xp = this[Users.xp],
        exp = this[Users.exp],
        activeBadgeKey = this[Users.activeBadgeKey],
        developerAccess = this[Users.developerAccess],
        hasActiveDeveloperKey = this[Users.developerKey] != null,
    )
}
