package com.xhacker.cedal.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xhacker.cedal.data.AndroidBuildJob
import com.xhacker.cedal.data.AndroidBuildRequest
import com.xhacker.cedal.data.AiChangeRequestBody
import com.xhacker.cedal.data.AiChangeRequestDto
import com.xhacker.cedal.data.AiCurrentFileDto
import com.xhacker.cedal.data.EditAiMessageRequest
import com.xhacker.cedal.data.EditAiRequestTextBody
import com.xhacker.cedal.data.MessagePinDto
import com.xhacker.cedal.data.PinMessageRequest
import com.xhacker.cedal.data.ReportMessageRequest
import com.xhacker.cedal.data.GuiSessionJob
import com.xhacker.cedal.data.GuiSessionRequest
import com.xhacker.cedal.data.AlucardChatMessageDto
import com.xhacker.cedal.data.AlucardChatRequest
import com.xhacker.cedal.data.ArcChatMessageDto
import com.xhacker.cedal.data.ArcChatRequest
import com.xhacker.cedal.data.ChatContextDto
import com.xhacker.cedal.data.CornealChatMessageDto
import com.xhacker.cedal.data.CornealChatRequest
import com.xhacker.cedal.data.CreateFeedPostRequest
import com.xhacker.cedal.data.CreateStickerRequest
import com.xhacker.cedal.data.ReactToFeedPostRequest
import com.xhacker.cedal.data.StickerDto
import com.xhacker.cedal.data.SystemFeedPostDto
import com.xhacker.cedal.data.ThemePackDto
import com.xhacker.cedal.data.VotePollRequest
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import com.xhacker.cedal.data.ArcMission
import com.xhacker.cedal.data.ArcMissionCompleteRequest
import com.xhacker.cedal.data.ArcMissionCompleteResponse
import com.xhacker.cedal.data.ArcMissionRequest
import com.xhacker.cedal.data.ArcPracticeAppStatusResponse
import com.xhacker.cedal.data.BackerReviewRequest
import com.xhacker.cedal.data.BackerReviewResponse
import com.xhacker.cedal.data.ApiService
import com.xhacker.cedal.data.AuthTokens
import com.xhacker.cedal.data.CodeFile
import com.xhacker.cedal.data.CodeLanguageItem
import com.xhacker.cedal.data.CodeRunRequest
import com.xhacker.cedal.data.CodeRunResult
import com.xhacker.cedal.data.local.ConversationCacheDao
import com.xhacker.cedal.data.local.FriendCacheDao
import com.xhacker.cedal.data.local.toEntity
import com.xhacker.cedal.data.local.toSummary
import com.xhacker.cedal.data.CompleteLessonRequest
import com.xhacker.cedal.data.CreatePasscodeRequest
import com.xhacker.cedal.data.DailyTaskResponse
import com.xhacker.cedal.data.ErrorResponse
import com.xhacker.cedal.data.ExplainErrorRequest
import com.xhacker.cedal.data.ForgotPasswordRequest
import com.xhacker.cedal.data.ContactMatchRequest
import com.xhacker.cedal.data.FriendStatusResult
import com.xhacker.cedal.data.FriendRequestCreate
import com.xhacker.cedal.data.FriendRequestItem
import com.xhacker.cedal.data.BulkChatActionRequest
import com.xhacker.cedal.data.GodmodeUserDto
import com.xhacker.cedal.data.MessageReportDto
import com.xhacker.cedal.data.PhoneStatusResponse
import com.xhacker.cedal.data.UserReportDto
import com.xhacker.cedal.data.RequestPhoneCodeRequest
import com.xhacker.cedal.data.ReportUserRequest
import com.xhacker.cedal.data.ResetPasswordRequest
import com.xhacker.cedal.data.VerifyPhoneCodeRequest
import com.xhacker.cedal.data.ChatMessageDto
import com.xhacker.cedal.data.ConversationSummary
import com.xhacker.cedal.data.FriendSummary
import com.xhacker.cedal.data.MarkReadRequest
import com.xhacker.cedal.data.EditChatMessageRequest
import com.xhacker.cedal.data.ReactToMessageRequest
import com.xhacker.cedal.data.SendChatMessageRequest
import com.xhacker.cedal.data.InvestTradeRequest
import com.xhacker.cedal.data.LoginRequest
import com.xhacker.cedal.data.LoginResponse
import com.xhacker.cedal.data.MarketAssetDetail
import com.xhacker.cedal.data.MarketAssetSummary
import com.xhacker.cedal.data.NodePasswordVerifyRequest
import com.xhacker.cedal.data.RefreshRequest
import com.xhacker.cedal.data.SavedAccount
import com.xhacker.cedal.data.NodePasswordVerifyResponse
import com.xhacker.cedal.data.NotificationItem
import com.xhacker.cedal.data.PortfolioResponse
import com.xhacker.cedal.data.PortfolioTransactionItem
import com.xhacker.cedal.data.SearchUserResult
import com.xhacker.cedal.data.SecureStorage
import com.xhacker.cedal.data.SignupRequest
import com.xhacker.cedal.data.SignupResponse
import com.xhacker.cedal.data.LinkEmailRequest
import com.xhacker.cedal.data.TermsConfig
import com.xhacker.cedal.data.TradeCreateRequest
import com.xhacker.cedal.data.TermsUpdateRequest
import com.xhacker.cedal.data.TwoFactorConfirmRequest
import com.xhacker.cedal.data.TwoFactorLoginConfirmRequest
import com.xhacker.cedal.data.UpdatePasscodeRequest
import com.xhacker.cedal.data.UpdateProfileRequest
import com.xhacker.cedal.data.UserProfile
import com.xhacker.cedal.data.VerifyEmailRequest
import com.xhacker.cedal.data.WalletSendRequest
import com.xhacker.cedal.data.WalletTransactionItem
import com.xhacker.cedal.data.WatchlistAddRequest
import com.xhacker.cedal.data.WatchlistItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val api: ApiService,
    private val json: Json,
    val storage: SecureStorage,
    private val friendCacheDao: FriendCacheDao,
    private val conversationCacheDao: ConversationCacheDao,
) : ViewModel() {

    companion object {
        // Settings > More > Security - see canAddAnotherAccount below.
        const val MAX_SAVED_ACCOUNTS = 4
    }

    // Retrofit's HttpException only exposes a generic "HTTP 400" message by
    // default — this unwraps the server's actual {"error": "..."} body so
    // the UI can show *why* a request failed instead of a blank "bad request".
    private suspend fun <T> apiCall(block: suspend () -> T): Result<T> = try {
        Result.success(block())
    } catch (e: HttpException) {
        val reason = e.response()?.errorBody()?.string()
            ?.let { runCatching { json.decodeFromString<ErrorResponse>(it).error }.getOrNull() }
        Result.failure(Exception(reason ?: e.message()))
    } catch (e: java.net.SocketTimeoutException) {
        Result.failure(Exception("Request timed out — check your internet connection and try again."))
    } catch (e: java.io.IOException) {
        Result.failure(Exception("Couldn't reach Cedal's server — check your internet connection and try again."))
    } catch (e: Exception) {
        Result.failure(e)
    }

    // No acceptedTerms param needed — you can't reach the sign-up screen
    // without already passing through the versioned terms gate. deviceId is
    // only meaningful (and required server-side) when guest=true — it's how
    // the server enforces one active guest node per device.
    // phoneNumber/verifyVia are required server-side for non-guest signups
    // (see AuthService.signup) - null/ignored for guest.
    suspend fun signup(
        email: String?,
        password: String?,
        guest: Boolean,
        deviceId: String? = null,
        phoneNumber: String? = null,
        verifyVia: String? = null,
    ): Result<SignupResponse> =
        apiCall {
            val res = api.signup(SignupRequest(email, password, guest, TermsConfig.CURRENT_VERSION, deviceId, phoneNumber, verifyVia))
            res.tokens?.let { persistTokens(it) }
            storage.userId = res.userId
            res
        }

    // Forgot Password (SignInScreen > "Forgot password?") - the passcode is
    // an extra identity check beyond just picking a delivery channel (see
    // AuthService.forgotPassword's own doc comment). Silently succeeds
    // either way server-side (never reveals which part was wrong), so the
    // client always shows the same "if that's right, a code is on its way"
    // messaging regardless of the actual outcome.
    // email/phoneNumber - user fills EITHER one (whichever they remember),
    // not necessarily both - see ForgotPasswordScreen.
    suspend fun forgotPassword(email: String?, phoneNumber: String?, passcode: String, verifyVia: String): Result<Unit> = apiCall {
        api.forgotPassword(ForgotPasswordRequest(email, phoneNumber, passcode, verifyVia))
    }

    suspend fun resetPassword(email: String?, phoneNumber: String?, code: String, newPassword: String): Result<Unit> = apiCall {
        api.resetPassword(ResetPasswordRequest(email, phoneNumber, code, newPassword))
    }

    // Called once from the terms gate. Caches locally always; also syncs to
    // the server (the compliance source of truth) when there's an existing
    // session, since a returning user might be re-accepting after a change.
    suspend fun acceptTerms(version: String): Result<Unit> = apiCall {
        storage.acceptedTermsVersion = version
        val token = storage.accessToken
        val uid = storage.userId
        if (token != null && uid != null) {
            api.updateTerms(uid, TermsUpdateRequest(version), "Bearer $token")
        }
    }

    suspend fun verifyEmail(userId: String, code: String): Result<Unit> =
        apiCall { api.verifyEmail(VerifyEmailRequest(userId, code)) }

    // May come back needing a second step — see confirmLoginTwoFactor.
    suspend fun login(email: String, password: String): Result<LoginResponse> =
        apiCall {
            val res = api.login(LoginRequest(email, password))
            res.tokens?.let { persistTokens(it) }
            res
        }

    suspend fun confirmLoginTwoFactor(userId: String, code: String): Result<AuthTokens> =
        apiCall {
            val tokens = api.confirmLoginTwoFactor(TwoFactorLoginConfirmRequest(userId, code))
            persistTokens(tokens)
            tokens
        }

    suspend fun verifyNodePassword(code: String, mode: String): Result<NodePasswordVerifyResponse> =
        apiCall {
            val uid = storage.userId ?: error("No signed-in user")
            val res = api.verifyNodePassword(NodePasswordVerifyRequest(uid, code, mode))
            if (res.success && res.role != null) storage.role = res.role
            res
        }

    suspend fun createPasscode(code: String, age: Int, favoriteColor: String): Result<Unit> =
        apiCall {
            val uid = storage.userId ?: error("No signed-in user")
            api.createPasscode(CreatePasscodeRequest(uid, code, age, favoriteColor))
            storage.passcodeDone = true
        }

    suspend fun getProfile(): Result<UserProfile> = apiCall {
        val uid = storage.userId ?: error("No signed-in user")
        val token = storage.accessToken ?: error("No session token")
        api.getProfile(uid, "Bearer $token")
    }

    // Read-only view of another user's profile - see chat header's tap-name
    // flow. GET /users/{id} has no ownership check server-side (unlike PUT).
    suspend fun getProfileFor(userId: String): Result<UserProfile> = apiCall {
        val token = storage.accessToken ?: error("No session token")
        api.getProfile(userId, "Bearer $token")
    }

    suspend fun updateProfile(
        nickname: String?,
        handle: String?,
        age: Int? = null,
        occupation: String? = null,
        hobby: String? = null,
        gender: String? = null,
        bio: String? = null,
    ): Result<UserProfile> = apiCall {
        val uid = storage.userId ?: error("No signed-in user")
        val token = storage.accessToken ?: error("No session token")
        val req = UpdateProfileRequest(
            nickname = nickname, handle = handle, age = age,
            occupation = occupation, hobby = hobby, gender = gender, bio = bio,
        )
        api.updateProfile(uid, req, "Bearer $token")
    }

    // "Friend Hider" - see UserNames/FriendService server-side. Bypasses the
    // fixed-param updateProfile() above (which doesn't expose every field)
    // for the same reason avatarUrl isn't threaded through it either -
    // simplest to send just this one field directly.
    suspend fun updateHideFromSearch(hidden: Boolean): Result<UserProfile> = apiCall {
        val uid = storage.userId ?: error("No signed-in user")
        val token = storage.accessToken ?: error("No session token")
        api.updateProfile(uid, UpdateProfileRequest(hideFromSearch = hidden), "Bearer $token")
    }

    // Same bypass-the-fixed-param-list pattern as updateHideFromSearch above -
    // called after uploadImage("avatar", ...) returns a URL.
    suspend fun updateAvatarUrl(url: String): Result<UserProfile> = apiCall {
        val uid = storage.userId ?: error("No signed-in user")
        val token = storage.accessToken ?: error("No session token")
        api.updateProfile(uid, UpdateProfileRequest(avatarUrl = url), "Bearer $token")
    }

    suspend fun updatePasscode(code: String): Result<Unit> = apiCall {
        val uid = storage.userId ?: error("No signed-in user")
        val token = storage.accessToken ?: error("No session token")
        api.updatePasscode(uid, UpdatePasscodeRequest(code), "Bearer $token")
    }

    suspend fun linkGuestToEmail(email: String, password: String): Result<UserProfile> = apiCall {
        val uid = storage.userId ?: error("No signed-in user")
        val token = storage.accessToken ?: error("No session token")
        api.linkEmail(uid, LinkEmailRequest(email, password), "Bearer $token")
    }

    suspend fun requestTwoFactorSetup(): Result<String?> = apiCall {
        val uid = storage.userId ?: error("No signed-in user")
        val token = storage.accessToken ?: error("No session token")
        api.requestTwoFactorSetup(uid, "Bearer $token").devVerificationCode
    }

    suspend fun confirmTwoFactorSetup(code: String): Result<UserProfile> = apiCall {
        val uid = storage.userId ?: error("No signed-in user")
        val token = storage.accessToken ?: error("No session token")
        api.confirmTwoFactorSetup(uid, TwoFactorConfirmRequest(code), "Bearer $token")
    }

    suspend fun disableTwoFactor(): Result<UserProfile> = apiCall {
        val uid = storage.userId ?: error("No signed-in user")
        val token = storage.accessToken ?: error("No session token")
        api.disableTwoFactor(uid, "Bearer $token")
    }

    suspend fun searchUsers(
        query: String,
        byGender: Boolean = false,
        byOccupation: Boolean = false,
        byHobby: Boolean = false,
        byAge: Boolean = false,
        byBio: Boolean = false,
    ): Result<List<SearchUserResult>> = apiCall {
        val token = storage.accessToken ?: error("No session token")
        api.searchUsers(query.ifBlank { null }, byGender, byOccupation, byHobby, byAge, byBio, "Bearer $token")
    }

    suspend fun findUserByPublicId(code: String): Result<SearchUserResult> = apiCall {
        val token = storage.accessToken ?: error("No session token")
        api.findUserByPublicId(code, "Bearer $token")
    }

    // ✚ > Search's "from your contacts" prompt - see FriendService.matchContacts.
    suspend fun matchContacts(phoneNumbers: List<String>): Result<List<SearchUserResult>> = apiCall {
        val token = storage.accessToken ?: error("No session token")
        api.matchContacts(ContactMatchRequest(phoneNumbers), "Bearer $token")
    }

    // Chat thread's proactive "this account has been deleted" check - see
    // FriendService.friendStatus.
    suspend fun getFriendStatus(friendId: String): Result<FriendStatusResult> = apiCall {
        val token = storage.accessToken ?: error("No session token")
        api.getFriendStatus(friendId, "Bearer $token")
    }

    suspend fun sendFriendRequest(toUserId: String): Result<Unit> = apiCall {
        val token = storage.accessToken ?: error("No session token")
        api.sendFriendRequest(FriendRequestCreate(toUserId), "Bearer $token")
    }

    suspend fun listFriendRequests(): Result<List<FriendRequestItem>> = apiCall {
        val token = storage.accessToken ?: error("No session token")
        api.listFriendRequests("Bearer $token")
    }

    suspend fun acceptFriendRequest(id: String): Result<Unit> = apiCall {
        val token = storage.accessToken ?: error("No session token")
        api.acceptFriendRequest(id, "Bearer $token")
    }

    suspend fun declineFriendRequest(id: String): Result<Unit> = apiCall {
        val token = storage.accessToken ?: error("No session token")
        api.declineFriendRequest(id, "Bearer $token")
    }

    suspend fun cancelFriendRequest(id: String): Result<Unit> = apiCall {
        val token = storage.accessToken ?: error("No session token")
        api.cancelFriendRequest(id, "Bearer $token")
    }

    // "Delete User" (profile screen, distinct from Block) - see
    // FriendService.deleteUser server-side.
    suspend fun deleteUser(id: String): Result<Unit> = apiCall {
        val token = storage.accessToken ?: error("No session token")
        api.deleteUser(id, "Bearer $token")
    }

    // "Offline Mode" (Settings > Privacy) short-circuits to empty here,
    // before ever attempting a live fetch or touching the cache - it's a
    // deliberate decoy, not a real connectivity check. Otherwise: fetch
    // live and write-through cache on success; on failure (a genuine
    // connectivity problem), fall back to whatever was last cached for
    // THIS account, instead of the list just going blank.
    suspend fun listFriends(): Result<List<FriendSummary>> {
        if (storage.offlineModeEnabled) return Result.success(emptyList())
        val uid = storage.userId
        return apiCall {
            val token = storage.accessToken ?: error("No session token")
            api.listFriends("Bearer $token")
        }.onSuccess { friends ->
            if (uid != null) {
                friendCacheDao.clearFor(uid)
                friendCacheDao.upsertAll(friends.map { it.toEntity(uid) })
            }
        }.recoverCatching { throwable ->
            if (uid == null) throw throwable
            friendCacheDao.getAll(uid).map { it.toSummary() }
        }
    }

    // Real 1-on-1 chat, but only ever between accepted friends - see
    // ChatService server-side.
    suspend fun listConversations(): Result<List<ConversationSummary>> {
        if (storage.offlineModeEnabled) return Result.success(emptyList())
        val uid = storage.userId
        return apiCall {
            val token = storage.accessToken ?: error("No session token")
            api.listConversations("Bearer $token")
        }.onSuccess { conversations ->
            if (uid != null) {
                conversationCacheDao.clearFor(uid)
                conversationCacheDao.upsertAll(conversations.map { it.toEntity(uid) })
            }
        }.recoverCatching { throwable ->
            if (uid == null) throw throwable
            conversationCacheDao.getAll(uid).map { it.toSummary() }
        }
    }

    // Settings > Languages - see TranslationService.LANGUAGES server-side
    // for the exact list. Dual-written to SecureStorage.chatLanguage too so
    // the Settings screen doesn't need a fresh profile fetch just to show
    // what's currently selected.
    suspend fun updateChatLanguage(language: String): Result<UserProfile> = apiCall {
        val uid = storage.userId ?: error("No signed-in user")
        val token = storage.accessToken ?: error("No session token")
        val profile = api.updateProfile(uid, UpdateProfileRequest(preferredLanguage = language), "Bearer $token")
        storage.chatLanguage = profile.preferredLanguage
        profile
    }

    suspend fun getMessages(friendId: String): Result<List<ChatMessageDto>> = apiCall {
        val token = storage.accessToken ?: error("No session token")
        api.getMessages(friendId, "Bearer $token")
    }

    suspend fun getPinnedState(friendId: String): Result<Boolean> = apiCall {
        val token = storage.accessToken ?: error("No session token")
        api.getPinnedState(friendId, "Bearer $token")["pinned"] == true
    }

    suspend fun pingTyping(friendId: String): Result<Unit> = apiCall {
        val token = storage.accessToken ?: error("No session token")
        api.pingTyping(friendId, "Bearer $token")
        Unit
    }

    suspend fun getTypingFriendIds(): Result<List<String>> = apiCall {
        val token = storage.accessToken ?: error("No session token")
        api.getTypingFriendIds("Bearer $token")
    }

    suspend fun markRead(friendId: String, upToMessageId: String): Result<Unit> = apiCall {
        val token = storage.accessToken ?: error("No session token")
        api.markRead(friendId, MarkReadRequest(upToMessageId), "Bearer $token")
        Unit
    }

    // Fires when leaving a chat thread or the app backgrounds - see
    // ChatService.purgeConsumedViewOnce.
    suspend fun purgeConsumedViewOnce(friendId: String): Result<Unit> = apiCall {
        val token = storage.accessToken ?: error("No session token")
        api.purgeConsumedViewOnce(friendId, "Bearer $token")
        Unit
    }

    // Same call, but launched on this ViewModel's own scope instead of the
    // caller's - safe to call from a composable's onDispose{}, which has no
    // coroutine scope of its own and may run exactly as the caller's scope
    // is being cancelled.
    fun purgeConsumedViewOnceFireAndForget(friendId: String) {
        viewModelScope.launch { purgeConsumedViewOnce(friendId) }
    }

    suspend fun sendMessage(
        friendId: String,
        text: String,
        replyToId: String? = null,
        isSticker: Boolean = false,
        mediaUrl: String? = null,
        mediaType: String? = null,
        fileName: String? = null,
        viewOnce: Boolean = false,
        viewOnceMode: String? = null,
        viewOnceDurationMs: Long? = null,
        viewOnceMaxViews: Int? = null,
        pollQuestion: String? = null,
        pollOptions: List<String>? = null,
    ): Result<ChatMessageDto> = apiCall {
        val token = storage.accessToken ?: error("No session token")
        api.sendMessage(
            friendId,
            SendChatMessageRequest(
                text, replyToId, isSticker, mediaUrl, mediaType, fileName, viewOnce,
                viewOnceMode, viewOnceDurationMs, viewOnceMaxViews, pollQuestion, pollOptions,
            ),
            "Bearer $token",
        )
    }

    suspend fun voteInPoll(friendId: String, messageId: String, optionIndex: Int): Result<Map<String, Int>> = apiCall {
        val token = storage.accessToken ?: error("No session token")
        api.voteInPoll(friendId, messageId, VotePollRequest(optionIndex), "Bearer $token")
    }

    suspend fun editMessage(friendId: String, messageId: String, text: String): Result<ChatMessageDto> = apiCall {
        val token = storage.accessToken ?: error("No session token")
        api.editMessage(friendId, messageId, EditChatMessageRequest(text), "Bearer $token")
    }

    suspend fun deleteMessage(friendId: String, messageId: String): Result<Unit> = apiCall {
        val token = storage.accessToken ?: error("No session token")
        api.deleteMessage(friendId, messageId, "Bearer $token")
    }

    suspend fun reactToMessage(friendId: String, messageId: String, emoji: String): Result<Map<String, String>> = apiCall {
        val token = storage.accessToken ?: error("No session token")
        api.reactToMessage(friendId, messageId, ReactToMessageRequest(emoji), "Bearer $token")
    }

    // "View Once" reveal - only callable by the recipient, see ChatService
    // server-side. Returns the real content exactly once.
    suspend fun revealMessage(friendId: String, messageId: String): Result<ChatMessageDto> = apiCall {
        val token = storage.accessToken ?: error("No session token")
        api.revealMessage(friendId, messageId, "Bearer $token")
    }

    // "Delete Chat" (header ⋮ menu) - real, both-sides delete, see
    // ChatService.deleteConversation.
    suspend fun deleteConversation(friendId: String): Result<Unit> = apiCall {
        val token = storage.accessToken ?: error("No session token")
        api.deleteConversation(friendId, "Bearer $token")
    }

    // Chat list long-press multi-select bulk menu - see ChatService.bulkAction
    // server-side for what each action string does.
    suspend fun bulkChatAction(friendIds: List<String>, action: String): Result<Unit> = apiCall {
        val token = storage.accessToken ?: error("No session token")
        api.bulkChatAction(BulkChatActionRequest(friendIds, action), "Bearer $token")
        Unit
    }

    // "Archived" row pinned above the chat list - see
    // ChatService.listArchivedConversations.
    suspend fun listArchivedConversations(): Result<List<ConversationSummary>> = apiCall {
        val token = storage.accessToken ?: error("No session token")
        api.listArchivedConversations("Bearer $token")
    }

    // Gate this behind AccountVerifyOverlay-style biometric/passcode before
    // calling it - see ChatService.listHiddenConversations' doc comment.
    suspend fun listHiddenConversations(): Result<List<ConversationSummary>> = apiCall {
        val token = storage.accessToken ?: error("No session token")
        api.listHiddenConversations("Bearer $token")
    }

    suspend fun blockFriend(friendId: String): Result<Unit> = apiCall {
        val token = storage.accessToken ?: error("No session token")
        api.blockFriend(friendId, "Bearer $token")
        Unit
    }

    suspend fun unblockFriend(friendId: String): Result<Unit> = apiCall {
        val token = storage.accessToken ?: error("No session token")
        api.unblockFriend(friendId, "Bearer $token")
        Unit
    }

    suspend fun isBlocked(friendId: String): Result<Boolean> = apiCall {
        val token = storage.accessToken ?: error("No session token")
        api.isBlocked(friendId, "Bearer $token")["blocked"] == true
    }

    suspend fun reportFriend(
        friendId: String,
        reason: String? = null,
        mediaUrl: String? = null,
        mediaType: String? = null,
        fileName: String? = null,
    ): Result<Unit> = apiCall {
        val token = storage.accessToken ?: error("No session token")
        api.reportFriend(friendId, ReportUserRequest(reason, mediaUrl, mediaType, fileName), "Bearer $token")
        Unit
    }

    suspend fun getWalletBalance(): Result<Int> = apiCall {
        val token = storage.accessToken ?: error("No session token")
        api.getWalletBalance("Bearer $token").balance
    }

    suspend fun sendWallet(toUserId: String, amount: Int): Result<Int> = apiCall {
        val token = storage.accessToken ?: error("No session token")
        api.sendWallet(WalletSendRequest(toUserId, amount), "Bearer $token").balance
    }

    suspend fun listWalletTransactions(filter: String? = null): Result<List<WalletTransactionItem>> = apiCall {
        val token = storage.accessToken ?: error("No session token")
        api.listWalletTransactions(filter, "Bearer $token")
    }

    suspend fun postTrade(amount: Int, plea: String?): Result<Unit> = apiCall {
        val token = storage.accessToken ?: error("No session token")
        api.postTrade(TradeCreateRequest(amount, plea), "Bearer $token")
    }

    suspend fun listNotifications(): Result<List<NotificationItem>> = apiCall {
        val token = storage.accessToken ?: error("No session token")
        api.listNotifications("Bearer $token")
    }

    suspend fun creditNotification(id: String): Result<Int> = apiCall {
        val token = storage.accessToken ?: error("No session token")
        api.creditNotification(id, "Bearer $token").balance
    }

    suspend fun deleteNotification(id: String): Result<Unit> = apiCall {
        val token = storage.accessToken ?: error("No session token")
        api.deleteNotification(id, "Bearer $token")
    }

    suspend fun listMarkets(): Result<List<MarketAssetSummary>> = apiCall {
        val token = storage.accessToken ?: error("No session token")
        api.listMarkets("Bearer $token")
    }

    suspend fun getMarketDetail(id: String): Result<MarketAssetDetail> = apiCall {
        val token = storage.accessToken ?: error("No session token")
        api.getMarketDetail(id, "Bearer $token")
    }

    suspend fun getPortfolio(): Result<PortfolioResponse> = apiCall {
        val token = storage.accessToken ?: error("No session token")
        api.getPortfolio("Bearer $token")
    }

    suspend fun buyAsset(symbol: String, quantity: Double): Result<Unit> = apiCall {
        val token = storage.accessToken ?: error("No session token")
        api.buyAsset(InvestTradeRequest(symbol, quantity), "Bearer $token")
    }

    suspend fun sellAsset(symbol: String, quantity: Double): Result<Unit> = apiCall {
        val token = storage.accessToken ?: error("No session token")
        api.sellAsset(InvestTradeRequest(symbol, quantity), "Bearer $token")
    }

    suspend fun listPortfolioTransactions(): Result<List<PortfolioTransactionItem>> = apiCall {
        val token = storage.accessToken ?: error("No session token")
        api.listPortfolioTransactions("Bearer $token")
    }

    suspend fun listWatchlist(): Result<List<WatchlistItem>> = apiCall {
        val token = storage.accessToken ?: error("No session token")
        api.listWatchlist("Bearer $token")
    }

    suspend fun addToWatchlist(symbol: String): Result<Unit> = apiCall {
        val token = storage.accessToken ?: error("No session token")
        api.addToWatchlist(WatchlistAddRequest(symbol), "Bearer $token")
    }

    suspend fun removeFromWatchlist(symbol: String): Result<Unit> = apiCall {
        val token = storage.accessToken ?: error("No session token")
        api.removeFromWatchlist(symbol, "Bearer $token")
    }

    suspend fun listCodeLanguages(): Result<List<CodeLanguageItem>> = apiCall {
        val token = storage.accessToken ?: error("No session token")
        api.listCodeLanguages("Bearer $token")
    }

    suspend fun runCode(language: String, code: String, stdin: String, extraFiles: List<CodeFile> = emptyList(), packages: List<String> = emptyList()): Result<CodeRunResult> = apiCall {
        val token = storage.accessToken ?: error("No session token")
        api.runCode(CodeRunRequest(language, code, stdin, extraFiles, packages), "Bearer $token")
    }

    suspend fun explainError(language: String, errorText: String): Result<String> = apiCall {
        val token = storage.accessToken ?: error("No session token")
        api.explainError(ExplainErrorRequest(language, errorText), "Bearer $token").explanation
    }

    suspend fun requestAndroidBuild(code: String): Result<AndroidBuildJob> = apiCall {
        val token = storage.accessToken ?: error("No session token")
        api.requestAndroidBuild(AndroidBuildRequest(code), "Bearer $token")
    }

    suspend fun getAndroidBuildJob(jobId: String): Result<AndroidBuildJob> = apiCall {
        val token = storage.accessToken ?: error("No session token")
        api.getAndroidBuildJob(jobId, "Bearer $token")
    }

    suspend fun startGuiSession(code: String): Result<GuiSessionJob> = apiCall {
        val token = storage.accessToken ?: error("No session token")
        api.startGuiSession(GuiSessionRequest(code), "Bearer $token")
    }

    suspend fun stopGuiSession(jobId: String): Result<Unit> = apiCall {
        val token = storage.accessToken ?: error("No session token")
        api.stopGuiSession(jobId, "Bearer $token")
    }

    suspend fun submitAiRequest(
        text: String,
        treePaths: List<String> = emptyList(),
        currentFile: AiCurrentFileDto? = null,
        replyToId: String? = null,
        mediaUrl: String? = null,
        mediaType: String? = null,
        fileName: String? = null,
        linkedFiles: List<AiCurrentFileDto> = emptyList(),
    ): Result<AiChangeRequestDto> = apiCall {
        val token = storage.accessToken ?: error("No session token")
        api.submitAiRequest(AiChangeRequestBody(text, treePaths, currentFile, replyToId, mediaUrl, mediaType, fileName, linkedFiles), "Bearer $token")
    }

    suspend fun getAiRequest(id: String): Result<AiChangeRequestDto> = apiCall {
        val token = storage.accessToken ?: error("No session token")
        api.getAiRequest(id, "Bearer $token")
    }

    suspend fun markFileActionExecuted(id: String): Result<AiChangeRequestDto> = apiCall {
        val token = storage.accessToken ?: error("No session token")
        api.markFileActionExecuted(id, "Bearer $token")
    }

    suspend fun editAiRequest(id: String, text: String): Result<AiChangeRequestDto> = apiCall {
        val token = storage.accessToken ?: error("No session token")
        api.editAiRequest(id, EditAiRequestTextBody(text), "Bearer $token")
    }

    suspend fun deleteAiRequest(id: String): Result<Unit> = apiCall {
        val token = storage.accessToken ?: error("No session token")
        api.deleteAiRequest(id, "Bearer $token")
    }

    suspend fun deleteAiRequestHistory(): Result<Unit> = apiCall {
        val token = storage.accessToken ?: error("No session token")
        api.deleteAiRequestHistory("Bearer $token")
    }

    suspend fun deleteCornealHistory(): Result<Unit> = apiCall {
        val token = storage.accessToken ?: error("No session token")
        api.deleteCornealHistory("Bearer $token")
    }

    suspend fun deleteArcHistory(): Result<Unit> = apiCall {
        val token = storage.accessToken ?: error("No session token")
        api.deleteArcHistory("Bearer $token")
    }

    // Hydrates Code AI's chat on open - AiChangeRequests rows already ARE
    // its turn-by-turn history, see AiChangeRequestService.listHistory.
    suspend fun getAiRequestHistory(): Result<List<AiChangeRequestDto>> = apiCall {
        val token = storage.accessToken ?: error("No session token")
        api.getAiRequestHistory("Bearer $token")
    }

    suspend fun listPendingAiRequests(): Result<List<AiChangeRequestDto>> = apiCall {
        val token = storage.accessToken ?: error("No session token")
        api.listPendingAiRequests("Bearer $token")
    }

    suspend fun listUserReports(): Result<List<UserReportDto>> = apiCall {
        val token = storage.accessToken ?: error("No session token")
        api.listUserReports("Bearer $token")
    }

    suspend fun dismissUserReport(id: String): Result<Unit> = apiCall {
        val token = storage.accessToken ?: error("No session token")
        api.dismissUserReport(id, "Bearer $token")
        Unit
    }

    suspend fun listMessageReports(): Result<List<MessageReportDto>> = apiCall {
        val token = storage.accessToken ?: error("No session token")
        api.listMessageReports("Bearer $token")
    }

    suspend fun dismissMessageReport(id: String): Result<Unit> = apiCall {
        val token = storage.accessToken ?: error("No session token")
        api.dismissMessageReport(id, "Bearer $token")
        Unit
    }

    suspend fun listGodmodeUsers(): Result<List<GodmodeUserDto>> = apiCall {
        val token = storage.accessToken ?: error("No session token")
        api.listGodmodeUsers("Bearer $token")
    }

    suspend fun banUser(id: String): Result<Unit> = apiCall {
        val token = storage.accessToken ?: error("No session token")
        api.banUser(id, "Bearer $token")
        Unit
    }

    suspend fun unbanUser(id: String): Result<Unit> = apiCall {
        val token = storage.accessToken ?: error("No session token")
        api.unbanUser(id, "Bearer $token")
        Unit
    }

    suspend fun permanentBanUser(id: String): Result<Unit> = apiCall {
        val token = storage.accessToken ?: error("No session token")
        api.permanentBanUser(id, "Bearer $token")
        Unit
    }

    suspend fun clearUserData(id: String): Result<Unit> = apiCall {
        val token = storage.accessToken ?: error("No session token")
        api.clearUserData(id, "Bearer $token")
        Unit
    }

    suspend fun listDeveloperAccessUsers(): Result<List<GodmodeUserDto>> = apiCall {
        val token = storage.accessToken ?: error("No session token")
        api.listDeveloperAccessUsers("Bearer $token")
    }

    suspend fun grantDeveloperAccess(id: String): Result<Unit> = apiCall {
        val token = storage.accessToken ?: error("No session token")
        api.grantDeveloperAccess(id, "Bearer $token")
        Unit
    }

    suspend fun revokeDeveloperAccess(id: String): Result<Unit> = apiCall {
        val token = storage.accessToken ?: error("No session token")
        api.revokeDeveloperAccess(id, "Bearer $token")
        Unit
    }

    // Returned key is shown to the owner exactly once - the delegated
    // account has no endpoint that ever exposes it.
    suspend fun generateDeveloperKey(id: String): Result<String> = apiCall {
        val token = storage.accessToken ?: error("No session token")
        api.generateDeveloperKey(id, "Bearer $token")["key"] ?: error("No key returned")
    }

    // ManageDeveloperAccessScreen's SEND KEY button - see
    // DeveloperAccessService.sendKeyMessage.
    suspend fun sendDeveloperKey(id: String, key: String): Result<Unit> = apiCall {
        val token = storage.accessToken ?: error("No session token")
        api.sendDeveloperKey(id, com.xhacker.cedal.data.SendDeveloperKeyRequest(key), "Bearer $token")
    }

    // Cedal Team chat thread's "Generate Key" action - self-service, only
    // works if this account currently has developerAccess (server-checked).
    suspend fun generateOwnDeveloperKey(): Result<Unit> = apiCall {
        val token = storage.accessToken ?: error("No session token")
        api.generateOwnDeveloperKey("Bearer $token")
        Unit
    }

    // Cedal Team chat thread's "Revoke my developer account" action -
    // irreversible; the client confirms with the user before ever calling
    // this.
    suspend fun revokeOwnDeveloperAccess(): Result<Unit> = apiCall {
        val token = storage.accessToken ?: error("No session token")
        api.revokeOwnDeveloperAccess("Bearer $token")
        Unit
    }

    // Developer Mode's submit -> review -> approve/deny pipeline - see
    // DeveloperSubmissionService server-side.
    suspend fun submitDeveloperPatch(title: String, targetFilePath: String, code: String, language: String): Result<com.xhacker.cedal.data.DeveloperSubmissionDto> = apiCall {
        val token = storage.accessToken ?: error("No session token")
        api.submitDeveloperPatch(com.xhacker.cedal.data.SubmitDeveloperPatchRequest(title, targetFilePath, code, language), "Bearer $token")
    }

    suspend fun listMyDeveloperSubmissions(): Result<List<com.xhacker.cedal.data.DeveloperSubmissionDto>> = apiCall {
        val token = storage.accessToken ?: error("No session token")
        api.listMyDeveloperSubmissions("Bearer $token")
    }

    suspend fun getDeveloperSubmission(id: String): Result<com.xhacker.cedal.data.DeveloperSubmissionDto> = apiCall {
        val token = storage.accessToken ?: error("No session token")
        api.getDeveloperSubmission(id, "Bearer $token")
    }

    suspend fun listPendingDeveloperSubmissions(): Result<List<com.xhacker.cedal.data.DeveloperSubmissionDto>> = apiCall {
        val token = storage.accessToken ?: error("No session token")
        api.listPendingDeveloperSubmissions("Bearer $token")
    }

    suspend fun approveDeveloperSubmission(id: String): Result<com.xhacker.cedal.data.DeveloperSubmissionDto> = apiCall {
        val token = storage.accessToken ?: error("No session token")
        api.approveDeveloperSubmission(id, "Bearer $token")
    }

    suspend fun denyDeveloperSubmission(id: String, reason: String): Result<com.xhacker.cedal.data.DeveloperSubmissionDto> = apiCall {
        val token = storage.accessToken ?: error("No session token")
        api.denyDeveloperSubmission(id, com.xhacker.cedal.data.DenyDeveloperSubmissionRequest(reason), "Bearer $token")
    }

    suspend fun getPopularitySettings(): Result<com.xhacker.cedal.data.PopularitySettingsDto> = apiCall {
        val uid = storage.userId ?: error("No signed-in user")
        val token = storage.accessToken ?: error("No session token")
        api.getPopularitySettings(uid, "Bearer $token")
    }

    suspend fun setPopularitySettings(req: com.xhacker.cedal.data.PopularitySettingsDto): Result<com.xhacker.cedal.data.PopularitySettingsDto> = apiCall {
        val uid = storage.userId ?: error("No signed-in user")
        val token = storage.accessToken ?: error("No session token")
        api.setPopularitySettings(uid, req, "Bearer $token")
    }

    suspend fun getChatPopularityOverride(friendId: String): Result<com.xhacker.cedal.data.ChatPopularityOverrideDto> = apiCall {
        val uid = storage.userId ?: error("No signed-in user")
        val token = storage.accessToken ?: error("No session token")
        api.getChatPopularityOverride(uid, friendId, "Bearer $token")
    }

    suspend fun setChatPopularityOverride(friendId: String, req: com.xhacker.cedal.data.ChatPopularityOverrideDto): Result<com.xhacker.cedal.data.ChatPopularityOverrideDto> = apiCall {
        val uid = storage.userId ?: error("No signed-in user")
        val token = storage.accessToken ?: error("No session token")
        api.setChatPopularityOverride(uid, friendId, req, "Bearer $token")
    }

    // Unauthenticated - see UpdateGateState.
    suspend fun getAppVersion(): Result<com.xhacker.cedal.data.AppVersionDto> = apiCall {
        api.getAppVersion()
    }

    // Fired when the update banner's "✕" is tapped - a permanent audit
    // record (see Users.declinedUpdateVersionCode), not just a local
    // dismiss. Best-effort: if it's not logged in yet (pre-auth banner
    // context doesn't apply here since the banner is member-only) this
    // would just fail silently via apiCall's Result wrapper.
    suspend fun declineUpdate(versionCode: Int): Result<Unit> = apiCall {
        val token = storage.accessToken ?: error("No session token")
        api.declineUpdate(com.xhacker.cedal.data.DeclineUpdateRequest(versionCode), "Bearer $token")
        Unit
    }

    // Unauthenticated - see SignInScreen's full-screen gate.
    suspend fun submitAppeal(email: String, reason: String, message: String): Result<Unit> = apiCall {
        api.submitAppeal(com.xhacker.cedal.data.SubmitAppealRequest(email, reason, message))
        Unit
    }

    suspend fun listAppeals(): Result<List<com.xhacker.cedal.data.AppealDto>> = apiCall {
        val token = storage.accessToken ?: error("No session token")
        api.listAppeals("Bearer $token")
    }

    suspend fun dismissAppeal(id: String): Result<Unit> = apiCall {
        val token = storage.accessToken ?: error("No session token")
        api.dismissAppeal(id, "Bearer $token")
        Unit
    }

    // Live in-session ban detection - see MemberScaffold's poll.
    suspend fun getAccountStatus(): Result<com.xhacker.cedal.data.AccountStatusDto> = apiCall {
        val uid = storage.userId ?: error("No signed-in user")
        val token = storage.accessToken ?: error("No session token")
        api.getAccountStatus(uid, "Bearer $token")
    }

    suspend fun setActiveBadge(key: String?): Result<UserProfile> = apiCall {
        val uid = storage.userId ?: error("No signed-in user")
        val token = storage.accessToken ?: error("No session token")
        api.setActiveBadge(uid, com.xhacker.cedal.data.SetActiveBadgeRequest(key), "Bearer $token")
    }

    // Self-attested "first time doing X" achievements - see
    // AchievementService.CLIENT_TRIGGERABLE server-side for the allowlist.
    // Fire-and-forget from call sites (Corneal/ARC first message, Code's
    // first file) - unlock() is idempotent, so calling this on every
    // success (not just the actual first time) is safe and simpler than
    // tracking "have I already sent this" locally.
    fun triggerAchievementFireAndForget(key: String) {
        viewModelScope.launch {
            val uid = storage.userId ?: return@launch
            val token = storage.accessToken ?: return@launch
            runCatching { api.triggerAchievement(uid, com.xhacker.cedal.data.TriggerAchievementRequest(key), "Bearer $token") }
        }
    }

    suspend fun listAchievements(): Result<List<com.xhacker.cedal.data.AchievementDto>> = apiCall {
        val uid = storage.userId ?: error("No signed-in user")
        val token = storage.accessToken ?: error("No session token")
        api.listAchievements(uid, "Bearer $token")
    }

    // Polled globally (see MemberScaffold) - each call also marks the
    // returned popups delivered server-side, so nothing repeats.
    suspend fun pollPendingPopups(): Result<List<com.xhacker.cedal.data.PendingPopupDto>> = apiCall {
        val uid = storage.userId ?: error("No signed-in user")
        val token = storage.accessToken ?: error("No session token")
        api.pollPendingPopups(uid, "Bearer $token")
    }

    suspend fun approveAiRequest(id: String): Result<AiChangeRequestDto> = apiCall {
        val token = storage.accessToken ?: error("No session token")
        api.approveAiRequest(id, "Bearer $token")
    }

    suspend fun rejectAiRequest(id: String): Result<Unit> = apiCall {
        val token = storage.accessToken ?: error("No session token")
        api.rejectAiRequest(id, "Bearer $token")
    }

    // Idempotent server-side (see LessonService) - safe to call every time a
    // lesson's checkbox is marked done, even if it was already completed
    // before.
    suspend fun completeLesson(lessonId: String): Result<Long> = apiCall {
        val token = storage.accessToken ?: error("No session token")
        api.completeLesson(CompleteLessonRequest(lessonId), "Bearer $token").exp
    }

    // Pad's "Backer" pre-flight check - see CodeBackerService server-side.
    suspend fun backerReview(language: String, code: String): Result<BackerReviewResponse> = apiCall {
        val token = storage.accessToken ?: error("No session token")
        api.backerReview(BackerReviewRequest(language, code), "Bearer $token")
    }

    // ARC's Assistant tab - see ArcChatService server-side. History is
    // persisted server-side now (see AiChatHistoryService); getArcChatHistory
    // hydrates it on open, arcChat only ever sends the new message.
    suspend fun getArcChatHistory(): Result<List<ArcChatMessageDto>> = apiCall {
        val token = storage.accessToken ?: error("No session token")
        api.getArcChatHistory("Bearer $token").messages
    }

    suspend fun arcChat(message: String, replyToId: String? = null, mediaUrl: String? = null, mediaType: String? = null, fileName: String? = null): Result<ArcChatMessageDto> = apiCall {
        val token = storage.accessToken ?: error("No session token")
        api.arcChat(ArcChatRequest(message, replyToId, mediaUrl, mediaType, fileName), "Bearer $token").message
    }

    suspend fun editArcMessage(id: String, content: String): Result<ArcChatMessageDto> = apiCall {
        val token = storage.accessToken ?: error("No session token")
        api.editArcMessage(id, EditAiMessageRequest(content), "Bearer $token")
    }

    suspend fun deleteArcMessage(id: String): Result<Unit> = apiCall {
        val token = storage.accessToken ?: error("No session token")
        api.deleteArcMessage(id, "Bearer $token")
    }

    // Developer Mode's "Alucard" security/code-review chat.
    suspend fun getAlucardChatHistory(): Result<List<AlucardChatMessageDto>> = apiCall {
        val token = storage.accessToken ?: error("No session token")
        api.getAlucardChatHistory("Bearer $token").messages
    }

    suspend fun alucardChat(message: String, replyToId: String? = null, mediaUrl: String? = null, mediaType: String? = null, fileName: String? = null): Result<AlucardChatMessageDto> = apiCall {
        val token = storage.accessToken ?: error("No session token")
        api.alucardChat(AlucardChatRequest(message, replyToId, mediaUrl, mediaType, fileName), "Bearer $token").message
    }

    suspend fun editAlucardMessage(id: String, content: String): Result<AlucardChatMessageDto> = apiCall {
        val token = storage.accessToken ?: error("No session token")
        api.editAlucardMessage(id, EditAiMessageRequest(content), "Bearer $token")
    }

    suspend fun deleteAlucardMessage(id: String): Result<Unit> = apiCall {
        val token = storage.accessToken ?: error("No session token")
        api.deleteAlucardMessage(id, "Bearer $token")
    }

    suspend fun deleteAlucardHistory(): Result<Unit> = apiCall {
        val token = storage.accessToken ?: error("No session token")
        api.deleteAlucardHistory("Bearer $token")
    }

    // Corneal - the app-wide help assistant reachable from Chats. See
    // CornealChatService server-side for what it actually knows about.
    // Same persisted-history shape as ARC's Assistant above.
    suspend fun getCornealChatHistory(): Result<List<CornealChatMessageDto>> = apiCall {
        val token = storage.accessToken ?: error("No session token")
        api.getCornealChatHistory("Bearer $token").messages
    }

    // settingsSnapshot defaults to the user's REAL current toggle states
    // (see SettingsSnapshotDto) - always sent, not opt-in, since it's the
    // only way Corneal ever knows these (all client-only SecureStorage
    // values) and answering accurately about them isn't privacy-sensitive
    // the way Bot View/Call Out's actual CONTENT is.
    suspend fun cornealChat(
        message: String,
        replyToId: String? = null,
        chatContext: ChatContextDto? = null,
        codeContext: com.xhacker.cedal.data.CodeContextDto? = null,
        mediaUrl: String? = null,
        mediaType: String? = null,
        fileName: String? = null,
        settingsSnapshot: com.xhacker.cedal.data.SettingsSnapshotDto? = currentSettingsSnapshot(),
    ): Result<CornealChatMessageDto> = apiCall {
        val token = storage.accessToken ?: error("No session token")
        api.cornealChat(CornealChatRequest(message, replyToId, chatContext, codeContext, settingsSnapshot, mediaUrl, mediaType, fileName), "Bearer $token").message
    }

    fun currentSettingsSnapshot() = com.xhacker.cedal.data.SettingsSnapshotDto(
        botView = storage.botViewEnabled,
        cornealHider = storage.cornealHiderEnabled,
        botAccess = storage.botAccessEnabled,
        callOutText = storage.callOutTextEnabled,
        callOutScreenCapture = storage.callOutScreenCaptureEnabled,
        offlineMode = storage.offlineModeEnabled,
        biometricEnabled = storage.biometricEnabled,
        appLockEnabled = storage.appLockEnabled,
    )

    // "Call Out" - user tapped "No, that's not it" (see MemberCodeBody's
    // circle/highlight confirm-deny UI).
    suspend fun rejectCallOut(filePath: String, snippet: String): Result<Unit> = apiCall {
        val token = storage.accessToken ?: error("No session token")
        api.rejectCallOut(com.xhacker.cedal.data.RejectCallOutRequest(filePath, snippet), "Bearer $token")
        Unit
    }

    suspend fun editCornealMessage(id: String, content: String): Result<CornealChatMessageDto> = apiCall {
        val token = storage.accessToken ?: error("No session token")
        api.editCornealMessage(id, EditAiMessageRequest(content), "Bearer $token")
    }

    suspend fun deleteCornealMessage(id: String): Result<Unit> = apiCall {
        val token = storage.accessToken ?: error("No session token")
        api.deleteCornealMessage(id, "Bearer $token")
    }

    // Cross-chat-type pin/report - see MessageInteractionService server-side.
    suspend fun listPinnedMessages(): Result<List<MessagePinDto>> = apiCall {
        val token = storage.accessToken ?: error("No session token")
        api.listPinnedMessages("Bearer $token")
    }

    suspend fun pinMessage(chatType: String, messageId: String, messageText: String, chatKey: String? = null): Result<MessagePinDto> = apiCall {
        val token = storage.accessToken ?: error("No session token")
        api.pinMessage(PinMessageRequest(chatType, messageId, chatKey, messageText), "Bearer $token")
    }

    suspend fun unpinMessage(pinId: String): Result<Unit> = apiCall {
        val token = storage.accessToken ?: error("No session token")
        api.unpinMessage(pinId, "Bearer $token")
    }

    suspend fun reportMessage(chatType: String, messageId: String, messageText: String): Result<Unit> = apiCall {
        val token = storage.accessToken ?: error("No session token")
        api.reportMessage(ReportMessageRequest(chatType, messageId, messageText), "Bearer $token")
    }

    // Cedal System Feed - see SystemFeedService server-side for the
    // admin-only-posting rule (enforced there, not just hidden client-side).
    suspend fun listFeedPosts(): Result<List<SystemFeedPostDto>> = apiCall {
        val token = storage.accessToken ?: error("No session token")
        api.listFeedPosts("Bearer $token")
    }

    suspend fun createFeedPost(text: String): Result<SystemFeedPostDto> = apiCall {
        val token = storage.accessToken ?: error("No session token")
        api.createFeedPost(CreateFeedPostRequest(text), "Bearer $token")
    }

    suspend fun reactToFeedPost(postId: String, emoji: String): Result<Map<String, String>> = apiCall {
        val token = storage.accessToken ?: error("No session token")
        api.reactToFeedPost(postId, ReactToFeedPostRequest(emoji), "Bearer $token")
    }

    // kind is "avatar" or "sticker" - see ImageUploadService server-side.
    // Returns just the uploaded URL; the caller decides what to do with it
    // (updateProfile for avatars, createSticker for custom stickers).
    suspend fun uploadImage(kind: String, bytes: ByteArray, mimeType: String): Result<String> = apiCall {
        val token = storage.accessToken ?: error("No session token")
        val body = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
        val part = MultipartBody.Part.createFormData("file", "upload", body)
        api.uploadImage(kind, part, "Bearer $token").url
    }

    suspend fun listMyStickers(): Result<List<StickerDto>> = apiCall {
        val token = storage.accessToken ?: error("No session token")
        api.listMyStickers("Bearer $token")
    }

    suspend fun createSticker(imageUrl: String): Result<StickerDto> = apiCall {
        val token = storage.accessToken ?: error("No session token")
        api.createSticker(CreateStickerRequest(imageUrl), "Bearer $token")
    }

    suspend fun listThemePacks(): Result<List<ThemePackDto>> = apiCall {
        val token = storage.accessToken ?: error("No session token")
        api.listThemePacks("Bearer $token")
    }

    suspend fun purchaseThemePack(packId: String): Result<Int> = apiCall {
        val token = storage.accessToken ?: error("No session token")
        api.purchaseThemePack(packId, "Bearer $token").newBalance
    }

    // ARC Ops - a fresh mission per request (see ArcOpsService), and a
    // separate, non-idempotent completion call that awards exp for that
    // attempt's score every time, replays included.
    suspend fun arcGenerateMission(targetName: String): Result<ArcMission> = apiCall {
        val token = storage.accessToken ?: error("No session token")
        api.arcGenerateMission(ArcMissionRequest(targetName), "Bearer $token")
    }

    suspend fun arcCompleteMission(scorePercent: Int): Result<ArcMissionCompleteResponse> = apiCall {
        val token = storage.accessToken ?: error("No session token")
        api.arcCompleteMission(ArcMissionCompleteRequest(scorePercent), "Bearer $token")
    }

    // ARC Ops' real, installable practice-target APKs - see ArcPracticeAppService
    // server-side. Polled repeatedly while status is "queued"/"building".
    suspend fun arcPracticeAppStatus(targetId: String): Result<ArcPracticeAppStatusResponse> = apiCall {
        val token = storage.accessToken ?: error("No session token")
        api.arcPracticeAppStatus(targetId, "Bearer $token")
    }

    // "Today's Task" for Invest/ARC - see DailyTaskService server-side.
    suspend fun dailyTask(area: String): Result<DailyTaskResponse> = apiCall {
        val token = storage.accessToken ?: error("No session token")
        api.dailyTask(area, "Bearer $token")
    }

    suspend fun completeDailyTask(area: String): Result<Long> = apiCall {
        val token = storage.accessToken ?: error("No session token")
        api.completeDailyTask(area, "Bearer $token").exp
    }

    private suspend fun persistTokens(tokens: AuthTokens) {
        storage.accessToken = tokens.accessToken
        storage.refreshToken = tokens.refreshToken
        storage.userId = tokens.userId
        storage.role = tokens.role
        rememberCurrentAccount(tokens.userId, tokens.refreshToken)
    }

    // Adds/updates this account in the on-device "Switch Account" list -
    // called after every successful login/signup/2FA confirm and every
    // switch, so display info (nickname/email/avatar) stays current and the
    // account you're using right now is always in the list. Best-effort:
    // a failed profile fetch just means a slightly blanker switcher row,
    // never blocks the sign-in flow that triggered it.
    private suspend fun rememberCurrentAccount(userId: String, refreshToken: String) {
        val token = storage.accessToken ?: return
        val profile = try { api.getProfile(userId, "Bearer $token") } catch (e: Exception) { null }
        val updated = storage.savedAccounts.filter { it.userId != userId }.toMutableList()
        updated.add(
            0,
            SavedAccount(
                userId = userId,
                refreshToken = refreshToken,
                email = profile?.email,
                nickname = profile?.nickname,
                avatarUrl = profile?.avatarUrl,
            ),
        )
        storage.savedAccounts = updated
    }

    // Silently switches the active session to a previously-saved account,
    // using its saved refresh token to mint a fresh access token - no
    // password re-entry. Saves the OUTGOING account's latest refresh token
    // first (it may have rotated since it was last saved), so switching
    // back to it later still works.
    suspend fun switchAccount(targetUserId: String): Result<Unit> = apiCall {
        val currentUserId = storage.userId
        val currentRefresh = storage.refreshToken
        if (currentUserId != null && currentRefresh != null) {
            storage.savedAccounts = storage.savedAccounts.map {
                if (it.userId == currentUserId) it.copy(refreshToken = currentRefresh) else it
            }
        }
        val target = storage.savedAccounts.firstOrNull { it.userId == targetUserId }
            ?: error("That account isn't saved on this device")
        val tokens = try {
            api.refresh(RefreshRequest(target.refreshToken))
        } catch (e: HttpException) {
            // AuthService.refresh() throws AuthException (mapped to 400, NOT
            // 401 - see Application.kt's StatusPages) for both "no such
            // token" and "expired token", meaning this saved account no
            // longer exists (or its token was revoked) server-side - a stale
            // entry from before, not a real switch failure. Any HttpException
            // from this specific call is safe to treat this way, since it's
            // the only remote call made above. Drop it locally so it stops
            // showing up as a dead row that can never be switched to.
            removeSavedAccount(targetUserId)
            error("That account no longer exists — it's been removed from this device.")
        }
        persistTokens(tokens)
    }

    // Forgets a saved account on this device only - doesn't touch the
    // account itself server-side. Only ever called internally now (see
    // deleteSavedAccount below) - "Remove" in the switcher UI means a real
    // delete, not just forgetting the login locally.
    fun removeSavedAccount(userId: String) {
        storage.savedAccounts = storage.savedAccounts.filter { it.userId != userId }
    }

    // "Remove" in Switch Account - a REAL, permanent server-side delete
    // (see AccountService.deleteAccount), same as Settings' own "Delete
    // Account". Works for any saved account, not just the currently active
    // one: mints a fresh access token from that account's own saved refresh
    // token first (without switching the active UI session to it) so the
    // delete call is authenticated as that account. If the deleted account
    // WAS the active session, clears it too - can't stay "signed in" to an
    // account that no longer exists.
    suspend fun deleteSavedAccount(userId: String): Result<Unit> = apiCall {
        val account = storage.savedAccounts.firstOrNull { it.userId == userId } ?: error("Account not found on this device")
        val tokens = try {
            api.refresh(RefreshRequest(account.refreshToken))
        } catch (e: HttpException) {
            // AuthService.refresh() throws AuthException (mapped to 400, not
            // 401) for any invalid/expired token - already gone server-side
            // (stale entry, e.g. account was deleted some other way).
            // Removing it locally IS the delete in that case, so this still
            // counts as success rather than an error the user can never clear.
            removeSavedAccount(userId)
            if (storage.userId == userId) storage.clearSession()
            return@apiCall
        }
        api.deleteAccount(userId, "Bearer ${tokens.accessToken}")
        removeSavedAccount(userId)
        if (storage.userId == userId) storage.clearSession()
    }

    // Permanently deletes the account server-side (see AccountService) -
    // does NOT clear the local session itself, so the caller can confirm
    // success first, then clearSession()/navigate away deliberately. Does
    // purge the now-nonexistent account from the local "Switch Account"
    // list though - leaving it there would just be a dead entry that fails
    // to switch to (per Settings > More > Security: a deleted account is
    // genuinely gone, someone who wants it back has to sign up fresh).
    suspend fun deleteAccount(): Result<Unit> = apiCall {
        val uid = storage.userId ?: error("No signed-in user")
        val token = storage.accessToken ?: error("No session token")
        api.deleteAccount(uid, "Bearer $token")
        removeSavedAccount(uid)
    }

    // Settings > More > Security - up to 4 saved accounts per device;
    // adding a 5th requires removing one first (see
    // MemberSwitchAccountScreen's "+ ADD ANOTHER ACCOUNT" button).
    val canAddAnotherAccount: Boolean get() = storage.savedAccounts.size < MAX_SAVED_ACCOUNTS

    suspend fun getPhoneStatus(): Result<PhoneStatusResponse> = apiCall {
        val uid = storage.userId ?: error("No signed-in user")
        val token = storage.accessToken ?: error("No session token")
        api.getPhoneStatus(uid, "Bearer $token")
    }

    // Returns the dev code directly if Twilio isn't configured server-side
    // yet (see SecurityService.requestPhoneCode) - null once real SMS is live.
    suspend fun requestPhoneCode(phoneNumber: String): Result<String?> = apiCall {
        val uid = storage.userId ?: error("No signed-in user")
        val token = storage.accessToken ?: error("No session token")
        api.requestPhoneCode(uid, RequestPhoneCodeRequest(phoneNumber), "Bearer $token").devVerificationCode
    }

    suspend fun verifyPhoneCode(code: String): Result<Unit> = apiCall {
        val uid = storage.userId ?: error("No signed-in user")
        val token = storage.accessToken ?: error("No session token")
        api.verifyPhoneCode(uid, VerifyPhoneCodeRequest(code), "Bearer $token")
        Unit
    }

    fun logout() {
        storage.clearSession()
    }
}
