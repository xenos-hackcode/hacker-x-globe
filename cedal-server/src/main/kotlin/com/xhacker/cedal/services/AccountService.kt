package com.xhacker.cedal.services

import com.xhacker.cedal.db.AiChangeRequests
import com.xhacker.cedal.db.AiMessages
import com.xhacker.cedal.db.AndroidBuilds
import com.xhacker.cedal.db.Blocks
import com.xhacker.cedal.db.CallOutRejectedSpans
import com.xhacker.cedal.db.ChatMessageReactions
import com.xhacker.cedal.db.ChatMessages
import com.xhacker.cedal.db.ChatPopularityOverrides
import com.xhacker.cedal.db.ConversationState
import com.xhacker.cedal.db.DailyTaskCompletions
import com.xhacker.cedal.db.FriendRequests
import com.xhacker.cedal.db.GuiSessions
import com.xhacker.cedal.db.LessonCompletions
import com.xhacker.cedal.db.LockoutState
import com.xhacker.cedal.db.MessagePins
import com.xhacker.cedal.db.MessageReports
import com.xhacker.cedal.db.Notifications
import com.xhacker.cedal.db.PendingPopups
import com.xhacker.cedal.db.PhoneVerifications
import com.xhacker.cedal.db.PollVotes
import com.xhacker.cedal.db.PopularitySettings
import com.xhacker.cedal.db.PortfolioHoldings
import com.xhacker.cedal.db.PortfolioTransactions
import com.xhacker.cedal.db.RefreshTokens
import com.xhacker.cedal.db.Stickers
import com.xhacker.cedal.db.SystemFeedPosts
import com.xhacker.cedal.db.SystemFeedReactions
import com.xhacker.cedal.db.SystemFeedReads
import com.xhacker.cedal.db.ThemePackPurchases
import com.xhacker.cedal.db.Trades
import com.xhacker.cedal.db.TypingStatus
import com.xhacker.cedal.db.UserAchievements
import com.xhacker.cedal.db.UserReports
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
//
// Must cover every table with a foreign key onto Users (see Tables.kt) -
// this list is otherwise silently out of sync with the schema, and a
// RESTRICT violation on any one of them fails the whole delete. Ordered so
// tables that reference chat_messages/system_feed_posts are cleared before
// those rows themselves, since those two are deleted here too (not just
// user-owned rows in other tables).
object AccountService {
    fun deleteAccount(userId: String): Unit = transaction {
        val uid = UUID.fromString(userId)

        val ownMessageIds = ChatMessages.selectAll()
            .where { (ChatMessages.senderId eq uid) or (ChatMessages.receiverId eq uid) }
            .map { it[ChatMessages.id].value }
        if (ownMessageIds.isNotEmpty()) {
            ChatMessageReactions.deleteWhere { ChatMessageReactions.messageId inList ownMessageIds }
            PollVotes.deleteWhere { PollVotes.messageId inList ownMessageIds }
        }
        ChatMessageReactions.deleteWhere { ChatMessageReactions.userId eq uid }
        PollVotes.deleteWhere { PollVotes.userId eq uid }
        ChatMessages.deleteWhere { (ChatMessages.senderId eq uid) or (ChatMessages.receiverId eq uid) }

        val ownPostIds = SystemFeedPosts.selectAll().where { SystemFeedPosts.authorId eq uid }.map { it[SystemFeedPosts.id].value }
        if (ownPostIds.isNotEmpty()) {
            SystemFeedReactions.deleteWhere { SystemFeedReactions.postId inList ownPostIds }
        }
        SystemFeedReactions.deleteWhere { SystemFeedReactions.userId eq uid }
        SystemFeedPosts.deleteWhere { SystemFeedPosts.authorId eq uid }
        SystemFeedReads.deleteWhere { SystemFeedReads.userId eq uid }

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
        PhoneVerifications.deleteWhere { PhoneVerifications.userId eq uid }

        AiChangeRequests.deleteWhere { AiChangeRequests.requesterUserId eq uid }
        AiMessages.deleteWhere { AiMessages.userId eq uid }
        Blocks.deleteWhere { (Blocks.blockerId eq uid) or (Blocks.blockedId eq uid) }
        CallOutRejectedSpans.deleteWhere { CallOutRejectedSpans.userId eq uid }
        ChatPopularityOverrides.deleteWhere { (ChatPopularityOverrides.userId eq uid) or (ChatPopularityOverrides.friendId eq uid) }
        ConversationState.deleteWhere { (ConversationState.userId eq uid) or (ConversationState.friendId eq uid) }
        GuiSessions.deleteWhere { GuiSessions.userId eq uid }
        MessagePins.deleteWhere { MessagePins.userId eq uid }
        MessageReports.deleteWhere { MessageReports.reporterUserId eq uid }
        PendingPopups.deleteWhere { PendingPopups.userId eq uid }
        PopularitySettings.deleteWhere { PopularitySettings.userId eq uid }
        Stickers.deleteWhere { Stickers.ownerId eq uid }
        ThemePackPurchases.deleteWhere { ThemePackPurchases.userId eq uid }
        TypingStatus.deleteWhere { (TypingStatus.userId eq uid) or (TypingStatus.friendId eq uid) }
        UserAchievements.deleteWhere { UserAchievements.userId eq uid }
        UserReports.deleteWhere { (UserReports.reporterId eq uid) or (UserReports.reportedId eq uid) }

        Users.deleteWhere { Users.id eq uid }
    }
}
