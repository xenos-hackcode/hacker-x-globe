package com.xhacker.cedal.routes

import com.xhacker.cedal.models.*
import com.xhacker.cedal.services.AuthService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.authRoutes() {
    route("/auth") {
        post("/signup") {
            val req = call.receive<SignupRequest>()
            call.respond(HttpStatusCode.Created, AuthService.signup(req))
        }
        post("/verify-email") {
            val req = call.receive<VerifyEmailRequest>()
            AuthService.verifyEmail(req)
            call.respond(HttpStatusCode.OK, mapOf("verified" to true))
        }
        post("/login") {
            val req = call.receive<LoginRequest>()
            call.respond(HttpStatusCode.OK, AuthService.login(req))
        }
        post("/login/2fa") {
            val req = call.receive<TwoFactorLoginConfirmRequest>()
            call.respond(HttpStatusCode.OK, AuthService.confirmLoginTwoFactor(req))
        }
        post("/refresh") {
            val req = call.receive<RefreshRequest>()
            call.respond(HttpStatusCode.OK, AuthService.refresh(req.refreshToken))
        }
        post("/forgot-password") {
            val req = call.receive<ForgotPasswordRequest>()
            AuthService.forgotPassword(req)
            call.respond(HttpStatusCode.OK, mapOf("sent" to true))
        }
        post("/reset-password") {
            val req = call.receive<ResetPasswordRequest>()
            AuthService.resetPassword(req)
            call.respond(HttpStatusCode.OK, mapOf("reset" to true))
        }
        post("/node-password/verify") {
            val req = call.receive<NodePasswordVerifyRequest>()
            call.respond(HttpStatusCode.OK, AuthService.verifyNodePassword(req))
        }
        post("/passcode") {
            val req = call.receive<CreatePasscodeRequest>()
            AuthService.createPasscode(req)
            call.respond(HttpStatusCode.OK, mapOf("saved" to true))
        }
        post("/logout") {
            val req = call.receive<LogoutRequest>()
            AuthService.logout(req.refreshToken)
            call.respond(HttpStatusCode.OK, mapOf("loggedOut" to true))
        }
    }
}
