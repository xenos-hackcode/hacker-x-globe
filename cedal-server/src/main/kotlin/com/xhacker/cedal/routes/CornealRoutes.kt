package com.xhacker.cedal.routes

import com.xhacker.cedal.models.CornealChatHistoryResponse
import com.xhacker.cedal.models.CornealChatMessageDto
import com.xhacker.cedal.models.CornealChatRequest
import com.xhacker.cedal.models.CornealChatResponse
import com.xhacker.cedal.models.EditAiMessageRequest
import com.xhacker.cedal.services.AiChatHistoryService
import com.xhacker.cedal.services.AuthException
import com.xhacker.cedal.services.CornealChatService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

private fun AiChatHistoryService.Turn.toCornealDto() = CornealChatMessageDto(id, role, content, createdAt, replyToId, editedAt, deleted, mediaUrl, mediaType, fileName)

fun Route.cornealRoutes() {
    route("/corneal") {
        authenticate("auth-jwt") {
            // Hydrates the chat with the real saved conversation on open -
            // see AiChatHistoryService, replaces the old session-only state.
            get("/chat") {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                val messages = CornealChatService.history(userId).map { it.toCornealDto() }
                call.respond(HttpStatusCode.OK, CornealChatHistoryResponse(messages))
            }
            post("/chat") {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                val req = call.receive<CornealChatRequest>()
                if (req.message.isBlank() && req.mediaUrl == null) throw AuthException("Say something first")
                val reply = CornealChatService.reply(userId, req.message, req.replyToId, req.chatContext, req.mediaUrl, req.mediaType, req.fileName)
                call.respond(HttpStatusCode.OK, CornealChatResponse(reply.toCornealDto()))
            }
            put("/chat/{id}") {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                val id = call.parameters["id"] ?: throw AuthException("Missing id")
                val req = call.receive<EditAiMessageRequest>()
                call.respond(HttpStatusCode.OK, AiChatHistoryService.editMessage(userId, id, req.content).toCornealDto())
            }
            delete("/chat/{id}") {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                val id = call.parameters["id"] ?: throw AuthException("Missing id")
                AiChatHistoryService.deleteMessage(userId, id)
                call.respond(HttpStatusCode.OK, mapOf("deleted" to true))
            }
        }
    }
}
