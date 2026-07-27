package com.xhacker.cedal.util

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import com.xhacker.cedal.data.SecureStorage

// Real notification-sound playback, called right alongside every
// NotificationManagerCompat.notify() (FriendRequestSession.kt,
// MemberCodeScreen.kt). Both notification channels (CedalApplication.kt)
// are created silent on purpose - a plain NotificationChannel sound is
// fixed at creation time and always plays at the SYSTEM notification
// volume, which can't be controlled per-app. Playing it ourselves via
// MediaPlayer is the only way "Sounds" (Settings) can actually control
// loudness like the user asked for, and lets "off" mean real vibrate-only
// instead of just falling back to the system default tone.
//
// SecureStorage's @Inject constructor is a plain public constructor - safe
// to call directly here even outside Hilt's graph, same Context either way.
object NotificationSound {
    fun playIfEnabled(context: Context) {
        val storage = SecureStorage(context)
        if (!storage.soundsEnabled) {
            vibrate(context, 300)
            return
        }
        val uri = storage.notificationSoundUri?.let { runCatching { Uri.parse(it) }.getOrNull() }
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        if (uri == null) {
            vibrate(context, 300)
            return
        }
        runCatching {
            val player = MediaPlayer()
            player.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            player.setDataSource(context, uri)
            val volume = (storage.notificationVolume / 100f).coerceIn(0f, 1f)
            player.setVolume(volume, volume)
            player.setOnCompletionListener { it.release() }
            player.setOnErrorListener { mp, _, _ -> mp.release(); true }
            player.prepare()
            player.start()
        }.onFailure {
            // A revoked/invalid picked-file URI shouldn't crash notification
            // delivery - fall back to vibration so something still happens.
            vibrate(context, 300)
        }
    }
}
