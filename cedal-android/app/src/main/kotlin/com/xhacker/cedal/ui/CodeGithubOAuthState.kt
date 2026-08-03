package com.xhacker.cedal.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

// In-memory handoff from MainActivity.onNewIntent (which catches the
// cedalcode-oauth://github-callback deep link the GitHub OAuth browser
// flow redirects back to) to MemberCodeScreen's Documents tab, which
// observes this and completes the "Connect GitHub" flow once it's set.
// Same ambient-singleton idiom as AppLockState/CornealBubbleState - this
// app is one Activity/NavHost, so there's no natural place to pass this as
// a normal navigation argument.
object CodeGithubOAuthState {
    var pendingResult by mutableStateOf<CodeGithubOAuthResult?>(null)
}

sealed class CodeGithubOAuthResult {
    object Success : CodeGithubOAuthResult()
    data class Failure(val reason: String?) : CodeGithubOAuthResult()
}
