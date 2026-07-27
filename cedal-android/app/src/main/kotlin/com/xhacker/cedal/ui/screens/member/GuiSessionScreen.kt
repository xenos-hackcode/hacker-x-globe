package com.xhacker.cedal.ui.screens.member

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.xhacker.cedal.ui.theme.CedalColors
import com.xhacker.cedal.viewmodel.AuthViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

// The live view for a gui-runner session (see GuiSessionService server-side)
// - a plain WebView pointed straight at gui-runner's own Cloud Run URL, no
// custom protocol handling needed since noVNC is just a normal web page
// that opens its own websocket. Leaving this screen for any reason (back
// button, system back, or navigating elsewhere) always calls stopGuiSession
// via DisposableEffect - gui-runner's own hard timeout is the real backstop
// if this call never lands, but there's no reason to leave the one
// globally-available session tied up a second longer than the user
// actually wants it. Deliberately NOT rememberCoroutineScope() for the
// dispose-time call - that scope gets cancelled around the same moment the
// composable leaves composition, which could silently drop the very call
// onDispose exists to make (same problem CodeRunSession.watchScope solves
// for surviving navigating away from Code).
@Composable
fun GuiSessionBody(
    sessionId: String,
    viewUrl: String,
    affinityCookies: List<String> = emptyList(),
    onBack: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    DisposableEffect(sessionId) {
        onDispose {
            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch { viewModel.stopGuiSession(sessionId) }
        }
    }
    BackHandler(onBack = onBack)

    Column(modifier = Modifier.fillMaxSize()) {
        MemberBackBar(title = "Live View", onBack = onBack)
        Box(modifier = Modifier.weight(1f)) {
            GuiSessionWebView(viewUrl, affinityCookies)
        }
        Text(
            "Closes automatically after 10 minutes.",
            color = CedalColors.TextMuted, fontSize = 11.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(8.dp),
        )
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun GuiSessionWebView(url: String, affinityCookies: List<String>) {
    // The page itself can load fine (noVNC's static HTML/JS) well before its
    // own websocket actually connects to the VNC session - without this,
    // that gap just looked like a frozen/blank screen with zero feedback
    // that anything was happening.
    var pageLoading by remember { mutableStateOf(true) }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                // Must happen before loadUrl - gui-runner can now host several
                // concurrent sessions across different instances (see
                // GuiSessionService.kt), and this cookie is what gets this
                // WebView's page load + websocket upgrade routed back to the
                // exact instance that's actually running this session instead
                // of whichever one Cloud Run would otherwise pick.
                val cookieManager = CookieManager.getInstance()
                cookieManager.setAcceptCookie(true)
                affinityCookies.forEach { cookie -> cookieManager.setCookie(url, cookie) }
                cookieManager.flush()

                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, loadedUrl: String?) {
                            pageLoading = false
                        }
                    }
                    loadUrl(url)
                }
            },
        )
        if (pageLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = CedalColors.AccentCyan)
                    Text(
                        "Starting the live session — a cold start can take a bit longer than usual…",
                        color = CedalColors.TextMuted, fontSize = 12.sp,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            }
        }
    }
}
