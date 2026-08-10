package com.xhacker.cedal.ui.nav

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.xhacker.cedal.data.TermsConfig
import com.xhacker.cedal.ui.AccountGateState
import com.xhacker.cedal.ui.UpdateGateState
import com.xhacker.cedal.ui.screens.AccountGateScreen
import com.xhacker.cedal.ui.screens.ForceUpdateScreen
import com.xhacker.cedal.ui.screens.UpdateBanner
import com.xhacker.cedal.ui.screens.UpdateCheckEffect
import com.xhacker.cedal.ui.AppLockState
import com.xhacker.cedal.ui.CornealBubbleState
import com.xhacker.cedal.ui.screens.member.AlucardBubbleOverlay
import com.xhacker.cedal.ui.screens.member.CodeTab
import com.xhacker.cedal.ui.screens.member.DeveloperHomeRoute
import com.xhacker.cedal.ui.screens.member.DeveloperSubmissionApprovalBody
import com.xhacker.cedal.ui.screens.member.DeveloperSubmitBody
import com.xhacker.cedal.ui.screens.member.MemberBackBar
import com.xhacker.cedal.ui.screens.member.MemberCodeBody
import com.xhacker.cedal.ui.screens.member.CornealBubbleOverlay
import com.xhacker.cedal.ui.screens.member.CornealFloatingWindow
import com.xhacker.cedal.ui.screens.CreatePasscodeScreen
import com.xhacker.cedal.ui.screens.EnterPasscodeScreen
import com.xhacker.cedal.ui.screens.ForgotPasswordScreen
import com.xhacker.cedal.ui.screens.LoadingScreen
import com.xhacker.cedal.ui.screens.OwnerHomeScreen
import com.xhacker.cedal.ui.screens.SignInScreen
import com.xhacker.cedal.ui.screens.SignUpScreen
import com.xhacker.cedal.ui.screens.TermsGateScreen
import com.xhacker.cedal.ui.screens.member.BankDebtBody
import com.xhacker.cedal.ui.screens.member.BankNotificationsBody
import com.xhacker.cedal.ui.screens.member.BankRouterBody
import com.xhacker.cedal.ui.screens.member.BankTradeBody
import com.xhacker.cedal.ui.screens.member.CornealChatBody
import com.xhacker.cedal.ui.screens.member.AiRequestApprovalBody
import com.xhacker.cedal.ui.screens.member.ArchivedChatsBody
import com.xhacker.cedal.ui.screens.member.HiddenChatsBody
import com.xhacker.cedal.ui.screens.member.SavedMessagesBody
import com.xhacker.cedal.ui.screens.member.CreateGroupBody
import com.xhacker.cedal.ui.screens.member.GroupChatThreadBody
import com.xhacker.cedal.ui.screens.member.GroupLinkJoinBody
import com.xhacker.cedal.ui.screens.member.GroupProfileBody
import com.xhacker.cedal.ui.screens.member.GuiSessionBody
import com.xhacker.cedal.ui.screens.member.AchievementsBody
import com.xhacker.cedal.ui.screens.member.AdminReviewBody
import com.xhacker.cedal.ui.screens.member.AppUpdatePublishBody
import com.xhacker.cedal.ui.screens.member.GodmodeBody
import com.xhacker.cedal.ui.screens.member.AlucardChatBody
import com.xhacker.cedal.ui.screens.member.ManageDeveloperAccessBody
import com.xhacker.cedal.ui.screens.member.MemberAboutBody
import com.xhacker.cedal.ui.screens.member.MemberBotsListBody
import com.xhacker.cedal.ui.screens.member.MemberBotEditBody
import com.xhacker.cedal.ui.screens.member.MemberBotTestChatBody
import com.xhacker.cedal.ui.screens.member.MemberSecurityBody
import com.xhacker.cedal.ui.screens.member.MemberChatThreadBody
import com.xhacker.cedal.ui.screens.member.MemberFriendProfileBody
import com.xhacker.cedal.ui.screens.member.MemberSwitchAccountBody
import com.xhacker.cedal.ui.screens.member.InvestRouterBody
import com.xhacker.cedal.ui.screens.member.MemberHistoryBody
import com.xhacker.cedal.ui.screens.member.MemberHomeRoute
import com.xhacker.cedal.ui.screens.member.MemberTab
import com.xhacker.cedal.ui.screens.member.PinnedMessagesBody
import com.xhacker.cedal.ui.screens.member.MemberProfileBody
import com.xhacker.cedal.ui.screens.member.MemberRulesBody
import com.xhacker.cedal.ui.screens.member.MemberSearchBody
import com.xhacker.cedal.ui.screens.member.MemberSettingsBody
import com.xhacker.cedal.ui.screens.member.MemberShopBody
import com.xhacker.cedal.ui.screens.member.SystemFeedBody
import com.xhacker.cedal.viewmodel.AuthViewModel

@Composable
fun CedalNavGraph() {
    val navController = rememberNavController()
    val lockViewModel: AuthViewModel = hiltViewModel()

    // Force-update gate - checked once per process start, before anything
    // else. If a previous run already tripped the 2-week grace period,
    // storage.forceUpdateGate is already true from last time too - no need
    // to wait for this fresh check to fire again before blocking entry.
    UpdateCheckEffect()
    if (lockViewModel.storage.forceUpdateGate || UpdateGateState.forceGate) {
        ForceUpdateScreen()
        return
    }

    // Group Profile's "LINK" tab (Round 5) - see GroupLinkDeepLinkState's
    // own doc comment. Consumes (clears) the pending token so re-composition
    // doesn't re-navigate.
    LaunchedEffect(com.xhacker.cedal.ui.GroupLinkDeepLinkState.pendingToken) {
        val token = com.xhacker.cedal.ui.GroupLinkDeepLinkState.pendingToken ?: return@LaunchedEffect
        com.xhacker.cedal.ui.GroupLinkDeepLinkState.pendingToken = null
        navController.navigate("member_group_link_join/$token")
    }

    // Per-chat "Add Shortcut" - see OpenChatDeepLinkState's own doc comment.
    // Consumes (clears) the pending friend so re-composition doesn't
    // re-navigate.
    LaunchedEffect(com.xhacker.cedal.ui.OpenChatDeepLinkState.pendingFriendId) {
        val friendId = com.xhacker.cedal.ui.OpenChatDeepLinkState.pendingFriendId ?: return@LaunchedEffect
        val name = com.xhacker.cedal.ui.OpenChatDeepLinkState.pendingFriendName
        com.xhacker.cedal.ui.OpenChatDeepLinkState.pendingFriendId = null
        com.xhacker.cedal.ui.OpenChatDeepLinkState.pendingFriendName = null
        val encodedName = java.net.URLEncoder.encode(name ?: "Chat", "UTF-8")
        navController.navigate("member_chat/$friendId?name=$encodedName")
    }

    val onPasscodeSuccess: (String) -> Unit = { role ->
        val dest = when (role) {
            "owner" -> "owner_home"
            "developer" -> "developer_home"
            else -> "member_home"
        }
        navController.navigate(dest) { popUpTo(0) }
    }

    // "Lock on exit" (Settings > Security) — re-shows the passcode/biometric
    // gate as an overlay whenever the whole app comes back from the
    // background, without disturbing the nav stack underneath. Only arms
    // once you're actually past onboarding (a real session + passcode set),
    // so it never fires while still on signup/terms/create-passcode.
    DisposableEffect(Unit) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                val storage = lockViewModel.storage
                if (storage.appLockEnabled && storage.accessToken != null && storage.passcodeDone) {
                    AppLockState.isLocked = true
                }
            }
        }
        ProcessLifecycleOwner.get().lifecycle.addObserver(observer)
        onDispose { ProcessLifecycleOwner.get().lifecycle.removeObserver(observer) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(navController = navController, startDestination = "loading") {
        composable("loading") {
            val viewModel: AuthViewModel = hiltViewModel()
            LoadingScreen {
                val dest = when {
                    viewModel.storage.acceptedTermsVersion != TermsConfig.CURRENT_VERSION -> "terms_gate"
                    viewModel.storage.accessToken == null -> "signup"
                    !viewModel.storage.passcodeDone -> "create_passcode"
                    else -> "enter_passcode"
                }
                navController.navigate(dest) { popUpTo("loading") { inclusive = true } }
            }
        }
        composable("terms_gate") {
            // Re-enter "loading" once accepted so it recomputes the real
            // destination now that the terms check will pass.
            TermsGateScreen(onAccepted = { navController.navigate("loading") { popUpTo(0) } })
        }
        composable("signup") {
            SignUpScreen(
                onDone = { navController.navigate("create_passcode") { popUpTo("signup") { inclusive = true } } },
                onNavigateToSignIn = { navController.navigate("signin") },
            )
        }
        composable("signin") {
            SignInScreen(
                onDone = { navController.navigate("enter_passcode") { popUpTo("signin") { inclusive = true } } },
                onNavigateToSignUp = { navController.navigate("signup") },
                onNavigateToForgotPassword = { navController.navigate("forgot_password") },
            )
        }
        composable("forgot_password") {
            ForgotPasswordScreen(
                onDone = { navController.navigate("signin") { popUpTo("signin") { inclusive = true } } },
                onBack = { navController.popBackStack() },
            )
        }
        composable("create_passcode") {
            CreatePasscodeScreen(
                // A brand-new account is always role "user" (set at signup) and
                // just typed this exact passcode seconds ago — no need to make
                // them immediately re-enter it to confirm. Straight into the app.
                onDone = { navController.navigate("member_home") { popUpTo(0) } },
            )
        }
        composable(
            "enter_passcode?mode={mode}",
            arguments = listOf(navArgument("mode") { type = NavType.StringType; nullable = true; defaultValue = null }),
        ) { backStackEntry ->
            val mode = backStackEntry.arguments?.getString("mode") ?: "user"
            EnterPasscodeScreen(onSuccess = onPasscodeSuccess, initialMode = mode)
        }
        // tab/highlightId are optional (only ever set by PinnedMessagesScreen's
        // "jump to source" double-tap) - a bare navigate("member_home") still
        // matches this and lands on Chats as usual, same optional-query-param
        // pattern as member_settings?section= below.
        composable(
            "member_home?tab={tab}&highlightId={highlightId}",
            arguments = listOf(
                navArgument("tab") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("highlightId") { type = NavType.StringType; nullable = true; defaultValue = null },
            ),
        ) { backStackEntry ->
            val tab = backStackEntry.arguments?.getString("tab")?.let { runCatching { MemberTab.valueOf(it) }.getOrNull() }
            val highlightId = backStackEntry.arguments?.getString("highlightId")
            MemberHomeRoute(navController, initialTab = tab, highlightId = highlightId)
        }
        // These are standalone screens reached by navigating forward from
        // Chats (matching cedal-mobile, where Profile/Settings/etc. are their
        // own routes, not wrapped in the chat list's header) — no shared
        // avatar/search header here, just their own back button.
        composable("member_profile") {
            MemberProfileBody(
                onBack = { navController.popBackStack() },
                onEditNumber = { navController.navigate("member_security") },
            )
        }
        composable("member_search") {
            MemberSearchBody(
                onBack = { navController.popBackStack() },
                onOpenGroup = { groupId -> navController.navigate("member_group_chat/$groupId") },
            )
        }
        // Gmail/Instagram-style multi-account switcher - see
        // SecureStorage.savedAccounts/AuthViewModel.switchAccount. Switching
        // lands directly on member_home (skipping the passcode gate - the
        // device is already unlocked, this isn't a fresh external sign-in).
        // Adding a new account reuses the plain "signin" flow as-is.
        composable("member_switch_account") {
            MemberSwitchAccountBody(
                onBack = { navController.popBackStack() },
                onSwitched = { navController.navigate("member_home") { popUpTo(0) } },
                onAddAccount = { navController.navigate("signin") },
                onSwitchToDev = { navController.navigate("enter_passcode?mode=developer") { popUpTo(0) } },
            )
        }
        // Real 1-on-1 chat with an accepted friend - see ChatService
        // server-side. name is passed along purely so the thread's header
        // doesn't need its own extra profile fetch just to show a title.
        composable(
            "member_chat/{friendId}?name={name}&highlightId={highlightId}",
            arguments = listOf(
                navArgument("friendId") { type = NavType.StringType },
                navArgument("name") { type = NavType.StringType; defaultValue = "Chat" },
                navArgument("highlightId") { type = NavType.StringType; nullable = true; defaultValue = null },
            ),
        ) { backStackEntry ->
            val friendId = backStackEntry.arguments?.getString("friendId") ?: return@composable
            val name = backStackEntry.arguments?.getString("name")?.let { java.net.URLDecoder.decode(it, "UTF-8") } ?: "Chat"
            val highlightId = backStackEntry.arguments?.getString("highlightId")
            MemberChatThreadBody(
                friendId = friendId,
                friendName = name,
                onBack = { navController.popBackStack() },
                highlightId = highlightId,
                onOpenProfile = { uid -> navController.navigate("member_friend_profile/$uid") },
            )
        }
        // "Archived" row pinned above the chat list, and the biometric-
        // gated "Hidden" screen reachable from inside it - see
        // ArchivedChatsScreen.kt.
        composable("member_archived_chats") {
            ArchivedChatsBody(
                onBack = { navController.popBackStack() },
                onOpenChat = { friendId, name ->
                    navController.navigate("member_chat/$friendId?name=${java.net.URLEncoder.encode(name, "UTF-8")}")
                },
                onOpenHidden = { navController.navigate("member_hidden_chats") },
            )
        }
        composable("member_saved_messages") {
            SavedMessagesBody(onBack = { navController.popBackStack() })
        }
        composable("member_hidden_chats") {
            HiddenChatsBody(
                onBack = { navController.popBackStack() },
                onOpenChat = { friendId, name ->
                    navController.navigate("member_chat/$friendId?name=${java.net.URLEncoder.encode(name, "UTF-8")}")
                },
            )
        }
        // Corneal - the app-wide help assistant, one of the two fixed
        // entries at the top of Chats. See CornealChatService server-side.
        composable(
            "member_corneal_chat?highlightId={highlightId}",
            arguments = listOf(navArgument("highlightId") { type = NavType.StringType; nullable = true; defaultValue = null }),
        ) { backStackEntry ->
            val highlightId = backStackEntry.arguments?.getString("highlightId")
            CornealChatBody(onBack = { navController.popBackStack() }, highlightId = highlightId)
        }
        // Cedal System Feed - the other fixed entry at the top of Chats. See
        // SystemFeedService server-side for the admin-only-posting rule.
        composable("member_system_feed") {
            SystemFeedBody(onBack = { navController.popBackStack() })
        }
        // Read-only view of a friend's profile - see chat header's tap-name
        // flow (MemberFriendProfileBody, distinct from MemberProfileBody
        // which is always the signed-in user's own, editable profile).
        composable(
            "member_friend_profile/{userId}",
            arguments = listOf(navArgument("userId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: return@composable
            MemberFriendProfileBody(
                userId = userId,
                onBack = { navController.popBackStack() },
                onNavigate = { route -> navController.navigate(route) },
            )
        }
        composable("member_settings") {
            MemberSettingsBody(
                onBack = { navController.popBackStack() },
                onSignOut = { navController.navigate("signin") { popUpTo(0) } },
                onViewTerms = { navController.navigate("member_terms_view") },
                onSwitchAccount = { navController.navigate("member_switch_account") },
                onOpenSecurity = { navController.navigate("member_security") },
            )
        }
        composable("member_terms_view") {
            // Same screen shown on first launch — just returns to Settings
            // instead of resetting the whole nav stack on "accept".
            TermsGateScreen(onAccepted = { navController.popBackStack() })
        }
        composable("member_about") {
            MemberAboutBody(onBack = { navController.popBackStack() })
        }
        composable("member_security") {
            MemberSecurityBody(onBack = { navController.popBackStack() })
        }
        composable("member_admin_review") {
            AdminReviewBody(onBack = { navController.popBackStack() })
        }
        composable("member_app_updates") {
            AppUpdatePublishBody(onBack = { navController.popBackStack() })
        }
        composable("member_godmode") {
            GodmodeBody(onBack = { navController.popBackStack() })
        }
        composable("member_achievements") {
            AchievementsBody(onBack = { navController.popBackStack() })
        }
        composable("member_rules") {
            MemberRulesBody(onBack = { navController.popBackStack() })
        }
        composable("member_shop") {
            MemberShopBody(onBack = { navController.popBackStack() })
        }
        composable("member_history") {
            MemberHistoryBody(onBack = { navController.popBackStack() })
        }
        composable("member_bots") {
            MemberBotsListBody(
                onBack = { navController.popBackStack() },
                onOpenBot = { botId -> navController.navigate("member_bot_edit/$botId") },
                onCreateBot = { navController.navigate("member_bot_edit/new") },
            )
        }
        composable(
            "member_bot_edit/{botId}",
            arguments = listOf(navArgument("botId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val botId = backStackEntry.arguments?.getString("botId") ?: return@composable
            MemberBotEditBody(
                botId = if (botId == "new") null else botId,
                onBack = { navController.popBackStack() },
                onTestChat = { id -> navController.navigate("member_bot_test_chat/$id") },
            )
        }
        composable(
            "member_bot_test_chat/{botId}",
            arguments = listOf(navArgument("botId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val botId = backStackEntry.arguments?.getString("botId") ?: return@composable
            MemberBotTestChatBody(botId = botId, onBack = { navController.popBackStack() })
        }
        composable("member_create_group") {
            CreateGroupBody(
                onBack = { navController.popBackStack() },
                onCreated = { groupId, name ->
                    navController.navigate("member_group_chat/$groupId?name=${java.net.URLEncoder.encode(name, "UTF-8")}") {
                        popUpTo("member_create_group") { inclusive = true }
                    }
                },
            )
        }
        // Group chat thread - a parallel route to member_chat/{friendId}, see
        // GroupChatThreadScreen.kt for why it's a separate screen rather than
        // a friendId/groupId branch inside the 1-on-1 one.
        composable(
            "member_group_chat/{groupId}?name={name}",
            arguments = listOf(
                navArgument("groupId") { type = NavType.StringType },
                navArgument("name") { type = NavType.StringType; defaultValue = "Group" },
            ),
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId") ?: return@composable
            val name = backStackEntry.arguments?.getString("name")?.let { java.net.URLDecoder.decode(it, "UTF-8") } ?: "Group"
            GroupChatThreadBody(
                groupId = groupId,
                groupNameArg = name,
                onBack = { navController.popBackStack() },
                onOpenGroupProfile = { navController.navigate("member_group_profile/$groupId") },
            )
        }
        // Group info/settings/members - the group counterpart to
        // member_friend_profile, see GroupProfileScreen.kt.
        composable(
            "member_group_profile/{groupId}",
            arguments = listOf(navArgument("groupId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId") ?: return@composable
            GroupProfileBody(
                groupId = groupId,
                onBack = { navController.popBackStack() },
                onLeftGroup = {
                    navController.navigate("member_home") { popUpTo(0) }
                },
                onMessageUser = { userId, name ->
                    navController.navigate("member_chat/$userId?name=${java.net.URLEncoder.encode(name, "UTF-8")}")
                },
            )
        }
        // Group Profile's "LINK" tab - a scanned/opened invite link lands
        // here via GroupLinkDeepLinkState (see the LaunchedEffect above).
        composable(
            "member_group_link_join/{token}",
            arguments = listOf(navArgument("token") { type = NavType.StringType }),
        ) { backStackEntry ->
            val token = backStackEntry.arguments?.getString("token") ?: return@composable
            GroupLinkJoinBody(
                token = token,
                onBack = { navController.popBackStack() },
                onOpenGroup = { groupId -> navController.navigate("member_group_chat/$groupId") { popUpTo(0) } },
            )
        }
        composable("member_ai_requests") {
            AiRequestApprovalBody(onBack = { navController.popBackStack() })
        }
        // Cross-chat-type pinned messages - see MessageInteractionService.
        // onNavigate reuses whatever route string the pin's "jump to
        // source" needs (member_chat/..., member_corneal_chat?highlightId=,
        // or member_home?tab=...) rather than a fixed destination.
        composable("member_pinned") {
            PinnedMessagesBody(
                onBack = { navController.popBackStack() },
                onNavigate = { route -> navController.navigate(route) },
            )
        }
        // The 4 Work modes (Bank/Invest/Code/ARC) are each their own large
        // subsystem. Bank: Home/Send/Transactions/Debt/Trade/Notifications
        // are all real (a ~2600-line Star Coins wallet system; only Tasks —
        // a reward/reset system — stays out of scope for now).
        composable("member_work_bank") {
            BankRouterBody(
                onBack = { navController.popBackStack() },
                onOpenDebt = { navController.navigate("member_bank_debt") },
                onOpenTrade = { navController.navigate("member_bank_trade") },
                onOpenNotifications = { navController.navigate("member_bank_notifications") },
                onOpenSettings = { navController.navigate("member_settings") },
            )
        }
        composable("member_bank_debt") {
            BankDebtBody(onBack = { navController.popBackStack() })
        }
        composable("member_bank_trade") {
            BankTradeBody(onBack = { navController.popBackStack() })
        }
        composable("member_bank_notifications") {
            BankNotificationsBody(onBack = { navController.popBackStack() })
        }
        // Invest is real too (see conversation — upgraded from
        // cedal-mobile's 100%-hardcoded mock Overview/Watchlist/Trade/Asset
        // screens to real crypto prices via CoinGecko, simulated trades
        // against a virtual USD cash balance). Stocks aren't wired up yet.
        composable("member_work_invest") {
            InvestRouterBody(onBack = { navController.popBackStack() })
        }
        // Code and ARC used to be routes reached from the Work picker - now
        // top-level tabs instead (see MemberScaffold's MemberTab.CODE/ARC),
        // so there's no separate pushed route for either anymore.
        composable(
            "member_gui_session/{sessionId}?url={url}&cookies={cookies}",
            arguments = listOf(
                navArgument("sessionId") { type = NavType.StringType },
                navArgument("url") { type = NavType.StringType },
                navArgument("cookies") { type = NavType.StringType; defaultValue = "" },
            ),
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: return@composable
            val url = backStackEntry.arguments?.getString("url")?.let { java.net.URLDecoder.decode(it, "UTF-8") } ?: return@composable
            val cookies = backStackEntry.arguments?.getString("cookies")
                ?.let { java.net.URLDecoder.decode(it, "UTF-8") }
                ?.split("\n")
                ?.filter { it.isNotBlank() }
                ?: emptyList()
            GuiSessionBody(sessionId = sessionId, viewUrl = url, affinityCookies = cookies, onBack = { navController.popBackStack() })
        }
        composable("developer_home") {
            DeveloperHomeRoute(navController)
        }
        composable("developer_rules") {
            Column(modifier = Modifier.fillMaxSize()) {
                MemberBackBar(title = "AI (Rules)", onBack = { navController.popBackStack() })
                Box(modifier = Modifier.weight(1f)) {
                    MemberCodeBody(onBack = { navController.popBackStack() }, externalActiveTab = CodeTab.RULES, showOwnBottomBar = false)
                }
            }
        }
        composable("developer_manage_access") {
            ManageDeveloperAccessBody(onBack = { navController.popBackStack() })
        }
        composable("developer_alucard_chat") {
            val viewModel: AuthViewModel = hiltViewModel()
            AlucardChatBody(onBack = { navController.popBackStack() }, viewModel = viewModel)
        }
        composable("developer_submit") {
            DeveloperSubmitBody(onBack = { navController.popBackStack() })
        }
        composable("developer_submission_approvals") {
            DeveloperSubmissionApprovalBody(onBack = { navController.popBackStack() })
        }
        composable("owner_home") {
            OwnerHomeScreen(onLogout = { navController.navigate("signin") { popUpTo(0) } })
        }
        }

        // The app-wide Corneal bubble ("Chat Heads" style) - shown on every
        // member-area screen (route starts with "member_"), never during
        // auth/onboarding/loading. Rendered above the nav content but below
        // the passcode lock overlay, so a locked app can't be poked at
        // through it. See CornealBubbleState/CornealBubbleOverlay.
        val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
        if (currentRoute?.startsWith("member_") == true) {
            CornealBubbleOverlay(onOpenFullScreen = { navController.navigate("member_corneal_chat") })
            if (CornealBubbleState.chatOpen) {
                CornealFloatingWindow(onClose = { CornealBubbleState.chatOpen = false })
            }
        }

        // Developer Mode's Alucard bubble - same idea as Corneal's above,
        // shown on every developer-area screen (route starts with
        // "developer_"). Always opens full-screen on tap - see
        // AlucardBubbleState's doc comment for why there's no floating
        // mini-window branch here.
        if (currentRoute?.startsWith("developer_") == true) {
            AlucardBubbleOverlay(onOpenFullScreen = { navController.navigate("developer_alucard_chat") })
        }

        if (AppLockState.isLocked) {
            EnterPasscodeScreen(onSuccess = { AppLockState.isLocked = false })
        }

        // Live in-session ban/clear-data detection (see BalloonPopupOverlay's
        // poll) - covers whatever screen was showing, same overlay pattern
        // as AppLockState above.
        if (AccountGateState.active) {
            AccountGateScreen(onCancel = { navController.navigate("signup") { popUpTo(0) } })
        }

        UpdateBanner()
    }
}
