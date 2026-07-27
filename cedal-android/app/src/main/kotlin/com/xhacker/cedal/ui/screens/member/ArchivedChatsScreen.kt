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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xhacker.cedal.data.ConversationSummary
import com.xhacker.cedal.ui.theme.CedalColors
import com.xhacker.cedal.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

// "Archived" row's destination (see ChatsListBody/ArchivedRow). Reachable
// without any extra verification, matching WhatsApp - only "Hidden" (below)
// needs biometrics, since archiving is just decluttering, not privacy.
@Composable
fun ArchivedChatsBody(
    onBack: () -> Unit,
    onOpenChat: (friendId: String, name: String) -> Unit,
    onOpenHidden: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    var conversations by remember { mutableStateOf<List<ConversationSummary>>(emptyList()) }
    var pendingHiddenVerify by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.listArchivedConversations().onSuccess { conversations = it }
    }

    fun refresh() {
        scope.launch { viewModel.listArchivedConversations().onSuccess { conversations = it } }
    }

    Column(modifier = Modifier.fillMaxSize().background(CedalColors.Background)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().border(width = androidx.compose.ui.unit.Dp.Hairline, color = CedalColors.BorderSlate).padding(horizontal = 8.dp, vertical = 8.dp),
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = CedalColors.TextPrimary)
            }
            Text("Archived Chats", color = CedalColors.TextPrimary, fontSize = 16.sp, modifier = Modifier.weight(1f))
            // A small labeled box rather than a bare icon - "to click hide
            // u will use ur passcode or fingerprint to open the hidden
            // chats" - biometric-gated, same check as opening a locked chat.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, CedalColors.BorderCyan, RoundedCornerShape(8.dp))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { pendingHiddenVerify = true }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Icon(Icons.Outlined.Lock, contentDescription = null, tint = CedalColors.TextPrimary, modifier = Modifier.size(14.dp))
                Text("Hide", color = CedalColors.TextPrimary, fontSize = 12.sp, modifier = Modifier.padding(start = 4.dp))
            }
        }

        if (conversations.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(24.dp)) {
                Text("No archived chats.", color = CedalColors.TextSecondary, fontSize = 13.sp)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(conversations, key = { it.friendId }) { convo ->
                    ArchivedOrHiddenRow(
                        convo = convo,
                        actionLabel = "UNARCHIVE",
                        onOpen = { onOpenChat(convo.friendId, convo.name) },
                        onAction = {
                            scope.launch {
                                viewModel.bulkChatAction(listOf(convo.friendId), "unarchive")
                                refresh()
                            }
                        },
                    )
                    RowDivider()
                }
            }
        }
    }

    if (pendingHiddenVerify) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { pendingHiddenVerify = false }) {
            AccountVerifyOverlay(
                viewModel = viewModel,
                message = "Viewing hidden chats needs your biometrics or passcode.",
                onVerified = { pendingHiddenVerify = false; onOpenHidden() },
                onCancel = { pendingHiddenVerify = false },
            )
        }
    }
}

// Only ever reached via ArchivedChatsBody's biometric-gated "Hidden" button
// - see ChatService.listHiddenConversations' doc comment for why the server
// itself doesn't also re-check this.
@Composable
fun HiddenChatsBody(
    onBack: () -> Unit,
    onOpenChat: (friendId: String, name: String) -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    var conversations by remember { mutableStateOf<List<ConversationSummary>>(emptyList()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.listHiddenConversations().onSuccess { conversations = it }
    }

    fun refresh() {
        scope.launch { viewModel.listHiddenConversations().onSuccess { conversations = it } }
    }

    Column(modifier = Modifier.fillMaxSize().background(CedalColors.Background)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().border(width = androidx.compose.ui.unit.Dp.Hairline, color = CedalColors.BorderSlate).padding(horizontal = 8.dp, vertical = 8.dp),
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = CedalColors.TextPrimary)
            }
            Text("Hidden Chats", color = CedalColors.TextPrimary, fontSize = 16.sp, modifier = Modifier.weight(1f))
        }

        if (conversations.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(24.dp)) {
                Text("No hidden chats.", color = CedalColors.TextSecondary, fontSize = 13.sp)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(conversations, key = { it.friendId }) { convo ->
                    ArchivedOrHiddenRow(
                        convo = convo,
                        actionLabel = "UNHIDE",
                        onOpen = { onOpenChat(convo.friendId, convo.name) },
                        onAction = {
                            scope.launch {
                                viewModel.bulkChatAction(listOf(convo.friendId), "unhide")
                                refresh()
                            }
                        },
                    )
                    RowDivider()
                }
            }
        }
    }
}

@Composable
private fun ArchivedOrHiddenRow(convo: ConversationSummary, actionLabel: String, onOpen: () -> Unit, onAction: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onOpen)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(CedalColors.BackgroundBlob)
                .border(1.dp, CedalColors.BorderCyan, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            val avatarUrl = convo.avatarUrl
            if (avatarUrl != null) {
                coil.compose.AsyncImage(
                    model = avatarUrl,
                    contentDescription = convo.name,
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                )
            } else {
                Icon(Icons.Outlined.Person, contentDescription = convo.name, tint = CedalColors.AccentCyan, modifier = Modifier.size(20.dp))
            }
        }
        Text(convo.name, color = CedalColors.TextPrimary, fontSize = 14.sp, modifier = Modifier.weight(1f).padding(start = 12.dp))
        Text(
            actionLabel, color = CedalColors.AccentCyan, fontSize = 11.sp,
            modifier = Modifier
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onAction)
                .padding(6.dp),
        )
    }
}
