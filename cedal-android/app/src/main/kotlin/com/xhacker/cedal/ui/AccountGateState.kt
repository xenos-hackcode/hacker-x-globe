package com.xhacker.cedal.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

// Live in-session ban detection (see MemberScaffold's poll, CedalNavGraph's
// overlay). In-memory only, same trivial pattern as AppLockState - set the
// moment the poll notices Users.banned=true for the current session, and
// naturally resets on a fresh process start (a cold launch would just hit
// the normal login flow and get "Invalid account" there instead).
object AccountGateState {
    var active by mutableStateOf(false)
    var permanent by mutableStateOf(false)
    var bannedAt by mutableStateOf<Long?>(null)
}
