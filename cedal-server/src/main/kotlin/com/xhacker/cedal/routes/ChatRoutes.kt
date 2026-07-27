package com.xhacker.cedal.routes

import com.xhacker.cedal.models.BulkChatActionRequest
import com.xhacker.cedal.models.EditChatMessageRequest
import com.xhacker.cedal.models.MarkReadRequest
import com.xhacker.cedal.models.ReactToMessageRequest
import com.xhacker.cedal.models.ReportUserRequest
import com.xhacker.cedal.models.SendChatMessageRequest
import com.xhacker.cedal.models.VotePollRequest
import com.xhacker.cedal.services.AuthException
import com.xhacker.cedal.services.ChatService
import com.xhacker.cedal.services.TypingService
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
            // "Archived" row pinned above the chat list - see
            // ChatService.listArchivedConversations.
            get("/archived") {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                call.respond(HttpStatusCode.OK, ChatService.listArchivedConversations(userId))
            }
            // Client gates this behind biometric/passcode before calling it
            // - see ChatService.listHiddenConversations' doc comment.
            get("/hidden") {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                call.respond(HttpStatusCode.OK, ChatService.listHiddenConversations(userId))
            }
            get("/{friendId}/messages") {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                val friendId = call.parameters["friendId"] ?: throw AuthException("Missing friendId")
                call.respond(HttpStatusCode.OK, ChatService.getMessages(userId, friendId))
            }
            // Chat thread ⋮ menu's Pin item - see ChatService.getPinnedState.
            get("/{friendId}/pinned") {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                val friendId = call.parameters["friendId"] ?: throw AuthException("Missing friendId")
                call.respond(HttpStatusCode.OK, mapOf("pinned" to ChatService.getPinnedState(userId, friendId)))
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
                        req.viewOnceMode, req.viewOnceDurationMs, req.viewOnceMaxViews,
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
            // Chat list long-press multi-select menu (Delete/Archive/Clear
            // Data/Pin/Mute/Favorite/Lock/Hide) - one endpoint applying the
            // same action to every selected friendId. See ChatService.bulkAction.
            post("/bulk-action") {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                val req = call.receive<BulkChatActionRequest>()
                ChatService.bulkAction(userId, req.friendIds, req.action)
                call.respond(HttpStatusCode.OK, mapOf("ok" to true))
            }
            // WhatsApp-style "is typing…" - ping while actively composing,
            // poll to see who's typing to you. See TypingService.
            post("/{friendId}/typing") {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                val friendId = call.parameters["friendId"] ?: throw AuthException("Missing friendId")
                TypingService.ping(userId, friendId)
                call.respond(HttpStatusCode.OK, mapOf("ok" to true))
            }
            get("/typing") {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                call.respond(HttpStatusCode.OK, TypingService.typingToMe(userId))
            }
            // Telegram-style incremental read-marking - called as messages
            // actually scroll into view, not just once on thread-open. See
            // ChatService.markRead.
            post("/{friendId}/mark-read") {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                val friendId = call.parameters["friendId"] ?: throw AuthException("Missing friendId")
                val req = call.receive<MarkReadRequest>()
                ChatService.markRead(userId, friendId, req.upToMessageId)
                call.respond(HttpStatusCode.OK, mapOf("ok" to true))
            }
            // Fires when the client leaves this chat thread or the app
            // backgrounds - see ChatService.purgeConsumedViewOnce.
            post("/{friendId}/purge-consumed-view-once") {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                val friendId = call.parameters["friendId"] ?: throw AuthException("Missing friendId")
                ChatService.purgeConsumedViewOnce(userId, friendId)
                call.respond(HttpStatusCode.OK, mapOf("ok" to true))
            }
            // Chat thread ⋮ menu - Block/Report. See ChatService.setBlocked/
            // reportUser.
            post("/{friendId}/block") {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                val friendId = call.parameters["friendId"] ?: throw AuthException("Missing friendId")
                ChatService.setBlocked(userId, friendId, true)
                call.respond(HttpStatusCode.OK, mapOf("ok" to true))
            }
            post("/{friendId}/unblock") {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                val friendId = call.parameters["friendId"] ?: throw AuthException("Missing friendId")
                ChatService.setBlocked(userId, friendId, false)
                call.respond(HttpStatusCode.OK, mapOf("ok" to true))
            }
            get("/{friendId}/blocked") {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                val friendId = call.parameters["friendId"] ?: throw AuthException("Missing friendId")
                call.respond(HttpStatusCode.OK, mapOf("blocked" to ChatService.isBlocked(userId, friendId)))
            }
            post("/{friendId}/report") {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                val friendId = call.parameters["friendId"] ?: throw AuthException("Missing friendId")
                val req = call.receive<ReportUserRequest>()
                ChatService.reportUser(userId, friendId, req.reason, req.mediaUrl, req.mediaType, req.fileName)
                call.respond(HttpStatusCode.OK, mapOf("ok" to true))
            }
        }
    }

    // Admin Review (chat list > More, admin-only) - user-level reports.
    route("/admin/user-reports") {
        authenticate("auth-jwt") {
            get {
                requireAdmin(call)
                call.respond(HttpStatusCode.OK, ChatService.listUserReports())
            }
            post("/{id}/dismiss") {
                requireAdmin(call)
                val id = call.parameters["id"] ?: throw AuthException("Missing id")
                ChatService.dismissUserReport(id)
                call.respond(HttpStatusCode.OK, mapOf("ok" to true))
            }
        }
    }
}

// Same admin-gate shape as AiChangeRequestRoutes'/MessageInteractionRoutes'
// own private requireAdmin - throws (never call.respond) so the global
// AuthException -> 400 StatusPages handler is the only thing that writes a
// response for this.
private fun requireAdmin(call: ApplicationCall) {
    val userId = call.principal<JWTPrincipal>()!!.payload.subject
    if (!com.xhacker.cedal.services.AdminService.isAdmin(userId)) {
        throw AuthException("Admins only")
    }
}
