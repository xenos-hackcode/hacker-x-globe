package com.xhacker.cedal.services

import com.xhacker.cedal.db.AndroidBuilds
import com.xhacker.cedal.db.ChatMessages
import com.xhacker.cedal.db.DailyTaskCompletions
import com.xhacker.cedal.db.FriendRequests
import com.xhacker.cedal.db.LessonCompletions
import com.xhacker.cedal.db.LockoutState
import com.xhacker.cedal.db.Notifications
import com.xhacker.cedal.db.PortfolioHoldings
import com.xhacker.cedal.db.PortfolioTransactions
import com.xhacker.cedal.db.RefreshTokens
import com.xhacker.cedal.db.Trades
import com.xhacker.cedal.db.Users
import com.xhacker.cedal.db.VerificationCodes
import com.xhacker.cedal.db.WalletTransactions
import com.xhacker.cedal.db.Watchlist
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

// Self-service account deletion - the exact same cascade a manual DB cleanup
// would need, just as reusable, transactional code instead of hand-written
// SQL each time (see MemberSettingsScreen's "Delete Account" row). Every
// Notifications row with fromUserId == this user is always tied 1:1 to one
// of their own trade posts (see TradeService.createTrade - the only place
// that ever sets fromUserId), so deleting by ownTradeIds already covers all
// of them; no other user's notification ever references this one as sender.
object AccountService {
    fun deleteAccount(userId: String): Unit = transaction {
        val uid = UUID.fromString(userId)

        ChatMessages.deleteWhere { (ChatMessages.senderId eq uid) or (ChatMessages.receiverId eq uid) }
        FriendRequests.deleteWhere { (FriendRequests.fromUserId eq uid) or (FriendRequests.toUserId eq uid) }
        WalletTransactions.deleteWhere { (WalletTransactions.userId eq uid) or (WalletTransactions.peerUserId eq uid) }

        val ownTradeIds = Trades.selectAll().where { Trades.userId eq uid }.map { it[Trades.id].value }
        if (ownTradeIds.isNotEmpty()) {
            Notifications.deleteWhere { Notifications.tradeId inList ownTradeIds }
        }
        Trades.deleteWhere { Trades.userId eq uid }

        PortfolioHoldings.deleteWhere { PortfolioHoldings.userId eq uid }
        PortfolioTransactions.deleteWhere { PortfolioTransactions.userId eq uid }
        Watchlist.deleteWhere { Watchlist.userId eq uid }
        LessonCompletions.deleteWhere { LessonCompletions.userId eq uid }
        DailyTaskCompletions.deleteWhere { DailyTaskCompletions.userId eq uid }
        AndroidBuilds.deleteWhere { AndroidBuilds.userId eq uid }
        RefreshTokens.deleteWhere { RefreshTokens.userId eq uid }
        LockoutState.deleteWhere { LockoutState.userId eq uid }
        VerificationCodes.deleteWhere { VerificationCodes.userId eq uid }
        Users.deleteWhere { Users.id eq uid }
    }
}
