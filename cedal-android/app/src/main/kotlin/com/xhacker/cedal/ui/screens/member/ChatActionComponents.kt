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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xhacker.cedal.data.FriendSummary
import com.xhacker.cedal.ui.theme.CedalColors
import com.xhacker.cedal.viewmodel.AuthViewModel

// Shared across every chat surface (real friend chat, Corneal, ARC's
// Assistant, Code AI) - generalized from what used to be real chat's own
// private MessageActionsOverlay, since all four now need the same
// forward/copy/pin/report/reply rows, plus edit/delete on your own messages
// and reactions only where that already made sense (real chat).
internal val QUICK_REACTIONS = listOf("👍", "❤️", "😂", "😮", "😢", "🙏")

@Composable
internal fun ChatBubbleActionsOverlay(
    showReactions: Boolean = false,
    canEdit: Boolean,
    canDelete: Boolean,
    onReact: (String) -> Unit = {},
    onReply: () -> Unit,
    onCopy: () -> Unit,
    onForward: () -> Unit,
    onPin: () -> Unit,
    onReport: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .padding(32.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(CedalColors.CardBackground)
                .border(1.dp, CedalColors.BorderCyan, RoundedCornerShape(18.dp))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {}) // absorb taps so they don't dismiss
                .padding(16.dp),
        ) {
            if (showReactions) {
                Row(modifier = Modifier.padding(bottom = 14.dp)) {
                    QUICK_REACTIONS.forEach { emoji ->
                        Text(
                            emoji, fontSize = 22.sp,
                            modifier = Modifier
                                .padding(end = 10.dp)
                                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onReact(emoji) },
                        )
                    }
                }
            }
            ActionMenuRow(label = "Reply", onClick = onReply)
            ActionMenuRow(label = "Copy", onClick = onCopy)
            ActionMenuRow(label = "Forward", onClick = onForward)
            ActionMenuRow(label = "Pin", onClick = onPin)
            if (canEdit) ActionMenuRow(label = "Edit", onClick = onEdit)
            if (canDelete) ActionMenuRow(label = "Delete", color = CedalColors.Error, onClick = onDelete)
            ActionMenuRow(label = "Report", color = CedalColors.Error, onClick = onReport)
        }
    }
}

@Composable
internal fun ActionMenuRow(label: String, color: Color = CedalColors.TextPrimary, onClick: () -> Unit) {
    Text(
        label,
        color = color, fontSize = 14.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(vertical = 10.dp),
    )
}

// The "forward to..." picker - your friends plus the 3 AI assistants, each
// a plain tappable row. Picking one sends the forwarded text there via that
// target's own existing send method (the caller supplies onPick), same as
// if the user had typed it themselves.
@Composable
internal fun ForwardMessageSheet(
    onPick: (targetLabel: String, targetKey: String) -> Unit,
    onDismiss: () -> Unit,
    viewModel: AuthViewModel,
) {
    var friends by remember { mutableStateOf<List<FriendSummary>?>(null) }
    LaunchedEffect(Unit) {
        viewModel.listFriends().onSuccess { friends = it }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .padding(32.dp)
                .heightIn(max = 420.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(CedalColors.CardBackground)
                .border(1.dp, CedalColors.BorderCyan, RoundedCornerShape(18.dp))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {})
                .padding(16.dp),
        ) {
            Text("FORWARD TO", color = CedalColors.TextSecondary, fontSize = 11.sp, letterSpacing = 2.sp, modifier = Modifier.padding(bottom = 10.dp))
            LazyColumn {
                items(listOf("Corneal" to "corneal", "ARC's Assistant" to "arc", "Code AI" to "code")) { (label, key) ->
                    ForwardTargetRow(label) { onPick(label, key) }
                }
                val currentFriends = friends
                if (currentFriends == null) {
                    item {
                        Row(modifier = Modifier.padding(vertical = 12.dp)) {
                            CircularProgressIndicator(color = CedalColors.AccentCyan, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                        }
                    }
                } else {
                    items(currentFriends, key = { it.id }) { friend ->
                        ForwardTargetRow(friend.name) { onPick(friend.name, "friend:${friend.id}") }
                    }
                }
            }
        }
    }
}

@Composable
private fun ForwardTargetRow(label: String, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(vertical = 10.dp),
    ) {
        Box(
            modifier = Modifier.size(30.dp).clip(CircleShape).background(CedalColors.BackgroundBlob).border(1.dp, CedalColors.BorderCyan, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(label.take(1).uppercase(), color = CedalColors.AccentCyan, fontSize = 12.sp)
        }
        Text(label, color = CedalColors.TextPrimary, fontSize = 14.sp, modifier = Modifier.padding(start = 12.dp))
    }
}
