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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xhacker.cedal.ui.theme.CedalColors

enum class WorkMode { BANK, INVEST }

// Ported from cedal-mobile's src/member/work/WorkPicker.tsx — originally 4
// modes, but Code and ARC graduated to their own top-level bottom tabs (see
// MemberScaffold's MemberTab.CODE/ARC) instead of living behind this picker,
// so Base is just Bank/Invest now.
@Composable
fun WorkPickerBody(onSelectMode: (WorkMode) -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(CedalColors.Background).verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("BASE MODE SELECTOR", color = CedalColors.TextSecondary, fontSize = 11.sp, letterSpacing = 2.sp)
        Text(
            "Pick how you want to grind.",
            color = CedalColors.TextPrimary, fontSize = 18.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 4.dp),
        )
        Text(
            "Bank and Invest are different shells for the same session.",
            color = CedalColors.TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(bottom = 18.dp),
        )

        WorkModeCard(
            title = "Bank", badge = "FOCUS BUDGET", pill = "SAFE · STRUCTURED",
            description = "Treat your time like currency. Allocate focus, protect energy, and track where every hour goes.",
            icon = Icons.Outlined.AccountBalance, accent = CedalColors.Success,
            onClick = { onSelectMode(WorkMode.BANK) },
        )
        WorkModeCard(
            title = "Invest", badge = "LONG GAME", pill = "GROWTH · STACKING",
            description = "Aim at long-term gains. Skills, projects, and habits that pay off over months, not minutes.",
            icon = Icons.Outlined.TrendingUp, accent = CedalColors.AccentIndigo,
            isLast = true,
            onClick = { onSelectMode(WorkMode.INVEST) },
        )
    }
}

@Composable
private fun WorkModeCard(
    title: String,
    badge: String,
    pill: String,
    description: String,
    icon: ImageVector,
    accent: Color,
    isLast: Boolean = false,
    showUnseenDot: Boolean = false,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = if (isLast) 0.dp else 12.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(accent.copy(alpha = 0.06f))
            .border(1.dp, accent.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(16.dp),
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Box {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(accent.copy(alpha = 0.14f))
                            .border(1.dp, accent, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(icon, contentDescription = title, tint = accent, modifier = Modifier.size(20.dp))
                    }
                    if (showUnseenDot) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(CedalColors.Error)
                                .border(1.5.dp, CedalColors.Background, CircleShape),
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(title, color = CedalColors.TextPrimary, fontSize = 16.sp)
                    Box(
                        modifier = Modifier
                            .padding(top = 3.dp)
                            .clip(RoundedCornerShape(50))
                            .background(CedalColors.BackgroundBlob)
                            .border(1.dp, CedalColors.BorderSlate, RoundedCornerShape(50))
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    ) {
                        Text(badge, color = CedalColors.TextMuted, fontSize = 9.sp, letterSpacing = 1.sp)
                    }
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .border(1.dp, accent.copy(alpha = 0.7f), RoundedCornerShape(50))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                ) {
                    Text(pill, color = accent, fontSize = 9.sp, letterSpacing = 0.5.sp)
                }
            }
            Text(
                description,
                color = CedalColors.TextSecondary, fontSize = 12.sp,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
    }
}
