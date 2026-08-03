package com.xhacker.cedal.routes

import com.xhacker.cedal.models.SaveMessageRequest
import com.xhacker.cedal.services.AuthException
import com.xhacker.cedal.services.SavedMessagesService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.savedMessagesRoutes() {
    route("/saved-messages") {
        authenticate("auth-jwt") {
            post {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                val req = call.receive<SaveMessageRequest>()
                call.respond(HttpStatusCode.OK, SavedMessagesService.save(userId, req.sourceLabel, req.text, req.mediaUrl, req.mediaType, req.fileName))
            }
            get {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                call.respond(HttpStatusCode.OK, SavedMessagesService.list(userId))
            }
            delete("/{id}") {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                val id = call.parameters["id"] ?: throw AuthException("Missing id")
                SavedMessagesService.delete(userId, id)
                call.respond(HttpStatusCode.OK, mapOf("ok" to true))
            }
        }
    }
}
