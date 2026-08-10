package com.xhacker.cedal.routes

import com.xhacker.cedal.models.BotConverseRequest
import com.xhacker.cedal.models.BotConverseResponse
import com.xhacker.cedal.models.BotCreate
import com.xhacker.cedal.models.BotSecretResponse
import com.xhacker.cedal.models.BotSetPremiumRequest
import com.xhacker.cedal.models.BotTestChatRequest
import com.xhacker.cedal.models.BotTurnDto
import com.xhacker.cedal.models.BotUpdate
import com.xhacker.cedal.services.AuthException
import com.xhacker.cedal.services.BotBrainService
import com.xhacker.cedal.services.BotService
import com.xhacker.cedal.services.BotTemplateService
import com.xhacker.cedal.services.DotEnv
import com.xhacker.cedal.services.TelegramBotService
import com.xhacker.cedal.services.WhatsAppBotService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

// Member > More > Bots (MemberBotsScreen.kt) - Round 1 CRUD, Round 2's
// brain endpoint, Round 3 (2026-08-10) real Telegram/WhatsApp connectivity.
// See BotBrainService's doc comment for how JWT-gated /test-chat and
// secretToken-gated /converse differ; see this file's telegram-webhook and
// /webhooks/whatsapp routes below for why Round 3 uses webhooks rather than
// Telegram's classic getUpdates polling (no precedent anywhere in this
// server for a process-lifetime background task, and Cloud Run's
// scale-to-zero/multi-instance model doesn't suit a persistent poller).
fun Route.botRoutes() {
    route("/bots") {
        authenticate("auth-jwt") {
            get {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                call.respond(HttpStatusCode.OK, BotService.list(userId))
            }
            post {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                val req = call.receive<BotCreate>()
                call.respond(HttpStatusCode.Created, BotService.create(userId, req))
            }
            get("/{id}") {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                val bot = BotService.get(userId, call.parameters["id"]!!)
                if (bot == null) call.respond(HttpStatusCode.NotFound, mapOf("error" to "Bot not found"))
                else call.respond(HttpStatusCode.OK, bot)
            }
            put("/{id}") {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                val req = call.receive<BotUpdate>()
                val bot = BotService.update(userId, call.parameters["id"]!!, req)
                if (bot == null) call.respond(HttpStatusCode.NotFound, mapOf("error" to "Bot not found"))
                else call.respond(HttpStatusCode.OK, bot)
            }
            delete("/{id}") {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                val deleted = BotService.delete(userId, call.parameters["id"]!!)
                if (!deleted) call.respond(HttpStatusCode.NotFound, mapOf("error" to "Bot not found"))
                else call.respond(HttpStatusCode.OK, mapOf("deleted" to true))
            }
            // In-app test chat - owner-only (BotService.get already scopes
            // to the caller's own bots). Not in Round 2's original scope
            // (that was just the external /converse path below, for
            // Round 3's self-hosted/cedal-hosted paths), added so a bot
            // can actually be tried before real platform connectivity.
            // Keyed by the owner's own userId as chatId, so this never
            // collides with a real external conversation.
            get("/{id}/test-chat") {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                val id = call.parameters["id"]!!
                BotService.get(userId, id) ?: throw AuthException("Bot not found")
                val turns = BotBrainService.history(id, userId).map { (role, content) -> BotTurnDto(role, content) }
                call.respond(HttpStatusCode.OK, turns)
            }
            post("/{id}/test-chat") {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                val id = call.parameters["id"]!!
                BotService.get(userId, id) ?: throw AuthException("Bot not found")
                val req = call.receive<BotTestChatRequest>()
                val reply = BotBrainService.converse(id, userId, req.message)
                call.respond(HttpStatusCode.OK, BotConverseResponse(reply))
            }
            // Admin-only (not owner-scoped - see BotService.setPremium).
            // Placeholder for real payment: same deferred, admin-toggle-only
            // shape PurchaseXpService already used for the XP Shop.
            post("/{id}/set-premium") {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                val req = call.receive<BotSetPremiumRequest>()
                val bot = BotService.setPremium(userId, call.parameters["id"]!!, req.isPremium)
                if (bot == null) call.respond(HttpStatusCode.NotFound, mapOf("error" to "Bot not found"))
                else call.respond(HttpStatusCode.OK, bot)
            }
            // Owner-only. Self-hosted download needs the raw secret embedded
            // in the generated code; cedal-hosted WhatsApp setup needs to
            // show it to the user as the Meta webhook verify token.
            get("/{id}/reveal-secret") {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                val secret = BotService.revealSecret(userId, call.parameters["id"]!!)
                if (secret == null) call.respond(HttpStatusCode.NotFound, mapOf("error" to "Bot not found"))
                else call.respond(HttpStatusCode.OK, BotSecretResponse(secret))
            }
            // Owner-only. Bundles a self-hosted platform client (the free
            // hostingMode="self" path) as a zip - see BotTemplateService.
            get("/{id}/download") {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                val id = call.parameters["id"]!!
                val bot = BotService.get(userId, id) ?: throw AuthException("Bot not found")
                val secret = BotService.revealSecret(userId, id) ?: throw AuthException("Bot not found")
                val creds = BotService.getCredentialsForDownload(userId, id) ?: throw AuthException("Bot not found")
                val zip = BotTemplateService.buildZip(
                    botId = id, secretToken = secret, serverUrl = TelegramBotService.serverBaseUrl(),
                    botType = creds.botType, telegramToken = creds.telegramToken,
                    whatsappPhoneNumberId = creds.whatsappPhoneNumberId, whatsappAccessToken = creds.whatsappAccessToken,
                )
                call.response.header(HttpHeaders.ContentDisposition, ContentDisposition.Attachment.withParameter(ContentDisposition.Parameters.FileName, "${bot.name}_bot.zip").toString())
                call.respondBytes(zip, ContentType.Application.Zip)
            }
        }
        // External path - self-hosted generated code calls this, not a
        // Cedal user, so there's no user JWT to check - gated by the bot's
        // own secretToken instead (Authorization: Bearer <secretToken>,
        // same header shape as a JWT for a consistent client experience).
        // Cedal-hosted mode's own webhook routes below call
        // BotBrainService.converse directly instead of hairpinning through
        // this HTTP path, since they're already in the same process.
        post("/{id}/converse") {
            val id = call.parameters["id"]!!
            val secretToken = call.request.headers["Authorization"]?.removePrefix("Bearer ")?.trim()
            if (secretToken.isNullOrBlank() || !BotService.verifySecretToken(id, secretToken)) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid bot secret"))
                return@post
            }
            val req = call.receive<BotConverseRequest>()
            val reply = BotBrainService.converse(id, req.chatId, req.message)
            call.respond(HttpStatusCode.OK, BotConverseResponse(reply))
        }
        // Cedal-hosted Telegram (hostingMode "cedal") - registered via
        // TelegramBotService.registerWebhook on save. Telegram calls this
        // directly on every incoming message; not a Cedal user, so auth is
        // the secret_token Telegram echoes back on every call instead of a
        // JWT (set at registration time, verified against the bot's own
        // secretToken - see TelegramBotService.registerWebhook).
        post("/{id}/telegram-webhook") {
            val id = call.parameters["id"]!!
            val secretToken = call.request.headers["X-Telegram-Bot-Api-Secret-Token"]
            if (secretToken.isNullOrBlank() || !BotService.verifySecretToken(id, secretToken)) {
                call.respond(HttpStatusCode.Unauthorized)
                return@post
            }
            // Swallow errors rather than throwing - Telegram retries failed
            // webhook deliveries, and a bad message shouldn't cause runaway
            // retries. Always 200 back to Telegram once auth passes.
            runCatching {
                val update = call.receive<TelegramUpdate>()
                val message = update.message
                if (message?.text != null) {
                    val token = BotService.getTelegramToken(id)
                    if (token != null) {
                        val reply = BotBrainService.converse(id, message.chat.id.toString(), message.text)
                        TelegramBotService.sendMessage(token, message.chat.id.toString(), reply)
                    }
                }
            }
            call.respond(HttpStatusCode.OK)
        }
    }
    // Cedal-hosted WhatsApp (hostingMode "cedal") - top-level, not nested
    // under /bots/{id}, because Meta's WhatsApp Cloud API has exactly one
    // webhook URL per Meta App (set by hand in the developer dashboard),
    // not settable per-bot the way Telegram's setWebhook is. Incoming
    // messages are routed back to the owning bot by the phone_number_id
    // embedded in every payload instead - see BotService.findByWhatsappPhoneNumberId.
    route("/webhooks/whatsapp") {
        get {
            val mode = call.request.queryParameters["hub.mode"]
            val token = call.request.queryParameters["hub.verify_token"]
            val challenge = call.request.queryParameters["hub.challenge"]
            val expected = DotEnv.get("WHATSAPP_WEBHOOK_VERIFY_TOKEN")
            if (mode == "subscribe" && token != null && expected != null && token == expected) {
                call.respondText(challenge ?: "")
            } else {
                call.respond(HttpStatusCode.Forbidden)
            }
        }
        post {
            runCatching {
                val payload = call.receive<WhatsAppWebhookPayload>()
                payload.entry.forEach { entry ->
                    entry.changes.forEach { change ->
                        val phoneNumberId = change.value.metadata?.phone_number_id
                        if (phoneNumberId != null) {
                            change.value.messages.orEmpty().forEach { message ->
                                if (message.type == "text" && message.text != null) {
                                    val lookup = BotService.findByWhatsappPhoneNumberId(phoneNumberId)
                                    if (lookup != null && lookup.hostingMode == "cedal") {
                                        val reply = BotBrainService.converse(lookup.id, message.from, message.text.body)
                                        WhatsAppBotService.sendMessage(phoneNumberId, lookup.accessToken, message.from, reply)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            call.respond(HttpStatusCode.OK)
        }
    }
}

@Serializable
private data class TelegramUpdate(val message: TelegramMessage? = null)

@Serializable
private data class TelegramMessage(val chat: TelegramChat, val text: String? = null)

@Serializable
private data class TelegramChat(val id: Long)

@Serializable
private data class WhatsAppWebhookPayload(val entry: List<WhatsAppEntry> = emptyList())

@Serializable
private data class WhatsAppEntry(val changes: List<WhatsAppChange> = emptyList())

@Serializable
private data class WhatsAppChange(val value: WhatsAppValue)

@Serializable
private data class WhatsAppValue(val metadata: WhatsAppMetadata? = null, val messages: List<WhatsAppMessage>? = null)

@Serializable
private data class WhatsAppMetadata(val phone_number_id: String? = null)

@Serializable
private data class WhatsAppMessage(val from: String, val type: String, val text: WhatsAppText? = null)

@Serializable
private data class WhatsAppText(val body: String)
