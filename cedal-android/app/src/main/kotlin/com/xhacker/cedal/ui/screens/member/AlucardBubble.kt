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
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.xhacker.cedal.ui.AlucardBubbleState
import com.xhacker.cedal.ui.theme.CedalColors
import kotlin.math.roundToInt

private val BUBBLE_SIZE = 56.dp

// Developer Mode's "Chat Heads"-style Alucard bubble - see AlucardBubbleState
// for the shared position, NavGraph.kt for where this is rendered (a
// sibling of NavHost, shown on every developer-area screen). Dragging moves
// it; tapping always opens the full-screen Alucard chat - unlike Corneal's
// bubble, there's no floating-mini-window mode here (see
// AlucardBubbleState's own doc comment for why).
@Composable
fun AlucardBubbleOverlay(onOpenFullScreen: () -> Unit) {
    BoxWithConstraints(modifier = Modifier) {
        val density = LocalDensity.current
        val maxWidthPx = with(density) { maxWidth.toPx() }
        val maxHeightPx = with(density) { maxHeight.toPx() }
        val bubbleSizePx = with(density) { BUBBLE_SIZE.toPx() }

        // Defaults to the bottom-right corner the first time the bubble ever
        // shows up in this process - after that, wherever it was last
        // dragged to persists (in-memory, see AlucardBubbleState).
        if (AlucardBubbleState.position == null) {
            AlucardBubbleState.position = Offset(maxWidthPx - bubbleSizePx - 24f, maxHeightPx - bubbleSizePx - 140f)
        }
        val position = AlucardBubbleState.position ?: Offset.Zero

        Icon(
            Icons.Outlined.Security,
            contentDescription = "Alucard",
            tint = CedalColors.AccentCyan,
            modifier = Modifier
                .offset { IntOffset(position.x.roundToInt(), position.y.roundToInt()) }
                .size(BUBBLE_SIZE)
                .clip(CircleShape)
                // "Glass" - translucent, not the usual solid card background.
                .background(CedalColors.CardBackground.copy(alpha = 0.35f))
                .border(BorderStroke(1.dp, CedalColors.BorderCyan.copy(alpha = 0.6f)), CircleShape)
                // Two independent gesture detectors, not one - see
                // CornealBubbleOverlay's own doc comment for why a plain
                // detectDragGestures alone would never fire a real tap.
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { onOpenFullScreen() })
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val base = AlucardBubbleState.position ?: Offset.Zero
                        val next = base + dragAmount
                        AlucardBubbleState.position = Offset(
                            next.x.coerceIn(0f, (maxWidthPx - bubbleSizePx).coerceAtLeast(0f)),
                            next.y.coerceIn(0f, (maxHeightPx - bubbleSizePx).coerceAtLeast(0f)),
                        )
                    }
                }
                .padding(14.dp),
        )
    }
}
