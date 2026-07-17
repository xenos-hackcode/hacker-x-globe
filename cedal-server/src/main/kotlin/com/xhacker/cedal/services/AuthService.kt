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
    const val CURRENT_TERMS_VERSION = "2025-12-28"

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

    fun signup(req: SignupRequest): SignupResponse = transaction {
        if (req.acceptedTermsVersion != CURRENT_TERMS_VERSION) {
            throw AuthException("Terms must be accepted")
        }
        val trimmedDeviceId = req.deviceId?.trim()
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
            it[createdAt] = now
            it[lastSeen] = now
        } get Users.id

        LockoutState.insert { it[LockoutState.userId] = userId.value }

        if (req.guest) {
            val tokens = issueTokens(userId.value, "user")
            SignupResponse(userId.value.toString(), emailVerificationRequired = false, tokens = tokens)
        } else {
            val code = generateCode()
            VerificationCodes.insert {
                it[VerificationCodes.userId] = userId.value
                it[purpose] = "verify_email"
                it[VerificationCodes.code] = code
                it[expiresAt] = now + CODE_TTL_MS
            }
            // Dev-only: no email provider wired yet, so log the code and also
            // echo it back in the response for the app to display.
            println("[cedal-server] Verification code for ${req.email}: $code")
            SignupResponse(userId.value.toString(), emailVerificationRequired = true, tokens = null, devVerificationCode = code)
        }
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
            ?: throw AuthException("Invalid email or password")
        val hash = row[Users.passwordHash] ?: throw AuthException("Invalid email or password")
        if (!BCrypt.checkpw(req.password, hash)) throw AuthException("Invalid email or password")
        if (!row[Users.emailVerified]) throw AuthException("Email not verified")

        val uid = row[Users.id].value
        Users.update({ Users.id eq uid }) { it[lastSeen] = System.currentTimeMillis() }

        if (!row[Users.twoFactorEnabled]) {
            return@transaction LoginResponse(requiresTwoFactor = false, tokens = issueTokens(uid, row[Users.role]))
        }

        val code = generateCode()
        VerificationCodes.deleteWhere { (VerificationCodes.userId eq uid) and (VerificationCodes.purpose eq "login_2fa") }
        VerificationCodes.insert {
            it[VerificationCodes.userId] = uid
            it[purpose] = "login_2fa"
            it[VerificationCodes.code] = code
            it[expiresAt] = System.currentTimeMillis() + CODE_TTL_MS
        }
        println("[cedal-server] Two-factor login code for ${req.email}: $code")
        LoginResponse(requiresTwoFactor = true, userId = uid.toString(), devVerificationCode = code)
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
        issueTokens(uid, userRow[Users.role])
    }

    // --- Two-factor (security settings) ---

    // Enabling requires proving control of the linked email first (same
    // in-app-code pattern as signup verification / password reset).
    fun requestTwoFactorSetup(userId: String): String = transaction {
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
        println("[cedal-server] Two-factor setup code for ${row[Users.email]}: $code")
        code
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
        getProfile(userId)
    }

    // Disabling doesn't need a code — you're already inside an authenticated
    // session, same trust level as any other settings change.
    fun disableTwoFactor(userId: String): UserProfile = transaction {
        val uid = UUID.fromString(userId)
        Users.update({ Users.id eq uid }) { it[twoFactorEnabled] = false }
        getProfile(userId)
    }

    fun forgotPassword(req: ForgotPasswordRequest): Unit = transaction {
        val row = Users.selectAll().where { Users.email eq req.email }.firstOrNull() ?: return@transaction
        val uid = row[Users.id].value
        val code = generateCode()
        VerificationCodes.deleteWhere { (VerificationCodes.userId eq uid) and (VerificationCodes.purpose eq "reset_password") }
        VerificationCodes.insert {
            it[userId] = uid
            it[purpose] = "reset_password"
            it[VerificationCodes.code] = code
            it[expiresAt] = System.currentTimeMillis() + CODE_TTL_MS
        }
        println("[cedal-server] Password reset code for ${req.email}: $code")
    }

    fun resetPassword(req: ResetPasswordRequest): Unit = transaction {
        val userRow = Users.selectAll().where { Users.email eq req.email }.firstOrNull()
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

        val expected = when (req.mode) {
            "developer" -> userRow[Users.devKey]
            else -> userRow[Users.passcode]
        }
        val matches = expected != null && expected == req.code

        if (matches) {
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

    fun getProfile(userId: String): UserProfile = transaction {
        val uid = UUID.fromString(userId)
        val row = Users.selectAll().where { Users.id eq uid }.firstOrNull() ?: throw AuthException("User not found")
        row.toProfile()
    }

    fun updateProfile(userId: String, req: UpdateProfileRequest): UserProfile = transaction {
        val uid = UUID.fromString(userId)

        // Handle is display text like any other profile field — not unique.
        // publicId is the only identifier guaranteed unique on a node.
        val normalizedHandle = req.handle?.trim()?.removePrefix("@")?.lowercase()
        if (normalizedHandle != null && !HANDLE_REGEX.matches(normalizedHandle)) {
            throw AuthException("Handle must be 3-20 characters: letters, numbers, underscore")
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
        xp = this[Users.xp],
        exp = this[Users.exp],
    )
}
