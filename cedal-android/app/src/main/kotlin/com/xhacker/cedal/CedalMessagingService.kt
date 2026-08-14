package com.xhacker.cedal

import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.xhacker.cedal.data.SecureStorage
import com.xhacker.cedal.util.NotificationSound
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

// Real push notifications (2026-08-13) - this app previously had zero push
// infrastructure at all (see MessageNotificationSession's own doc comment,
// written before this existed): notifications only ever fired while the
// app's process was alive to poll, so closing the app meant nothing ever
// arrived. cedal-server's PushNotificationService sends a DATA-only FCM
// message (never the `notification` payload) specifically so this method
// always runs - a `notification` payload is auto-displayed by the OS only
// when the app's backgrounded, silently skipping onMessageReceived exactly
// when foreground-only apps want custom handling too.
class CedalMessagingService : FirebaseMessagingService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Registered fresh on every login (see AuthViewModel.registerFcmToken)
    // AND here - a token can rotate at any time (app reinstall, data
    // clear, Google Play Services update), independent of any login event.
    override fun onNewToken(token: String) {
        val storage = SecureStorage(applicationContext)
        val userId = storage.userId ?: return
        val accessToken = storage.accessToken ?: return
        scope.launch {
            runCatching {
                val client = OkHttpClient()
                val body = JSONObject().put("token", token).toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url("${com.xhacker.cedal.data.BASE_URL}users/fcm-token")
                    .header("Authorization", "Bearer $accessToken")
                    .post(body)
                    .build()
                client.newCall(request).execute().close()
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        val title = data["title"] ?: return
        val body = data["body"].orEmpty()
        val channelId = when (data["type"]) {
            "friend_request", "friend_request_accepted" -> FRIENDS_NOTIFICATION_CHANNEL_ID
            else -> MESSAGES_NOTIFICATION_CHANNEL_ID
        }

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            data["friend_id"]?.let { putExtra("open_chat_friend_id", it) }
            data["friend_name"]?.let { putExtra("open_chat_friend_name", it) }
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            (data["notify_key"] ?: title).hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_menu_add)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        // Same "grouped per-conversation" offset MessageNotificationSession
        // already uses, so a push covering the same conversation the poll
        // already notified about (a normal race while the app happens to
        // still be alive) updates that same notification instead of
        // duplicating it.
        val id = 3000 + ((data["notify_key"] ?: title).hashCode() and 0xFFFF)
        NotificationManagerCompat.from(applicationContext).notify(id, notification)
        NotificationSound.playIfEnabled(applicationContext)
    }
}
