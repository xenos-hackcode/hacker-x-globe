package com.xhacker.cedal.db

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Table

object Users : UUIDTable("users") {
    // Postgres SERIAL - a plain incrementing number alongside the UUID
    // primary key, purely for the "Cedal 1"/"Cedal 2"/... display-name
    // fallback (see displayNameFor() in UserNames.kt) when an account has
    // neither a nickname nor an email to show. Adding a SERIAL column to an
    // existing table backfills every existing row with sequential values
    // automatically - no separate migration needed.
    val serial = integer("serial").autoIncrement()
    val email = varchar("email", 255).nullable().uniqueIndex()
    val passwordHash = varchar("password_hash", 255).nullable()
    val isGuest = bool("is_guest").default(false)
    val emailVerified = bool("email_verified").default(false)
    // Versioned so a later terms change can force re-acceptance without
    // needing a fresh account — see AuthService.CURRENT_TERMS_VERSION.
    val acceptedTermsVersion = varchar("accepted_terms_version", 20).nullable()
    val acceptedTermsAt = long("accepted_terms_at").nullable()
    val role = varchar("role", 20).default("user")
    val devKey = varchar("dev_key", 7)
    val passcode = varchar("passcode", 10).nullable()
    val age = integer("age").nullable()
    val favoriteColor = varchar("favorite_color", 100).nullable()
    val nickname = varchar("nickname", 100).nullable()
    // Auto-generated at signup, unique, shown on the user's profile. Nullable
    // only so adding this column to the existing dev DB doesn't need a
    // backfill migration — AuthService.signup() always sets it for new rows.
    val publicId = varchar("public_id", 12).nullable().uniqueIndex()
    // Optional user-chosen @handle. Not unique — publicId is the only thing
    // guaranteed unique on a profile; handle/nickname/etc. are just display
    // text, same as any other profile field.
    val handle = varchar("handle", 20).nullable()
    val occupation = varchar("occupation", 200).nullable()
    val hobby = varchar("hobby", 200).nullable()
    val bio = varchar("bio", 1000).nullable()
    val gender = varchar("gender", 50).nullable()
    val avatarUrl = varchar("avatar_url", 500).nullable()
    // "Friend Hider" - when on, this account is excluded from every friend
    // search/suggestion result entirely, including an exact name/email
    // match (see FriendService.search()) - not just hidden from a default
    // browse list.
    val hideFromSearch = bool("hide_from_search").default(false)
    val createdAt = long("created_at")
    val lastSeen = long("last_seen").nullable()
    // "Two-way verification" — an extra emailed code required alongside
    // password on future sign-ins. Only meaningful for linked (non-guest)
    // accounts, since guests have no email to send a code to.
    val twoFactorEnabled = bool("two_factor_enabled").default(false)
    // Set only on guest signups (the device's Android ID) — restricts a
    // device to one *active* guest node at a time. Not unique: once a guest
    // links an email, isGuest flips to false and this stops counting, so
    // the same device is free to spawn a new guest again.
    val deviceId = varchar("device_id", 200).nullable()
    // Star Coins wallet balance — in-app currency only, see REMINDER.md
    // (one-way purchase, no cash-out, until there's a licensed payments
    // partner). Allowed to go negative down to WalletService.MIN_DEBT_SC,
    // matching cedal-mobile's DebtScreen "debt limit" concept.
    val scBalance = integer("sc_balance").default(0)
    // Virtual USD balance for the Invest simulator — a separate ledger from
    // Star Coins (themed as a brokerage account, matching cedal-mobile's
    // Overview screen showing dollar amounts). Never real money; trades are
    // simulated but priced against real market data. Starts at
    // PortfolioService.STARTING_CASH_USD.
    val virtualCashBalance = double("virtual_cash_balance").default(10000.0)

    // Two distinct progress currencies (user's own distinction, not
    // interchangeable): "exp" comes from in-app activity (completing Invest
    // lessons, etc.) and powers other things TBD; "xp" comes only from real
    // money spent in the Shop (e.g. 0.99 GBP -> 10 xp, per the planned
    // economy - not finalized yet) and is what RankService uses to compute
    // the F/E/D/C/B -> A/AA/AAA -> S/SS/SSS -> Legend/Mythic/Instinct/EX/L/M
    // ladder. Feature gates (e.g. Kotlin app builds) check rank, not xp
    // directly - see RankService.meetsRank.
    val exp = long("exp").default(0)
    val xp = long("xp").default(0)
    // When this account last had its rank/tier decay applied (both exp and
    // xp decay together, every 3 months) - see DecayService. Null means
    // "never decayed yet"; DecayService treats that as due starting 3
    // months after createdAt, not immediately, so brand-new accounts aren't
    // punished on their very first scheduled run.
    val lastDecayAt = long("last_decay_at").nullable()
}

// Server-side passcode/dev-key lockout state — replaces the client-only
// SecureStore lockout in the RN app, which could be bypassed by clearing app storage.
object LockoutState : Table("lockout_state") {
    val userId = reference("user_id", Users)
    val failCount = integer("fail_count").default(0)
    val lockUntil = long("lock_until").nullable()
    override val primaryKey = PrimaryKey(userId)
}

// One-time codes for email verification and password reset (in-app 6-digit code
// instead of a clickable link, to avoid needing a hosted redirect for the MVP).
object VerificationCodes : Table("verification_codes") {
    val userId = reference("user_id", Users)
    val purpose = varchar("purpose", 20) // "verify_email" | "reset_password"
    val code = varchar("code", 6)
    val expiresAt = long("expires_at")
    override val primaryKey = PrimaryKey(userId, purpose)
}

object RefreshTokens : UUIDTable("refresh_tokens") {
    val userId = reference("user_id", Users)
    val tokenHash = varchar("token_hash", 255)
    val expiresAt = long("expires_at")
    val revoked = bool("revoked").default(false)
}

// Backs the "find friends" flow (cedal-mobile's search.tsx Search/Requests
// tabs) — replaces its Firestore "friendRequests" collection.
object FriendRequests : UUIDTable("friend_requests") {
    val fromUserId = reference("from_user_id", Users)
    val toUserId = reference("to_user_id", Users)
    val status = varchar("status", 20).default("pending") // pending | accepted | declined
    val createdAt = long("created_at")
}

// Real 1-on-1 chat between accepted friends (see ChatService) - one row per
// message, no separate "conversation" row; the (sender, receiver) pair
// itself is the conversation. Only ever writable between two users with an
// accepted FriendRequests row.
object ChatMessages : UUIDTable("chat_messages") {
    val senderId = reference("sender_id", Users)
    val receiverId = reference("receiver_id", Users)
    val text = text("text")
    val sentAt = long("sent_at")
    // Quoting/replying to an earlier message - nullable, no enforced FK
    // constraint (a plain UUID column, not reference()) so deleting old
    // messages later never needs cascade handling just for this pointer.
    val replyToId = uuid("reply_to_id").nullable()
    val editedAt = long("edited_at").nullable()
    // Soft-delete only - text stays in the DB (never actually erased) but
    // ChatService always serves an empty string once this is true, same as
    // cedal-mobile's tombstone behavior.
    val deleted = bool("deleted").default(false)
    // True when `text` holds a sticker (an emoji rendered large, no bubble)
    // rather than typed text - see ChatMessageThreadScreen's MessageBubble.
    val isSticker = bool("is_sticker").default(false)

    // Camera/gallery/file attachments (see ImageUploadService/ChatService) -
    // mediaType is "image" | "video" | "file"; text still carries a caption
    // if the user added one alongside the attachment.
    val mediaUrl = varchar("media_url", 500).nullable()
    val mediaType = varchar("media_type", 10).nullable()
    val fileName = varchar("file_name", 255).nullable()

    // "View Once" - viewedAt is set the first time the RECIPIENT fetches
    // this message; once set, both sides see it stripped (see
    // ChatService.toDto) - same one-time-reveal idea as Snapchat/WhatsApp's
    // view-once, applied to text messages too, not just media.
    val viewOnce = bool("view_once").default(false)
    val viewedAt = long("viewed_at").nullable()

    // A poll message - options is a simple newline-joined list (never
    // contains newlines itself, already trimmed client-side) rather than a
    // separate table, since a poll's option set never changes after send.
    val pollQuestion = varchar("poll_question", 500).nullable()
    val pollOptions = text("poll_options").nullable()

    // Set the moment the RECIPIENT actually opens this conversation and
    // fetches its messages (see ChatService.getMessages) - drives the
    // unread-count badge on the chat list (listConversations). Independent
    // of view-once's viewedAt, which is about content reveal, not read
    // status - a view-once message is "read" (seen in the list) the moment
    // you open the thread even before you tap to reveal it.
    val readAt = long("read_at").nullable()
}

// One row per user per poll, changing your vote just updates optionIndex -
// see ChatService.voteInPoll.
object PollVotes : Table("poll_votes") {
    val messageId = reference("message_id", ChatMessages)
    val userId = reference("user_id", Users)
    val optionIndex = integer("option_index")
    override val primaryKey = PrimaryKey(messageId, userId)
}

// One reaction per user per message (matches cedal-mobile: a single emoji
// per user, not a list) - tapping the same emoji again removes it (see
// ChatService.reactToMessage).
object ChatMessageReactions : Table("chat_message_reactions") {
    val messageId = reference("message_id", ChatMessages)
    val userId = reference("user_id", Users)
    val emoji = varchar("emoji", 16)
    override val primaryKey = PrimaryKey(messageId, userId)
}

// Cedal System Feed - the broadcast "chat" every account sees. Only the
// admin account (see SystemFeedService.ADMIN_EMAIL) can post; everyone else
// can only react, same reaction-toggle shape as ChatMessageReactions.
object SystemFeedPosts : UUIDTable("system_feed_posts") {
    val authorId = reference("author_id", Users)
    val text = text("text")
    val createdAt = long("created_at")
}

object SystemFeedReactions : Table("system_feed_reactions") {
    val postId = reference("post_id", SystemFeedPosts)
    val userId = reference("user_id", Users)
    val emoji = varchar("emoji", 16)
    override val primaryKey = PrimaryKey(postId, userId)
}

// One row per user, tracking when they last opened the System Feed - drives
// its unread-count badge on the Chats list (see ChatService.listConversations
// merging in SystemFeedService.unreadCountFor). No row yet = never opened,
// so every existing post counts as unread until their first visit.
object SystemFeedReads : Table("system_feed_reads") {
    val userId = reference("user_id", Users)
    val lastSeenAt = long("last_seen_at")
    override val primaryKey = PrimaryKey(userId)
}

// A user's own uploaded stickers (see ImageUploadService/StickerService) -
// distinct from the client-side default emoji pack (MemberChatThreadScreen's
// DEFAULT_STICKERS), which needs no backend at all. Private to ownerId - not
// a shared library everyone draws from, same as WhatsApp's "your stickers".
object Stickers : UUIDTable("stickers") {
    val ownerId = reference("owner_id", Users)
    val imageUrl = varchar("image_url", 500)
    val createdAt = long("created_at")
}

// Which cosmetic theme-color packs a user has bought - see
// ThemePackService.CATALOG for the fixed price/color list (server-side
// source of truth, not stored per-row). One row per owned pack.
object ThemePackPurchases : Table("theme_pack_purchases") {
    val userId = reference("user_id", Users)
    val packId = varchar("pack_id", 50)
    val purchasedAt = long("purchased_at")
    override val primaryKey = PrimaryKey(userId, packId)
}

// Star Coins wallet ledger — replaces cedal-mobile's Firestore
// "walletTransactions" collection. One row per side of a transfer (a send
// writes both a credit_out row for the sender and a credit_in row for the
// receiver), so each user's own history is a simple filter on userId.
object WalletTransactions : UUIDTable("wallet_transactions") {
    val userId = reference("user_id", Users)
    val type = varchar("type", 20) // "topup" | "credit_in" | "credit_out"
    val amount = integer("amount") // always positive; sign implied by type
    val peerUserId = reference("peer_user_id", Users).nullable()
    // Denormalized display name at the time of the transaction, so history
    // still reads correctly even if the peer later renames themselves.
    val peerName = varchar("peer_name", 100).nullable()
    val createdAt = long("created_at")
}

// A global "I need SC" request board — replaces cedal-mobile's Firestore
// "trades" collection. Posting one also mirrors into Notifications so it
// shows up in the global feed; this table is the durable record.
object Trades : UUIDTable("trades") {
    val userId = reference("user_id", Users)
    val amount = integer("amount")
    val plea = varchar("plea", 280).nullable()
    val creatorName = varchar("creator_name", 100).nullable()
    val createdAt = long("created_at")
}

// Global feed (replaces Firestore "notifications") — currently only ever
// populated by Trades, same as cedal-mobile (the "system" type exists in
// the RN model but nothing writes one yet).
object Notifications : UUIDTable("notifications") {
    val type = varchar("type", 20) // "trade" | "system"
    val title = varchar("title", 200)
    val body = varchar("body", 500)
    val fromUserId = reference("from_user_id", Users).nullable()
    val fromUserName = varchar("from_user_name", 100).nullable()
    val tradeId = reference("trade_id", Trades).nullable()
    val amount = integer("amount").nullable()
    val createdAt = long("created_at")
}

// --- Invest simulator (real prices via MarketDataService/CoinGecko,
// simulated buy/sell against Users.virtualCashBalance). Replaces
// cedal-mobile's 100%-hardcoded Overview/Watchlist/Trade/Asset screens. ---

object PortfolioHoldings : UUIDTable("portfolio_holdings") {
    val userId = reference("user_id", Users)
    val assetType = varchar("asset_type", 10) // "crypto" | "stock" (stock not wired up yet)
    val symbol = varchar("symbol", 40) // CoinGecko coin id, e.g. "bitcoin"
    val quantity = double("quantity")
    val avgCostBasis = double("avg_cost_basis") // per-unit, in virtual USD
    val updatedAt = long("updated_at")
}

object PortfolioTransactions : UUIDTable("portfolio_transactions") {
    val userId = reference("user_id", Users)
    val assetType = varchar("asset_type", 10)
    val symbol = varchar("symbol", 40)
    val side = varchar("side", 4) // "buy" | "sell"
    val quantity = double("quantity")
    val pricePerUnit = double("price_per_unit")
    val totalValue = double("total_value")
    val createdAt = long("created_at")
}

object Watchlist : UUIDTable("watchlist") {
    val userId = reference("user_id", Users)
    val assetType = varchar("asset_type", 10)
    val symbol = varchar("symbol", 40)
    val createdAt = long("created_at")
}

// Kotlin -> real installable APK build jobs, via the android-builder Cloud
// Run service (a Gradle/Android SDK build server - too slow for a normal
// request/response, builds take 1-5+ minutes). android-builder updates
// status/downloadUrl/errorMessage via a callback once a build finishes; the
// client polls GET /code/android-build/{jobId} in the meantime. Replaces
// cedal-mobile's Firestore "androidBuilds" collection.
// One row per (user, lesson) - the unique index is what makes awarding exp
// idempotent: completing the same lesson twice (the client lets you toggle
// a lesson's checkbox back off and on) only ever awards once per account.
object LessonCompletions : Table("lesson_completions") {
    val userId = reference("user_id", Users)
    val lessonId = varchar("lesson_id", 200)
    val completedAt = long("completed_at")
    override val primaryKey = PrimaryKey(userId, lessonId)
}

object AndroidBuilds : UUIDTable("android_builds") {
    val userId = reference("user_id", Users)
    val status = varchar("status", 20).default("queued") // queued | building | done | error
    val downloadUrl = varchar("download_url", 500).nullable()
    // text (unbounded), not varchar - android-builder sends up to the last
    // 4000 chars of raw Gradle stderr on a build failure, which silently
    // 500'd every single time it exceeded this column's old 2000-char cap
    // (Exposed's schema migration doesn't retroactively widen an existing
    // column - see the one-time ALTER TABLE run alongside this change).
    val errorMessage = text("error_message").nullable()
    val createdAt = long("created_at")
}

// Live interactive GUI (Xvfb+VNC) sessions for the Code screen's Python
// runner - see GuiSessionService/gui-runner. gui-runner itself enforces one
// live session globally (containerConcurrency=1, max-instances=1), this row
// is just this account's history/status view of that.
object GuiSessions : UUIDTable("gui_sessions") {
    val userId = reference("user_id", Users)
    val status = varchar("status", 20).default("starting") // starting | active | ended | error
    val viewUrl = varchar("view_url", 500).nullable()
    val errorMessage = text("error_message").nullable()
    val createdAt = long("created_at")
}

// The Code screen's "Rules" tab AI - see AiChangeRequestService. Two shapes
// live in one row: a pure Q&A ("answered", answerText filled, nothing else
// ever happens) and a real code proposal ("pending_approval" through
// "deployed"/"rejected"/"error", filesJson + prUrl filled instead). Never a
// deletion - GitHubService (the only thing that ever touches GitHub for
// this) has no delete-capable method at all, so there's nothing here to
// gate beyond the approval step every row already goes through.
object AiChangeRequests : UUIDTable("ai_change_requests") {
    val requesterUserId = reference("requester_user_id", Users)
    val requestText = text("request_text")
    val status = varchar("status", 20).default("pending_judgment")
    val answerText = text("answer_text").nullable()
    val summary = text("summary").nullable()
    val filesJson = text("files_json").nullable()
    val prUrl = varchar("pr_url", 500).nullable()
    val errorMessage = text("error_message").nullable()
    // A third, unrelated shape alongside the two above: a direct action
    // against the user's OWN on-device Code area (create/edit/delete/run a
    // file), resolved the moment the judge sees it - never a GitHub PR,
    // never admin-approval-gated, since it only ever touches that one
    // user's own sandboxed files. Stored as JSON (action/path/content) and
    // executed client-side by MemberCodeBody, since the server has no
    // access to the on-device Storage Access Framework folder at all.
    val fileActionJson = text("file_action_json").nullable()
    // Reply-tagging, same no-enforced-FK philosophy as ChatMessages.replyToId
    // below - a plain string rather than a uuid() since it can point at
    // either half of a row: "{id}#user" or "{id}#assistant" (a row bundles
    // one whole user+assistant exchange, unlike AiMessages/ChatMessages
    // where every turn is its own row).
    val replyToId = varchar("reply_to_id", 120).nullable()
    // Soft-delete/edit for the user's own half of a row only - the
    // assistant's half (answerText/summary) is never user-editable.
    val requestDeleted = bool("request_deleted").default(false)
    val requestEditedAt = long("request_edited_at").nullable()
    // An attachment on the user's own request half of the row - mirrors
    // ChatMessages' mediaUrl/mediaType/fileName. An image here also gets
    // passed to the judge call as real vision input (see
    // AiProviderService.askWithImage); video/file are stored/rendered only.
    val mediaUrl = varchar("media_url", 500).nullable()
    val mediaType = varchar("media_type", 10).nullable()
    val fileName = varchar("file_name", 255).nullable()
    val createdAt = long("created_at")
}

// One row per turn of Corneal or ARC's Assistant chat - what makes those
// conversations survive navigating away or restarting the app, instead of
// living only in Compose `remember` state. The Code AI's own history reuses
// AiChangeRequests directly instead of a third lane here (requestText/
// answerText already *are* that turn-by-turn history).
object AiMessages : UUIDTable("ai_messages") {
    val userId = reference("user_id", Users)
    val assistant = varchar("assistant", 20) // "corneal" | "arc"
    val role = varchar("role", 10) // "user" | "assistant"
    val content = text("content")
    // Reply-tagging/edit/delete - same shape and rules as ChatMessages'
    // versions (edit only your own, within the window; delete leaves a
    // tombstone rather than erasing the row, same reasoning: reactions/
    // replies elsewhere may still reference this id).
    val replyToId = uuid("reply_to_id").nullable()
    val editedAt = long("edited_at").nullable()
    val deleted = bool("deleted").default(false)
    // Mirrors ChatMessages' mediaUrl/mediaType/fileName - an image here also
    // gets passed to the AI as real vision input on the turn it's sent (see
    // AiProviderService.askWithImage); video/file are stored/rendered only.
    val mediaUrl = varchar("media_url", 500).nullable()
    val mediaType = varchar("media_type", 10).nullable()
    val fileName = varchar("file_name", 255).nullable()
    val createdAt = long("created_at")
}

// Cross-chat-type pinning - a message can come from ChatMessages, AiMessages
// (corneal/arc), or AiChangeRequests (code, "{id}#user"/"{id}#assistant"),
// so this references it loosely by (chatType, messageId) rather than a real
// FK, same reasoning as every other loose message pointer in this file.
// chatKey is only meaningful for chatType "friend" - which friend's thread
// to jump back into; Corneal/ARC/Code AI each have exactly one chat per
// user, so there's nothing to disambiguate there.
object MessagePins : UUIDTable("message_pins") {
    val userId = reference("user_id", Users)
    val chatType = varchar("chat_type", 20) // "friend" | "corneal" | "arc" | "code"
    val messageId = varchar("message_id", 120)
    val chatKey = varchar("chat_key", 100).nullable()
    // Snapshot at pin time, same reasoning as MessageReports.messageText -
    // the pinned list renders like a real chat (sender + time + text)
    // without needing to cross-reference back into 4 different source
    // tables just to display it; won't reflect a later edit to the
    // original, same trade-off Reports already accepts.
    val messageText = text("message_text")
    val pinnedAt = long("pinned_at")
}

// A user-flagged message, reviewed only by the app's one admin account
// (see AdminService) - same lightweight "flag for review" shape as every
// other admin-gated queue in this app (AiChangeRequests' pending_approval,
// SystemFeedPosts). messageText is a snapshot at report time so a report
// stays reviewable even if the original is later edited or deleted.
object MessageReports : UUIDTable("message_reports") {
    val reporterUserId = reference("reporter_user_id", Users)
    val chatType = varchar("chat_type", 20)
    val messageId = varchar("message_id", 120)
    val messageText = text("message_text")
    val status = varchar("status", 20).default("pending") // pending | reviewed
    val createdAt = long("created_at")
}

// One AI-generated task per (area, date) - "area" is "invest" or "arc".
// Generated lazily on first request of the day (see DailyTaskService), not
// via a cron job - functionally daily either way, without needing a
// separate Cloud Scheduler entry. Never surfaced to the client as
// "AI-generated" - it's just presented as "Today's Task".
object DailyTasks : Table("daily_tasks") {
    val area = varchar("area", 20)
    val taskDate = varchar("task_date", 10) // ISO yyyy-MM-dd
    val title = varchar("title", 200)
    val description = text("description")
    val expReward = long("exp_reward")
    override val primaryKey = PrimaryKey(area, taskDate)
}

// Idempotent per (user, area, date) - same shape as LessonCompletions.
object DailyTaskCompletions : Table("daily_task_completions") {
    val userId = reference("user_id", Users)
    val area = varchar("area", 20)
    val taskDate = varchar("task_date", 10)
    val completedAt = long("completed_at")
    override val primaryKey = PrimaryKey(userId, area, taskDate)
}

// One row per ARC Ops "practice target" APK - built once via android-builder
// (fixed source we control, not user-submitted) and cached, so every player
// downloads the same APK instead of paying a fresh Gradle build per attempt.
object ArcPracticeApps : Table("arc_practice_apps") {
    val targetId = varchar("target_id", 100)
    val status = varchar("status", 20).default("queued") // queued | building | done | error
    val downloadUrl = varchar("download_url", 500).nullable()
    val errorMessage = text("error_message").nullable()
    val updatedAt = long("updated_at")
    override val primaryKey = PrimaryKey(targetId)
}
