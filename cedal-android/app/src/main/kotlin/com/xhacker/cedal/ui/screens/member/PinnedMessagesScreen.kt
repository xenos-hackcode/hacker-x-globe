package com.xhacker.cedal.ui.screens.member

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xhacker.cedal.data.MessagePinDto
import com.xhacker.cedal.ui.theme.CedalColors
import com.xhacker.cedal.viewmodel.AuthViewModel
import kotlinx.coroutines.launch
import java.net.URLEncoder

// friendNames resolves a "friend" pin's chatKey (the friend's user id) to
// their real display name, instead of the generic placeholder "Chat" - a
// friend who's since been removed just falls back to that placeholder.
private fun chatLabelFor(pin: MessagePinDto, friendNames: Map<String, String>): String = when (pin.chatType) {
    "friend" -> pin.chatKey?.let { friendNames[it] } ?: "Chat"
    "corneal" -> "Corneal"
    "arc" -> "ARC's Assistant"
    "code" -> "Code AI"
    else -> pin.chatType
}

// Reachable from the profile "⋮" menu - every pinned message across every
// chat surface (real friend chats + all 3 AI assistants), rendered like a
// normal chat bubble (sender + time + text, see MessagePins.messageText -
// a snapshot at pin time, same reasoning as MessageReports). Single tap
// selects/highlights an entry (revealing Unpin); double tap jumps to that
// message in its real chat and highlights it there via the shared
// highlightId nav pattern (see NavGraph.kt / formatMessageTime callers).
// The search bar filters by chat name ("corneal" surfaces every pin from
// that chat) or by the pinned text itself.
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PinnedMessagesBody(onBack: () -> Unit, onNavigate: (String) -> Unit, viewModel: AuthViewModel = hiltViewModel()) {
    val scope = rememberCoroutineScope()
    var pins by remember { mutableStateOf<List<MessagePinDto>>(emptyList()) }
    var friendNames by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var loading by remember { mutableStateOf(true) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.listPinnedMessages().onSuccess { pins = it }
        viewModel.listFriends().onSuccess { friends -> friendNames = friends.associate { it.id to it.name } }
        loading = false
    }

    fun unpin(pin: MessagePinDto) {
        scope.launch {
            viewModel.unpinMessage(pin.id).onSuccess { pins = pins.filterNot { it.id == pin.id } }
        }
        selectedId = null
    }

    fun navigateToSource(pin: MessagePinDto) {
        val highlight = URLEncoder.encode(pin.messageId, "UTF-8")
        when (pin.chatType) {
            "friend" -> {
                val name = URLEncoder.encode(chatLabelFor(pin, friendNames), "UTF-8")
                onNavigate("member_chat/${pin.chatKey}?name=$name&highlightId=$highlight")
            }
            "corneal" -> onNavigate("member_corneal_chat?highlightId=$highlight")
            "arc" -> onNavigate("member_home?tab=ARC&highlightId=$highlight")
            "code" -> onNavigate("member_home?tab=CODE&highlightId=$highlight")
        }
    }

    val visiblePins = pins.filter { pin ->
        query.isBlank() ||
            chatLabelFor(pin, friendNames).contains(query, ignoreCase = true) ||
            pin.messageText.contains(query, ignoreCase = true)
    }

    Column(modifier = Modifier.fillMaxSize().background(CedalColors.Background).padding(16.dp).imePadding()) {
        MemberBackBar(title = "Pinned Messages", onBack = onBack)

        if (pins.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(CedalColors.CardBackground)
                    .border(1.dp, CedalColors.BorderSlate, RoundedCornerShape(50))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                if (query.isEmpty()) {
                    Text("Search by chat (e.g. Corneal) or text…", color = CedalColors.TextMuted, fontSize = 13.sp)
                }
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    textStyle = TextStyle(color = CedalColors.TextPrimary, fontSize = 13.sp),
                    cursorBrush = SolidColor(CedalColors.AccentCyan),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        if (!loading && pins.isEmpty()) {
            Text("Nothing pinned yet - long-press or tap ⋮ on any message to pin it.", color = CedalColors.TextMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 16.dp))
        } else if (!loading && visiblePins.isEmpty()) {
            Text("No pinned messages match \"$query\".", color = CedalColors.TextMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 16.dp))
        }
        LazyColumn(modifier = Modifier.weight(1f).padding(top = 8.dp)) {
            items(visiblePins, key = { it.id }) { pin ->
                val isSelected = pin.id == selectedId
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isSelected) CedalColors.AccentCyan.copy(alpha = 0.15f) else CedalColors.CardBackground)
                        .border(1.dp, if (isSelected) CedalColors.AccentCyan else CedalColors.BorderSlate, RoundedCornerShape(14.dp))
                        .combinedClickable(
                            interactionSource = remember { MutableInteractionSource() }, indication = null,
                            onClick = { selectedId = if (isSelected) null else pin.id },
                            onDoubleClick = { navigateToSource(pin) },
                        )
                        .padding(14.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
                        Text(chatLabelFor(pin, friendNames).uppercase(), color = CedalColors.AccentCyan, fontSize = 10.sp, letterSpacing = 1.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text(formatMessageTime(pin.pinnedAt), color = CedalColors.TextMuted, fontSize = 10.sp)
                    }
                    Text(pin.messageText, color = CedalColors.TextPrimary, fontSize = 14.sp, lineHeight = 19.sp)
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .padding(top = 10.dp)
                                .clip(RoundedCornerShape(50))
                                .background(CedalColors.Error.copy(alpha = 0.15f))
                                .border(1.dp, CedalColors.Error, RoundedCornerShape(50))
                                .combinedClickable(
                                    interactionSource = remember { MutableInteractionSource() }, indication = null,
                                    onClick = { unpin(pin) }, onDoubleClick = {},
                                )
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                        ) {
                            Text("UNPIN", color = CedalColors.Error, fontSize = 11.sp, letterSpacing = 1.sp)
                        }
                    }
                }
            }
        }
    }
}
