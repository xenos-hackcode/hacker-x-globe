package com.xhacker.cedal.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset

// In-memory only (not persisted), same trivial pattern as CornealBubbleState -
// tracks the Developer Mode-only Alucard bubble's position. Unlike Corneal,
// Alucard has no floating mini-window mode - tapping it always opens the
// full-screen chat (see AlucardBubbleOverlay/NavGraph.kt), so there's no
// chatOpen/windowPosition/windowSize to track here. Naturally resets on a
// fresh process start; the bubble just re-appears at its default corner.
object AlucardBubbleState {
    var position by mutableStateOf<Offset?>(null)
}
