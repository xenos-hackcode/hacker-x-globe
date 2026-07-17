package com.xhacker.cedal.services

import com.xhacker.cedal.db.PortfolioHoldings
import com.xhacker.cedal.db.PortfolioTransactions
import com.xhacker.cedal.db.Users
import com.xhacker.cedal.db.Watchlist
import com.xhacker.cedal.models.PortfolioTransactionItem
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.util.UUID

// A holding row, exposed as a simple data holder so route handlers can
// enrich it with a live price from MarketDataService (a suspend call that
// can't happen inside an Exposed `transaction {}` block).
data class HoldingRow(val symbol: String, val quantity: Double, val avgCostBasis: Double)

// Simulated buy/sell against Users.virtualCashBalance, priced against real
// market data fetched by the caller (route handlers call MarketDataService
// first, then pass the price in here) — keeps suspend network calls out of
// Exposed's synchronous transaction blocks.
object PortfolioService {
    const val STARTING_CASH_USD = 10000.0

    fun getCashBalance(userId: String): Double = transaction {
        val uid = UUID.fromString(userId)
        Users.selectAll().where { Users.id eq uid }.firstOrNull()?.get(Users.virtualCashBalance)
            ?: throw AuthException("User not found")
    }

    fun getHoldings(userId: String): List<HoldingRow> = transaction {
        val uid = UUID.fromString(userId)
        PortfolioHoldings.selectAll().where { PortfolioHoldings.userId eq uid }
            .map { HoldingRow(it[PortfolioHoldings.symbol], it[PortfolioHoldings.quantity], it[PortfolioHoldings.avgCostBasis]) }
    }

    fun buy(userId: String, symbol: String, quantity: Double, pricePerUnit: Double): Unit = transaction {
        if (quantity <= 0) throw AuthException("Quantity must be greater than zero")
        val uid = UUID.fromString(userId)
        val cost = quantity * pricePerUnit

        val userRow = Users.selectAll().where { Users.id eq uid }.firstOrNull() ?: throw AuthException("User not found")
        val cash = userRow[Users.virtualCashBalance]
        if (cash < cost) throw AuthException("Not enough virtual cash for this trade")

        val existing = PortfolioHoldings.selectAll()
            .where { (PortfolioHoldings.userId eq uid) and (PortfolioHoldings.symbol eq symbol) }.firstOrNull()
        val now = System.currentTimeMillis()

        if (existing == null) {
            PortfolioHoldings.insert {
                it[PortfolioHoldings.userId] = uid
                it[assetType] = "crypto"
                it[PortfolioHoldings.symbol] = symbol
                it[PortfolioHoldings.quantity] = quantity
                it[avgCostBasis] = pricePerUnit
                it[updatedAt] = now
            }
        } else {
            val oldQty = existing[PortfolioHoldings.quantity]
            val oldCost = existing[PortfolioHoldings.avgCostBasis]
            val newQty = oldQty + quantity
            val newAvgCost = ((oldQty * oldCost) + (quantity * pricePerUnit)) / newQty
            PortfolioHoldings.update({ (PortfolioHoldings.userId eq uid) and (PortfolioHoldings.symbol eq symbol) }) {
                it[PortfolioHoldings.quantity] = newQty
                it[PortfolioHoldings.avgCostBasis] = newAvgCost
                it[updatedAt] = now
            }
        }

        Users.update({ Users.id eq uid }) { it[virtualCashBalance] = cash - cost }

        PortfolioTransactions.insert {
            it[PortfolioTransactions.userId] = uid
            it[assetType] = "crypto"
            it[PortfolioTransactions.symbol] = symbol
            it[side] = "buy"
            it[PortfolioTransactions.quantity] = quantity
            it[PortfolioTransactions.pricePerUnit] = pricePerUnit
            it[totalValue] = cost
            it[createdAt] = now
        }
    }

    fun sell(userId: String, symbol: String, quantity: Double, pricePerUnit: Double): Unit = transaction {
        if (quantity <= 0) throw AuthException("Quantity must be greater than zero")
        val uid = UUID.fromString(userId)
        val existing = PortfolioHoldings.selectAll()
            .where { (PortfolioHoldings.userId eq uid) and (PortfolioHoldings.symbol eq symbol) }.firstOrNull()
            ?: throw AuthException("You don't own any ${symbol.uppercase()}")
        val ownedQty = existing[PortfolioHoldings.quantity]
        if (quantity > ownedQty) throw AuthException("You only own $ownedQty ${symbol.uppercase()}")

        val proceeds = quantity * pricePerUnit
        val now = System.currentTimeMillis()
        val remainingQty = ownedQty - quantity

        if (remainingQty <= 0.00000001) {
            PortfolioHoldings.deleteWhere { (PortfolioHoldings.userId eq uid) and (PortfolioHoldings.symbol eq symbol) }
        } else {
            PortfolioHoldings.update({ (PortfolioHoldings.userId eq uid) and (PortfolioHoldings.symbol eq symbol) }) {
                it[PortfolioHoldings.quantity] = remainingQty
                it[updatedAt] = now
                // avgCostBasis is unchanged on a partial sell.
            }
        }

        val cash = Users.selectAll().where { Users.id eq uid }.first()[Users.virtualCashBalance]
        Users.update({ Users.id eq uid }) { it[virtualCashBalance] = cash + proceeds }

        PortfolioTransactions.insert {
            it[PortfolioTransactions.userId] = uid
            it[assetType] = "crypto"
            it[PortfolioTransactions.symbol] = symbol
            it[side] = "sell"
            it[PortfolioTransactions.quantity] = quantity
            it[PortfolioTransactions.pricePerUnit] = pricePerUnit
            it[totalValue] = proceeds
            it[createdAt] = now
        }
    }

    fun listTransactions(userId: String): List<PortfolioTransactionItem> = transaction {
        val uid = UUID.fromString(userId)
        PortfolioTransactions.selectAll()
            .where { PortfolioTransactions.userId eq uid }
            .orderBy(PortfolioTransactions.createdAt, SortOrder.DESC)
            .map { row ->
                PortfolioTransactionItem(
                    id = row[PortfolioTransactions.id].value.toString(),
                    symbol = row[PortfolioTransactions.symbol],
                    side = row[PortfolioTransactions.side],
                    quantity = row[PortfolioTransactions.quantity],
                    pricePerUnit = row[PortfolioTransactions.pricePerUnit],
                    totalValue = row[PortfolioTransactions.totalValue],
                    createdAt = row[PortfolioTransactions.createdAt],
                )
            }
    }

    fun listWatchlist(userId: String): List<String> = transaction {
        val uid = UUID.fromString(userId)
        Watchlist.selectAll().where { Watchlist.userId eq uid }.map { it[Watchlist.symbol] }
    }

    fun addToWatchlist(userId: String, symbol: String): Unit = transaction {
        val uid = UUID.fromString(userId)
        val exists = Watchlist.selectAll().where { (Watchlist.userId eq uid) and (Watchlist.symbol eq symbol) }.firstOrNull() != null
        if (!exists) {
            Watchlist.insert {
                it[Watchlist.userId] = uid
                it[assetType] = "crypto"
                it[Watchlist.symbol] = symbol
                it[createdAt] = System.currentTimeMillis()
            }
        }
    }

    fun removeFromWatchlist(userId: String, symbol: String): Unit = transaction {
        val uid = UUID.fromString(userId)
        Watchlist.deleteWhere { (Watchlist.userId eq uid) and (Watchlist.symbol eq symbol) }
    }
}
