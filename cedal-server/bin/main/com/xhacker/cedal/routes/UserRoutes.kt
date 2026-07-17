package com.xhacker.cedal.routes

import com.xhacker.cedal.models.LinkEmailRequest
import com.xhacker.cedal.models.TermsUpdateRequest
import com.xhacker.cedal.models.TwoFactorConfirmRequest
import com.xhacker.cedal.models.UpdatePasscodeRequest
import com.xhacker.cedal.models.UpdateProfileRequest
import com.xhacker.cedal.services.AccountService
import com.xhacker.cedal.services.AuthService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.userRoutes() {
    route("/users") {
        authenticate("auth-jwt") {
            get("/{id}") {
                val id = call.parameters["id"]!!
                call.respond(HttpStatusCode.OK, AuthService.getProfile(id))
            }
            put("/{id}") {
                val id = call.parameters["id"]!!
                val principal = call.principal<JWTPrincipal>()!!
                if (principal.payload.subject != id) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Cannot edit another user's profile"))
                    return@put
                }
                val req = call.receive<UpdateProfileRequest>()
                call.respond(HttpStatusCode.OK, AuthService.updateProfile(id, req))
            }
            put("/{id}/terms") {
                val id = call.parameters["id"]!!
                val principal = call.principal<JWTPrincipal>()!!
                if (principal.payload.subject != id) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Cannot edit another user's profile"))
                    return@put
                }
                val req = call.receive<TermsUpdateRequest>()
                call.respond(HttpStatusCode.OK, AuthService.updateTermsAcceptance(id, req.version))
            }
            put("/{id}/passcode") {
                val id = call.parameters["id"]!!
                val principal = call.principal<JWTPrincipal>()!!
                if (principal.payload.subject != id) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Cannot edit another user's profile"))
                    return@put
                }
                val req = call.receive<UpdatePasscodeRequest>()
                AuthService.updatePasscode(id, req.code)
                call.respond(HttpStatusCode.OK, mapOf("updated" to true))
            }
            put("/{id}/link-email") {
                val id = call.parameters["id"]!!
                val principal = call.principal<JWTPrincipal>()!!
                if (principal.payload.subject != id) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Cannot edit another user's profile"))
                    return@put
                }
                val req = call.receive<LinkEmailRequest>()
                call.respond(HttpStatusCode.OK, AuthService.linkGuestToEmail(id, req.email, req.password))
            }
            post("/{id}/2fa/request") {
                val id = call.parameters["id"]!!
                val principal = call.principal<JWTPrincipal>()!!
                if (principal.payload.subject != id) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Cannot edit another user's profile"))
                    return@post
                }
                val code = AuthService.requestTwoFactorSetup(id)
                call.respond(HttpStatusCode.OK, mapOf("devVerificationCode" to code))
            }
            post("/{id}/2fa/confirm") {
                val id = call.parameters["id"]!!
                val principal = call.principal<JWTPrincipal>()!!
                if (principal.payload.subject != id) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Cannot edit another user's profile"))
                    return@post
                }
                val req = call.receive<TwoFactorConfirmRequest>()
                call.respond(HttpStatusCode.OK, AuthService.confirmTwoFactorSetup(id, req.code))
            }
            post("/{id}/2fa/disable") {
                val id = call.parameters["id"]!!
                val principal = call.principal<JWTPrincipal>()!!
                if (principal.payload.subject != id) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Cannot edit another user's profile"))
                    return@post
                }
                call.respond(HttpStatusCode.OK, AuthService.disableTwoFactor(id))
            }
            delete("/{id}") {
                val id = call.parameters["id"]!!
                val principal = call.principal<JWTPrincipal>()!!
                if (principal.payload.subject != id) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Cannot delete another user's account"))
                    return@delete
                }
                AccountService.deleteAccount(id)
                call.respond(HttpStatusCode.OK, mapOf("deleted" to true))
            }
        }
    }
}
