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

// Real push notifications (2026-08-13) - see PushNotificationService's own
// doc comment. Self-scoped (no {id} path param) - always the caller's own
// token, registered from CedalMessagingService.onNewToken.
@Serializable
data class RegisterFcmTokenRequest(val token: String)

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
    val groupTypingIndicatorsEnabled: Boolean? = null,
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
    // A display name from TranslationService.LANGUAGES (e.g. "French"), or
    // null for "no translation" - see ChatMessageDto.translatedText.
    val preferredLanguage: String? = null,
    val dmClosed: Boolean = false,
    val noTag: Boolean = false,
    val hiderEnabled: Boolean = true,
    // Settings > Privacy > "Share My Number" - see Users.shareNumberDefault's
    // own doc comment. Always this account's own setting, regardless of viewer.
    val shareNumberDefault: Boolean = false,
    // Settings > Call - see Users.denyAllCalls/denyNonFriendCalls/
    // denyUnknownCallers's own doc comments. Always this account's own
    // setting, regardless of viewer.
    val denyAllCalls: Boolean = false,
    val denyNonFriendCalls: Boolean = false,
    val denyUnknownCallers: Boolean = false,
    // Settings > Groups - see Users.autoMuteNewGroups/mentionsOnlyDefault/
    // autoPinOwnedGroups/requireGroupAddApproval's own doc comments.
    val autoMuteNewGroups: Boolean = false,
    val mentionsOnlyDefault: Boolean = false,
    val autoPinOwnedGroups: Boolean = false,
    val requireGroupAddApproval: Boolean = false,
    val groupTypingIndicatorsEnabled: Boolean = true,
    // Whether the REQUESTING viewer is currently allowed to see/call this
    // profile's real phone number - true for your own profile, otherwise
    // computed per-request by CallService.canCall (global default + this
    // owner's per-viewer PhoneShareOverrides row). phoneNumber is only ever
    // non-null when this is true - see AuthService.getProfile.
    val canCall: Boolean = true,
    val phoneNumber: String? = null,
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
    // "Known" calling - see UserProfile.canCall's doc comment, same
    // per-viewer computation, just inlined onto the Call tab's contact list
    // so it doesn't need a profile fetch per row.
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
    val botType: String, // telegram | whatsapp | inapp | both
    val telegramToken: String? = null,
    val whatsappPhoneNumberId: String? = null,
    val whatsappAccessToken: String? = null,
    // Optional BYOK - an Anthropic API key. When set, converse() routes
    // through this key directly instead of Cedal's shared provider chain,
    // and the bot is exempt from the free-token cap (see BotBrainService).
    val userApiKey: String? = null,
    // "self" (default) | "cedal" - rejected by BotService unless isPremium
    // is already true. See BotService.kt's Round 3 doc comment.
    val hostingMode: String = "self",
    // "cloud_api" (default, needs whatsappPhoneNumberId/whatsappAccessToken)
    // | "baileys" (QR-scan, no Meta credentials at all, self-hosted only).
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
    // A null credential field leaves the stored value unchanged - only an
    // explicit non-null value overwrites it. There's no way to clear a
    // credential back to null via update; delete+recreate the bot instead.
    val telegramToken: String? = null,
    val whatsappPhoneNumberId: String? = null,
    val whatsappAccessToken: String? = null,
    val userApiKey: String? = null,
    val hostingMode: String = "self",
    val whatsappMethod: String = "cloud_api",
)

// List/detail response - deliberately omits secretToken/telegramToken/
// whatsappAccessToken/userApiKey (credentials, never echoed back in bulk).
// hasTelegramToken/hasWhatsappCredentials just tell the UI whether a
// credential is already on file, without revealing it.
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

// --- Bots Round 2 (brain endpoint) ---

@Serializable
data class BotConverseRequest(val chatId: String, val message: String)

@Serializable
data class BotTestChatRequest(val message: String)

@Serializable
data class BotConverseResponse(val reply: String)

@Serializable
data class BotTurnDto(val role: String, val content: String)

// --- Bots Round 3 (real Telegram/WhatsApp connectivity) ---

@Serializable
data class BotSetPremiumRequest(val isPremium: Boolean)

@Serializable
data class BotSecretResponse(val secretToken: String)

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
    // Holds a groupId (not a real friend's Users.id) when isGroup is true -
    // reusing this field rather than adding a parallel groupId keeps the
    // row shape/sort-merge logic in ChatService.listConversations simple;
    // the client's isGroup flag is what actually decides which detail
    // screen a tap opens.
    val friendId: String,
    val name: String,
    val email: String? = null,
    val avatarUrl: String? = null,
    val lastMessage: String? = null,
    val lastMessageAt: Long? = null,
    val lastMessageFromMe: Boolean? = null,
    val lastMessageViewOnce: Boolean = false,
    // "image" | "video" | "audio" | "file" when the last message is an
    // attachment - lets the client render a "Photo"/"Voice note"/etc. label
    // (with the same "You: "/sender-name prefix plain text gets) instead of
    // a blank line for captionless media - see ChatRow client-side.
    val lastMessageMediaType: String? = null,
    val unreadCount: Int = 0,
    val isSystemFeed: Boolean = false,
    // See ConversationState/ChatService bulk-action functions - archived
    // and hidden conversations are excluded from listConversations() by
    // default; the rest are just flags the client renders/sorts on.
    val pinned: Boolean = false,
    val muted: Boolean = false,
    val favorite: Boolean = false,
    val locked: Boolean = false,
    val isGroup: Boolean = false,
    val memberAvatarUrls: List<String>? = null,
    // Group-only (see GroupConversationState.mentionsOnly) - when true,
    // MessageNotificationSession should only notify if lastMessageMentionsMe
    // is also true, rather than for every new message in this group.
    val mentionsOnly: Boolean = false,
    val lastMessageMentionsMe: Boolean = false,
    // Group-only, see GroupTypingService - display names (not ids) of
    // whoever's currently typing in this group, excluding the requester.
    // Empty when nobody's typing (the common case) or when this group's/
    // the requester's own typing indicator gates are off.
    val typingUserNames: List<String> = emptyList(),
)

// --- Group chat (see GroupChatService) ---

// role is "CREATOR" | "VICE_CREATOR" | "ADMIN" | "MEMBER" - see
// GroupChatService's kick/promote permission matrix.
@Serializable
data class GroupMemberDto(
    val userId: String,
    val role: String,
    val joinedAt: Long,
    // Whether the REQUESTING viewer is currently allowed to DM this member
    // via the group's "Message" action - computed per-request in
    // GroupChatService.canDm (Users.dmClosed > Groups.dmClosedByCreator >
    // GroupConversationState.dmOverride precedence). Always false for the
    // viewer's own row.
    val canDm: Boolean = false,
    // "Known" calling for this member, from the REQUESTING viewer's
    // perspective - same computation as UserProfile.canCall, see
    // GroupChatService.buildGroupDto. Always false for the viewer's own row.
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
    // required, not a binary flag (see GroupChatService.roleRank). Only
    // visible/editable by admin-tier members client-side; re-enforced
    // server-side regardless (see GroupChatService.updateGroupSettings).
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
    // Which of the 5 rank-threshold settings (by key, e.g.
    // "whoCanSendMessages") are currently locked - see
    // GroupChatService.updateGroupSettings' lock enforcement.
    val lockedSettings: List<String> = emptyList(),
    val rules: String? = null,
    val autoDeleteAt: Long? = null,
    val dmClosedByCreator: Boolean = false,
    // "Known" group calling - Creator-only, see Groups.callsEnabled's own
    // doc comment. Governs whether Group Profile's Call button is usable at
    // all; per-member canCall/phoneNumber on GroupMemberDto above still
    // apply on top of this.
    val callsEnabled: Boolean = true,
    // Settings > Groups follow-up - Creator-only, see Groups.
    // typingIndicatorsEnabled's own doc comment.
    val typingIndicatorsEnabled: Boolean = true,
    // This viewer's own dmOverride for this group - "OPEN" | "CLOSED" | null.
    val myDmOverride: String? = null,
    // Round 5 "Link" tab - only meaningful/shown client-side when isPublic.
    val inviteToken: String? = null,
    // Empty (not full membership hidden some other way) when the caller
    // doesn't meet whoCanSeeGroupStats - see GroupChatService.buildGroupDto.
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
    // Whether the REQUESTING viewer has revealed this (computed per-request
    // in GroupChatService.toDto, unlike a 1-on-1 message this isn't a fixed
    // property of the row - see GroupMessageViews).
    val viewed: Boolean = false,
    val viewOnceMode: String? = null,
    val viewOnceDurationMs: Long? = null,
    val viewOnceMaxViews: Int? = null,
    val kept: Boolean = false,
    // Non-empty taggedUserIds = one or more specific-user tags (each renders
    // with "#"); tagAll=true = a broadcast "@all" tag (renders with "@",
    // never private). tagHidden is computed per-viewer (like hideContent
    // for view-once, but permanent/no reveal) - true means text/mediaUrl/etc
    // above are already blanked because this viewer is neither the sender
    // nor one of the tagged users.
    val taggedUserIds: List<String> = emptyList(),
    val tagAll: Boolean = false,
    val tagPrivate: Boolean = false,
    val tagHidden: Boolean = false,
    val pollQuestion: String? = null,
    val pollOptions: List<String>? = null,
    val pollVotes: Map<String, Int> = emptyMap(),
    // Settings > Groups > "Join & leave messages" - see GroupMessages.
    // isSystemMessage's own doc comment. senderId is the affected user,
    // text is already the fully-rendered notice.
    val isSystemMessage: Boolean = false,
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
    // Round 5 per-message disappearing - independent of the group-wide
    // Security-tab setting. disappearSelfOnly=true = "Custom" (hides only
    // from the sender once expired); false = "For Everyone" (real delete).
    val disappearDurationMs: Long? = null,
    val disappearSelfOnly: Boolean = false,
)

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
    // Explicit wrapper (not just a nullable Long) so "set back to off" is
    // distinguishable from "don't touch this field" in a PUT that only
    // patches whatever's non-null - matches this endpoint's existing
    // partial-update convention for every other field here.
    val disappearingMessagesDurationMs: Long? = null,
    val disappearingMessagesOff: Boolean = false,
    val lockedSettings: List<String>? = null,
    val autoDeleteDurationMs: Long? = null,
    val autoDeleteOff: Boolean = false,
    val dmClosedByCreator: Boolean? = null,
    val callsEnabled: Boolean? = null,
    val typingIndicatorsEnabled: Boolean? = null,
)

@Serializable
data class LeaveGroupRequest(
    val dissolve: Boolean = false,
    val successorId: String? = null,
    val random: Boolean = false,
    val systemOwner: Boolean = false,
    // Only used with systemOwner=true - the values the leaving Creator
    // chose in Android's confirm mini-form, applied atomically with the
    // ownership handoff (see GroupChatService.leaveGroup).
    val securedMode: Boolean? = null,
    val isPublic: Boolean? = null,
)

@Serializable
data class SetDmOverrideRequest(val dmOverride: String? = null)

@Serializable
data class ReportGroupRequest(val reason: String? = null, val mediaUrl: String? = null, val mediaType: String? = null, val fileName: String? = null)

@Serializable
data class SetGroupRoleRequest(val role: String)

@Serializable
data class GroupJoinRequestDto(val userId: String, val requestedAt: Long)

// Settings > Groups > "Request" (2026-08-10) - what the invited user sees
// on their own pending-group-invite list, distinct from GroupJoinRequestDto
// above (that's the opposite direction - a public group's admin approving
// someone who asked to join themselves).
@Serializable
data class GroupAddRequestDto(val groupId: String, val groupName: String, val groupAvatarUrl: String? = null, val invitedByName: String, val requestedAt: Long)

@Serializable
data class RespondGroupAddRequest(val accept: Boolean)

@Serializable
data class GroupSearchResultDto(val id: String, val name: String, val avatarUrl: String? = null, val description: String? = null, val memberCount: Int)

// Round 5 "Link" tab - what GET /groups/by-token/{token} returns, enough to
// show a "Request to Join" confirmation without exposing the full GroupDto
// (member list etc) to someone who isn't a member yet.
@Serializable
data class GroupLinkPreviewDto(val id: String, val name: String, val avatarUrl: String? = null, val description: String? = null, val memberCount: Int, val isPublic: Boolean, val alreadyMember: Boolean, val alreadyRequested: Boolean)

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
data class AppVersionDto(val versionCode: Int, val versionName: String, val apkUrl: String? = null, val changelog: String? = null)

@Serializable
data class SetAppVersionRequest(val versionCode: Int, val versionName: String, val apkUrl: String? = null, val changelog: String? = null)

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

// --- Code area <-> GitHub sync (CodeGithubSyncService/CodeGithubRoutes) ---

@Serializable
data class GithubAuthorizeUrlDto(val url: String)

@Serializable
data class GithubStatusDto(
    val connected: Boolean,
    val githubLogin: String? = null,
    val selectedOwner: String? = null,
    val selectedRepo: String? = null,
    val selectedBranch: String? = null,
)

@Serializable
data class GithubRepoDto(val owner: String, val name: String, val defaultBranch: String, val private: Boolean)

@Serializable
data class SelectGithubRepoRequest(val owner: String, val repo: String, val branch: String)

@Serializable
data class CodeSyncFileEntry(val path: String, val content: String)

@Serializable
data class SyncStartRequest(val files: List<CodeSyncFileEntry>)

@Serializable
data class SyncStartResponseDto(val jobId: String)

@Serializable
data class SyncConflictDto(val path: String, val localContent: String, val remoteContent: String)

@Serializable
data class ResolveConflictRequest(val path: String, val keepLocal: Boolean, val localContent: String)

@Serializable
data class ResolveConflictResponseDto(val path: String, val content: String)

@Serializable
data class SyncJobDto(
    val id: String,
    val status: String, // running | done | error
    val totalFiles: Int,
    val processedFiles: Int,
    val pushed: List<String> = emptyList(),
    val pulled: List<CodeSyncFileEntry> = emptyList(),
    val deletedRemote: List<String> = emptyList(),
    val deletedLocal: List<String> = emptyList(),
    val conflicts: List<SyncConflictDto> = emptyList(),
    val errorMessage: String? = null,
)

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
