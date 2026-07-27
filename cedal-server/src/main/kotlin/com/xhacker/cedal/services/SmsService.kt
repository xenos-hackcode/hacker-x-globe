package com.xhacker.cedal.services

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.post
import io.ktor.client.request.basicAuth
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Parameters
import io.ktor.http.formUrlEncode
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess

// Real SMS delivery for phone verification (Settings > More > Security).
// Two paths: TWILIO_ACCOUNT_SID/TWILIO_AUTH_TOKEN/TWILIO_FROM_NUMBER (same
// DotEnv/env var convention as GROQ_API_KEY etc. in AiProviderService) sends
// immediately via Twilio's REST API when configured; otherwise this queues
// the message for the app owner's own SIM-based relay (see
// SmsRelayService/PendingSmsJobs) - a phone the owner controls polls for
// these and sends the real text itself, no third-party SMS API needed. Only
// if NEITHER is set up does sendSms() fall back to the "log it, echo it
// back in the dev response" pattern AuthService already uses for email too.
object SmsService {
    private val client = HttpClient(CIO) {
        install(HttpTimeout) { requestTimeoutMillis = 15_000 }
    }

    suspend fun sendSms(to: String, body: String): Boolean {
        val sid = DotEnv.get("TWILIO_ACCOUNT_SID")
        val token = DotEnv.get("TWILIO_AUTH_TOKEN")
        val from = DotEnv.get("TWILIO_FROM_NUMBER")
        if (sid == null || token == null || from == null) {
            SmsRelayService.enqueue(to, body)
            return true
        }
        return try {
            val response = client.post("https://api.twilio.com/2010-04-01/Accounts/$sid/Messages.json") {
                basicAuth(sid, token)
                contentType(ContentType.Application.FormUrlEncoded)
                setBody(Parameters.build {
                    append("To", to)
                    append("From", from)
                    append("Body", body)
                }.formUrlEncode())
            }
            if (!response.status.isSuccess()) {
                println("[cedal-server] Twilio SMS send failed: ${response.status} ${response.bodyAsText()}")
            }
            response.status.isSuccess()
        } catch (e: Exception) {
            println("[cedal-server] Twilio SMS send error: ${e.message}")
            false
        }
    }
}
