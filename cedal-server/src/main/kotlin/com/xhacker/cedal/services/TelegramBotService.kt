package com.xhacker.cedal.services

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// Cedal-hosted Telegram bots (Round 3, 2026-08-10) - a thin wrapper around
// Telegram's Bot API, used only when a bot's hostingMode is "cedal".
// Webhook mode, not getUpdates polling - see BotRoutes.kt's
// /{id}/telegram-webhook doc comment for why (Cloud Run has no precedent
// for a process-lifetime background task, and polling doesn't survive
// instance scale-to-zero/multi-instance the way a webhook does).
object TelegramBotService {
    private val client = HttpClient(CIO) {
        install(HttpTimeout) { requestTimeoutMillis = 15_000 }
    }
    private val json = Json { ignoreUnknownKeys = true }

    fun serverBaseUrl(): String =
        DotEnv.get("SERVER_BASE_URL") ?: "https://cedal-server-717899371194.us-central1.run.app"

    @Serializable
    private data class SetWebhookRequest(val url: String, val secret_token: String)

    // Telegram's Bot API always responds HTTP 200 even for a rejected
    // request (bad token, bad URL, etc.) - the real success/failure signal
    // is the "ok" field in the body, not the status code. A silent
    // runCatching that ignores this would report "saved" to the user while
    // Telegram never actually registered anything - the exact bug this was
    // found fixing (2026-08-10, a real bot's Save succeeded but nothing
    // arrived on Telegram, traced to this swallowing setWebhook's failure).
    @Serializable
    private data class TelegramApiResponse(val ok: Boolean, val description: String? = null)

    // Registered once per bot on create/update into "cedal" hosting mode -
    // Telegram then calls our webhook route directly on every incoming
    // message. secret_token round-trips back on every call as the
    // X-Telegram-Bot-Api-Secret-Token header, doubling as free per-bot
    // webhook auth (reuses the bot's own secretToken, no separate secret).
    // Throws AuthException (with Telegram's own description) on failure,
    // rather than swallowing it - this runs synchronously inside the
    // owner's Save action, so they need to actually see it if it fails.
    suspend fun registerWebhook(botId: String, token: String, secretToken: String) {
        val bodyText = try {
            client.post("https://api.telegram.org/bot$token/setWebhook") {
                contentType(ContentType.Application.Json)
                setBody(json.encodeToString(SetWebhookRequest(url = "${serverBaseUrl()}/bots/$botId/telegram-webhook", secret_token = secretToken)))
            }.body<String>()
        } catch (e: Exception) {
            throw AuthException("Couldn't reach Telegram to register the webhook: ${e.message}")
        }
        val parsed = runCatching { json.decodeFromString<TelegramApiResponse>(bodyText) }.getOrNull()
        if (parsed?.ok != true) {
            throw AuthException("Telegram rejected the webhook registration: ${parsed?.description ?: bodyText.take(200)}")
        }
    }

    suspend fun unregisterWebhook(token: String) {
        runCatching { client.post("https://api.telegram.org/bot$token/deleteWebhook") }
    }

    @Serializable
    private data class SetMyNameRequest(val name: String)

    // Cosmetic, best-effort (unlike registerWebhook, a failure here doesn't
    // break the bot actually working, just its displayed Telegram name) -
    // called whenever the character sheet's name changes, on any hosting
    // mode. Telegram limits this to 64 chars; Bots.name allows up to 100,
    // so this truncates rather than rejecting a longer character name.
    // There's no equivalent Bot API method for the bot's profile picture -
    // that's still a manual @BotFather /setuserpic step, not automatable.
    suspend fun setMyName(token: String, name: String) {
        runCatching {
            client.post("https://api.telegram.org/bot$token/setMyName") {
                contentType(ContentType.Application.Json)
                setBody(json.encodeToString(SetMyNameRequest(name = name.take(64))))
            }
        }
    }

    @Serializable
    private data class SendMessageRequest(val chat_id: String, val text: String)

    suspend fun sendMessage(token: String, chatId: String, text: String) {
        runCatching {
            client.post("https://api.telegram.org/bot$token/sendMessage") {
                contentType(ContentType.Application.Json)
                setBody(json.encodeToString(SendMessageRequest(chat_id = chatId, text = text)))
            }
        }
    }
}
