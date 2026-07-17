package com.xhacker.cedal.routes

import com.xhacker.cedal.models.CompleteLessonRequest
import com.xhacker.cedal.models.CompleteLessonResponse
import com.xhacker.cedal.services.LessonService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.learnRoutes() {
    route("/learn") {
        authenticate("auth-jwt") {
            // Idempotent per (user, lessonId) - see LessonService. Called
            // from Invest > Learn only when a lesson's checkbox is newly
            // marked done, not when it's un-marked.
            post("/complete-lesson") {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                val req = call.receive<CompleteLessonRequest>()
                val newExp = LessonService.completeLesson(userId, req.lessonId)
                call.respond(HttpStatusCode.OK, CompleteLessonResponse(newExp))
            }
        }
    }
}
