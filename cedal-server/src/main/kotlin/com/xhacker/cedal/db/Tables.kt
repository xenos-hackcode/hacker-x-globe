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
    // The one well-known "Cedal System" account a group's creatorId can be
    // reassigned to when its Creator leaves with nobody left to hand it to
    // (see GroupChatService.ensureSystemAccountId/leaveGroup) - null
    // email/passwordHash like any other row here already means it can never
    // log in, so no separate "can't authenticate" flag is needed. Lazily
    // inserted on first use, same convention as everything else in this
    // codebase; excluded from friend search/1-on-1 chat/friend requests.
    val isSystemAccount = bool("is_system_account").default(false)
    // Personal privacy toggles. dmClosed/noTag are self-contained to the new
    // group-member "Message" button + group tag feature (see Groups.dmClosedByCreator
    // /GroupMembers.dmOverride and GroupMessages.taggedUserId) - they don't
    // touch the existing flat friend/1-on-1 ChatService at all, deliberately
    // kept out of scope to avoid retrofitting a system that isn't group-aware.
    val dmClosed = bool("dm_closed").default(false)
    val noTag = bool("no_tag").default(false)
    // "Hider" - distinct from noTag above (which blocks being tagged AT
    // ALL). This controls whether THIS user allows a #tag pointing at them
    // to be sent as a PRIVATE/hidden tag. Default true (permissive, matches
    // this codebase's other opt-OUT privacy defaults). When false, anyone
    // can still tag this user, but GroupChatService.sendGroupMessage
    // silently downgrades that whole message to public instead of honoring
    // the composer's hide choice - see its own doc comment for the exact
    // multi-tag precedence rule (Round-4 feedback).
    val hiderEnabled = bool("hider_enabled").default(true)
    // "Known" calling (Call tab / DM & Group profile Call button) - opt-in,
    // off by default, matching this codebase's privacy-first posture for
    // anything that discloses real account data. Global default; a specific
    // friend can be overridden either way via PhoneShareOverrides, same
    // global+per-friend shape as PopularitySettings/ChatPopularityOverrides.
    // Deliberately does NOT touch Users.phoneNumber's existing owner-only
    // access rule anywhere else (SecurityService's /phone routes) - this
    // only ever governs whether CallService.canCall hands the number back
    // inside a UserProfile/GroupMemberDto for a specific viewer.
    val shareNumberDefault = bool("share_number_default").default(false)
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
    // A display name from TranslationService.LANGUAGES (e.g. "French") -
    // when set and different from the other side of a chat, ChatService
    // auto-translates each message for the recipient. Null = no
    // translation (messages pass through as typed), same as before this
    // feature existed.
    val preferredLanguage = varchar("preferred_language", 20).nullable()
    val createdAt = long("created_at")
    val lastSeen = long("last_seen").nullable()
    // Permanent record of the last time this account dismissed the update
    // banner (see AppVersionConfig/UpdateGateScreen.kt) - an audit trail,
    // not a preference: there's deliberately no route that ever clears
    // these, only AccountService.setDeclinedUpdate() which overwrites them
    // with the CURRENT decline. Visible to the admin via Godmode.
    val declinedUpdateVersionCode = integer("declined_update_version_code").nullable()
    val declinedUpdateAt = long("declined_update_at").nullable()
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
    // Settings > More > Security - "1 account per phone number" (see
    // PhoneVerifications below for the pending-code flow that sets this).
    // Only VERIFIED numbers are unique-indexed; a still-pending claim in
    // PhoneVerifications doesn't block anyone else from also trying it.
    val phoneNumber = varchar("phone_number", 20).nullable().uniqueIndex()
    val phoneVerified = bool("phone_verified").default(false)
    // Godmode (chat list > More, admin-only) "Ban" - blocks sign-in without
    // deleting anything, reversible (unlike Godmode's separate "Clear Data"
    // action, which reuses AccountService.deleteAccount's real, permanent
    // wipe). Checked in AuthService.login.
    val banned = bool("banned").default(false)
    // When the current ban started - null whenever banned=false. Drives the
    // 24h temp-ban -> permanent-ban escalation (see BanEscalationService).
    val bannedAt = long("banned_at").nullable()
    // A temp ban (banned=true, banPermanent=false) auto-escalates to
    // permanent 24h after bannedAt unless Unban happens first (see
    // BanEscalationService) - Godmode's new "Permanent Ban" button skips
    // straight to true, no 24h grace/appeal window. Permanent is a one-way
    // door: Unban still restores login, but never resets this back to
    // false - a once-permanent identity stays flagged even if unbanned
    // later (matches the "never be able to be used again" ban permanence
    // already enforced via BannedIdentities).
    val banPermanent = bool("ban_permanent").default(false)
    // Chat list > More > Achievements - "USE" on an unlocked achievement
    // sets it here; shown as a small badge next to the name on Profile (see
    // AchievementService.setActiveBadge, which validates the key is
    // actually unlocked before allowing this to be set).
    val activeBadgeKey = varchar("active_badge_key", 40).nullable()
    // Developer mode delegation (see DeveloperAccessService) - granted only
    // by the app owner (hackerxenos06@gmail.com), who has their own fixed
    // "cedalstar" passcode and doesn't need any of this. A delegated
    // account with this on sees the DEVELOPER chip on EnterPasscodeScreen,
    // but still needs a live developerKey below to actually get in.
    val developerAccess = bool("developer_access").default(false)
    // One-time, admin-issued - null means "no usable key right now" (never
    // issued yet, or already spent entering developer mode once). The
    // owner has to generate a fresh one every single time a delegated
    // account wants back in - see DeveloperAccessService.generateKey /
    // AuthService.verifyNodePassword's consume-on-use logic.
    val developerKey = varchar("developer_key", 12).nullable()
}

// Real SMS delivery via a phone the app owner controls, running its own SIM
// card, instead of Twilio - see SmsRelayService. cedal-server just queues a
// job here; a small standalone companion app ("cedal-sms-relay") installed
// on that phone polls for pending rows, sends the real text via
// SmsManager, then reports back. sentAt null = still waiting for the relay
// phone to pick it up.
object PendingSmsJobs : UUIDTable("pending_sms_jobs") {
    val phoneNumber = varchar("phone_number", 20)
    val message = varchar("message", 500)
    val createdAt = long("created_at")
    val sentAt = long("sent_at").nullable()
}

// SMS relay platform - lets OTHER developers run their own instance of the
// "cedal-sms-relay" app on their own phone/carrier plan instead of every
// deployment funneling through the owner's one device. See
// PlatformDeveloperService for the GitHub-OAuth signup + split-key
// activation flow. Deliberately separate from Users - a platform developer
// isn't a Cedal end-user (no chat/profile/friends), just an API tenant.
object PlatformDevelopers : UUIDTable("platform_developers") {
    val githubId = varchar("github_id", 40).uniqueIndex()
    val githubLogin = varchar("github_login", 100)
    val packageName = varchar("package_name", 200).uniqueIndex()
    // Email/phone are only ever used raw in-flight (to send the
    // verification code / key-half) - only their BCrypt hashes are
    // persisted, matching AuthService's password/refresh-token convention.
    // There is deliberately no way to recover the raw value later.
    val emailHash = varchar("email_hash", 60)
    val phoneHash = varchar("phone_hash", 60)
    // BCrypt hash of the full (both-halves-combined) activation key - the
    // raw key is shown/sent exactly once during signup and never stored.
    val keyHash = varchar("key_hash", 60)
    val acceptedTermsAt = long("accepted_terms_at")
    val createdAt = long("created_at")
    // Owner-side kill switch - checked inside verifyDeveloperToken itself,
    // so flipping this blocks a developer from EVERY platform endpoint
    // (SMS relay + email) uniformly, not just one surface. No admin UI yet
    // - v1 is a direct DB update.
    val suspended = bool("suspended").default(false)
}

// A developer's OPTIONAL email-sending setup - only exists once they've
// registered via /platform/email/register. "own_smtp" fields are populated
// (AES-GCM encrypted - see CryptoService, since unlike everything else in
// this table's parent, the server must actually RECOVER these to use them,
// not just verify a guess) only when mode == "own_smtp"; "shared" mode
// needs no credentials of its own, just this row's existence + mode.
object PlatformEmailCredentials : Table("platform_email_credentials") {
    val developerId = reference("developer_id", PlatformDevelopers)
    val mode = varchar("mode", 20) // "own_smtp" | "shared"
    val smtpHostEnc = varchar("smtp_host_enc", 500).nullable()
    val smtpPortEnc = varchar("smtp_port_enc", 500).nullable()
    val smtpUsernameEnc = varchar("smtp_username_enc", 500).nullable()
    val smtpPasswordEnc = varchar("smtp_password_enc", 500).nullable()
    val smtpFromEnc = varchar("smtp_from_enc", 500).nullable()
    override val primaryKey = PrimaryKey(developerId)
}

// Shared-mode-only throttle - a fixed rolling window (see
// PlatformEmailService's cooldown/cap logic), same read-check-update-in-
// one-transaction shape as AuthService's LockoutState. Necessary because
// shared mode sends through the OWNER's own SMTP account - unlike SMS
// (each developer's own phone, own blast radius), unlimited shared email
// volume risks getting the owner's whole sending domain blacklisted.
object PlatformEmailRateLimit : Table("platform_email_rate_limit") {
    val developerId = reference("developer_id", PlatformDevelopers)
    val windowStart = long("window_start")
    val countInWindow = integer("count_in_window").default(0)
    override val primaryKey = PrimaryKey(developerId)
}

// Audit trail for every email send attempt, either mode - deliberately NOT
// hashed (unlike the signup email/phone) since this is the developer's own
// outbound traffic, not their identity, and needs to stay traceable if
// there's ever an abuse complaint to investigate.
object PlatformEmailSends : UUIDTable("platform_email_sends") {
    val developerId = reference("developer_id", PlatformDevelopers)
    val mode = varchar("mode", 20)
    val toAddress = varchar("to_address", 255)
    val subject = varchar("subject", 200)
    val sentAt = long("sent_at")
    val success = bool("success")
}

// Short-lived proof that a browser actually completed GitHub OAuth as a
// specific githubId - the callback page hands the CLIENT only this opaque
// token, never the raw githubId, so a malicious client can't just POST an
// arbitrary githubId to /verify/submit and skip GitHub auth entirely. See
// PlatformDeveloperService.createOAuthSession/resolveOAuthSession.
object PlatformOAuthSessions : Table("platform_oauth_sessions") {
    val token = varchar("token", 64)
    val githubId = varchar("github_id", 40)
    val githubLogin = varchar("github_login", 100)
    val expiresAt = long("expires_at")
    override val primaryKey = PrimaryKey(token)
}

// A developer's own scoped SMS queue - identical shape to PendingSmsJobs
// but keyed to one PlatformDevelopers row, so SmsRelayRoutes can serve each
// developer's relay app only ITS OWN traffic (see that file's platform-
// token branch). The legacy PendingSmsJobs queue is untouched by this -
// still owner-only, still Cedal's own internal verification flows.
object PlatformSmsJobs : UUIDTable("platform_sms_jobs") {
    val developerId = reference("developer_id", PlatformDevelopers)
    val phoneNumber = varchar("phone_number", 20)
    val message = varchar("message", 500)
    val createdAt = long("created_at")
    val sentAt = long("sent_at").nullable()
}

// Holds a platform signup between "submitVerification" (sends both codes)
// and "completeSignup" (both codes confirmed) - keyed by githubId since
// there's no Users row yet to hang this off of. email/phone are raw here
// ONLY transiently - this whole row is deleted once signup completes or
// expires, same lifecycle as VerificationCodes' userId+purpose rows.
object PendingPlatformSignups : Table("pending_platform_signups") {
    val githubId = varchar("github_id", 40)
    val githubLogin = varchar("github_login", 100)
    val packageName = varchar("package_name", 200)
    val email = varchar("email", 255)
    val phone = varchar("phone", 20)
    val emailCode = varchar("email_code", 6)
    val phoneCode = varchar("phone_code", 6)
    val acceptedTermsAt = long("accepted_terms_at")
    // Throttles "resend" - see PlatformDeveloperService's cooldown check -
    // so a script hammering this endpoint can't spam the OWNER'S OWN phone
    // with texts (each submit sends one via the legacy relay).
    val lastSentAt = long("last_sent_at")
    val expiresAt = long("expires_at")
    override val primaryKey = PrimaryKey(githubId)
}

// Pending phone-verification codes - separate from VerificationCodes (email)
// since a phone claim shouldn't touch Users.phoneNumber (and its unique
// index) until the code is actually confirmed. One row per user - a new
// request for a different number just overwrites the old pending one.
object PhoneVerifications : Table("phone_verifications") {
    val userId = reference("user_id", Users)
    val phoneNumber = varchar("phone_number", 20)
    val code = varchar("code", 6)
    val expiresAt = long("expires_at")
    override val primaryKey = PrimaryKey(userId)
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

// Per-(viewer, friend) conversation state - archive/pin/mute/favorite/lock/
// hide (see ChatService's bulk-action functions and the chat list's
// long-press multi-select). One row per pair, upserted as needed; a pair
// with no row here just means every flag defaults false, same as never
// having touched any of these. Deliberately NOT shared with the other
// side of the conversation - archiving a chat only affects your own list.
object ConversationState : UUIDTable("conversation_state") {
    val userId = reference("user_id", Users)
    val friendId = reference("friend_id", Users)
    val archived = bool("archived").default(false)
    val pinned = bool("pinned").default(false)
    val muted = bool("muted").default(false)
    val favorite = bool("favorite").default(false)
    // Requires biometric/passcode to actually open the thread - see
    // MemberHomeScreen.kt's tap handler, same verification mechanism as
    // Switch Account's AccountVerifyOverlay.
    val locked = bool("locked").default(false)
    val hidden = bool("hidden").default(false)
    // "Clear Data" - per-viewer only, matching pin/mute/etc (see
    // ChatService.bulkAction "clear"/"delete"). Messages sent by either side
    // at or before this cutoff are hidden from THIS user's history/preview/
    // unread-count only - the other person's copy of the conversation is
    // completely untouched, unlike the old (wrong) behavior which hard-
    // deleted the shared ChatMessages rows for both sides.
    val clearedAt = long("cleared_at").nullable()
}

// One row per (blocker, blocked) pair. A blocked user can no longer send the
// blocker new messages (ChatService.sendMessage) and disappears from the
// blocker's own chat list (ChatService.listConversations) - one-sided, the
// blocked user isn't notified and still sees the conversation on their end
// until they also try to send and get rejected.
object Blocks : UUIDTable("blocks") {
    val blockerId = reference("blocker_id", Users)
    val blockedId = reference("blocked_id", Users)
}

// User-level reports (distinct from MessageReports, which flags one specific
// message) - "Report" in the chat thread's ⋮ menu, for reporting the person/
// conversation as a whole rather than a single message.
object UserReports : UUIDTable("user_reports") {
    val reporterId = reference("reporter_id", Users)
    val reportedId = reference("reported_id", Users)
    val createdAt = long("created_at")
    // Optional free-text explanation and a single evidence attachment
    // (photo/video/file) - same mediaUrl/mediaType/fileName shape as
    // ChatMessages, uploaded through the same /uploads flow first.
    val reason = text("reason").nullable()
    val mediaUrl = text("media_url").nullable()
    val mediaType = varchar("media_type", 20).nullable()
    val fileName = varchar("file_name", 255).nullable()
    // Same pending/reviewed convention as MessageReports - Admin Review
    // (chat list > More) only ever shows pending ones.
    val status = varchar("status", 20).default("pending")
}

// Member > More > Bots' "character sheet" form (MemberBotsScreen.kt),
// ported from cedal-mobile's bots.tsx - previously a UI-only stub whose
// handleSave() never persisted anywhere. Round 1 of the Bots/"Leo"
// bot-builder platform: just owner-scoped CRUD for the character sheet
// itself. secretToken/telegramToken/whatsappAccessToken/userApiKey are
// credentials - BotService/BotRoutes must never include them in a list
// response, only on create or an explicit reveal. freeTokensUsed/isPremium
// are reserved for Round 2/4 (the brain endpoint + quota gate) - nothing
// reads or writes them yet.
object Bots : UUIDTable("bots") {
    val ownerUserId = reference("owner_user_id", Users)
    val name = varchar("name", 100)
    val age = integer("age").nullable()
    val gender = varchar("gender", 50).nullable()
    val character = text("character")
    val personality = text("personality")
    val bio = text("bio")
    val occupation = varchar("occupation", 200).nullable()
    val lifeStory = text("life_story").nullable()
    val description = text("description")
    val iconUrl = varchar("icon_url", 500).nullable()
    // "telegram" | "whatsapp" | "both" - which platform credential fields
    // below are expected to be filled in.
    val botType = varchar("bot_type", 20)
    val telegramToken = varchar("telegram_token", 200).nullable()
    val whatsappPhoneNumberId = varchar("whatsapp_phone_number_id", 100).nullable()
    val whatsappAccessToken = varchar("whatsapp_access_token", 500).nullable()
    // Server-generated on create (same UUID-no-dashes convention as
    // Groups.inviteToken) - what Round 2's self-hosted generated bot code
    // authenticates its /bots/{id}/converse calls with. Never returned in a
    // list response.
    val secretToken = varchar("secret_token", 40).uniqueIndex()
    val freeTokensUsed = integer("free_tokens_used").default(0)
    val isPremium = bool("is_premium").default(false)
    // BYOK override - user's own LLM provider key, used instead of the
    // shared key once set, regardless of freeTokensUsed/isPremium.
    val userApiKey = varchar("user_api_key", 500).nullable()
    // "self" (default, free - user downloads and runs their own bot code)
    // | "cedal" (premium-gated - cedal-server itself registers the Telegram
    // webhook / receives the shared WhatsApp webhook and replies directly).
    // Only settable to "cedal" while isPremium is true - see BotService.
    val hostingMode = varchar("hosting_mode", 20).default("self")
    val createdAt = long("created_at")
    val updatedAt = long("updated_at")
}

// Round 2 - the brain endpoint's conversation memory. Deliberately NOT
// AiChatHistoryService/AiMessages' (userId, assistant) shape - a bot talks
// to arbitrary external chat partners (a Telegram user, a WhatsApp number),
// not a Cedal Users row, so this is keyed by (botId, chatId) instead, where
// chatId is whatever opaque per-conversation identifier the calling
// platform client passes (a Telegram chat id, a WhatsApp phone number, or
// the owner's own userId for the in-app test-chat path) - one independent
// conversation thread per (bot, external party) pair.
object BotConversationTurns : UUIDTable("bot_conversation_turns") {
    val botId = reference("bot_id", Bots)
    val chatId = varchar("chat_id", 200)
    val role = varchar("role", 10) // "user" | "assistant"
    val content = text("content")
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

    // "View Once" - viewedAt is set the first time the RECIPIENT actually
    // reveals this message; once fully consumed (see viewOnceMode below),
    // both sides see it stripped (see ChatService.toDto) - same one-time-
    // reveal idea as Snapchat/WhatsApp's view-once, applied to text
    // messages too, not just media.
    val viewOnce = bool("view_once").default(false)
    val viewedAt = long("viewed_at").nullable()
    // null when viewOnce is false. "once" = the original behavior (gone
    // after a single reveal). "custom_time" = stays revealable until
    // viewOnceDurationMs after the FIRST reveal (viewedAt), then locks for
    // good. "custom_count" = revealable up to viewOnceMaxViews times total
    // (viewOnceViewCount tracks how many so far), then locks for good.
    val viewOnceMode = varchar("view_once_mode", 20).nullable()
    val viewOnceDurationMs = long("view_once_duration_ms").nullable()
    val viewOnceMaxViews = integer("view_once_max_views").nullable()
    val viewOnceViewCount = integer("view_once_view_count").default(0)

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

    // Auto-translation into the RECIPIENT's preferredLanguage, computed once
    // at send time (see ChatService.sendMessage/TranslationService) - null
    // when languages match or either side hasn't set one. The sender's own
    // `text` column is never touched; only the recipient's client ever
    // prefers this field over `text` when rendering.
    val translatedText = text("translated_text").nullable()
}

// Ephemeral "X is typing" pings (see TypingService) - a plain DB table
// rather than in-memory state since cedal-server can run more than one
// instance; freshness is enforced by `updatedAt` rather than an explicit
// "stopped typing" signal (simpler, and survives the sender's app being
// killed mid-type without leaving a stale "typing…" stuck on forever).
object TypingStatus : UUIDTable("typing_status") {
    val userId = reference("user_id", Users)
    val friendId = reference("friend_id", Users)
    val updatedAt = long("updated_at")
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

// Group chat - deliberately a fully separate schema from ChatMessages/
// friend chat rather than a retrofit (nullable receiverId etc.) - see
// GroupChatService's own doc comment. A bug here can't touch 1-on-1 chat.
// creatorId is kept as the original-owner record, but live authority lives
// in GroupMembers.role - see GroupChatService for the CREATOR/VICE_CREATOR/
// ADMIN/MEMBER hierarchy and how ownership transfers when the creator
// leaves. who*Setting columns are "ALL" | "ADMINS_ONLY", admin-tier being
// ADMIN+VICE_CREATOR+CREATOR - mirrors the WhatsApp/Snapchat/TikTok group-
// settings split between admin-only and everyone-editable settings.
object Groups : UUIDTable("groups") {
    val name = varchar("name", 100)
    val creatorId = reference("creator_id", Users)
    val avatarUrl = varchar("avatar_url", 500).nullable()
    val description = varchar("description", 500).nullable()
    // who*Setting columns hold a RANK, not a binary flag - "MEMBER" |
    // "ADMIN" | "VICE_CREATOR" | "CREATOR" - see GroupChatService.roleRank.
    // A setting of "ADMIN" means ADMIN and everyone above it (VICE_CREATOR,
    // CREATOR) qualifies, "MEMBER" means everyone. Generalizes what used to
    // be a plain ALL/ADMINS_ONLY binary.
    val whoCanSendMessages = varchar("who_can_send_messages", 20).default("MEMBER")
    // Round 5: unlike the other 4 rank settings, this one can only ever be
    // VICE_CREATOR or CREATOR - see GroupChatService.updateGroupSettings'
    // validation. Default matches (existing rows created before this stay
    // whatever they were - no retroactive migration).
    val whoCanEditInfo = varchar("who_can_edit_info", 20).default("VICE_CREATOR")
    val whoCanAddMembers = varchar("who_can_add_members", 20).default("ADMIN")
    val whoCanSeeGroupStats = varchar("who_can_see_group_stats", 20).default("MEMBER")
    val whoCanSendMedia = varchar("who_can_send_media", 20).default("MEMBER")
    // "Add Members" sub-setting - whether a newly-added member sees message
    // history from before they joined, or starts from a clean slate (see
    // GroupChatService.getGroupMessages' history filter). Admin-tier always
    // sees everything regardless of this flag.
    val shareHistoryWithNewMembers = bool("share_history_with_new_members").default(true)
    // Discoverable via group search (GroupChatService.searchPublicGroups) -
    // joining goes through GroupJoinRequests instead of an instant add.
    val isPublic = bool("is_public").default(false)
    // The ONE group-wide pinned message (Discord/Telegram-style, admin-tier
    // sets it for everyone) - deliberately separate from the existing
    // per-user personal message pin (PinnedMessagesBody/MessagePins), which
    // is unrelated and untouched by this. pinnedByRole records the rank of
    // whoever pinned it, since unpinning requires rank >= that (see
    // GroupChatService.unpinMessage) - a plain Admin can't undo a Creator's pin.
    val pinnedMessageId = uuid("pinned_message_id").nullable()
    val pinnedByRole = varchar("pinned_by_role", 20).nullable()
    // WhatsApp "Advanced Chat Privacy" equivalent, scoped to what this app
    // actually has: disables the Save-to-SavedMessages action and forces
    // FLAG_SECURE for the whole thread client-side (see
    // GroupChatThreadScreen.kt) even with no view-once content present.
    val securedMode = bool("secured_mode").default(false)
    // null = off. When set, GroupChatService.getGroupMessages lazily purges
    // any non-kept message older than this on every fetch - same
    // "opportunistic cleanup on read" idea view-once purge already uses,
    // not a background job (this codebase has no job scheduler).
    val disappearingMessagesDurationMs = long("disappearing_messages_duration_ms").nullable()
    // CSV of setting keys (e.g. "whoCanSendMessages,whoCanAddMembers") that
    // are locked - see GroupChatService.updateGroupSettings. Only rank >=
    // VICE_CREATOR can add/remove an entry here; while a key is present, that
    // ONE setting also requires rank >= VICE_CREATOR to change (narrower
    // than the normal ADMIN_TIER gate on every other setting).
    val lockedSettings = varchar("locked_settings", 300).nullable()
    // Separate from `description` - shown once to a new member (client-side
    // "seen" tracking, same as chat lock) and always readable from Group
    // Profile. Edited under the same whoCanEditInfo gate as description.
    val rules = varchar("rules", 1000).nullable()
    // null = off. Creator-only. Absolute timestamp - GroupChatService lazily
    // tears the group down (deleteGroupFully) once past this, opportunistically
    // on getGroup/listMyGroupSummaries, same "no job scheduler" convention as
    // disappearingMessagesDurationMs above.
    val autoDeleteAt = long("auto_delete_at").nullable()
    // Creator-only. When true, nobody can use the group-member "Message"
    // button to DM anyone else in this group, period - overrides even a
    // member's own dmOverride="OPEN" (see GroupMembers.dmOverride and
    // GroupChatService's canDm precedence: personal Users.dmClosed > this >
    // per-member dmOverride).
    val dmClosedByCreator = bool("dm_closed_by_creator").default(false)
    // "Known" (native-dialer) group calling - Creator-only (narrower than
    // the Vice-Creator-can-too pattern most other locks use, per the user's
    // explicit ask). Off just hides/disables the Call button in Group
    // Profile for everyone including the Creator; doesn't affect the
    // per-member DM/call permissions those calls still rely on.
    val callsEnabled = bool("calls_enabled").default(true)
    // Round 5 "Link" tab - only ever shown/meaningful for isPublic groups
    // (private groups keep the existing direct-add-by-friend flow, no link
    // at all). Lazily generated on first need (see
    // GroupChatService.ensureInviteToken), regenerated by "Reset Link"
    // (invalidating the old one) - see resetInviteLink.
    val inviteToken = varchar("invite_token", 40).nullable().uniqueIndex()
    val createdAt = long("created_at")
}

// role is "CREATOR" | "VICE_CREATOR" | "ADMIN" | "MEMBER" - see
// GroupChatService for the kick/promote permission matrix. Exactly one
// CREATOR and at most one VICE_CREATOR per group at any time.
object GroupMembers : Table("group_members") {
    val groupId = reference("group_id", Groups)
    val userId = reference("user_id", Users)
    val role = varchar("role", 20).default("MEMBER")
    val joinedAt = long("joined_at")
    override val primaryKey = PrimaryKey(groupId, userId)
}

// Mirrors ChatMessages' shape for every v1-in-scope feature (text, media,
// stickers, polls, replies, edit/delete), plus view-once fields mirroring
// ChatMessages' (viewOnce/viewOnceMode/viewOnceDurationMs/viewOnceMaxViews).
// Unlike 1-on-1 chat, per-member view state (viewedAt/viewCount) can't live
// on the message row since a group has many recipients - see
// GroupMessageViews below.
object GroupMessages : UUIDTable("group_messages") {
    val groupId = reference("group_id", Groups)
    val senderId = reference("sender_id", Users)
    val text = text("text")
    val sentAt = long("sent_at")
    val replyToId = uuid("reply_to_id").nullable()
    val editedAt = long("edited_at").nullable()
    val deleted = bool("deleted").default(false)
    val isSticker = bool("is_sticker").default(false)
    val mediaUrl = varchar("media_url", 500).nullable()
    val mediaType = varchar("media_type", 10).nullable()
    val fileName = varchar("file_name", 255).nullable()
    val pollQuestion = varchar("poll_question", 500).nullable()
    val pollOptions = text("poll_options").nullable()
    val viewOnce = bool("view_once").default(false)
    val viewOnceMode = varchar("view_once_mode", 20).nullable()
    val viewOnceDurationMs = long("view_once_duration_ms").nullable()
    val viewOnceMaxViews = integer("view_once_max_views").nullable()
    // Exempts this message from Groups.disappearingMessagesDurationMs'
    // lazy-purge sweep - WhatsApp's "Keep" action equivalent.
    val kept = bool("kept").default(false)
    // Tag metadata on a regular message, not a separate message type. A
    // message can tag MULTIPLE specific users at once (e.g. "#mike #leo") -
    // CSV of UUIDs, same convention as Groups.lockedSettings - each renders
    // with a "#" badge client-side; a broadcast tag (tagAll=true,
    // taggedUserIds empty) renders with "@" and is never private.
    // tagPrivate (only meaningful alongside a non-empty taggedUserIds)
    // permanently hides text/media from every viewer except the sender and
    // whichever tagged users are actually in taggedUserIds - see
    // GroupChatService.toDto's tag-hide computation. Unlike View Once this
    // has no reveal/consume/expiry and no FLAG_SECURE - a tagged viewer can
    // screenshot freely. tagPrivate is only ever true if EVERY tagged user
    // has Users.hiderEnabled - see sendGroupMessage's downgrade logic.
    val taggedUserIds = varchar("tagged_user_ids", 2000).nullable()
    val tagAll = bool("tag_all").default(false)
    val tagPrivate = bool("tag_private").default(false)
    // Client-supplied at upload time (the app already has the full byte
    // array in memory before uploading - see GroupChatThreadScreen.kt's
    // galleryPicker/filePicker) - same trust level as mediaType/fileName
    // above, not independently verified server-side. Powers Group Profile's
    // real per-type Media & Storage byte totals instead of just a row count.
    val mediaSizeBytes = long("media_size_bytes").nullable()
    // Per-message disappearing (Round 5) - independent of Groups.
    // disappearingMessagesDurationMs (the group-wide admin setting).
    // disappearSelfOnly=false ("For Everyone"): purged for real once past
    // disappearAt, same sweep as the group-wide one (see
    // purgeExpiredGroupMessages). disappearSelfOnly=true ("Custom"): the row
    // is NEVER deleted - GroupChatService.getGroupMessages just filters it
    // out of the result for the SENDER specifically once expired, so
    // everyone else keeps seeing it normally.
    val disappearAt = long("disappear_at").nullable()
    val disappearSelfOnly = bool("disappear_self_only").default(false)
}

object GroupMessageReactions : Table("group_message_reactions") {
    val messageId = reference("message_id", GroupMessages)
    val userId = reference("user_id", Users)
    val emoji = varchar("emoji", 16)
    override val primaryKey = PrimaryKey(messageId, userId)
}

object GroupPollVotes : Table("group_poll_votes") {
    val messageId = reference("message_id", GroupMessages)
    val userId = reference("user_id", Users)
    val optionIndex = integer("option_index")
    override val primaryKey = PrimaryKey(messageId, userId)
}

// Per-member view-once reveal state for group messages - see
// ChatMessages.viewedAt/viewOnceViewCount for the 1-on-1 equivalent that
// lives directly on the message row (fine there since there's exactly one
// recipient). GroupChatService.revealGroupMessage/purgeConsumedGroupViewOnce
// are the read/write paths.
object GroupMessageViews : Table("group_message_views") {
    val messageId = reference("message_id", GroupMessages)
    val userId = reference("user_id", Users)
    val viewedAt = long("viewed_at")
    val viewCount = integer("view_count").default(0)
    override val primaryKey = PrimaryKey(messageId, userId)
}

// Pending join for a public group (Groups.isPublic) - requesting to join
// doesn't add the member immediately, it waits here for an admin-tier
// approve/reject (see GroupChatService.requestToJoin/approveJoinRequest).
// Private groups never touch this table - they keep the existing
// direct-add-by-a-friend flow unchanged.
object GroupJoinRequests : Table("group_join_requests") {
    val groupId = reference("group_id", Groups)
    val userId = reference("user_id", Users)
    val requestedAt = long("requested_at")
    override val primaryKey = PrimaryKey(groupId, userId)
}

// Whole-group reports - exact mirror of UserReports' shape, just aimed at a
// Groups row instead of a Users row. "Report Group" in Group Profile.
object GroupReports : UUIDTable("group_reports") {
    val reporterId = reference("reporter_id", Users)
    val groupId = reference("group_id", Groups)
    val createdAt = long("created_at")
    val reason = text("reason").nullable()
    val mediaUrl = text("media_url").nullable()
    val mediaType = varchar("media_type", 20).nullable()
    val fileName = varchar("file_name", 255).nullable()
    val status = varchar("status", 20).default("pending")
}

// One row per (user, group) that user has blocked - "Block Group" in Group
// Profile. Nobody can add or approve-join this user into that group again
// (see GroupChatService.addMember/approveJoinRequest) - not a retroactive
// kick, and one-sided (only affects future adds against the blocker).
object BlockedGroups : Table("blocked_groups") {
    val userId = reference("user_id", Users)
    val groupId = reference("group_id", Groups)
    val blockedAt = long("blocked_at")
    override val primaryKey = PrimaryKey(userId, groupId)
}

// The group-keyed sibling of ConversationState (that table's FK is to
// Users/friendId, not Groups, so it can't just be reused directly). Only
// `muted` for now - group chat has no archive/pin/hide-conversation v1
// scope yet (see GroupChatService's own doc comment), and the group-wide
// pinned message above is a completely different, unrelated concept from a
// personal "pin this conversation to the top of my list" flag.
object GroupConversationState : Table("group_conversation_state") {
    val userId = reference("user_id", Users)
    val groupId = reference("group_id", Groups)
    val muted = bool("muted").default(false)
    // This member's own override of Groups.dmClosedByCreator's default for
    // themself specifically in THIS group - "OPEN" | "CLOSED" | null (=
    // follow the group default). Only takes effect when the group itself
    // isn't creator-closed - see GroupChatService.canDm.
    val dmOverride = varchar("dm_override", 10).nullable()
    override val primaryKey = PrimaryKey(userId, groupId)
}

// Round-5 anti-spam rule: after being kicked (by Creator/Vice-Creator) or
// leaving voluntarily, a user can't be re-added/re-approved into that same
// group again until availableAt - see GroupChatService.removeMember (which
// writes this) and addMember/approveJoinRequest (which check it).
object GroupRejoinCooldowns : Table("group_rejoin_cooldowns") {
    val groupId = reference("group_id", Groups)
    val userId = reference("user_id", Users)
    val availableAt = long("available_at")
    override val primaryKey = PrimaryKey(groupId, userId)
}

// "Saved Messages" - a personal conversation-with-yourself (WhatsApp/
// Telegram equivalent), deliberately NOT built on ChatMessages/ChatService
// (those assume a real two-party accepted friendship and would need an
// awkward self-friendship special case). A message is copied in here via
// the "Save" action on a group message (see SavedMessagesService.save) -
// this table has no sender/reactions/polls/view-once, it's a personal log,
// not a real chat. Hidden entirely when the source group has securedMode on.
object SavedMessages : UUIDTable("saved_messages") {
    val userId = reference("user_id", Users)
    // e.g. "From Weekend Trip" (the group name at save time) - denormalized
    // so it still reads correctly even if the source group is later renamed,
    // left, or deleted.
    val sourceLabel = varchar("source_label", 200).nullable()
    val text = text("text")
    val mediaUrl = varchar("media_url", 500).nullable()
    val mediaType = varchar("media_type", 10).nullable()
    val fileName = varchar("file_name", 255).nullable()
    val savedAt = long("saved_at")
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

// Code screen "Documents" <-> GitHub sync (CodeGithubSyncService) - a
// per-user link to the user's OWN GitHub account/repo, unrelated to
// GitHubService's app-repo-only, no-user-tokens flow used by the Rules tab.
// One row per user; accessTokenEnc is AES-GCM via CryptoService, the same
// reversible-encryption capability PlatformEmailCredentials already uses for
// a developer's SMTP password - this is the same shape of problem (must
// actually USE the secret later, not just verify a guess).
object CodeGithubConnections : Table("code_github_connections") {
    val userId = reference("user_id", Users)
    val githubId = varchar("github_id", 40)
    val githubLogin = varchar("github_login", 100)
    val accessTokenEnc = varchar("access_token_enc", 500)
    val selectedOwner = varchar("selected_owner", 200).nullable()
    val selectedRepo = varchar("selected_repo", 200).nullable()
    val selectedBranch = varchar("selected_branch", 200).nullable().default("main")
    val connectedAt = long("connected_at")
    override val primaryKey = PrimaryKey(userId)
}

// Short-lived state->userId mapping for the code-sync GitHub OAuth leg -
// same purpose as PlatformOAuthSessions (proves a callback belongs to a
// specific pending request) but this flow's authorize-url call is made by
// an already-JWT-authenticated app user, so this maps straight to a real
// Users row instead of an opaque post-OAuth identity handshake. The
// callback itself (routes/PlatformRoutes.kt) is unauthenticated - this row
// is the only thing that ties it back to the right user. Single-use,
// deleted the moment the callback resolves it.
object PendingCodeGithubOAuth : Table("pending_code_github_oauth") {
    val state = varchar("state", 64)
    val userId = reference("user_id", Users)
    val expiresAt = long("expires_at")
    override val primaryKey = PrimaryKey(state)
}

// The entire conflict-detection mechanism for two-way sync: one row per
// (user, repo-relative file path) recording that file's fingerprint as of
// the last successful sync. "Changed since last sync" = the file's current
// state (local content hash, or GitHub blob sha) no longer matches what's
// stored here. No row for a path = never synced (treated as a null
// baseline on both sides). Deliberately just a last-known-good fingerprint,
// not a full history/CRDT - see CodeGithubSyncService's own doc comment.
object CodeSyncFiles : Table("code_sync_files") {
    val userId = reference("user_id", Users)
    val filePath = varchar("file_path", 1000)
    val lastSyncedSha = varchar("last_synced_sha", 64).nullable()
    val lastSyncedContentHash = varchar("last_synced_content_hash", 64).nullable()
    val lastSyncedAt = long("last_synced_at")
    override val primaryKey = PrimaryKey(userId, filePath)
}

// A whole-folder sync is many sequential GitHub API round trips - too slow
// for one blocking mobile HTTP request, so the client starts a job here and
// polls it, same "kick off work, poll a status row" shape as AndroidBuilds/
// GuiSessions above. The *Json columns hold small JSON arrays (paths, or
// {path, localContent, remoteContent} for conflicts) - not worth a whole
// separate table each for what's always read/written as one blob per job.
object CodeSyncJobs : UUIDTable("code_sync_jobs") {
    val userId = reference("user_id", Users)
    val status = varchar("status", 20).default("running") // running | done | error
    val totalFiles = integer("total_files").default(0)
    val processedFiles = integer("processed_files").default(0)
    val pushedJson = text("pushed_json").nullable()
    val pulledJson = text("pulled_json").nullable()
    val deletedRemoteJson = text("deleted_remote_json").nullable()
    val deletedLocalJson = text("deleted_local_json").nullable()
    val conflictsJson = text("conflicts_json").nullable()
    val errorMessage = text("error_message").nullable()
    val createdAt = long("created_at")
    val updatedAt = long("updated_at")
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
    // Set once the client has actually performed the local file operation -
    // fileActionJson alone only records what SHOULD happen. Without this,
    // navigating away from the Code AI chat mid-generation (which cancels
    // the client-side coroutine that was going to invoke it) left rows
    // permanently stuck claiming "Created x.py" in the chat while nothing
    // was ever written on-device. refreshHistory() checks this flag on
    // every fetch (including a fresh mount after navigating back, or even
    // a cold app restart) and executes any not-yet-applied action then.
    val fileActionExecuted = bool("file_action_executed").default(false)
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
    // Speech-to-text of a mediaType="audio" voice note - AI-internal only,
    // never returned in AiChangeRequestDto/rendered to any user. requestText
    // itself stays exactly what the user typed (often blank for a pure
    // voice send) - see AiChangeRequestService.process().
    val transcript = text("transcript").nullable()
    val createdAt = long("created_at")
}

// Developer Mode's submit -> Alucard review -> owner approve/deny -> deploy
// pipeline (see DeveloperSubmissionService) - deliberately a separate,
// parallel table/service from AiChangeRequests above, not a generalization
// of it, so this doesn't risk destabilizing the existing (already fragile -
// see DeployService's own doc comment) language-proposal flow. status
// walks: pending_stage1 -> stage1_failed | pending_stage2 -> stage2_failed |
// pending_approval -> approved | denied. A submission never advances past a
// failed stage - the developer has to fix it and submit again as a new row.
object DeveloperSubmissions : UUIDTable("developer_submissions") {
    val userId = reference("user_id", Users)
    val title = varchar("title", 200)
    // Which real file in cedal-server this patches, and its full new
    // content - see the plan's own note on why "explicit target path + full
    // file" was chosen over AI-inferred insertion points for v1.
    val targetFilePath = varchar("target_file_path", 500)
    val code = text("code")
    val language = varchar("language", 40)
    val status = varchar("status", 20).default("pending_stage1")
    val stage1Result = text("stage1_result").nullable()
    val stage2Result = text("stage2_result").nullable()
    val deniedReason = text("denied_reason").nullable()
    val prUrl = varchar("pr_url", 500).nullable()
    val createdAt = long("created_at")
    val updatedAt = long("updated_at")
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
    // Speech-to-text of a mediaType="audio" voice note - AI-internal only,
    // never returned to the client or rendered as a chat bubble. content
    // itself stays exactly what the user typed (often blank for a pure
    // voice send) - see CornealChatService/ArcChatService.reply().
    val transcript = text("transcript").nullable()
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

// Godmode "Ban" (see AdminService.setBanned) - permanent, separate from
// Users.banned (which only blocks THIS account's login and is reversible via
// Unban). Recording the identity here means even if the banned account is
// later deleted/cleared, the same email can never sign up again and the
// same phone number can never be verified onto ANY account again - per the
// app owner's own words, banning is meant to be permanent for the identity,
// not just the one account row.
object BannedIdentities : UUIDTable("banned_identities") {
    val email = varchar("email", 255).nullable()
    val phoneNumber = varchar("phone_number", 20).nullable()
    val bannedAt = long("banned_at")
}

// A banned/admin-cleared user has no valid session (see AuthService.login's
// ACCOUNT_BANNED/ACCOUNT_CLEARED sentinels + the client's full-screen gate
// panel) - "Appeal" on that panel posts here unauthenticated, identified by
// the email they tried to log in with rather than a userId.
object Appeals : UUIDTable("appeals") {
    val email = varchar("email", 255)
    val reason = varchar("reason", 20) // "banned" | "cleared"
    val message = varchar("message", 2000)
    val status = varchar("status", 20).default("pending") // pending | reviewed
    val createdAt = long("created_at")
}

// Godmode "Clear Data" (see AdminService/AccountService.deleteAccount) -
// unlike a user's own self-service Delete Account, an admin-initiated clear
// leaves this tombstone so a later login attempt with the same email can
// show "Admin has deleted your account" instead of a generic "invalid email
// or password" (the Users row itself is gone once deleteAccount runs, same
// cascade as self-delete - this is the only trace left behind, and only for
// the admin-triggered path).
object AdminClearedIdentities : UUIDTable("admin_cleared_identities") {
    val email = varchar("email", 255).nullable()
    val phoneNumber = varchar("phone_number", 20).nullable()
    val clearedAt = long("cleared_at")
}

// Client force-update gate (see AppVersionService) - a single row (id
// always "current"), set by the admin (POST /admin/app-version) each time a
// real release goes out. The client polls this, shows a persistent "update
// now" toast once it's outdated, and after 2 weeks with no update, blocks
// sign-in/sign-up entirely until the user actually installs the newer
// build - see cedal-android's UpdateGateState/SecureStorage tracking
// (entirely client-side from here; the server only ever hands back "what's
// current").
object AppVersionConfig : Table("app_version_config") {
    val id = varchar("id", 20)
    val versionCode = integer("version_code")
    val versionName = varchar("version_name", 20)
    val apkUrl = varchar("apk_url", 500).nullable()
    // "What's new" shown alongside the update prompt (About screen, force-
    // update gate) - set via Admin > App Updates, see AppUpdatePublishScreen
    // client-side. text (unbounded) since this is free-form release notes,
    // not a short label.
    val changelog = text("changelog").nullable()
    val updatedAt = long("updated_at")
    override val primaryKey = PrimaryKey(id)
}

// Achievements (chat list > More > Achievements) - the catalog of titles/big
// words/bodies lives in code (AchievementService.CATALOG plus dynamically
// generated rank-up entries), this table only records which keys a given
// user has unlocked so the same key can never fire twice - see
// AchievementService.unlock's idempotency check.
object UserAchievements : UUIDTable("user_achievements") {
    val userId = reference("user_id", Users)
    val key = varchar("key", 40)
    val title = varchar("title", 200)
    val bigWord = varchar("big_word", 40)
    val body = varchar("body", 500)
    val unlockedAt = long("unlocked_at")
}

// Generic queued popup delivery, shared by achievement unlocks and rank-up
// notifications (the "balloon" popup / "Congrats on reaching X"). Queued
// instead of pushed directly so a popup queued while the user is offline
// still surfaces next time they open the app (see
// PendingPopupService.pollPending) - delivered exactly once, marked via
// deliveredAt so a retried poll never redelivers the same row.
// Settings > Security > "Popularity" - lets a user filter which of their own
// profile fields (name, pfp, age, rank) other people can see. Global,
// applies to every viewer unless a ChatPopularityOverride below says
// otherwise for that specific viewer. One row per user, upserted as needed -
// a user with no row here just means every field defaults visible (true),
// same as never having touched this setting.
object PopularitySettings : UUIDTable("popularity_settings") {
    val userId = reference("user_id", Users).uniqueIndex()
    val showName = bool("show_name").default(true)
    val showPfp = bool("show_pfp").default(true)
    val showAge = bool("show_age").default(true)
    val showRank = bool("show_rank").default(true)
    val showOccupation = bool("show_occupation").default(true)
    val showHobby = bool("show_hobby").default(true)
    val showBio = bool("show_bio").default(true)
    val showGender = bool("show_gender").default(true)
}

// Per-(profile owner, specific viewer) Popularity override - the same idea
// as PopularitySettings but scoped to one chat partner (see each chat
// thread's ⋮ menu > Popularity), taking precedence over the owner's global
// PopularitySettings for that one viewer only. Nullable columns mean "no
// override for this field, defer to global" - not simply false, so a
// per-chat entry can selectively override just one field while leaving the
// rest to the global setting.
object ChatPopularityOverrides : UUIDTable("chat_popularity_overrides") {
    val userId = reference("user_id", Users) // the profile owner
    val friendId = reference("friend_id", Users) // the specific viewer this override applies to
    val showName = bool("show_name").nullable()
    val showPfp = bool("show_pfp").nullable()
    val showAge = bool("show_age").nullable()
    val showRank = bool("show_rank").nullable()
    val showOccupation = bool("show_occupation").nullable()
    val showHobby = bool("show_hobby").nullable()
    val showBio = bool("show_bio").nullable()
    val showGender = bool("show_gender").nullable()
}

// Per-(number owner, specific friend) override of Users.shareNumberDefault -
// same shape/precedence idea as ChatPopularityOverrides above ("Known"
// calling, see that column's own doc comment). A row's existence IS the
// override (`allowed` true always shares with this friend even if the
// global default is off; false always withholds even if it's on) - see
// CallService.canCall/setOverride. Removed entirely (not left as a null
// row) when a user picks "use my default" again.
object PhoneShareOverrides : UUIDTable("phone_share_overrides") {
    val userId = reference("user_id", Users)
    val friendId = reference("friend_id", Users)
    val allowed = bool("allowed")
}

// "Call Out" (Settings > Corneal AI > Call Out, text-based) - the user
// tapped "No, that's not it" on a snippet Corneal circled/highlighted in the
// Code editor (see CallOutService). Recorded so CornealChatService's prompt
// can tell the model not to re-suggest the exact same snippet in that same
// file unless the user explicitly asks again.
object CallOutRejectedSpans : UUIDTable("call_out_rejected_spans") {
    val userId = reference("user_id", Users)
    val filePath = varchar("file_path", 500)
    val snippet = varchar("snippet", 500)
    val rejectedAt = long("rejected_at")
}

object PendingPopups : UUIDTable("pending_popups") {
    val userId = reference("user_id", Users)
    val kind = varchar("kind", 20) // "achievement" | "rank_up" | "rank_up_big"
    val title = varchar("title", 200)
    val bigWord = varchar("big_word", 40).nullable()
    val body = varchar("body", 500)
    val createdAt = long("created_at")
    val deliveredAt = long("delivered_at").nullable()
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
