package com.xhacker.cedal.routes

import com.xhacker.cedal.models.DailyTaskCompleteResponse
import com.xhacker.cedal.models.DailyTaskResponse
import com.xhacker.cedal.services.AuthException
import com.xhacker.cedal.services.DailyTaskService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

private val VALID_AREAS = setOf("invest", "arc")

fun Route.dailyTaskRoutes() {
    route("/daily-task") {
        authenticate("auth-jwt") {
            get("/{area}") {
                val area = call.parameters["area"]?.takeIf { it in VALID_AREAS } ?: throw AuthException("Unknown area")
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                val task = DailyTaskService.today(area, userId)
                call.respond(HttpStatusCode.OK, DailyTaskResponse(task.title, task.description, task.expReward, task.completed))
            }
            post("/{area}/complete") {
                val area = call.parameters["area"]?.takeIf { it in VALID_AREAS } ?: throw AuthException("Unknown area")
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                val newExp = DailyTaskService.complete(area, userId)
                call.respond(HttpStatusCode.OK, DailyTaskCompleteResponse(newExp))
            }
        }
    }
}
