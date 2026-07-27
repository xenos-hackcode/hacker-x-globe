package com.xhacker.cedal.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

// What's currently on screen that Corneal could be given as context - only
// ever set by MemberChatThreadBody, and only when Settings > Privacy >
// "Bot View" is on (see SecureStorage.botViewEnabled). Nothing populates
// this when that's off, so there's no code path where chat content reaches
// Corneal at all, not just a prompt-level instruction to ignore it.
data class ChatContext(val friendId: String, val friendName: String, val recentMessages: List<String>)

// "Call Out" (Settings > Corneal AI, text-based) - only ever set by
// MemberCodeBody, and only when SecureStorage.callOutTextEnabled is on. Same
// off-by-default privacy pattern as ChatContext/Bot View above - nothing
// populates this when the toggle is off.
data class CodeContext(val path: String, val content: String)

// In-memory only (not persisted), same trivial pattern as AppLockState -
// tracks the app-wide Corneal bubble (position, whether its floating
// "Bot Access" window is open, that window's size) and whatever chat
// context is currently available to hand it. Naturally resets on a fresh
// process start; the bubble just re-appears at its default corner.
object CornealBubbleState {
    var position by mutableStateOf<Offset?>(null)
    var chatOpen by mutableStateOf(false)
    var windowPosition by mutableStateOf<Offset?>(null)
    var windowSize by mutableStateOf(DpSize(300.dp, 420.dp))
    var currentChatContext by mutableStateOf<ChatContext?>(null)
    var currentCodeContext by mutableStateOf<CodeContext?>(null)

    // Mirrors SecureStorage.cornealHiderEnabled, same dual-write pattern as
    // ThemeState.isDark/SecureStorage.darkThemeEnabled - CornealBubbleOverlay
    // reads THIS (Compose-observable) rather than the raw storage getter, so
    // toggling it in Settings actually recomposes the bubble immediately.
    // A plain SharedPreferences read has no Compose state link to it, so the
    // bubble would only pick up a storage change on its next unrelated
    // recomposition (e.g. a drag), not the moment the toggle flips.
    var hiderEnabled by mutableStateOf(false)
}
