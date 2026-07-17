package com.xhacker.cedal.routes

import com.xhacker.cedal.models.CreateFeedPostRequest
import com.xhacker.cedal.models.ReactToFeedPostRequest
import com.xhacker.cedal.services.SystemFeedService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.systemFeedRoutes() {
    route("/feed") {
        authenticate("auth-jwt") {
            get {
                // Opening the feed is what marks it read - drives the
                // unread-count badge back on the Chats list, same pattern as
                // ChatService.getMessages() marking a friend thread read.
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                SystemFeedService.markSeen(userId)
                call.respond(HttpStatusCode.OK, SystemFeedService.listPosts())
            }
            post {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                val req = call.receive<CreateFeedPostRequest>()
                val post = SystemFeedService.createPost(userId, req.text)
                call.respond(HttpStatusCode.OK, post)
            }
            post("/{postId}/react") {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                val postId = call.parameters["postId"]!!
                val req = call.receive<ReactToFeedPostRequest>()
                val reactions = SystemFeedService.reactToPost(userId, postId, req.emoji)
                call.respond(HttpStatusCode.OK, reactions)
            }
        }
    }
}
