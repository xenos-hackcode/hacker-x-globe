package com.xhacker.cedal.services

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// Cedal-hosted WhatsApp bots (Round 3, 2026-08-10). Unlike Telegram, Meta's
// WhatsApp Cloud API has exactly one webhook URL per Meta App (configured
// by hand in the developer dashboard, not settable per-bot via API) - see
// BotRoutes.kt's /webhooks/whatsapp doc comment for how incoming messages
// get routed back to the right bot.
object WhatsAppBotService {
    private val client = HttpClient(CIO) {
        install(HttpTimeout) { requestTimeoutMillis = 15_000 }
    }
    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    private data class TextBody(val body: String)

    @Serializable
    private data class SendMessageRequest(
        val messaging_product: String = "whatsapp",
        val to: String,
        val type: String = "text",
        val text: TextBody,
    )

    suspend fun sendMessage(phoneNumberId: String, accessToken: String, toNumber: String, text: String) {
        runCatching {
            client.post("https://graph.facebook.com/v21.0/$phoneNumberId/messages") {
                header("Authorization", "Bearer $accessToken")
                contentType(ContentType.Application.Json)
                setBody(json.encodeToString(SendMessageRequest(to = toNumber, text = TextBody(text))))
            }
        }
    }
}
