package com.xhacker.cedal.ui.screens.member

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.xhacker.cedal.ui.screens.PermissionBlockedDialog
import com.xhacker.cedal.ui.screens.rememberPermissionGate
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xhacker.cedal.ui.theme.CedalColors
import com.xhacker.cedal.viewmodel.AuthViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Corneal - the "Your personal AI" entry in Chats, styled to match the real
// friend-chat thread (cyan/squared-corner bubbles, "neural transmission"
// input bar) so it reads as one more chat rather than a bolted-on separate
// screen. History now persists server-side (see AiChatHistoryService) and
// supports the same reply/copy/forward/pin/report/edit/delete menu every
// other chat surface has - see ChatActionComponents.kt.

private const val EDIT_WINDOW_MS = 5 * 60 * 1000L

private data class CornealBubble(
    val id: String,
    val role: String,
    val text: String,
    val createdAt: Long,
    val replyToId: String? = null,
    val editedAt: Long? = null,
    val deleted: Boolean = false,
    val mediaUrl: String? = null,
    val mediaType: String? = null,
    val fileName: String? = null,
    // "Call Out" (Settings > Corneal AI, text-based) - see
    // CallOutConfirmCard below.
    val callOutSnippet: String? = null,
    val callOutNote: String? = null,
    val callOutFixRequested: Boolean = false,
)

// "Call Out" (Settings > Corneal AI, text-based) - the highlight/circle
// confirm-deny UI under a Corneal message that flagged a specific snippet in
// the user's open Code file (see CornealChatService's CALLOUT_ tag parsing
// server-side). "That's it" hands the fix to Code AI (if Corneal offered
// to); "Not it" tells the server never to re-suggest this exact snippet in
// this file (see CallOutService).
@Composable
private fun CallOutConfirmCard(snippet: String, note: String?, fixOffered: Boolean, onConfirm: () -> Unit, onReject: () -> Unit) {
    Column(
        modifier = Modifier
            .padding(top = 6.dp)
            .widthIn(max = 280.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(CedalColors.CardBackground)
            .border(1.dp, CedalColors.AccentCyan, RoundedCornerShape(12.dp))
            .padding(12.dp),
    ) {
        Text("📍 \"$snippet\"", color = CedalColors.AccentCyan, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        note?.let { Text(it, color = CedalColors.TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp)) }
        Row(modifier = Modifier.padding(top = 10.dp)) {
            Text(
                if (fixOffered) "THAT'S IT - FIX IT" else "THAT'S IT",
                color = CedalColors.AccentCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onConfirm)
                    .padding(end = 16.dp),
            )
            Text(
                "NOT IT",
                color = CedalColors.TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onReject),
            )
        }
    }
}

private val BUBBLE_MINE_SHAPE = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 2.dp)
private val BUBBLE_THEIRS_SHAPE = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 2.dp, bottomEnd = 16.dp)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CornealChatBody(
    onBack: () -> Unit,
    highlightId: String? = null,
    viewModel: AuthViewModel = hiltViewModel(),
    // Both false when embedded in CornealFloatingWindow - that window
    // already has its own title/close bar (a second "Corneal" header here
    // would be redundant) and already repositions itself above the keyboard
    // (see CornealFloatingWindow's imeBottomPx shift), so applying imePadding
    // here too would double-reserve space for a keyboard the window has
    // already moved clear of, pushing the composer out of the fixed-size
    // window entirely.
    showHeader: Boolean = true,
    applyImePadding: Boolean = true,
) {
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val clipboardManager = LocalClipboardManager.current
    var input by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val greeting = CornealBubble("greeting", "assistant", "Hey, I'm Corneal - your personal AI. Ask me anything about how Cedal works and I'll walk you through it.", 0)
    val messages = remember { mutableStateOf(listOf(greeting)) }

    var replyTarget by remember { mutableStateOf<CornealBubble?>(null) }
    var editingBubble by remember { mutableStateOf<CornealBubble?>(null) }
    var actionsFor by remember { mutableStateOf<CornealBubble?>(null) }
    var forwardingBubble by remember { mutableStateOf<CornealBubble?>(null) }
    var actionNotice by remember { mutableStateOf<String?>(null) }
    var highlightedId by remember { mutableStateOf(highlightId) }
    val context = androidx.compose.ui.platform.LocalContext.current

    // Attachment picker state - see AttachmentSheetOverlay/CameraChoiceOverlay/
    // EmojiChoiceOverlay/StickerPickerOverlay in ChatActionComponents.kt,
    // shared with friend chat rather than a separate copy.
    var attachSheetOpen by remember { mutableStateOf(false) }
    var cameraChoiceOpen by remember { mutableStateOf(false) }
    var emojiChoiceOpen by remember { mutableStateOf(false) }
    var stickerPickerOpen by remember { mutableStateOf(false) }
    var stickerPickerInitialTab by remember { mutableStateOf(StickerPanelTab.EMOJI) }
    var myStickers by remember { mutableStateOf<List<com.xhacker.cedal.data.StickerDto>>(emptyList()) }
    var uploadingSticker by remember { mutableStateOf(false) }
    var uploadingAttachment by remember { mutableStateOf(false) }
    var pendingCameraUri by remember { mutableStateOf<android.net.Uri?>(null) }
    // Voice notes - empty-composer Send becomes a mic icon (see the input
    // row below); tapping it shows VoiceRecorderPanel instead of the normal
    // row. A finished recording is held here, not sent immediately, so you
    // can keep typing and send it together with any text - see send().
    var voiceRecorderActive by remember { mutableStateOf(false) }
    var pendingVoiceFile by remember { mutableStateOf<java.io.File?>(null) }
    var pendingVoiceDurationMs by remember { mutableStateOf(0L) }
    // Shows an explicit "go to Settings" dialog on a "Don't ask again"
    // denial instead of silently doing nothing forever - see
    // PermissionGate.kt.
    val permissionGate = rememberPermissionGate()
    fun openVoiceRecorder() {
        permissionGate.request(context, android.Manifest.permission.RECORD_AUDIO) { voiceRecorderActive = true }
    }

    fun toBubble(dto: com.xhacker.cedal.data.CornealChatMessageDto) =
        CornealBubble(dto.id, dto.role, dto.content, dto.createdAt, dto.replyToId, dto.editedAt, dto.deleted, dto.mediaUrl, dto.mediaType, dto.fileName)

    // "Call Out" - only the live response of the POST that just happened
    // carries these (see CornealChatService.ReplyResult server-side); the
    // history refresh right after would otherwise silently drop them since
    // GET /corneal/chat replays plain stored content.
    fun currentCodeContext() = com.xhacker.cedal.ui.CornealBubbleState.currentCodeContext?.let {
        com.xhacker.cedal.data.CodeContextDto(it.path, it.content)
    }

    // Hydrates the real saved conversation on open (see AiChatHistoryService
    // server-side) - this used to live only in Compose state and reset
    // every time you navigated away or restarted the app.
    LaunchedEffect(Unit) {
        viewModel.getCornealChatHistory().onSuccess { history ->
            if (history.isNotEmpty()) {
                messages.value = history.map(::toBubble)
            }
        }
        viewModel.listMyStickers().onSuccess { myStickers = it }
    }

    // A picked image/video/file uploads and sends as its own message right
    // away (empty caption), same as friend chat's uploadAndSend/sendMedia -
    // no separate "staged attachment" preview.
    fun sendMedia(url: String, mediaType: String, fileName: String?) {
        error = null
        messages.value = messages.value + CornealBubble("pending-${System.currentTimeMillis()}", "user", "", System.currentTimeMillis(), mediaUrl = url, mediaType = mediaType, fileName = fileName)
        sending = true
        scope.launch {
            val chatContext = com.xhacker.cedal.ui.CornealBubbleState.currentChatContext?.let {
                com.xhacker.cedal.data.ChatContextDto(it.friendName, it.recentMessages)
            }
            viewModel.cornealChat("", null, chatContext, currentCodeContext(), url, mediaType, fileName)
                .onSuccess { dto ->
                    viewModel.triggerAchievementFireAndForget("first_corneal_chat")
                    viewModel.getCornealChatHistory().onSuccess { history ->
                        messages.value = history.map(::toBubble).map { b ->
                            if (b.id == dto.id) b.copy(callOutSnippet = dto.callOutSnippet, callOutNote = dto.callOutNote, callOutFixRequested = dto.callOutFixRequested) else b
                        }
                    }
                }
                .onFailure { error = it.message ?: "Couldn't reach Corneal" }
            sending = false
        }
    }

    fun uploadAndSend(uri: android.net.Uri, kind: String, mediaType: String, fallbackFileName: String? = null) {
        uploadingAttachment = true
        scope.launch {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"
            val fName = fallbackFileName ?: queryDisplayName(context, uri)
            if (bytes != null) {
                viewModel.uploadImage(kind, bytes, mimeType)
                    .onSuccess { url -> sendMedia(url, mediaType, fName) }
                    .onFailure { error = it.message }
            }
            uploadingAttachment = false
        }
    }

    val galleryPicker = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.PickMultipleVisualMedia(),
    ) { uris ->
        uris.forEach { uri ->
            val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
            uploadAndSend(uri, "chat_media", if (mimeType.startsWith("video/")) "video" else "image")
        }
    }
    val cameraPhotoLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.TakePicture(),
    ) { success -> pendingCameraUri?.let { uri -> if (success) uploadAndSend(uri, "chat_media", "image") } }
    val cameraVideoLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.CaptureVideo(),
    ) { success -> pendingCameraUri?.let { uri -> if (success) uploadAndSend(uri, "chat_media", "video") } }
    fun newCameraUri(extension: String): android.net.Uri {
        val dir = java.io.File(context.cacheDir, "chat_camera").apply { mkdirs() }
        val file = java.io.File(dir, "capture_${System.currentTimeMillis()}.$extension")
        return androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }
    val filePicker = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocument(),
    ) { uri -> if (uri != null) uploadAndSend(uri, "chat_file", "file") }
    val stickerImagePicker = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        uploadingSticker = true
        scope.launch {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
            if (bytes != null) {
                viewModel.uploadImage("sticker", bytes, mimeType)
                    .onSuccess { url -> viewModel.createSticker(url).onSuccess { sticker -> myStickers = myStickers + sticker } }
                    .onFailure { error = it.message }
            }
            uploadingSticker = false
        }
    }

    // "Call Out (Screen Capture)" (Settings > AI, off by default) - a single
    // on-demand screenshot, not continuous watching (see
    // CallOutCaptureService's doc comment for why). Reuses the same
    // upload-then-cornealChat(image) path as any other picked photo, so
    // Corneal's existing askWithImage vision handles the actual "look at
    // this" reasoning with no server changes needed.
    var captureRequesting by remember { mutableStateOf(false) }
    val projectionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK && result.data != null) {
            com.xhacker.cedal.CallOutCaptureService.onCaptured = { bytes ->
                captureRequesting = false
                if (bytes != null) {
                    scope.launch {
                        viewModel.uploadImage("chat_media", bytes, "image/png")
                            .onSuccess { url -> sendMedia(url, "image", "screen.png") }
                            .onFailure { error = it.message }
                    }
                } else {
                    error = "Couldn't capture your screen"
                }
            }
            val serviceIntent = android.content.Intent(context, com.xhacker.cedal.CallOutCaptureService::class.java).apply {
                putExtra(com.xhacker.cedal.CallOutCaptureService.EXTRA_RESULT_CODE, result.resultCode)
                putExtra(com.xhacker.cedal.CallOutCaptureService.EXTRA_RESULT_DATA, result.data)
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        } else {
            captureRequesting = false
        }
    }
    fun requestScreenCapture() {
        if (captureRequesting) return
        captureRequesting = true
        val projectionManager = context.getSystemService(android.media.projection.MediaProjectionManager::class.java)
        projectionLauncher.launch(projectionManager.createScreenCaptureIntent())
    }

    LaunchedEffect(highlightedId, messages.value.size) {
        val id = highlightedId ?: return@LaunchedEffect
        val index = messages.value.indexOfFirst { it.id == id }
        if (index >= 0) {
            listState.animateScrollToItem(index)
            delay(2000)
            highlightedId = null
        }
    }

    fun send() {
        val text = input.trim()
        val voiceFile = pendingVoiceFile
        if ((text.isBlank() && voiceFile == null) || sending) return
        val editing = editingBubble
        if (editing != null) {
            input = ""
            editingBubble = null
            sending = true
            scope.launch {
                viewModel.editCornealMessage(editing.id, text)
                    .onSuccess { updated -> messages.value = messages.value.map { if (it.id == updated.id) it.copy(text = updated.content, editedAt = updated.editedAt) else it } }
                    .onFailure { error = it.message ?: "Couldn't edit that" }
                sending = false
            }
            return
        }
        val replyId = replyTarget?.id
        input = ""
        replyTarget = null
        pendingVoiceFile = null
        pendingVoiceDurationMs = 0L
        error = null
        messages.value = messages.value + CornealBubble("pending-${System.currentTimeMillis()}", "user", text, System.currentTimeMillis(), replyId)
        sending = true
        scope.launch {
            val chatContext = com.xhacker.cedal.ui.CornealBubbleState.currentChatContext?.let {
                com.xhacker.cedal.data.ChatContextDto(it.friendName, it.recentMessages)
            }
            val codeContext = currentCodeContext()
            val result = if (voiceFile != null) {
                viewModel.uploadImage("chat_media", voiceFile.readBytes(), "audio/mp4a-latm")
                    .mapCatching { url -> viewModel.cornealChat(text, replyId, chatContext, codeContext, url, "audio", voiceFile.name).getOrThrow() }
            } else {
                viewModel.cornealChat(text, replyId, chatContext, codeContext)
            }
            result
                .onSuccess { dto ->
                    viewModel.triggerAchievementFireAndForget("first_corneal_chat")
                    viewModel.getCornealChatHistory().onSuccess { history ->
                        messages.value = history.map(::toBubble).map { b ->
                            if (b.id == dto.id) b.copy(callOutSnippet = dto.callOutSnippet, callOutNote = dto.callOutNote, callOutFixRequested = dto.callOutFixRequested) else b
                        }
                    }
                }
                .onFailure { error = it.message ?: "Couldn't reach Corneal" }
            sending = false
        }
    }

    LaunchedEffect(messages.value.size, sending) {
        if (messages.value.isNotEmpty() && highlightedId == null) listState.animateScrollToItem(messages.value.size - 1)
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).let { if (applyImePadding) it.imePadding() else it }) {
        if (showHeader) MemberBackBar(title = "Corneal", onBack = onBack)

        LazyColumn(state = listState, modifier = Modifier.weight(1f)) {
            itemsIndexed(messages.value, key = { _, bubble -> bubble.id }) { _, bubble ->
                val isMine = bubble.role == "user"
                val replySnippet = bubble.replyToId?.let { rid -> messages.value.firstOrNull { it.id == rid } }
                Box(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp), contentAlignment = if (isMine) Alignment.CenterEnd else Alignment.CenterStart) {
                    Column(horizontalAlignment = if (isMine) Alignment.End else Alignment.Start) {
                        if (bubble.mediaUrl != null && !bubble.deleted) {
                            ChatMediaOrIcon(bubble.mediaUrl, bubble.mediaType, bubble.fileName, isMine, onLongPress = { actionsFor = bubble })
                            if (bubble.text.isNotBlank()) {
                                Box(
                                    modifier = Modifier
                                        .padding(top = 2.dp)
                                        .clip(if (isMine) BUBBLE_MINE_SHAPE else BUBBLE_THEIRS_SHAPE)
                                        .background(if (isMine) CedalColors.AccentCyan else CedalColors.CardBackground)
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                ) {
                                    ChatMessageContent(bubble.text, if (isMine) CedalColors.Background else CedalColors.TextPrimary, viewModel, fontSize = 13.sp)
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .widthIn(max = 280.dp)
                                    .clip(if (isMine) BUBBLE_MINE_SHAPE else BUBBLE_THEIRS_SHAPE)
                                    .background(
                                        when {
                                            bubble.id == highlightedId -> CedalColors.AccentCyan.copy(alpha = 0.35f)
                                            isMine -> CedalColors.AccentCyan
                                            else -> CedalColors.CardBackground
                                        },
                                    )
                                    .let { if (isMine) it else it.border(1.dp, CedalColors.BorderSlate, BUBBLE_THEIRS_SHAPE) }
                                    .combinedClickable(
                                        interactionSource = remember { MutableInteractionSource() }, indication = null,
                                        onClick = {}, onLongClick = { if (!bubble.deleted) actionsFor = bubble },
                                    )
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                            ) {
                                Column {
                                    if (replySnippet != null) {
                                        Text(
                                            replySnippet.text.take(80),
                                            color = if (isMine) CedalColors.Background.copy(alpha = 0.7f) else CedalColors.TextMuted,
                                            fontSize = 12.sp, modifier = Modifier.padding(bottom = 4.dp),
                                        )
                                    }
                                    if (bubble.deleted) {
                                        Text(
                                            androidx.compose.ui.text.AnnotatedString("Message deleted"),
                                            color = if (isMine) CedalColors.Background else CedalColors.TextPrimary,
                                            fontSize = 15.sp,
                                            lineHeight = 21.sp,
                                        )
                                    } else {
                                        ChatMessageContent(bubble.text, if (isMine) CedalColors.Background else CedalColors.TextPrimary, viewModel, fontSize = 15.sp)
                                    }
                                }
                            }
                        }
                        if (bubble.id != "greeting") {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                                Text(formatMessageTime(bubble.createdAt), color = CedalColors.TextMuted, fontSize = 9.sp)
                                if (bubble.editedAt != null && !bubble.deleted) {
                                    Text(" · edited", color = CedalColors.TextMuted, fontSize = 9.sp)
                                }
                                if (!bubble.deleted) {
                                    Text(
                                        "⋮", color = CedalColors.TextMuted, fontSize = 12.sp,
                                        modifier = Modifier
                                            .padding(start = 6.dp)
                                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { actionsFor = bubble },
                                    )
                                }
                            }
                        }
                        bubble.callOutSnippet?.let { snippet ->
                            CallOutConfirmCard(
                                snippet = snippet,
                                note = bubble.callOutNote,
                                fixOffered = bubble.callOutFixRequested,
                                onConfirm = {
                                    messages.value = messages.value.map { if (it.id == bubble.id) it.copy(callOutSnippet = null) else it }
                                    if (bubble.callOutFixRequested) {
                                        val ctx = currentCodeContext()
                                        if (ctx != null) {
                                            messages.value = messages.value + CornealBubble("callout-status-${System.currentTimeMillis()}", "assistant", "🔧 Talking to Code AI...", System.currentTimeMillis())
                                            scope.launch {
                                                viewModel.submitAiRequest(
                                                    "Fix this in ${ctx.path}: ${bubble.callOutNote ?: "the flagged issue"}. The problematic part is exactly: \"$snippet\"",
                                                    currentFile = com.xhacker.cedal.data.AiCurrentFileDto(ctx.path, ctx.content),
                                                ).onSuccess { dto ->
                                                    val summary = dto.summary ?: dto.answerText ?: "Done."
                                                    val statusText = if (dto.fileAction != null) "✅ Code AI has a fix ready: $summary\nOpen Code AI to apply it." else "✅ Code AI says: $summary"
                                                    messages.value = messages.value + CornealBubble("callout-result-${System.currentTimeMillis()}", "assistant", statusText, System.currentTimeMillis())
                                                }.onFailure {
                                                    messages.value = messages.value + CornealBubble("callout-err-${System.currentTimeMillis()}", "assistant", "Couldn't reach Code AI: ${it.message}", System.currentTimeMillis())
                                                }
                                            }
                                        }
                                    }
                                },
                                onReject = {
                                    messages.value = messages.value.map { if (it.id == bubble.id) it.copy(callOutSnippet = null) else it }
                                    val ctx = currentCodeContext()
                                    if (ctx != null) scope.launch { viewModel.rejectCallOut(ctx.path, snippet) }
                                },
                            )
                        }
                    }
                }
            }
            if (sending) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 10.dp)) {
                        CircularProgressIndicator(color = CedalColors.AccentCyan, strokeWidth = 2.dp, modifier = Modifier.size(14.dp))
                        Text("Corneal is thinking…", color = CedalColors.TextMuted, fontSize = 12.sp, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        }

        error?.let { Text(it, color = CedalColors.Error, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp)) }

        replyTarget?.let { target ->
            ComposerContextBanner(label = if (target.role == "user") "Replying to yourself" else "Replying to Corneal", snippet = target.text, onCancel = { replyTarget = null })
        }
        editingBubble?.let {
            ComposerContextBanner(label = "Editing message", snippet = it.text, onCancel = { editingBubble = null; input = "" })
        }

        if (voiceRecorderActive) {
            VoiceRecorderPanel(
                onReady = { file, durationMs ->
                    voiceRecorderActive = false
                    pendingVoiceFile = file
                    pendingVoiceDurationMs = durationMs
                },
                onCancel = { voiceRecorderActive = false },
            )
        } else {
            pendingVoiceFile?.let { file ->
                PendingVoiceChip(pendingVoiceDurationMs, onRemove = { pendingVoiceFile = null; pendingVoiceDurationMs = 0L })
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
                Text("›", color = CedalColors.AccentCyan, fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 8.dp))
                Icon(
                    Icons.Filled.AttachFile,
                    contentDescription = "Attach",
                    tint = CedalColors.AccentCyan,
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .size(22.dp)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { attachSheetOpen = true },
                )
                if (viewModel.storage.callOutScreenCaptureEnabled) {
                    Icon(
                        Icons.Filled.Visibility,
                        contentDescription = "Call Out - let Corneal see your screen",
                        tint = if (captureRequesting) CedalColors.TextMuted else CedalColors.AccentCyan,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(22.dp)
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { requestScreenCapture() },
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(50))
                        .background(CedalColors.CardBackground)
                        .border(1.dp, CedalColors.BorderCyan, RoundedCornerShape(50))
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                ) {
                    if (input.isEmpty()) {
                        Text("Type a neural transmission…", color = CedalColors.TextMuted, fontSize = 15.sp, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    }
                    BasicTextField(
                        value = input,
                        onValueChange = { input = it },
                        textStyle = TextStyle(color = CedalColors.TextPrimary, fontSize = 15.sp),
                        cursorBrush = SolidColor(CedalColors.AccentCyan),
                        maxLines = 6,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                val canSend = (input.isNotBlank() || pendingVoiceFile != null) && !sending
                // Matches every other chat's send button (real chat thread,
                // Cedal System Feed) - same size/glow/icon everywhere. Empty
                // composer (and no pending voice note) swaps Send for Mic.
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(44.dp)
                        .let {
                            if (canSend) {
                                it.shadow(elevation = 8.dp, shape = RoundedCornerShape(14.dp), spotColor = Color(0xFF00FF41), ambientColor = Color(0xFF00FF41))
                            } else {
                                it
                            }
                        }
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (canSend) Color(0xFF00FF41) else CedalColors.CardBackground)
                        .border(1.dp, if (canSend) Color(0xFF00FF41) else CedalColors.BorderSlate, RoundedCornerShape(14.dp))
                        .clickable(
                            enabled = !sending,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = if (canSend) { { send() } } else { { openVoiceRecorder() } },
                        ),
                ) {
                    Icon(
                        if (canSend) Icons.AutoMirrored.Filled.Send else Icons.Filled.Mic,
                        contentDescription = if (canSend) "Send" else "Record a voice note",
                        tint = if (canSend) CedalColors.Background else CedalColors.TextMuted,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }

    actionsFor?.let { bubble ->
        val isMine = bubble.role == "user"
        ChatBubbleActionsOverlay(
            canEdit = isMine && System.currentTimeMillis() - bubble.createdAt < EDIT_WINDOW_MS,
            canDelete = isMine,
            onReply = { replyTarget = bubble; editingBubble = null; actionsFor = null },
            onCopy = { actionsFor = null; clipboardManager.setText(AnnotatedString(bubble.text)) },
            onForward = { forwardingBubble = bubble; actionsFor = null },
            onPin = {
                actionsFor = null
                scope.launch { viewModel.pinMessage(chatType = "corneal", messageId = bubble.id, messageText = bubble.text).onSuccess { actionNotice = "Pinned" } }
            },
            onReport = {
                actionsFor = null
                scope.launch { viewModel.reportMessage("corneal", bubble.id, bubble.text).onSuccess { actionNotice = "Reported for admin review" } }
            },
            onEdit = { editingBubble = bubble; replyTarget = null; input = bubble.text; actionsFor = null },
            onDelete = {
                actionsFor = null
                scope.launch {
                    viewModel.deleteCornealMessage(bubble.id).onSuccess {
                        messages.value = messages.value.map { if (it.id == bubble.id) it.copy(deleted = true) else it }
                    }
                }
            },
            onDismiss = { actionsFor = null },
        )
    }

    forwardingBubble?.let { bubble ->
        ForwardMessageSheet(
            viewModel = viewModel,
            onDismiss = { forwardingBubble = null },
            onPick = { label, key ->
                forwardingBubble = null
                scope.launch {
                    when {
                        key == "corneal" -> viewModel.cornealChat(bubble.text)
                        key == "arc" -> viewModel.arcChat(bubble.text)
                        key == "code" -> viewModel.submitAiRequest(bubble.text)
                        key.startsWith("friend:") -> viewModel.sendMessage(key.removePrefix("friend:"), bubble.text)
                    }
                    actionNotice = "Forwarded to $label"
                }
            },
        )
    }

    actionNotice?.let { notice ->
        LaunchedEffect(notice) { delay(2000); actionNotice = null }
        Box(modifier = Modifier.fillMaxSize().padding(bottom = 100.dp), contentAlignment = Alignment.BottomCenter) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(CedalColors.CardBackground)
                    .border(1.dp, CedalColors.BorderCyan, RoundedCornerShape(50))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            ) {
                Text(notice, color = CedalColors.TextPrimary, fontSize = 13.sp)
            }
        }
    }

    if (attachSheetOpen) {
        AttachmentSheetOverlay(
            onCamera = { attachSheetOpen = false; cameraChoiceOpen = true },
            onEmoji = { attachSheetOpen = false; emojiChoiceOpen = true },
            onFolder = { attachSheetOpen = false; filePicker.launch(arrayOf("*/*")) },
            onDismiss = { attachSheetOpen = false },
        )
    }
    if (cameraChoiceOpen) {
        CameraChoiceOverlay(
            onPhoto = { cameraChoiceOpen = false; pendingCameraUri = newCameraUri("jpg"); pendingCameraUri?.let { cameraPhotoLauncher.launch(it) } },
            onVideo = { cameraChoiceOpen = false; pendingCameraUri = newCameraUri("mp4"); pendingCameraUri?.let { cameraVideoLauncher.launch(it) } },
            onGallery = { cameraChoiceOpen = false; galleryPicker.launch(androidx.activity.result.PickVisualMediaRequest(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageAndVideo)) },
            onDismiss = { cameraChoiceOpen = false },
        )
    }
    if (emojiChoiceOpen) {
        EmojiChoiceOverlay(
            onEmoji = { emojiChoiceOpen = false; stickerPickerInitialTab = StickerPanelTab.EMOJI; stickerPickerOpen = true },
            onSticker = { emojiChoiceOpen = false; stickerPickerInitialTab = StickerPanelTab.STICKERS; stickerPickerOpen = true },
            onIcon = { emojiChoiceOpen = false; stickerPickerInitialTab = StickerPanelTab.ICON; stickerPickerOpen = true },
            onDismiss = { emojiChoiceOpen = false },
        )
    }
    if (stickerPickerOpen) {
        StickerPickerOverlay(
            myStickers = myStickers,
            uploading = uploadingSticker,
            initialTab = stickerPickerInitialTab,
            onPickEmoji = { emoji -> input += emoji; stickerPickerOpen = false },
            onPickSticker = { url ->
                stickerPickerOpen = false
                // An Icon-pack pick is "icon:<Name>" (see ChatMediaOrIcon) -
                // not a real image, so it must NOT be sent as mediaType
                // "image" or the server would try to hand this fake URL to
                // an AI vision call and every provider would fail on it.
                sendMedia(url, if (url.startsWith("icon:")) "icon" else "sticker", null)
            },
            onUploadSticker = { stickerImagePicker.launch(androidx.activity.result.PickVisualMediaRequest(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            onDismiss = { stickerPickerOpen = false },
        )
    }
    permissionGate.PermissionBlockedDialog()
}
