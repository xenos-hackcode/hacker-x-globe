package com.xhacker.cedalsmsrelay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.telephony.SmsManager
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

// Cedal's SMS gateway - polls cedal-server for pending verification texts
// (see PendingSmsJobs/SmsRelayService server-side) and sends each one for
// real through THIS phone's own SIM via SmsManager, instead of paying for
// Twilio. A plain HttpURLConnection/org.json poll loop rather than
// Retrofit/OkHttp - this whole app has exactly one job, so it isn't worth
// the extra dependencies.
class RelayService : Service() {
    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification("Watching for pending SMS…"))
        val serverUrl = intent?.getStringExtra(EXTRA_SERVER_URL) ?: return START_NOT_STICKY
        val secret = intent.getStringExtra(EXTRA_SECRET) ?: return START_NOT_STICKY

        isRunning = true
        job?.cancel()
        job = scope.launch {
            while (true) {
                try {
                    pollOnce(serverUrl, secret)
                } catch (e: Exception) {
                    updateNotification("Error: ${e.message}")
                }
                delay(POLL_INTERVAL_MS)
            }
        }
        return START_STICKY
    }

    private fun pollOnce(serverUrl: String, secret: String) {
        val jobs = fetchPendingJobs(serverUrl, secret)
        if (jobs.isEmpty()) return
        @Suppress("DEPRECATION")
        val smsManager = SmsManager.getDefault()
        jobs.forEach { pending ->
            try {
                smsManager.sendTextMessage(pending.phoneNumber, null, pending.message, null, null)
                markSent(serverUrl, secret, pending.id)
                updateNotification("Sent to ${pending.phoneNumber} just now.")
            } catch (e: Exception) {
                updateNotification("Failed to send to ${pending.phoneNumber}: ${e.message}")
            }
        }
    }

    private data class SmsJob(val id: String, val phoneNumber: String, val message: String)

    private fun fetchPendingJobs(serverUrl: String, secret: String): List<SmsJob> {
        val conn = URL("$serverUrl/sms-relay/pending").openConnection() as HttpURLConnection
        conn.setRequestProperty("Authorization", "Bearer $secret")
        conn.connectTimeout = 15_000
        conn.readTimeout = 15_000
        return try {
            if (conn.responseCode != 200) return emptyList()
            val body = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
            val array = JSONArray(body)
            (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                SmsJob(obj.getString("id"), obj.getString("phoneNumber"), obj.getString("message"))
            }
        } finally {
            conn.disconnect()
        }
    }

    private fun markSent(serverUrl: String, secret: String, id: String) {
        val conn = URL("$serverUrl/sms-relay/$id/sent").openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Authorization", "Bearer $secret")
        conn.setRequestProperty("Content-Type", "application/json")
        conn.doOutput = true
        conn.connectTimeout = 15_000
        conn.readTimeout = 15_000
        try {
            OutputStreamWriter(conn.outputStream).use { it.write("{}") }
            conn.responseCode
        } finally {
            conn.disconnect()
        }
    }

    private fun buildNotification(text: String): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "SMS Relay", NotificationManager.IMPORTANCE_LOW)
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Cedal SMS Relay")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_send)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(text: String) {
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
            .notify(NOTIFICATION_ID, buildNotification(text))
    }

    override fun onDestroy() {
        isRunning = false
        job?.cancel()
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val EXTRA_SERVER_URL = "serverUrl"
        const val EXTRA_SECRET = "secret"
        private const val CHANNEL_ID = "sms_relay"
        private const val NOTIFICATION_ID = 4201
        private const val POLL_INTERVAL_MS = 8_000L
        var isRunning = false
            private set
    }
}
