package com.xhacker.cedal.ui.screens.member

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xhacker.cedal.ui.theme.CedalColors
import com.xhacker.cedal.ui.theme.CedalToggle

// Matches cedal-mobile's src/member/settings/SettingsRow.tsx — a label,
// one-line description, and a toggle. Used across the Chat/Groups/
// Navigation/Call settings sections.
@Composable
fun SettingsToggleRow(label: String, description: String, value: Boolean, onValueChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
            Text(label, color = CedalColors.TextPrimary, fontSize = 13.sp)
            Text(description, color = CedalColors.TextSecondary, fontSize = 11.sp)
        }
        CedalToggle(checked = value, onCheckedChange = onValueChange)
    }
}
