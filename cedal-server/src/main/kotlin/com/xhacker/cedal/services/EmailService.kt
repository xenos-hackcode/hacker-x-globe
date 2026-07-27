package com.xhacker.cedal.services

import jakarta.mail.Authenticator
import jakarta.mail.Message
import jakarta.mail.PasswordAuthentication
import jakarta.mail.Session
import jakarta.mail.Transport
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage
import java.util.Properties

// Real signup/verification emails via plain SMTP - provider-agnostic
// (Gmail, Google Workspace, Microsoft 365, or any company domain's mail
// server), not hardcoded to one provider. Needs SMTP_HOST/SMTP_PORT/
// SMTP_USERNAME/SMTP_PASSWORD/SMTP_FROM (same DotEnv/env var convention as
// TWILIO_* in SmsService) - until those are set, send() returns false and
// the caller falls back to the same "log it, echo it back in the dev
// response" pattern AuthService already uses.
object EmailService {
    // Every param optionally overrides the corresponding SMTP_* env var -
    // added for PlatformEmailService's "bring your own SMTP" mode, where a
    // platform developer's own decrypted credentials get passed in instead
    // of Cedal's own. Every existing call site passes none of these, so
    // behavior is byte-for-byte unchanged from before this existed.
    fun send(
        to: String, subject: String, body: String,
        host: String? = null, port: String? = null, username: String? = null, password: String? = null, from: String? = null,
    ): Boolean {
        val h = host ?: DotEnv.get("SMTP_HOST") ?: return false
        val p = port ?: DotEnv.get("SMTP_PORT") ?: "587"
        val u = username ?: DotEnv.get("SMTP_USERNAME") ?: return false
        val pw = password ?: DotEnv.get("SMTP_PASSWORD") ?: return false
        val f = from ?: DotEnv.get("SMTP_FROM") ?: u

        val session = buildSession(h, p, u, pw)
        return try {
            val message = MimeMessage(session).apply {
                setFrom(InternetAddress(f, "Cedal"))
                setRecipients(Message.RecipientType.TO, InternetAddress.parse(to))
                setSubject(subject)
                setText(body)
            }
            Transport.send(message)
            true
        } catch (e: Exception) {
            println("[cedal-server] SMTP send error: ${e.message}")
            false
        }
    }

    // Auth-only handshake, no message sent - lets PlatformEmailService
    // validate a developer's own SMTP credentials at registration time
    // instead of only discovering they're wrong on the first real send.
    fun testConnection(host: String, port: String, username: String, password: String): Boolean {
        val session = buildSession(host, port, username, password)
        return try {
            session.getTransport("smtp").apply {
                connect(host, port.toIntOrNull() ?: 587, username, password)
                close()
            }
            true
        } catch (e: Exception) {
            println("[cedal-server] SMTP test-connection error: ${e.message}")
            false
        }
    }

    private fun buildSession(host: String, port: String, username: String, password: String): Session {
        val props = Properties().apply {
            put("mail.smtp.host", host)
            put("mail.smtp.port", port)
            put("mail.smtp.auth", "true")
            put("mail.smtp.starttls.enable", "true")
        }
        return Session.getInstance(props, object : Authenticator() {
            override fun getPasswordAuthentication() = PasswordAuthentication(username, password)
        })
    }
}
