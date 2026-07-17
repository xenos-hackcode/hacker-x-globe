package com.xhacker.cedal.ui.screens.member

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xhacker.cedal.ui.CornealBubbleState
import com.xhacker.cedal.ui.theme.CedalColors
import com.xhacker.cedal.viewmodel.AuthViewModel
import kotlin.math.roundToInt

private val BUBBLE_SIZE = 56.dp

// The app-wide "Chat Heads"-style Corneal bubble - see CornealBubbleState
// for the shared position/window state, NavGraph.kt for where this is
// rendered (a sibling of NavHost, shown on every member-area screen).
// Dragging moves it; tapping opens Corneal, either full-screen or as a
// floating window depending on Settings > Navigation > "Bot Access" (see
// SecureStorage.botAccessEnabled). Hidden entirely when Settings > Privacy
// > "Corneal Hider" is on (see SecureStorage.cornealHiderEnabled).
@Composable
fun CornealBubbleOverlay(onOpenFullScreen: () -> Unit, viewModel: AuthViewModel = hiltViewModel()) {
    // Reads the live CornealBubbleState flag (not viewModel.storage directly)
    // so toggling it in Settings recomposes this immediately - see
    // CornealBubbleState.hiderEnabled for why a raw storage read wouldn't.
    if (CornealBubbleState.hiderEnabled) return

    BoxWithConstraints(modifier = Modifier) {
        val density = LocalDensity.current
        val maxWidthPx = with(density) { maxWidth.toPx() }
        val maxHeightPx = with(density) { maxHeight.toPx() }
        val bubbleSizePx = with(density) { BUBBLE_SIZE.toPx() }

        // Defaults to the bottom-right corner the first time the bubble ever
        // shows up in this process - after that, wherever the user last
        // dragged it to persists (in-memory, see CornealBubbleState).
        if (CornealBubbleState.position == null) {
            CornealBubbleState.position = Offset(maxWidthPx - bubbleSizePx - 24f, maxHeightPx - bubbleSizePx - 140f)
        }
        val position = CornealBubbleState.position ?: Offset.Zero

        Icon(
            Icons.Outlined.SmartToy,
            contentDescription = "Corneal",
            tint = CedalColors.AccentCyan,
            modifier = Modifier
                .offset { IntOffset(position.x.roundToInt(), position.y.roundToInt()) }
                .size(BUBBLE_SIZE)
                .clip(CircleShape)
                // "Glass" - translucent, not the usual solid card background.
                .background(CedalColors.CardBackground.copy(alpha = 0.35f))
                .border(BorderStroke(1.dp, CedalColors.BorderCyan.copy(alpha = 0.6f)), CircleShape)
                // Two independent gesture detectors, not one - detectDragGestures
                // only fires its callbacks once the pointer crosses touch slop,
                // so a real tap (down+up with ~zero movement) never reaches
                // onDragEnd and nothing would ever open. detectTapGestures
                // handles that case directly; it naturally won't fire if the
                // touch was already consumed by an actual drag.
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            if (viewModel.storage.botAccessEnabled) {
                                CornealBubbleState.chatOpen = true
                            } else {
                                onOpenFullScreen()
                            }
                        },
                    )
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        // Read the live state, not a val captured once at
                        // composition time - this pointerInput coroutine is
                        // keyed on Unit and never restarts, so a stale local
                        // would keep re-adding each frame's tiny delta to the
                        // same original position instead of accumulating,
                        // making the bubble barely move.
                        val base = CornealBubbleState.position ?: Offset.Zero
                        val next = base + dragAmount
                        CornealBubbleState.position = Offset(
                            next.x.coerceIn(0f, (maxWidthPx - bubbleSizePx).coerceAtLeast(0f)),
                            next.y.coerceIn(0f, (maxHeightPx - bubbleSizePx).coerceAtLeast(0f)),
                        )
                    }
                }
                .padding(14.dp),
        )
    }
}
