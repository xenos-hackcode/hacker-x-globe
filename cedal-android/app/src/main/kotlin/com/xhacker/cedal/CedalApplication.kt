package com.xhacker.cedal

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import dagger.hilt.android.HiltAndroidApp

const val ANDROID_BUILD_NOTIFICATION_CHANNEL_ID = "android_builds"
const val FRIENDS_NOTIFICATION_CHANNEL_ID = "friends"

@HiltAndroidApp
class CedalApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val codeChannel = NotificationChannel(
                ANDROID_BUILD_NOTIFICATION_CHANNEL_ID,
                "Code notifications",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Tells you when a Kotlin app build or a Backer code check (Code > Pad) finishes."
            }
            val friendsChannel = NotificationChannel(
                FRIENDS_NOTIFICATION_CHANNEL_ID,
                "Friend requests",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Tells you when someone sends you a friend request."
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(codeChannel)
            manager.createNotificationChannel(friendsChannel)
        }
    }
}
