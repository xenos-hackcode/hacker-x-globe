package com.xhacker.cedal.db

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseFactory {
    fun init() {
        // On Cloud Run (DB_INSTANCE_CONNECTION_NAME set via env), connect to
        // Cloud SQL Postgres through its socket factory instead of the local
        // H2 file - Cloud Run instances don't keep local disk between
        // requests/instances, so H2-on-disk isn't viable there. Local dev
        // keeps using the H2 file exactly as before when that var is unset.
        val cloudSqlInstance = System.getenv("DB_INSTANCE_CONNECTION_NAME")
        if (cloudSqlInstance != null) {
            val dbName = System.getenv("DB_NAME") ?: "cedal"
            val dbUser = System.getenv("DB_USER") ?: "cedal"
            val dbPassword = System.getenv("DB_PASSWORD") ?: ""
            Database.connect(
                url = "jdbc:postgresql:///$dbName?cloudSqlInstance=$cloudSqlInstance" +
                    "&socketFactory=com.google.cloud.sql.postgres.SocketFactory" +
                    "&user=$dbUser&password=$dbPassword",
                driver = "org.postgresql.Driver",
            )
        } else {
            Database.connect(
                url = "jdbc:h2:file:./data/cedal;DB_CLOSE_DELAY=-1",
                driver = "org.h2.Driver",
            )
        }
        transaction {
            // ...AndColumns (vs. plain .create) lets the dev schema evolve —
            // adds missing tables/columns without dropping existing data.
            SchemaUtils.createMissingTablesAndColumns(
                Users, LockoutState, VerificationCodes, RefreshTokens, FriendRequests,
                WalletTransactions, Trades, Notifications,
                PortfolioHoldings, PortfolioTransactions, Watchlist, AndroidBuilds,
                DailyTasks, DailyTaskCompletions, ArcPracticeApps, ChatMessages,
                // LessonCompletions was defined but never actually included
                // here - it's been silently missing from the live DB this
                // whole time, meaning every Learn lesson "mark as done"
                // (Invest, ARC) has been throwing away its exp award.
                LessonCompletions, ChatMessageReactions,
                SystemFeedPosts, SystemFeedReactions, SystemFeedReads, Stickers, ThemePackPurchases, PollVotes,
                GuiSessions, AiChangeRequests, AiMessages, MessagePins, MessageReports, TypingStatus, ConversationState,
                Blocks, UserReports, PhoneVerifications,
                // Achievements/rank-up popups/Popularity/Call Out - added
                // this session but originally missed here, which silently
                // 500'd every route touching them in production (see
                // AchievementService/PendingPopupService/PopularityService/
                // CallOutService).
                UserAchievements, PendingPopups, PopularitySettings, ChatPopularityOverrides, CallOutRejectedSpans,
                BannedIdentities, Appeals, AdminClearedIdentities, PendingSmsJobs, AppVersionConfig,
                // DeveloperSubmissions (Phase B's submit/review/approve
                // pipeline) was defined in Tables.kt but never added here -
                // same class of miss as LessonCompletions/Achievements
                // above, meaning it likely never actually existed in the
                // live DB and every submission attempt would have 500'd.
                DeveloperSubmissions,
                // SMS relay multi-developer platform - see PlatformDevelopers'
                // own doc comment.
                PlatformDevelopers, PlatformSmsJobs, PendingPlatformSignups, PlatformOAuthSessions,
                PlatformEmailCredentials, PlatformEmailRateLimit, PlatformEmailSends,
                Groups, GroupMembers, GroupMessages, GroupMessageReactions, GroupPollVotes, GroupMessageViews,
                // Code area <-> GitHub sync - see Tables.kt's own doc comments
                // on each of these for why they're separate from the
                // Rules-tab GitHubService/AiChangeRequests flow.
                CodeGithubConnections, PendingCodeGithubOAuth, CodeSyncFiles, CodeSyncJobs,
                // Group chat settings/permissions/moderation expansion - see
                // each table's own doc comment in Tables.kt.
                GroupJoinRequests, GroupReports, BlockedGroups, GroupConversationState, GroupAddRequests, GroupTypingStatus, SavedMessages,
                // Round 5 - see each table's own doc comment in Tables.kt.
                GroupRejoinCooldowns,
                // "Known" native-dialer calling - see Users.shareNumberDefault/
                // Groups.callsEnabled's own doc comments in Tables.kt.
                PhoneShareOverrides,
                // Bots/"Leo" Round 1 (2026-08-09) - same class of miss as
                // LessonCompletions/DeveloperSubmissions above: defined in
                // Tables.kt, used throughout BotService/BotRoutes, but never
                // added here, so every /bots call 500'd in production with
                // "relation \"bots\" does not exist" until this was caught.
                Bots,
                // Bots/"Leo" Round 2 (2026-08-10) - added here immediately
                // this time, having just been burned by forgetting it for
                // Bots itself the day before.
                BotConversationTurns,
            )
            // createMissingTablesAndColumns only ever *adds* schema — it never
            // drops a constraint that used to be declared here. handle was
            // uniqueIndex() before; now that it isn't, the old UNIQUE
            // constraint (H2 names single-column uniqueIndex() constraints
            // "<TABLE>_<COLUMN>_UNIQUE") still physically exists on any dev
            // DB created before this change, and its backing index can't be
            // dropped directly in H2 — you have to drop the constraint that
            // owns it. IF EXISTS makes this a no-op on a fresh DB.
            exec("ALTER TABLE USERS DROP CONSTRAINT IF EXISTS \"USERS_HANDLE_UNIQUE\"")
        }
    }
}
