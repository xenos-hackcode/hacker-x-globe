package com.xhacker.cedal.sync

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

// Cedal Internal Sync - lets Cedal's own apps on the same device exchange
// small internal directives (never user data) so a security fix/config
// change can reach every installed Cedal app at once instead of updating
// each one separately. Off by default (Settings > Security > "Cedal
// Internal Sync") - the user has to explicitly opt in.
//
// How it stays legitimate/policy-safe (not a "collusion" pattern):
// - <queries> in the manifest names each known Cedal package explicitly -
//   this only ever checks "is this ONE specific app of mine installed",
//   never broad package visibility (which Play Protect does flag).
// - The broadcast itself requires a signature-level permission
//   (INTERNAL_SYNC) - Android enforces that only apps signed with the exact
//   same certificate as this one can send OR receive it. A different
//   developer's app literally cannot participate even if it tried.
// - The payload is encrypted (not to hide it from the app owner - it's a
//   fixed shared key the owner controls - just so nothing else on the
//   device can read it in transit).
// - Fully disclosed to the user via the Settings toggle + its own
//   description, not hidden.
object CedalInternalSync {
    const val ACTION = "com.xhacker.cedal.action.INTERNAL_SYNC"
    const val PERMISSION = "com.xhacker.cedal.permission.INTERNAL_SYNC"
    const val EXTRA_PAYLOAD = "payload"

    // Every known Cedal app's applicationId - must match this app's own
    // <queries> block exactly, or Android's package-visibility rules (API
    // 30+) will silently hide the other app from getPackageInfo/isInstalled
    // even though it's really there.
    val KNOWN_PACKAGES = listOf("com.xhacker.cedalmobiledev", "com.xhacker.cedalsmsrelay")

    private const val PREFS = "cedal_internal_sync"
    private const val KEY_ENABLED = "enabled"

    // Fixed across every Cedal app - both sides need the identical key to
    // decrypt each other's broadcasts. Not a secret from the app owner (who
    // wrote it), just from anything else that might intercept the broadcast.
    private val AES_KEY = byteArrayOf(
        0x4c, 0x1a, 0x8e.toByte(), 0x2f, 0x7d, 0x33, 0x91.toByte(), 0x6b,
        0xa5.toByte(), 0x0c, 0x5e, 0xf8.toByte(), 0x22, 0x74, 0xd9.toByte(), 0x3a,
        0x88.toByte(), 0x11, 0x67, 0xbc.toByte(), 0x4e, 0x9f.toByte(), 0x05, 0xea.toByte(),
        0x36, 0x71, 0xc4.toByte(), 0x19, 0x8a.toByte(), 0x2d, 0x53, 0xf1.toByte(),
    )

    fun isEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_ENABLED, false)

    fun setEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    fun isInstalled(context: Context, packageName: String): Boolean = try {
        context.packageManager.getPackageInfo(packageName, 0)
        true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }

    // "Transfer" - sends to every other known Cedal app currently installed
    // (each one's "collector" receiver). A no-op if the user has this off.
    fun send(context: Context, message: String) {
        if (!isEnabled(context)) return
        val encrypted = encrypt(message)
        KNOWN_PACKAGES.filterNot { it == context.packageName }.forEach { pkg ->
            if (isInstalled(context, pkg)) {
                val intent = Intent(ACTION).setPackage(pkg).putExtra(EXTRA_PAYLOAD, encrypted)
                context.sendBroadcast(intent, PERMISSION)
            }
        }
    }

    fun encrypt(message: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val iv = ByteArray(12).also { java.security.SecureRandom().nextBytes(it) }
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(AES_KEY, "AES"), GCMParameterSpec(128, iv))
        val cipherText = cipher.doFinal(message.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(iv + cipherText, Base64.NO_WRAP)
    }

    fun decrypt(payload: String): String? = try {
        val bytes = Base64.decode(payload, Base64.NO_WRAP)
        val iv = bytes.copyOfRange(0, 12)
        val cipherText = bytes.copyOfRange(12, bytes.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(AES_KEY, "AES"), GCMParameterSpec(128, iv))
        String(cipher.doFinal(cipherText), Charsets.UTF_8)
    } catch (e: Exception) {
        null
    }
}

// "Collector" - receives a directive from another Cedal app. Currently just
// logs it (no directives are defined yet) - a real handler gets added here
// once there's an actual security action to take (e.g. "force-refresh
// config", "disable feature X").
class CedalSyncReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (!CedalInternalSync.isEnabled(context)) return
        val payload = intent.getStringExtra(CedalInternalSync.EXTRA_PAYLOAD) ?: return
        val message = CedalInternalSync.decrypt(payload) ?: return
        android.util.Log.i("CedalInternalSync", "Received from ${intent.`package`}: $message")
    }
}
