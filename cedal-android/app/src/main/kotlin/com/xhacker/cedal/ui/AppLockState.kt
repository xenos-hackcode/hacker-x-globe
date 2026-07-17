package com.xhacker.cedal.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

// In-memory only (not persisted) — tracks whether the "lock on exit" gate
// (Settings > Security > "Lock on exit") should currently be shown as an
// overlay on top of whatever screen the app was on. Naturally resets to
// false on a fresh process start, since a cold launch already goes through
// the normal passcode gate via CedalNavGraph's "loading" destination check.
object AppLockState {
    var isLocked by mutableStateOf(false)
}
