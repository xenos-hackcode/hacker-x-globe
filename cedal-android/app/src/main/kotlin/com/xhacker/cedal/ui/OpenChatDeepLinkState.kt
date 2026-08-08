package com.xhacker.cedal.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

// Per-chat "Add Shortcut" (MemberChatThreadScreen/MemberFriendProfileScreen)
// - tapping a pinned home-screen shortcut hands the target chat off here
// rather than as a navigation argument, same ambient-object pattern as
// GroupLinkDeepLinkState for the group invite link. CedalNavGraph observes
// this and navigates straight into the chat thread, clearing it once
// consumed.
object OpenChatDeepLinkState {
    var pendingFriendId by mutableStateOf<String?>(null)
    var pendingFriendName by mutableStateOf<String?>(null)
}
