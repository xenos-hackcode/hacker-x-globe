package com.xhacker.cedal.routes

import com.xhacker.cedal.models.GuiSessionRequest
import com.xhacker.cedal.services.AuthException
import com.xhacker.cedal.services.GuiSessionService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.guiSessionRoutes() {
    route("/code/gui-session") {
        authenticate("auth-jwt") {
            // Synchronous, unlike /code/android-build - gui-runner's own
            // /session/start already waits for the display/VNC chain to
            // come up (a few seconds) before responding, so there's nothing
            // to poll for; the caller gets viewUrl back directly.
            post {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                val req = call.receive<GuiSessionRequest>()
                if (req.code.isBlank()) throw AuthException("Write some Python first")
                val job = GuiSessionService.startSession(userId, req.code)
                call.respond(HttpStatusCode.OK, job)
            }
            get("/{jobId}") {
                val jobId = call.parameters["jobId"] ?: throw AuthException("Missing jobId")
                call.respond(HttpStatusCode.OK, GuiSessionService.getJob(jobId))
            }
            post("/{jobId}/stop") {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                val jobId = call.parameters["jobId"] ?: throw AuthException("Missing jobId")
                GuiSessionService.stopSession(userId, jobId)
                call.respond(HttpStatusCode.OK, mapOf("ok" to true))
            }
        }
    }
}
