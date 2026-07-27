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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.RateReview
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material.icons.outlined.SwitchAccount
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.xhacker.cedal.ui.theme.CedalColors
import com.xhacker.cedal.ui.theme.CedalHeader
import com.xhacker.cedal.viewmodel.AuthViewModel
import kotlinx.coroutines.delay

// Phase C of the Developer Mode redesign - replaces the old vertical-menu
// DeveloperHomeScreen with a tab shell that visually mirrors MemberScaffold
// exactly (same swell-animated bottom bar - see TabItem below, copied
// verbatim from MemberScaffold's own). Every developerAccess account gets
// this (not just the owner) - it's the actual coding surface delegated
// developers use. Only the "⋮ More" menu's admin tools (AI Requests, Admin
// Review, Godmode, Manage Developer Access, Submission Approvals) are
// owner-only, same gate as everywhere else in this app (see isOwner below).
//
// Shipped incrementally (see the plan doc): CODE folds Pad+Command together
// via an in-tab toggle (codeSubTab below) rather than its own outer tab;
// EXPLORER/VIEW reuse MemberCodeBody's Documents/View bodies as-is; SECURITY
// promotes DeveloperSubmitBody directly. A "SEND TO ALUCARD" shortcut on the
// Code tab jumps straight to Security rather than pre-filling its form -
// pre-fill is real follow-up work, not something to rush.
enum class DeveloperTab { CODE, EXPLORER, VIEW, SECURITY }

@Composable
fun DeveloperHomeRoute(navController: NavHostController, viewModel: AuthViewModel = hiltViewModel()) {
    var activeTab by remember { mutableStateOf(DeveloperTab.CODE) }
    // Which of Pad/Command is showing while CODE is the active outer tab -
    // a toggle WITHIN Code, not its own bottom-bar tab (see CodeBottomBar's
    // now-superseded 5-tab layout in MemberCodeScreen.kt, whose Pad+Command
    // this folds together). Only ever PAD or COMMAND.
    var codeSubTab by remember { mutableStateOf(CodeTab.PAD) }
    var isOwner by remember { mutableStateOf(false) }
    var pendingAiRequests by remember { mutableStateOf(0) }
    var pendingSubmissions by remember { mutableStateOf(0) }
    var moreMenuOpen by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.getProfile().onSuccess { isOwner = it.email?.equals("hackerxenos06@gmail.com", ignoreCase = true) == true }
    }
    // Both endpoints are admin-only server-side - only polled for the
    // owner, so a delegated developer's session isn't spamming 403s every
    // 15s in the background.
    LaunchedEffect(isOwner) {
        while (isOwner) {
            viewModel.listPendingAiRequests().onSuccess { pendingAiRequests = it.size }
            viewModel.listPendingDeveloperSubmissions().onSuccess { pendingSubmissions = it.size }
            delay(15_000)
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(CedalColors.Background)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 16.dp)) {
            Box(modifier = Modifier.weight(1f)) {
                CedalHeader("CEDAL NODE", "DEVELOPER TERMINAL")
            }
            Box {
                Text(
                    "⋮",
                    color = CedalColors.TextPrimary,
                    fontSize = 20.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { moreMenuOpen = true }
                        .padding(8.dp),
                )
                DropdownMenu(
                    expanded = moreMenuOpen,
                    onDismissRequest = { moreMenuOpen = false },
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(CedalColors.CardBackground)
                        .border(1.dp, CedalColors.BorderCyan, RoundedCornerShape(16.dp)),
                ) {
                    val items = buildList {
                        add(Triple("AI (Rules)", "rules", Icons.Outlined.SmartToy))
                        if (isOwner) {
                            add(Triple(if (pendingAiRequests > 0) "AI Requests ($pendingAiRequests)" else "AI Requests", "ai_requests", Icons.Outlined.SmartToy))
                            add(Triple("Admin Review", "admin_review", Icons.Outlined.Gavel))
                            add(Triple("Godmode", "godmode", Icons.Outlined.Shield))
                            add(Triple("Manage Developer Access", "manage_access", Icons.Outlined.AdminPanelSettings))
                            add(Triple(if (pendingSubmissions > 0) "Submission Approvals ($pendingSubmissions)" else "Submission Approvals", "submission_approvals", Icons.Outlined.RateReview))
                        }
                        add(Triple("Switch To My Account", "switch_account", Icons.Outlined.SwitchAccount))
                        add(Triple("Log Out", "logout", Icons.Outlined.Logout))
                    }
                    items.forEach { (label, key, icon) ->
                        DropdownMenuItem(
                            text = { Text(label.uppercase(), color = CedalColors.TextPrimary, fontSize = 12.sp, letterSpacing = 1.sp) },
                            leadingIcon = { Icon(icon, contentDescription = null, tint = CedalColors.AccentCyan, modifier = Modifier.size(18.dp)) },
                            onClick = {
                                moreMenuOpen = false
                                when (key) {
                                    "rules" -> navController.navigate("developer_rules")
                                    "ai_requests" -> navController.navigate("member_ai_requests")
                                    "admin_review" -> navController.navigate("member_admin_review")
                                    "godmode" -> navController.navigate("member_godmode")
                                    "manage_access" -> navController.navigate("developer_manage_access")
                                    "submission_approvals" -> navController.navigate("developer_submission_approvals")
                                    "switch_account" -> navController.navigate("member_home") { popUpTo(0) }
                                    "logout" -> { viewModel.logout(); navController.navigate("signin") { popUpTo(0) } }
                                }
                            },
                        )
                    }
                }
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            // A single persistent MemberCodeBody instance covers CODE/
            // EXPLORER/VIEW (only its externalActiveTab argument changes)
            // so its hoisted state (file tree, entered file, undo/redo, run
            // output) survives switching between those three - exactly the
            // same reason that state is hoisted in the first place. It's
            // unmounted while SECURITY is active; that's fine, edits are
            // already autosaved to the on-device file (see MemberCodeBody's
            // own autosave) so nothing is lost, just some transient UI
            // state (scroll position, run output) resets on the way back.
            if (activeTab == DeveloperTab.SECURITY) {
                DeveloperSubmitBody(onBack = {}, showBackBar = false, viewModel = viewModel)
            } else {
                val codeTab = when (activeTab) {
                    DeveloperTab.CODE -> codeSubTab
                    DeveloperTab.EXPLORER -> CodeTab.DOCUMENTS
                    else -> CodeTab.VIEW
                }
                Column(modifier = Modifier.fillMaxSize()) {
                    if (activeTab == DeveloperTab.CODE) {
                        CodeSubTabRow(
                            active = codeSubTab,
                            onSelect = { codeSubTab = it },
                            onSendToAlucard = { activeTab = DeveloperTab.SECURITY },
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        MemberCodeBody(
                            onBack = {},
                            externalActiveTab = codeTab,
                            showOwnBottomBar = false,
                            viewModel = viewModel,
                            // MemberCodeBody's own internal navigation (e.g.
                            // Pad's "no file entered, go to Documents" link,
                            // or the auto-jump to View after a run) needs to
                            // move the OUTER tab too, not just flip a tab
                            // MemberCodeBody no longer visibly renders a bar
                            // for - otherwise the swell bar would look stuck
                            // on Code while Explorer/View content shows.
                            onActiveTabChange = { newTab ->
                                when (newTab) {
                                    CodeTab.PAD, CodeTab.COMMAND -> { codeSubTab = newTab; activeTab = DeveloperTab.CODE }
                                    CodeTab.DOCUMENTS -> activeTab = DeveloperTab.EXPLORER
                                    CodeTab.VIEW -> activeTab = DeveloperTab.VIEW
                                    CodeTab.RULES -> {}
                                }
                            },
                        )
                    }
                }
            }
        }

        DeveloperBottomBar(activeTab) { activeTab = it }
    }
}

// Sits above the Code tab's content (Pad or Command, whichever codeSubTab
// currently is) - toggles between the two, plus the "SEND TO ALUCARD"
// shortcut (see the plan's own "separate button from Run" note - this
// jumps to Security's submit form rather than pre-filling it).
@Composable
private fun CodeSubTabRow(active: CodeTab, onSelect: (CodeTab) -> Unit, onSendToAlucard: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(CedalColors.CardBackground)
            .border(width = Dp.Hairline, color = CedalColors.BorderSlate)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(
            "PAD",
            color = if (active == CodeTab.PAD) CedalColors.Success else CedalColors.TextMuted,
            fontSize = 11.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onSelect(CodeTab.PAD) }
                .padding(end = 16.dp),
        )
        Text(
            "COMMAND",
            color = if (active == CodeTab.COMMAND) CedalColors.Success else CedalColors.TextMuted,
            fontSize = 11.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier
                .weight(1f)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onSelect(CodeTab.COMMAND) },
        )
        Text(
            "SEND TO ALUCARD",
            color = CedalColors.AccentCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onSendToAlucard),
        )
    }
}

@Composable
private fun DeveloperBottomBar(active: DeveloperTab, onNavigateTab: (DeveloperTab) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .border(width = Dp.Hairline, color = CedalColors.BorderSlate),
    ) {
        DeveloperTabItem("Code", Icons.Outlined.Code, active == DeveloperTab.CODE) { onNavigateTab(DeveloperTab.CODE) }
        DeveloperTabItem("Explorer", Icons.Outlined.Folder, active == DeveloperTab.EXPLORER) { onNavigateTab(DeveloperTab.EXPLORER) }
        DeveloperTabItem("View", Icons.Outlined.Visibility, active == DeveloperTab.VIEW) { onNavigateTab(DeveloperTab.VIEW) }
        DeveloperTabItem("Security", Icons.Outlined.Security, active == DeveloperTab.SECURITY) { onNavigateTab(DeveloperTab.SECURITY) }
    }
}

// Copied verbatim from MemberScaffold's private TabItem - same "swell" pop
// animation, so the outer bar is pixel-for-pixel consistent with the
// normal member app's own bottom bar, per the plan's explicit intent.
@Composable
private fun DeveloperTabItem(label: String, icon: ImageVector, active: Boolean, onClick: () -> Unit) {
    val color = if (active) CedalColors.Background else CedalColors.TextMuted
    val bubbleScale by animateFloatAsState(
        targetValue = if (active) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "devTabBubbleScale",
    )
    val iconSize by animateDpAsState(
        targetValue = if (active) 22.dp else 18.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "devTabIconSize",
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick),
    ) {
        Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
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
