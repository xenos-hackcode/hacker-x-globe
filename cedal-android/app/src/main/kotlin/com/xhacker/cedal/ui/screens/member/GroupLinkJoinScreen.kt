package com.xhacker.cedal.ui.screens.member

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Groups
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.xhacker.cedal.data.GroupLinkPreviewDto
import com.xhacker.cedal.ui.theme.CedalColors
import com.xhacker.cedal.ui.theme.CedalErrorText
import com.xhacker.cedal.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

// Landing screen for a scanned/opened group invite link/QR (Round 5) - see
// GroupLinkDeepLinkState/NavGraph's "member_group_link_join/{token}" route.
// Deliberately a preview + "Request to Join" rather than an instant add -
// public groups still go through the same approval flow as finding a group
// via search (see GroupChatService.requestToJoin), the link just skips
// having to search for it.
@Composable
fun GroupLinkJoinBody(token: String, onBack: () -> Unit, onOpenGroup: (groupId: String) -> Unit, viewModel: AuthViewModel = hiltViewModel()) {
    var preview by remember { mutableStateOf<GroupLinkPreviewDto?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var requested by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(token) {
        viewModel.getGroupByToken(token).onSuccess { preview = it; requested = it.alreadyRequested }.onFailure { error = it.message }
        loading = false
    }

    Column(modifier = Modifier.fillMaxSize().background(CedalColors.Background).padding(16.dp)) {
        MemberBackBar(title = "Group Invite", onBack = onBack)
        CedalErrorText(error)

        if (loading) {
            Text("Loading…", color = CedalColors.TextMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 24.dp))
            return@Column
        }
        val p = preview ?: return@Column

        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(top = 24.dp)) {
            Box(
                modifier = Modifier.size(88.dp).clip(CircleShape).background(CedalColors.BackgroundBlob).border(2.dp, CedalColors.BorderCyan, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                val avatarUrl = p.avatarUrl
                if (avatarUrl != null) {
                    AsyncImage(model = avatarUrl, contentDescription = "Group avatar", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(CircleShape))
                } else {
                    Icon(Icons.Outlined.Groups, contentDescription = null, tint = CedalColors.AccentCyan, modifier = Modifier.size(36.dp))
                }
            }
            Text(p.name, color = CedalColors.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp))
            Text("${p.memberCount} members", color = CedalColors.TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
            p.description?.takeIf { it.isNotBlank() }?.let {
                Text(it, color = CedalColors.TextMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
            }

            when {
                p.alreadyMember -> {
                    Text(
                        "OPEN GROUP", color = CedalColors.Background, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .padding(top = 24.dp)
                            .clip(RoundedCornerShape(50))
                            .background(CedalColors.AccentCyan)
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onOpenGroup(p.id) }
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                    )
                }
                requested -> {
                    Text("Request sent - waiting for an admin to approve.", color = CedalColors.TextMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 24.dp))
                }
                else -> {
                    Text(
                        "REQUEST TO JOIN", color = CedalColors.Background, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .padding(top = 24.dp)
                            .clip(RoundedCornerShape(50))
                            .background(CedalColors.AccentCyan)
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                scope.launch { viewModel.requestToJoinGroup(p.id).onSuccess { requested = true }.onFailure { error = it.message } }
                            }
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                    )
                }
            }
        }
    }
}
