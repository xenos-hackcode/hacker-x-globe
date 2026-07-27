package com.xhacker.cedal.ui.screens.member

import android.app.Activity
import android.os.Build
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import com.xhacker.cedal.data.SecureStorage
import com.xhacker.cedal.data.UserProfile
import com.xhacker.cedal.ui.screens.BiometricAuth
import com.xhacker.cedal.ui.theme.CedalChip
import com.xhacker.cedal.ui.theme.CedalColors
import com.xhacker.cedal.ui.theme.CedalErrorText
import com.xhacker.cedal.ui.theme.CedalGhostButton
import com.xhacker.cedal.ui.theme.CedalPrimaryButton
import com.xhacker.cedal.ui.theme.CedalTextField
import com.xhacker.cedal.ui.theme.CedalToggle
import com.xhacker.cedal.ui.theme.ThemeState
import com.xhacker.cedal.util.vibrate
import com.xhacker.cedal.viewmodel.AuthViewModel
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// Ported from cedal-mobile's settings.tsx, which composes Chat/Groups/
// Navigation/Call sections. Note: in the real app almost all of these rows
// are also just local useState with no persistence or backend behind them —
// matching that same level of fidelity here isn't a shortcut, it's accurate.
// The two rows that ARE real here (Update passcode, Link guest node) use
// our actual backend.
@Composable
fun MemberSettingsBody(
    onBack: () -> Unit,
    onSignOut: () -> Unit,
    onViewTerms: () -> Unit,
    onSwitchAccount: () -> Unit,
    onOpenSecurity: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    var profile by remember { mutableStateOf<UserProfile?>(null) }
    LaunchedEffect(Unit) { viewModel.getProfile().onSuccess { profile = it } }

    val scrollState = rememberScrollState()

    Column(modifier = Modifier.fillMaxSize().background(CedalColors.Background).padding(16.dp).imePadding()) {
        // Fixed — only the sections below scroll.
        MemberBackBar(title = "Settings", onBack = onBack)

        Column(modifier = Modifier.weight(1f).verticalScroll(scrollState)) {
            ChatSettingsSection()
            GroupSettingsSection()
            SecuritySettingsSection(profile = profile, viewModel = viewModel)
            PrivacySettingsSection(profile = profile, viewModel = viewModel)
            NavigationSettingsSection(viewModel = viewModel)
            CallSettingsSection()
            LanguageSettingsSection(viewModel = viewModel)
            AiSettingsSection(viewModel = viewModel)

            SettingsSectionCard(title = "Legal") {
                SettingsNavRow(label = "Terms & Conditions", description = "View the Cedal terms you accepted.", onClick = onViewTerms)
            }

            SettingsSectionCard(title = "Account") {
                SettingsNavRow(label = "Popularity & Phone Number", description = "Choose what other people can see on your profile, and manage your verified number.", onClick = onOpenSecurity)
                SettingsNavRow(label = "Switch Account", description = "Instantly swap between accounts saved on this device.", onClick = onSwitchAccount)
            }

            CedalGhostButton(text = "SIGN OUT", modifier = Modifier.padding(top = 4.dp), onClick = { viewModel.logout(); onSignOut() })
            DeleteAccountRow(viewModel, onDeleted = { viewModel.logout(); onSignOut() })
        }
    }
}

@Composable
fun SettingsSectionCard(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.padding(bottom = 18.dp)) {
        if (title.isNotEmpty()) {
            Text(title.uppercase(), color = CedalColors.TextSecondary, fontSize = 11.sp, letterSpacing = 2.sp, modifier = Modifier.padding(bottom = 6.dp))
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(CedalColors.Background)
                .border(1.dp, CedalColors.BorderSlate, RoundedCornerShape(18.dp)),
        ) {
            content()
        }
    }
}

@Composable
private fun SettingsNavRow(label: String, description: String, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = CedalColors.TextPrimary, fontSize = 13.sp)
            Text(description, color = CedalColors.TextSecondary, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
        }
        Text("›", color = CedalColors.TextSecondary, fontSize = 18.sp)
    }
}

// --- Chat ---

@Composable
private fun ChatSettingsSection(viewModel: AuthViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var volume by remember { mutableFloatStateOf(viewModel.storage.notificationVolume / 100f) }
    var soundsEnabled by remember { mutableStateOf(viewModel.storage.soundsEnabled) }
    var typingIndicators by remember { mutableStateOf(viewModel.storage.typingIndicatorsEnabled) }
    var soundUri by remember { mutableStateOf(viewModel.storage.notificationSoundUri) }
    // Up to 3 saved clips (recorded or picked) the user can switch between -
    // see SecureStorage.notificationSoundClips' own doc comment.
    var clips by remember { mutableStateOf(viewModel.storage.notificationSoundClips) }
    var clipError by remember { mutableStateOf<String?>(null) }
    var recording by remember { mutableStateOf(false) }
    var recordJob by remember { mutableStateOf<android.media.MediaRecorder?>(null) }
    var recordFile by remember { mutableStateOf<java.io.File?>(null) }
    var recordSecondsLeft by remember { mutableIntStateOf(15) }

    fun labelFor(uri: String): String =
        if (uri.startsWith("content://")) {
            runCatching { queryDisplayName(context, android.net.Uri.parse(uri)) }.getOrNull() ?: "Picked file"
        } else {
            "Recorded clip"
        }

    fun soundLabel(): String? = soundUri?.let { labelFor(it) }

    // New clips are added to the list AND immediately selected as active -
    // matches the old behavior (record/pick used to overwrite the single
    // active sound outright). Capped at 3 - delete one first to make room
    // rather than silently evicting the oldest.
    fun addClip(uri: String) {
        if (clips.size >= 3) {
            clipError = "You already have 3 saved clips - delete one first."
            return
        }
        clipError = null
        clips = clips + uri
        viewModel.storage.notificationSoundClips = clips
        soundUri = uri
        viewModel.storage.notificationSoundUri = uri
    }

    fun selectClip(uri: String) {
        soundUri = uri
        viewModel.storage.notificationSoundUri = uri
    }

    fun deleteClip(uri: String) {
        clips = clips - uri
        viewModel.storage.notificationSoundClips = clips
        clipError = null
        if (soundUri == uri) {
            val next = clips.firstOrNull()
            soundUri = next
            viewModel.storage.notificationSoundUri = next
        }
    }

    // Recording writes straight into internal app storage (not cache) so it
    // survives past a cache-clear, same as it needing to survive an app
    // restart to actually be usable as a notification sound later.
    fun stopRecording() {
        recording = false
        runCatching { recordJob?.stop(); recordJob?.release() }
        recordJob = null
        val file = recordFile
        if (file != null && file.length() > 0) {
            addClip(android.net.Uri.fromFile(file).toString())
        }
    }

    fun startRecording() {
        val dir = java.io.File(context.filesDir, "notification_sounds").apply { mkdirs() }
        val file = java.io.File(dir, "alert_${System.currentTimeMillis()}.m4a")
        recordFile = file
        runCatching {
            val recorder = if (android.os.Build.VERSION.SDK_INT >= 31) {
                android.media.MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                android.media.MediaRecorder()
            }
            recorder.setAudioSource(android.media.MediaRecorder.AudioSource.MIC)
            recorder.setOutputFormat(android.media.MediaRecorder.OutputFormat.MPEG_4)
            recorder.setAudioEncoder(android.media.MediaRecorder.AudioEncoder.AAC)
            recorder.setOutputFile(file.absolutePath)
            recorder.prepare()
            recorder.start()
            recordJob = recorder
            recording = true
            recordSecondsLeft = 15
        }
    }

    // Auto-stops at 15s - counts down live so it's obvious a cap exists,
    // rather than silently cutting off with no warning.
    LaunchedEffect(recording) {
        while (recording && recordSecondsLeft > 0) {
            kotlinx.coroutines.delay(1000)
            recordSecondsLeft -= 1
        }
        if (recording && recordSecondsLeft <= 0) stopRecording()
    }

    val micPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),
    ) { granted -> if (granted) startRecording() }

    val filePicker = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            // Needed to still be able to read/play this URI after the
            // picker closes and across future app restarts - without this,
            // the permission grant is one-shot and playback would silently
            // fail the next time NotificationSound tries to use it.
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            addClip(uri.toString())
        }
    }

    SettingsSectionCard("Chat") {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Notification volume", color = CedalColors.TextPrimary, fontSize = 13.sp)
                Text("Controls how loud Cedal's own alert sound plays.", color = CedalColors.TextSecondary, fontSize = 11.sp)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
                    VolumeStepButton("-") {
                        volume = (volume - 0.1f).coerceAtLeast(0f)
                        viewModel.storage.notificationVolume = (volume * 100).roundToInt()
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                            .clip(RoundedCornerShape(50))
                            .background(CedalColors.BorderSlate),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(volume)
                                .clip(RoundedCornerShape(50))
                                .background(CedalColors.Success)
                                .padding(vertical = 3.dp),
                        ) {}
                    }
                    VolumeStepButton("+") {
                        volume = (volume + 0.1f).coerceAtMost(1f)
                        viewModel.storage.notificationVolume = (volume * 100).roundToInt()
                    }
                    Text("${(volume * 100).roundToInt()}%", color = CedalColors.TextPrimary, fontSize = 13.sp, modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
        SettingsToggleRow(
            "Sounds",
            if (!soundsEnabled) "Off - notifications vibrate instead of playing a sound." else (soundLabel()?.let { "Custom alert sound: $it" } ?: "Plays the system default alert sound."),
            soundsEnabled,
        ) { soundsEnabled = it; viewModel.storage.soundsEnabled = it }
        SettingsToggleRow("Typing indicators", "Show when you and a friend are typing to each other.", typingIndicators) {
            typingIndicators = it; viewModel.storage.typingIndicatorsEnabled = it
        }
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp)) {
            Text("Alert sound", color = CedalColors.TextPrimary, fontSize = 13.sp)
            Text(
                if (recording) "Recording… $recordSecondsLeft s left" else "Record up to 15 seconds, or pick an audio file already on your phone - either one becomes your notification sound.",
                color = CedalColors.TextSecondary, fontSize = 11.sp, modifier = Modifier.padding(bottom = 8.dp),
            )
            Row {
                CedalGhostButton(
                    text = if (recording) "STOP" else "RECORD",
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                    onClick = {
                        if (recording) {
                            stopRecording()
                        } else if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) ==
                            android.content.pm.PackageManager.PERMISSION_GRANTED
                        ) {
                            startRecording()
                        } else {
                            micPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                        }
                    },
                )
                CedalGhostButton(text = "PICK FILE", modifier = Modifier.weight(1f), onClick = { filePicker.launch(arrayOf("audio/*")) })
            }
            soundLabel()?.let { Text("Selected sound: $it", color = CedalColors.TextSecondary, fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp)) }
        }
    }
}

@Composable
private fun VolumeStepButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .border(1.dp, CedalColors.BorderSlate, RoundedCornerShape(50))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(label, color = CedalColors.TextPrimary, fontSize = 14.sp)
    }
}

// Chat message translation - a language here does NOT relocalize the app's
// own UI (buttons/menus/screens stay English for now, a much bigger
// separate effort) - it only affects real chat message CONTENT: your own
// messages send in your language and get auto-translated for a friend
// whose own choice differs, and theirs get translated back for you, same
// idea both directions. See TranslationService server-side.
private val CHAT_LANGUAGES = listOf(
    "English", "French", "Yoruba", "German", "Spanish", "Chinese", "Japanese",
    "Portuguese", "Italian", "Russian", "Arabic", "Hindi", "Korean", "Swahili",
    "Igbo", "Hausa", "Dutch", "Turkish", "Vietnamese", "Polish",
)

@Composable
private fun LanguageSettingsSection(viewModel: AuthViewModel = hiltViewModel()) {
    val scope = rememberCoroutineScope()
    var selected by remember { mutableStateOf(viewModel.storage.chatLanguage ?: "English") }
    var saving by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }

    SettingsSectionCard("Languages") {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp)) {
            Text("Chat language", color = CedalColors.TextPrimary, fontSize = 13.sp)
            Text(
                "Messages you send and receive get auto-translated to match - this doesn't change the app's own screens/menus, just chat content.",
                color = CedalColors.TextSecondary, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp, bottom = 10.dp),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.dp, CedalColors.BorderCyan, RoundedCornerShape(10.dp))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { expanded = !expanded }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        if (saving) "Saving…" else selected,
                        color = CedalColors.TextPrimary, fontSize = 13.sp,
                        modifier = Modifier.weight(1f),
                    )
                    Text(if (expanded) "▲" else "▼", color = CedalColors.TextSecondary, fontSize = 12.sp)
                }
            }
            if (expanded) {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
                    CHAT_LANGUAGES.forEach { language ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                    expanded = false
                                    if (language != selected) {
                                        selected = language
                                        saving = true
                                        viewModel.storage.chatLanguage = language
                                        scope.launch {
                                            viewModel.updateChatLanguage(language)
                                            saving = false
                                        }
                                    }
                                }
                                .padding(vertical = 10.dp),
                        ) {
                            Text(
                                language,
                                color = if (language == selected) CedalColors.AccentCyan else CedalColors.TextPrimary,
                                fontSize = 13.sp, fontWeight = if (language == selected) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.weight(1f),
                            )
                            if (language == selected) Text("✓", color = CedalColors.AccentCyan, fontSize = 14.sp)
                        }
                    }
                }
            }
            Text(
                "Tip: for the best translation quality, avoid slang - plain, literal wording translates far more accurately (this applies to whoever you're chatting with too).",
                color = CedalColors.TextMuted, fontSize = 11.sp, modifier = Modifier.padding(top = 10.dp),
            )
        }
    }
}

// The three AI assistants Cedal actually has, each with their own separate
// conversation history (see AiChatHistoryService for Corneal/ARC,
// AiChangeRequestService for Coder) - a "Clear history" here wipes that ONE
// lane's history only, never the other two.
@Composable
private fun AiSettingsSection(viewModel: AuthViewModel = hiltViewModel()) {
    val scope = rememberCoroutineScope()

    SettingsSectionCard("AI") {
        AiAssistantRow(
            name = "Corneal",
            description = "Your personal assistant for the whole app - floats as a draggable bubble, helps you figure out how to do things in Cedal, understands photos/stickers/voice messages you send it.",
            onClearHistory = { scope.launch { viewModel.deleteCornealHistory() } },
        )
        CallOutTextToggleRow(viewModel)
        CallOutScreenCaptureToggleRow(viewModel)
        AiAssistantRow(
            name = "ARC",
            description = "The cybersecurity tutor behind ARC's Assistant tab - explains security/networking concepts, knows ARC's own Learn/Labs/Ops sub-tabs, understands photos/stickers/voice messages.",
            onClearHistory = { scope.launch { viewModel.deleteArcHistory() } },
        )
        AiAssistantRow(
            name = "Coder",
            description = "The AI behind Code's own chat - creates/edits/deletes/renames/moves/copies files in your Code area, runs code, proposes new languages, understands photos/stickers/voice messages and code blocks sent in any chat.",
            onClearHistory = { scope.launch { viewModel.deleteAiRequestHistory() } },
        )
    }
}

// "Call Out" (text-based) - off by default, same pattern as Bot View. Gates
// whether Code is ever allowed to give Corneal the currently open file's
// real content (see CornealBubbleState.currentCodeContext/MemberCodeScreen).
@Composable
private fun CallOutTextToggleRow(viewModel: AuthViewModel) {
    var enabled by remember { mutableStateOf(viewModel.storage.callOutTextEnabled) }
    SettingsToggleRow(
        "Call Out",
        "Lets Corneal read the file you have open in Code and point out a specific mistake by circling/highlighting it - can hand the fix to Coder if you confirm. Off by default.",
        enabled,
    ) { turnOn ->
        enabled = turnOn
        viewModel.storage.callOutTextEnabled = turnOn
    }
}

// "Call Out" (real screen capture) - off by default. Distinct from the
// text-based toggle above: this triggers a real Android screen-capture
// permission prompt and periodically samples what's actually on screen, so
// it costs noticeably more battery - called out explicitly here rather than
// discovered the hard way on an older/smaller phone.
@Composable
private fun CallOutScreenCaptureToggleRow(viewModel: AuthViewModel) {
    var enabled by remember { mutableStateOf(viewModel.storage.callOutScreenCaptureEnabled) }
    SettingsToggleRow(
        "Call Out (Screen Capture)",
        "Lets Corneal actually look at your screen, not just text - real vision, on demand. Bad for small/older phones: this drains battery noticeably more than the text-based Call Out above. Off by default.",
        enabled,
    ) { turnOn ->
        enabled = turnOn
        viewModel.storage.callOutScreenCaptureEnabled = turnOn
    }
}

@Composable
private fun AiAssistantRow(name: String, description: String, onClearHistory: () -> Unit) {
    var confirming by remember { mutableStateOf(false) }
    var cleared by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp)) {
        Text(name, color = CedalColors.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Text(description, color = CedalColors.TextSecondary, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp, bottom = 8.dp))
        if (confirming) {
            Row {
                CedalGhostButton(
                    text = "YES, CLEAR IT",
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                    onClick = { confirming = false; cleared = true; onClearHistory() },
                )
                CedalGhostButton(text = "CANCEL", modifier = Modifier.weight(1f), onClick = { confirming = false })
            }
        } else {
            CedalGhostButton(
                text = if (cleared) "HISTORY CLEARED" else "CLEAR $name HISTORY",
                onClick = { confirming = true },
            )
        }
    }
}

// --- Groups ---

@Composable
private fun GroupSettingsSection() {
    var muteNewGroups by remember { mutableStateOf(false) }
    var mentionsOnly by remember { mutableStateOf(false) }
    var joinLeaveMessages by remember { mutableStateOf(true) }
    var typingIndicators by remember { mutableStateOf(true) }
    var autoPinOwned by remember { mutableStateOf(true) }

    SettingsSectionCard("Groups") {
        SettingsToggleRow("Mute new groups", if (muteNewGroups) "Newly joined groups start muted." else "New groups follow your normal notification settings.", muteNewGroups) {
            muteNewGroups = it; if (it) mentionsOnly = false
        }
        SettingsToggleRow("Mentions only by default", "Only notify for @mentions when joining a new group.", mentionsOnly) {
            mentionsOnly = it; if (it) muteNewGroups = false
        }
        SettingsToggleRow("Join & leave messages", "Show small system messages when members join or leave groups.", joinLeaveMessages) { joinLeaveMessages = it }
        SettingsToggleRow("Typing indicators in groups", "Show when members are typing in any group chat.", typingIndicators) { typingIndicators = it }
        SettingsToggleRow("Auto-pin owned groups", "Automatically keep groups you create at the top of your list.", autoPinOwned) { autoPinOwned = it }
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp)) {
            CedalGhostButton(
                text = "RESET GROUP SETTINGS",
                onClick = {
                    muteNewGroups = false; mentionsOnly = false
                    joinLeaveMessages = true; typingIndicators = true; autoPinOwned = true
                },
            )
        }
    }
}

// --- Navigation (theme, language) ---

@Composable
private fun NavigationSettingsSection(viewModel: AuthViewModel) {
    var language by remember { mutableStateOf("en") }

    SettingsSectionCard("Navigation") {
        // Bound directly to the real, Compose-observable theme flag - flips
        // the whole app's colors immediately, not just this row's own state.
        SettingsToggleRow(
            "Theme",
            if (ThemeState.isDark) "Cedal mesh is running in night mode." else "Cedal mesh is running in day mode.",
            ThemeState.isDark,
        ) { turnOn ->
            ThemeState.isDark = turnOn
            viewModel.storage.darkThemeEnabled = turnOn
        }

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Language", color = CedalColors.TextPrimary, fontSize = 13.sp)
                Text("Choose the language Cedal uses.", color = CedalColors.TextSecondary, fontSize = 11.sp)
            }
            listOf("en" to "English", "fr" to "Français").forEach { (code, label) ->
                LanguageChip(label = label, active = language == code, onClick = { language = code })
            }
        }

        BotAccessToggleRow(viewModel)
    }
}

// Off by default - controls what tapping the Corneal bubble does (see
// CornealBubbleOverlay). Off: opens Corneal full-screen, same as before it
// became a bubble. On: opens as a resizable/draggable floating window
// (CornealFloatingWindow) on top of whatever's currently on screen.
@Composable
private fun BotAccessToggleRow(viewModel: AuthViewModel) {
    var enabled by remember { mutableStateOf(viewModel.storage.botAccessEnabled) }
    SettingsToggleRow(
        "Bot Access",
        "Opens Corneal as a floating window you can resize and move around, instead of taking over the whole screen.",
        enabled,
    ) { turnOn ->
        enabled = turnOn
        viewModel.storage.botAccessEnabled = turnOn
    }
}

@Composable
private fun LanguageChip(label: String, active: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .padding(start = 6.dp)
            .clip(RoundedCornerShape(50))
            .background(if (active) CedalColors.Success.copy(alpha = 0.2f) else CedalColors.Background)
            .border(1.dp, if (active) CedalColors.Success else CedalColors.BorderSlate, RoundedCornerShape(50))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(label, color = if (active) CedalColors.Success else CedalColors.TextSecondary, fontSize = 11.sp)
    }
}

@Composable
private fun LinkGuestRow(viewModel: AuthViewModel) {
    var expanded by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var done by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp)) {
        Text("Link guest node", color = CedalColors.TextPrimary, fontSize = 13.sp)
        Text(
            "Bind this guest node to an email so you can sign in or recover it later.",
            color = CedalColors.TextSecondary, fontSize = 11.sp, modifier = Modifier.padding(bottom = 8.dp),
        )
        if (!expanded) {
            CedalGhostButton(text = if (done) "LINKED" else "LINK EMAIL", onClick = { expanded = true })
            return@Column
        }
        CedalTextField(
            value = email, onValueChange = { email = it; done = false }, prefix = "⧉", placeholder = "you@cedal.app",
            keyboardType = KeyboardType.Email, modifier = Modifier.padding(bottom = 8.dp),
        )
        CedalTextField(
            value = password, onValueChange = { password = it; done = false }, prefix = "★", placeholder = "master key",
            isPassword = true, modifier = Modifier.padding(bottom = 8.dp),
        )
        CedalErrorText(error)
        Row {
            CedalPrimaryButton(
                text = when { loading -> "LINKING…"; done -> "LINKED"; else -> "LINK" },
                enabled = !loading,
                loading = loading,
                modifier = Modifier.weight(1f).padding(end = 8.dp),
                onClick = {
                    loading = true; error = null
                    scope.launch {
                        val result = viewModel.linkGuestToEmail(email, password)
                        loading = false
                        result.onSuccess { done = true; expanded = false }.onFailure { error = it.message }
                    }
                },
            )
            CedalGhostButton(text = "CANCEL", modifier = Modifier.weight(1f), onClick = { expanded = false; error = null })
        }
    }
}

// --- Call ---

private val REGIONS = listOf("Auto", "EU-West", "US-East", "Asia-Pacific")

@Composable
private fun CallSettingsSection() {
    var aiMode by remember { mutableStateOf(false) }
    var regionIndex by remember { mutableIntStateOf(0) }

    SettingsSectionCard("Call") {
        SettingsToggleRow(
            "AI mode",
            if (aiMode) "Reduced IP tracking & noise filters. Not safe; expect unstable calls." else "Standard call safety. Turn on to experiment with looser AI call settings.",
            aiMode,
        ) { aiMode = it }
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp)) {
            Text("Region VPN", color = CedalColors.TextPrimary, fontSize = 13.sp)
            Text("Switch between regional signal hubs if your current region is bad.", color = CedalColors.TextSecondary, fontSize = 11.sp)
            Box(modifier = Modifier.padding(top = 8.dp)) {
                CedalChip(REGIONS[regionIndex])
            }
            Box(
                modifier = Modifier
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                        regionIndex = (regionIndex + 1) % REGIONS.size
                    },
            ) {
                Text("Tap to cycle", color = CedalColors.TextMuted, fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

// --- Security ---

// Ported/extended from cedal-mobile's "Update passcode" row, plus new
// ground covered by this section: a device-without-biometric warning
// nudge, a real biometric-proof-required toggle (not just a stored flag),
// and two-way (email code) verification on top of password sign-in.
@Composable
private fun SecuritySettingsSection(profile: UserProfile?, viewModel: AuthViewModel) {
    // Hoisted here (not local to AppViewOnceToggleRow) so SeeableToggleRow
    // can immediately react when App View Once changes, instead of only
    // picking up the new value next time this screen is opened.
    var appViewOnceEnabled by remember { mutableStateOf(viewModel.storage.appViewOnceEnabled) }

    SettingsSectionCard("Security") {
        if (!viewModel.storage.biometricEnabled) {
            SecurityWarningBanner()
        }
        AppViewOnceToggleRow(viewModel, appViewOnceEnabled) { appViewOnceEnabled = it }
        SeeableToggleRow(viewModel, appViewOnceOverriding = appViewOnceEnabled)
        BiometricToggleRow(viewModel)
        PasscodeChangeRow(viewModel)
        AppLockToggleRow(viewModel)
        TwoFactorRow(profile, viewModel)
        CedalInternalSyncToggleRow()
        if (profile?.isGuest == true) {
            LinkGuestRow(viewModel)
        }
    }
}

// Off by default - the user has to explicitly opt in. See
// CedalInternalSync.kt for the actual mechanism (signature-permission
// broadcast between Cedal's own apps on this device, never user data).
@Composable
private fun CedalInternalSyncToggleRow() {
    val context = androidx.compose.ui.platform.LocalContext.current
    var enabled by remember { mutableStateOf(com.xhacker.cedal.sync.CedalInternalSync.isEnabled(context)) }
    var expanded by remember { mutableStateOf(false) }

    Column {
        SettingsToggleRow(
            "Cedal Internal Sync",
            "Lets Cedal's own apps on this phone (not other apps, not other developers) share small internal security directives with each other - never your data. Off by default.",
            enabled,
        ) { turnOn ->
            enabled = turnOn
            com.xhacker.cedal.sync.CedalInternalSync.setEnabled(context, turnOn)
        }
        Text(
            if (expanded) "Hide details" else "Learn more",
            color = CedalColors.AccentCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier
                .padding(horizontal = 14.dp)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { expanded = !expanded },
        )
        if (expanded) {
            Text(
                "When on, if another app made by Cedal is installed on this same phone, the two can exchange small internal messages - things like a security fix or a config change - so we can react quickly across every Cedal app you have, instead of updating each one separately. This never includes your personal data, messages, or account info. Only apps Cedal itself builds and signs can take part - no other app on your phone can see or join this, and it's entirely under your control here.",
                color = CedalColors.TextSecondary, fontSize = 11.sp,
                modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 4.dp, bottom = 8.dp),
            )
        }
    }
}

// A dedicated section (not buried inside the long Security list) so
// "Friend Hider" is easy to actually find. Delete Account sits down by
// Sign Out instead - both are "leave this account" actions, so they
// belong together rather than split across two different places.
@Composable
private fun PrivacySettingsSection(profile: UserProfile?, viewModel: AuthViewModel) {
    SettingsSectionCard("Privacy") {
        FriendHiderToggleRow(profile, viewModel)
        OfflineModeToggleRow(viewModel)
        BotViewToggleRow(viewModel)
        CornealHiderToggleRow(viewModel)
    }
}

// Off by default, same decoy-toggle pattern as Friend Hider/Offline Mode -
// hides the Corneal bubble everywhere in the app when on.
@Composable
private fun CornealHiderToggleRow(viewModel: AuthViewModel) {
    SettingsToggleRow(
        "Corneal Hider",
        "Hides the Corneal bubble everywhere in the app. Turn this back off to bring it back.",
        com.xhacker.cedal.ui.CornealBubbleState.hiderEnabled,
    ) { turnOn ->
        // Bound directly to the live, Compose-observable flag - see
        // CornealBubbleState.hiderEnabled - not just local row state, so the
        // bubble (rendered elsewhere in the tree, over every member screen)
        // actually reacts immediately instead of only on its next unrelated
        // recomposition.
        com.xhacker.cedal.ui.CornealBubbleState.hiderEnabled = turnOn
        viewModel.storage.cornealHiderEnabled = turnOn
    }
}

// Off by default, same privacy-first posture as Friend Hider/Offline Mode
// above - gates whether the Corneal bubble (see CornealBubbleState) is ever
// given the currently-open chat's content. Purely local; when off, there is
// no code path anywhere that sends chat content to Corneal at all.
@Composable
private fun BotViewToggleRow(viewModel: AuthViewModel) {
    var enabled by remember { mutableStateOf(viewModel.storage.botViewEnabled) }
    SettingsToggleRow(
        "Bot View",
        "Lets the Corneal bubble see the chat you currently have open, so it can help with what's on screen. Off by default - nothing about your chats reaches Corneal unless this is on.",
        enabled,
    ) { turnOn ->
        enabled = turnOn
        viewModel.storage.botViewEnabled = turnOn
    }
}

// A decoy, not a real connectivity setting - when on, Chats/Friends/Requests
// all behave exactly as if this device had no internet (empty lists), even
// though everything else in the app still works normally. Purely local
// (SecureStorage), nothing server-side changes when this is on.
@Composable
private fun OfflineModeToggleRow(viewModel: AuthViewModel) {
    var enabled by remember { mutableStateOf(viewModel.storage.offlineModeEnabled) }
    SettingsToggleRow(
        "Offline Mode",
        "Makes your friends and chats appear empty, as if you had no internet - handy for handing someone your phone.",
        enabled,
    ) { turnOn ->
        enabled = turnOn
        viewModel.storage.offlineModeEnabled = turnOn
    }
}

// "Friend Hider" - excludes this account from every other node's friend
// search/suggestions entirely (see FriendService.search() server-side),
// including an exact name/email match, not just the default "Quick Add"
// list.
@Composable
private fun FriendHiderToggleRow(profile: UserProfile?, viewModel: AuthViewModel) {
    var enabled by remember(profile?.hideFromSearch) { mutableStateOf(profile?.hideFromSearch ?: false) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column {
        SettingsToggleRow(
            "Friend Hider",
            "Hides you from everyone else's friend search and suggestions, even if they search your exact name or email.",
            enabled,
        ) { turnOn ->
            if (loading) return@SettingsToggleRow
            val previous = enabled
            enabled = turnOn
            loading = true
            error = null
            scope.launch {
                val result = viewModel.updateHideFromSearch(turnOn)
                loading = false
                result.onFailure { enabled = previous; error = it.message }
            }
        }
        CedalErrorText(error)
    }
}

// Permanently deletes the account server-side (see AccountService/
// DELETE users/{id}) - gated behind typing the literal word DELETE, same
// bar most apps use for an irreversible action, since a plain confirm
// dialog is too easy to tap through without reading. Also requires
// biometrics/passcode before that confirm UI even opens (same
// AccountVerifyOverlay as Switch Account's per-account delete) - someone
// picking up an unlocked phone shouldn't be able to get this far either.
@Composable
private fun DeleteAccountRow(viewModel: AuthViewModel, onDeleted: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var pendingVerify by remember { mutableStateOf(false) }
    var confirmText by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    if (pendingVerify) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { pendingVerify = false }) {
            AccountVerifyOverlay(
                viewModel = viewModel,
                message = "Deleting your account is permanent and needs your biometrics or passcode.",
                onVerified = { pendingVerify = false; expanded = true; confirmText = ""; error = null },
                onCancel = { pendingVerify = false },
            )
        }
    }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                Text("Delete Account", color = CedalColors.Error, fontSize = 13.sp)
                Text(
                    "Permanently deletes your account and everything tied to it. This can't be undone.",
                    color = CedalColors.TextSecondary, fontSize = 11.sp,
                )
            }
            // A compact button, not CedalGhostButton - that one fills its
            // whole row by design (fine standalone, breaks badly placed
            // inline next to other Row siblings like the label here does -
            // same fix as Pad's Check Code button earlier).
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(CedalColors.CardBackground)
                    .border(1.dp, CedalColors.Error, RoundedCornerShape(50))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                        if (expanded) {
                            expanded = false; confirmText = ""; error = null
                        } else {
                            pendingVerify = true
                        }
                    }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Text(if (expanded) "CANCEL" else "DELETE", color = CedalColors.Error, fontSize = 11.sp, letterSpacing = 0.4.sp)
            }
        }

        if (expanded) {
            Text(
                "Type DELETE below to confirm. This immediately and permanently removes your account, chats, friends, wallet history, and everything else - there is no recovery.",
                color = CedalColors.TextMuted, fontSize = 11.sp, modifier = Modifier.padding(top = 10.dp, bottom = 8.dp),
            )
            CedalTextField(value = confirmText, onValueChange = { confirmText = it }, prefix = "!", placeholder = "DELETE")
            CedalErrorText(error)
            CedalPrimaryButton(
                text = if (loading) "DELETING…" else "PERMANENTLY DELETE MY ACCOUNT",
                enabled = !loading && confirmText.trim() == "DELETE",
                loading = loading,
                modifier = Modifier.padding(top = 8.dp),
                onClick = {
                    loading = true
                    error = null
                    scope.launch {
                        val result = viewModel.deleteAccount()
                        loading = false
                        result.onSuccess { onDeleted() }.onFailure { error = it.message }
                    }
                },
            )
        }
    }
}

// Applied immediately, not just on next launch - MainActivity.onCreate reads
// the same preference, but that only runs once per process. Only clears the
// flag if NEITHER security setting wants it anymore - App View Once and Lock
// on exit share this one system flag.
private fun applyScreenCaptureFlag(activity: Activity?, storage: SecureStorage) {
    val window = activity?.window ?: return
    val currentlySecure = (window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE) != 0
    val shouldBeSecure = storage.shouldBlockScreenCapture
    if (currentlySecure == shouldBeSecure) return // already correct, skip the recreate() flicker below

    if (shouldBeSecure) {
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
    } else {
        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }
    // Android doesn't reliably apply a FLAG_SECURE change to an
    // already-in-progress screenshot/recording capture from just
    // setFlags/clearFlags - the old state can stick around for the rest of
    // that capture session until the window is genuinely torn down and
    // rebuilt (this is what made "close the app fully and reopen" the only
    // thing that worked before). recreate() forces that rebuild immediately.
    // ViewModels (and the login session they hold) survive this the same
    // way they survive a rotation - only this screen's own remembered UI
    // state resets.
    activity.recreate()
}

// Independent of applyScreenCaptureFlag above - see SecureStorage.seeableEnabled
// for why these are deliberately two separate mechanisms. API 31+ only; a
// no-op below that, since there's no way to control just the Recents
// thumbnail on older Android versions (FLAG_SECURE is all-or-nothing there).
private fun applyRecentsScreenshotFlag(activity: Activity?, storage: SecureStorage) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        activity?.setRecentsScreenshotEnabled(storage.seeableEnabled && !storage.appViewOnceEnabled)
    }
}

@Composable
private fun AppViewOnceToggleRow(viewModel: AuthViewModel, enabled: Boolean, onChange: (Boolean) -> Unit) {
    val activity = LocalContext.current as? Activity

    SettingsToggleRow(
        "App View Once",
        "Blocks screenshots and screen recording anywhere in Cedal.",
        enabled,
    ) { turnOn ->
        onChange(turnOn)
        viewModel.storage.appViewOnceEnabled = turnOn
        applyScreenCaptureFlag(activity, viewModel.storage)
        applyRecentsScreenshotFlag(activity, viewModel.storage)
    }
}

// The inverse framing of the same block: whether Recents is allowed to show
// what you were last doing. Disabled (shown greyed, non-interactive) while
// App View Once is on, since that already forces blocking regardless of
// this setting's own value.
@Composable
private fun SeeableToggleRow(viewModel: AuthViewModel, appViewOnceOverriding: Boolean) {
    var enabled by remember { mutableStateOf(viewModel.storage.seeableEnabled) }
    val activity = LocalContext.current as? Activity

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
            Text("Seeable", color = CedalColors.TextPrimary, fontSize = 13.sp)
            Text(
                if (appViewOnceOverriding) {
                    "Locked off while App View Once is on."
                } else {
                    "Let Recents show what you were last doing in Cedal."
                },
                color = CedalColors.TextSecondary, fontSize = 11.sp,
            )
        }
        CedalToggle(
            checked = enabled && !appViewOnceOverriding,
            onCheckedChange = { turnOn ->
                if (!appViewOnceOverriding) {
                    enabled = turnOn
                    viewModel.storage.seeableEnabled = turnOn
                    applyRecentsScreenshotFlag(activity, viewModel.storage)
                }
            },
        )
    }
}

@Composable
private fun SecurityWarningBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(CedalColors.Error.copy(alpha = 0.08f))
                .border(1.dp, CedalColors.Error.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                .padding(10.dp),
        ) {
            Text("⚠", color = CedalColors.Error, fontSize = 14.sp, modifier = Modifier.padding(end = 8.dp))
            Text(
                "Your node isn't fully secured. Turn on biometric unlock below — it also gets you back in if you ever forget your passcode.",
                color = CedalColors.TextSecondary, fontSize = 11.sp,
            )
        }
    }
}

@Composable
private fun BiometricToggleRow(viewModel: AuthViewModel) {
    var enabled by remember { mutableStateOf(viewModel.storage.biometricEnabled) }
    var error by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    Column {
        SettingsToggleRow("Biometric unlock", "Use fingerprint/face to skip typing your passcode next time.", enabled) { turnOn ->
            if (!turnOn) {
                enabled = false
                viewModel.storage.biometricEnabled = false
                error = null
                return@SettingsToggleRow
            }
            // Turning it ON requires an actual successful fingerprint/face
            // prompt — proves the device really has biometrics set up,
            // rather than just flipping a stored flag.
            val activity = context as? FragmentActivity
            if (activity == null) {
                error = "Biometric unlock isn't available here"
                return@SettingsToggleRow
            }
            BiometricAuth.authenticate(
                activity,
                onSuccess = { enabled = true; viewModel.storage.biometricEnabled = true; error = null },
                onError = { error = it },
            )
        }
        CedalErrorText(error)
    }
}

@Composable
private fun AppLockToggleRow(viewModel: AuthViewModel) {
    var enabled by remember { mutableStateOf(viewModel.storage.appLockEnabled) }
    val activity = LocalContext.current as? Activity
    SettingsToggleRow(
        "Lock on exit",
        "Require your passcode or biometric every time you reopen Cedal from the background — like WhatsApp's Screen Lock.",
        enabled,
    ) {
        enabled = it
        viewModel.storage.appLockEnabled = it
        applyScreenCaptureFlag(activity, viewModel.storage)
    }
}

@Composable
private fun TwoFactorRow(profile: UserProfile?, viewModel: AuthViewModel) {
    var enabled by remember(profile?.twoFactorEnabled) { mutableStateOf(profile?.twoFactorEnabled ?: false) }
    var awaitingCode by remember { mutableStateOf(false) }
    var code by remember { mutableStateOf("") }
    var devCodeHint by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val eligible = profile != null && !profile.isGuest && !profile.email.isNullOrBlank()

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp)) {
        Text("Two-way verification", color = CedalColors.TextPrimary, fontSize = 13.sp)
        Text(
            if (!eligible) "Link an email to this node (below) to turn this on."
            else "Requires a one-time code from your email in addition to your password when signing in.",
            color = CedalColors.TextSecondary, fontSize = 11.sp, modifier = Modifier.padding(bottom = 8.dp, top = 2.dp),
        )

        if (!awaitingCode) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CedalToggle(
                    checked = enabled,
                    onCheckedChange = { turnOn ->
                        if (eligible && !loading) {
                            if (!turnOn) {
                                loading = true; error = null
                                scope.launch {
                                    val result = viewModel.disableTwoFactor()
                                    loading = false
                                    result.onSuccess { enabled = false }.onFailure { error = it.message }
                                }
                            } else {
                                loading = true; error = null
                                scope.launch {
                                    val result = viewModel.requestTwoFactorSetup()
                                    loading = false
                                    result.onSuccess { hint -> devCodeHint = hint; awaitingCode = true }.onFailure { error = it.message }
                                }
                            }
                        }
                    },
                )
                if (loading) {
                    Text("Working…", color = CedalColors.TextMuted, fontSize = 11.sp, modifier = Modifier.padding(start = 8.dp))
                }
            }
        } else {
            devCodeHint?.let {
                Text("Dev mode — code: $it", color = CedalColors.TextMuted, fontSize = 10.sp, modifier = Modifier.padding(bottom = 6.dp))
            }
            CedalTextField(
                value = code, onValueChange = { code = it }, prefix = "✓", placeholder = "6-digit code",
                keyboardType = KeyboardType.Number, modifier = Modifier.padding(bottom = 8.dp),
            )
            Row {
                CedalPrimaryButton(
                    text = if (loading) "CONFIRMING…" else "CONFIRM",
                    enabled = !loading,
                    loading = loading,
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                    onClick = {
                        loading = true; error = null
                        scope.launch {
                            val result = viewModel.confirmTwoFactorSetup(code)
                            loading = false
                            result.onSuccess { enabled = true; awaitingCode = false; code = "" }.onFailure { error = it.message }
                        }
                    },
                )
                CedalGhostButton(text = "CANCEL", modifier = Modifier.weight(1f), onClick = { awaitingCode = false; code = ""; error = null })
            }
        }
        CedalErrorText(error)
    }
}

@Composable
private fun PasscodeChangeRow(viewModel: AuthViewModel) {
    var editing by remember { mutableStateOf(false) }
    var code by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var done by remember { mutableStateOf(false) }
    // Setting a NEW passcode needs proof you know the current one first -
    // biometric/passcode, same gate as everything else identity-sensitive.
    var pendingVerify by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    if (pendingVerify) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { pendingVerify = false }) {
            AccountVerifyOverlay(
                viewModel = viewModel,
                message = "Changing your passcode needs your biometrics or current passcode.",
                onVerified = { pendingVerify = false; editing = true; done = false },
                onCancel = { pendingVerify = false },
            )
        }
    }

    if (!editing) {
        SettingsNavRow(label = "Update passcode", description = "Change the code you use to unlock Cedal.", onClick = { pendingVerify = true })
        return
    }

    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
        CedalTextField(
            value = code, onValueChange = { code = it; done = false }, prefix = "★", placeholder = "new passcode",
            isPassword = true, keyboardType = KeyboardType.Number, modifier = Modifier.padding(bottom = 8.dp),
        )
        CedalTextField(
            value = confirm, onValueChange = { confirm = it; done = false }, prefix = "★", placeholder = "confirm passcode",
            isPassword = true, keyboardType = KeyboardType.Number, modifier = Modifier.padding(bottom = 8.dp),
        )
        CedalErrorText(error)
        Row {
            CedalPrimaryButton(
                text = when { loading -> "SAVING…"; done -> "SAVED"; else -> "SAVE" },
                enabled = !loading,
                loading = loading,
                modifier = Modifier.weight(1f).padding(end = 8.dp),
                onClick = {
                    if (code.length !in 4..6) { error = "Passcode must be 4-6 digits"; return@CedalPrimaryButton }
                    if (code != confirm) { error = "Passcodes do not match"; return@CedalPrimaryButton }
                    loading = true; error = null
                    scope.launch {
                        val result = viewModel.updatePasscode(code)
                        loading = false
                        result.onSuccess { done = true; code = ""; confirm = "" }.onFailure { error = it.message }
                    }
                },
            )
            CedalGhostButton(text = "CANCEL", modifier = Modifier.weight(1f), onClick = { editing = false; code = ""; confirm = ""; error = null })
        }
    }
}

