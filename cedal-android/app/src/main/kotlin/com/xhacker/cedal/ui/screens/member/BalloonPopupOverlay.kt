package com.xhacker.cedal.ui.screens.member

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xhacker.cedal.data.PendingPopupDto
import com.xhacker.cedal.ui.theme.CedalColors
import com.xhacker.cedal.viewmodel.AuthViewModel
import kotlinx.coroutines.delay

// Global achievement/rank-up popup poller - lives once at the top of
// MemberRoute (see NavGraph/MemberScaffold.kt) so it fires no matter which
// tab/screen the user is on. Achievements and rank-ups show ONLY as this
// in-app popup, never a system push notification, per spec. Each poll also
// marks the returned rows delivered server-side (see
// PendingPopupService.pollPending), so nothing repeats, and anything queued
// while the user was offline still surfaces the next time they're online.
private const val POLL_INTERVAL_MS = 20_000L
private const val POPUP_VISIBLE_MS = 10_000L

@Composable
fun BalloonPopupOverlay(viewModel: AuthViewModel = hiltViewModel()) {
    var queue by remember { mutableStateOf<List<PendingPopupDto>>(emptyList()) }
    var current by remember { mutableStateOf<PendingPopupDto?>(null) }

    LaunchedEffect(Unit) {
        while (true) {
            viewModel.pollPendingPopups().onSuccess { popups ->
                if (popups.isNotEmpty()) queue = queue + popups
            }
            // Live in-session ban/clear-data detection - piggybacks this
            // same poll cycle rather than a separate loop. See
            // AccountGateState/CedalNavGraph's overlay for what happens
            // once this trips.
            viewModel.getAccountStatus().onSuccess { status ->
                if (status.gated) {
                    com.xhacker.cedal.ui.AccountGateState.active = true
                    com.xhacker.cedal.ui.AccountGateState.permanent = status.permanent
                    com.xhacker.cedal.ui.AccountGateState.bannedAt = status.bannedAt
                }
            }
            delay(POLL_INTERVAL_MS)
        }
    }

    LaunchedEffect(queue, current) {
        if (current == null && queue.isNotEmpty()) {
            current = queue.first()
            queue = queue.drop(1)
        }
    }

    val popup = current
    AnimatedVisibility(
        visible = popup != null,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
    ) {
        if (popup != null) {
            LaunchedEffect(popup.id) {
                delay(POPUP_VISIBLE_MS)
                if (current?.id == popup.id) current = null
            }
            BalloonPopupCard(popup, onDismiss = { if (current?.id == popup.id) current = null })
        }
    }
}

@Composable
private fun BalloonPopupCard(popup: PendingPopupDto, onDismiss: () -> Unit) {
    val infinite = rememberInfiniteTransition(label = "balloon-float")
    val floatOffset by infinite.animateFloat(
        initialValue = 0f,
        targetValue = -8f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Reverse),
        label = "balloon-float-offset",
    )
    val emoji = when (popup.kind) {
        "rank_up_big" -> "🎈" // balloon
        "rank_up" -> "⭐" // star
        else -> "🏆" // trophy, achievements
    }

    Box(modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(12.dp), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(CedalColors.CardBackground)
                .border(1.dp, CedalColors.BorderCyan, RoundedCornerShape(16.dp))
                .padding(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    emoji,
                    fontSize = 26.sp,
                    modifier = Modifier.graphicsLayer { translationY = if (popup.kind == "rank_up_big") floatOffset else 0f },
                )
                Column(modifier = Modifier.padding(start = 10.dp).weight(1f)) {
                    Text(popup.title, color = CedalColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    popup.bigWord?.let {
                        Text(it.uppercase(), color = CedalColors.AccentCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(popup.body, color = CedalColors.TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
                }
                Text(
                    "✕",
                    color = CedalColors.TextMuted,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
                )
            }
        }
    }
}
