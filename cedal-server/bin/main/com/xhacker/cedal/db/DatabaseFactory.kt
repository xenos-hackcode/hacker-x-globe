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
                GuiSessions, AiChangeRequests, AiMessages, MessagePins, MessageReports,
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
