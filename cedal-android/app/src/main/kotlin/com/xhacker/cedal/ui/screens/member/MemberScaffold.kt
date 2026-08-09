package com.xhacker.cedal.ui.screens.member

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.GroupAdd
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material.icons.outlined.SwitchAccount
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.xhacker.cedal.ui.screens.PermissionBlockedDialog
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.xhacker.cedal.ui.FriendRequestSession
import com.xhacker.cedal.ui.theme.CedalColors
import com.xhacker.cedal.viewmodel.AuthViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class MemberTab { CHATS, CALL, CODE, ARC }

// The 4 bottom tabs (Chats/Call/Code/Arc) are handled as *local UI state*
// inside a single persistent screen, not as separate nav destinations. That
// was the bug: navigating between them as full routes both grew the back
// stack (so "going back" eventually exited the app) and destroyed/recreated
// the bottom bar each switch, which meant the bubble animation had no
// previous state to animate from — it just appeared already-active.
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun MemberHomeRoute(
    navController: NavHostController,
    initialTab: MemberTab? = null,
    highlightId: String? = null,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    var activeTab by remember { mutableStateOf(initialTab ?: MemberTab.CHATS) }
    var searchQuery by remember { mutableStateOf("") }
    val chatSelection = rememberChatSelectionState()
    MemberRoute(
        navController = navController,
        activeTab = activeTab,
        onNavigateTab = { activeTab = it },
        searchQuery = searchQuery,
        onSearchQueryChange = { searchQuery = it },
        viewModel = viewModel,
        chatSelection = chatSelection,
    ) {
        when (activeTab) {
            MemberTab.CHATS -> ChatsListBody(
                searchQuery = searchQuery,
                viewModel = viewModel,
                onOpenChat = { friendId, name ->
                    navController.navigate("member_chat/$friendId?name=${java.net.URLEncoder.encode(name, "UTF-8")}")
                },
                onOpenSystemFeed = { navController.navigate("member_system_feed") },
                onOpenGroup = { groupId, name ->
                    navController.navigate("member_group_chat/$groupId?name=${java.net.URLEncoder.encode(name, "UTF-8")}")
                },
                selection = chatSelection,
            )
            MemberTab.CALL -> CallListBody(onOpenProfile = { uid -> navController.navigate("member_friend_profile/$uid") })
            // onBack here can't pop a route - this is inline tab content,
            // not a pushed screen (see the comment above on why tabs are
            // local state) - so "back" just returns to Chats instead.
            MemberTab.CODE -> MemberCodeBody(
                onBack = { activeTab = MemberTab.CHATS },
                highlightId = if (initialTab == MemberTab.CODE) highlightId else null,
                onOpenGuiSession = { sessionId, url, cookies ->
                    val encodedUrl = java.net.URLEncoder.encode(url, "UTF-8")
                    val encodedCookies = java.net.URLEncoder.encode(cookies.joinToString("\n"), "UTF-8")
                    navController.navigate("member_gui_session/$sessionId?url=$encodedUrl&cookies=$encodedCookies")
                },
            )
            MemberTab.ARC -> ArcRouterBody(onBack = { activeTab = MemberTab.CHATS }, highlightId = if (initialTab == MemberTab.ARC) highlightId else null)
        }
    }
}

// Shared wiring so every member-area screen (tabs + menu destinations) gets
// the same header/bottom-bar behavior without repeating navigation glue.
// For non-tab destinations (profile, settings, ...) tapping a bottom tab
// resets the stack back to the tabbed home screen rather than local state.
@Composable
fun MemberRoute(
    navController: NavHostController,
    activeTab: MemberTab?,
    onNavigateTab: ((MemberTab) -> Unit)? = null,
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    viewModel: AuthViewModel = hiltViewModel(),
    chatSelection: ChatSelectionState? = null,
    content: @Composable () -> Unit,
) {
    var initial by remember { mutableStateOf("U") }
    LaunchedEffect(Unit) {
        viewModel.getProfile().onSuccess { profile ->
            initial = (profile.nickname?.firstOrNull() ?: profile.email?.firstOrNull() ?: 'U').uppercaseChar().toString()
        }
    }
    val context = LocalContext.current
    LaunchedEffect(viewModel.storage.userId) {
        FriendRequestSession.activate(context, viewModel.storage.userId) { viewModel.listFriendRequests() }
        com.xhacker.cedal.ui.MessageNotificationSession.activate(context, viewModel.storage.userId) { viewModel.listConversations() }
        com.xhacker.cedal.ui.AiRequestNotificationSession.activate(context, viewModel.storage.userId) { viewModel.getAiRequestHistory() }
    }
    // Neither FriendRequestSession nor MessageNotificationSession ever
    // actually REQUESTS POST_NOTIFICATIONS (Android 13+) - they only check
    // it, same as every notify*() call's own hasNotificationPermission
    // guard. The only place that ever launched the real system prompt was
    // MemberCodeScreen's build-finished notification, gated behind
    // actually starting a Kotlin build - so for anyone who never did that,
    // every notification in the app (friend requests included) has been
    // silently no-op'ing this whole time. Requested here once, as soon as
    // the member area loads, so it's actually asked for at all.
    val notificationPermissionGate = com.xhacker.cedal.ui.screens.rememberPermissionGate()
    LaunchedEffect(Unit) {
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            notificationPermissionGate.request(context, android.Manifest.permission.POST_NOTIFICATIONS) {}
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        MemberScaffold(
            activeTab = activeTab,
            avatarInitial = initial,
            onNavigateTab = onNavigateTab ?: { navController.navigate("member_home") { popUpTo("member_home") { inclusive = true } } },
            onOpenProfile = { navController.navigate("member_profile") },
            searchQuery = searchQuery,
            onSearchQueryChange = onSearchQueryChange,
            onFindFriends = { navController.navigate("member_search") },
            onMenuItem = { item -> navController.navigate("member_$item") },
            chatSelection = chatSelection,
            viewModel = viewModel,
            content = content,
        )
        // Global achievement/rank-up popups - lives here (not per-screen) so
        // it surfaces no matter which tab/destination the user's on.
        BalloonPopupOverlay(viewModel = viewModel)
    }
    notificationPermissionGate.PermissionBlockedDialog()
}

@Composable
fun MemberScaffold(
    activeTab: MemberTab?,
    avatarInitial: String,
    onNavigateTab: (MemberTab) -> Unit,
    onOpenProfile: () -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onFindFriends: () -> Unit,
    onMenuItem: (String) -> Unit,
    chatSelection: ChatSelectionState? = null,
    viewModel: AuthViewModel = hiltViewModel(),
    content: @Composable () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().background(CedalColors.Background)) {
        // The whole header (including the profile avatar) is chat-specific —
        // matching cedal-mobile, where only home.tsx has ChatListHeader at
        // all. Base/Code/Arc show nothing here, not even the avatar.
        if (activeTab == MemberTab.CHATS) {
            if (chatSelection?.active == true) {
                ChatSelectionHeader(chatSelection, viewModel)
            } else {
                MemberHeader(avatarInitial, onOpenProfile, searchQuery, onSearchQueryChange, onFindFriends, onMenuItem)
            }
        }
        Box(modifier = Modifier.weight(1f)) { content() }
        // Code and Arc each have their own full internal navigation
        // (CodeBottomBar / ArcBottomBar for their own sub-tabs, plus a back
        // arrow in every sub-screen that already returns straight to Chats -
        // see MemberCodeBody.onBack/ArcRouterBody.onBack below) - showing
        // this outer bar too would stack two bottom tab rows on screen at
        // once. Non-tab destinations (Profile, Settings, ...) pass
        // activeTab=null - nothing to switch to from here either.
        if (activeTab != null && activeTab != MemberTab.CODE && activeTab != MemberTab.ARC) {
            MemberBottomBar(activeTab, onNavigateTab)
        }
    }
}

// Replaces the whole normal header row (avatar/search/+/⋮) the moment a chat
// is long-pressed - "Select All", a Group/Chat/Custom-Time filter icon, and
// a ⋮ menu whose contents change to the bulk-action list (Delete/Archive/
// Clear Data/Pin/Mute/Favorite/Lock/Hide). See ChatSelectionState and
// ChatService.bulkAction for what backs this.
@Composable
private fun ChatSelectionHeader(selection: ChatSelectionState, viewModel: AuthViewModel) {
    var filterMenuOpen by remember { mutableStateOf(false) }
    var actionMenuOpen by remember { mutableStateOf(false) }
    var customDaysPrompt by remember { mutableStateOf(false) }
    var customDaysText by remember { mutableStateOf("2") }
    // Lock/unlock both require biometric/passcode verification either way -
    // holds which action ("lock" or "unlock") is waiting on that check.
    var pendingLockAction by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val allSelected = selection.selectedIds.isNotEmpty() && selection.selectedIds.size == selection.allSelectable.size
    val selectedConvos = selection.allSelectable.filter { it.friendId in selection.selectedIds }
    // Smart toggle: a selection that's already ALL pinned/locked shows the
    // "un-" action; a MIXED selection also shows "un-" (so pressing it
    // normalizes everything to unpinned/unlocked first) - only a selection
    // with NOTHING pinned/locked yet shows the plain "Pin"/"Lock" action.
    val anyPinned = selectedConvos.any { it.pinned }
    val anyLocked = selectedConvos.any { it.locked }

    fun runBulk(action: String) {
        actionMenuOpen = false
        val ids = selection.selectedIds.toList()
        scope.launch {
            viewModel.bulkChatAction(ids, action)
            selection.clear()
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .border(width = Dp.Hairline, color = CedalColors.BorderSlate)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        IconButton(onClick = { selection.clear() }) {
            Text("✕", color = CedalColors.TextPrimary, fontSize = 16.sp)
        }

        Text(
            if (allSelected) "Deselect All" else "Select All",
            color = CedalColors.AccentCyan, fontSize = 13.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier
                .padding(start = 4.dp)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                    if (allSelected) selection.selectedIds = emptySet() else selection.selectAll()
                },
        )
        Text(
            "  ${selection.selectedIds.size} selected",
            color = CedalColors.TextSecondary, fontSize = 13.sp,
            modifier = Modifier.weight(1f),
        )

        Box {
            IconButton(onClick = { filterMenuOpen = true }) {
                Icon(Icons.Outlined.FilterList, contentDescription = "Filter", tint = CedalColors.TextPrimary, modifier = Modifier.size(20.dp))
            }
            DropdownMenu(
                expanded = filterMenuOpen,
                onDismissRequest = { filterMenuOpen = false },
                modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(CedalColors.CardBackground).border(1.dp, CedalColors.BorderCyan, RoundedCornerShape(16.dp)),
            ) {
                DropdownMenuItem(text = { Text("GROUP", color = CedalColors.TextPrimary, fontSize = 12.sp, letterSpacing = 1.sp) }, onClick = { filterMenuOpen = false; selection.selectGroups() })
                DropdownMenuItem(text = { Text("CHAT", color = CedalColors.TextPrimary, fontSize = 12.sp, letterSpacing = 1.sp) }, onClick = { filterMenuOpen = false; selection.selectChats() })
                DropdownMenuItem(text = { Text("CUSTOM TIME", color = CedalColors.TextPrimary, fontSize = 12.sp, letterSpacing = 1.sp) }, onClick = { filterMenuOpen = false; customDaysPrompt = true })
            }
        }

        Box {
            IconButton(onClick = { actionMenuOpen = true }) {
                Text("⋮", color = CedalColors.TextPrimary, fontSize = 18.sp)
            }
            DropdownMenu(
                expanded = actionMenuOpen,
                onDismissRequest = { actionMenuOpen = false },
                modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(CedalColors.CardBackground).border(1.dp, CedalColors.BorderCyan, RoundedCornerShape(16.dp)),
            ) {
                listOf(
                    "Delete" to "delete", "Archive" to "archive", "Clear Data" to "clear",
                    (if (anyPinned) "Unpin" else "Pin") to (if (anyPinned) "unpin" else "pin"),
                    "Mute" to "mute", "Favorite" to "favorite",
                    "Hide" to "hide",
                ).forEach { (label, action) ->
                    DropdownMenuItem(
                        text = { Text(label.uppercase(), color = if (action == "delete") CedalColors.Error else CedalColors.TextPrimary, fontSize = 12.sp, letterSpacing = 1.sp) },
                        onClick = { runBulk(action) },
                    )
                }
                // Both directions need biometric/passcode - see
                // pendingLockAction above and the AccountVerifyOverlay below.
                DropdownMenuItem(
                    text = { Text((if (anyLocked) "Unlock" else "Lock").uppercase(), color = CedalColors.TextPrimary, fontSize = 12.sp, letterSpacing = 1.sp) },
                    onClick = { actionMenuOpen = false; pendingLockAction = if (anyLocked) "unlock" else "lock" },
                )
            }
        }
    }

    // Locking OR unlocking selected chats both need biometric/passcode -
    // same AccountVerifyOverlay as Switch Account and opening a locked chat.
    // Wrapped in a real Dialog for the same reason as the custom-days
    // prompt below - this composable is only ever a Column child here, not
    // a NavHost-level destination, so AccountVerifyOverlay's own internal
    // fillMaxSize() Box wouldn't actually overlay the full screen otherwise.
    val lockAction = pendingLockAction
    if (lockAction != null) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { pendingLockAction = null },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        ) {
            AccountVerifyOverlay(
                viewModel = viewModel,
                message = if (lockAction == "lock") "Locking chats needs your biometrics or passcode." else "Unlocking chats needs your biometrics or passcode.",
                onVerified = { pendingLockAction = null; runBulk(lockAction) },
                onCancel = { pendingLockAction = null },
            )
        }
    }

    // Custom Time filter - "every chat with no activity in the last N days".
    // Uses a real Dialog (its own window) rather than a fillMaxSize() Box,
    // since this composable is only ever placed as the header row inside
    // MemberScaffold's Column - a Box here would be laid out as just another
    // Column child, not a full-screen overlay, unlike the NavHost-level
    // overlays elsewhere in this app.
    if (customDaysPrompt) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { customDaysPrompt = false }) {
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(CedalColors.CardBackground)
                    .border(1.dp, CedalColors.BorderCyan, RoundedCornerShape(16.dp))
                    .padding(20.dp),
            ) {
                Text("Select chats inactive for...", color = CedalColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Box(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, CedalColors.BorderSlate, RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                ) {
                    BasicTextField(
                        value = customDaysText,
                        onValueChange = { if (it.length <= 4 && it.all(Char::isDigit)) customDaysText = it },
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(color = CedalColors.TextPrimary, fontSize = 14.sp),
                        cursorBrush = SolidColor(CedalColors.AccentCyan),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    )
                }
                Text("days", color = CedalColors.TextMuted, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))

                Row(modifier = Modifier.padding(top = 18.dp), horizontalArrangement = Arrangement.End) {
                    Text(
                        "CANCEL", color = CedalColors.TextSecondary, fontSize = 12.sp,
                        modifier = Modifier
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { customDaysPrompt = false }
                            .padding(10.dp),
                    )
                    Text(
                        "SELECT", color = CedalColors.AccentCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                selection.selectStaleSince(customDaysText.toIntOrNull() ?: 2)
                                customDaysPrompt = false
                            }
                            .padding(10.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun MemberHeader(
    avatarInitial: String,
    onOpenProfile: () -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onFindFriends: () -> Unit,
    onMenuItem: (String) -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    var menuOpen by remember { mutableStateOf(false) }
    // "More" reuses the exact same panel styling, just with a different
    // item set (Bots/Rules/About) - see the items list below. Godmode/Admin
    // Review/AI Requests moved to Developer mode (see DeveloperHomeScreen) -
    // normal accounts, admin or not, no longer see them here.
    var moreMenuOpen by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .border(width = Dp.Hairline, color = CedalColors.BorderSlate)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(CedalColors.Background)
                .border(1.dp, CedalColors.BorderCyan, CircleShape)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onOpenProfile),
            contentAlignment = Alignment.Center,
        ) {
            Text(avatarInitial, color = CedalColors.TextPrimary, fontSize = 16.sp)
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp)
                .clip(RoundedCornerShape(50))
                .border(1.dp, CedalColors.BorderSlate, RoundedCornerShape(50))
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            // Live filter over your existing chats (e.g. typing "cedal
            // system feed" surfaces the Cedal System Feed entry) — this is
            // NOT navigation, unlike the ✚ button below.
            BasicTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(color = CedalColors.TextPrimary, fontSize = 13.sp),
                cursorBrush = SolidColor(CedalColors.AccentCyan),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    if (searchQuery.isEmpty()) {
                        Text("Search chats", color = CedalColors.TextSecondary, fontSize = 13.sp)
                    }
                    inner()
                },
            )
        }

        // Finding other people (friends) is a separate flow from filtering
        // your own chat list — it navigates away, the search bar doesn't.
        // Red while a request is waiting on YOU (needs action - outranks
        // green), green while one of YOUR sent requests just got accepted
        // (good news, no action needed) - both reflect only the currently
        // active account (see FriendRequestSession), never a different
        // saved account you're not even signed into right now.
        IconButton(onClick = onFindFriends) {
            Text(
                "✚",
                color = when (FriendRequestSession.indicator) {
                    FriendRequestSession.Indicator.PENDING -> CedalColors.Error
                    FriendRequestSession.Indicator.ACCEPTED -> CedalColors.Success
                    FriendRequestSession.Indicator.NONE -> CedalColors.TextPrimary
                },
                fontSize = 16.sp,
            )
        }

        Box {
            IconButton(onClick = { menuOpen = true }) {
                Text("⋮", color = CedalColors.TextPrimary, fontSize = 18.sp)
            }
            DropdownMenu(
                expanded = menuOpen,
                onDismissRequest = { menuOpen = false },
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(CedalColors.CardBackground)
                    .border(1.dp, CedalColors.BorderCyan, RoundedCornerShape(16.dp)),
            ) {
                val items = buildList {
                    add(Triple("Create Group", "create_group", Icons.Outlined.GroupAdd))
                    add(Triple("Pinned Messages", "pinned", Icons.Outlined.PushPin))
                    add(Triple("Settings", "settings", Icons.Outlined.Settings))
                    add(Triple("Archive", "archived_chats", Icons.Outlined.Archive))
                    add(Triple("Saved Messages", "saved_messages", Icons.Outlined.Bookmark))
                    add(Triple("History", "history", Icons.Outlined.History))
                    add(Triple("Switch Account", "switch_account", Icons.Outlined.SwitchAccount))
                    // "More" doesn't navigate itself - it opens the same
                    // panel again with Arsenal/Bots/Rules/About/Security
                    // inside (see moreMenuOpen below).
                    add(Triple("More", "more", Icons.Outlined.MoreHoriz))
                }
                items.forEach { (label, key, icon) ->
                    DropdownMenuItem(
                        text = { Text(label.uppercase(), color = CedalColors.TextPrimary, fontSize = 12.sp, letterSpacing = 1.sp) },
                        leadingIcon = { Icon(icon, contentDescription = null, tint = CedalColors.AccentCyan, modifier = Modifier.size(18.dp)) },
                        onClick = {
                            menuOpen = false
                            if (key == "more") moreMenuOpen = true else onMenuItem(key)
                        },
                    )
                }
            }
            // Same panel styling as the main ⋮ menu, opened from "More" -
            // just Bots/Rules/About instead of the usual item set.
            DropdownMenu(
                expanded = moreMenuOpen,
                onDismissRequest = { moreMenuOpen = false },
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(CedalColors.CardBackground)
                    .border(1.dp, CedalColors.BorderCyan, RoundedCornerShape(16.dp)),
            ) {
                val moreItems = buildList {
                    // Arsenal hidden from the More menu (user request) -
                    // route "shop"/MemberShopScreen.kt is untouched, still
                    // reachable if this line is restored.
                    add(Triple("Bots", "bots", Icons.Outlined.SmartToy))
                    add(Triple("Rules", "rules", Icons.Outlined.Gavel))
                    add(Triple("About", "about", Icons.Outlined.Info))
                    add(Triple("Security", "security", Icons.Outlined.Security))
                    add(Triple("Achievements", "achievements", Icons.Outlined.EmojiEvents))
                }
                moreItems.forEach { (label, key, icon) ->
                    DropdownMenuItem(
                        text = { Text(label.uppercase(), color = CedalColors.TextPrimary, fontSize = 12.sp, letterSpacing = 1.sp) },
                        leadingIcon = { Icon(icon, contentDescription = null, tint = CedalColors.AccentCyan, modifier = Modifier.size(18.dp)) },
                        onClick = { moreMenuOpen = false; onMenuItem(key) },
                    )
                }
            }
        }
    }
}

@Composable
private fun MemberBottomBar(active: MemberTab?, onNavigateTab: (MemberTab) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .border(width = Dp.Hairline, color = CedalColors.BorderSlate),
    ) {
        TabItem("Chats", Icons.Outlined.ChatBubbleOutline, active == MemberTab.CHATS) { onNavigateTab(MemberTab.CHATS) }
        // Replaces the old empty "Base" placeholder tab entirely - see
        // CallListScreen.kt.
        TabItem("Call", Icons.Outlined.Call, active == MemberTab.CALL) { onNavigateTab(MemberTab.CALL) }
        TabItem("Code", Icons.Outlined.Code, active == MemberTab.CODE) { onNavigateTab(MemberTab.CODE) }
        TabItem("Arc", Icons.Outlined.Security, active == MemberTab.ARC) { onNavigateTab(MemberTab.ARC) }
    }
}

@Composable
private fun TabItem(label: String, icon: ImageVector, active: Boolean, onClick: () -> Unit) {
    val color = if (active) CedalColors.Background else CedalColors.TextMuted
    // "Swell" pop: bouncy overshoot on scale (snaps past 1x then settles),
    // a solid (not faint) pill background, and the icon itself grows +
    // recolors — much more visible than a faint tint fading in.
    val bubbleScale by animateFloatAsState(
        targetValue = if (active) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "tabBubbleScale",
    )
    val iconSize by animateDpAsState(
        targetValue = if (active) 22.dp else 18.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "tabIconSize",
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick),
    ) {
        Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
            // Bubble and icon are stacked (not nested) so the icon stays put
            // and only the pill behind it scales in on activation.
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .graphicsLayer { scaleX = bubbleScale; scaleY = bubbleScale }
                    .clip(RoundedCornerShape(50))
                    .background(CedalColors.Success),
            )
            Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(iconSize))
        }
        Text(
            label,
            color = if (active) CedalColors.Success else CedalColors.TextMuted,
            fontSize = 11.sp,
            modifier = Modifier.padding(top = 3.dp),
        )
    }
}
