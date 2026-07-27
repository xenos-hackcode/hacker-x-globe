package com.xhacker.cedal.services

import com.xhacker.cedal.db.PendingPlatformSignups
import com.xhacker.cedal.db.PlatformDevelopers
import com.xhacker.cedal.db.PlatformOAuthSessions
import com.xhacker.cedal.db.PlatformSmsJobs
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.isNull
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.mindrot.jbcrypt.BCrypt
import java.security.SecureRandom
import java.util.UUID

// GitHub-OAuth self-service signup for the SMS relay platform - lets any
// developer run their own "cedal-sms-relay" instance on their own phone
// instead of every deployment funneling through the owner's one device. See
// the plan doc / memory `cedal_sms_relay_multidev_platform.md`. A "platform
// developer" is NOT a Cedal end-user (no Users row) - a separate, minimal
// tenant identity just for relay access.
//
// Flow: GitHub OAuth (routes/PlatformRoutes.kt handles the `state` CSRF
// cookie) -> createOAuthSession (proves this browser really is that
// githubId, without ever trusting a client-supplied identity) ->
// submitVerification (validates input, rate-limits resends, sends an email
// code + an SMS code - the latter via the OWNER's existing relay phone) ->
// confirmCodes -> completeSignup, which mints the real activation key,
// splits it in half (half emailed, half texted - again via the owner's
// phone, one more bounded text), and persists only BCrypt hashes of
// email/phone/key. The raw key is never stored and never returned from any
// API - the only way to get it is to actually receive and combine both
// halves.
object PlatformDeveloperService {
    private val random = SecureRandom()
    private const val CODE_TTL_MS = 15L * 60 * 1000
    private const val OAUTH_SESSION_TTL_MS = 15L * 60 * 1000
    private const val RESEND_COOLDOWN_MS = 60L * 1000
    private const val KEY_CHARS = "ABCDEFGHJKMNPQRSTUVWXYZ23456789abcdefghjkmnpqrstuvwxyz23456789"

    private val PACKAGE_NAME_REGEX = Regex("^[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z][a-zA-Z0-9_]*)+$")
    private val EMAIL_REGEX = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
    private val PHONE_REGEX = Regex("^\\+?[0-9]{7,15}$")

    private val CALLBACK_BASE_URL = System.getenv("PUBLIC_BASE_URL")
        ?: "https://cedal-server-717899371194.us-central1.run.app"
    private val CLIENT_ID = System.getenv("GITHUB_OAUTH_CLIENT_ID") ?: ""
    private val CLIENT_SECRET = System.getenv("GITHUB_OAUTH_CLIENT_SECRET") ?: ""
    val REDIRECT_URI = "$CALLBACK_BASE_URL/platform/github/callback"

    private val jsonParser = Json { ignoreUnknownKeys = true }
    private val client = HttpClient(CIO) {
        install(HttpTimeout) { requestTimeoutMillis = 20_000 }
    }

    fun githubAuthorizeUrl(state: String): String =
        "https://github.com/login/oauth/authorize?client_id=$CLIENT_ID&redirect_uri=$REDIRECT_URI&scope=read:user&state=$state"

    @Serializable
    private data class GitHubTokenResponse(val access_token: String? = null, val error: String? = null)

    @Serializable
    data class GitHubUserResponse(val id: Long, val login: String)

    // Hand-rolled authorization-code exchange, matching this codebase's
    // existing style (GitHubService/SmsService - plain HttpClient(CIO) +
    // manual JSON, no OAuth framework) rather than pulling in Ktor's
    // separate client-OAuth machinery for one call site.
    suspend fun handleGitHubCallback(code: String): GitHubUserResponse? {
        val tokenBody = client.post("https://github.com/login/oauth/access_token") {
            contentType(ContentType.Application.Json)
            header("Accept", "application/json")
            setBody("""{"client_id":"$CLIENT_ID","client_secret":"$CLIENT_SECRET","code":"$code","redirect_uri":"$REDIRECT_URI"}""")
        }.body<String>()
        val token = jsonParser.decodeFromString<GitHubTokenResponse>(tokenBody).access_token ?: return null

        val userBody = client.get("https://api.github.com/user") {
            header("Authorization", "Bearer $token")
            header("Accept", "application/vnd.github+json")
        }.body<String>()
        return jsonParser.decodeFromString<GitHubUserResponse>(userBody)
    }

    private fun generateToken(bytes: Int = 32): String {
        val b = ByteArray(bytes)
        random.nextBytes(b)
        return b.joinToString("") { "%02x".format(it) }
    }

    // Proves a browser really did complete GitHub OAuth as this githubId -
    // the ONLY thing PlatformRoutes ever hands back to the client after the
    // callback. Every subsequent step takes this token, never a raw
    // githubId, closing off "just POST whatever identity you want."
    fun createOAuthSession(githubId: String, githubLogin: String): String = transaction {
        val token = generateToken()
        PlatformOAuthSessions.insert {
            it[PlatformOAuthSessions.token] = token
            it[PlatformOAuthSessions.githubId] = githubId
            it[PlatformOAuthSessions.githubLogin] = githubLogin
            it[expiresAt] = System.currentTimeMillis() + OAUTH_SESSION_TTL_MS
        }
        token
    }

    private data class OAuthIdentity(val githubId: String, val githubLogin: String)

    private fun resolveOAuthSession(token: String): OAuthIdentity? = transaction {
        val row = PlatformOAuthSessions.selectAll().where { PlatformOAuthSessions.token eq token }.firstOrNull()
            ?: return@transaction null
        if (row[PlatformOAuthSessions.expiresAt] < System.currentTimeMillis()) return@transaction null
        OAuthIdentity(row[PlatformOAuthSessions.githubId], row[PlatformOAuthSessions.githubLogin])
    }

    private fun generateCode(): String = (100000 + random.nextInt(900000)).toString()

    private fun generateKey(): String = (1..24).map { KEY_CHARS[random.nextInt(KEY_CHARS.length)] }.joinToString("")

    sealed class SubmitResult {
        object Sent : SubmitResult()
        object InvalidSession : SubmitResult()
        object TermsNotAccepted : SubmitResult()
        data class InvalidInput(val reason: String) : SubmitResult()
        object AlreadyRegistered : SubmitResult()
        object TooSoon : SubmitResult()
        object EmailFailed : SubmitResult()
    }

    // Step 2 - validates input, rate-limits, sends both verification codes.
    // Re-callable ("resend"), subject to RESEND_COOLDOWN_MS so a script
    // hammering this can't spam the owner's own relay phone with texts.
    fun submitVerification(signupToken: String, packageNameRaw: String, emailRaw: String, phoneRaw: String, acceptedTerms: Boolean): SubmitResult {
        val identity = resolveOAuthSession(signupToken) ?: return SubmitResult.InvalidSession
        if (!acceptedTerms) return SubmitResult.TermsNotAccepted

        val packageName = packageNameRaw.trim()
        val email = emailRaw.trim()
        val phone = phoneRaw.trim()
        if (!PACKAGE_NAME_REGEX.matches(packageName)) return SubmitResult.InvalidInput("Package name doesn't look like a valid Android application ID (e.g. com.yourname.yourapp).")
        if (!EMAIL_REGEX.matches(email)) return SubmitResult.InvalidInput("That doesn't look like a valid email address.")
        if (!PHONE_REGEX.matches(phone)) return SubmitResult.InvalidInput("That doesn't look like a valid phone number - include your country code, e.g. +15551234567.")

        return transaction {
            val alreadyRegistered = PlatformDevelopers.selectAll()
                .where { (PlatformDevelopers.githubId eq identity.githubId) or (PlatformDevelopers.packageName eq packageName) }
                .any()
            if (alreadyRegistered) return@transaction SubmitResult.AlreadyRegistered

            val existing = PendingPlatformSignups.selectAll().where { PendingPlatformSignups.githubId eq identity.githubId }.firstOrNull()
            val now = System.currentTimeMillis()
            if (existing != null && now - existing[PendingPlatformSignups.lastSentAt] < RESEND_COOLDOWN_MS) {
                return@transaction SubmitResult.TooSoon
            }

            val emailCode = generateCode()
            val phoneCode = generateCode()
            val emailSent = EmailService.send(email, "Your Cedal developer signup code", "Your verification code is: $emailCode\n\nThis expires in 15 minutes.")
            if (!emailSent) return@transaction SubmitResult.EmailFailed
            // Bounded owner-phone usage #1 of 2 for this signup - see this
            // object's own doc comment.
            SmsRelayService.enqueue(phone, "Your Cedal developer signup code is: $phoneCode")

            PendingPlatformSignups.deleteWhere { PendingPlatformSignups.githubId eq identity.githubId }
            PendingPlatformSignups.insert {
                it[githubId] = identity.githubId
                it[githubLogin] = identity.githubLogin
                it[PendingPlatformSignups.packageName] = packageName
                it[PendingPlatformSignups.email] = email
                it[PendingPlatformSignups.phone] = phone
                it[PendingPlatformSignups.emailCode] = emailCode
                it[PendingPlatformSignups.phoneCode] = phoneCode
                it[acceptedTermsAt] = now
                it[lastSentAt] = now
                it[expiresAt] = now + CODE_TTL_MS
            }
            SubmitResult.Sent
        }
    }

    sealed class ConfirmResult {
        object Completed : ConfirmResult()
        object InvalidSession : ConfirmResult()
        object InvalidCode : ConfirmResult()
        object Expired : ConfirmResult()
    }

    // Step 3 - both codes must match in one call (simpler and just as safe
    // as confirming independently, since both are single-use and short-
    // lived either way). On success, mints the real key.
    fun confirmCodes(signupToken: String, emailCode: String, phoneCode: String): ConfirmResult {
        val identity = resolveOAuthSession(signupToken) ?: return ConfirmResult.InvalidSession
        return transaction {
            val row = PendingPlatformSignups.selectAll().where { PendingPlatformSignups.githubId eq identity.githubId }.firstOrNull()
                ?: return@transaction ConfirmResult.InvalidSession
            if (row[PendingPlatformSignups.expiresAt] < System.currentTimeMillis()) {
                PendingPlatformSignups.deleteWhere { PendingPlatformSignups.githubId eq identity.githubId }
                return@transaction ConfirmResult.Expired
            }
            val emailOk = row[PendingPlatformSignups.emailCode] == emailCode.trim()
            val phoneOk = row[PendingPlatformSignups.phoneCode] == phoneCode.trim()
            if (!emailOk || !phoneOk) return@transaction ConfirmResult.InvalidCode

            completeSignup(
                identity.githubId, identity.githubLogin,
                row[PendingPlatformSignups.packageName], row[PendingPlatformSignups.email], row[PendingPlatformSignups.phone],
                row[PendingPlatformSignups.acceptedTermsAt],
            )
            PendingPlatformSignups.deleteWhere { PendingPlatformSignups.githubId eq identity.githubId }
            PlatformOAuthSessions.deleteWhere { PlatformOAuthSessions.token eq signupToken }
            ConfirmResult.Completed
        }
    }

    // Step 4 - mints the real key, splits it, sends both halves, persists
    // only hashes, discards every raw value including the pending row.
    // MUST be called from within the same transaction as its caller (not
    // its own `transaction {}`) so a failure here rolls back cleanly rather
    // than leaving a half-completed signup.
    private fun completeSignup(githubId: String, githubLogin: String, packageName: String, email: String, phone: String, acceptedTermsAt: Long) {
        val key = generateKey()
        val half = key.length / 2
        val firstHalf = key.substring(0, half)
        val secondHalf = key.substring(half)

        PlatformDevelopers.insert {
            it[PlatformDevelopers.githubId] = githubId
            it[PlatformDevelopers.githubLogin] = githubLogin
            it[PlatformDevelopers.packageName] = packageName
            it[emailHash] = BCrypt.hashpw(email, BCrypt.gensalt(10))
            it[phoneHash] = BCrypt.hashpw(phone, BCrypt.gensalt(10))
            it[keyHash] = BCrypt.hashpw(key, BCrypt.gensalt(10))
            it[PlatformDevelopers.acceptedTermsAt] = acceptedTermsAt
            it[createdAt] = System.currentTimeMillis()
        }

        EmailService.send(
            email, "Your Cedal SMS relay key - part 1 of 2",
            "Here is the first half of your activation key:\n\n$firstHalf\n\n" +
                "The second half is coming by text to the phone number you verified. Combine both halves " +
                "(first half + second half, no space) to get your real key - enter that into the cedal-sms-relay " +
                "app on your own phone. Keep both halves and the combined key private.",
        )
        // Bounded owner-phone usage #2 of 2 for this signup - after this,
        // this developer's own relay phone handles their traffic entirely;
        // the owner's phone is never touched again on their behalf.
        SmsRelayService.enqueue(phone, "Cedal relay key - part 2 of 2: $secondHalf")
    }

    // Linear BCrypt scan, same pattern AuthService uses for refresh tokens -
    // fine at this scale (a handful of platform developers, not millions).
    // Suspended developers fail this exactly like an unknown key - one
    // check here blocks them from every platform endpoint (SMS relay +
    // email) uniformly, since every one of those endpoints calls this
    // first. See PlatformDevelopers.suspended's own doc comment.
    fun verifyDeveloperToken(rawKey: String): String? = transaction {
        PlatformDevelopers.selectAll()
            .firstOrNull { BCrypt.checkpw(rawKey, it[PlatformDevelopers.keyHash]) }
            ?.takeIf { !it[PlatformDevelopers.suspended] }
            ?.get(PlatformDevelopers.id)?.value?.toString()
    }

    fun enqueueSms(developerId: String, phoneNumber: String, message: String): Unit = transaction {
        PlatformSmsJobs.insert {
            it[PlatformSmsJobs.developerId] = UUID.fromString(developerId)
            it[PlatformSmsJobs.phoneNumber] = phoneNumber
            it[PlatformSmsJobs.message] = message
            it[createdAt] = System.currentTimeMillis()
        }
    }

    @Serializable
    data class PlatformSmsJobDto(val id: String, val phoneNumber: String, val message: String)

    fun listPendingFor(developerId: String): List<PlatformSmsJobDto> = transaction {
        PlatformSmsJobs.selectAll()
            .where { (PlatformSmsJobs.developerId eq UUID.fromString(developerId)) and PlatformSmsJobs.sentAt.isNull() }
            .orderBy(PlatformSmsJobs.createdAt, SortOrder.ASC)
            .map { row ->
                PlatformSmsJobDto(
                    id = row[PlatformSmsJobs.id].value.toString(),
                    phoneNumber = row[PlatformSmsJobs.phoneNumber],
                    message = row[PlatformSmsJobs.message],
                )
            }
    }

    fun markSentFor(developerId: String, jobId: String): Unit = transaction {
        PlatformSmsJobs.update({
            (PlatformSmsJobs.id eq UUID.fromString(jobId)) and (PlatformSmsJobs.developerId eq UUID.fromString(developerId))
        }) { it[sentAt] = System.currentTimeMillis() }
    }

    // Best-effort housekeeping - expired sessions/pending signups are
    // harmless left alone (every read path already checks expiresAt), but
    // there's no reason to let these tables grow forever. Not wired to a
    // scheduler yet; cheap enough to call opportunistically if one gets
    // added later.
    fun purgeExpired(): Unit = transaction {
        val now = System.currentTimeMillis()
        PlatformOAuthSessions.deleteWhere { PlatformOAuthSessions.expiresAt less now }
        PendingPlatformSignups.deleteWhere { PendingPlatformSignups.expiresAt less now }
    }
}
