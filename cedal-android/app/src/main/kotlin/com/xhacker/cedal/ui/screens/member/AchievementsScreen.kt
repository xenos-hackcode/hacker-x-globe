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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xhacker.cedal.data.AchievementDto
import com.xhacker.cedal.ui.theme.CedalColors
import com.xhacker.cedal.viewmodel.AuthViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Chat list > More > Achievements - shows the WHOLE catalog (see
// AchievementService.listAll), locked ones dimmed/no-glow, unlocked ones
// glowing with a "USE" tap that equips it as a small badge next to the name
// on Profile (see AchievementService.setActiveBadge). Achievements
// themselves only ever surface as the in-app BalloonPopupOverlay toast at
// unlock time; this screen is the permanent record.
@Composable
fun AchievementsBody(onBack: () -> Unit, viewModel: AuthViewModel = hiltViewModel()) {
    var achievements by remember { mutableStateOf<List<AchievementDto>>(emptyList()) }
    var activeBadgeKey by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun refresh() {
        scope.launch {
            viewModel.listAchievements().onSuccess { achievements = it }
            viewModel.getProfile().onSuccess { activeBadgeKey = it.activeBadgeKey }
        }
    }
    LaunchedEffect(Unit) { refresh() }

    val unlockedCount = achievements.count { it.unlocked }

    Column(modifier = Modifier.fillMaxSize().background(CedalColors.Background)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().border(width = Dp.Hairline, color = CedalColors.BorderSlate).padding(horizontal = 8.dp, vertical = 8.dp),
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = CedalColors.TextPrimary)
            }
            Text("Achievements", color = CedalColors.TextPrimary, fontSize = 16.sp, modifier = Modifier.weight(1f))
            Text("$unlockedCount/${achievements.size} unlocked", color = CedalColors.TextMuted, fontSize = 11.sp)
        }

        if (achievements.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Loading…", color = CedalColors.TextSecondary, fontSize = 13.sp)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                items(achievements, key = { it.key }) { achievement ->
                    AchievementRow(
                        achievement = achievement,
                        inUse = achievement.key == activeBadgeKey,
                        onUse = {
                            val nextKey = if (achievement.key == activeBadgeKey) null else achievement.key
                            scope.launch { viewModel.setActiveBadge(nextKey).onSuccess { activeBadgeKey = it.activeBadgeKey } }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun AchievementRow(achievement: AchievementDto, inUse: Boolean, onUse: () -> Unit) {
    val fmt = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
    val unlocked = achievement.unlocked
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(CedalColors.CardBackground)
            .border(1.dp, if (unlocked) CedalColors.AccentCyan else CedalColors.BorderSlate, RoundedCornerShape(14.dp))
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(if (unlocked) "🏆" else "🔒", fontSize = 22.sp)
            Column(modifier = Modifier.padding(start = 10.dp).weight(1f)) {
                Text(
                    achievement.title,
                    color = if (unlocked) CedalColors.TextPrimary else CedalColors.TextMuted,
                    fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                )
                Text(
                    achievement.bigWord.uppercase(),
                    color = if (unlocked) CedalColors.AccentCyan else CedalColors.TextMuted,
                    fontSize = 11.sp, fontWeight = FontWeight.Bold,
                )
            }
            achievement.unlockedAt?.let { Text(fmt.format(Date(it)), color = CedalColors.TextMuted, fontSize = 10.sp) }
        }
        Text(
            achievement.body,
            color = if (unlocked) CedalColors.TextSecondary else CedalColors.TextMuted,
            fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp),
        )
        if (unlocked) {
            Text(
                if (inUse) "IN USE" else "USE",
                color = if (inUse) CedalColors.Success else CedalColors.AccentCyan,
                fontSize = 11.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onUse),
            )
        }
    }
}
