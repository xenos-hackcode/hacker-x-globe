package com.xhacker.cedal.routes

import com.xhacker.cedal.models.ThemePackPurchaseResponse
import com.xhacker.cedal.services.ThemePackService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.themePackRoutes() {
    route("/theme-packs") {
        authenticate("auth-jwt") {
            get {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                call.respond(HttpStatusCode.OK, ThemePackService.listPacks(userId))
            }
            post("/{packId}/purchase") {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                val packId = call.parameters["packId"]!!
                val newBalance = ThemePackService.purchase(userId, packId)
                call.respond(HttpStatusCode.OK, ThemePackPurchaseResponse(newBalance))
            }
        }
    }
}
