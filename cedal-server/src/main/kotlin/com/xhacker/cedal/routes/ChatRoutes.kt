package com.xhacker.cedal.routes

import com.xhacker.cedal.models.EditChatMessageRequest
import com.xhacker.cedal.models.ReactToMessageRequest
import com.xhacker.cedal.models.SendChatMessageRequest
import com.xhacker.cedal.models.VotePollRequest
import com.xhacker.cedal.services.AuthException
import com.xhacker.cedal.services.ChatService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.chatRoutes() {
    route("/chats") {
        authenticate("auth-jwt") {
            get {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                call.respond(HttpStatusCode.OK, ChatService.listConversations(userId))
            }
            get("/{friendId}/messages") {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                val friendId = call.parameters["friendId"] ?: throw AuthException("Missing friendId")
                call.respond(HttpStatusCode.OK, ChatService.getMessages(userId, friendId))
            }
            post("/{friendId}/messages") {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                val friendId = call.parameters["friendId"] ?: throw AuthException("Missing friendId")
                val req = call.receive<SendChatMessageRequest>()
                call.respond(
                    HttpStatusCode.OK,
                    ChatService.sendMessage(
                        userId, friendId, req.text, req.replyToId, req.isSticker,
                        req.mediaUrl, req.mediaType, req.fileName, req.viewOnce,
                        req.pollQuestion, req.pollOptions,
                    ),
                )
            }
            put("/{friendId}/messages/{messageId}") {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                val messageId = call.parameters["messageId"] ?: throw AuthException("Missing messageId")
                val req = call.receive<EditChatMessageRequest>()
                call.respond(HttpStatusCode.OK, ChatService.editMessage(userId, messageId, req.text))
            }
            delete("/{friendId}/messages/{messageId}") {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                val messageId = call.parameters["messageId"] ?: throw AuthException("Missing messageId")
                ChatService.deleteMessage(userId, messageId)
                call.respond(HttpStatusCode.OK, mapOf("deleted" to true))
            }
            post("/{friendId}/messages/{messageId}/react") {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                val messageId = call.parameters["messageId"] ?: throw AuthException("Missing messageId")
                val req = call.receive<ReactToMessageRequest>()
                call.respond(HttpStatusCode.OK, ChatService.reactToMessage(userId, messageId, req.emoji))
            }
            post("/{friendId}/messages/{messageId}/reveal") {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                val messageId = call.parameters["messageId"] ?: throw AuthException("Missing messageId")
                call.respond(HttpStatusCode.OK, ChatService.revealMessage(userId, messageId))
            }
            post("/{friendId}/messages/{messageId}/vote") {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                val messageId = call.parameters["messageId"] ?: throw AuthException("Missing messageId")
                val req = call.receive<VotePollRequest>()
                call.respond(HttpStatusCode.OK, ChatService.voteInPoll(userId, messageId, req.optionIndex))
            }
            delete("/{friendId}") {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                val friendId = call.parameters["friendId"] ?: throw AuthException("Missing friendId")
                ChatService.deleteConversation(userId, friendId)
                call.respond(HttpStatusCode.OK, mapOf("deleted" to true))
            }
        }
    }
}
