package com.xhacker.cedal.ui.screens.member

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xhacker.cedal.ui.theme.CedalColors
import com.xhacker.cedal.ui.theme.CedalErrorText
import com.xhacker.cedal.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

// Permanent Firebase Storage object path + download token, reused for every
// build instead of a fresh one each time - re-upload to this exact object
// (gs://cedal-fd4a2.firebasestorage.app/manualBuilds/cedal-android-latest.apk)
// with this same token and the URL below never changes.
private const val STABLE_APK_URL = "https://firebasestorage.googleapis.com/v0/b/cedal-fd4a2.firebasestorage.app/o/manualBuilds%2Fcedal-android-latest.apk?alt=media&token=b1460d9e-aca3-4a8a-9c34-36b723dd1850"

// Chat list > More > App Updates (owner-only, same gating pattern as Admin
// Review/Godmode - see DeveloperScaffold.kt's own doc comment). Replaces
// hand-rolled curl calls to POST /admin/app-version with a real form: an
// APK still has to be built and hosted somewhere ELSE first (a phone can't
// compile the app that's running on it), but publishing the already-built
// artifact - the part every client actually polls for via GET /app-version
// (see UpdateGateScreen.kt) - becomes one tap instead of a terminal command.
@Composable
fun AppUpdatePublishBody(onBack: () -> Unit, viewModel: AuthViewModel = hiltViewModel()) {
    var versionName by remember { mutableStateOf("") }
    var versionCode by remember { mutableStateOf("") }
    // Defaults to the one permanent, reused-forever Firebase Storage link
    // (same object path + token every build gets uploaded to) so this field
    // never has to be hand-pasted - only overridden below if the server
    // already has some other URL explicitly set.
    var apkUrl by remember { mutableStateOf(STABLE_APK_URL) }
    var changelog by remember { mutableStateOf("") }
    var currentLive by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var publishing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var published by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.getAppVersion().onSuccess { v ->
            versionName = v.versionName
            versionCode = v.versionCode.toString()
            apkUrl = v.apkUrl?.takeIf { it.isNotBlank() } ?: STABLE_APK_URL
            changelog = v.changelog ?: ""
            currentLive = "v${v.versionName} (code ${v.versionCode})" + if (v.apkUrl == null) " - no download link set" else ""
        }.onFailure { error = it.message }
        loading = false
    }

    fun publish() {
        val code = versionCode.toIntOrNull()
        if (code == null || versionName.isBlank()) {
            error = "Version code and version name are required."
            return
        }
        publishing = true
        error = null
        published = false
        scope.launch {
            viewModel.setAppVersion(code, versionName.trim(), apkUrl.trim().ifBlank { null }, changelog.trim().ifBlank { null })
                .onSuccess { published = true; currentLive = "v$versionName (code $code)" }
                .onFailure { error = it.message ?: "Couldn't publish - are you sure this account is an admin?" }
            publishing = false
        }
    }

    // imePadding(): MainActivity uses enableEdgeToEdge(), so without this the
    // keyboard can cover the lower fields (changelog, publish button) instead
    // of the screen shrinking to fit - same fix as GroupChatThreadScreen.kt.
    Column(modifier = Modifier.fillMaxSize().background(CedalColors.Background).padding(16.dp).imePadding()) {
        MemberBackBar(title = "App Updates", onBack = onBack)
        CedalErrorText(error)

        if (loading) {
            Text("Loading…", color = CedalColors.TextMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 24.dp))
            return@Column
        }

        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            currentLive?.let {
                Text(
                    "CURRENTLY LIVE", color = CedalColors.TextSecondary, fontSize = 10.sp, letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
                Text(it, color = CedalColors.TextMuted, fontSize = 12.sp, modifier = Modifier.padding(bottom = 16.dp))
            }
            if (published) {
                Text(
                    "Published - every client now sees this as the latest version.",
                    color = CedalColors.Success, fontSize = 12.sp, modifier = Modifier.padding(bottom = 12.dp),
                )
            }

            AppUpdateField(label = "Version Name", value = versionName, onValueChange = { versionName = it }, placeholder = "e.g. 1.4.0")
            AppUpdateField(
                label = "Version Code", value = versionCode, onValueChange = { if (it.all(Char::isDigit)) versionCode = it },
                placeholder = "e.g. 14", keyboardType = KeyboardType.Number,
            )
            AppUpdateField(label = "APK Download URL", value = apkUrl, onValueChange = { apkUrl = it }, placeholder = "https://…")
            AppUpdateField(
                label = "Changelog", value = changelog, onValueChange = { changelog = it },
                placeholder = "What's new in this build…", multiline = true,
            )

            Text(
                if (publishing) "PUBLISHING…" else "PUBLISH UPDATE",
                color = CedalColors.Background, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(50))
                    .background(CedalColors.AccentCyan)
                    .clickable(enabled = !publishing, interactionSource = remember { MutableInteractionSource() }, indication = null) { publish() }
                    .padding(vertical = 12.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Text(
                "Every signed-in client polls this once per app launch (GET /app-version). Clients on an older versionCode than what's published here see an update prompt with this changelog; after a 14-day grace period they're force-gated until they update.",
                color = CedalColors.TextMuted, fontSize = 11.sp, modifier = Modifier.padding(top = 10.dp),
            )
        }
    }
}

@Composable
private fun AppUpdateField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    multiline: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)) {
        Text(label.uppercase(), color = CedalColors.TextSecondary, fontSize = 10.sp, letterSpacing = 1.sp, modifier = Modifier.padding(bottom = 6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .border(1.dp, CedalColors.BorderCyan, RoundedCornerShape(10.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            if (value.isEmpty()) {
                Text(placeholder, color = CedalColors.TextMuted, fontSize = 13.sp)
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = TextStyle(color = CedalColors.TextPrimary, fontSize = 13.sp),
                cursorBrush = SolidColor(CedalColors.AccentCyan),
                singleLine = !multiline,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                modifier = Modifier.fillMaxWidth().let { if (multiline) it.heightIn(min = 80.dp) else it },
            )
        }
    }
}
