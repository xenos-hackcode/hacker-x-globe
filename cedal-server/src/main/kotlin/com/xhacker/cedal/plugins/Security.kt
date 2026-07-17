package com.xhacker.cedal.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import java.io.File
import java.security.SecureRandom
import java.util.Date

object JwtConfig {
    // Persisted to a local, gitignored file instead of regenerated every
    // process start — this project gets restarted constantly during dev
    // (every Code-mode change this session), and a fresh secret each time
    // silently invalidates every session token already on a phone, which
    // then surfaces as a confusing "unauthorized" on whatever feature
    // happens to get tapped next. Generated once on first run, reused after.
    private val secret = loadOrCreateSecret()

    private fun loadOrCreateSecret(): String {
        // On Cloud Run, JWT_SECRET is mounted from Secret Manager - every
        // instance needs the exact same value, so the local generate-and-persist
        // fallback below (fine for a single dev machine) would break token
        // verification across instances/restarts if used there instead.
        System.getenv("JWT_SECRET")?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }

        val file = File("jwt_secret.local")
        if (file.isFile) {
            val existing = file.readText().trim()
            if (existing.isNotEmpty()) return existing
        }
        val generated = ByteArray(32).also { SecureRandom().nextBytes(it) }.joinToString("") { "%02x".format(it) }
        file.writeText(generated)
        return generated
    }

    const val ISSUER = "cedal-server"
    const val AUDIENCE = "cedal-android"
    const val REALM = "cedal"
    // Long-lived for now (dev/personal-use stage, not yet a public-facing
    // product) so day-to-day testing doesn't hit expiry constantly. The
    // client's TokenAuthenticator refreshes automatically regardless, but
    // this keeps that path from being needed on every short session.
    private const val ACCESS_TOKEN_TTL_MS = 24 * 60 * 60 * 1000L // 24 hours

    val algorithm: Algorithm = Algorithm.HMAC256(secret)

    fun generateAccessToken(userId: String, role: String): String =
        JWT.create()
            .withIssuer(ISSUER)
            .withAudience(AUDIENCE)
            .withSubject(userId)
            .withClaim("role", role)
            .withExpiresAt(Date(System.currentTimeMillis() + ACCESS_TOKEN_TTL_MS))
            .sign(algorithm)
}

fun Application.configureSecurity() {
    install(Authentication) {
        jwt("auth-jwt") {
            realm = JwtConfig.REALM
            verifier(
                JWT.require(JwtConfig.algorithm)
                    .withIssuer(JwtConfig.ISSUER)
                    .withAudience(JwtConfig.AUDIENCE)
                    .build()
            )
            validate { credential ->
                if (credential.payload.subject != null) JWTPrincipal(credential.payload) else null
            }
        }
    }
}
