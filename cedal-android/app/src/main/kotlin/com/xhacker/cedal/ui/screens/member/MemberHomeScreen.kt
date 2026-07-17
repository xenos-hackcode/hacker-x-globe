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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xhacker.cedal.data.ConversationSummary
import com.xhacker.cedal.ui.theme.CedalColors
import kotlinx.coroutines.delay
import com.xhacker.cedal.viewmodel.AuthViewModel

@Composable
fun ChatsListBody(
    searchQuery: String = "",
    viewModel: AuthViewModel,
    onOpenChat: (friendId: String, name: String) -> Unit,
    onOpenSystemFeed: () -> Unit = {},
) {
    var conversations by remember { mutableStateOf<List<ConversationSummary>>(emptyList()) }

    // Polls rather than a one-shot fetch - a friend accepted while this tab
    // is already open (or was accepted moments ago from the requests side)
    // should still show up here without needing to navigate away and back.
    LaunchedEffect(Unit) {
        while (true) {
            viewModel.listConversations().onSuccess { conversations = it }
            delay(15_000)
        }
    }

    // Already sorted newest-first server-side, System Feed included as just
    // another row that sorts by its own latest post (see
    // ChatService.listConversations) - no more separate pinned section.
    val visible = conversations.filter { searchQuery.isBlank() || it.name.contains(searchQuery, ignoreCase = true) }

    if (visible.isEmpty()) {
        Column(modifier = Modifier.fillMaxSize().background(CedalColors.Background).padding(24.dp)) {
            Text(
                if (searchQuery.isBlank()) "No chats yet." else "No chats match \"$searchQuery\".",
                color = CedalColors.TextSecondary, fontSize = 13.sp,
            )
        }
        return
    }

    LazyColumn(modifier = Modifier.fillMaxSize().background(CedalColors.Background)) {
        items(visible, key = { it.friendId }) { convo ->
            ChatRow(convo) {
                if (convo.isSystemFeed) onOpenSystemFeed() else onOpenChat(convo.friendId, convo.name)
            }
            RowDivider()
        }
    }
}

@Composable
private fun RowDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 66.dp)
            .border(width = Dp.Hairline, color = CedalColors.BorderSlate),
    )
}

// Renders both real friend conversations and the synthetic "Cedal System
// Feed" row (convo.isSystemFeed) with the same layout - they're sorted and
// listed together now (see ChatsListBody), not a separate pinned section.
@Composable
private fun ChatRow(convo: ConversationSummary, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(CedalColors.BackgroundBlob)
                .border(1.dp, CedalColors.BorderCyan, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (convo.isSystemFeed) Icons.Outlined.Public else Icons.Outlined.Person,
                contentDescription = convo.name, tint = CedalColors.AccentCyan, modifier = Modifier.size(22.dp),
            )
        }

        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
            Text(convo.name, color = CedalColors.TextPrimary, fontSize = 15.sp, fontWeight = MaterialTheme.typography.titleSmall.fontWeight)
            Text(
                when {
                    convo.isSystemFeed -> convo.lastMessage ?: "System announcements land here"
                    convo.lastMessageViewOnce -> if (convo.lastMessageFromMe == true) "You sent a view once" else "You received a view once"
                    else -> convo.lastMessage?.let { if (convo.lastMessageFromMe == true) "You: $it" else it } ?: "Say hi 👋"
                },
                color = CedalColors.TextSecondary, fontSize = 13.sp, maxLines = 1,
            )
        }

        // Same "latest message + when" convention as every other chat app -
        // the list is already sorted newest-conversation-first server-side
        // (see ChatService.listConversations), this is just showing it.
        Column(horizontalAlignment = Alignment.End) {
            convo.lastMessageAt?.let {
                Text(formatMessageTime(it), color = CedalColors.TextMuted, fontSize = 11.sp)
            }
            if (convo.unreadCount > 0) {
                Box(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .clip(CircleShape)
                        // System Feed's badge is red (unread announcements),
                        // distinct from friends' cyan unread badge.
                        .background(if (convo.isSystemFeed) CedalColors.Error else CedalColors.AccentCyan)
                        .padding(horizontal = 7.dp, vertical = 2.dp),
                ) {
                    Text(
                        if (convo.unreadCount > 99) "99+" else convo.unreadCount.toString(),
                        color = CedalColors.Background, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}
