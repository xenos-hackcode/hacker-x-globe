package com.xhacker.cedal.ui.screens.member

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xhacker.cedal.ui.theme.CedalColors

// The "Back" pill + uppercase title header used by cedal-mobile's standalone
// member screens (ProfileHeader, settings.tsx's topBar, ...) — shared here
// so Profile/Settings/etc. all match it consistently.
@Composable
fun MemberBackBar(title: String, onBack: () -> Unit, busy: Boolean = false, busyLabel: String = "SAVING…") {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .border(1.dp, CedalColors.BorderSlate, RoundedCornerShape(50))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, enabled = !busy, onClick = onBack)
                .padding(horizontal = 10.dp, vertical = 6.dp),
        ) {
            Text(if (busy) busyLabel else "BACK", color = CedalColors.TextPrimary, fontSize = 13.sp, letterSpacing = 1.5.sp)
        }
        Text(
            title.uppercase(),
            color = CedalColors.TextSecondary,
            fontSize = 10.sp,
            letterSpacing = 2.sp,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}
