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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xhacker.cedal.ui.theme.CedalColors
import com.xhacker.cedal.viewmodel.AuthViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class HistoryItem(val lane: String, val title: String, val subtitle: String, val at: Long)

// Real activity now, merged from the three AI assistants' own real,
// server-backed history (AiChangeRequestService/AiChatHistoryService) -
// this used to be MOCK_HISTORY, a hardcoded 3-item placeholder with a
// 12-option filter that mostly filtered nothing (9 of 12 options matched
// zero mock items). Filters are now just what actually has real data
// behind them: All/Corneal/ARC/Code.
private val FILTERS = listOf("all" to "All", "corneal" to "Corneal", "arc" to "ARC", "code" to "Code")

@Composable
fun MemberHistoryBody(onBack: () -> Unit, viewModel: AuthViewModel = hiltViewModel()) {
    var activeFilter by remember { mutableStateOf("all") }
    var items by remember { mutableStateOf<List<HistoryItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val merged = mutableListOf<HistoryItem>()

        viewModel.getAiRequestHistory().onSuccess { requests ->
            requests.filter { !it.requestDeleted }.forEach { req ->
                val title = when {
                    req.status == "deployed" && !req.summary.isNullOrBlank() -> "Coder added the ${req.summary} language"
                    req.status == "pending_approval" && !req.summary.isNullOrBlank() -> "Coder proposed adding ${req.summary}"
                    req.fileActionExecuted -> "Coder made a file change"
                    !req.answerText.isNullOrBlank() -> "Coder replied"
                    else -> null
                }
                if (title != null) {
                    merged.add(HistoryItem("code", title, (req.answerText ?: req.summary ?: "").take(90), req.createdAt))
                }
            }
        }
        viewModel.getCornealChatHistory().onSuccess { turns ->
            turns.filter { it.role == "assistant" && !it.deleted && it.content.isNotBlank() }.forEach { turn ->
                merged.add(HistoryItem("corneal", "Corneal replied", turn.content.take(90), turn.createdAt))
            }
        }
        viewModel.getArcChatHistory().onSuccess { turns ->
            turns.filter { it.role == "assistant" && !it.deleted && it.content.isNotBlank() }.forEach { turn ->
                merged.add(HistoryItem("arc", "ARC replied", turn.content.take(90), turn.createdAt))
            }
        }

        items = merged.sortedByDescending { it.at }
        loading = false
    }

    val filtered = if (activeFilter == "all") items else items.filter { it.lane == activeFilter }

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
            when {
                loading -> Text("Loading…", color = CedalColors.TextSecondary, fontSize = 13.sp, modifier = Modifier.padding(top = 24.dp))
                filtered.isEmpty() -> {
                    Text("Nothing yet.", color = CedalColors.TextPrimary, fontSize = 15.sp, modifier = Modifier.padding(top = 24.dp))
                    Text(
                        "Once Corneal, ARC, or Coder do something for you, it'll show up here.",
                        color = CedalColors.TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp),
                    )
                }
                else -> {
                    SettingsSectionCard("Recent") {
                        filtered.forEach { item ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp)) {
                                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(CedalColors.AccentCyan))
                                Column(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
                                    Text(item.title, color = CedalColors.TextPrimary, fontSize = 13.sp)
                                    if (item.subtitle.isNotBlank()) {
                                        Text(item.subtitle, color = CedalColors.TextSecondary, fontSize = 11.sp)
                                    }
                                }
                                Text(formatHistoryTime(item.at), color = CedalColors.TextMuted, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatHistoryTime(epochMs: Long): String =
    SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(epochMs))

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
