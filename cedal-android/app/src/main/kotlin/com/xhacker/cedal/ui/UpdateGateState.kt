package com.xhacker.cedal.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.xhacker.cedal.data.AppVersionDto

// Force-update gate (Settings has no toggle for this - it's not optional).
// Checked once at app start (see CedalNavGraph) against GET /app-version:
// - Up to date: nothing shows.
// - Outdated, within the 2-week grace period: a persistent bottom banner
//   ("tap to upgrade, this version might stop working soon") - dismissible
//   per-session but reappears next launch until actually updated.
// - Outdated, past 2 weeks since first noticed (see
//   SecureStorage.updateNoticeFirstShownAt): forceGate=true blocks sign-in/
//   sign-up entirely behind a "please update" panel - see
//   CedalNavGraph/ForceUpdateScreen. Deleting and reinstalling the app is
//   the explicit reset (wipes local storage, starts fresh on whatever
//   version was just installed).
object UpdateGateState {
    var latest by mutableStateOf<AppVersionDto?>(null)
    var outdated by mutableStateOf(false)
    var forceGate by mutableStateOf(false)
    var bannerDismissed by mutableStateOf(false)
}
