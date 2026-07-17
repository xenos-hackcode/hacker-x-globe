package com.xhacker.cedal.routes

import com.xhacker.cedal.models.AndroidBuildCallbackRequest
import com.xhacker.cedal.models.AndroidBuildJob
import com.xhacker.cedal.models.AndroidBuildRequest
import com.xhacker.cedal.services.AndroidBuildService
import com.xhacker.cedal.services.AuthException
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

fun Route.androidBuildRoutes() {
    route("/code/android-build") {
        authenticate("auth-jwt") {
            post {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                val req = call.receive<AndroidBuildRequest>()
                if (req.code.isBlank()) throw AuthException("Write some Kotlin first")

                val jobId = AndroidBuildService.requestBuild(userId)
                // Builds take minutes - don't block this response on it.
                CoroutineScope(Dispatchers.IO).launch {
                    AndroidBuildService.triggerBuild(jobId, userId, req.code)
                }
                val job: AndroidBuildJob = AndroidBuildService.getJob(jobId)
                call.respond(HttpStatusCode.Accepted, job)
            }
            get("/{jobId}") {
                val jobId = call.parameters["jobId"] ?: throw AuthException("Missing jobId")
                call.respond(HttpStatusCode.OK, AndroidBuildService.getJob(jobId))
            }
        }

        // android-builder calls this back on completion - it isn't a logged-in
        // user, so it's gated by the shared secret instead of a user JWT.
        post("/{jobId}/callback") {
            val expected = "Bearer ${System.getenv("ANDROID_BUILDER_SECRET") ?: ""}"
            val header = call.request.headers["Authorization"] ?: ""
            if (header.isEmpty() || header != expected) {
                call.respond(HttpStatusCode.Unauthorized)
                return@post
            }
            val jobId = call.parameters["jobId"] ?: throw AuthException("Missing jobId")
            val req = call.receive<AndroidBuildCallbackRequest>()
            AndroidBuildService.updateStatus(jobId, req.status, req.downloadUrl, req.errorMessage)
            call.respond(HttpStatusCode.OK, mapOf("ok" to true))
        }
    }
}
