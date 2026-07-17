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
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.GroupAdd
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.xhacker.cedal.ui.FriendRequestSession
import com.xhacker.cedal.ui.theme.CedalColors
import com.xhacker.cedal.viewmodel.AuthViewModel
import kotlinx.coroutines.delay

enum class MemberTab { CHATS, BASE, CODE, ARC }

// The 4 bottom tabs (Chats/Base/Code/Arc) are handled as *local UI state*
// inside a single persistent screen, not as separate nav destinations. That
// was the bug: navigating between them as full routes both grew the back
// stack (so "going back" eventually exited the app) and destroyed/recreated
// the bottom bar each switch, which meant the bubble animation had no
// previous state to animate from — it just appeared already-active.
@Composable
fun MemberHomeRoute(
    navController: NavHostController,
    initialTab: MemberTab? = null,
    highlightId: String? = null,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    var activeTab by remember { mutableStateOf(initialTab ?: MemberTab.CHATS) }
    var searchQuery by remember { mutableStateOf("") }
    MemberRoute(
        navController = navController,
        activeTab = activeTab,
        onNavigateTab = { activeTab = it },
        searchQuery = searchQuery,
        onSearchQueryChange = { searchQuery = it },
        viewModel = viewModel,
    ) {
        when (activeTab) {
            MemberTab.CHATS -> ChatsListBody(
                searchQuery = searchQuery,
                viewModel = viewModel,
                onOpenChat = { friendId, name ->
                    navController.navigate("member_chat/$friendId?name=${java.net.URLEncoder.encode(name, "UTF-8")}")
                },
                onOpenSystemFeed = { navController.navigate("member_system_feed") },
            )
            MemberTab.BASE -> WelcomeToBaseBody()
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

// Base tab content - intentionally empty besides a placeholder. Bank/Invest
// used to live here behind a picker; this tab no longer routes to them.
@Composable
private fun WelcomeToBaseBody() {
    Box(modifier = Modifier.fillMaxSize().background(CedalColors.Background), contentAlignment = Alignment.Center) {
        Text("Welcome to Base", color = CedalColors.TextPrimary, fontSize = 18.sp)
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
    }

    MemberScaffold(
        activeTab = activeTab,
        avatarInitial = initial,
        onNavigateTab = onNavigateTab ?: { navController.navigate("member_home") { popUpTo("member_home") { inclusive = true } } },
        onOpenProfile = { navController.navigate("member_profile") },
        searchQuery = searchQuery,
        onSearchQueryChange = onSearchQueryChange,
        onFindFriends = { navController.navigate("member_search") },
        onMenuItem = { item -> navController.navigate("member_$item") },
        content = content,
    )
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
    content: @Composable () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().background(CedalColors.Background)) {
        // The whole header (including the profile avatar) is chat-specific —
        // matching cedal-mobile, where only home.tsx has ChatListHeader at
        // all. Base/Code/Arc show nothing here, not even the avatar.
        if (activeTab == MemberTab.CHATS) {
            MemberHeader(avatarInitial, onOpenProfile, searchQuery, onSearchQueryChange, onFindFriends, onMenuItem)
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
    // Client-side check only decides whether to SHOW the menu entry - every
    // route behind it (see AiChangeRequestRoutes.requireAdmin) enforces the
    // same admin check server-side regardless, same principle as System
    // Feed's composer. No push notifications yet (this app has no FCM
    // integration anywhere), so a red-dot badge on a poll is the v1 "you
    // have something to review" signal instead.
    var isAdmin by remember { mutableStateOf(false) }
    var pendingAiRequests by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        viewModel.getProfile().onSuccess { isAdmin = it.email?.equals("hackerxenos06@gmail.com", ignoreCase = true) == true }
    }
    LaunchedEffect(isAdmin) {
        if (!isAdmin) return@LaunchedEffect
        while (true) {
            viewModel.listPendingAiRequests().onSuccess { pendingAiRequests = it.size }
            delay(15_000)
        }
    }

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
                    if (isAdmin) {
                        val label = if (pendingAiRequests > 0) "AI Requests ($pendingAiRequests)" else "AI Requests"
                        add(Triple(label, "ai_requests", Icons.Outlined.SmartToy))
                    }
                    add(Triple("Create Group", "create_group", Icons.Outlined.GroupAdd))
                    add(Triple("Pinned Messages", "pinned", Icons.Outlined.PushPin))
                    add(Triple("Settings", "settings", Icons.Outlined.Settings))
                    add(Triple("About", "about", Icons.Outlined.Info))
                    add(Triple("Rules", "rules", Icons.Outlined.Gavel))
                    add(Triple("Bots", "bots", Icons.Outlined.SmartToy))
                    add(Triple("History", "history", Icons.Outlined.History))
                    add(Triple("Arsenal", "shop", Icons.Outlined.Shield))
                }
                items.forEach { (label, key, icon) ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                label.uppercase(),
                                color = if (key == "ai_requests" && pendingAiRequests > 0) CedalColors.Error else CedalColors.TextPrimary,
                                fontSize = 12.sp, letterSpacing = 1.sp,
                            )
                        },
                        leadingIcon = {
                            Icon(
                                icon, contentDescription = null,
                                tint = if (key == "ai_requests" && pendingAiRequests > 0) CedalColors.Error else CedalColors.AccentCyan,
                                modifier = Modifier.size(18.dp),
                            )
                        },
                        onClick = { menuOpen = false; onMenuItem(key) },
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
        TabItem("Base", Icons.Outlined.Work, active == MemberTab.BASE) { onNavigateTab(MemberTab.BASE) }
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
