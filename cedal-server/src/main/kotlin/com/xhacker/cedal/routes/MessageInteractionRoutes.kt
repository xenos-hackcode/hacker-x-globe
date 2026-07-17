package com.xhacker.cedal.routes

import com.xhacker.cedal.services.AdminService
import com.xhacker.cedal.services.AuthException
import com.xhacker.cedal.services.MessageInteractionService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

@Serializable
private data class PinBody(val chatType: String, val messageId: String, val chatKey: String? = null, val messageText: String)

@Serializable
private data class ReportBody(val chatType: String, val messageId: String, val messageText: String)

fun Route.messageInteractionRoutes() {
    route("/messages") {
        authenticate("auth-jwt") {
            get("/pins") {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                call.respond(HttpStatusCode.OK, MessageInteractionService.listPins(userId))
            }
            post("/pin") {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                val req = call.receive<PinBody>()
                call.respond(HttpStatusCode.OK, MessageInteractionService.pin(userId, req.chatType, req.messageId, req.chatKey, req.messageText))
            }
            post("/pins/{id}/unpin") {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                val id = call.parameters["id"] ?: throw AuthException("Missing id")
                MessageInteractionService.unpin(userId, id)
                call.respond(HttpStatusCode.OK, mapOf("ok" to true))
            }
            post("/report") {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                val req = call.receive<ReportBody>()
                call.respond(HttpStatusCode.OK, MessageInteractionService.report(userId, req.chatType, req.messageId, req.messageText))
            }
        }
    }

    route("/admin/message-reports") {
        authenticate("auth-jwt") {
            get {
                requireAdmin(call)
                call.respond(HttpStatusCode.OK, MessageInteractionService.listReports())
            }
            post("/{id}/dismiss") {
                requireAdmin(call)
                val id = call.parameters["id"] ?: throw AuthException("Missing id")
                MessageInteractionService.dismissReport(id)
                call.respond(HttpStatusCode.OK, mapOf("ok" to true))
            }
        }
    }
}

// Same admin-gate shape as AiChangeRequestRoutes' requireAdmin - throws
// (never call.respond) so the global AuthException -> 400 StatusPages
// handler is the only thing that ever writes a response for this.
private fun requireAdmin(call: ApplicationCall) {
    val userId = call.principal<JWTPrincipal>()!!.payload.subject
    if (!AdminService.isAdmin(userId)) {
        throw AuthException("Admins only")
    }
}
