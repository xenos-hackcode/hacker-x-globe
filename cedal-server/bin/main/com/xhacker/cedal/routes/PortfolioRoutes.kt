package com.xhacker.cedal.routes

import com.xhacker.cedal.models.PortfolioHoldingItem
import com.xhacker.cedal.models.PortfolioResponse
import com.xhacker.cedal.models.TradeRequest
import com.xhacker.cedal.models.WatchlistAddRequest
import com.xhacker.cedal.models.WatchlistItem
import com.xhacker.cedal.services.MarketDataService
import com.xhacker.cedal.services.PortfolioService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.marketRoutes() {
    route("/market") {
        authenticate("auth-jwt") {
            get("/crypto") {
                call.respond(HttpStatusCode.OK, MarketDataService.listMarkets())
            }
            get("/crypto/{id}") {
                call.respond(HttpStatusCode.OK, MarketDataService.getDetail(call.parameters["id"]!!))
            }
        }
    }
}

fun Route.portfolioRoutes() {
    route("/portfolio") {
        authenticate("auth-jwt") {
            get {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                val cash = PortfolioService.getCashBalance(userId)
                val holdings = PortfolioService.getHoldings(userId)
                val markets = MarketDataService.listMarkets().associateBy { it.id }

                val holdingItems = holdings.map { holding ->
                    val market = markets[holding.symbol]
                    val currentPrice = market?.price ?: MarketDataService.getPrice(holding.symbol)
                    val currentValue = holding.quantity * currentPrice
                    val costBasisTotal = holding.quantity * holding.avgCostBasis
                    val gainLossValue = currentValue - costBasisTotal
                    PortfolioHoldingItem(
                        symbol = holding.symbol,
                        name = market?.name ?: holding.symbol,
                        quantity = holding.quantity,
                        avgCostBasis = holding.avgCostBasis,
                        currentPrice = currentPrice,
                        currentValue = currentValue,
                        gainLossValue = gainLossValue,
                        gainLossPercent = if (costBasisTotal > 0) (gainLossValue / costBasisTotal) * 100 else 0.0,
                    )
                }
                val totalHoldingsValue = holdingItems.sumOf { it.currentValue }
                call.respond(
                    HttpStatusCode.OK,
                    PortfolioResponse(cash, holdingItems, totalHoldingsValue, cash + totalHoldingsValue),
                )
            }
            post("/buy") {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                val req = call.receive<TradeRequest>()
                val price = MarketDataService.getPrice(req.symbol)
                PortfolioService.buy(userId, req.symbol, req.quantity, price)
                call.respond(HttpStatusCode.OK, mapOf("bought" to true))
            }
            post("/sell") {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                val req = call.receive<TradeRequest>()
                val price = MarketDataService.getPrice(req.symbol)
                PortfolioService.sell(userId, req.symbol, req.quantity, price)
                call.respond(HttpStatusCode.OK, mapOf("sold" to true))
            }
            get("/transactions") {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                call.respond(HttpStatusCode.OK, PortfolioService.listTransactions(userId))
            }
            get("/watchlist") {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                val symbols = PortfolioService.listWatchlist(userId)
                val markets = MarketDataService.listMarkets().associateBy { it.id }
                val items = symbols.mapNotNull { sym -> markets[sym]?.let { WatchlistItem(sym, it.name, it.price, it.changePercent24h) } }
                call.respond(HttpStatusCode.OK, items)
            }
            post("/watchlist") {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                val req = call.receive<WatchlistAddRequest>()
                PortfolioService.addToWatchlist(userId, req.symbol)
                call.respond(HttpStatusCode.OK, mapOf("added" to true))
            }
            delete("/watchlist/{symbol}") {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                PortfolioService.removeFromWatchlist(userId, call.parameters["symbol"]!!)
                call.respond(HttpStatusCode.OK, mapOf("removed" to true))
            }
        }
    }
}
