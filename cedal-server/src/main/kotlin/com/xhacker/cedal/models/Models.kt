package com.xhacker.cedal.models

import kotlinx.serialization.Serializable

@Serializable
data class SignupRequest(
    val email: String? = null,
    val password: String? = null,
    val guest: Boolean = false,
    // Must match AuthService.CURRENT_TERMS_VERSION, proving the client showed
    // the current terms text before signing up.
    val acceptedTermsVersion: String? = null,
    // Required when guest=true — enforces one active guest node per device.
    val deviceId: String? = null,
    // Required for non-guest signups (see AuthService.signup) - E.164 format
    // e.g. +14155551234. Enforces "one account per phone number" right at
    // account creation, not just as a later optional Settings add-on.
    val phoneNumber: String? = null,
    // Which channel delivers the signup verification code - "sms" or
    // "email", user's choice (see the popup on SignUpScreen). Required for
    // non-guest signups.
    val verifyVia: String? = null,
)

@Serializable
data class SignupResponse(
    val userId: String,
    val emailVerificationRequired: Boolean,
    val tokens: AuthTokens? = null,
    // Dev-only: no real email provider is wired up yet, so the verification
    // code is echoed back here instead of actually being emailed. Remove
    // once a real provider (Resend/SendGrid/etc.) is in place.
    val devVerificationCode: String? = null,
)

@Serializable
data class VerifyEmailRequest(val userId: String, val code: String)

@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class AuthTokens(val accessToken: String, val refreshToken: String, val userId: String, val role: String)

// login() no longer returns AuthTokens directly — if the account has
// two-factor verification on, a code must be confirmed first via
// /auth/login/2fa before tokens are issued.
@Serializable
data class LoginResponse(
    val requiresTwoFactor: Boolean = false,
    val userId: String? = null,
    val tokens: AuthTokens? = null,
    // Dev-only: no real email provider wired up yet, same pattern as signup.
    val devVerificationCode: String? = null,
)

@Serializable
data class TwoFactorLoginConfirmRequest(val userId: String, val code: String)

@Serializable
data class TwoFactorConfirmRequest(val code: String)

@Serializable
data class RefreshRequest(val refreshToken: String)

@Serializable
data class ForgotPasswordRequest(
    // At least one of email/phoneNumber identifies the account - user's
    // choice, since some people remember one but not the other (see
    // AuthService.forgotPassword).
    val email: String? = null,
    val phoneNumber: String? = null,
    // Extra identity proof beyond just owning the email/phone on file -
    // the account's own node passcode (see AuthService.forgotPassword).
    val passcode: String,
    // "sms" or "email" - user's choice of where the reset code goes.
    val verifyVia: String,
)

@Serializable
data class ResetPasswordRequest(
    val email: String? = null,
    val phoneNumber: String? = null,
    val code: String,
    val newPassword: String,
)

@Serializable
data class NodePasswordVerifyRequest(val userId: String, val code: String, val mode: String)

@Serializable
data class NodePasswordVerifyResponse(
    val success: Boolean,
    val role: String? = null,
    val locked: Boolean = false,
    val lockUntil: Long? = null,
    val failCount: Int = 0,
    val message: String? = null,
)

@Serializable
data class CreatePasscodeRequest(val userId: String, val code: String, val age: Int, val favoriteColor: String)

@Serializable
data class LogoutRequest(val refreshToken: String)

@Serializable
data class TermsUpdateRequest(val version: String)

@Serializable
data class UpdatePasscodeRequest(val code: String)

@Serializable
data class LinkEmailRequest(val email: String, val password: String)

@Serializable
data class UpdateProfileRequest(
    val nickname: String? = null,
    val handle: String? = null,
    val age: Int? = null,
    val occupation: String? = null,
    val hobby: String? = null,
    val bio: String? = null,
    val gender: String? = null,
    val avatarUrl: String? = null,
    val hideFromSearch: Boolean? = null,
    val preferredLanguage: String? = null,
)

@Serializable
data class UserProfile(
    val id: String,
    val email: String?,
    val isGuest: Boolean,
    val emailVerified: Boolean,
    val role: String,
    val nickname: String?,
    val publicId: String?,
    val handle: String?,
    val occupation: String?,
    val hobby: String?,
    val bio: String?,
    val gender: String?,
    val avatarUrl: String?,
    val age: Int?,
    val createdAt: Long,
    val acceptedTermsVersion: String?,
    val twoFactorEnabled: Boolean,
    val hideFromSearch: Boolean = false,
    // A display name from TranslationService.LANGUAGES (e.g. "French"), or
    // null for "no translation" - see ChatMessageDto.translatedText.
    val preferredLanguage: String? = null,
    // Real-money-bought progression (Shop's Tier system) - see RankService.
    val xp: Long,
    // Lesson-completion progression (Profile's Human-Godhood rank) - see
    // RankService and LessonService. Not the same currency as xp above.
    val exp: Long,
    // Settings > Security > Popularity - always true when viewing your own
    // profile; only ever false when a DIFFERENT viewer fetched this profile
    // and the owner has hidden that field (globally or for this one viewer -
    // see PopularityService.effectiveFor). Enforced server-side: when a flag
    // here is false, the corresponding field above (nickname/handle,
    // avatarUrl, age) is already redacted to null before this DTO is ever
    // built - these flags just tell the client WHY, so it can show a
    // "hidden" placeholder instead of a blank field.
    val nameVisible: Boolean = true,
    val pfpVisible: Boolean = true,
    val ageVisible: Boolean = true,
    val rankVisible: Boolean = true,
    val occupationVisible: Boolean = true,
    val hobbyVisible: Boolean = true,
    val bioVisible: Boolean = true,
    val genderVisible: Boolean = true,
    // Chat list > More > Achievements - "USE" on an unlocked achievement
    // sets this; null means no badge equipped.
    val activeBadgeKey: String? = null,
    // Developer mode delegation - see Users.developerAccess/developerKey.
    // hasActiveDeveloperKey never exposes the actual key value (only the
    // admin ever sees that, at generation time) - just whether one is
    // currently usable, so EnterPasscodeScreen knows whether to show the
    // passcode field or "ask the admin for a key".
    val developerAccess: Boolean = false,
    val hasActiveDeveloperKey: Boolean = false,
)

@Serializable
data class SetActiveBadgeRequest(val key: String?)

@Serializable
data class ErrorResponse(val error: String)

// --- Find friends (search + requests) ---

@Serializable
data class SearchUserResult(
    val id: String,
    val name: String,
    val email: String? = null,
    val avatarUrl: String? = null,
    val occupation: String? = null,
    val bio: String? = null,
    val gender: String? = null,
    val hobby: String? = null,
    val age: Int? = null,
)

@Serializable
data class FriendRequestCreate(val toUserId: String)

// ✚ > Search's contacts-match prompt - raw numbers straight off the device's
// contact list, in whatever format Android's ContactsContract handed back
// (see FriendService.matchContacts for how these get normalized).
@Serializable
data class ContactMatchRequest(val phoneNumbers: List<String>)

// Chat thread's proactive "this account has been deleted" check - see
// FriendService.friendStatus.
@Serializable
data class FriendStatusResult(val exists: Boolean, val isFriend: Boolean, val isCedalTeam: Boolean = false)

// ManageDeveloperAccessScreen's SEND KEY button - see
// DeveloperAccessService.sendKeyMessage.
@Serializable
data class SendDeveloperKeyRequest(val key: String)

// Developer Mode's submit -> review -> approve/deny -> deploy pipeline -
// see DeveloperSubmissionService and DeveloperSubmissions in Tables.kt.
@Serializable
data class SubmitDeveloperPatchRequest(val title: String, val targetFilePath: String, val code: String, val language: String)

@Serializable
data class DenyDeveloperSubmissionRequest(val reason: String)

@Serializable
data class DeveloperSubmissionDto(
    val id: String,
    val userId: String,
    val userName: String,
    val title: String,
    val targetFilePath: String,
    val language: String,
    val status: String,
    val stage1Result: String? = null,
    val stage2Result: String? = null,
    val deniedReason: String? = null,
    val prUrl: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
)

@Serializable
data class FriendRequestItem(
    val id: String,
    val fromUserId: String,
    val toUserId: String,
    val status: String, // pending | accepted | declined
    val direction: String, // outgoing | incoming (relative to the caller)
    val name: String,
    val email: String? = null,
    val avatarUrl: String? = null,
)

@Serializable
data class FriendSummary(
    val id: String,
    val name: String,
    val email: String? = null,
    val avatarUrl: String? = null,
)

// --- Chat (real 1-on-1 messaging between accepted friends) ---

@Serializable
data class ChatMessageDto(
    val id: String,
    val senderId: String,
    val receiverId: String,
    val text: String,
    val sentAt: Long,
    val editedAt: Long? = null,
    val deleted: Boolean = false,
    val replyToId: String? = null,
    // userId -> emoji, one reaction per user (see ChatMessageReactions).
    val reactions: Map<String, String> = emptyMap(),
    val isSticker: Boolean = false,
    val mediaUrl: String? = null,
    val mediaType: String? = null,
    val fileName: String? = null,
    val viewOnce: Boolean = false,
    val viewed: Boolean = false,
    // null (or "once") = classic single-reveal. "custom_time"/"custom_count"
    // - see ChatMessages.viewOnceMode's doc comment server-side.
    val viewOnceMode: String? = null,
    val viewOnceDurationMs: Long? = null,
    val viewOnceMaxViews: Int? = null,
    val viewOnceViewCount: Int = 0,
    val pollQuestion: String? = null,
    val pollOptions: List<String>? = null,
    // userId -> optionIndex, every vote so far - client tallies counts and
    // highlights the caller's own pick.
    val pollVotes: Map<String, Int> = emptyMap(),
    // Only ever set when sender/receiver have different preferredLanguage -
    // the recipient's client shows this INSTEAD of text; the sender always
    // sees their own original text unchanged. See TranslationService.
    val translatedText: String? = null,
    // Only meaningful for a message someone else sent you - see
    // ChatService.markRead/getMessages doc comments.
    val read: Boolean = true,
)

@Serializable
data class MarkReadRequest(val upToMessageId: String)

@Serializable
data class SendChatMessageRequest(
    val text: String,
    val replyToId: String? = null,
    val isSticker: Boolean = false,
    val mediaUrl: String? = null,
    val mediaType: String? = null,
    val fileName: String? = null,
    val viewOnce: Boolean = false,
    // See ChatMessages.viewOnceMode - null defaults to "once" when
    // viewOnce=true, matching the pre-existing single-reveal behavior.
    val viewOnceMode: String? = null,
    val viewOnceDurationMs: Long? = null,
    val viewOnceMaxViews: Int? = null,
    val pollQuestion: String? = null,
    val pollOptions: List<String>? = null,
)

@Serializable
data class VotePollRequest(val optionIndex: Int)

@Serializable
data class EditChatMessageRequest(val text: String)

@Serializable
data class ReactToMessageRequest(val emoji: String)

@Serializable
data class ConversationSummary(
    val friendId: String,
    val name: String,
    val email: String? = null,
    val avatarUrl: String? = null,
    val lastMessage: String? = null,
    val lastMessageAt: Long? = null,
    val lastMessageFromMe: Boolean? = null,
    val lastMessageViewOnce: Boolean = false,
    val unreadCount: Int = 0,
    val isSystemFeed: Boolean = false,
    // See ConversationState/ChatService bulk-action functions - archived
    // and hidden conversations are excluded from listConversations() by
    // default; the rest are just flags the client renders/sorts on.
    val pinned: Boolean = false,
    val muted: Boolean = false,
    val favorite: Boolean = false,
    val locked: Boolean = false,
)

@Serializable
data class BulkChatActionRequest(val friendIds: List<String>, val action: String)

@Serializable
data class ReportUserRequest(
    val reason: String? = null,
    val mediaUrl: String? = null,
    val mediaType: String? = null,
    val fileName: String? = null,
)

// Admin Review (chat list > More, admin-only) - the user-level counterpart
// to MessageInteractionService.ReportDto.
@Serializable
data class UserReportDto(
    val id: String,
    val reporterId: String,
    val reporterName: String,
    val reportedId: String,
    val reportedName: String,
    val reason: String?,
    val mediaUrl: String?,
    val mediaType: String?,
    val fileName: String?,
    val createdAt: Long,
)

// Live in-session ban/clear-data detection (see MemberScaffold's poll
// client-side) - distinct from the login-time "Invalid account" message,
// which never reveals any of this. Only a user who was actually logged in
// when it happened ever sees the gate. gated=true means "show the
// full-screen panel"; permanent=true means "no appeal option, just
// Cancel" - true immediately for Clear Data (nothing left to restore) or
// once a temp ban's 24h window has passed with no Unban.
@Serializable
data class AccountStatusDto(val gated: Boolean, val permanent: Boolean, val bannedAt: Long? = null)

// --- "Appeal" (full-screen gate shown to a banned/admin-cleared user) ---
// See Appeals table's doc comment.

@Serializable
data class SubmitAppealRequest(val email: String, val reason: String, val message: String)

@Serializable
data class AppealDto(
    val id: String,
    val email: String,
    val reason: String,
    val message: String,
    val status: String,
    val createdAt: Long,
)

// Client force-update gate - see AppVersionConfig's own doc comment.
@Serializable
data class AppVersionDto(val versionCode: Int, val versionName: String, val apkUrl: String? = null)

@Serializable
data class SetAppVersionRequest(val versionCode: Int, val versionName: String, val apkUrl: String? = null)

@Serializable
data class DeclineUpdateRequest(val versionCode: Int)

// --- SMS relay platform (multi-developer self-service signup) ---
// See PlatformDeveloperService/PlatformDevelopers.

@Serializable
data class PlatformSubmitVerificationRequest(
    // Opaque, server-issued proof of the GitHub OAuth callback - never the
    // raw githubId, so a client can't just supply an arbitrary identity and
    // skip GitHub auth. See PlatformOAuthSessions.
    val signupToken: String,
    val packageName: String,
    val email: String,
    val phone: String,
    val acceptedTerms: Boolean,
)

@Serializable
data class PlatformConfirmCodesRequest(
    val signupToken: String,
    val emailCode: String,
    val phoneCode: String,
)

@Serializable
data class PlatformSendSmsRequest(val phoneNumber: String, val message: String)

@Serializable
data class PlatformRegisterEmailRequest(
    val mode: String, // "own_smtp" | "shared"
    val host: String? = null,
    val port: String? = null,
    val username: String? = null,
    val password: String? = null,
    val from: String? = null,
)

@Serializable
data class PlatformSendEmailRequest(val to: String, val subject: String, val body: String)

// --- Achievements (chat list > More > Achievements) + rank-up popups ---
// See AchievementService/RankUpService/PendingPopupService.

@Serializable
data class AchievementDto(
    val key: String,
    val title: String,
    val bigWord: String,
    val body: String,
    val unlocked: Boolean,
    val unlockedAt: Long? = null,
)

// Settings > Security > Popularity - see PopularityService.
@Serializable
data class PopularitySettingsDto(
    val showName: Boolean = true,
    val showPfp: Boolean = true,
    val showAge: Boolean = true,
    val showRank: Boolean = true,
    val showOccupation: Boolean = true,
    val showHobby: Boolean = true,
    val showBio: Boolean = true,
    val showGender: Boolean = true,
)

// Per-chat override (null = "no override, defer to global") - see each chat
// thread's ⋮ menu > Popularity.
@Serializable
data class ChatPopularityOverrideDto(
    val showName: Boolean? = null,
    val showPfp: Boolean? = null,
    val showAge: Boolean? = null,
    val showRank: Boolean? = null,
    val showOccupation: Boolean? = null,
    val showHobby: Boolean? = null,
    val showBio: Boolean? = null,
    val showGender: Boolean? = null,
)

// See AchievementService.CLIENT_TRIGGERABLE for the allowlist of keys this
// accepts.
@Serializable
data class TriggerAchievementRequest(val key: String)

@Serializable
data class PendingPopupDto(
    val id: String,
    val kind: String, // "achievement" | "rank_up" | "rank_up_big"
    val title: String,
    val bigWord: String?,
    val body: String,
)

// --- Security: phone number verification ---

@Serializable
data class RequestPhoneCodeRequest(val phoneNumber: String)

@Serializable
data class RequestPhoneCodeResponse(val devVerificationCode: String? = null)

@Serializable
data class VerifyPhoneCodeRequest(val code: String)

@Serializable
data class PhoneStatusResponse(val phoneNumber: String? = null, val phoneVerified: Boolean = false)

// --- Bank / Star Coins wallet ---

@Serializable
data class WalletBalanceResponse(val balance: Int)

@Serializable
data class WalletSendRequest(val toUserId: String, val amount: Int)

@Serializable
data class WalletTransactionItem(
    val id: String,
    val type: String, // topup | credit_in | credit_out
    val amount: Int,
    val peerName: String? = null,
    val createdAt: Long,
)

// --- Trade / Notifications (global "request SC" board) ---

@Serializable
data class TradeCreateRequest(val amount: Int, val plea: String? = null)

@Serializable
data class NotificationItem(
    val id: String,
    val type: String, // trade | system
    val title: String,
    val body: String,
    val fromUserId: String? = null,
    val fromUserName: String? = null,
    val tradeId: String? = null,
    val amount: Int? = null,
    val createdAt: Long,
    val isMine: Boolean,
)

// --- Invest simulator (real prices, simulated trades) ---

@Serializable
data class MarketAssetSummary(
    val id: String, // CoinGecko coin id, e.g. "bitcoin"
    val symbol: String, // "BTC"
    val name: String, // "Bitcoin"
    val price: Double,
    val changePercent24h: Double,
)

@Serializable
data class PricePoint(val timestamp: Long, val price: Double)

@Serializable
data class MarketAssetDetail(
    val id: String,
    val symbol: String,
    val name: String,
    val price: Double,
    val changePercent24h: Double,
    val priceHistory: List<PricePoint>,
)

@Serializable
data class PortfolioHoldingItem(
    val symbol: String,
    val name: String,
    val quantity: Double,
    val avgCostBasis: Double,
    val currentPrice: Double,
    val currentValue: Double,
    val gainLossValue: Double,
    val gainLossPercent: Double,
)

@Serializable
data class PortfolioResponse(
    val cashBalance: Double,
    val holdings: List<PortfolioHoldingItem>,
    val totalHoldingsValue: Double,
    val totalPortfolioValue: Double,
)

@Serializable
data class TradeRequest(val symbol: String, val quantity: Double)

@Serializable
data class PortfolioTransactionItem(
    val id: String,
    val symbol: String,
    val side: String, // buy | sell
    val quantity: Double,
    val pricePerUnit: Double,
    val totalValue: Double,
    val createdAt: Long,
)

@Serializable
data class WatchlistItem(val symbol: String, val name: String, val price: Double, val changePercent24h: Double)

@Serializable
data class WatchlistAddRequest(val symbol: String)

@Serializable
data class CodeFile(val name: String, val content: String)

@Serializable
data class CodeRunRequest(
    val language: String,
    val code: String,
    val stdin: String = "",
    val extraFiles: List<CodeFile> = emptyList(),
    // Explicit opt-in package names (pip/npm/gem/composer) - see
    // CodeExecutionService/runner's installPackages. Most common packages
    // are already pre-baked into the runner image and need this to be
    // empty; this is only for anything not already covered.
    val packages: List<String> = emptyList(),
)

@Serializable
data class CodeRunResult(
    val stdout: String,
    val stderr: String,
    val exitCode: Int?,
    val compileOutput: String? = null,
    val language: String,
    val version: String,
)

@Serializable
data class CodeLanguageItem(val language: String, val version: String)

@Serializable
data class ExplainErrorRequest(val language: String, val errorText: String)

@Serializable
data class ExplainErrorResponse(val explanation: String)

@Serializable
data class AndroidBuildRequest(val code: String)

@Serializable
data class AndroidBuildJob(
    val jobId: String,
    val status: String, // "queued" | "building" | "done" | "error"
    val downloadUrl: String? = null,
    val errorMessage: String? = null,
)

@Serializable
data class AndroidBuildCallbackRequest(
    val status: String,
    val downloadUrl: String? = null,
    val errorMessage: String? = null,
)

@Serializable
data class GuiSessionRequest(val code: String)

@Serializable
data class GuiSessionJob(
    val sessionId: String,
    val status: String, // "starting" | "active" | "ended" | "error"
    val viewUrl: String? = null,
    val errorMessage: String? = null,
    // Cloud Run's session-affinity cookie(s) from gui-runner's own response
    // to /session/start - relayed as-is so the Android client can set them
    // on its WebView before loading viewUrl, otherwise its first request
    // could land on a different instance than the one actually running the
    // session (see GuiSessionService.kt).
    val affinityCookies: List<String> = emptyList(),
)

@Serializable
data class CompleteLessonRequest(val lessonId: String)

@Serializable
data class CompleteLessonResponse(val exp: Long)

@Serializable
data class BackerReviewRequest(val language: String, val code: String)

@Serializable
data class BackerReviewResponse(
    val hasIssue: Boolean,
    val reason: String = "",
    val fixedCode: String = "",
)

@Serializable
data class ArcChatMessageDto(
    val id: String,
    val role: String,
    val content: String,
    val createdAt: Long,
    val replyToId: String? = null,
    val editedAt: Long? = null,
    val deleted: Boolean = false,
    val mediaUrl: String? = null,
    val mediaType: String? = null,
    val fileName: String? = null,
)

// message replaces the old full messages: List<ArcChatMessageDto> body -
// history now lives server-side (see AiChatHistoryService) and is appended
// to on every turn, rather than the client resending the whole
// conversation each time.
@Serializable
data class ArcChatRequest(val message: String, val replyToId: String? = null, val mediaUrl: String? = null, val mediaType: String? = null, val fileName: String? = null)

@Serializable
data class ArcChatResponse(val message: ArcChatMessageDto)

@Serializable
data class ArcChatHistoryResponse(val messages: List<ArcChatMessageDto>)

// Alucard - the developer-mode-only security/code-review chatbot (see
// AlucardChatService). Same "flatten history, one text reply" shape as
// Arc/Corneal, kept as its own types for the same reason Corneal's are kept
// separate from Arc's - lets the three diverge later without a shared name
// getting confusing.
@Serializable
data class AlucardChatMessageDto(
    val id: String,
    val role: String,
    val content: String,
    val createdAt: Long,
    val replyToId: String? = null,
    val editedAt: Long? = null,
    val deleted: Boolean = false,
    val mediaUrl: String? = null,
    val mediaType: String? = null,
    val fileName: String? = null,
)

@Serializable
data class AlucardChatRequest(val message: String, val replyToId: String? = null, val mediaUrl: String? = null, val mediaType: String? = null, val fileName: String? = null)

@Serializable
data class AlucardChatResponse(val message: AlucardChatMessageDto)

@Serializable
data class AlucardChatHistoryResponse(val messages: List<AlucardChatMessageDto>)

// Corneal - the app-wide help assistant reachable from Chats (see
// CornealChatService). Same shape as Arc's chat DTOs since it's the same
// "flatten history, one text reply" pattern - kept as its own types rather
// than reusing Arc's so the two features can diverge later without a
// confusing shared name.
@Serializable
data class CornealChatMessageDto(
    val id: String,
    val role: String,
    val content: String,
    val createdAt: Long,
    val replyToId: String? = null,
    val editedAt: Long? = null,
    val deleted: Boolean = false,
    val mediaUrl: String? = null,
    val mediaType: String? = null,
    val fileName: String? = null,
    // "Call Out" (Settings > Corneal AI > Call Out, text-based) - present
    // only when Corneal spotted a specific issue in the user's currently
    // open code file (see CallOutService/CornealChatService's CALLOUT_
    // tag parsing). callOutSnippet is the exact substring Corneal believes
    // is the problem - the client locates and circles/highlights it in the
    // Code editor. callOutFixRequested means Corneal is offering to hand
    // the fix to Code AI if the user confirms.
    val callOutSnippet: String? = null,
    val callOutNote: String? = null,
    val callOutFixRequested: Boolean = false,
)

// Only ever sent when the client's Settings > Privacy > "Bot View" is on
// (see SecureStorage.botViewEnabled client-side) - what chat the user
// currently has open, so Corneal can help with what's on screen. Never
// stored beyond the single request it arrives with (see
// CornealChatService.reply) - not written into AiMessages, which only ever
// holds what Corneal actually said.
@Serializable
data class ChatContextDto(val friendName: String, val recentMessages: List<String>)

// Only ever sent when Settings > Corneal AI > "Call Out" (text-based) is on
// AND the user currently has a file open in Code - same off-by-default
// pattern as ChatContextDto above.
@Serializable
data class CodeContextDto(val path: String, val content: String)

// Sent with every Corneal message so it answers about the user's ACTUAL
// current toggle states instead of generic scripted defaults (e.g. telling
// someone to "turn on Bot View" when it's already on) - these all live in
// client-only SecureStorage, the server has no other way to know them.
@Serializable
data class SettingsSnapshotDto(
    val botView: Boolean,
    val cornealHider: Boolean,
    val botAccess: Boolean,
    val callOutText: Boolean,
    val callOutScreenCapture: Boolean,
    val offlineMode: Boolean,
    val biometricEnabled: Boolean,
    val appLockEnabled: Boolean,
)

@Serializable
data class CornealChatRequest(
    val message: String,
    val replyToId: String? = null,
    val chatContext: ChatContextDto? = null,
    val codeContext: CodeContextDto? = null,
    val settingsSnapshot: SettingsSnapshotDto? = null,
    val mediaUrl: String? = null,
    val mediaType: String? = null,
    val fileName: String? = null,
)

// "Call Out" - the user said no, this isn't the spot / not what they meant
// (see MemberCodeScreen's circle/highlight confirm-deny UI). Persisted so
// CornealChatService can tell the model not to re-suggest the same snippet
// in this file unless the user explicitly asks again.
@Serializable
data class RejectCallOutRequest(val filePath: String, val snippet: String)

@Serializable
data class CornealChatResponse(val message: CornealChatMessageDto)

@Serializable
data class CornealChatHistoryResponse(val messages: List<CornealChatMessageDto>)

@Serializable
data class EditAiMessageRequest(val content: String)

// Cedal System Feed - see SystemFeedService for the admin-only-posting rule.
@Serializable
data class SystemFeedPostDto(
    val id: String,
    val authorName: String,
    val text: String,
    val createdAt: Long,
    val reactions: Map<String, String> = emptyMap(),
)

@Serializable
data class CreateFeedPostRequest(val text: String)

@Serializable
data class ReactToFeedPostRequest(val emoji: String)

// See ImageUploadService/StickerService - a two-step flow: upload the bytes
// (kind="avatar" or "sticker") to get a URL back, then for stickers, a
// separate POST /stickers with that URL actually creates the owned row.
// Avatars don't need a second step - updateProfile already accepts a plain
// avatarUrl string.
@Serializable
data class UploadImageResponse(val url: String)

@Serializable
data class StickerDto(val id: String, val imageUrl: String, val createdAt: Long)

@Serializable
data class CreateStickerRequest(val imageUrl: String)

// Cosmetic accent-color packs, purchasable with Star Coins - see
// ThemePackService.CATALOG for the fixed list this mirrors.
@Serializable
data class ThemePackDto(val id: String, val name: String, val priceSC: Int, val accentHex: String, val owned: Boolean)

@Serializable
data class ThemePackPurchaseResponse(val newBalance: Int)

@Serializable
data class ArcMissionPrompt(
    val text: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String,
    val secondsVisible: Int,
)

@Serializable
data class ArcMission(
    val scenario: String,
    val prompts: List<ArcMissionPrompt>,
)

@Serializable
data class ArcMissionRequest(val targetName: String)

@Serializable
data class ArcMissionCompleteRequest(val scorePercent: Int)

@Serializable
data class ArcMissionCompleteResponse(val exp: Long, val expAwarded: Long)

@Serializable
data class DailyTaskResponse(
    val title: String,
    val description: String,
    val expReward: Long,
    val completed: Boolean,
)

@Serializable
data class DailyTaskCompleteResponse(val exp: Long)

@Serializable
data class ArcPracticeAppStatusResponse(
    val status: String,
    val downloadUrl: String? = null,
    val errorMessage: String? = null,
)
