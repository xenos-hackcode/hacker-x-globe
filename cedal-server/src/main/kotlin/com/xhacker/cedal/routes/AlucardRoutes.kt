package com.xhacker.cedal.routes

import com.xhacker.cedal.models.AlucardChatHistoryResponse
import com.xhacker.cedal.models.AlucardChatMessageDto
import com.xhacker.cedal.models.AlucardChatRequest
import com.xhacker.cedal.models.AlucardChatResponse
import com.xhacker.cedal.models.EditAiMessageRequest
import com.xhacker.cedal.services.AiChatHistoryService
import com.xhacker.cedal.services.AlucardChatService
import com.xhacker.cedal.services.AuthException
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

private fun AiChatHistoryService.Turn.toAlucardDto() = AlucardChatMessageDto(id, role, content, createdAt, replyToId, editedAt, deleted, mediaUrl, mediaType, fileName)

fun Route.alucardRoutes() {
    route("/alucard") {
        authenticate("auth-jwt") {
            // Developer Mode's "Alucard" chat - hydrates with the real saved
            // conversation on open, same pattern as Corneal/Arc (see
            // AiChatHistoryService).
            get("/chat") {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                val messages = AlucardChatService.history(userId).map { it.toAlucardDto() }
                call.respond(HttpStatusCode.OK, AlucardChatHistoryResponse(messages))
            }
            post("/chat") {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                val req = call.receive<AlucardChatRequest>()
                if (req.message.isBlank() && req.mediaUrl == null) throw AuthException("Say something first")
                val reply = AlucardChatService.reply(userId, req.message, req.replyToId, req.mediaUrl, req.mediaType, req.fileName)
                call.respond(HttpStatusCode.OK, AlucardChatResponse(reply.toAlucardDto()))
            }
            put("/chat/{id}") {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                val id = call.parameters["id"] ?: throw AuthException("Missing id")
                val req = call.receive<EditAiMessageRequest>()
                call.respond(HttpStatusCode.OK, AiChatHistoryService.editMessage(userId, id, req.content).toAlucardDto())
            }
            delete("/chat/{id}") {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                val id = call.parameters["id"] ?: throw AuthException("Missing id")
                AiChatHistoryService.deleteMessage(userId, id)
                call.respond(HttpStatusCode.OK, mapOf("deleted" to true))
            }
            delete("/history") {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                AiChatHistoryService.deleteAllHistory(userId, "alucard")
                call.respond(HttpStatusCode.OK, mapOf("deleted" to true))
            }
        }
    }
}
