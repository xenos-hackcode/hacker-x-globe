package com.xhacker.cedal.ui.screens.member

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Star
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

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun ChatsListBody(
    searchQuery: String = "",
    viewModel: AuthViewModel,
    onOpenChat: (friendId: String, name: String) -> Unit,
    onOpenSystemFeed: () -> Unit = {},
    onOpenGroup: (groupId: String, name: String) -> Unit = { _, _ -> },
    selection: ChatSelectionState = rememberChatSelectionState(),
) {
    var conversations by remember { mutableStateOf<List<ConversationSummary>>(emptyList()) }
    var typingFriendIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var pendingLockedOpen by remember { mutableStateOf<ConversationSummary?>(null) }

    // Polls rather than a one-shot fetch - a friend accepted while this tab
    // is already open (or was accepted moments ago from the requests side)
    // should still show up here without needing to navigate away and back.
    LaunchedEffect(Unit) {
        while (true) {
            viewModel.listConversations().onSuccess { conversations = it }
            typingFriendIds = if (viewModel.storage.typingIndicatorsEnabled) {
                viewModel.getTypingFriendIds().getOrNull()?.toSet() ?: emptySet()
            } else {
                emptySet()
            }
            delay(3_000)
        }
    }
    // The header reads this to know what's on screen (Select All / filters).
    LaunchedEffect(conversations) { selection.allSelectable = conversations.filterNot { it.isSystemFeed } }

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

    // Archived is reached via the ⋮ menu's "Archive" item now (see
    // MemberHeader in MemberScaffold.kt), not a scroll-reveal row here.
    LazyColumn(modifier = Modifier.fillMaxSize().background(CedalColors.Background)) {
        items(visible, key = { it.friendId }) { convo ->
            ChatRow(
                convo,
                isTyping = convo.friendId in typingFriendIds,
                selectionMode = selection.active,
                selected = convo.friendId in selection.selectedIds,
                onLongPress = { if (!convo.isSystemFeed) selection.beginWith(convo.friendId) },
                onClick = {
                    when {
                        convo.isSystemFeed && !selection.active -> onOpenSystemFeed()
                        selection.active -> selection.toggle(convo.friendId)
                        convo.locked -> pendingLockedOpen = convo
                        convo.isGroup -> onOpenGroup(convo.friendId, convo.name)
                        else -> onOpenChat(convo.friendId, convo.name)
                    }
                },
            )
            RowDivider()
        }
    }

    // Locked chats (see ConversationState.locked) need the same
    // biometric/passcode gate as switching accounts before they'll open -
    // reuses AccountVerifyOverlay from MemberSwitchAccountScreen.kt.
    val locked = pendingLockedOpen
    if (locked != null) {
        AccountVerifyOverlay(
            viewModel = viewModel,
            message = "This chat is locked. Verify it's you to open it.",
            onVerified = { pendingLockedOpen = null; onOpenChat(locked.friendId, locked.name) },
            onCancel = { pendingLockedOpen = null },
        )
    }
}

// Drives the chat-list long-press multi-select flow: entering/exiting
// selection mode, which friendIds are picked, and the filter-icon's
// Group/Chat/Custom-Time bulk-select. Lives above ChatsListBody (owned by
// MemberHomeRoute) so the header row (Select All, filter, ⋮ bulk menu) and
// the list itself share one source of truth.
class ChatSelectionState {
    var active by mutableStateOf(false)
    var selectedIds by mutableStateOf<Set<String>>(emptySet())
    var allSelectable by mutableStateOf<List<ConversationSummary>>(emptyList())

    fun beginWith(friendId: String) {
        active = true
        selectedIds = setOf(friendId)
    }

    fun toggle(friendId: String) {
        selectedIds = if (friendId in selectedIds) selectedIds - friendId else selectedIds + friendId
        if (selectedIds.isEmpty()) active = false
    }

    fun selectAll() {
        selectedIds = allSelectable.map { it.friendId }.toSet()
    }

    fun selectChats() {
        selectedIds = allSelectable.map { it.friendId }.toSet()
    }

    fun selectGroups() {
        selectedIds = allSelectable.filter { it.isGroup }.map { it.friendId }.toSet()
    }

    // Every chat with no activity (sent or received) in the last N days -
    // never having messaged at all counts as stale too.
    fun selectStaleSince(days: Int) {
        val cutoff = System.currentTimeMillis() - days * 24L * 60 * 60 * 1000
        selectedIds = allSelectable.filter { (it.lastMessageAt ?: 0L) < cutoff }.map { it.friendId }.toSet()
    }

    fun clear() {
        active = false
        selectedIds = emptySet()
    }
}

@Composable
fun rememberChatSelectionState(): ChatSelectionState = remember { ChatSelectionState() }

// "typing…" with an animated 1/2/3-dot cycle in place of the last-message
// preview - same idea as WhatsApp's chat list, just text-only rather than
// three literal bouncing dots (keeps this cheap - no Canvas/custom drawing).
@Composable
private fun TypingPreview() {
    val transition = rememberInfiniteTransition(label = "typing")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "dots",
    )
    val dotCount = progress.toInt().coerceIn(0, 3)
    Text(
        "typing" + ".".repeat(dotCount),
        color = CedalColors.AccentCyan, fontSize = 13.sp,
    )
}

@Composable
fun RowDivider() {
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
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun ChatRow(
    convo: ConversationSummary,
    isTyping: Boolean = false,
    selectionMode: Boolean = false,
    selected: Boolean = false,
    onLongPress: () -> Unit = {},
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(if (selected) CedalColors.AccentCyan.copy(alpha = 0.12f) else CedalColors.Background)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
                onLongClick = onLongPress,
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        if (selectionMode) {
            Icon(
                if (selected) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                contentDescription = if (selected) "Selected" else "Not selected",
                tint = if (selected) CedalColors.AccentCyan else CedalColors.TextMuted,
                modifier = Modifier.size(22.dp).padding(end = 10.dp),
            )
        }
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(CedalColors.BackgroundBlob)
                .border(1.dp, CedalColors.BorderCyan, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            // Matches the profile picture actually chosen in Profile (see
            // MemberProfileScreen's AsyncImage) - previously this always
            // showed the generic Person icon regardless of avatarUrl.
            val avatarUrl = convo.avatarUrl
            val memberAvatars = convo.memberAvatarUrls
            if (convo.isGroup && !memberAvatars.isNullOrEmpty()) {
                // Small overlapping cluster of up to 3 other members' own
                // avatars - a group has no single "face" of its own unless
                // the creator sets one (avatarUrl), so this is the fallback
                // most groups will actually show.
                Row {
                    memberAvatars.take(3).forEachIndexed { index, url ->
                        Box(
                            modifier = Modifier
                                .padding(start = if (index == 0) 0.dp else (-10).dp)
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(CedalColors.BackgroundBlob)
                                .border(1.5.dp, CedalColors.Background, CircleShape),
                        ) {
                            coil.compose.AsyncImage(
                                model = url,
                                contentDescription = null,
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                modifier = Modifier.fillMaxSize().clip(CircleShape),
                            )
                        }
                    }
                }
            } else if (convo.isGroup) {
                Icon(Icons.Outlined.Groups, contentDescription = convo.name, tint = CedalColors.AccentCyan, modifier = Modifier.size(22.dp))
            } else if (!convo.isSystemFeed && avatarUrl != null) {
                coil.compose.AsyncImage(
                    model = avatarUrl,
                    contentDescription = convo.name,
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                )
            } else {
                Icon(
                    if (convo.isSystemFeed) Icons.Outlined.Public else Icons.Outlined.Person,
                    contentDescription = convo.name, tint = CedalColors.AccentCyan, modifier = Modifier.size(22.dp),
                )
            }
        }

        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(convo.name, color = CedalColors.TextPrimary, fontSize = 15.sp, fontWeight = MaterialTheme.typography.titleSmall.fontWeight)
                if (convo.pinned) {
                    Icon(Icons.Outlined.PushPin, contentDescription = "Pinned", tint = CedalColors.TextMuted, modifier = Modifier.size(12.dp).padding(start = 6.dp))
                }
                if (convo.locked) {
                    Icon(Icons.Outlined.Lock, contentDescription = "Locked", tint = CedalColors.TextMuted, modifier = Modifier.size(12.dp).padding(start = 6.dp))
                }
                if (convo.muted) {
                    Icon(Icons.Outlined.NotificationsOff, contentDescription = "Muted", tint = CedalColors.TextMuted, modifier = Modifier.size(12.dp).padding(start = 6.dp))
                }
                if (convo.favorite) {
                    Icon(Icons.Outlined.Star, contentDescription = "Favorite", tint = CedalColors.TextMuted, modifier = Modifier.size(12.dp).padding(start = 6.dp))
                }
            }
            if (isTyping) {
                TypingPreview()
            } else {
                Text(
                    when {
                        convo.isSystemFeed -> convo.lastMessage ?: "System announcements land here"
                        convo.lastMessageViewOnce -> if (convo.lastMessageFromMe == true) "You sent a view once" else "You received a view once"
                        else -> convo.lastMessage?.let { if (convo.lastMessageFromMe == true) "You: $it" else it } ?: "Say hi 👋"
                    },
                    color = CedalColors.TextSecondary, fontSize = 13.sp, maxLines = 1,
                )
            }
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
