package com.xhacker.cedal.services

import com.xhacker.cedal.db.PlatformEmailCredentials
import com.xhacker.cedal.db.PlatformEmailRateLimit
import com.xhacker.cedal.db.PlatformEmailSends
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.util.UUID

// The SMS relay platform's email add-on - see the plan doc / this file's
// sibling PlatformDeveloperService for the base platform account. A
// developer picks one of two modes when they register (see
// PlatformEmailCredentials' own doc comment): "own_smtp" (their own
// credentials, decentralized, mirrors the SMS design) or "shared" (through
// Cedal's own SMTP account, rate-limited - see RATE_LIMIT_MAX/WINDOW below).
object PlatformEmailService {
    private const val RATE_LIMIT_MAX = 20
    private const val RATE_LIMIT_WINDOW_MS = 24L * 60 * 60 * 1000

    sealed class RegisterResult {
        object Registered : RegisterResult()
        object InvalidKey : RegisterResult()
        object InvalidMode : RegisterResult()
        object ConnectionFailed : RegisterResult()
        object MissingFields : RegisterResult()
    }

    fun registerCredentials(rawKey: String, mode: String, host: String?, port: String?, username: String?, password: String?, from: String?): RegisterResult {
        val developerId = PlatformDeveloperService.verifyDeveloperToken(rawKey) ?: return RegisterResult.InvalidKey
        if (mode != "own_smtp" && mode != "shared") return RegisterResult.InvalidMode

        if (mode == "own_smtp") {
            if (host.isNullOrBlank() || username.isNullOrBlank() || password.isNullOrBlank()) return RegisterResult.MissingFields
            val p = port?.takeIf { it.isNotBlank() } ?: "587"
            // Auth-only handshake before persisting anything - a developer
            // finds out immediately if their credentials are wrong, instead
            // of silently failing on their first real send later.
            if (!EmailService.testConnection(host, p, username, password)) return RegisterResult.ConnectionFailed

            transaction {
                PlatformEmailCredentials.deleteWhere { PlatformEmailCredentials.developerId eq UUID.fromString(developerId) }
                PlatformEmailCredentials.insert {
                    it[PlatformEmailCredentials.developerId] = UUID.fromString(developerId)
                    it[PlatformEmailCredentials.mode] = mode
                    it[smtpHostEnc] = CryptoService.encrypt(host)
                    it[smtpPortEnc] = CryptoService.encrypt(p)
                    it[smtpUsernameEnc] = CryptoService.encrypt(username)
                    it[smtpPasswordEnc] = CryptoService.encrypt(password)
                    it[smtpFromEnc] = from?.takeIf { it.isNotBlank() }?.let { CryptoService.encrypt(it) }
                }
            }
        } else {
            transaction {
                PlatformEmailCredentials.deleteWhere { PlatformEmailCredentials.developerId eq UUID.fromString(developerId) }
                PlatformEmailCredentials.insert {
                    it[PlatformEmailCredentials.developerId] = UUID.fromString(developerId)
                    it[PlatformEmailCredentials.mode] = mode
                }
            }
        }
        return RegisterResult.Registered
    }

    sealed class SendResult {
        object Sent : SendResult()
        object InvalidKey : SendResult()
        object NotRegistered : SendResult()
        object RateLimited : SendResult()
        object SendFailed : SendResult()
    }

    fun sendEmail(rawKey: String, to: String, subject: String, body: String): SendResult {
        val developerId = PlatformDeveloperService.verifyDeveloperToken(rawKey) ?: return SendResult.InvalidKey
        val uid = UUID.fromString(developerId)
        val creds = transaction {
            PlatformEmailCredentials.selectAll().where { PlatformEmailCredentials.developerId eq uid }.firstOrNull()
        } ?: return SendResult.NotRegistered

        val mode = creds[PlatformEmailCredentials.mode]
        val success: Boolean

        if (mode == "own_smtp") {
            val host = creds[PlatformEmailCredentials.smtpHostEnc]?.let { CryptoService.decrypt(it) }
            val port = creds[PlatformEmailCredentials.smtpPortEnc]?.let { CryptoService.decrypt(it) }
            val username = creds[PlatformEmailCredentials.smtpUsernameEnc]?.let { CryptoService.decrypt(it) }
            val password = creds[PlatformEmailCredentials.smtpPasswordEnc]?.let { CryptoService.decrypt(it) }
            val from = creds[PlatformEmailCredentials.smtpFromEnc]?.let { CryptoService.decrypt(it) }
            success = EmailService.send(to, subject, body, host, port, username, password, from)
        } else {
            if (!checkAndConsumeRateLimit(uid)) {
                recordSend(uid, mode, to, subject, success = false)
                return SendResult.RateLimited
            }
            success = EmailService.send(to, subject, body)
        }

        recordSend(uid, mode, to, subject, success)
        return if (success) SendResult.Sent else SendResult.SendFailed
    }

    // Fixed rolling window, same read-check-update-in-one-transaction shape
    // as AuthService's LockoutState - the one throttle pattern already
    // established in this codebase.
    private fun checkAndConsumeRateLimit(developerId: UUID): Boolean = transaction {
        val now = System.currentTimeMillis()
        val row = PlatformEmailRateLimit.selectAll().where { PlatformEmailRateLimit.developerId eq developerId }.firstOrNull()
        if (row == null) {
            PlatformEmailRateLimit.insert {
                it[PlatformEmailRateLimit.developerId] = developerId
                it[windowStart] = now
                it[countInWindow] = 1
            }
            return@transaction true
        }
        val windowStart = row[PlatformEmailRateLimit.windowStart]
        if (now - windowStart > RATE_LIMIT_WINDOW_MS) {
            PlatformEmailRateLimit.update({ PlatformEmailRateLimit.developerId eq developerId }) {
                it[PlatformEmailRateLimit.windowStart] = now
                it[countInWindow] = 1
            }
            return@transaction true
        }
        val count = row[PlatformEmailRateLimit.countInWindow]
        if (count >= RATE_LIMIT_MAX) return@transaction false
        PlatformEmailRateLimit.update({ PlatformEmailRateLimit.developerId eq developerId }) {
            it[countInWindow] = count + 1
        }
        true
    }

    private fun recordSend(developerId: UUID, mode: String, to: String, subject: String, success: Boolean): Unit = transaction {
        PlatformEmailSends.insert {
            it[PlatformEmailSends.developerId] = developerId
            it[PlatformEmailSends.mode] = mode
            it[toAddress] = to
            it[PlatformEmailSends.subject] = subject
            it[sentAt] = System.currentTimeMillis()
            it[PlatformEmailSends.success] = success
        }
    }
}
