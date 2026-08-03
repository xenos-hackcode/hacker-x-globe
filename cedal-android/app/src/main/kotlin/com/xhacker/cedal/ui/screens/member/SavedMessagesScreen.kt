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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.xhacker.cedal.data.SavedMessageDto
import com.xhacker.cedal.ui.theme.CedalColors
import com.xhacker.cedal.ui.theme.CedalErrorText
import com.xhacker.cedal.viewmodel.AuthViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// "Saved Messages" - a personal conversation-with-yourself, reached from the
// Chats list's overflow menu (see MemberScaffold.kt/NavGraph.kt), same
// discoverability tier as Archived/Hidden Chats. See SavedMessagesService's
// own doc comment server-side for why this is a dedicated table/screen
// instead of reusing ChatService.
@Composable
fun SavedMessagesBody(onBack: () -> Unit, viewModel: AuthViewModel = hiltViewModel()) {
    var messages by remember { mutableStateOf<List<SavedMessageDto>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun refresh() {
        scope.launch { viewModel.listSavedMessages().onSuccess { messages = it }.onFailure { error = it.message } }
    }

    LaunchedEffect(Unit) { refresh() }

    Column(modifier = Modifier.fillMaxSize().background(CedalColors.Background)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().border(width = androidx.compose.ui.unit.Dp.Hairline, color = CedalColors.BorderSlate).padding(horizontal = 8.dp, vertical = 8.dp),
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = CedalColors.TextPrimary)
            }
            Text("Saved Messages", color = CedalColors.TextPrimary, fontSize = 16.sp, modifier = Modifier.weight(1f))
        }
        CedalErrorText(error)

        if (messages.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(24.dp)) {
                Text("Nothing saved yet - long-press a group message and tap Save.", color = CedalColors.TextSecondary, fontSize = 13.sp)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
                items(messages, key = { it.id }) { msg ->
                    SavedMessageRow(msg, onDelete = { scope.launch { viewModel.deleteSavedMessage(msg.id); refresh() } })
                }
            }
        }
    }
}

@Composable
private fun SavedMessageRow(msg: SavedMessageDto, onDelete: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(CedalColors.CardBackground)
            .border(1.dp, CedalColors.BorderSlate, RoundedCornerShape(12.dp))
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                msg.sourceLabel ?: "Saved", color = CedalColors.AccentCyan, fontSize = 11.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            Text(
                SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(msg.savedAt)),
                color = CedalColors.TextMuted, fontSize = 10.sp,
            )
            Text(
                "DELETE", color = CedalColors.Error, fontSize = 10.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                modifier = Modifier.padding(start = 10.dp).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDelete),
            )
        }
        val mediaUrl = msg.mediaUrl
        if (mediaUrl != null && msg.mediaType == "image") {
            AsyncImage(
                model = mediaUrl, contentDescription = null, contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp).clip(RoundedCornerShape(8.dp)),
            )
        } else if (mediaUrl != null) {
            Text(msg.fileName ?: "Attachment", color = CedalColors.TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
        }
        if (msg.text.isNotBlank()) {
            Text(msg.text, color = CedalColors.TextPrimary, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
        }
    }
}
