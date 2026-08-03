package com.xhacker.cedal.ui.screens.member

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xhacker.cedal.ui.UpdateGateState
import com.xhacker.cedal.ui.theme.CedalColors

// Ported field-for-field from cedal-mobile's app/(auth)/(member)/about.tsx —
// entirely static text, no backend needed.
@Composable
fun MemberAboutBody(onBack: () -> Unit) {
    val context = LocalContext.current
    val latest = UpdateGateState.latest
    Column(modifier = Modifier.fillMaxSize().background(CedalColors.Background).padding(16.dp)) {
        MemberBackBar(title = "About", onBack = onBack)
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            SettingsSectionCard("") {
                Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                    Text("Cedal mesh node client", color = CedalColors.TextPrimary, fontSize = 16.sp)
                    Text("Alpha build · Experimental", color = CedalColors.TextSecondary, fontSize = 11.sp, modifier = Modifier.padding(bottom = 4.dp))
                    Text(
                        "Cedal is a cyber-native chat and work node. It blends chats, work lanes, calls, and fantasy spaces into one mesh so you can stay in flow instead of juggling apps.",
                        color = CedalColors.TextSecondary, fontSize = 12.sp,
                    )
                }
            }

            // Live update status - reachable here anytime regardless of
            // whether the update banner was dismissed (see UpdateBanner's
            // own doc comment) - dismissing it isn't a dead end.
            if (UpdateGateState.outdated && latest != null) {
                SettingsSectionCard("Update available") {
                    Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                        Text("v${latest.versionName} is available.", color = CedalColors.TextPrimary, fontSize = 13.sp)
                        latest.changelog?.takeIf { it.isNotBlank() }?.let {
                            Text(it, color = CedalColors.TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
                        }
                        Text(
                            if (latest.apkUrl != null) "UPDATE NOW" else "No download link configured yet.",
                            color = if (latest.apkUrl != null) CedalColors.AccentCyan else CedalColors.TextMuted,
                            fontSize = 12.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .let {
                                    val url = latest.apkUrl
                                    if (url != null) {
                                        it.clip(RoundedCornerShape(8.dp)).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                        }
                                    } else it
                                },
                        )
                    }
                }
            }

            SettingsSectionCard("Build") {
                InfoTextRow("Version", "0.1.0 (alpha channel)")
                InfoTextRow("Mesh ID", "Local dev mesh · not yet federated")
                InfoTextRow("Build channel", "Internal preview · subject to breaking changes")
            }

            SettingsSectionCard("Credits") {
                InfoTextRow("Crafted by", "Xenos Hacker")
                InfoTextRow("Stack", "Kotlin, Jetpack Compose, Ktor, and AI-powered assistance.")
            }

            SettingsSectionCard("Legal") {
                Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                    Text(
                        "This build is experimental. Do not store sensitive production data. Features, visuals, and behavior may change without notice.",
                        color = CedalColors.TextSecondary, fontSize = 12.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoTextRow(label: String, description: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp)) {
        Text(label, color = CedalColors.TextPrimary, fontSize = 13.sp)
        Text(description, color = CedalColors.TextSecondary, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
    }
}
