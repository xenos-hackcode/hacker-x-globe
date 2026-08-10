package com.xhacker.cedal.services

import io.ktor.client.HttpClient
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

    // Registered once per bot on create/update into "cedal" hosting mode -
    // Telegram then calls our webhook route directly on every incoming
    // message. secret_token round-trips back on every call as the
    // X-Telegram-Bot-Api-Secret-Token header, doubling as free per-bot
    // webhook auth (reuses the bot's own secretToken, no separate secret).
    suspend fun registerWebhook(botId: String, token: String, secretToken: String) {
        runCatching {
            client.post("https://api.telegram.org/bot$token/setWebhook") {
                contentType(ContentType.Application.Json)
                setBody(json.encodeToString(SetWebhookRequest(url = "${serverBaseUrl()}/bots/$botId/telegram-webhook", secret_token = secretToken)))
            }
        }
    }

    suspend fun unregisterWebhook(token: String) {
        runCatching { client.post("https://api.telegram.org/bot$token/deleteWebhook") }
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
