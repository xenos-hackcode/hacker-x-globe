package com.xhacker.cedal.services

import com.xhacker.cedal.db.Notifications
import com.xhacker.cedal.db.Trades
import com.xhacker.cedal.db.Users
import com.xhacker.cedal.models.NotificationItem
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

// Ported from cedal-mobile's TradeScreen.tsx + NotificationsScreen.tsx — a
// global "I need SC" request board. Posting a trade mirrors it into the
// global Notifications feed (replaces the RN app's Firestore
// "notifications" collection); other users can "Credit" it (send the
// requester the SC, reusing WalletService's atomic transfer + debt-limit
// check) or the creator can delete their own post.
object TradeService {

    fun createTrade(userId: String, amount: Int, plea: String?): Unit = transaction {
        if (amount <= 0) throw AuthException("Amount must be greater than zero")
        val uid = UUID.fromString(userId)
        val userRow = Users.selectAll().where { Users.id eq uid }.firstOrNull() ?: throw AuthException("User not found")
        val creatorName = displayNameFor(userRow)
        val cleanPlea = plea?.trim()?.take(280)?.ifBlank { null }

        val now = System.currentTimeMillis()
        val tradeId = Trades.insert {
            it[Trades.userId] = uid
            it[Trades.amount] = amount
            it[Trades.plea] = cleanPlea
            it[Trades.creatorName] = creatorName
            it[Trades.createdAt] = now
        } get Trades.id

        Notifications.insert {
            it[type] = "trade"
            it[title] = "Trade request: $amount SC"
            it[body] = cleanPlea ?: "$creatorName requested $amount Star Coins."
            it[fromUserId] = uid
            it[fromUserName] = creatorName
            it[Notifications.tradeId] = tradeId
            it[Notifications.amount] = amount
            it[createdAt] = now
        }
    }

    fun listNotifications(callerId: String): List<NotificationItem> = transaction {
        val callerUid = UUID.fromString(callerId)
        Notifications.selectAll()
            .orderBy(Notifications.createdAt, SortOrder.DESC)
            .map { row ->
                NotificationItem(
                    id = row[Notifications.id].value.toString(),
                    type = row[Notifications.type],
                    title = row[Notifications.title],
                    body = row[Notifications.body],
                    fromUserId = row[Notifications.fromUserId]?.value?.toString(),
                    fromUserName = row[Notifications.fromUserName],
                    tradeId = row[Notifications.tradeId]?.value?.toString(),
                    amount = row[Notifications.amount],
                    createdAt = row[Notifications.createdAt],
                    isMine = row[Notifications.fromUserId]?.value == callerUid,
                )
            }
    }

    // Fulfilling someone else's trade request — sends them the requested
    // amount. Reuses WalletService.send so the debt-limit check and ledger
    // writes are identical to a direct Send.
    fun creditNotification(callerId: String, notificationId: String): Int {
        val notifRow = transaction {
            Notifications.selectAll().where { Notifications.id eq UUID.fromString(notificationId) }.firstOrNull()
        } ?: throw AuthException("Notification not found")

        val fromUserId = notifRow[Notifications.fromUserId]?.value ?: throw AuthException("This trade is missing a receiver")
        val amount = notifRow[Notifications.amount] ?: throw AuthException("This trade is missing an amount")
        if (fromUserId.toString() == callerId) throw AuthException("You cannot credit your own trade")

        return WalletService.send(callerId, fromUserId.toString(), amount)
    }

    fun deleteNotification(callerId: String, notificationId: String): Unit = transaction {
        val uid = UUID.fromString(callerId)
        val nid = UUID.fromString(notificationId)
        val row = Notifications.selectAll().where { Notifications.id eq nid }.firstOrNull() ?: throw AuthException("Notification not found")
        if (row[Notifications.fromUserId]?.value != uid) throw AuthException("Only the trade creator can delete this")
        Notifications.deleteWhere { Notifications.id eq nid }
    }
}
