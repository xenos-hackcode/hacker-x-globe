package com.xhacker.cedal.services

import com.xhacker.cedal.db.Users
import com.xhacker.cedal.db.WalletTransactions
import com.xhacker.cedal.models.WalletTransactionItem
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.util.UUID

// Star Coins wallet — replaces cedal-mobile's Send/Transactions screens'
// direct Firestore reads/writes. "Buy SC" (real-money top-up) isn't wired
// up here at all — see REMINDER.md, that needs actual payments licensing
// first, so it stays a disabled stub in the UI regardless of this service.
object WalletService {
    // Matches cedal-mobile's DebtScreen "debt limit" — a soft credit line,
    // not zero, so small overspends don't hard-block a send.
    const val MIN_DEBT_SC = -30

    fun getBalance(userId: String): Int = transaction {
        val uid = UUID.fromString(userId)
        Users.selectAll().where { Users.id eq uid }.firstOrNull()?.get(Users.scBalance)
            ?: throw AuthException("User not found")
    }

    // Atomic transfer: debits the sender, credits the receiver, and writes
    // one ledger row on each side so both users' histories stay a simple
    // filter on their own userId (mirrors the RN version's two Firestore
    // writes per transfer).
    fun send(fromUserId: String, toUserId: String, amount: Int): Int = transaction {
        if (amount <= 0) throw AuthException("Amount must be greater than zero")
        val fromUid = UUID.fromString(fromUserId)
        val toUid = UUID.fromString(toUserId)
        if (fromUid == toUid) throw AuthException("Cannot send Star Coins to yourself")

        val fromRow = Users.selectAll().where { Users.id eq fromUid }.firstOrNull() ?: throw AuthException("User not found")
        val toRow = Users.selectAll().where { Users.id eq toUid }.firstOrNull() ?: throw AuthException("Recipient not found")

        val newFromBalance = fromRow[Users.scBalance] - amount
        if (newFromBalance < MIN_DEBT_SC) {
            throw AuthException("That would put you past your debt limit ($MIN_DEBT_SC SC)")
        }

        val fromName = displayNameFor(fromRow)
        val toName = displayNameFor(toRow)

        Users.update({ Users.id eq fromUid }) { it[scBalance] = newFromBalance }
        Users.update({ Users.id eq toUid }) { it[scBalance] = toRow[Users.scBalance] + amount }

        val now = System.currentTimeMillis()
        WalletTransactions.insert {
            it[WalletTransactions.userId] = fromUid
            it[type] = "credit_out"
            it[WalletTransactions.amount] = amount
            it[peerUserId] = toUid
            it[peerName] = toName
            it[createdAt] = now
        }
        WalletTransactions.insert {
            it[WalletTransactions.userId] = toUid
            it[type] = "credit_in"
            it[WalletTransactions.amount] = amount
            it[peerUserId] = fromUid
            it[peerName] = fromName
            it[createdAt] = now
        }

        newFromBalance
    }

    // Debits SC for a system purchase (no peer/recipient) - see
    // ThemePackService. Same debt-limit check as send(), just no credit
    // side since the coins aren't going to another user.
    fun spend(userId: String, amount: Int, reason: String): Int = transaction {
        if (amount <= 0) throw AuthException("Amount must be greater than zero")
        val uid = UUID.fromString(userId)
        val row = Users.selectAll().where { Users.id eq uid }.firstOrNull() ?: throw AuthException("User not found")

        val newBalance = row[Users.scBalance] - amount
        if (newBalance < MIN_DEBT_SC) {
            throw AuthException("That would put you past your debt limit ($MIN_DEBT_SC SC)")
        }

        Users.update({ Users.id eq uid }) { it[scBalance] = newBalance }
        WalletTransactions.insert {
            it[WalletTransactions.userId] = uid
            it[type] = "credit_out"
            it[WalletTransactions.amount] = amount
            it[peerName] = reason
            it[createdAt] = System.currentTimeMillis()
        }
        newBalance
    }

    fun listTransactions(userId: String, filter: String?): List<WalletTransactionItem> = transaction {
        val uid = UUID.fromString(userId)
        WalletTransactions.selectAll()
            .where { WalletTransactions.userId eq uid }
            .orderBy(WalletTransactions.createdAt, SortOrder.DESC)
            .mapNotNull { row ->
                val type = row[WalletTransactions.type]
                val isCredit = type != "credit_out"
                if (filter == "credit" && !isCredit) return@mapNotNull null
                if (filter == "debit" && isCredit) return@mapNotNull null
                WalletTransactionItem(
                    id = row[WalletTransactions.id].value.toString(),
                    type = type,
                    amount = row[WalletTransactions.amount],
                    peerName = row[WalletTransactions.peerName],
                    createdAt = row[WalletTransactions.createdAt],
                )
            }
    }
}
