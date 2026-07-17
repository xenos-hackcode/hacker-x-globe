package com.xhacker.cedal.ui.screens.member

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xhacker.cedal.data.DailyTaskResponse
import com.xhacker.cedal.ui.theme.CedalColors
import com.xhacker.cedal.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

// "Today's Task" for Invest and ARC's Learn tabs - a fresh task per day,
// generated server-side (see DailyTaskService) and cached for the rest of
// the day. Deliberately not labeled as AI-generated anywhere here - it's
// presented exactly like any other piece of built-in content, same as the
// hand-written lessons around it.
@Composable
fun DailyTaskCard(area: String, viewModel: AuthViewModel) {
    var task by remember(area) { mutableStateOf<DailyTaskResponse?>(null) }
    var completing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(area) {
        viewModel.dailyTask(area).onSuccess { task = it }
    }

    val current = task ?: return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 14.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(CedalColors.CardBackground)
            .border(1.dp, if (current.completed) CedalColors.Success.copy(alpha = 0.5f) else CedalColors.BorderCyan, RoundedCornerShape(14.dp))
            .padding(14.dp),
    ) {
        Text("TODAY'S TASK", color = CedalColors.AccentCyan, fontSize = 10.sp, letterSpacing = 1.sp, modifier = Modifier.padding(bottom = 6.dp))
        Text(current.title, color = CedalColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text(current.description, color = CedalColors.TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp, bottom = 10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(CedalColors.Background)
                    .border(1.dp, CedalColors.BorderSlate, RoundedCornerShape(50))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            ) {
                Text("+${current.expReward} EXP", color = CedalColors.TextMuted, fontSize = 9.sp, letterSpacing = 0.4.sp)
            }
            if (current.completed) {
                Text("Done for today ✓", color = CedalColors.Success, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            } else {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(CedalColors.AccentCyan.copy(alpha = if (completing) 0.4f else 1f))
                        .clickable(enabled = !completing, interactionSource = remember { MutableInteractionSource() }, indication = null) {
                            completing = true
                            scope.launch {
                                viewModel.completeDailyTask(area)
                                task = current.copy(completed = true)
                                completing = false
                            }
                        }
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                ) {
                    Text(if (completing) "Marking…" else "Mark Done", color = CedalColors.Background, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
