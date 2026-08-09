package com.xhacker.cedal

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import dagger.hilt.android.HiltAndroidApp

const val ANDROID_BUILD_NOTIFICATION_CHANNEL_ID = "android_builds"
const val FRIENDS_NOTIFICATION_CHANNEL_ID = "friends"
const val MESSAGES_NOTIFICATION_CHANNEL_ID = "messages"

@HiltAndroidApp
class CedalApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Both channels are silent on purpose - Cedal plays its own
            // notification sound (see util/NotificationSound.kt) instead of
            // the system's fixed-at-creation channel sound, since that's
            // the only way Settings > Sounds' volume/vibrate-only toggle
            // can actually take effect.
            val codeChannel = NotificationChannel(
                ANDROID_BUILD_NOTIFICATION_CHANNEL_ID,
                "Code notifications",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Tells you when a Kotlin app build or a Backer code check (Code > Pad) finishes."
                setSound(null, null)
            }
            val friendsChannel = NotificationChannel(
                FRIENDS_NOTIFICATION_CHANNEL_ID,
                "Friend requests",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Tells you when someone sends you a friend request."
                setSound(null, null)
            }
            val messagesChannel = NotificationChannel(
                MESSAGES_NOTIFICATION_CHANNEL_ID,
                "Messages",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Tells you when someone sends you a new message."
                setSound(null, null)
            }
            val manager = getSystemService(NotificationManager::class.java)
            // A NotificationChannel's sound is fixed the moment it's first
            // created - devices that already had these channels (from
            // before this file added setSound(null, null)) would otherwise
            // keep their old default-sound behavior forever. Deleting +
            // recreating on every launch is cheap and safe (doesn't touch
            // notification history) and guarantees the silent setting
            // actually sticks everywhere.
            manager.deleteNotificationChannel(ANDROID_BUILD_NOTIFICATION_CHANNEL_ID)
            manager.deleteNotificationChannel(FRIENDS_NOTIFICATION_CHANNEL_ID)
            manager.deleteNotificationChannel(MESSAGES_NOTIFICATION_CHANNEL_ID)
            manager.createNotificationChannel(codeChannel)
            manager.createNotificationChannel(friendsChannel)
            manager.createNotificationChannel(messagesChannel)
        }
    }
}
