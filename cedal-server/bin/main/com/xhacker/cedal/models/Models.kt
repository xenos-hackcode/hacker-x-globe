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
data class ForgotPasswordRequest(val email: String)

@Serializable
data class ResetPasswordRequest(val email: String, val code: String, val newPassword: String)

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
    // Real-money-bought progression (Shop's Tier system) - see RankService.
    val xp: Long,
    // Lesson-completion progression (Profile's Human-Godhood rank) - see
    // RankService and LessonService. Not the same currency as xp above.
    val exp: Long,
)

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
    val pollQuestion: String? = null,
    val pollOptions: List<String>? = null,
    // userId -> optionIndex, every vote so far - client tallies counts and
    // highlights the caller's own pick.
    val pollVotes: Map<String, Int> = emptyMap(),
)

@Serializable
data class SendChatMessageRequest(
    val text: String,
    val replyToId: String? = null,
    val isSticker: Boolean = false,
    val mediaUrl: String? = null,
    val mediaType: String? = null,
    val fileName: String? = null,
    val viewOnce: Boolean = false,
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
)

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
)

// Only ever sent when the client's Settings > Privacy > "Bot View" is on
// (see SecureStorage.botViewEnabled client-side) - what chat the user
// currently has open, so Corneal can help with what's on screen. Never
// stored beyond the single request it arrives with (see
// CornealChatService.reply) - not written into AiMessages, which only ever
// holds what Corneal actually said.
@Serializable
data class ChatContextDto(val friendName: String, val recentMessages: List<String>)

@Serializable
data class CornealChatRequest(
    val message: String,
    val replyToId: String? = null,
    val chatContext: ChatContextDto? = null,
    val mediaUrl: String? = null,
    val mediaType: String? = null,
    val fileName: String? = null,
)

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
