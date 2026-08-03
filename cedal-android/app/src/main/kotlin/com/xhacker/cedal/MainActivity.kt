package com.xhacker.cedal

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.fragment.app.FragmentActivity
import com.xhacker.cedal.data.SecureStorage
import com.xhacker.cedal.ui.AppLockState
import com.xhacker.cedal.ui.CodeGithubOAuthResult
import com.xhacker.cedal.ui.CodeGithubOAuthState
import com.xhacker.cedal.ui.CornealBubbleState
import com.xhacker.cedal.ui.GroupLinkDeepLinkState
import com.xhacker.cedal.ui.nav.CedalNavGraph
import com.xhacker.cedal.ui.theme.CedalColors
import com.xhacker.cedal.ui.theme.CedalTheme
import com.xhacker.cedal.ui.theme.ThemeState
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    @Inject lateinit var storage: SecureStorage

    // The most common "return from background" path (app still sitting in
    // Recents, not killed) never calls onCreate at all — Android just brings
    // the existing task forward and calls onResume directly. The
    // ProcessLifecycleOwner listener in CedalNavGraph (ON_STOP -> isLocked =
    // true) should already cover this, but checking again here too is cheap
    // insurance against that signal's ~700ms debounce or any timing gap.
    //
    // Gated on wasStopped rather than firing on every onResume: showing the
    // system biometric prompt itself triggers onPause/onResume on this
    // Activity (without ever calling onStop, since the dialog sits on top of
    // — not away from — this Activity) — checking unconditionally re-locked
    // the app the instant a successful fingerprint/face unlock closed that
    // dialog. Only a real onStop means the app actually left the foreground.
    private var wasStopped = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Seeds the live theme flag from the persisted setting once per
        // process - the Settings toggle then updates ThemeState directly for
        // instant re-theming, and writes storage.darkThemeEnabled to persist it.
        ThemeState.isDark = storage.darkThemeEnabled
        // Same idea for the Corneal bubble's "Corneal Hider" toggle - see
        // CornealBubbleState.hiderEnabled for why the bubble needs the live
        // flag instead of reading storage directly.
        CornealBubbleState.hiderEnabled = storage.cornealHiderEnabled
        // Same idea for an equipped theme pack (see Shop's "Theme Packs") -
        // stored as a plain hex string so this doesn't need a network call
        // just to restore the right accent color on a cold start.
        storage.equippedThemePackHex?.let { hex ->
            runCatching { ThemeState.equippedAccentColor = android.graphics.Color.parseColor(hex).let(::Color) }
        }

        // FLAG_SECURE: blocks screenshots/screen recording of this app and
        // replaces its Recents thumbnail with a blank placeholder. Wanted by
        // either App View Once (always, by default) or Lock on exit (so the
        // Recents thumbnail can't leak whatever was open while "locked").
        if (storage.shouldBlockScreenCapture) {
            window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        }

        // "Seeable" (API 31+ only) - a genuinely separate mechanism from
        // FLAG_SECURE above, so it can control just the Recents thumbnail
        // without touching screenshot/recording ability at all. Locked off
        // whenever App View Once is on, same as the Settings row shows.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            setRecentsScreenshotEnabled(storage.seeableEnabled && !storage.appViewOnceEnabled)
        }

        // savedInstanceState != null means Android is recreating this Activity
        // from a previously saved Bundle — the process got killed in the
        // background (memory pressure) and the user reopened it from
        // Recents. Compose Navigation restores its saved back stack straight
        // to wherever the user left off (e.g. member_home), skipping the
        // "loading" composable entirely — so AppLockState (in-memory, reset
        // to false on a fresh process) never gets a chance to flip true via
        // the normal ON_STOP listener. Set it here instead, before anything
        // renders, so a killed-and-restored session still hits the gate.
        if (savedInstanceState != null) checkAppLock()

        // Covers a cold start straight from the GitHub OAuth deep link
        // (Activity was fully killed, not just backgrounded) - the more
        // common "already running" case is handled by onNewIntent below.
        handleGithubOAuthDeepLink(intent)
        handleGroupLinkDeepLink(intent)

        enableEdgeToEdge()
        setContent {
            CedalTheme {
                // enableEdgeToEdge() draws content behind the status/nav bars;
                // this keeps our own content (header, bottom tab bar) clear of
                // them, while the background still extends to the true edges
                // for the edge-to-edge look. Deliberately excludes ime from
                // this (safeDrawing normally includes it) - consuming it here
                // at the root left nothing for Modifier.imePadding() calls
                // further down (e.g. Code > Pad/Command) to react to, so the
                // keyboard covered their input fields instead of making room.
                // Each screen that needs ime-aware layout applies imePadding()
                // itself, exactly where it's needed.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(CedalColors.Background)
                        .windowInsetsPadding(WindowInsets.safeDrawing.exclude(WindowInsets.ime)),
                ) {
                    CedalNavGraph()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (wasStopped) checkAppLock()
        wasStopped = false
    }

    // android:launchMode="singleTask" (AndroidManifest.xml) routes the
    // GitHub OAuth browser's redirect back into THIS running instance via
    // here, instead of spawning a duplicate Activity on top of it.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleGithubOAuthDeepLink(intent)
        handleGroupLinkDeepLink(intent)
    }

    // Group Profile's "LINK" tab (Round 5) - see GroupLinkDeepLinkState's
    // own doc comment.
    private fun handleGroupLinkDeepLink(intent: Intent) {
        val uri = intent.data ?: return
        if (uri.scheme != "cedalcode" || uri.host != "group") return
        val token = uri.lastPathSegment ?: return
        GroupLinkDeepLinkState.pendingToken = token
    }

    // See CodeGithubOAuthState's own doc comment for why this hands off to
    // a plain ambient object rather than a navigation argument.
    private fun handleGithubOAuthDeepLink(intent: Intent) {
        val uri = intent.data ?: return
        if (uri.scheme != "cedalcode-oauth" || uri.host != "github-callback") return
        CodeGithubOAuthState.pendingResult = if (uri.getQueryParameter("ok") == "true") {
            CodeGithubOAuthResult.Success
        } else {
            CodeGithubOAuthResult.Failure(uri.getQueryParameter("reason"))
        }
    }

    override fun onStop() {
        super.onStop()
        wasStopped = true
    }

    private fun checkAppLock() {
        if (storage.appLockEnabled && storage.accessToken != null && storage.passcodeDone) {
            AppLockState.isLocked = true
        }
    }
}
