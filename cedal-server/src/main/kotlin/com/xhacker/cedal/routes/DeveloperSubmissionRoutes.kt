package com.xhacker.cedal.routes

import com.xhacker.cedal.models.DenyDeveloperSubmissionRequest
import com.xhacker.cedal.models.SubmitDeveloperPatchRequest
import com.xhacker.cedal.services.AdminService
import com.xhacker.cedal.services.AuthException
import com.xhacker.cedal.services.DeveloperSubmissionService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.developerSubmissionRoutes() {
    route("/developer/submissions") {
        authenticate("auth-jwt") {
            post {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                val req = call.receive<SubmitDeveloperPatchRequest>()
                if (req.code.isBlank()) throw AuthException("Nothing to submit")
                if (req.targetFilePath.isBlank()) throw AuthException("Missing target file path")
                call.respond(HttpStatusCode.Accepted, DeveloperSubmissionService.submit(userId, req))
            }
            // Own submissions only - see DeveloperSubmissionService's own
            // doc comment for why this (polled) status IS the private
            // notification mechanism, not a generic Notifications row.
            get {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                call.respond(HttpStatusCode.OK, DeveloperSubmissionService.listMine(userId))
            }
            get("/{id}") {
                val id = call.parameters["id"] ?: throw AuthException("Missing id")
                val dto = DeveloperSubmissionService.get(id) ?: throw AuthException("Submission not found")
                call.respond(HttpStatusCode.OK, dto)
            }
        }
    }

    route("/admin/developer-submissions") {
        authenticate("auth-jwt") {
            get {
                requireAdmin(call)
                call.respond(HttpStatusCode.OK, DeveloperSubmissionService.listPendingApprovals())
            }
            post("/{id}/approve") {
                requireAdmin(call)
                val id = call.parameters["id"] ?: throw AuthException("Missing id")
                call.respond(HttpStatusCode.OK, DeveloperSubmissionService.approve(id))
            }
            post("/{id}/deny") {
                requireAdmin(call)
                val id = call.parameters["id"] ?: throw AuthException("Missing id")
                val req = call.receive<DenyDeveloperSubmissionRequest>()
                if (req.reason.isBlank()) throw AuthException("A reason is required")
                call.respond(HttpStatusCode.OK, DeveloperSubmissionService.deny(id, req.reason))
            }
        }
    }
}

private fun requireAdmin(call: ApplicationCall) {
    val userId = call.principal<JWTPrincipal>()!!.payload.subject
    if (!AdminService.isAdmin(userId)) {
        throw AuthException("Admins only")
    }
}
