package com.xhacker.cedal.ui.screens.member

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Person
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xhacker.cedal.data.FriendSummary
import com.xhacker.cedal.ui.theme.CedalColors
import com.xhacker.cedal.viewmodel.AuthViewModel

// The entry point exists (Chats list ⋮ menu → Create Group) and this picks
// a name + members, but there's no group-messaging backend yet - no Groups
// table, no group ChatService, no group thread screen. Creating one here
// just tells you that honestly instead of pretending to succeed.
@Composable
fun CreateGroupBody(onBack: () -> Unit, viewModel: AuthViewModel = hiltViewModel()) {
    var name by remember { mutableStateOf("") }
    var friends by remember { mutableStateOf<List<FriendSummary>>(emptyList()) }
    var selected by remember { mutableStateOf(setOf<String>()) }
    var notice by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.listFriends().onSuccess { friends = it }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).imePadding()) {
        MemberBackBar(title = "Create Group", onBack = onBack)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 16.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(CedalColors.CardBackground)
                .border(1.dp, CedalColors.BorderCyan, RoundedCornerShape(14.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            if (name.isEmpty()) {
                Text("Group name…", color = CedalColors.TextMuted, fontSize = 15.sp)
            }
            BasicTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                textStyle = TextStyle(color = CedalColors.TextPrimary, fontSize = 15.sp),
                cursorBrush = SolidColor(CedalColors.AccentCyan),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Text("MEMBERS", color = CedalColors.TextSecondary, fontSize = 11.sp, letterSpacing = 2.sp, modifier = Modifier.padding(bottom = 8.dp))

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(friends, key = { it.id }) { friend ->
                val isSelected = friend.id in selected
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                            selected = if (isSelected) selected - friend.id else selected + friend.id
                        }
                        .padding(vertical = 10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(CedalColors.BackgroundBlob)
                            .border(1.dp, CedalColors.BorderCyan, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Outlined.Person, contentDescription = null, tint = CedalColors.AccentCyan, modifier = Modifier.size(18.dp))
                    }
                    Text(friend.name, color = CedalColors.TextPrimary, fontSize = 14.sp, modifier = Modifier.padding(start = 12.dp).weight(1f))
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) CedalColors.AccentCyan else CedalColors.Background)
                            .border(1.dp, if (isSelected) CedalColors.AccentCyan else CedalColors.BorderSlate, RoundedCornerShape(6.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isSelected) Icon(Icons.Filled.Check, contentDescription = null, tint = CedalColors.Background, modifier = Modifier.size(14.dp))
                    }
                }
            }
            if (friends.isEmpty()) {
                item { Text("Add some friends first.", color = CedalColors.TextMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 12.dp)) }
            }
        }

        notice?.let { Text(it, color = CedalColors.TextMuted, fontSize = 12.sp, modifier = Modifier.padding(vertical = 8.dp)) }

        val canCreate = name.isNotBlank() && selected.size >= 2
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .clip(RoundedCornerShape(50))
                .background(if (canCreate) CedalColors.AccentCyan else CedalColors.CardBackground)
                .border(1.dp, if (canCreate) CedalColors.AccentCyan else CedalColors.BorderSlate, RoundedCornerShape(50))
                .clickable(enabled = canCreate, interactionSource = remember { MutableInteractionSource() }, indication = null) {
                    notice = "Group messaging isn't built yet - this just saves the idea for now."
                }
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "CREATE (${selected.size} selected)",
                color = if (canCreate) CedalColors.Background else CedalColors.TextMuted,
                fontSize = 13.sp, letterSpacing = 1.sp,
            )
        }
    }
}
