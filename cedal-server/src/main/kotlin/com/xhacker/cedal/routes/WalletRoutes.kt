package com.xhacker.cedal.routes

import com.xhacker.cedal.models.WalletBalanceResponse
import com.xhacker.cedal.models.WalletSendRequest
import com.xhacker.cedal.services.WalletService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.walletRoutes() {
    route("/wallet") {
        authenticate("auth-jwt") {
            get("/balance") {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                call.respond(HttpStatusCode.OK, WalletBalanceResponse(WalletService.getBalance(userId)))
            }
            post("/send") {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                val req = call.receive<WalletSendRequest>()
                val newBalance = WalletService.send(userId, req.toUserId, req.amount)
                call.respond(HttpStatusCode.OK, WalletBalanceResponse(newBalance))
            }
            get("/transactions") {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                val filter = call.request.queryParameters["filter"]
                call.respond(HttpStatusCode.OK, WalletService.listTransactions(userId, filter))
            }
        }
    }
}
