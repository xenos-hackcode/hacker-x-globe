package com.xhacker.cedal.services

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

// The ONLY reversible-encryption capability in this codebase - everything
// else (passwords, refresh tokens, platform activation keys) is BCrypt
// one-way hashing, which can verify a guess but never recover a value.
// This exists specifically because PlatformEmailService needs to actually
// USE a developer's own SMTP password to authenticate on their behalf, not
// just check it against a stored guess. EMAIL_CREDENTIALS_ENCRYPTION_KEY is
// the single point of failure for every credential encrypted with it -
// back it up the same way as the SMS-relay release keystore, never let it
// exist on only one machine.
object CryptoService {
    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val GCM_TAG_BITS = 128
    private const val NONCE_BYTES = 12

    private val random = SecureRandom()
    private val key: SecretKeySpec by lazy {
        val raw = System.getenv("EMAIL_CREDENTIALS_ENCRYPTION_KEY")
            ?: throw IllegalStateException("EMAIL_CREDENTIALS_ENCRYPTION_KEY is not set")
        SecretKeySpec(Base64.getDecoder().decode(raw), "AES")
    }

    // Output: base64(nonce (12 bytes) + ciphertext+tag) - the nonce travels
    // with the ciphertext since GCM needs the exact same one to decrypt,
    // and a fresh random nonce per call is what makes GCM safe to reuse a
    // single key across many encrypt() calls.
    fun encrypt(plaintext: String): String {
        val nonce = ByteArray(NONCE_BYTES).also { random.nextBytes(it) }
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, nonce))
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        return Base64.getEncoder().encodeToString(nonce + ciphertext)
    }

    fun decrypt(blob: String): String {
        val bytes = Base64.getDecoder().decode(blob)
        val nonce = bytes.copyOfRange(0, NONCE_BYTES)
        val ciphertext = bytes.copyOfRange(NONCE_BYTES, bytes.size)
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, nonce))
        return String(cipher.doFinal(ciphertext), Charsets.UTF_8)
    }
}
