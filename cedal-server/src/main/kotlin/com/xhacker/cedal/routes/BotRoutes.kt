package com.xhacker.cedal.routes

import com.xhacker.cedal.models.BotCreate
import com.xhacker.cedal.models.BotUpdate
import com.xhacker.cedal.services.BotService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

// Member > More > Bots (MemberBotsScreen.kt) - Round 1 CRUD only, see
// BotService's own doc comment for what's deliberately not here yet.
fun Route.botRoutes() {
    route("/bots") {
        authenticate("auth-jwt") {
            get {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                call.respond(HttpStatusCode.OK, BotService.list(userId))
            }
            post {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                val req = call.receive<BotCreate>()
                call.respond(HttpStatusCode.Created, BotService.create(userId, req))
            }
            get("/{id}") {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                val bot = BotService.get(userId, call.parameters["id"]!!)
                if (bot == null) call.respond(HttpStatusCode.NotFound, mapOf("error" to "Bot not found"))
                else call.respond(HttpStatusCode.OK, bot)
            }
            put("/{id}") {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                val req = call.receive<BotUpdate>()
                val bot = BotService.update(userId, call.parameters["id"]!!, req)
                if (bot == null) call.respond(HttpStatusCode.NotFound, mapOf("error" to "Bot not found"))
                else call.respond(HttpStatusCode.OK, bot)
            }
            delete("/{id}") {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                val deleted = BotService.delete(userId, call.parameters["id"]!!)
                if (!deleted) call.respond(HttpStatusCode.NotFound, mapOf("error" to "Bot not found"))
                else call.respond(HttpStatusCode.OK, mapOf("deleted" to true))
            }
        }
    }
}
