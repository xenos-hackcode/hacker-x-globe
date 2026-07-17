package com.xhacker.cedal.routes

import com.xhacker.cedal.models.TradeCreateRequest
import com.xhacker.cedal.models.WalletBalanceResponse
import com.xhacker.cedal.services.TradeService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.tradeRoutes() {
    route("/trades") {
        authenticate("auth-jwt") {
            post {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                val req = call.receive<TradeCreateRequest>()
                TradeService.createTrade(userId, req.amount, req.plea)
                call.respond(HttpStatusCode.OK, mapOf("posted" to true))
            }
        }
    }
    route("/notifications") {
        authenticate("auth-jwt") {
            get {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                call.respond(HttpStatusCode.OK, TradeService.listNotifications(userId))
            }
            post("/{id}/credit") {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                val newBalance = TradeService.creditNotification(userId, call.parameters["id"]!!)
                call.respond(HttpStatusCode.OK, WalletBalanceResponse(newBalance))
            }
            delete("/{id}") {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                TradeService.deleteNotification(userId, call.parameters["id"]!!)
                call.respond(HttpStatusCode.OK, mapOf("deleted" to true))
            }
        }
    }
}
