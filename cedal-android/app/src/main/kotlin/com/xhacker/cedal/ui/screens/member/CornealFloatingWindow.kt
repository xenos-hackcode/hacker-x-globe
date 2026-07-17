package com.xhacker.cedal.ui.screens.member

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xhacker.cedal.ui.CornealBubbleState
import com.xhacker.cedal.ui.theme.CedalColors
import kotlin.math.roundToInt

private val MIN_WINDOW_SIZE = DpSize(220.dp, 280.dp)
private val MAX_WINDOW_SIZE = DpSize(420.dp, 640.dp)

// The "Bot Access" floating window - Corneal rendered as a resizable,
// draggable panel on top of whatever's currently on screen, instead of
// taking it over full-screen (see CornealBubbleOverlay for the tap that
// opens this, gated on SecureStorage.botAccessEnabled). Reuses
// CornealChatBody as-is for the actual chat content.
@Composable
fun CornealFloatingWindow(onClose: () -> Unit) {
    BoxWithConstraints(modifier = Modifier) {
        val density = LocalDensity.current
        val maxWidthPx = with(density) { maxWidth.toPx() }
        val maxHeightPx = with(density) { maxHeight.toPx() }

        if (CornealBubbleState.windowPosition == null) {
            val sizePx = with(density) { CornealBubbleState.windowSize.width.toPx() to CornealBubbleState.windowSize.height.toPx() }
            CornealBubbleState.windowPosition = Offset(
                ((maxWidthPx - sizePx.first) / 2f).coerceAtLeast(0f),
                ((maxHeightPx - sizePx.second) / 2f).coerceAtLeast(0f),
            )
        }
        val windowPosition = CornealBubbleState.windowPosition ?: Offset.Zero
        val windowSize = CornealBubbleState.windowSize

        // The window has a fixed size/position and never resizes itself for
        // the keyboard, so CornealChatBody's own imePadding() (built for a
        // full-screen scaffold) would just clip the input field against this
        // window's fixed bottom edge instead of making room. Instead this
        // pins the window's bottom edge to the keyboard's top edge and, if
        // the window's normal height doesn't fit in the space left above the
        // keyboard, actually shrinks the *displayed* height to fit - just
        // repositioning without shrinking (an earlier version of this fix)
        // left the composer row rendered but physically behind/under the
        // keyboard, invisible and untouchable. Only display position/size
        // change here - the stored windowPosition/windowSize stay untouched
        // so nothing drifts once the keyboard closes again.
        val imeBottomPx = WindowInsets.ime.getBottom(density).toFloat()
        val windowHeightPx = with(density) { windowSize.height.toPx() }
        val availableAboveKeyboardPx = (maxHeightPx - imeBottomPx).coerceAtLeast(0f)
        val displayHeightPx = if (imeBottomPx > 0f) minOf(windowHeightPx, availableAboveKeyboardPx) else windowHeightPx
        val displaySize = DpSize(windowSize.width, with(density) { displayHeightPx.toDp() })
        val displayPosition = if (imeBottomPx > 0f) {
            Offset(windowPosition.x, (availableAboveKeyboardPx - displayHeightPx).coerceAtLeast(0f))
        } else {
            windowPosition
        }

        Box(
            modifier = Modifier
                .offset { IntOffset(displayPosition.x.roundToInt(), displayPosition.y.roundToInt()) }
                .size(displaySize)
                .clip(RoundedCornerShape(18.dp))
                .background(CedalColors.CardBackground)
                .border(1.dp, CedalColors.BorderCyan, RoundedCornerShape(18.dp)),
        ) {
            Column(modifier = Modifier.size(displaySize)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                // Read live state each event, same reason as
                                // CornealBubbleOverlay's drag - this pointerInput
                                // coroutine never restarts, so a val captured
                                // once at composition time would go stale and
                                // the window would barely move.
                                val base = CornealBubbleState.windowPosition ?: Offset.Zero
                                val size = CornealBubbleState.windowSize
                                val next = base + dragAmount
                                CornealBubbleState.windowPosition = Offset(
                                    next.x.coerceIn(0f, (maxWidthPx - with(density) { size.width.toPx() }).coerceAtLeast(0f)),
                                    next.y.coerceIn(0f, (maxHeightPx - with(density) { size.height.toPx() }).coerceAtLeast(0f)),
                                )
                            }
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Icon(Icons.Filled.DragHandle, contentDescription = null, tint = CedalColors.TextMuted, modifier = Modifier.size(16.dp))
                    Text(
                        "Corneal", color = CedalColors.TextPrimary, fontSize = 13.sp,
                        modifier = Modifier.weight(1f).padding(start = 6.dp),
                    )
                    Icon(
                        Icons.Filled.Close, contentDescription = "Close",
                        tint = CedalColors.TextMuted,
                        modifier = Modifier
                            .size(18.dp)
                            .clickable(interactionSource = androidx.compose.runtime.remember { MutableInteractionSource() }, indication = null, onClick = onClose),
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    CornealChatBody(onBack = onClose, showHeader = false, applyImePadding = false)
                }
            }

            // Resize handle - bottom-right corner, dragging adjusts window
            // size directly, clamped between a usable minimum and a
            // reasonable maximum so it can never grow past the screen.
            Icon(
                Icons.Filled.OpenInFull,
                contentDescription = "Resize",
                tint = CedalColors.TextMuted,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(20.dp)
                    .padding(4.dp)
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val deltaDp = with(density) { DpSize(dragAmount.x.toDp(), dragAmount.y.toDp()) }
                            val current = CornealBubbleState.windowSize
                            val newWidth = (current.width + deltaDp.width).coerceIn(MIN_WINDOW_SIZE.width, MAX_WINDOW_SIZE.width)
                            val newHeight = (current.height + deltaDp.height).coerceIn(MIN_WINDOW_SIZE.height, MAX_WINDOW_SIZE.height)
                            CornealBubbleState.windowSize = DpSize(newWidth, newHeight)
                        }
                    },
            )
        }
    }
}
