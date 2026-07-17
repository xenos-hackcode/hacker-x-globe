package com.xhacker.cedal.viewmodel

import androidx.lifecycle.ViewModel
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
import com.xhacker.cedal.data.FriendRequestCreate
import com.xhacker.cedal.data.FriendRequestItem
import com.xhacker.cedal.data.ChatMessageDto
import com.xhacker.cedal.data.ConversationSummary
import com.xhacker.cedal.data.FriendSummary
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
    suspend fun signup(email: String?, password: String?, guest: Boolean, deviceId: String? = null): Result<SignupResponse> =
        apiCall {
            val res = api.signup(SignupRequest(email, password, guest, TermsConfig.CURRENT_VERSION, deviceId))
            res.tokens?.let { persistTokens(it) }
            storage.userId = res.userId
            res
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

    suspend fun getMessages(friendId: String): Result<List<ChatMessageDto>> = apiCall {
        val token = storage.accessToken ?: error("No session token")
        api.getMessages(friendId, "Bearer $token")
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
        pollQuestion: String? = null,
        pollOptions: List<String>? = null,
    ): Result<ChatMessageDto> = apiCall {
        val token = storage.accessToken ?: error("No session token")
        api.sendMessage(
            friendId,
            SendChatMessageRequest(text, replyToId, isSticker, mediaUrl, mediaType, fileName, viewOnce, pollQuestion, pollOptions),
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
    ): Result<AiChangeRequestDto> = apiCall {
        val token = storage.accessToken ?: error("No session token")
        api.submitAiRequest(AiChangeRequestBody(text, treePaths, currentFile, replyToId, mediaUrl, mediaType, fileName), "Bearer $token")
    }

    suspend fun getAiRequest(id: String): Result<AiChangeRequestDto> = apiCall {
        val token = storage.accessToken ?: error("No session token")
        api.getAiRequest(id, "Bearer $token")
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

    // Corneal - the app-wide help assistant reachable from Chats. See
    // CornealChatService server-side for what it actually knows about.
    // Same persisted-history shape as ARC's Assistant above.
    suspend fun getCornealChatHistory(): Result<List<CornealChatMessageDto>> = apiCall {
        val token = storage.accessToken ?: error("No session token")
        api.getCornealChatHistory("Bearer $token").messages
    }

    suspend fun cornealChat(
        message: String,
        replyToId: String? = null,
        chatContext: ChatContextDto? = null,
        mediaUrl: String? = null,
        mediaType: String? = null,
        fileName: String? = null,
    ): Result<CornealChatMessageDto> = apiCall {
        val token = storage.accessToken ?: error("No session token")
        api.cornealChat(CornealChatRequest(message, replyToId, chatContext, mediaUrl, mediaType, fileName), "Bearer $token").message
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
        val tokens = api.refresh(RefreshRequest(target.refreshToken))
        persistTokens(tokens)
    }

    // Forgets a saved account on this device only - doesn't touch the
    // account itself server-side, and if it's the currently active one,
    // doesn't sign it out (just stops offering it in the switcher).
    fun removeSavedAccount(userId: String) {
        storage.savedAccounts = storage.savedAccounts.filter { it.userId != userId }
    }

    // Permanently deletes the account server-side (see AccountService) -
    // does NOT clear the local session itself, so the caller can confirm
    // success first, then clearSession()/navigate away deliberately.
    suspend fun deleteAccount(): Result<Unit> = apiCall {
        val uid = storage.userId ?: error("No signed-in user")
        val token = storage.accessToken ?: error("No session token")
        api.deleteAccount(uid, "Bearer $token")
    }

    fun logout() {
        storage.clearSession()
    }
}
