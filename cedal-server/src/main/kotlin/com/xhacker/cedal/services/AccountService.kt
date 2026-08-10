package com.xhacker.cedal.services

import com.xhacker.cedal.db.AiChangeRequests
import com.xhacker.cedal.db.AiMessages
import com.xhacker.cedal.db.AndroidBuilds
import com.xhacker.cedal.db.BlockedGroups
import com.xhacker.cedal.db.Blocks
import com.xhacker.cedal.db.BotConversationTurns
import com.xhacker.cedal.db.Bots
import com.xhacker.cedal.db.CallOutRejectedSpans
import com.xhacker.cedal.db.ChatMessageReactions
import com.xhacker.cedal.db.ChatMessages
import com.xhacker.cedal.db.ChatPopularityOverrides
import com.xhacker.cedal.db.CodeGithubConnections
import com.xhacker.cedal.db.CodeSyncFiles
import com.xhacker.cedal.db.CodeSyncJobs
import com.xhacker.cedal.db.ConversationState
import com.xhacker.cedal.db.DailyTaskCompletions
import com.xhacker.cedal.db.DeveloperSubmissions
import com.xhacker.cedal.db.FriendRequests
import com.xhacker.cedal.db.GroupConversationState
import com.xhacker.cedal.db.GroupJoinRequests
import com.xhacker.cedal.db.GroupMembers
import com.xhacker.cedal.db.GroupMessageReactions
import com.xhacker.cedal.db.GroupMessageViews
import com.xhacker.cedal.db.GroupMessages
import com.xhacker.cedal.db.GroupPollVotes
import com.xhacker.cedal.db.GroupRejoinCooldowns
import com.xhacker.cedal.db.GroupReports
import com.xhacker.cedal.db.Groups
import com.xhacker.cedal.db.GuiSessions
import com.xhacker.cedal.db.LessonCompletions
import com.xhacker.cedal.db.LockoutState
import com.xhacker.cedal.db.MessagePins
import com.xhacker.cedal.db.MessageReports
import com.xhacker.cedal.db.Notifications
import com.xhacker.cedal.db.PendingCodeGithubOAuth
import com.xhacker.cedal.db.PendingPopups
import com.xhacker.cedal.db.PhoneShareOverrides
import com.xhacker.cedal.db.PhoneVerifications
import com.xhacker.cedal.db.PollVotes
import com.xhacker.cedal.db.PopularitySettings
import com.xhacker.cedal.db.PortfolioHoldings
import com.xhacker.cedal.db.PortfolioTransactions
import com.xhacker.cedal.db.RefreshTokens
import com.xhacker.cedal.db.SavedMessages
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
import org.jetbrains.exposed.sql.SqlExpressionBuilder.neq
import org.jetbrains.exposed.sql.and
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
        // Added with the Bots/"Leo" Round 1 table (2026-08-09) - see this
        // function's own doc comment on why every FK onto Users must be
        // listed here. Round 2's BotConversationTurns must be cleared
        // BEFORE the Bots rows themselves - its FK is onto Bots, not Users,
        // so deleting Bots first would hit the exact FK RESTRICT trap this
        // function's whole doc comment already warns about.
        val ownedBotIds = Bots.selectAll().where { Bots.ownerUserId eq uid }.map { it[Bots.id].value }
        if (ownedBotIds.isNotEmpty()) {
            BotConversationTurns.deleteWhere { BotConversationTurns.botId inList ownedBotIds }
        }
        Bots.deleteWhere { Bots.ownerUserId eq uid }
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

        // Added 2026-08-09 - found by tracing every reference(..., Users) in
        // Tables.kt against this function's coverage (see risks.md), after
        // a real Clear Data silently failed on an account that had touched
        // one of these. Groups this user CREATED get the exact same
        // succession/dissolve handling as the self-service "Leave Group"
        // flow (GroupChatService.leaveGroup) - handed off to a
        // Vice-Creator/Admin/random member if others remain, fully torn
        // down (deleteGroupFully) if they were alone - rather than a blunt
        // delete that would erase the group out from under other members.
        // Must run before the plain GroupMembers cleanup below, since
        // leaveGroup needs this user's membership row to still exist to
        // compute succession correctly.
        val ownedGroupIds = Groups.selectAll().where { Groups.creatorId eq uid }.map { it[Groups.id].value }
        ownedGroupIds.forEach { gid ->
            val hasOtherMembers = GroupMembers.selectAll()
                .where { (GroupMembers.groupId eq gid) and (GroupMembers.userId neq uid) }
                .any()
            GroupChatService.leaveGroup(gid.toString(), uid.toString(), dissolve = !hasOtherMembers, random = true, systemOwner = true)
        }

        // Their own sent messages in whatever groups remain (handed-off
        // owned groups, plus groups they were just a member of) - same
        // full-erasure precedent as ChatMessages above, not left behind
        // attributed to a deleted sender.
        val ownGroupMessageIds = GroupMessages.selectAll().where { GroupMessages.senderId eq uid }.map { it[GroupMessages.id].value }
        if (ownGroupMessageIds.isNotEmpty()) {
            GroupMessageReactions.deleteWhere { GroupMessageReactions.messageId inList ownGroupMessageIds }
            GroupPollVotes.deleteWhere { GroupPollVotes.messageId inList ownGroupMessageIds }
            GroupMessageViews.deleteWhere { GroupMessageViews.messageId inList ownGroupMessageIds }
        }
        // Their own reactions/votes/views on OTHER members' messages.
        GroupMessageReactions.deleteWhere { GroupMessageReactions.userId eq uid }
        GroupPollVotes.deleteWhere { GroupPollVotes.userId eq uid }
        GroupMessageViews.deleteWhere { GroupMessageViews.userId eq uid }
        GroupMessages.deleteWhere { GroupMessages.senderId eq uid }

        GroupMembers.deleteWhere { GroupMembers.userId eq uid }
        GroupJoinRequests.deleteWhere { GroupJoinRequests.userId eq uid }
        GroupReports.deleteWhere { GroupReports.reporterId eq uid }
        BlockedGroups.deleteWhere { BlockedGroups.userId eq uid }
        GroupConversationState.deleteWhere { GroupConversationState.userId eq uid }
        GroupRejoinCooldowns.deleteWhere { GroupRejoinCooldowns.userId eq uid }
        SavedMessages.deleteWhere { SavedMessages.userId eq uid }
        CodeGithubConnections.deleteWhere { CodeGithubConnections.userId eq uid }
        PendingCodeGithubOAuth.deleteWhere { PendingCodeGithubOAuth.userId eq uid }
        CodeSyncFiles.deleteWhere { CodeSyncFiles.userId eq uid }
        CodeSyncJobs.deleteWhere { CodeSyncJobs.userId eq uid }
        DeveloperSubmissions.deleteWhere { DeveloperSubmissions.userId eq uid }
        PhoneShareOverrides.deleteWhere { (PhoneShareOverrides.userId eq uid) or (PhoneShareOverrides.friendId eq uid) }

        Users.deleteWhere { Users.id eq uid }
    }
}
