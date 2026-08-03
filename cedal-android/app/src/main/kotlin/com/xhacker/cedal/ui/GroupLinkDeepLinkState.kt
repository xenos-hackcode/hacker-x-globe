package com.xhacker.cedal.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

// Group Profile's "LINK" tab (Round 5) - scanning/opening a group's QR or
// invite link (cedalcode://group/{token}) hands the token off here rather
// than as a navigation argument, same ambient-object pattern as
// CodeGithubOAuthState for the GitHub OAuth callback. CedalNavGraph observes
// this and navigates to the join-preview screen, clearing it once consumed.
object GroupLinkDeepLinkState {
    var pendingToken by mutableStateOf<String?>(null)
}
