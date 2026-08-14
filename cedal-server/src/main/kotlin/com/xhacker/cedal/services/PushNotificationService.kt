package com.xhacker.cedal.services

import com.xhacker.cedal.db.Users
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

// Real push notifications (2026-08-13). Before this, every notification in
// this app was poll-while-app-alive (MessageNotificationSession,
// FriendRequestSession, AiRequestNotificationSession - see their own doc
// comments) - closing the app stopped polling entirely, so nothing arrived
// until the app was reopened. This calls FCM's v1 HTTP API directly (no
// Firebase Admin SDK - it's a heavy dependency for one endpoint) using an
// OAuth2 token from Cloud Run's own attached service account, fetched off
// the instance metadata server rather than managing a service account key
// file - the service account already holds roles/firebasecloudmessaging.admin
// (granted 2026-08-13, see planner's done.md for the exact gcloud command).
//
// Always sends a DATA-only payload (never the `notification` field) -
// CedalMessagingService.onMessageReceived needs to run unconditionally,
// including while the app's foregrounded, which a `notification` payload
// doesn't guarantee (the OS may auto-display and skip onMessageReceived).
//
// Every call here is fire-and-forget from the caller's perspective (a new
// message/friend request/etc. must never fail or slow down because a push
// failed to send) - launched on its own scope rather than making every
// call site suspend just for this one side effect.
object PushNotificationService {
    private val client = HttpClient(CIO) {
        install(HttpTimeout) { requestTimeoutMillis = 10_000 }
    }
    private val json = Json { ignoreUnknownKeys = true }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private const val PROJECT_ID = "cedal-fd4a2"

    @Serializable
    private data class MetadataTokenResponse(val access_token: String, val expires_in: Long)

    @Volatile private var cachedToken: String? = null
    @Volatile private var cachedTokenExpiresAtMs: Long = 0L

    private suspend fun accessToken(): String? {
        val now = System.currentTimeMillis()
        cachedToken?.let { if (now < cachedTokenExpiresAtMs - 60_000) return it }
        return try {
            val bodyText = client.get("http://metadata.google.internal/computeMetadata/v1/instance/service-accounts/default/token") {
                header("Metadata-Flavor", "Google")
            }.body<String>()
            val parsed = json.decodeFromString<MetadataTokenResponse>(bodyText)
            cachedToken = parsed.access_token
            cachedTokenExpiresAtMs = now + parsed.expires_in * 1000L
            parsed.access_token
        } catch (e: Exception) {
            null
        }
    }

    // notifyKey groups related pushes the same way MessageNotificationSession's
    // own notification-id offset already does client-side (e.g. the sender's
    // friendId) - CedalMessagingService reuses it so a second push about the
    // same conversation updates one notification instead of stacking.
    fun send(userId: String, title: String, body: String, type: String, notifyKey: String, extraData: Map<String, String> = emptyMap()) {
        scope.launch {
            val token = transaction { Users.selectAll().where { Users.id eq UUID.fromString(userId) }.firstOrNull()?.get(Users.fcmToken) } ?: return@launch
            val accessToken = accessToken() ?: return@launch
            val dataPayload: JsonObject = buildJsonObject {
                put("title", title)
                put("body", body)
                put("type", type)
                put("notify_key", notifyKey)
                extraData.forEach { (k, v) -> put(k, v) }
            }
            runCatching {
                client.post("https://fcm.googleapis.com/v1/projects/$PROJECT_ID/messages:send") {
                    header("Authorization", "Bearer $accessToken")
                    contentType(ContentType.Application.Json)
                    setBody(
                        buildJsonObject {
                            putJsonObject("message") {
                                put("token", token)
                                put("data", dataPayload)
                            }
                        }.toString(),
                    )
                }
            }
        }
    }
}
