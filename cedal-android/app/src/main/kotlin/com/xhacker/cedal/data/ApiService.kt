package com.xhacker.cedal.data

import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming

interface ApiService {
    // Unauthenticated - see SignInScreen's full-screen gate for a banned/
    // admin-cleared account.
    @POST("appeals")
    suspend fun submitAppeal(@Body req: SubmitAppealRequest): Map<String, Boolean>

    @GET("admin/appeals")
    suspend fun listAppeals(@Header("Authorization") bearer: String): List<AppealDto>

    @POST("admin/appeals/{id}/dismiss")
    suspend fun dismissAppeal(@Path("id") id: String, @Header("Authorization") bearer: String): Map<String, Boolean>

    // Unauthenticated, checked even before/without a live session - see
    // UpdateGateState.
    @GET("app-version")
    suspend fun getAppVersion(): AppVersionDto

    @POST("app-version/decline")
    suspend fun declineUpdate(@Body req: DeclineUpdateRequest, @Header("Authorization") bearer: String): Map<String, Boolean>

    // Admin > App Updates - see AppUpdatePublishScreen.kt.
    @POST("admin/app-version")
    suspend fun setAppVersion(@Body req: SetAppVersionRequest, @Header("Authorization") bearer: String): Map<String, Boolean>

    @POST("auth/signup")
    suspend fun signup(@Body req: SignupRequest): SignupResponse

    @POST("auth/verify-email")
    suspend fun verifyEmail(@Body req: VerifyEmailRequest)

    @POST("auth/login")
    suspend fun login(@Body req: LoginRequest): LoginResponse

    @POST("auth/login/2fa")
    suspend fun confirmLoginTwoFactor(@Body req: TwoFactorLoginConfirmRequest): AuthTokens

    @POST("auth/refresh")
    suspend fun refresh(@Body req: RefreshRequest): AuthTokens

    @POST("auth/forgot-password")
    suspend fun forgotPassword(@Body req: ForgotPasswordRequest)

    @POST("auth/reset-password")
    suspend fun resetPassword(@Body req: ResetPasswordRequest)

    @POST("auth/node-password/verify")
    suspend fun verifyNodePassword(@Body req: NodePasswordVerifyRequest): NodePasswordVerifyResponse

    @POST("auth/passcode")
    suspend fun createPasscode(@Body req: CreatePasscodeRequest)

    @POST("auth/logout")
    suspend fun logout(@Body req: LogoutRequest)

    @POST("users/fcm-token")
    suspend fun registerFcmToken(@Body req: RegisterFcmTokenRequest, @Header("Authorization") bearer: String)

    @GET("users/{id}")
    suspend fun getProfile(@Path("id") id: String, @Header("Authorization") bearer: String): UserProfile

    @PUT("users/{id}")
    suspend fun updateProfile(@Path("id") id: String, @Body req: UpdateProfileRequest, @Header("Authorization") bearer: String): UserProfile

    // id here is the FRIEND being granted/revoked access, not the caller -
    // see CallService.setOverride server-side.
    @GET("users/{id}/number-share")
    suspend fun getNumberShareOverride(@Path("id") friendId: String, @Header("Authorization") bearer: String): NumberShareOverrideResponse

    @PUT("users/{id}/number-share")
    suspend fun setNumberShareOverride(@Path("id") friendId: String, @Body req: SetNumberShareOverrideRequest, @Header("Authorization") bearer: String): Map<String, Boolean>

    @DELETE("users/{id}")
    suspend fun deleteAccount(@Path("id") id: String, @Header("Authorization") bearer: String)

    @PUT("users/{id}/terms")
    suspend fun updateTerms(@Path("id") id: String, @Body req: TermsUpdateRequest, @Header("Authorization") bearer: String): UserProfile

    @PUT("users/{id}/passcode")
    suspend fun updatePasscode(@Path("id") id: String, @Body req: UpdatePasscodeRequest, @Header("Authorization") bearer: String)

    @PUT("users/{id}/link-email")
    suspend fun linkEmail(@Path("id") id: String, @Body req: LinkEmailRequest, @Header("Authorization") bearer: String): UserProfile

    @POST("users/{id}/2fa/request")
    suspend fun requestTwoFactorSetup(@Path("id") id: String, @Header("Authorization") bearer: String): TwoFactorSetupResponse

    @POST("users/{id}/2fa/confirm")
    suspend fun confirmTwoFactorSetup(@Path("id") id: String, @Body req: TwoFactorConfirmRequest, @Header("Authorization") bearer: String): UserProfile

    @POST("users/{id}/2fa/disable")
    suspend fun disableTwoFactor(@Path("id") id: String, @Header("Authorization") bearer: String): UserProfile

    @GET("users/{id}/phone")
    suspend fun getPhoneStatus(@Path("id") id: String, @Header("Authorization") bearer: String): PhoneStatusResponse

    // Live in-session ban detection - see MemberScaffold's poll.
    @GET("users/{id}/account-status")
    suspend fun getAccountStatus(@Path("id") id: String, @Header("Authorization") bearer: String): AccountStatusDto

    @POST("users/{id}/phone/request-code")
    suspend fun requestPhoneCode(@Path("id") id: String, @Body req: RequestPhoneCodeRequest, @Header("Authorization") bearer: String): RequestPhoneCodeResponse

    @POST("users/{id}/phone/verify")
    suspend fun verifyPhoneCode(@Path("id") id: String, @Body req: VerifyPhoneCodeRequest, @Header("Authorization") bearer: String): Map<String, Boolean>

    // ✚ > QR tab > Scan - see FriendService.findByPublicId.
    @GET("friends/by-public-id/{code}")
    suspend fun findUserByPublicId(@Path("code") code: String, @Header("Authorization") bearer: String): SearchUserResult

    @GET("friends/search")
    suspend fun searchUsers(
        @Query("q") q: String?,
        @Query("byGender") byGender: Boolean,
        @Query("byOccupation") byOccupation: Boolean,
        @Query("byHobby") byHobby: Boolean,
        @Query("byAge") byAge: Boolean,
        @Query("byBio") byBio: Boolean,
        @Header("Authorization") bearer: String,
    ): List<SearchUserResult>

    // Chat thread's proactive "this account has been deleted" check - see
    // FriendService.friendStatus.
    @GET("friends/{id}/status")
    suspend fun getFriendStatus(@Path("id") id: String, @Header("Authorization") bearer: String): FriendStatusResult

    // ✚ > Search's "from your contacts" prompt - see FriendService.matchContacts.
    @POST("friends/match-contacts")
    suspend fun matchContacts(@Body req: ContactMatchRequest, @Header("Authorization") bearer: String): List<SearchUserResult>

    @POST("friends/request")
    suspend fun sendFriendRequest(@Body req: FriendRequestCreate, @Header("Authorization") bearer: String)

    @GET("friends/requests")
    suspend fun listFriendRequests(@Header("Authorization") bearer: String): List<FriendRequestItem>

    @POST("friends/requests/{id}/accept")
    suspend fun acceptFriendRequest(@Path("id") id: String, @Header("Authorization") bearer: String)

    @POST("friends/requests/{id}/decline")
    suspend fun declineFriendRequest(@Path("id") id: String, @Header("Authorization") bearer: String)

    @DELETE("friends/requests/{id}")
    suspend fun cancelFriendRequest(@Path("id") id: String, @Header("Authorization") bearer: String)

    // Member > More > Bots (MemberBotsScreen.kt) - Round 1 CRUD only.
    @GET("bots")
    suspend fun listBots(@Header("Authorization") bearer: String): List<BotResponse>

    @POST("bots")
    suspend fun createBot(@Body req: BotCreate, @Header("Authorization") bearer: String): BotResponse

    @GET("bots/{id}")
    suspend fun getBot(@Path("id") id: String, @Header("Authorization") bearer: String): BotResponse

    @PUT("bots/{id}")
    suspend fun updateBot(@Path("id") id: String, @Body req: BotUpdate, @Header("Authorization") bearer: String): BotResponse

    @DELETE("bots/{id}")
    suspend fun deleteBot(@Path("id") id: String, @Header("Authorization") bearer: String)

    // Round 2 in-app test chat - owner-only, see BotBrainService's own doc
    // comment server-side.
    @GET("bots/{id}/test-chat")
    suspend fun getBotTestChatHistory(@Path("id") id: String, @Header("Authorization") bearer: String): List<BotTurnDto>

    @POST("bots/{id}/test-chat")
    suspend fun sendBotTestChatMessage(@Path("id") id: String, @Body req: BotTestChatRequest, @Header("Authorization") bearer: String): BotConverseResponse

    // Round 3 (2026-08-10) - real Telegram/WhatsApp connectivity. See
    // BotRoutes.kt server-side for why set-premium is admin-only (not
    // owner-scoped) and why /download streams a zip.
    @POST("bots/{id}/set-premium")
    suspend fun setBotPremium(@Path("id") id: String, @Body req: BotSetPremiumRequest, @Header("Authorization") bearer: String): BotResponse

    @GET("bots/{id}/reveal-secret")
    suspend fun revealBotSecret(@Path("id") id: String, @Header("Authorization") bearer: String): BotSecretResponse

    @Streaming
    @GET("bots/{id}/download")
    suspend fun downloadBot(@Path("id") id: String, @Header("Authorization") bearer: String): ResponseBody

    // "Delete User" (profile screen, distinct from Block) - see
    // FriendService.deleteUser server-side.
    @DELETE("friends/{id}")
    suspend fun deleteUser(@Path("id") id: String, @Header("Authorization") bearer: String)

    @GET("friends")
    suspend fun listFriends(@Header("Authorization") bearer: String): List<FriendSummary>

    @GET("chats")
    suspend fun listConversations(@Header("Authorization") bearer: String): List<ConversationSummary>

    @GET("chats/archived")
    suspend fun listArchivedConversations(@Header("Authorization") bearer: String): List<ConversationSummary>

    @GET("chats/hidden")
    suspend fun listHiddenConversations(@Header("Authorization") bearer: String): List<ConversationSummary>

    @GET("chats/{friendId}/messages")
    suspend fun getMessages(@Path("friendId") friendId: String, @Header("Authorization") bearer: String): List<ChatMessageDto>

    @GET("chats/{friendId}/pinned")
    suspend fun getPinnedState(@Path("friendId") friendId: String, @Header("Authorization") bearer: String): Map<String, Boolean>

    @POST("chats/{friendId}/messages")
    suspend fun sendMessage(@Path("friendId") friendId: String, @Body req: SendChatMessageRequest, @Header("Authorization") bearer: String): ChatMessageDto

    @PUT("chats/{friendId}/messages/{messageId}")
    suspend fun editMessage(@Path("friendId") friendId: String, @Path("messageId") messageId: String, @Body req: EditChatMessageRequest, @Header("Authorization") bearer: String): ChatMessageDto

    @POST("chats/{friendId}/typing")
    suspend fun pingTyping(@Path("friendId") friendId: String, @Header("Authorization") bearer: String): Map<String, Boolean>

    @GET("chats/typing")
    suspend fun getTypingFriendIds(@Header("Authorization") bearer: String): List<String>

    @POST("chats/{friendId}/mark-read")
    suspend fun markRead(@Path("friendId") friendId: String, @Body req: MarkReadRequest, @Header("Authorization") bearer: String): Map<String, Boolean>

    @POST("chats/{friendId}/purge-consumed-view-once")
    suspend fun purgeConsumedViewOnce(@Path("friendId") friendId: String, @Header("Authorization") bearer: String): Map<String, Boolean>

    @DELETE("chats/{friendId}/messages/{messageId}")
    suspend fun deleteMessage(@Path("friendId") friendId: String, @Path("messageId") messageId: String, @Header("Authorization") bearer: String)

    @POST("chats/{friendId}/messages/{messageId}/react")
    suspend fun reactToMessage(@Path("friendId") friendId: String, @Path("messageId") messageId: String, @Body req: ReactToMessageRequest, @Header("Authorization") bearer: String): Map<String, String>

    @POST("chats/{friendId}/messages/{messageId}/reveal")
    suspend fun revealMessage(@Path("friendId") friendId: String, @Path("messageId") messageId: String, @Header("Authorization") bearer: String): ChatMessageDto

    @POST("chats/{friendId}/messages/{messageId}/vote")
    suspend fun voteInPoll(@Path("friendId") friendId: String, @Path("messageId") messageId: String, @Body req: VotePollRequest, @Header("Authorization") bearer: String): Map<String, Int>

    @DELETE("chats/{friendId}")
    suspend fun deleteConversation(@Path("friendId") friendId: String, @Header("Authorization") bearer: String)

    // Group chat - see GroupChatThreadScreen.kt / GroupChatService server-side.
    @POST("groups")
    suspend fun createGroup(@Body req: CreateGroupRequest, @Header("Authorization") bearer: String): GroupDto

    @GET("groups/{groupId}")
    suspend fun getGroup(@Path("groupId") groupId: String, @Header("Authorization") bearer: String): GroupDto

    @PUT("groups/{groupId}/info")
    suspend fun updateGroupInfo(@Path("groupId") groupId: String, @Body req: UpdateGroupInfoRequest, @Header("Authorization") bearer: String): GroupDto

    @PUT("groups/{groupId}/settings")
    suspend fun updateGroupSettings(@Path("groupId") groupId: String, @Body req: UpdateGroupSettingsRequest, @Header("Authorization") bearer: String): GroupDto

    @PUT("groups/{groupId}/members/{targetUserId}/role")
    suspend fun setGroupRole(@Path("groupId") groupId: String, @Path("targetUserId") targetUserId: String, @Body req: SetGroupRoleRequest, @Header("Authorization") bearer: String): Map<String, Boolean>

    @POST("groups/{groupId}/messages/{messageId}/reveal")
    suspend fun revealGroupMessage(@Path("groupId") groupId: String, @Path("messageId") messageId: String, @Header("Authorization") bearer: String): GroupMessageDto

    @POST("groups/{groupId}/purge-consumed-view-once")
    suspend fun purgeConsumedGroupViewOnce(@Path("groupId") groupId: String, @Header("Authorization") bearer: String): Map<String, Boolean>

    @GET("groups/{groupId}/messages")
    suspend fun getGroupMessages(
        @Path("groupId") groupId: String,
        @Header("Authorization") bearer: String,
        @Query("before") beforeTimestamp: Long? = null,
    ): List<GroupMessageDto>

    @POST("groups/{groupId}/messages")
    suspend fun sendGroupMessage(@Path("groupId") groupId: String, @Body req: SendGroupMessageRequest, @Header("Authorization") bearer: String): GroupMessageDto

    @PUT("groups/{groupId}/messages/{messageId}")
    suspend fun editGroupMessage(@Path("groupId") groupId: String, @Path("messageId") messageId: String, @Body req: EditGroupMessageRequest, @Header("Authorization") bearer: String): GroupMessageDto

    @DELETE("groups/{groupId}/messages/{messageId}")
    suspend fun deleteGroupMessage(@Path("groupId") groupId: String, @Path("messageId") messageId: String, @Header("Authorization") bearer: String)

    @POST("groups/{groupId}/messages/{messageId}/keep")
    suspend fun keepGroupMessage(@Path("groupId") groupId: String, @Path("messageId") messageId: String, @Header("Authorization") bearer: String): Map<String, Boolean>

    @POST("groups/{groupId}/messages/{messageId}/react")
    suspend fun reactToGroupMessage(@Path("groupId") groupId: String, @Path("messageId") messageId: String, @Body req: ReactToGroupMessageRequest, @Header("Authorization") bearer: String): Map<String, String>

    @POST("groups/{groupId}/messages/{messageId}/vote")
    suspend fun voteInGroupPoll(@Path("groupId") groupId: String, @Path("messageId") messageId: String, @Body req: VoteInGroupPollRequest, @Header("Authorization") bearer: String): Map<String, Int>

    @POST("groups/{groupId}/members")
    suspend fun addGroupMember(@Path("groupId") groupId: String, @Body req: AddGroupMemberRequest, @Header("Authorization") bearer: String): Map<String, Boolean>

    @DELETE("groups/{groupId}/members/{targetUserId}")
    suspend fun removeGroupMember(@Path("groupId") groupId: String, @Path("targetUserId") targetUserId: String, @Header("Authorization") bearer: String): Map<String, Boolean>

    @POST("groups/{groupId}/leave")
    suspend fun leaveGroup(@Path("groupId") groupId: String, @Body req: LeaveGroupRequest, @Header("Authorization") bearer: String): Map<String, Boolean>

    @POST("groups/{groupId}/clear")
    suspend fun clearGroupChat(@Path("groupId") groupId: String, @Header("Authorization") bearer: String): Map<String, Boolean>

    @POST("groups/{groupId}/pin-message/{messageId}")
    suspend fun pinGroupMessage(@Path("groupId") groupId: String, @Path("messageId") messageId: String, @Header("Authorization") bearer: String): GroupDto

    @POST("groups/{groupId}/unpin-message")
    suspend fun unpinGroupMessage(@Path("groupId") groupId: String, @Header("Authorization") bearer: String): GroupDto

    @POST("groups/{groupId}/report")
    suspend fun reportGroup(@Path("groupId") groupId: String, @Body req: ReportGroupRequest, @Header("Authorization") bearer: String): Map<String, Boolean>

    @POST("groups/{groupId}/block")
    suspend fun blockGroup(@Path("groupId") groupId: String, @Header("Authorization") bearer: String): Map<String, Boolean>

    @DELETE("groups/{groupId}/block")
    suspend fun unblockGroup(@Path("groupId") groupId: String, @Header("Authorization") bearer: String): Map<String, Boolean>

    @POST("groups/{groupId}/mute")
    suspend fun muteGroup(@Path("groupId") groupId: String, @Header("Authorization") bearer: String): Map<String, Boolean>

    @DELETE("groups/{groupId}/mute")
    suspend fun unmuteGroup(@Path("groupId") groupId: String, @Header("Authorization") bearer: String): Map<String, Boolean>

    @PUT("groups/{groupId}/dm-preference")
    suspend fun setGroupDmOverride(@Path("groupId") groupId: String, @Body req: SetDmOverrideRequest, @Header("Authorization") bearer: String): Map<String, Boolean>

    @GET("groups/search")
    suspend fun searchPublicGroups(@Query("q") query: String, @Header("Authorization") bearer: String): List<GroupSearchResultDto>

    @GET("groups/by-token/{token}")
    suspend fun getGroupByToken(@Path("token") token: String, @Header("Authorization") bearer: String): GroupLinkPreviewDto

    @POST("groups/{groupId}/reset-link")
    suspend fun resetGroupInviteLink(@Path("groupId") groupId: String, @Header("Authorization") bearer: String): GroupDto

    @POST("groups/{groupId}/join-requests")
    suspend fun requestToJoinGroup(@Path("groupId") groupId: String, @Header("Authorization") bearer: String): Map<String, Boolean>

    @GET("groups/{groupId}/join-requests")
    suspend fun listGroupJoinRequests(@Path("groupId") groupId: String, @Header("Authorization") bearer: String): List<GroupJoinRequestDto>

    @POST("groups/{groupId}/join-requests/{targetUserId}/approve")
    suspend fun approveGroupJoinRequest(@Path("groupId") groupId: String, @Path("targetUserId") targetUserId: String, @Header("Authorization") bearer: String): Map<String, Boolean>

    @POST("groups/{groupId}/join-requests/{targetUserId}/reject")
    suspend fun rejectGroupJoinRequest(@Path("groupId") groupId: String, @Path("targetUserId") targetUserId: String, @Header("Authorization") bearer: String): Map<String, Boolean>

    // Settings > Groups > "Request" - the invited user's own pending list,
    // distinct from the join-requests above (opposite direction).
    @GET("groups/add-requests")
    suspend fun listGroupAddRequests(@Header("Authorization") bearer: String): List<GroupAddRequestDto>

    @POST("groups/add-requests/{groupId}/respond")
    suspend fun respondToGroupAddRequest(@Path("groupId") groupId: String, @Body req: RespondGroupAddRequest, @Header("Authorization") bearer: String): Map<String, Boolean>

    // Settings > Groups follow-up - see GroupTypingService server-side.
    @POST("groups/{groupId}/typing")
    suspend fun pingGroupTyping(@Path("groupId") groupId: String, @Header("Authorization") bearer: String): Map<String, Boolean>

    @GET("groups/{groupId}/typing")
    suspend fun getGroupTypingUserIds(@Path("groupId") groupId: String, @Header("Authorization") bearer: String): List<String>

    @GET("groups/{groupId}/media-summary")
    suspend fun getGroupMediaSummary(@Path("groupId") groupId: String, @Header("Authorization") bearer: String): MediaSummaryDto

    @DELETE("groups/{groupId}/media")
    suspend fun clearGroupMedia(@Path("groupId") groupId: String, @Header("Authorization") bearer: String): Map<String, Boolean>

    @POST("saved-messages")
    suspend fun saveMessage(@Body req: SaveMessageRequest, @Header("Authorization") bearer: String): SavedMessageDto

    @GET("saved-messages")
    suspend fun listSavedMessages(@Header("Authorization") bearer: String): List<SavedMessageDto>

    @DELETE("saved-messages/{id}")
    suspend fun deleteSavedMessage(@Path("id") id: String, @Header("Authorization") bearer: String): Map<String, Boolean>

    @POST("chats/bulk-action")
    suspend fun bulkChatAction(@Body req: BulkChatActionRequest, @Header("Authorization") bearer: String): Map<String, Boolean>

    @POST("chats/{friendId}/block")
    suspend fun blockFriend(@Path("friendId") friendId: String, @Header("Authorization") bearer: String): Map<String, Boolean>

    @POST("chats/{friendId}/unblock")
    suspend fun unblockFriend(@Path("friendId") friendId: String, @Header("Authorization") bearer: String): Map<String, Boolean>

    @GET("chats/{friendId}/blocked")
    suspend fun isBlocked(@Path("friendId") friendId: String, @Header("Authorization") bearer: String): Map<String, Boolean>

    @POST("chats/{friendId}/report")
    suspend fun reportFriend(@Path("friendId") friendId: String, @Body req: ReportUserRequest, @Header("Authorization") bearer: String): Map<String, Boolean>

    @GET("wallet/balance")
    suspend fun getWalletBalance(@Header("Authorization") bearer: String): WalletBalanceResponse

    @POST("wallet/send")
    suspend fun sendWallet(@Body req: WalletSendRequest, @Header("Authorization") bearer: String): WalletBalanceResponse

    @GET("wallet/transactions")
    suspend fun listWalletTransactions(@Query("filter") filter: String?, @Header("Authorization") bearer: String): List<WalletTransactionItem>

    @POST("trades")
    suspend fun postTrade(@Body req: TradeCreateRequest, @Header("Authorization") bearer: String)

    @GET("notifications")
    suspend fun listNotifications(@Header("Authorization") bearer: String): List<NotificationItem>

    @POST("notifications/{id}/credit")
    suspend fun creditNotification(@Path("id") id: String, @Header("Authorization") bearer: String): WalletBalanceResponse

    @DELETE("notifications/{id}")
    suspend fun deleteNotification(@Path("id") id: String, @Header("Authorization") bearer: String)

    @GET("market/crypto")
    suspend fun listMarkets(@Header("Authorization") bearer: String): List<MarketAssetSummary>

    @GET("market/crypto/{id}")
    suspend fun getMarketDetail(@Path("id") id: String, @Header("Authorization") bearer: String): MarketAssetDetail

    @GET("portfolio")
    suspend fun getPortfolio(@Header("Authorization") bearer: String): PortfolioResponse

    @POST("portfolio/buy")
    suspend fun buyAsset(@Body req: InvestTradeRequest, @Header("Authorization") bearer: String)

    @POST("portfolio/sell")
    suspend fun sellAsset(@Body req: InvestTradeRequest, @Header("Authorization") bearer: String)

    @GET("portfolio/transactions")
    suspend fun listPortfolioTransactions(@Header("Authorization") bearer: String): List<PortfolioTransactionItem>

    @GET("portfolio/watchlist")
    suspend fun listWatchlist(@Header("Authorization") bearer: String): List<WatchlistItem>

    @POST("portfolio/watchlist")
    suspend fun addToWatchlist(@Body req: WatchlistAddRequest, @Header("Authorization") bearer: String)

    @DELETE("portfolio/watchlist/{symbol}")
    suspend fun removeFromWatchlist(@Path("symbol") symbol: String, @Header("Authorization") bearer: String)

    @GET("code/languages")
    suspend fun listCodeLanguages(@Header("Authorization") bearer: String): List<CodeLanguageItem>

    @POST("code/run")
    suspend fun runCode(@Body req: CodeRunRequest, @Header("Authorization") bearer: String): CodeRunResult

    @POST("code/explain-error")
    suspend fun explainError(@Body req: ExplainErrorRequest, @Header("Authorization") bearer: String): ExplainErrorResponse

    @POST("code/android-build")
    suspend fun requestAndroidBuild(@Body req: AndroidBuildRequest, @Header("Authorization") bearer: String): AndroidBuildJob

    @GET("code/android-build/{jobId}")
    suspend fun getAndroidBuildJob(@Path("jobId") jobId: String, @Header("Authorization") bearer: String): AndroidBuildJob

    @POST("code/gui-session")
    suspend fun startGuiSession(@Body req: GuiSessionRequest, @Header("Authorization") bearer: String): GuiSessionJob

    @POST("code/gui-session/{jobId}/stop")
    suspend fun stopGuiSession(@Path("jobId") jobId: String, @Header("Authorization") bearer: String)

    // Code area "Documents" <-> GitHub sync - see CodeGithubModels.kt /
    // CodeGithubSyncService server-side.
    @GET("code/github/authorize-url")
    suspend fun getGithubAuthorizeUrl(@Header("Authorization") bearer: String): GithubAuthorizeUrlDto

    @GET("code/github/status")
    suspend fun getGithubStatus(@Header("Authorization") bearer: String): GithubStatusDto

    @GET("code/github/repos")
    suspend fun listGithubRepos(@Header("Authorization") bearer: String): List<GithubRepoDto>

    @POST("code/github/select-repo")
    suspend fun selectGithubRepo(@Body req: SelectGithubRepoRequest, @Header("Authorization") bearer: String): GithubStatusDto

    @POST("code/github/disconnect")
    suspend fun disconnectGithub(@Header("Authorization") bearer: String): Map<String, Boolean>

    @POST("code/github/sync/start")
    suspend fun startGithubSync(@Body req: SyncStartRequest, @Header("Authorization") bearer: String): SyncStartResponseDto

    @GET("code/github/sync/{jobId}")
    suspend fun getGithubSyncJob(@Path("jobId") jobId: String, @Header("Authorization") bearer: String): SyncJobDto

    @POST("code/github/sync/resolve")
    suspend fun resolveGithubConflict(@Body req: ResolveConflictRequest, @Header("Authorization") bearer: String): ResolveConflictResponseDto

    @GET("code/ai-request/history")
    suspend fun getAiRequestHistory(@Header("Authorization") bearer: String): List<AiChangeRequestDto>

    @POST("code/ai-request")
    suspend fun submitAiRequest(@Body req: AiChangeRequestBody, @Header("Authorization") bearer: String): AiChangeRequestDto

    @GET("code/ai-request/{id}")
    suspend fun getAiRequest(@Path("id") id: String, @Header("Authorization") bearer: String): AiChangeRequestDto

    @POST("code/ai-request/{id}/file-action-executed")
    suspend fun markFileActionExecuted(@Path("id") id: String, @Header("Authorization") bearer: String): AiChangeRequestDto

    @PUT("code/ai-request/{id}")
    suspend fun editAiRequest(@Path("id") id: String, @Body req: EditAiRequestTextBody, @Header("Authorization") bearer: String): AiChangeRequestDto

    @DELETE("code/ai-request/{id}")
    suspend fun deleteAiRequest(@Path("id") id: String, @Header("Authorization") bearer: String)

    @DELETE("code/ai-request/history")
    suspend fun deleteAiRequestHistory(@Header("Authorization") bearer: String)

    @GET("admin/ai-requests")
    suspend fun listPendingAiRequests(@Header("Authorization") bearer: String): List<AiChangeRequestDto>

    @GET("admin/user-reports")
    suspend fun listUserReports(@Header("Authorization") bearer: String): List<UserReportDto>

    @POST("admin/user-reports/{id}/dismiss")
    suspend fun dismissUserReport(@Path("id") id: String, @Header("Authorization") bearer: String): Map<String, Boolean>

    @GET("admin/message-reports")
    suspend fun listMessageReports(@Header("Authorization") bearer: String): List<MessageReportDto>

    @POST("admin/message-reports/{id}/dismiss")
    suspend fun dismissMessageReport(@Path("id") id: String, @Header("Authorization") bearer: String): Map<String, Boolean>

    @GET("admin/godmode/users")
    suspend fun listGodmodeUsers(@Header("Authorization") bearer: String): List<GodmodeUserDto>

    @POST("admin/godmode/{id}/ban")
    suspend fun banUser(@Path("id") id: String, @Header("Authorization") bearer: String): Map<String, Boolean>

    @POST("admin/godmode/{id}/unban")
    suspend fun unbanUser(@Path("id") id: String, @Header("Authorization") bearer: String): Map<String, Boolean>

    @POST("admin/godmode/{id}/permanent-ban")
    suspend fun permanentBanUser(@Path("id") id: String, @Header("Authorization") bearer: String): Map<String, Boolean>

    @POST("admin/godmode/{id}/clear-data")
    suspend fun clearUserData(@Path("id") id: String, @Header("Authorization") bearer: String): Map<String, Boolean>

    // Developer terminal > Manage Developer Access - owner-only, see
    // DeveloperAccessService server-side.
    @GET("admin/developer/users")
    suspend fun listDeveloperAccessUsers(@Header("Authorization") bearer: String): List<GodmodeUserDto>

    @POST("admin/developer/{id}/grant")
    suspend fun grantDeveloperAccess(@Path("id") id: String, @Header("Authorization") bearer: String): Map<String, Boolean>

    @POST("admin/developer/{id}/revoke")
    suspend fun revokeDeveloperAccess(@Path("id") id: String, @Header("Authorization") bearer: String): Map<String, Boolean>

    @POST("admin/developer/{id}/generate-key")
    suspend fun generateDeveloperKey(@Path("id") id: String, @Header("Authorization") bearer: String): Map<String, String>

    // ManageDeveloperAccessScreen's SEND KEY button - see
    // DeveloperAccessService.sendKeyMessage.
    @POST("admin/developer/{id}/send-key")
    suspend fun sendDeveloperKey(@Path("id") id: String, @Body req: SendDeveloperKeyRequest, @Header("Authorization") bearer: String)

    // Cedal Team chat thread's own two self-service actions - see
    // DeveloperAccessService.generateKeyForSelf/revokeSelf.
    @POST("developer/access/generate-key")
    suspend fun generateOwnDeveloperKey(@Header("Authorization") bearer: String): Map<String, Boolean>

    @POST("developer/access/revoke")
    suspend fun revokeOwnDeveloperAccess(@Header("Authorization") bearer: String): Map<String, Boolean>

    // Developer Mode's submit -> review -> approve/deny pipeline - see
    // DeveloperSubmissionService.
    @POST("developer/submissions")
    suspend fun submitDeveloperPatch(@Body req: SubmitDeveloperPatchRequest, @Header("Authorization") bearer: String): DeveloperSubmissionDto

    @GET("developer/submissions")
    suspend fun listMyDeveloperSubmissions(@Header("Authorization") bearer: String): List<DeveloperSubmissionDto>

    @GET("developer/submissions/{id}")
    suspend fun getDeveloperSubmission(@Path("id") id: String, @Header("Authorization") bearer: String): DeveloperSubmissionDto

    @GET("admin/developer-submissions")
    suspend fun listPendingDeveloperSubmissions(@Header("Authorization") bearer: String): List<DeveloperSubmissionDto>

    @POST("admin/developer-submissions/{id}/approve")
    suspend fun approveDeveloperSubmission(@Path("id") id: String, @Header("Authorization") bearer: String): DeveloperSubmissionDto

    @POST("admin/developer-submissions/{id}/deny")
    suspend fun denyDeveloperSubmission(@Path("id") id: String, @Body req: DenyDeveloperSubmissionRequest, @Header("Authorization") bearer: String): DeveloperSubmissionDto

    @GET("users/{id}/popularity")
    suspend fun getPopularitySettings(@Path("id") id: String, @Header("Authorization") bearer: String): PopularitySettingsDto

    @PUT("users/{id}/popularity")
    suspend fun setPopularitySettings(@Path("id") id: String, @Body req: PopularitySettingsDto, @Header("Authorization") bearer: String): PopularitySettingsDto

    @GET("users/{id}/popularity/{friendId}")
    suspend fun getChatPopularityOverride(@Path("id") id: String, @Path("friendId") friendId: String, @Header("Authorization") bearer: String): ChatPopularityOverrideDto

    @PUT("users/{id}/popularity/{friendId}")
    suspend fun setChatPopularityOverride(@Path("id") id: String, @Path("friendId") friendId: String, @Body req: ChatPopularityOverrideDto, @Header("Authorization") bearer: String): ChatPopularityOverrideDto

    @GET("users/{id}/achievements")
    suspend fun listAchievements(@Path("id") id: String, @Header("Authorization") bearer: String): List<AchievementDto>

    @POST("users/{id}/achievements/trigger")
    suspend fun triggerAchievement(@Path("id") id: String, @Body req: TriggerAchievementRequest, @Header("Authorization") bearer: String): Map<String, Boolean>

    @PUT("users/{id}/active-badge")
    suspend fun setActiveBadge(@Path("id") id: String, @Body req: SetActiveBadgeRequest, @Header("Authorization") bearer: String): UserProfile

    @GET("users/{id}/popups/pending")
    suspend fun pollPendingPopups(@Path("id") id: String, @Header("Authorization") bearer: String): List<PendingPopupDto>

    @POST("admin/ai-requests/{id}/approve")
    suspend fun approveAiRequest(@Path("id") id: String, @Header("Authorization") bearer: String): AiChangeRequestDto

    @POST("admin/ai-requests/{id}/reject")
    suspend fun rejectAiRequest(@Path("id") id: String, @Header("Authorization") bearer: String)

    @POST("learn/complete-lesson")
    suspend fun completeLesson(@Body req: CompleteLessonRequest, @Header("Authorization") bearer: String): CompleteLessonResponse

    @POST("code/backer-review")
    suspend fun backerReview(@Body req: BackerReviewRequest, @Header("Authorization") bearer: String): BackerReviewResponse

    @GET("arc/chat")
    suspend fun getArcChatHistory(@Header("Authorization") bearer: String): ArcChatHistoryResponse

    @POST("arc/chat")
    suspend fun arcChat(@Body req: ArcChatRequest, @Header("Authorization") bearer: String): ArcChatResponse

    @PUT("arc/chat/{id}")
    suspend fun editArcMessage(@Path("id") id: String, @Body req: EditAiMessageRequest, @Header("Authorization") bearer: String): ArcChatMessageDto

    @DELETE("arc/chat/{id}")
    suspend fun deleteArcMessage(@Path("id") id: String, @Header("Authorization") bearer: String)

    @DELETE("arc/history")
    suspend fun deleteArcHistory(@Header("Authorization") bearer: String)

    @GET("alucard/chat")
    suspend fun getAlucardChatHistory(@Header("Authorization") bearer: String): AlucardChatHistoryResponse

    @POST("alucard/chat")
    suspend fun alucardChat(@Body req: AlucardChatRequest, @Header("Authorization") bearer: String): AlucardChatResponse

    @PUT("alucard/chat/{id}")
    suspend fun editAlucardMessage(@Path("id") id: String, @Body req: EditAiMessageRequest, @Header("Authorization") bearer: String): AlucardChatMessageDto

    @DELETE("alucard/chat/{id}")
    suspend fun deleteAlucardMessage(@Path("id") id: String, @Header("Authorization") bearer: String)

    @DELETE("alucard/history")
    suspend fun deleteAlucardHistory(@Header("Authorization") bearer: String)

    @GET("corneal/chat")
    suspend fun getCornealChatHistory(@Header("Authorization") bearer: String): CornealChatHistoryResponse

    @POST("corneal/chat")
    suspend fun cornealChat(@Body req: CornealChatRequest, @Header("Authorization") bearer: String): CornealChatResponse

    @POST("corneal/call-out/reject")
    suspend fun rejectCallOut(@Body req: RejectCallOutRequest, @Header("Authorization") bearer: String): Map<String, Boolean>

    @DELETE("corneal/history")
    suspend fun deleteCornealHistory(@Header("Authorization") bearer: String)

    @PUT("corneal/chat/{id}")
    suspend fun editCornealMessage(@Path("id") id: String, @Body req: EditAiMessageRequest, @Header("Authorization") bearer: String): CornealChatMessageDto

    @DELETE("corneal/chat/{id}")
    suspend fun deleteCornealMessage(@Path("id") id: String, @Header("Authorization") bearer: String)

    @GET("messages/pins")
    suspend fun listPinnedMessages(@Header("Authorization") bearer: String): List<MessagePinDto>

    @POST("messages/pin")
    suspend fun pinMessage(@Body req: PinMessageRequest, @Header("Authorization") bearer: String): MessagePinDto

    @POST("messages/pins/{id}/unpin")
    suspend fun unpinMessage(@Path("id") id: String, @Header("Authorization") bearer: String)

    @POST("messages/report")
    suspend fun reportMessage(@Body req: ReportMessageRequest, @Header("Authorization") bearer: String)

    @GET("feed")
    suspend fun listFeedPosts(@Header("Authorization") bearer: String): List<SystemFeedPostDto>

    @POST("feed")
    suspend fun createFeedPost(@Body req: CreateFeedPostRequest, @Header("Authorization") bearer: String): SystemFeedPostDto

    @POST("feed/{postId}/react")
    suspend fun reactToFeedPost(@Path("postId") postId: String, @Body req: ReactToFeedPostRequest, @Header("Authorization") bearer: String): Map<String, String>

    @Multipart
    @POST("uploads/image")
    suspend fun uploadImage(@Query("kind") kind: String, @Part file: MultipartBody.Part, @Header("Authorization") bearer: String): UploadImageResponse

    @GET("stickers")
    suspend fun listMyStickers(@Header("Authorization") bearer: String): List<StickerDto>

    @POST("stickers")
    suspend fun createSticker(@Body req: CreateStickerRequest, @Header("Authorization") bearer: String): StickerDto

    @GET("theme-packs")
    suspend fun listThemePacks(@Header("Authorization") bearer: String): List<ThemePackDto>

    @POST("theme-packs/{packId}/purchase")
    suspend fun purchaseThemePack(@Path("packId") packId: String, @Header("Authorization") bearer: String): ThemePackPurchaseResponse

    @POST("arc/ops/mission")
    suspend fun arcGenerateMission(@Body req: ArcMissionRequest, @Header("Authorization") bearer: String): ArcMission

    @POST("arc/ops/complete")
    suspend fun arcCompleteMission(@Body req: ArcMissionCompleteRequest, @Header("Authorization") bearer: String): ArcMissionCompleteResponse

    @GET("arc/ops/practice-app/{targetId}")
    suspend fun arcPracticeAppStatus(@Path("targetId") targetId: String, @Header("Authorization") bearer: String): ArcPracticeAppStatusResponse

    @GET("daily-task/{area}")
    suspend fun dailyTask(@Path("area") area: String, @Header("Authorization") bearer: String): DailyTaskResponse

    @POST("daily-task/{area}/complete")
    suspend fun completeDailyTask(@Path("area") area: String, @Header("Authorization") bearer: String): DailyTaskCompleteResponse
}
