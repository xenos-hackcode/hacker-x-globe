package com.xhacker.cedal.routes

import com.xhacker.cedal.models.SubmitAppealRequest
import com.xhacker.cedal.services.AppealService
import com.xhacker.cedal.services.AuthException
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.appealRoutes() {
    // Unauthenticated - submitted from the full-screen gate a banned/
    // admin-cleared user sees (see AuthService.login's ACCOUNT_BANNED/
    // ACCOUNT_CLEARED sentinels), identified by email since that account
    // either can't log in or no longer exists.
    route("/appeals") {
        post {
            val req = call.receive<SubmitAppealRequest>()
            if (req.email.isBlank() || req.message.isBlank()) throw AuthException("Enter a message before submitting")
            AppealService.submit(req.email, req.reason, req.message)
            call.respond(HttpStatusCode.OK, mapOf("ok" to true))
        }
    }
    route("/admin/appeals") {
        authenticate("auth-jwt") {
            get {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                if (!com.xhacker.cedal.services.AdminService.isAdmin(userId)) throw AuthException("Admins only")
                call.respond(HttpStatusCode.OK, AppealService.list())
            }
            post("/{id}/dismiss") {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                if (!com.xhacker.cedal.services.AdminService.isAdmin(userId)) throw AuthException("Admins only")
                val id = call.parameters["id"] ?: throw AuthException("Missing id")
                AppealService.dismiss(id)
                call.respond(HttpStatusCode.OK, mapOf("ok" to true))
            }
        }
    }
}
