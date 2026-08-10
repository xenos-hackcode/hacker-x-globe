package com.xhacker.cedal.routes

import com.xhacker.cedal.models.BotConverseRequest
import com.xhacker.cedal.models.BotConverseResponse
import com.xhacker.cedal.models.BotCreate
import com.xhacker.cedal.models.BotTestChatRequest
import com.xhacker.cedal.models.BotTurnDto
import com.xhacker.cedal.models.BotUpdate
import com.xhacker.cedal.services.AuthException
import com.xhacker.cedal.services.BotBrainService
import com.xhacker.cedal.services.BotService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

// Member > More > Bots (MemberBotsScreen.kt) - Round 1 CRUD, plus Round 2's
// brain endpoint (2026-08-10) - see BotBrainService's own doc comment for
// how the JWT-gated /test-chat and secretToken-gated /converse paths
// differ.
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
            // (that was just the external /converse path below, for Round
            // 3's not-yet-built generated code), added so a bot can
            // actually be tried before Round 3 exists. Keyed by the
            // owner's own userId as chatId, so this never collides with a
            // real external conversation once one exists.
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
        }
        // External path - Round 3's self-hosted generated code calls this,
        // not a Cedal user, so there's no user JWT to check - gated by the
        // bot's own secretToken instead (Authorization: Bearer <secretToken>,
        // same header shape as a JWT for a consistent client experience).
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
    }
}
