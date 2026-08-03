package com.xhacker.cedal.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xhacker.cedal.ui.UpdateGateState
import com.xhacker.cedal.ui.theme.CedalCard
import com.xhacker.cedal.ui.theme.CedalColors
import com.xhacker.cedal.ui.theme.CedalGhostButton
import com.xhacker.cedal.ui.theme.CedalHeader
import com.xhacker.cedal.ui.theme.CedalPrimaryButton
import com.xhacker.cedal.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

private val GRACE_PERIOD_MS = 14L * 24 * 60 * 60 * 1000

// Runs once per process start (see CedalNavGraph) - compares the real
// installed versionCode (via PackageManager, not BuildConfig - avoids
// needing a buildFeatures change) against GET /app-version, and drives
// UpdateGateState accordingly.
@Composable
fun UpdateCheckEffect(viewModel: AuthViewModel = hiltViewModel()) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        val installedVersionCode = try {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            if (android.os.Build.VERSION.SDK_INT >= 28) info.longVersionCode.toInt() else @Suppress("DEPRECATION") info.versionCode
        } catch (e: Exception) {
            Int.MAX_VALUE // can't determine our own version - never gate on a failure to read it
        }

        viewModel.getAppVersion().onSuccess { latest ->
            UpdateGateState.latest = latest
            val storage = viewModel.storage
            if (latest.versionCode > installedVersionCode) {
                UpdateGateState.outdated = true
                val firstShown = storage.updateNoticeFirstShownAt ?: System.currentTimeMillis().also { storage.updateNoticeFirstShownAt = it }
                if (System.currentTimeMillis() - firstShown > GRACE_PERIOD_MS) {
                    storage.forceUpdateGate = true
                    storage.clearSession()
                    UpdateGateState.forceGate = true
                }
            } else {
                // Genuinely up to date now (or a fresh install) - clear any
                // stale gate state from before.
                storage.updateNoticeFirstShownAt = null
                storage.forceUpdateGate = false
                UpdateGateState.outdated = false
                UpdateGateState.forceGate = false
            }
        }
    }
}

// Persistent bottom banner - shown whenever outdated and not yet force-
// gated. Re-appears every launch (bannerDismissed is in-memory only) until
// an actual update lands, per "stays there" in the app owner's own words.
@Composable
fun UpdateBanner(viewModel: AuthViewModel = hiltViewModel()) {
    val latest = UpdateGateState.latest
    if (!UpdateGateState.outdated || UpdateGateState.bannerDismissed || latest == null) return
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.BottomCenter) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(CedalColors.CardBackground)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                    val url = latest.apkUrl
                    if (url != null) {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    } else {
                        android.widget.Toast.makeText(context, "No download link configured yet.", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "New version available (v${latest.versionName}) - tap to upgrade",
                    color = CedalColors.TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                )
                Text(
                    "This version might stop working soon.",
                    color = CedalColors.TextSecondary, fontSize = 10.sp,
                )
            }
            Text(
                "✕", color = CedalColors.TextMuted, fontSize = 14.sp,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                        // Only hides it for this process - it reappears next
                        // launch (see UpdateCheckEffect), and About always
                        // has a live "update now" link regardless (see
                        // MemberAboutBody) - dismissing isn't a dead end.
                        UpdateGateState.bannerDismissed = true
                        scope.launch { viewModel.declineUpdate(latest.versionCode) }
                    },
            )
        }
    }
}

// Blocks sign-in/sign-up entirely - shown instead of the normal nav once
// the 2-week grace period runs out with no update. Only way out is an
// actual update (or reinstalling) - there's deliberately no "continue
// anyway" here.
@Composable
fun ForceUpdateScreen() {
    val context = LocalContext.current
    val latest = UpdateGateState.latest

    Box(
        modifier = Modifier.fillMaxSize().background(CedalColors.Background).padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        CedalCard {
            CedalHeader("UPDATE REQUIRED", "NEURAL SIGN IN PANEL")
            Text(
                "Please update Cedal to keep using it.",
                color = CedalColors.Error, fontSize = 16.sp, modifier = Modifier.padding(top = 8.dp, bottom = 10.dp),
            )
            latest?.let {
                Text(
                    "The latest version is v${it.versionName}.",
                    color = CedalColors.TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(bottom = 6.dp),
                )
                it.changelog?.takeIf { c -> c.isNotBlank() }?.let { changelog ->
                    Text(changelog, color = CedalColors.TextSecondary, fontSize = 11.sp, modifier = Modifier.padding(bottom = 14.dp))
                }
            }
            CedalPrimaryButton(
                text = "UPDATE NOW",
                modifier = Modifier.padding(bottom = 8.dp),
                onClick = {
                    val url = latest?.apkUrl
                    if (url != null) {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    } else {
                        android.widget.Toast.makeText(context, "No download link configured yet.", android.widget.Toast.LENGTH_SHORT).show()
                    }
                },
            )
        }
    }
}
