package com.xhacker.cedal.ui.screens.member

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.xhacker.cedal.data.FriendSummary
import com.xhacker.cedal.data.GroupDto
import com.xhacker.cedal.data.GroupJoinRequestDto
import com.xhacker.cedal.data.GroupMemberDto
import com.xhacker.cedal.data.MediaSummaryDto
import com.xhacker.cedal.ui.theme.CedalColors
import com.xhacker.cedal.ui.theme.CedalErrorText
import com.xhacker.cedal.viewmodel.AuthViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Group info/settings/members - the group counterpart to
// MemberFriendProfileScreen.kt, reached from GroupChatThreadScreen.kt's
// header ⋮ menu ("Group Info", visible to everyone). Role/permission
// gating mirrors GroupChatThreadScreen.kt (canKickRole/groupRoleLabel are
// shared from there - internal, not duplicated, since permission-matrix
// logic drifting between two copies is a real risk); the dialog styling
// here is its own small set, patterned on MemberFriendProfileScreen.kt /
// GroupChatThreadScreen.kt's overlays rather than reused from either.
@Composable
fun GroupProfileBody(
    groupId: String,
    onBack: () -> Unit,
    onLeftGroup: () -> Unit,
    onMessageUser: (userId: String, name: String) -> Unit = { _, _ -> },
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val scope = rememberCoroutineScope()
    val myUserId = viewModel.storage.userId
    var group by remember { mutableStateOf<GroupDto?>(null) }
    var friends by remember { mutableStateOf<List<FriendSummary>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var editNameOpen by remember { mutableStateOf(false) }
    var editDescriptionOpen by remember { mutableStateOf(false) }
    var editRulesOpen by remember { mutableStateOf(false) }
    var actionSheetFor by remember { mutableStateOf<GroupMemberDto?>(null) }
    var leaveConfirmOpen by remember { mutableStateOf(false) }
    var addMemberSheetOpen by remember { mutableStateOf(false) }
    var mediaSummary by remember { mutableStateOf<MediaSummaryDto?>(null) }
    var joinRequests by remember { mutableStateOf<List<GroupJoinRequestDto>>(emptyList()) }
    var reportOpen by remember { mutableStateOf(false) }
    var clearChatConfirmOpen by remember { mutableStateOf(false) }
    var clearMediaConfirmOpen by remember { mutableStateOf(false) }
    var autoDeleteOverlayOpen by remember { mutableStateOf(false) }
    var profileTab by remember { mutableStateOf("OVERVIEW") } // "OVERVIEW" | "SECURITY"
    var overflowMenuOpen by remember { mutableStateOf(false) }
    var mediaScreenOpen by remember { mutableStateOf(false) }
    var groupCallPickerOpen by remember { mutableStateOf(false) }
    val context = LocalContext.current

    fun refresh() {
        scope.launch { viewModel.getGroup(groupId).onSuccess { group = it }.onFailure { error = it.message } }
    }

    LaunchedEffect(groupId) {
        refresh()
        viewModel.listFriends().onSuccess { friends = it }
    }

    val myRole = group?.members?.firstOrNull { it.userId == myUserId }?.role
    val isAdminTier = isAdminTierRole(myRole)
    val isCreator = myRole == "CREATOR"
    // Public groups' link is visible to every member (also freely
    // searchable/joinable, no approval needed); a private group's link is
    // admin-tier only - it's a manual-invite channel, still gated behind an
    // admin-tier approval on the other end (see GroupLinkJoinBody).
    val canSeeLink = group?.isPublic == true || isAdminTier
    val nameFor = { id: String -> if (id == myUserId) "You" else friends.firstOrNull { it.id == id }?.name ?: id.take(8) }

    LaunchedEffect(isAdminTier) {
        if (isAdminTier) {
            viewModel.getGroupMediaSummary(groupId).onSuccess { mediaSummary = it }
            // Public groups join instantly now (no request ever created);
            // private groups' link-based joins still go through this, so
            // admin-tier always checks regardless of isPublic.
            viewModel.listGroupJoinRequests(groupId).onSuccess { joinRequests = it }
        }
    }

    if (mediaScreenOpen) {
        GroupMediaBody(groupId = groupId, onBack = { mediaScreenOpen = false }, viewModel = viewModel)
        return
    }

    Column(modifier = Modifier.fillMaxSize().background(CedalColors.Background).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.weight(1f)) { MemberBackBar(title = "Group Info", onBack = onBack) }
            Box {
                Text(
                    "⋮", color = CedalColors.TextPrimary, fontSize = 20.sp,
                    modifier = Modifier.padding(horizontal = 8.dp).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { overflowMenuOpen = true },
                )
                androidx.compose.material3.DropdownMenu(
                    expanded = overflowMenuOpen,
                    onDismissRequest = { overflowMenuOpen = false },
                    modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(CedalColors.CardBackground).border(1.dp, CedalColors.BorderCyan, RoundedCornerShape(16.dp)),
                ) {
                    // Rare/destructive actions - kept out of the main scroll
                    // per Round-4 "don't gush everything inline" feedback.
                    if (isCreator) {
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text("CLEAR CHAT", color = CedalColors.Error, fontSize = 12.sp) },
                            onClick = { overflowMenuOpen = false; clearChatConfirmOpen = true },
                        )
                    }
                    if (isAdminTier) {
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text("CLEAR ALL MEDIA", color = CedalColors.Error, fontSize = 12.sp) },
                            onClick = { overflowMenuOpen = false; clearMediaConfirmOpen = true },
                        )
                    }
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text("REPORT GROUP", color = CedalColors.TextPrimary, fontSize = 12.sp) },
                        onClick = { overflowMenuOpen = false; reportOpen = true },
                    )
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text("BLOCK GROUP", color = CedalColors.Error, fontSize = 12.sp) },
                        onClick = {
                            overflowMenuOpen = false
                            scope.launch { viewModel.setGroupBlocked(groupId, true); myUserId?.let { viewModel.removeGroupMember(groupId, it) }; onLeftGroup() }
                        },
                    )
                }
            }
        }
        CedalErrorText(error)

        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            val g = group
            if (g == null) {
                Text("Loading…", color = CedalColors.TextMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 24.dp))
                return@Column
            }
            val canEditInfo = groupMeetsThreshold(myRole, g.whoCanEditInfo)

            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)) {
                Box(
                    modifier = Modifier.size(88.dp).clip(CircleShape).background(CedalColors.BackgroundBlob).border(2.dp, CedalColors.BorderCyan, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    val avatarUrl = g.avatarUrl
                    if (avatarUrl != null) {
                        AsyncImage(model = avatarUrl, contentDescription = "Group avatar", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(CircleShape))
                    } else {
                        Icon(Icons.Outlined.Groups, contentDescription = null, tint = CedalColors.AccentCyan, modifier = Modifier.size(36.dp))
                    }
                }
                Text(
                    g.name, color = CedalColors.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .let { if (canEditInfo) it.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { editNameOpen = true } else it },
                )
                Text("${g.members.size} members", color = CedalColors.TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))

                // "Known" group calling - up top, before the description, per
                // the explicit ask. Tapping opens a member picker (a real
                // multi-party conference call isn't possible over a native
                // dialer intent - see CallUtils.kt - so this places a normal
                // 1:1 call to whichever member you pick). Creator-only lock,
                // narrower than the Vice-Creator-can-too pattern the other
                // settings below use.
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 10.dp)) {
                    Text(
                        "Group Call", color = if (g.callsEnabled) CedalColors.AccentCyan else CedalColors.TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .border(1.dp, if (g.callsEnabled) CedalColors.BorderCyan else CedalColors.BorderSlate, RoundedCornerShape(50))
                            .let { if (g.callsEnabled) it.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { groupCallPickerOpen = true } else it }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                    if (isCreator) {
                        Icon(
                            if (g.callsEnabled) Icons.Filled.LockOpen else Icons.Filled.Lock,
                            contentDescription = if (g.callsEnabled) "Group Calls enabled - tap to lock it off" else "Group Calls locked off - tap to enable",
                            tint = if (g.callsEnabled) CedalColors.TextMuted else CedalColors.Error,
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .size(16.dp)
                                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                    scope.launch { viewModel.updateGroupSettings(groupId, callsEnabled = !g.callsEnabled).onSuccess { group = it } }
                                },
                        )
                    }
                }

                Text(
                    g.description?.takeIf { it.isNotBlank() } ?: if (canEditInfo) "Add a description" else "",
                    color = CedalColors.TextMuted, fontSize = 12.sp,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .let { if (canEditInfo) it.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { editDescriptionOpen = true } else it },
                )
            }

            if (g.rules?.isNotBlank() == true || canEditInfo) {
                Text(
                    "GROUP RULES", color = CedalColors.TextSecondary, fontSize = 11.sp, letterSpacing = 2.sp,
                    modifier = Modifier.padding(top = 4.dp, bottom = 6.dp),
                )
                Text(
                    g.rules?.takeIf { it.isNotBlank() } ?: if (canEditInfo) "Add group rules - shown once to new members" else "",
                    color = CedalColors.TextMuted, fontSize = 12.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .let { if (canEditInfo) it.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { editRulesOpen = true } else it },
                )
            }

            if (g.pinnedMessageId != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(CedalColors.CardBackground)
                        .border(1.dp, CedalColors.BorderCyan, RoundedCornerShape(10.dp))
                        .padding(12.dp),
                ) {
                    Text("Pinned message for everyone", color = CedalColors.TextPrimary, fontSize = 12.sp, modifier = Modifier.weight(1f))
                    if (isAdminTier && groupRoleRank(myRole) >= groupRoleRank(g.pinnedByRole ?: "MEMBER")) {
                        Text(
                            "UNPIN", color = CedalColors.AccentCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                scope.launch { viewModel.unpinGroupMessage(groupId).onSuccess { group = it } }
                            },
                        )
                    }
                }
            }

            // Overview/Security tab switcher - Round-4 "too gushed" fix.
            // Overview: identity/members/permissions. Security: privacy/
            // moderation-adjacent settings. Everything above this line
            // (avatar/name/description/rules/pinned banner) is a shared
            // header, same for both tabs.
            Row(
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .clip(RoundedCornerShape(50))
                    .border(1.dp, CedalColors.BorderSlate, RoundedCornerShape(50))
                    .padding(2.dp),
            ) {
                // "LINK" - every group has one now, but who can SEE it
                // differs (see canSeeLink above): every member for a public
                // group, admin-tier only for a private one.
                (if (canSeeLink) listOf("OVERVIEW", "SECURITY", "LINK") else listOf("OVERVIEW", "SECURITY")).forEach { tab ->
                    val selected = profileTab == tab
                    Text(
                        tab, color = if (selected) CedalColors.Background else CedalColors.TextSecondary, fontSize = 11.sp, letterSpacing = 0.8.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (selected) CedalColors.AccentCyan else Color.Transparent)
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { profileTab = tab }
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                    )
                }
            }

            if (profileTab == "OVERVIEW") {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                Text("MEMBERS", color = CedalColors.TextSecondary, fontSize = 11.sp, letterSpacing = 2.sp, modifier = Modifier.weight(1f))
                val canAddMember = groupMeetsThreshold(myRole, g.whoCanAddMembers)
                if (canAddMember) {
                    Text(
                        "ADD", color = CedalColors.AccentCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp,
                        modifier = Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { addMemberSheetOpen = true },
                    )
                }
            }
            g.members.forEach { member ->
                val isSelf = member.userId == myUserId
                val hasAuthority = canKickRole(myRole, member.role, isSelf) || rolePromoteOptions(myRole, member.role, isSelf).isNotEmpty()
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .let { if (hasAuthority) it.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { actionSheetFor = member } else it }
                        .padding(vertical = 10.dp),
                ) {
                    Box(
                        modifier = Modifier.size(36.dp).clip(CircleShape).background(CedalColors.BackgroundBlob).border(1.dp, CedalColors.BorderCyan, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Outlined.Person, contentDescription = null, tint = CedalColors.AccentCyan, modifier = Modifier.size(16.dp))
                    }
                    Column(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
                        Text(nameFor(member.userId), color = CedalColors.TextPrimary, fontSize = 13.sp)
                        Text(groupRoleLabel(member.role), color = CedalColors.AccentCyan, fontSize = 10.sp)
                    }
                    if (!isSelf && member.canDm) {
                        Text(
                            "MESSAGE", color = CedalColors.AccentCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                onMessageUser(member.userId, nameFor(member.userId))
                            },
                        )
                    }
                }
            }

            if (isAdminTier && joinRequests.isNotEmpty()) {
                Text(
                    "JOIN REQUESTS", color = CedalColors.TextSecondary, fontSize = 11.sp, letterSpacing = 2.sp,
                    modifier = Modifier.padding(top = 20.dp, bottom = 8.dp),
                )
                joinRequests.forEach { req ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                        Text(nameFor(req.userId), color = CedalColors.TextPrimary, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        Text(
                            "APPROVE", color = CedalColors.Success, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(end = 12.dp).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                scope.launch { viewModel.approveGroupJoinRequest(groupId, req.userId); joinRequests = joinRequests.filter { it.userId != req.userId }; refresh() }
                            },
                        )
                        Text(
                            "REJECT", color = CedalColors.Error, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                scope.launch { viewModel.rejectGroupJoinRequest(groupId, req.userId); joinRequests = joinRequests.filter { it.userId != req.userId } }
                            },
                        )
                    }
                }
            }

            Text(
                "NOTIFICATIONS", color = CedalColors.TextSecondary, fontSize = 11.sp, letterSpacing = 2.sp,
                modifier = Modifier.padding(top = 20.dp, bottom = 4.dp),
            )
            SettingsToggleRow(
                label = "Mute this group", description = "Stop notifications for new messages",
                value = g.muted,
                onValueChange = { v -> scope.launch { viewModel.setGroupMuted(groupId, v); refresh() } },
            )

            if (isAdminTier) {
                val canLock = groupRoleRank(myRole) >= groupRoleRank("VICE_CREATOR")
                fun toggleLock(key: String) {
                    val next = if (key in g.lockedSettings) g.lockedSettings - key else g.lockedSettings + key
                    scope.launch { viewModel.updateGroupSettings(groupId, lockedSettings = next).onSuccess { group = it } }
                }
                Text(
                    "GROUP PERMISSIONS", color = CedalColors.TextSecondary, fontSize = 11.sp, letterSpacing = 2.sp,
                    modifier = Modifier.padding(top = 20.dp, bottom = 8.dp),
                )
                GroupPermissionRow(
                    label = "Who can send messages", settingKey = "whoCanSendMessages", value = g.whoCanSendMessages,
                    lockedSettings = g.lockedSettings, myRole = myRole, canLock = canLock, onToggleLock = ::toggleLock,
                    onChange = { v -> scope.launch { viewModel.updateGroupSettings(groupId, whoCanSendMessages = v).onSuccess { group = it } } },
                )
                GroupPermissionRow(
                    label = "Who can edit group info", settingKey = "whoCanEditInfo", value = g.whoCanEditInfo,
                    lockedSettings = g.lockedSettings, myRole = myRole, canLock = canLock, onToggleLock = ::toggleLock,
                    onChange = { v -> scope.launch { viewModel.updateGroupSettings(groupId, whoCanEditInfo = v).onSuccess { group = it } } },
                    restrictToViceCreatorAndCreator = true,
                )
                GroupPermissionRow(
                    label = "Who can add members", settingKey = "whoCanAddMembers", value = g.whoCanAddMembers,
                    lockedSettings = g.lockedSettings, myRole = myRole, canLock = canLock, onToggleLock = ::toggleLock,
                    onChange = { v -> scope.launch { viewModel.updateGroupSettings(groupId, whoCanAddMembers = v).onSuccess { group = it } } },
                )
                GroupPermissionRow(
                    label = "Who can see group stats", settingKey = "whoCanSeeGroupStats", value = g.whoCanSeeGroupStats,
                    lockedSettings = g.lockedSettings, myRole = myRole, canLock = canLock, onToggleLock = ::toggleLock,
                    onChange = { v -> scope.launch { viewModel.updateGroupSettings(groupId, whoCanSeeGroupStats = v).onSuccess { group = it } } },
                )
                GroupPermissionRow(
                    label = "Who can send media", settingKey = "whoCanSendMedia", value = g.whoCanSendMedia,
                    lockedSettings = g.lockedSettings, myRole = myRole, canLock = canLock, onToggleLock = ::toggleLock,
                    onChange = { v -> scope.launch { viewModel.updateGroupSettings(groupId, whoCanSendMedia = v).onSuccess { group = it } } },
                )

                SettingsToggleRow(
                    label = "Share history with new members", description = "New members see messages sent before they joined",
                    value = g.shareHistoryWithNewMembers,
                    onValueChange = { v -> scope.launch { viewModel.updateGroupSettings(groupId, shareHistoryWithNewMembers = v).onSuccess { group = it } } },
                )
                SettingsToggleRow(
                    label = "Public group",
                    description = "Discoverable in group search; anyone can join instantly, no approval needed. Off: link is admin-tier-only and joining through it needs an admin's approval.",
                    value = g.isPublic,
                    onValueChange = { v -> scope.launch { viewModel.updateGroupSettings(groupId, isPublic = v).onSuccess { group = it } } },
                )
            }
            } // end OVERVIEW

            if (profileTab == "SECURITY") {
            if (isAdminTier) {
                SettingsToggleRow(
                    label = "Secured mode", description = "Disables saving messages; screenshot-blocks the chat",
                    value = g.securedMode,
                    onValueChange = { v -> scope.launch { viewModel.updateGroupSettings(groupId, securedMode = v).onSuccess { group = it } } },
                )
                SettingsToggleRow(
                    label = "Disappearing messages", description = if (g.disappearingMessagesDurationMs != null) "On - new messages expire automatically" else "Off",
                    value = g.disappearingMessagesDurationMs != null,
                    onValueChange = { v ->
                        scope.launch {
                            if (v) viewModel.updateGroupSettings(groupId, disappearingMessagesDurationMs = 7L * 24 * 60 * 60 * 1000).onSuccess { group = it }
                            else viewModel.updateGroupSettings(groupId, disappearingMessagesOff = true).onSuccess { group = it }
                        }
                    },
                )
            }
            if (isCreator) {
                SettingsToggleRow(
                    label = "Close group DMs", description = "Nobody can message another member directly from this group",
                    value = g.dmClosedByCreator,
                    onValueChange = { v -> scope.launch { viewModel.updateGroupSettings(groupId, dmClosedByCreator = v).onSuccess { group = it } } },
                )
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { autoDeleteOverlayOpen = true }) {
                    Text("Auto-delete group", color = CedalColors.TextPrimary, fontSize = 13.sp)
                    Text(
                        if (g.autoDeleteAt != null) {
                            "On - deletes ${SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(g.autoDeleteAt!!))}"
                        } else {
                            "Off - tap to set a custom date (1 day - 1 year out)"
                        },
                        color = CedalColors.TextSecondary, fontSize = 11.sp,
                    )
                }
            }

            SettingsToggleRow(
                label = "My DM setting for this group", description = if (g.myDmOverride == "CLOSED") "Closed - nobody can message you from here" else "Open - members can message you (unless the group closes DMs)",
                value = g.myDmOverride != "CLOSED",
                onValueChange = { v -> scope.launch { viewModel.setGroupDmOverride(groupId, if (v) "OPEN" else "CLOSED"); refresh() } },
            )
            var lockedLocally by remember { mutableStateOf(viewModel.storage.isGroupLocked(groupId)) }
            SettingsToggleRow(
                label = "Lock this chat", description = "Require biometrics/passcode to open this group's chat on this device",
                value = lockedLocally,
                onValueChange = { v -> viewModel.storage.setGroupLocked(groupId, v); lockedLocally = v },
            )

            mediaSummary?.let { m ->
                Text(
                    "MEDIA & STORAGE", color = CedalColors.TextSecondary, fontSize = 11.sp, letterSpacing = 2.sp,
                    modifier = Modifier.padding(top = 20.dp, bottom = 6.dp),
                )
                Text(
                    "${m.images} images, ${m.videos} videos, ${m.files} files - tap for details",
                    color = CedalColors.TextMuted, fontSize = 12.sp,
                    modifier = Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { mediaScreenOpen = true },
                )
            }
            } // end SECURITY

            if (profileTab == "LINK" && canSeeLink) {
                GroupLinkTabContent(
                    groupId = groupId,
                    inviteToken = g.inviteToken,
                    canReset = isAdminTier,
                    onReset = { scope.launch { viewModel.resetGroupInviteLink(groupId).onSuccess { group = it } } },
                )
            }

            Text(
                "LEAVE GROUP", color = CedalColors.Error, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .padding(top = 24.dp)
                    .clip(RoundedCornerShape(50))
                    .border(1.dp, CedalColors.Error, RoundedCornerShape(50))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { leaveConfirmOpen = true }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            )
        }
    }

    if (groupCallPickerOpen) {
        val g = group
        if (g != null) {
            GroupCallPickerOverlay(
                members = g.members.filter { it.userId != myUserId },
                nameFor = nameFor,
                onDismiss = { groupCallPickerOpen = false },
                onPick = { phoneNumber ->
                    groupCallPickerOpen = false
                    launchDialer(context, phoneNumber)
                },
            )
        }
    }

    if (editNameOpen) {
        val g = group
        GroupProfileTextEditOverlay(
            title = "Rename group", initial = g?.name.orEmpty(), multiline = false,
            onSave = { v -> if (v.isNotBlank()) scope.launch { viewModel.updateGroupInfo(groupId, name = v).onSuccess { group = it } }; editNameOpen = false },
            onDismiss = { editNameOpen = false },
        )
    }

    if (editDescriptionOpen) {
        val g = group
        GroupProfileTextEditOverlay(
            title = "Group description", initial = g?.description.orEmpty(), multiline = true,
            onSave = { v -> scope.launch { viewModel.updateGroupInfo(groupId, description = v).onSuccess { group = it } }; editDescriptionOpen = false },
            onDismiss = { editDescriptionOpen = false },
        )
    }

    if (editRulesOpen) {
        val g = group
        GroupProfileTextEditOverlay(
            title = "Group rules", initial = g?.rules.orEmpty(), multiline = true,
            onSave = { v -> scope.launch { viewModel.updateGroupInfo(groupId, rules = v).onSuccess { group = it } }; editRulesOpen = false },
            onDismiss = { editRulesOpen = false },
        )
    }

    if (autoDeleteOverlayOpen) {
        GroupAutoDeleteOverlay(
            currentlyOn = group?.autoDeleteAt != null,
            onSet = { durationMs ->
                autoDeleteOverlayOpen = false
                scope.launch { viewModel.updateGroupSettings(groupId, autoDeleteDurationMs = durationMs).onSuccess { group = it } }
            },
            onTurnOff = {
                autoDeleteOverlayOpen = false
                scope.launch { viewModel.updateGroupSettings(groupId, autoDeleteOff = true).onSuccess { group = it } }
            },
            onDismiss = { autoDeleteOverlayOpen = false },
        )
    }

    if (reportOpen) {
        GroupProfileTextEditOverlay(
            title = "Report group - reason", initial = "", multiline = true,
            onSave = { v -> scope.launch { viewModel.reportGroup(groupId, reason = v.ifBlank { null }) }; reportOpen = false },
            onDismiss = { reportOpen = false },
        )
    }

    if (clearChatConfirmOpen) {
        GroupSimpleConfirmOverlay(
            title = "Clear all messages?",
            body = "This deletes every message in this group for everyone. The group and its members stay.",
            confirmLabel = "CLEAR",
            onConfirm = { clearChatConfirmOpen = false; scope.launch { viewModel.clearGroupChat(groupId) } },
            onDismiss = { clearChatConfirmOpen = false },
        )
    }

    if (clearMediaConfirmOpen) {
        GroupSimpleConfirmOverlay(
            title = "Clear all media?",
            body = "This removes every photo, video, and file from this group's messages.",
            confirmLabel = "CLEAR",
            onConfirm = {
                clearMediaConfirmOpen = false
                scope.launch { viewModel.clearGroupMedia(groupId); viewModel.getGroupMediaSummary(groupId).onSuccess { mediaSummary = it } }
            },
            onDismiss = { clearMediaConfirmOpen = false },
        )
    }

    actionSheetFor?.let { member ->
        GroupMemberActionSheet(
            member = member,
            memberName = nameFor(member.userId),
            myRole = myRole,
            onSetRole = { newRole ->
                actionSheetFor = null
                scope.launch { viewModel.setGroupRole(groupId, member.userId, newRole); refresh() }
            },
            onKick = {
                actionSheetFor = null
                scope.launch { viewModel.removeGroupMember(groupId, member.userId); refresh() }
            },
            onDismiss = { actionSheetFor = null },
        )
    }

    if (addMemberSheetOpen) {
        AddGroupMemberSheet(
            currentMemberIds = group?.members.orEmpty().map { it.userId }.toSet(),
            viewModel = viewModel,
            onAdd = { friendId ->
                scope.launch {
                    viewModel.addGroupMember(groupId, friendId).onSuccess {
                        addMemberSheetOpen = false
                        refresh()
                    }.onFailure { error = it.message }
                }
            },
            onDismiss = { addMemberSheetOpen = false },
        )
    }

    if (leaveConfirmOpen) {
        val g = group
        if (g != null && isCreator) {
            GroupCreatorLeaveOverlay(
                group = g,
                myUserId = myUserId,
                nameFor = nameFor,
                onDissolve = {
                    leaveConfirmOpen = false
                    scope.launch { viewModel.leaveGroup(groupId, dissolve = true); onLeftGroup() }
                },
                onTransfer = { successorId, random ->
                    leaveConfirmOpen = false
                    scope.launch {
                        viewModel.leaveGroup(groupId, successorId = successorId, random = random)
                            .onSuccess { onLeftGroup() }.onFailure { error = it.message }
                    }
                },
                onSystemOwner = {
                    leaveConfirmOpen = false
                    scope.launch {
                        viewModel.leaveGroup(groupId, systemOwner = true)
                            .onSuccess { onLeftGroup() }.onFailure { error = it.message }
                    }
                },
                onDismiss = { leaveConfirmOpen = false },
            )
        } else {
            GroupSimpleConfirmOverlay(
                title = "Leave ${group?.name ?: "group"}?",
                body = "You'll stop receiving messages from this group. You'd need to be added back to rejoin.",
                confirmLabel = "LEAVE",
                onConfirm = {
                    leaveConfirmOpen = false
                    scope.launch {
                        myUserId?.let { viewModel.removeGroupMember(groupId, it) }
                        onLeftGroup()
                    }
                },
                onDismiss = { leaveConfirmOpen = false },
            )
        }
    }
}

// Creator-only leave flow - GroupChatService.removeMember now rejects a
// creator self-leave outright (see its own doc comment), so this is the
// only path a Creator has to leave. Mirrors leaveGroup's exact succession
// order server-side: Vice-Creator auto-succeeds; otherwise Admins, then
// plain Members, require an explicit pick (or Random) - never a silent
// auto-choice. If nobody else is in the group, Dissolve or System Owner are
// the only two options.
@Composable
private fun GroupCreatorLeaveOverlay(
    group: GroupDto,
    myUserId: String?,
    nameFor: (String) -> String,
    onDissolve: () -> Unit,
    onTransfer: (successorId: String?, random: Boolean) -> Unit,
    onSystemOwner: () -> Unit,
    onDismiss: () -> Unit,
) {
    val others = group.members.filter { it.userId != myUserId }
    val viceCreator = others.firstOrNull { it.role == "VICE_CREATOR" }
    val admins = others.filter { it.role == "ADMIN" }
    val plainMembers = others.filter { it.role == "MEMBER" }
    // Whoever "Choose a specific person" should offer - Admins first, only
    // falling back to plain Members when there are no Admins, same
    // succession order as leaveGroup server-side.
    val pickablePeople = admins.ifEmpty { plainMembers }
    var systemConfirmOpen by remember { mutableStateOf(false) }
    var pickPersonOpen by remember { mutableStateOf(false) }

    if (pickPersonOpen) {
        var searchQuery by remember { mutableStateOf("") }
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = { pickPersonOpen = false }),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .padding(32.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(CedalColors.CardBackground)
                    .border(1.dp, CedalColors.BorderCyan, RoundedCornerShape(18.dp))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {})
                    .padding(20.dp),
            ) {
                Text(
                    if (admins.isNotEmpty()) "Choose which Admin becomes the new Creator" else "No Admins - choose who becomes the new Creator",
                    color = CedalColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp),
                )
                Box(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).border(1.dp, CedalColors.BorderSlate, RoundedCornerShape(10.dp)).padding(horizontal = 10.dp, vertical = 8.dp),
                ) {
                    if (searchQuery.isEmpty()) Text("Search…", color = CedalColors.TextMuted, fontSize = 13.sp)
                    BasicTextField(
                        value = searchQuery, onValueChange = { searchQuery = it }, singleLine = true,
                        textStyle = TextStyle(color = CedalColors.TextPrimary, fontSize = 13.sp),
                        cursorBrush = SolidColor(CedalColors.AccentCyan),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Column(modifier = Modifier.heightIn(max = 240.dp).verticalScroll(rememberScrollState()).padding(top = 8.dp)) {
                    pickablePeople.filter { searchQuery.isBlank() || nameFor(it.userId).contains(searchQuery, ignoreCase = true) }
                        .forEach { p -> ActionMenuRow(label = nameFor(p.userId), onClick = { pickPersonOpen = false; onTransfer(p.userId, false) }) }
                }
                ActionMenuRow(label = "Cancel", onClick = { pickPersonOpen = false })
            }
        }
        return
    }

    if (systemConfirmOpen) {
        GroupSimpleConfirmOverlay(
            title = "Hand this group to Cedal System?",
            body = "Nobody else is in this group. It'll stay around, owned by the built-in Cedal System account, instead of being deleted. Are you sure, or would you rather edit your choice?",
            confirmLabel = "CONFIRM",
            onConfirm = onSystemOwner,
            onDismiss = { systemConfirmOpen = false },
        )
        return
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .padding(32.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(CedalColors.CardBackground)
                .border(1.dp, CedalColors.BorderCyan, RoundedCornerShape(18.dp))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {})
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
        ) {
            Text("Leave ${group.name}", color = CedalColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
            Text("You're the Creator - choose what happens to the group.", color = CedalColors.TextMuted, fontSize = 11.sp, modifier = Modifier.padding(bottom = 14.dp))

            // Options only, never a name list here - see Round-4 feedback.
            // Tapping "Choose a specific person" is the only thing that
            // opens the actual name list (pickPersonOpen, above).
            when {
                viceCreator != null -> {
                    Text("${nameFor(viceCreator.userId)} (Vice-Creator) will become the new Creator.", color = CedalColors.TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(bottom = 14.dp))
                    ActionMenuRow(label = "Leave and transfer ownership", onClick = { onTransfer(null, false) })
                }
                pickablePeople.isNotEmpty() -> {
                    ActionMenuRow(label = "Pick Random", onClick = { onTransfer(null, true) })
                    ActionMenuRow(label = "Choose a specific person", onClick = { pickPersonOpen = true })
                }
                else -> {
                    Text("You're the only member left.", color = CedalColors.TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(bottom = 6.dp))
                    ActionMenuRow(label = "Hand group to Cedal System", onClick = { systemConfirmOpen = true })
                }
            }
            ActionMenuRow(label = "Dissolve Group Instead", color = CedalColors.Error, onClick = onDissolve)
            ActionMenuRow(label = "Cancel", onClick = onDismiss)
        }
    }
}

// Role-change matrix from GroupChatService.setRole: CREATOR can set anyone
// (but self) to any role, appointing a new VICE_CREATOR auto-demotes the
// old one; VICE_CREATOR can only move ADMIN<->MEMBER, never touching
// CREATOR or the VICE_CREATOR slot itself. Returns (label, newRole) pairs
// for whatever's actually available - never the CREATOR role as a target
// (untouchable) and never for self.
private fun rolePromoteOptions(myRole: String?, targetRole: String, isSelf: Boolean): List<Pair<String, String>> {
    if (isSelf || targetRole == "CREATOR") return emptyList()
    return when (myRole) {
        "CREATOR" -> when (targetRole) {
            "VICE_CREATOR" -> listOf("Demote to Admin" to "ADMIN")
            "ADMIN" -> listOf("Promote to Vice-Creator" to "VICE_CREATOR", "Demote to Member" to "MEMBER")
            "MEMBER" -> listOf("Promote to Admin" to "ADMIN", "Promote to Vice-Creator" to "VICE_CREATOR")
            else -> emptyList()
        }
        "VICE_CREATOR" -> when (targetRole) {
            "ADMIN" -> listOf("Demote to Member" to "MEMBER")
            "MEMBER" -> listOf("Promote to Admin" to "ADMIN")
            else -> emptyList()
        }
        else -> emptyList()
    }
}

@Composable
private fun GroupMemberActionSheet(
    member: GroupMemberDto,
    memberName: String,
    myRole: String?,
    onSetRole: (String) -> Unit,
    onKick: () -> Unit,
    onDismiss: () -> Unit,
) {
    val promoteOptions = rolePromoteOptions(myRole, member.role, isSelf = false)
    val canKick = canKickRole(myRole, member.role, isSelf = false)
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .padding(32.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(CedalColors.CardBackground)
                .border(1.dp, CedalColors.BorderCyan, RoundedCornerShape(18.dp))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {})
                .padding(8.dp),
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                Text(memberName, color = CedalColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(groupRoleLabel(member.role), color = CedalColors.TextMuted, fontSize = 11.sp)
            }
            promoteOptions.forEach { (label, newRole) ->
                ActionMenuRow(label = label, onClick = { onSetRole(newRole) })
            }
            if (canKick) {
                ActionMenuRow(label = "Remove from group", color = CedalColors.Error, onClick = onKick)
            }
        }
    }
}

// 4-tier rank picker (MEMBER/ADMIN/VICE_CREATOR/CREATOR), generalized from
// the old 2-pill ALL/ADMINS_ONLY row - wraps to two rows of 2, four pills
// don't fit one line at this sizing. The lock icon (rank >= VICE_CREATOR
// only) toggles this ONE setting's entry in Groups.lockedSettings; while
// locked, a plain Admin sees the pills as read-only.
@Composable
private fun GroupPermissionRow(
    label: String,
    settingKey: String,
    value: String,
    lockedSettings: List<String>,
    myRole: String?,
    canLock: Boolean,
    onToggleLock: (String) -> Unit,
    onChange: (String) -> Unit,
    // "Who can edit group info" is narrower than the other 4 rank-threshold
    // settings - only ever VICE_CREATOR/CREATOR, matching the same
    // restriction GroupChatService.updateGroupSettings enforces server-side.
    restrictToViceCreatorAndCreator: Boolean = false,
) {
    val locked = settingKey in lockedSettings
    val editable = !locked || groupRoleRank(myRole) >= groupRoleRank("VICE_CREATOR")
    val options = if (restrictToViceCreatorAndCreator) {
        listOf("VICE_CREATOR" to "Vice-Creator+", "CREATOR" to "Creator only")
    } else {
        listOf("MEMBER" to "Everyone", "ADMIN" to "Admin+", "VICE_CREATOR" to "Vice-Creator+", "CREATOR" to "Creator only")
    }
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = CedalColors.TextPrimary, fontSize = 13.sp, modifier = Modifier.weight(1f))
            if (canLock) {
                Icon(
                    if (locked) Icons.Filled.Lock else Icons.Filled.LockOpen,
                    contentDescription = if (locked) "Locked" else "Unlocked",
                    tint = if (locked) CedalColors.Error else CedalColors.TextMuted,
                    modifier = Modifier.size(16.dp).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onToggleLock(settingKey) },
                )
            } else if (locked) {
                Icon(Icons.Filled.Lock, contentDescription = "Locked", tint = CedalColors.Error, modifier = Modifier.size(16.dp))
            }
        }
        options.chunked(2).forEach { row ->
            Row(modifier = Modifier.padding(top = 6.dp)) {
                row.forEach { (opt, optLabel) ->
                    val selected = value == opt
                    Text(
                        optLabel, color = if (selected) CedalColors.Background else CedalColors.TextPrimary, fontSize = 12.sp,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .clip(RoundedCornerShape(50))
                            .background(if (selected) CedalColors.AccentCyan else Color.Transparent)
                            .border(1.dp, if (selected) CedalColors.AccentCyan else CedalColors.BorderSlate, RoundedCornerShape(50))
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, enabled = editable) { if (!selected) onChange(opt) }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            }
        }
    }
}

// Group Profile's "LINK" tab (Round 5) - invite link + QR + share/copy/
// reset, shown to every member of a public group or admin-tier members of
// a private one (see canSeeLink at its call site). The link is a
// custom-scheme deep link (cedalcode://group/{token}, see
// AndroidManifest.xml/MainActivity.kt/GroupLinkDeepLinkState) that lands on
// GroupLinkJoinBody's join-preview screen: for a public group that's an
// instant join (same as finding it in search), for a private group it
// still needs an admin-tier member to approve, same as the old
// public-only-link behavior.
@Composable
private fun GroupLinkTabContent(groupId: String, inviteToken: String?, canReset: Boolean, onReset: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var resetConfirmOpen by remember { mutableStateOf(false) }
    val link = inviteToken?.let { "cedalcode://group/$it" }

    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        if (link == null) {
            Text("Generating link…", color = CedalColors.TextMuted, fontSize = 12.sp, modifier = Modifier.padding(vertical = 24.dp))
        } else {
            val qrBitmap = remember(link) { generateQrBitmap(link, 480) }
            if (qrBitmap != null) {
                androidx.compose.foundation.Image(
                    bitmap = qrBitmap.asImageBitmap(),
                    contentDescription = "Group QR code",
                    modifier = Modifier
                        .padding(vertical = 16.dp)
                        .size(200.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .padding(12.dp),
                )
            }
            Text(
                link, color = CedalColors.TextSecondary, fontSize = 12.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.dp, CedalColors.BorderSlate, RoundedCornerShape(10.dp))
                    .padding(10.dp),
            )
            Row(modifier = Modifier.padding(top = 14.dp)) {
                Text(
                    "COPY", color = CedalColors.AccentCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .border(1.dp, CedalColors.AccentCyan, RoundedCornerShape(50))
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                            val cm = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            cm.setPrimaryClip(android.content.ClipData.newPlainText("Group invite link", link))
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
                Text(
                    "SHARE", color = CedalColors.AccentCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .padding(start = 10.dp)
                        .clip(RoundedCornerShape(50))
                        .border(1.dp, CedalColors.AccentCyan, RoundedCornerShape(50))
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(android.content.Intent.EXTRA_TEXT, link)
                            }
                            context.startActivity(android.content.Intent.createChooser(intent, "Share group link"))
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            if (canReset) {
                Text(
                    "RESET LINK", color = CedalColors.Error, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { resetConfirmOpen = true },
                )
            }
        }
    }

    if (resetConfirmOpen) {
        GroupSimpleConfirmOverlay(
            title = "Reset invite link?",
            body = "Anyone with the old link or QR code will no longer be able to use it.",
            confirmLabel = "RESET",
            onConfirm = { resetConfirmOpen = false; onReset() },
            onDismiss = { resetConfirmOpen = false },
        )
    }
}

// zxing's core QRCodeWriter (already a dependency for the QR SCANNER in
// MemberSearchScreen.kt) also does encoding - no separate library needed to
// generate one.
private fun generateQrBitmap(content: String, sizePx: Int): android.graphics.Bitmap? = try {
    val matrix = com.google.zxing.qrcode.QRCodeWriter().encode(content, com.google.zxing.BarcodeFormat.QR_CODE, sizePx, sizePx)
    val bitmap = android.graphics.Bitmap.createBitmap(sizePx, sizePx, android.graphics.Bitmap.Config.RGB_565)
    for (x in 0 until sizePx) {
        for (y in 0 until sizePx) {
            bitmap.setPixel(x, y, if (matrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
        }
    }
    bitmap
} catch (e: Exception) {
    null
}

// Real customizable duration (not a fixed 30-day on/off toggle, per
// Round-4 feedback) - number + unit, clamped server-side to [1 day, 1 year]
// anyway (see GroupChatService.updateGroupSettings), clamped here too so
// the preview shown before saving isn't misleading.
@Composable
private fun GroupAutoDeleteOverlay(currentlyOn: Boolean, onSet: (durationMs: Long) -> Unit, onTurnOff: () -> Unit, onDismiss: () -> Unit) {
    var amountText by remember { mutableStateOf("30") }
    var unit by remember { mutableStateOf("Days") } // Days | Weeks | Months
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .padding(28.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(CedalColors.CardBackground)
                .border(1.dp, CedalColors.BorderCyan, RoundedCornerShape(18.dp))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {})
                .padding(20.dp),
        ) {
            Text("Auto-delete group", color = CedalColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
            Text("Deletes the whole group automatically after this long from now (1 day - 1 year).", color = CedalColors.TextSecondary, fontSize = 11.sp, modifier = Modifier.padding(bottom = 12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, CedalColors.BorderSlate, RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                ) {
                    BasicTextField(
                        value = amountText,
                        onValueChange = { if (it.length <= 3 && it.all(Char::isDigit)) amountText = it },
                        singleLine = true,
                        textStyle = TextStyle(color = CedalColors.TextPrimary, fontSize = 14.sp),
                        cursorBrush = SolidColor(CedalColors.AccentCyan),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    )
                }
                listOf("Days", "Weeks", "Months").forEach { u ->
                    Text(
                        u, color = if (unit == u) CedalColors.Background else CedalColors.TextPrimary, fontSize = 11.sp,
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .clip(RoundedCornerShape(50))
                            .background(if (unit == u) CedalColors.AccentCyan else Color.Transparent)
                            .border(1.dp, CedalColors.BorderSlate, RoundedCornerShape(50))
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { unit = u }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                    )
                }
            }
            Row(modifier = Modifier.padding(top = 16.dp)) {
                if (currentlyOn) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.dp, CedalColors.Error, RoundedCornerShape(10.dp))
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onTurnOff)
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) { Text("TURN OFF", color = CedalColors.Error, fontSize = 12.sp) }
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = if (currentlyOn) 10.dp else 0.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(CedalColors.AccentCyan)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                            val amount = (amountText.toIntOrNull() ?: 1).coerceAtLeast(1)
                            val days = when (unit) { "Weeks" -> amount * 7; "Months" -> amount * 30; else -> amount }
                            onSet(days.coerceIn(1, 365).toLong() * 24 * 60 * 60 * 1000)
                        }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) { Text("SET", color = CedalColors.Background, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

// Group Call's member picker - a real multi-party conference call isn't
// achievable through a native dialer intent (see CallUtils.kt), so this
// places a normal 1:1 "Known" call to whichever member is picked. Only
// members who've shared their number with the viewer (member.canCall) are
// tappable - everyone else is listed but grayed out, same "show it but
// explain why it's unavailable" convention as MemberFriendProfileScreen.kt.
@Composable
private fun GroupCallPickerOverlay(members: List<GroupMemberDto>, nameFor: (String) -> String, onPick: (String) -> Unit, onDismiss: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .padding(32.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(CedalColors.CardBackground)
                .border(1.dp, CedalColors.BorderCyan, RoundedCornerShape(18.dp))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {})
                .padding(20.dp),
        ) {
            Text("Call a member", color = CedalColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
            Text(
                "Group calls place a normal call to one member at a time, not a conference.",
                color = CedalColors.TextSecondary, fontSize = 11.sp, modifier = Modifier.padding(bottom = 12.dp),
            )
            if (members.isEmpty()) {
                Text("No other members.", color = CedalColors.TextMuted, fontSize = 12.sp)
            }
            members.forEach { m ->
                val phoneNumber = m.phoneNumber
                val callable = m.canCall && phoneNumber != null
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .let { if (callable) it.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onPick(phoneNumber!!) } else it }
                        .padding(vertical = 10.dp),
                ) {
                    Text(nameFor(m.userId), color = if (callable) CedalColors.TextPrimary else CedalColors.TextMuted, fontSize = 13.sp, modifier = Modifier.weight(1f))
                    if (!callable) {
                        Text("Hasn't shared their number", color = CedalColors.TextMuted, fontSize = 10.sp)
                    }
                }
            }
            Text(
                "CLOSE", color = CedalColors.AccentCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
            )
        }
    }
}

@Composable
private fun GroupProfileTextEditOverlay(title: String, initial: String, multiline: Boolean, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf(initial) }
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss).imePadding(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .padding(32.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(CedalColors.CardBackground)
                .border(1.dp, CedalColors.BorderCyan, RoundedCornerShape(18.dp))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {})
                .padding(20.dp),
        ) {
            Text(title, color = CedalColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))
            Box(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).border(1.dp, CedalColors.BorderCyan, RoundedCornerShape(10.dp)).padding(12.dp),
            ) {
                BasicTextField(
                    value = text, onValueChange = { text = it },
                    textStyle = TextStyle(color = CedalColors.TextPrimary, fontSize = 14.sp),
                    cursorBrush = SolidColor(CedalColors.AccentCyan),
                    singleLine = !multiline,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Row(modifier = Modifier.padding(top = 16.dp)) {
                Box(
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).border(1.dp, CedalColors.BorderSlate, RoundedCornerShape(10.dp)).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss).padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) { Text("CANCEL", color = CedalColors.TextPrimary, fontSize = 12.sp) }
                Box(
                    modifier = Modifier.weight(1f).padding(start = 10.dp).clip(RoundedCornerShape(10.dp)).background(CedalColors.AccentCyan).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = { onSave(text) }).padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) { Text("SAVE", color = CedalColors.Background, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            }
        }
    }
}
