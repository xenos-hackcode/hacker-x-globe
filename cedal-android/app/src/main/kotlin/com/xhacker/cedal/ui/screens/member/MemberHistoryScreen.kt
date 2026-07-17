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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xhacker.cedal.ui.theme.CedalColors

private data class HistoryItem(val kind: String, val title: String, val subtitle: String)

// Mock data, same as cedal-mobile's MOCK_HISTORY — that file is explicitly
// commented "later you'll load this from backend" there too, so this isn't
// a shortcut, it's the same placeholder state as the real app.
private val MOCK_HISTORY = listOf(
    HistoryItem("chats", "Chat with Corneal", "Continued a deep dive thread."),
    HistoryItem("bank", "Bank session", "Focused for 45 minutes."),
    HistoryItem("games", "Bloodstrike link", "Docked a game lobby to a group."),
)

private val FILTERS = listOf(
    "all" to "All", "group" to "Groups", "games" to "Games", "guilds" to "Guilds",
    "chats" to "Chats", "calls" to "Calls", "streams" to "Streams", "bank" to "Bank",
    "invest" to "Invest", "code" to "Code", "arc" to "ARC", "bots" to "Bots",
)

@Composable
fun MemberHistoryBody(onBack: () -> Unit) {
    var activeFilter by remember { mutableStateOf("all") }
    val filtered = if (activeFilter == "all") MOCK_HISTORY else MOCK_HISTORY.filter { it.kind == activeFilter }

    Column(modifier = Modifier.fillMaxSize().background(CedalColors.Background).padding(16.dp)) {
        MemberBackBar(title = "History", onBack = onBack)

        // A single compact dropdown instead of a scrolling chip row — the
        // filter picker itself never needs to scroll.
        FilterDropdown(
            selectedLabel = FILTERS.first { it.first == activeFilter }.second,
            onSelect = { activeFilter = it },
            modifier = Modifier.padding(bottom = 12.dp),
        )

        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            if (filtered.isEmpty()) {
                Text("Nothing yet.", color = CedalColors.TextPrimary, fontSize = 15.sp, modifier = Modifier.padding(top = 24.dp))
                Text(
                    "When you chat, grind, play, or call, your activity will show up here.",
                    color = CedalColors.TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp),
                )
            } else {
                SettingsSectionCard("Recent") {
                    filtered.forEach { item ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp)) {
                            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(CedalColors.AccentCyan))
                            Column(modifier = Modifier.padding(start = 10.dp)) {
                                Text(item.title, color = CedalColors.TextPrimary, fontSize = 13.sp)
                                Text(item.subtitle, color = CedalColors.TextSecondary, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterDropdown(selectedLabel: String, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(CedalColors.Background)
                .border(1.dp, CedalColors.BorderSlate, RoundedCornerShape(50))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { open = true }
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Filter: ", color = CedalColors.TextSecondary, fontSize = 12.sp)
                Text(selectedLabel, color = CedalColors.TextPrimary, fontSize = 12.sp)
                Text(" ▾", color = CedalColors.TextSecondary, fontSize = 10.sp)
            }
        }
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(CedalColors.CardBackground)
                .border(1.dp, CedalColors.BorderCyan, RoundedCornerShape(16.dp)),
        ) {
            FILTERS.forEach { (key, label) ->
                DropdownMenuItem(
                    text = { Text(label, color = CedalColors.TextPrimary, fontSize = 13.sp) },
                    onClick = { open = false; onSelect(key) },
                )
            }
        }
    }
}
