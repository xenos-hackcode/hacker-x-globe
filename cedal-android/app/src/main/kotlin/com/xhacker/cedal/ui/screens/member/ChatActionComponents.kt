package com.xhacker.cedal.ui.screens.member

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Stop
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import com.xhacker.cedal.data.CodeRunResult
import com.xhacker.cedal.data.FriendSummary
import com.xhacker.cedal.ui.theme.CedalColors
import kotlinx.coroutines.delay
import com.xhacker.cedal.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

// Parses **bold** markers into real bold spans - the AI providers tend to
// emit markdown-style emphasis even though these chat bubbles just render
// plain Text, which used to show the literal asterisks. Bold-only (no
// italics/links/etc.) since that's the only marker the AI reliably uses,
// keeping this simple. Shared by Corneal/ARC/Code AI's chat bubbles.
fun renderInlineMarkdown(text: String): AnnotatedString = buildAnnotatedString {
    var i = 0
    while (i < text.length) {
        val boldStart = text.indexOf("**", i)
        if (boldStart == -1) {
            append(text.substring(i))
            break
        }
        val boldEnd = text.indexOf("**", boldStart + 2)
        if (boldEnd == -1) {
            append(text.substring(i))
            break
        }
        append(text.substring(i, boldStart))
        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
            append(text.substring(boldStart + 2, boldEnd))
        }
        i = boldEnd + 2
    }
}

// Splits a chat message into plain-text and fenced-code-block (```lang ... ```)
// segments, so code the AI (or a user) sends can be rendered in its own
// monospace block with a Run button, instead of as part of the normal text
// flow. Shared by every chat surface - real friend chat, Corneal, ARC's
// Assistant, Code AI.
sealed class ChatContentPart {
    data class TextPart(val text: String) : ChatContentPart()
    data class CodePart(val language: String?, val code: String) : ChatContentPart()
}

private val CODE_FENCE_REGEX = Regex("```([a-zA-Z0-9+#]*)\\n?([\\s\\S]*?)```")

// Catches raw HTML pasted WITHOUT ```html fencing - someone pasting a full
// document doesn't know (or bother with) markdown fence syntax, but it's
// still obviously code and deserves the same code-box + preview treatment.
private val HTML_SNIFF_REGEX = Regex("<!DOCTYPE\\s+html|<html[\\s>]|<head[\\s>]|<body[\\s>]", RegexOption.IGNORE_CASE)

fun parseChatContent(text: String): List<ChatContentPart> {
    val parts = mutableListOf<ChatContentPart>()
    var lastEnd = 0
    for (match in CODE_FENCE_REGEX.findAll(text)) {
        if (match.range.first > lastEnd) {
            parts.add(ChatContentPart.TextPart(text.substring(lastEnd, match.range.first)))
        }
        parts.add(ChatContentPart.CodePart(match.groupValues[1].takeIf { it.isNotBlank() }, match.groupValues[2].trimEnd('\n')))
        lastEnd = match.range.last + 1
    }
    if (lastEnd < text.length) parts.add(ChatContentPart.TextPart(text.substring(lastEnd)))
    if (parts.isEmpty()) parts.add(ChatContentPart.TextPart(text))
    return parts.map { part ->
        if (part is ChatContentPart.TextPart && HTML_SNIFF_REGEX.containsMatchIn(part.text)) {
            ChatContentPart.CodePart("html", part.text.trim())
        } else {
            part
        }
    }
}

// Maps a fenced code block's language tag (as written by a user or an AI,
// e.g. "py", "js", "cpp") to the exact language string CodeExecutionService
// expects server-side. Kotlin is deliberately absent - it doesn't run as a
// snippet anywhere in this app (see MemberCodeScreen.kt), it builds a real
// APK, so a chat code block just displays it without a Run button.
private val RUNNABLE_LANGUAGES: Map<String, String> = mapOf(
    "js" to "JavaScript", "javascript" to "JavaScript",
    "ts" to "TypeScript", "typescript" to "TypeScript",
    "py" to "Python", "python" to "Python", "python3" to "Python",
    "php" to "PHP",
    "rb" to "Ruby", "ruby" to "Ruby",
    "lua" to "Lua",
    "sh" to "Bash", "bash" to "Bash", "shell" to "Bash",
    "c" to "C",
    "cpp" to "C++", "c++" to "C++",
    "go" to "Go", "golang" to "Go",
    "rust" to "Rust", "rs" to "Rust",
    "java" to "Java",
    "csharp" to "C#", "cs" to "C#", "c#" to "C#",
)

// Renders a full chat bubble's text, including any fenced code blocks with
// an inline Run button - replaces a plain Text(renderInlineMarkdown(...))
// call wherever a chat surface wants runnable code support.
@Composable
fun ChatMessageContent(
    text: String,
    textColor: Color,
    viewModel: AuthViewModel,
    fontSize: TextUnit = 14.sp,
) {
    val parts = remember(text) { parseChatContent(text) }
    Column {
        parts.forEach { part ->
            when (part) {
                is ChatContentPart.TextPart -> if (part.text.isNotBlank()) {
                    Text(renderInlineMarkdown(part.text), color = textColor, fontSize = fontSize)
                }
                is ChatContentPart.CodePart -> ChatCodeBlock(part.language, part.code, viewModel)
            }
        }
    }
}

@Composable
private fun ChatCodeBlock(language: String?, code: String, viewModel: AuthViewModel) {
    val runLanguage = language?.lowercase()?.let { RUNNABLE_LANGUAGES[it] }
    // HTML has no stdout/stderr to run and capture - "running" it just
    // means rendering it, so it gets its own Preview toggle (a real WebView)
    // instead of the Run-and-show-output flow the other languages use.
    val isHtml = language.equals("html", ignoreCase = true)
    var running by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<CodeRunResult?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var showHtmlPreview by remember { mutableStateOf(false) }
    var justCopied by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    fun copyCode() {
        clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(code))
        justCopied = true
        scope.launch { delay(1500); justCopied = false }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF0D1420))
            .border(1.dp, CedalColors.BorderSlate, RoundedCornerShape(10.dp))
            .padding(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(
                (language ?: "code").uppercase(),
                color = CedalColors.TextMuted,
                fontSize = 10.sp,
                modifier = Modifier.weight(1f),
            )
            // Every code block gets a copy action, not just runnable ones -
            // this is also how "Cedal Team"'s key-delivery message shows a
            // copyable box (see DeveloperAccessService.sendKeyMessage,
            // which fences the key as ```key ... ```). Both this icon+label
            // AND tapping the code text itself (below) copy - two ways in,
            // same action.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(end = if (runLanguage != null || isHtml) 14.dp else 0.dp)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { copyCode() },
            ) {
                Icon(
                    if (justCopied) Icons.Filled.Check else Icons.Filled.ContentCopy,
                    contentDescription = "Copy",
                    tint = if (justCopied) CedalColors.Success else CedalColors.AccentCyan,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    if (justCopied) "Copied" else "Copy",
                    color = if (justCopied) CedalColors.Success else CedalColors.AccentCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
            if (runLanguage != null) {
                if (running) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = CedalColors.AccentCyan)
                } else {
                    Text(
                        "▶ Run",
                        color = CedalColors.AccentCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                            running = true
                            error = null
                            result = null
                            scope.launch {
                                val res = viewModel.runCode(runLanguage, code, "")
                                running = false
                                res.onSuccess { result = it }.onFailure { error = it.message ?: "Run failed" }
                            }
                        },
                    )
                }
            } else if (isHtml) {
                Text(
                    if (showHtmlPreview) "✕ Close" else "▶ Preview",
                    color = CedalColors.AccentCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                        showHtmlPreview = !showHtmlPreview
                    },
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { copyCode() }
                .horizontalScroll(rememberScrollState()),
        ) {
            Text(code, color = CedalColors.TextPrimary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
        }
        if (isHtml && showHtmlPreview) {
            androidx.compose.ui.viewinterop.AndroidView(
                factory = { ctx -> android.webkit.WebView(ctx) },
                update = { webView -> webView.loadDataWithBaseURL(null, code, "text/html", "utf-8", null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .padding(top = 8.dp)
                    .clip(RoundedCornerShape(8.dp)),
            )
        }
        result?.let { r ->
            Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                if (r.stdout.isNotBlank()) {
                    Box(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        Text(r.stdout, color = CedalColors.Success, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                }
                if (r.stderr.isNotBlank()) {
                    Box(modifier = Modifier.padding(top = 4.dp).horizontalScroll(rememberScrollState())) {
                        Text(r.stderr, color = CedalColors.Error, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                }
                if (r.stdout.isBlank() && r.stderr.isBlank()) {
                    Text("(no output)", color = CedalColors.TextMuted, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
        error?.let {
            Text(it, color = CedalColors.Error, fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp))
        }
    }
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

private enum class VoiceRecorderPhase { RECORDING, PAUSED, REVIEWING }

// Replaces the whole composer while recording/reviewing a voice note -
// shared by every chat surface (real friend chat, Corneal, ARC's Assistant,
// Code AI). Records immediately on entering composition; stopping moves to
// a REVIEWING phase (play/pause + seek-to-start, so you can actually listen
// back before committing) instead of sending right away - onReady is only
// called once you explicitly tap the checkmark, and even then it just hands
// the file up to the caller, which holds it as a pending attachment so you
// can keep typing and send everything together (see each screen's own
// pendingVoiceFile/pendingVoiceDurationMs state + send() logic).
@Composable
fun VoiceRecorderPanel(onReady: (file: java.io.File, durationMs: Long) -> Unit, onCancel: () -> Unit) {
    val context = LocalContext.current
    var phase by remember { mutableStateOf(VoiceRecorderPhase.RECORDING) }
    var recorder by remember { mutableStateOf<android.media.MediaRecorder?>(null) }
    var file by remember { mutableStateOf<java.io.File?>(null) }
    var elapsedMs by remember { mutableStateOf(0L) }
    var recordingStartRealtime by remember { mutableStateOf(0L) }
    var accumulatedBeforePause by remember { mutableStateOf(0L) }

    var player by remember { mutableStateOf<android.media.MediaPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var durationMs by remember { mutableStateOf(0L) }

    LaunchedEffect(Unit) {
        val dir = java.io.File(context.cacheDir, "chat_voice").apply { mkdirs() }
        val f = java.io.File(dir, "voice_${System.currentTimeMillis()}.m4a")
        file = f
        runCatching {
            val r = if (android.os.Build.VERSION.SDK_INT >= 31) {
                android.media.MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                android.media.MediaRecorder()
            }
            r.setAudioSource(android.media.MediaRecorder.AudioSource.MIC)
            r.setOutputFormat(android.media.MediaRecorder.OutputFormat.MPEG_4)
            r.setAudioEncoder(android.media.MediaRecorder.AudioEncoder.AAC)
            r.setOutputFile(f.absolutePath)
            r.prepare()
            r.start()
            recorder = r
            recordingStartRealtime = System.currentTimeMillis()
        }.onFailure { onCancel() }
    }

    LaunchedEffect(phase) {
        while (phase == VoiceRecorderPhase.RECORDING) {
            elapsedMs = accumulatedBeforePause + (System.currentTimeMillis() - recordingStartRealtime)
            delay(200)
        }
    }

    fun pauseRecording() {
        if (phase != VoiceRecorderPhase.RECORDING) return
        runCatching { recorder?.pause() }
        accumulatedBeforePause = elapsedMs
        phase = VoiceRecorderPhase.PAUSED
    }
    fun resumeRecording() {
        if (phase != VoiceRecorderPhase.PAUSED) return
        runCatching { recorder?.resume() }
        recordingStartRealtime = System.currentTimeMillis()
        phase = VoiceRecorderPhase.RECORDING
    }
    fun stopAndReview() {
        runCatching { recorder?.stop(); recorder?.release() }
        recorder = null
        durationMs = elapsedMs
        phase = VoiceRecorderPhase.REVIEWING
    }
    fun togglePlayback() {
        val f = file ?: return
        val existing = player
        if (existing == null) {
            runCatching {
                val p = android.media.MediaPlayer()
                p.setDataSource(f.absolutePath)
                p.setOnCompletionListener { isPlaying = false }
                p.prepare()
                player = p
                p.start()
                isPlaying = true
            }
        } else if (isPlaying) {
            existing.pause()
            isPlaying = false
        } else {
            existing.start()
            isPlaying = true
        }
    }
    fun seekToStart() {
        player?.seekTo(0)
    }

    DisposableEffect(Unit) {
        onDispose {
            runCatching { recorder?.stop(); recorder?.release() }
            player?.release()
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp)
            .clip(RoundedCornerShape(50))
            .background(CedalColors.CardBackground)
            .border(1.dp, if (phase == VoiceRecorderPhase.REVIEWING) CedalColors.AccentCyan else CedalColors.Error, RoundedCornerShape(50))
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        when (phase) {
            VoiceRecorderPhase.RECORDING, VoiceRecorderPhase.PAUSED -> {
                Box(
                    modifier = Modifier.size(10.dp).clip(CircleShape).background(if (phase == VoiceRecorderPhase.RECORDING) CedalColors.Error else CedalColors.TextMuted),
                )
                Text(
                    formatDuration(elapsedMs),
                    color = CedalColors.TextPrimary, fontSize = 14.sp,
                    modifier = Modifier.weight(1f).padding(start = 10.dp),
                )
                Icon(
                    if (phase == VoiceRecorderPhase.RECORDING) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (phase == VoiceRecorderPhase.RECORDING) "Pause recording" else "Resume recording",
                    tint = CedalColors.AccentCyan,
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .size(22.dp)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                            if (phase == VoiceRecorderPhase.RECORDING) pauseRecording() else resumeRecording()
                        },
                )
                Icon(
                    Icons.Filled.Stop,
                    contentDescription = "Stop recording",
                    tint = CedalColors.TextPrimary,
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .size(22.dp)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { stopAndReview() },
                )
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Discard",
                    tint = CedalColors.TextMuted,
                    modifier = Modifier
                        .size(22.dp)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onCancel() },
                )
            }
            VoiceRecorderPhase.REVIEWING -> {
                Icon(
                    if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = CedalColors.AccentCyan,
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .size(22.dp)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { togglePlayback() },
                )
                Icon(
                    Icons.Filled.SkipPrevious,
                    contentDescription = "Back to start",
                    tint = CedalColors.TextMuted,
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .size(22.dp)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { seekToStart() },
                )
                Text(
                    formatDuration(durationMs),
                    color = CedalColors.TextPrimary, fontSize = 14.sp,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Discard",
                    tint = CedalColors.TextMuted,
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .size(22.dp)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onCancel() },
                )
                Icon(
                    Icons.Filled.Check,
                    contentDescription = "Use this recording",
                    tint = CedalColors.Success,
                    modifier = Modifier
                        .size(22.dp)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                            player?.release()
                            player = null
                            file?.let { onReady(it, durationMs) }
                        },
                )
            }
        }
    }
}

// Shared across every chat surface (real friend chat, Corneal, ARC's
// Assistant, Code AI) - generalized from what used to be real chat's own
// private MessageActionsOverlay, since all four now need the same
// forward/copy/pin/report/reply rows, plus edit/delete on your own messages
// and reactions only where that already made sense (real chat).
internal val QUICK_REACTIONS = listOf("👍", "❤️", "😂", "😮", "😢", "🙏")

// A StickerPickerOverlay ICON-tab pick arrives as a mediaUrl of
// "icon:<Name>" (see ALL_ICONS/ICON_BY_NAME in MemberChatThreadScreen.kt) -
// not a real loadable URL, so it needs to render as an actual glyph instead
// of being handed to MediaAttachment's AsyncImage, which would just fail to
// load it. Everything else (a real photo/video/file, or a custom uploaded
// sticker's http URL) still goes through the normal MediaAttachment.
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatMediaOrIcon(mediaUrl: String, mediaType: String?, fileName: String?, isMine: Boolean, onLongPress: () -> Unit) {
    val iconName = mediaUrl.removePrefix("icon:").takeIf { mediaUrl.startsWith("icon:") }
    val icon = iconName?.let { ICON_BY_NAME[it] }
    if (icon != null) {
        Icon(
            icon,
            contentDescription = "Sticker",
            tint = CedalColors.AccentCyan,
            modifier = Modifier
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                    onLongClick = onLongPress,
                )
                .padding(4.dp)
                .size(64.dp),
        )
    } else {
        MediaAttachment(mediaUrl, mediaType, fileName, isMine, onLongPress)
    }
}

@Composable
internal fun ChatBubbleActionsOverlay(
    showReactions: Boolean = false,
    canEdit: Boolean,
    canDelete: Boolean,
    onReact: (String) -> Unit = {},
    onReply: () -> Unit,
    onCopy: () -> Unit,
    onForward: () -> Unit,
    onPin: () -> Unit,
    onReport: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
    // Group-chat-only extras (see GroupChatThreadScreen.kt) - null/absent
    // for 1-on-1 chat, which never passes these, so this overlay's shape is
    // unchanged there.
    onSave: (() -> Unit)? = null,
    onPinForEveryone: (() -> Unit)? = null,
    onKeep: (() -> Unit)? = null,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .padding(32.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(CedalColors.CardBackground)
                .border(1.dp, CedalColors.BorderCyan, RoundedCornerShape(18.dp))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {}) // absorb taps so they don't dismiss
                .padding(16.dp),
        ) {
            if (showReactions) {
                Row(modifier = Modifier.padding(bottom = 14.dp)) {
                    QUICK_REACTIONS.forEach { emoji ->
                        Text(
                            emoji, fontSize = 22.sp,
                            modifier = Modifier
                                .padding(end = 10.dp)
                                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onReact(emoji) },
                        )
                    }
                }
            }
            ActionMenuRow(label = "Reply", onClick = onReply)
            ActionMenuRow(label = "Copy", onClick = onCopy)
            ActionMenuRow(label = "Forward", onClick = onForward)
            ActionMenuRow(label = "Pin", onClick = onPin)
            onSave?.let { ActionMenuRow(label = "Save", onClick = it) }
            onPinForEveryone?.let { ActionMenuRow(label = "Pin for Everyone", onClick = it) }
            onKeep?.let { ActionMenuRow(label = "Keep", onClick = it) }
            if (canEdit) ActionMenuRow(label = "Edit", onClick = onEdit)
            if (canDelete) ActionMenuRow(label = "Delete", color = CedalColors.Error, onClick = onDelete)
            ActionMenuRow(label = "Report", color = CedalColors.Error, onClick = onReport)
        }
    }
}

@Composable
internal fun ActionMenuRow(label: String, color: Color = CedalColors.TextPrimary, onClick: () -> Unit) {
    Text(
        label,
        color = color, fontSize = 14.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(vertical = 10.dp),
    )
}

// The "forward to..." picker - your friends plus the 3 AI assistants, each
// a plain tappable row. Picking one sends the forwarded text there via that
// target's own existing send method (the caller supplies onPick), same as
// if the user had typed it themselves.
@Composable
internal fun ForwardMessageSheet(
    onPick: (targetLabel: String, targetKey: String) -> Unit,
    onDismiss: () -> Unit,
    viewModel: AuthViewModel,
) {
    var friends by remember { mutableStateOf<List<FriendSummary>?>(null) }
    LaunchedEffect(Unit) {
        viewModel.listFriends().onSuccess { friends = it }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .padding(32.dp)
                .heightIn(max = 420.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(CedalColors.CardBackground)
                .border(1.dp, CedalColors.BorderCyan, RoundedCornerShape(18.dp))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {})
                .padding(16.dp),
        ) {
            Text("FORWARD TO", color = CedalColors.TextSecondary, fontSize = 11.sp, letterSpacing = 2.sp, modifier = Modifier.padding(bottom = 10.dp))
            LazyColumn {
                items(listOf("Corneal" to "corneal", "ARC's Assistant" to "arc", "Code AI" to "code")) { (label, key) ->
                    ForwardTargetRow(label) { onPick(label, key) }
                }
                val currentFriends = friends
                if (currentFriends == null) {
                    item {
                        Row(modifier = Modifier.padding(vertical = 12.dp)) {
                            CircularProgressIndicator(color = CedalColors.AccentCyan, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                        }
                    }
                } else {
                    items(currentFriends, key = { it.id }) { friend ->
                        ForwardTargetRow(friend.name) { onPick(friend.name, "friend:${friend.id}") }
                    }
                }
            }
        }
    }
}

@Composable
private fun ForwardTargetRow(label: String, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(vertical = 10.dp),
    ) {
        Box(
            modifier = Modifier.size(30.dp).clip(CircleShape).background(CedalColors.BackgroundBlob).border(1.dp, CedalColors.BorderCyan, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(label.take(1).uppercase(), color = CedalColors.AccentCyan, fontSize = 12.sp)
        }
        Text(label, color = CedalColors.TextPrimary, fontSize = 14.sp, modifier = Modifier.padding(start = 12.dp))
    }
}
