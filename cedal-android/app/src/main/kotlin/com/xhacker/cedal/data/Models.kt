package com.xhacker.cedal.data

import kotlinx.serialization.Serializable

@Serializable
data class SignupRequest(
    val email: String? = null,
    val password: String? = null,
    val guest: Boolean = false,
    val acceptedTermsVersion: String? = null,
    val deviceId: String? = null,
    // Required for non-guest signups - see SignUpScreen's phone field +
    // verification-channel popup.
    val phoneNumber: String? = null,
    val verifyVia: String? = null,
)

@Serializable
data class SignupResponse(
    val userId: String,
    val emailVerificationRequired: Boolean,
    val tokens: AuthTokens? = null,
    val devVerificationCode: String? = null,
)

@Serializable
data class VerifyEmailRequest(val userId: String, val code: String)

@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class AuthTokens(val accessToken: String, val refreshToken: String, val userId: String, val role: String)

@Serializable
data class LoginResponse(
    val requiresTwoFactor: Boolean = false,
    val userId: String? = null,
    val tokens: AuthTokens? = null,
    val devVerificationCode: String? = null,
)

@Serializable
data class TwoFactorLoginConfirmRequest(val userId: String, val code: String)

@Serializable
data class TwoFactorConfirmRequest(val code: String)

@Serializable
data class TwoFactorSetupResponse(val devVerificationCode: String? = null)

@Serializable
data class RefreshRequest(val refreshToken: String)

@Serializable
data class ForgotPasswordRequest(
    val email: String? = null,
    val phoneNumber: String? = null,
    val passcode: String,
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
data class ErrorResponse(val error: String)

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
    val dmClosed: Boolean? = null,
    val noTag: Boolean? = null,
    val hiderEnabled: Boolean? = null,
    val shareNumberDefault: Boolean? = null,
    val denyAllCalls: Boolean? = null,
    val denyNonFriendCalls: Boolean? = null,
    val denyUnknownCallers: Boolean? = null,
    val autoMuteNewGroups: Boolean? = null,
    val mentionsOnlyDefault: Boolean? = null,
    val autoPinOwnedGroups: Boolean? = null,
    val requireGroupAddApproval: Boolean? = null,
)

@Serializable
data class SetNumberShareOverrideRequest(val allowed: Boolean? = null)

@Serializable
data class NumberShareOverrideResponse(val allowed: Boolean? = null)

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
    val preferredLanguage: String? = null,
    val dmClosed: Boolean = false,
    val noTag: Boolean = false,
    val hiderEnabled: Boolean = true,
    // Settings > Privacy > "Share My Number" - this account's own setting,
    // regardless of viewer.
    val shareNumberDefault: Boolean = false,
    // Settings > Call - see server-side Users.denyAllCalls/
    // denyNonFriendCalls/denyUnknownCallers doc comments.
    val denyAllCalls: Boolean = false,
    val denyNonFriendCalls: Boolean = false,
    val denyUnknownCallers: Boolean = false,
    // Settings > Groups - see server-side Users.autoMuteNewGroups/
    // mentionsOnlyDefault/autoPinOwnedGroups/requireGroupAddApproval.
    val autoMuteNewGroups: Boolean = false,
    val mentionsOnlyDefault: Boolean = false,
    val autoPinOwnedGroups: Boolean = false,
    val requireGroupAddApproval: Boolean = false,
    // Whether the REQUESTING viewer can currently see/call this profile's
    // real phone number - true for your own profile. phoneNumber is only
    // ever non-null when this is true.
    val canCall: Boolean = true,
    val phoneNumber: String? = null,
    // Real-money-bought progression (Shop's Tier system) - see RankTable in
    // MemberShopScreen.kt.
    val xp: Long = 0,
    // Lesson-completion progression (Profile's Human-Godhood rank) - see
    // RankTable in MemberProfileScreen.kt. Not the same currency as xp.
    val exp: Long = 0,
    // Settings > Security > Popularity - only ever false when viewing
    // someone ELSE's profile and they've hidden that field for you (see
    // AuthService.getProfile server-side). Always true on your own profile.
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
    // Developer mode delegation - see EnterPasscodeScreen.
    val developerAccess: Boolean = false,
    val hasActiveDeveloperKey: Boolean = false,
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

@Serializable
data class ArcChatRequest(val message: String, val replyToId: String? = null, val mediaUrl: String? = null, val mediaType: String? = null, val fileName: String? = null)

@Serializable
data class ArcChatResponse(val message: ArcChatMessageDto)

@Serializable
data class ArcChatHistoryResponse(val messages: List<ArcChatMessageDto>)

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
    // "Call Out" (Settings > Corneal AI, text-based) - see CornealChatService
    // server-side for the CALLOUT_ tag format this is parsed from.
    val callOutSnippet: String? = null,
    val callOutNote: String? = null,
    val callOutFixRequested: Boolean = false,
)

@Serializable
data class ChatContextDto(val friendName: String, val recentMessages: List<String>)

@Serializable
data class CodeContextDto(val path: String, val content: String)

// Sent with every Corneal message so it answers about the user's ACTUAL
// current toggle states instead of generic scripted defaults - see
// CornealChatService's use of this server-side.
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

@Serializable
data class RejectCallOutRequest(val filePath: String, val snippet: String)

@Serializable
data class CornealChatResponse(val message: CornealChatMessageDto)

@Serializable
data class CornealChatHistoryResponse(val messages: List<CornealChatMessageDto>)

@Serializable
data class EditAiMessageRequest(val content: String)

@Serializable
data class EditAiRequestTextBody(val text: String)

@Serializable
data class MessagePinDto(val id: String, val chatType: String, val messageId: String, val chatKey: String? = null, val messageText: String, val pinnedAt: Long)

@Serializable
data class PinMessageRequest(val chatType: String, val messageId: String, val chatKey: String? = null, val messageText: String)

@Serializable
data class ReportMessageRequest(val chatType: String, val messageId: String, val messageText: String)

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

@Serializable
data class UploadImageResponse(val url: String)

@Serializable
data class StickerDto(val id: String, val imageUrl: String, val createdAt: Long)

@Serializable
data class CreateStickerRequest(val imageUrl: String)

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
data class ArcPracticeAppStatusResponse(
    val status: String,
    val downloadUrl: String? = null,
    val errorMessage: String? = null,
)

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
data class ContactMatchRequest(val phoneNumbers: List<String>)

@Serializable
data class FriendStatusResult(val exists: Boolean, val isFriend: Boolean, val isCedalTeam: Boolean = false)

@Serializable
data class SendDeveloperKeyRequest(val key: String)

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
    val status: String,
    val direction: String,
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
    // "Known" calling - see UserProfile.canCall's doc comment.
    val canCall: Boolean = false,
    val phoneNumber: String? = null,
)

// --- Bots ("Leo" bot-builder platform, Round 1: character-sheet CRUD) ---

@Serializable
data class BotCreate(
    val name: String,
    val age: Int? = null,
    val gender: String? = null,
    val character: String,
    val personality: String,
    val bio: String,
    val occupation: String? = null,
    val lifeStory: String? = null,
    val description: String,
    val iconUrl: String? = null,
    val botType: String,
    val telegramToken: String? = null,
    val whatsappPhoneNumberId: String? = null,
    val whatsappAccessToken: String? = null,
    val userApiKey: String? = null,
    val hostingMode: String = "self",
    val whatsappMethod: String = "cloud_api",
)

@Serializable
data class BotUpdate(
    val name: String,
    val age: Int? = null,
    val gender: String? = null,
    val character: String,
    val personality: String,
    val bio: String,
    val occupation: String? = null,
    val lifeStory: String? = null,
    val description: String,
    val iconUrl: String? = null,
    val botType: String,
    val telegramToken: String? = null,
    val whatsappPhoneNumberId: String? = null,
    val whatsappAccessToken: String? = null,
    val userApiKey: String? = null,
    val hostingMode: String = "self",
    val whatsappMethod: String = "cloud_api",
)

@Serializable
data class BotResponse(
    val id: String,
    val name: String,
    val age: Int? = null,
    val gender: String? = null,
    val character: String,
    val personality: String,
    val bio: String,
    val occupation: String? = null,
    val lifeStory: String? = null,
    val description: String,
    val iconUrl: String? = null,
    val botType: String,
    val hasTelegramToken: Boolean = false,
    val hasWhatsappCredentials: Boolean = false,
    val freeTokensUsed: Int = 0,
    val isPremium: Boolean = false,
    val hasUserApiKey: Boolean = false,
    val hostingMode: String = "self",
    val whatsappMethod: String = "cloud_api",
    val createdAt: Long,
    val updatedAt: Long,
)

@Serializable
data class BotTestChatRequest(val message: String)

@Serializable
data class BotConverseResponse(val reply: String)

@Serializable
data class BotSetPremiumRequest(val isPremium: Boolean)

@Serializable
data class BotSecretResponse(val secretToken: String)

@Serializable
data class BotTurnDto(val role: String, val content: String)

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
    val reactions: Map<String, String> = emptyMap(),
    val isSticker: Boolean = false,
    val mediaUrl: String? = null,
    val mediaType: String? = null,
    val fileName: String? = null,
    val viewOnce: Boolean = false,
    val viewed: Boolean = false,
    // null (or "once") = classic single-reveal. "custom_time"/"custom_count"
    // - see ChatService.revealMessage's doc comment server-side.
    val viewOnceMode: String? = null,
    val viewOnceDurationMs: Long? = null,
    val viewOnceMaxViews: Int? = null,
    val viewOnceViewCount: Int = 0,
    val pollQuestion: String? = null,
    val pollOptions: List<String>? = null,
    val pollVotes: Map<String, Int> = emptyMap(),
    // Only ever set when sender/receiver have different preferredLanguage -
    // isMine messages ignore this (you always see your own original text);
    // a received message prefers this over `text` when rendering, if set.
    val translatedText: String? = null,
    // Only meaningful for a message someone else sent you - see
    // ChatService.markRead/getMessages doc comments server-side.
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
    // Holds a groupId (not a friend's user id) when isGroup is true - see
    // ChatRow/ChatsListBody which branch on isGroup to decide how to open it.
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
    val pinned: Boolean = false,
    val muted: Boolean = false,
    val favorite: Boolean = false,
    val locked: Boolean = false,
    val isGroup: Boolean = false,
    val memberAvatarUrls: List<String>? = null,
    val mentionsOnly: Boolean = false,
    val lastMessageMentionsMe: Boolean = false,
)

// --- Group chat (see GroupChatThreadScreen.kt / GroupProfileScreen.kt) ---

// role is "CREATOR" | "VICE_CREATOR" | "ADMIN" | "MEMBER" - see
// GroupChatService's kick/promote permission matrix server-side.
@Serializable
data class GroupMemberDto(
    val userId: String,
    val role: String,
    val joinedAt: Long,
    val canDm: Boolean = false,
    // "Known" calling for this member, from the viewer's own perspective -
    // see UserProfile.canCall's doc comment.
    val canCall: Boolean = false,
    val phoneNumber: String? = null,
)

@Serializable
data class GroupDto(
    val id: String,
    val name: String,
    val creatorId: String,
    val avatarUrl: String? = null,
    val description: String? = null,
    // "MEMBER" | "ADMIN" | "VICE_CREATOR" | "CREATOR" - the minimum rank
    // required, not a binary flag.
    val whoCanSendMessages: String = "MEMBER",
    val whoCanEditInfo: String = "ADMIN",
    val whoCanAddMembers: String = "ADMIN",
    val whoCanSeeGroupStats: String = "MEMBER",
    val whoCanSendMedia: String = "MEMBER",
    val shareHistoryWithNewMembers: Boolean = true,
    val isPublic: Boolean = false,
    val pinnedMessageId: String? = null,
    val pinnedByRole: String? = null,
    val securedMode: Boolean = false,
    val disappearingMessagesDurationMs: Long? = null,
    val muted: Boolean = false,
    val lockedSettings: List<String> = emptyList(),
    val rules: String? = null,
    val autoDeleteAt: Long? = null,
    val dmClosedByCreator: Boolean = false,
    // "Known" group calling - Creator-only lock, see the server's
    // Groups.callsEnabled doc comment.
    val callsEnabled: Boolean = true,
    val myDmOverride: String? = null,
    val inviteToken: String? = null,
    val members: List<GroupMemberDto> = emptyList(),
    val createdAt: Long,
)

@Serializable
data class GroupMessageDto(
    val id: String,
    val groupId: String,
    val senderId: String,
    val text: String,
    val sentAt: Long,
    val editedAt: Long? = null,
    val deleted: Boolean = false,
    val replyToId: String? = null,
    val reactions: Map<String, String> = emptyMap(),
    val isSticker: Boolean = false,
    val mediaUrl: String? = null,
    val mediaType: String? = null,
    val fileName: String? = null,
    val mediaSizeBytes: Long? = null,
    val viewOnce: Boolean = false,
    val viewed: Boolean = false,
    val viewOnceMode: String? = null,
    val viewOnceDurationMs: Long? = null,
    val viewOnceMaxViews: Int? = null,
    val kept: Boolean = false,
    val taggedUserIds: List<String> = emptyList(),
    val tagAll: Boolean = false,
    val tagPrivate: Boolean = false,
    val tagHidden: Boolean = false,
    val pollQuestion: String? = null,
    val pollOptions: List<String>? = null,
    val pollVotes: Map<String, Int> = emptyMap(),
)

@Serializable
data class CreateGroupRequest(val name: String, val memberIds: List<String>)

@Serializable
data class SendGroupMessageRequest(
    val text: String,
    val replyToId: String? = null,
    val isSticker: Boolean = false,
    val mediaUrl: String? = null,
    val mediaType: String? = null,
    val fileName: String? = null,
    val mediaSizeBytes: Long? = null,
    val viewOnce: Boolean = false,
    val viewOnceMode: String? = null,
    val viewOnceDurationMs: Long? = null,
    val viewOnceMaxViews: Int? = null,
    val pollQuestion: String? = null,
    val pollOptions: List<String>? = null,
    val taggedUserIds: List<String> = emptyList(),
    val tagAll: Boolean = false,
    val tagPrivate: Boolean = false,
    val disappearDurationMs: Long? = null,
    val disappearSelfOnly: Boolean = false,
)

@Serializable
data class GroupLinkPreviewDto(val id: String, val name: String, val avatarUrl: String? = null, val description: String? = null, val memberCount: Int, val isPublic: Boolean = false, val alreadyMember: Boolean, val alreadyRequested: Boolean)

@Serializable
data class EditGroupMessageRequest(val text: String)

@Serializable
data class ReactToGroupMessageRequest(val emoji: String)

@Serializable
data class VoteInGroupPollRequest(val optionIndex: Int)

@Serializable
data class AddGroupMemberRequest(val userId: String)

@Serializable
data class UpdateGroupInfoRequest(val name: String? = null, val description: String? = null, val avatarUrl: String? = null, val rules: String? = null)

@Serializable
data class UpdateGroupSettingsRequest(
    val whoCanSendMessages: String? = null,
    val whoCanEditInfo: String? = null,
    val whoCanAddMembers: String? = null,
    val whoCanSeeGroupStats: String? = null,
    val whoCanSendMedia: String? = null,
    val shareHistoryWithNewMembers: Boolean? = null,
    val isPublic: Boolean? = null,
    val securedMode: Boolean? = null,
    val disappearingMessagesDurationMs: Long? = null,
    val disappearingMessagesOff: Boolean = false,
    val lockedSettings: List<String>? = null,
    val autoDeleteDurationMs: Long? = null,
    val autoDeleteOff: Boolean = false,
    val dmClosedByCreator: Boolean? = null,
    val callsEnabled: Boolean? = null,
)

@Serializable
data class LeaveGroupRequest(
    val dissolve: Boolean = false,
    val successorId: String? = null,
    val random: Boolean = false,
    val systemOwner: Boolean = false,
    val securedMode: Boolean? = null,
    val isPublic: Boolean? = null,
)

@Serializable
data class SetDmOverrideRequest(val dmOverride: String? = null)

@Serializable
data class ReportGroupRequest(val reason: String? = null, val mediaUrl: String? = null, val mediaType: String? = null, val fileName: String? = null)

@Serializable
data class GroupJoinRequestDto(val userId: String, val requestedAt: Long)

@Serializable
data class GroupAddRequestDto(val groupId: String, val groupName: String, val groupAvatarUrl: String? = null, val invitedByName: String, val requestedAt: Long)

@Serializable
data class RespondGroupAddRequest(val accept: Boolean)

@Serializable
data class GroupSearchResultDto(val id: String, val name: String, val avatarUrl: String? = null, val description: String? = null, val memberCount: Int)

@Serializable
data class MediaSummaryDto(
    val images: Int, val videos: Int, val files: Int, val stickers: Int,
    val imagesBytes: Long, val videosBytes: Long, val filesBytes: Long, val stickersBytes: Long,
)

@Serializable
data class SaveMessageRequest(val sourceLabel: String? = null, val text: String, val mediaUrl: String? = null, val mediaType: String? = null, val fileName: String? = null)

@Serializable
data class SavedMessageDto(
    val id: String,
    val sourceLabel: String? = null,
    val text: String,
    val mediaUrl: String? = null,
    val mediaType: String? = null,
    val fileName: String? = null,
    val savedAt: Long,
)

@Serializable
data class SetGroupRoleRequest(val role: String)

@Serializable
data class BulkChatActionRequest(val friendIds: List<String>, val action: String)

@Serializable
data class ReportUserRequest(
    val reason: String? = null,
    val mediaUrl: String? = null,
    val mediaType: String? = null,
    val fileName: String? = null,
)

@Serializable
data class RequestPhoneCodeRequest(val phoneNumber: String)

@Serializable
data class RequestPhoneCodeResponse(val devVerificationCode: String? = null)

@Serializable
data class VerifyPhoneCodeRequest(val code: String)

@Serializable
data class PhoneStatusResponse(val phoneNumber: String? = null, val phoneVerified: Boolean = false)

// "Appeal" (full-screen gate a banned/admin-cleared user sees on login) +
// Admin Review's Appeals tab.
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

// Admin Review (chat list > More, admin-only).
@Serializable
data class UserReportDto(
    val id: String,
    val reporterId: String,
    val reporterName: String,
    val reportedId: String,
    val reportedName: String,
    val reason: String? = null,
    val mediaUrl: String? = null,
    val mediaType: String? = null,
    val fileName: String? = null,
    val createdAt: Long,
)

@Serializable
data class MessageReportDto(
    val id: String,
    val chatType: String,
    val messageId: String,
    val messageText: String,
    val status: String,
    val createdAt: Long,
)

// Godmode (chat list > More, admin-only).
@Serializable
data class GodmodeUserDto(
    val id: String,
    val name: String,
    val email: String? = null,
    val publicId: String? = null,
    val avatarUrl: String? = null,
    val banned: Boolean = false,
    val banPermanent: Boolean = false,
    val createdAt: Long,
    // Only meaningful on Manage Developer Access - see Users.developerAccess.
    val developerAccess: Boolean = false,
    val hasActiveDeveloperKey: Boolean = false,
    val declinedUpdateVersionCode: Int? = null,
    val declinedUpdateAt: Long? = null,
)

// Settings > Security > Popularity (global) + each chat's ⋮ menu > Popularity
// (per-viewer override, null = "no override, defer to global").
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

// Chat list > More > Achievements + rank-up popups.
@Serializable
data class AchievementDto(
    val key: String,
    val title: String,
    val bigWord: String,
    val body: String,
    val unlocked: Boolean = false,
    val unlockedAt: Long? = null,
)

@Serializable
data class SetActiveBadgeRequest(val key: String?)

@Serializable
data class TriggerAchievementRequest(val key: String)

// Live in-session ban/clear-data detection (see MemberScaffold's poll).
@Serializable
data class AccountStatusDto(val gated: Boolean, val permanent: Boolean, val bannedAt: Long? = null)

// Force-update gate - see UpdateGateState.
@Serializable
data class AppVersionDto(val versionCode: Int, val versionName: String, val apkUrl: String? = null, val changelog: String? = null)

@Serializable
data class SetAppVersionRequest(val versionCode: Int, val versionName: String, val apkUrl: String? = null, val changelog: String? = null)

@Serializable
data class DeclineUpdateRequest(val versionCode: Int)

@Serializable
data class PendingPopupDto(
    val id: String,
    val kind: String,
    val title: String,
    val bigWord: String? = null,
    val body: String,
)

@Serializable
data class WalletBalanceResponse(val balance: Int)

@Serializable
data class WalletSendRequest(val toUserId: String, val amount: Int)

@Serializable
data class WalletTransactionItem(
    val id: String,
    val type: String,
    val amount: Int,
    val peerName: String? = null,
    val createdAt: Long,
)

@Serializable
data class TradeCreateRequest(val amount: Int, val plea: String? = null)

@Serializable
data class NotificationItem(
    val id: String,
    val type: String,
    val title: String,
    val body: String,
    val fromUserId: String? = null,
    val fromUserName: String? = null,
    val tradeId: String? = null,
    val amount: Int? = null,
    val createdAt: Long,
    val isMine: Boolean,
)

@Serializable
data class MarketAssetSummary(
    val id: String,
    val symbol: String,
    val name: String,
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
data class InvestTradeRequest(val symbol: String, val quantity: Double)

@Serializable
data class PortfolioTransactionItem(
    val id: String,
    val symbol: String,
    val side: String,
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
data class GuiSessionRequest(val code: String)

@Serializable
data class GuiSessionJob(
    val sessionId: String,
    val status: String, // "starting" | "active" | "ended" | "error"
    val viewUrl: String? = null,
    val errorMessage: String? = null,
    val affinityCookies: List<String> = emptyList(),
)

@Serializable
data class AiCurrentFileDto(val path: String, val content: String)

@Serializable
data class AiFileActionDto(
    val action: String,
    val path: String,
    val content: String? = null,
    val newName: String? = null,
    val destinationFolder: String? = null,
)

@Serializable
data class AiChangeRequestBody(
    val text: String,
    val treePaths: List<String> = emptyList(),
    val currentFile: AiCurrentFileDto? = null,
    val replyToId: String? = null,
    val mediaUrl: String? = null,
    val mediaType: String? = null,
    val fileName: String? = null,
    val linkedFiles: List<AiCurrentFileDto> = emptyList(),
)

@Serializable
data class AiChangeRequestDto(
    val id: String,
    val status: String, // pending_judgment | answered | pending_approval | approved | rejected | deploying | deployed | error
    val requestText: String? = null,
    val answerText: String? = null,
    val summary: String? = null,
    val prUrl: String? = null,
    val errorMessage: String? = null,
    val fileAction: List<AiFileActionDto>? = null,
    val createdAt: Long = 0,
    val replyToId: String? = null,
    val requestEditedAt: Long? = null,
    val requestDeleted: Boolean = false,
    val mediaUrl: String? = null,
    val mediaType: String? = null,
    val fileName: String? = null,
    val fileActionExecuted: Boolean = false,
)
