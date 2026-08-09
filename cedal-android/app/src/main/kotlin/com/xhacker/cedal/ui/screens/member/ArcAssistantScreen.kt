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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xhacker.cedal.ui.theme.CedalColors
import com.xhacker.cedal.viewmodel.AuthViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ARC's "Assistant" tab - a real AI chat the user can ask cybersecurity
// questions and get guided help from, on top of (not instead of) Learn's
// written lessons. Same underlying AiProviderService every other AI feature
// in this app uses - see ArcChatService server-side for the tutor framing
// and legal/ethical guardrails baked into every reply. History persists
// server-side and supports the same reply/copy/forward/pin/report/edit/
// delete menu every other chat surface has - see ChatActionComponents.kt.

private const val EDIT_WINDOW_MS = 5 * 60 * 1000L

// Matches CornealChatBody's bubble shapes exactly (asymmetric rounded
// corners, solid fill for your own messages) so ARC's Assistant chat looks
// visually consistent with Cedal's other AI assistant.
private val BUBBLE_MINE_SHAPE = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 2.dp)
private val BUBBLE_THEIRS_SHAPE = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 2.dp, bottomEnd = 16.dp)

private data class ArcChatBubble(
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
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ArcAssistantBody(onBack: () -> Unit, highlightId: String? = null, viewModel: AuthViewModel) {
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val clipboardManager = LocalClipboardManager.current
    var input by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val greeting = ArcChatBubble(
        "greeting", "assistant",
        "Hey! I'm ARC's assistant. Ask me anything about ethical hacking, networking, or what a lesson meant - I'll only ever help you learn this legally.",
        0,
    )
    val messages = remember { mutableStateOf(listOf(greeting)) }

    var replyTarget by remember { mutableStateOf<ArcChatBubble?>(null) }
    var editingBubble by remember { mutableStateOf<ArcChatBubble?>(null) }
    var actionsFor by remember { mutableStateOf<ArcChatBubble?>(null) }
    var forwardingBubble by remember { mutableStateOf<ArcChatBubble?>(null) }
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

    fun toBubble(dto: com.xhacker.cedal.data.ArcChatMessageDto) =
        ArcChatBubble(dto.id, dto.role, dto.content, dto.createdAt, dto.replyToId, dto.editedAt, dto.deleted, dto.mediaUrl, dto.mediaType, dto.fileName)

    // Hydrates the real saved conversation on open (see AiChatHistoryService
    // server-side) - this used to live only in Compose state and reset
    // every time you navigated away or restarted the app.
    LaunchedEffect(Unit) {
        viewModel.getArcChatHistory().onSuccess { history ->
            if (history.isNotEmpty()) {
                messages.value = history.map(::toBubble)
            }
        }
        viewModel.listMyStickers().onSuccess { myStickers = it }
    }

    fun sendMedia(url: String, mediaType: String, fileName: String?) {
        errorMessage = null
        messages.value = messages.value + ArcChatBubble("pending-${System.currentTimeMillis()}", "user", "", System.currentTimeMillis(), mediaUrl = url, mediaType = mediaType, fileName = fileName)
        sending = true
        scope.launch {
            viewModel.arcChat("", null, url, mediaType, fileName)
                .onSuccess {
                    viewModel.triggerAchievementFireAndForget("first_arc_chat")
                    viewModel.getArcChatHistory().onSuccess { history -> messages.value = history.map(::toBubble) }
                }
                .onFailure { errorMessage = it.message ?: "Couldn't reach the assistant" }
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
                    .onFailure { errorMessage = it.message }
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
                    .onFailure { errorMessage = it.message }
            }
            uploadingSticker = false
        }
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
                viewModel.editArcMessage(editing.id, text)
                    .onSuccess { updated -> messages.value = messages.value.map { if (it.id == updated.id) it.copy(text = updated.content, editedAt = updated.editedAt) else it } }
                    .onFailure { errorMessage = it.message ?: "Couldn't edit that" }
                sending = false
            }
            return
        }
        val replyId = replyTarget?.id
        input = ""
        replyTarget = null
        pendingVoiceFile = null
        pendingVoiceDurationMs = 0L
        errorMessage = null
        messages.value = messages.value + ArcChatBubble("pending-${System.currentTimeMillis()}", "user", text, System.currentTimeMillis(), replyId)
        sending = true
        scope.launch {
            val result = if (voiceFile != null) {
                viewModel.uploadImage("chat_media", voiceFile.readBytes(), "audio/mp4a-latm")
                    .mapCatching { url -> viewModel.arcChat(text, replyId, url, "audio", voiceFile.name).getOrThrow() }
            } else {
                viewModel.arcChat(text, replyId)
            }
            result
                .onSuccess {
                    viewModel.triggerAchievementFireAndForget("first_arc_chat")
                    viewModel.getArcChatHistory().onSuccess { history -> messages.value = history.map(::toBubble) }
                }
                .onFailure { errorMessage = it.message ?: "Couldn't reach the assistant" }
            sending = false
        }
    }

    LaunchedEffect(messages.value.size, sending) {
        if (messages.value.isNotEmpty() && highlightedId == null) listState.animateScrollToItem(messages.value.size - 1)
    }

    Column(modifier = Modifier.fillMaxSize().imePadding()) {
        MemberBackBar(title = "ARC", onBack = onBack)
        Text("ASSISTANT", color = CedalColors.TextSecondary, fontSize = 11.sp, letterSpacing = 2.sp, modifier = Modifier.padding(bottom = 10.dp))

        LazyColumn(state = listState, modifier = Modifier.weight(1f)) {
            itemsIndexed(messages.value, key = { _, bubble -> bubble.id }) { _, bubble ->
                val isUser = bubble.role == "user"
                val replySnippet = bubble.replyToId?.let { rid -> messages.value.firstOrNull { it.id == rid } }
                Box(modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp), contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart) {
                    Column(horizontalAlignment = if (isUser) Alignment.End else Alignment.Start) {
                        if (bubble.mediaUrl != null && !bubble.deleted) {
                            ChatMediaOrIcon(bubble.mediaUrl, bubble.mediaType, bubble.fileName, isUser, onLongPress = { actionsFor = bubble })
                            if (bubble.text.isNotBlank()) {
                                Box(
                                    modifier = Modifier
                                        .padding(top = 4.dp)
                                        .clip(if (isUser) BUBBLE_MINE_SHAPE else BUBBLE_THEIRS_SHAPE)
                                        .background(if (isUser) CedalColors.AccentCyan else CedalColors.CardBackground)
                                        .padding(12.dp),
                                ) {
                                    ChatMessageContent(bubble.text, if (isUser) CedalColors.Background else CedalColors.TextPrimary, viewModel, fontSize = 13.sp)
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .widthIn(max = 280.dp)
                                    .clip(if (isUser) BUBBLE_MINE_SHAPE else BUBBLE_THEIRS_SHAPE)
                                    .background(
                                        when {
                                            bubble.id == highlightedId -> CedalColors.AccentCyan.copy(alpha = 0.35f)
                                            isUser -> CedalColors.AccentCyan
                                            else -> CedalColors.CardBackground
                                        },
                                    )
                                    .let { if (isUser) it else it.border(1.dp, CedalColors.BorderSlate, BUBBLE_THEIRS_SHAPE) }
                                    .combinedClickable(
                                        interactionSource = remember { MutableInteractionSource() }, indication = null,
                                        onClick = {}, onLongClick = { if (!bubble.deleted) actionsFor = bubble },
                                    )
                                    .padding(12.dp),
                            ) {
                                Column {
                                    if (replySnippet != null) {
                                        Text(
                                            replySnippet.text.take(80),
                                            color = if (isUser) CedalColors.Background.copy(alpha = 0.7f) else CedalColors.TextMuted,
                                            fontSize = 11.sp, modifier = Modifier.padding(bottom = 4.dp),
                                        )
                                    }
                                    if (bubble.deleted) {
                                        Text(AnnotatedString("Message deleted"), color = if (isUser) CedalColors.Background else CedalColors.TextPrimary, fontSize = 13.sp, lineHeight = 18.sp)
                                    } else {
                                        ChatMessageContent(bubble.text, if (isUser) CedalColors.Background else CedalColors.TextPrimary, viewModel, fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                        if (bubble.id != "greeting" && !bubble.deleted) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                                Text(formatMessageTime(bubble.createdAt), color = CedalColors.TextMuted, fontSize = 9.sp)
                                if (bubble.editedAt != null) Text(" · edited", color = CedalColors.TextMuted, fontSize = 9.sp)
                                Text(
                                    "⋮", color = CedalColors.TextMuted, fontSize = 12.sp,
                                    modifier = Modifier
                                        .padding(start = 6.dp)
                                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { actionsFor = bubble },
                                )
                            }
                        }
                    }
                }
            }
            if (sending) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 10.dp)) {
                        CircularProgressIndicator(color = CedalColors.AccentCyan, strokeWidth = 2.dp, modifier = Modifier.size(14.dp))
                        Text("Thinking…", color = CedalColors.TextMuted, fontSize = 12.sp, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        }

        errorMessage?.let {
            Text(it, color = CedalColors.Error, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))
        }

        replyTarget?.let { target ->
            ComposerContextBanner(label = if (target.role == "user") "Replying to yourself" else "Replying to ARC", snippet = target.text, onCancel = { replyTarget = null })
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
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(44.dp)
                        .let {
                            if (canSend) {
                                it.shadow(elevation = 8.dp, shape = RoundedCornerShape(14.dp), spotColor = androidx.compose.ui.graphics.Color(0xFF00FF41), ambientColor = androidx.compose.ui.graphics.Color(0xFF00FF41))
                            } else {
                                it
                            }
                        }
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (canSend) androidx.compose.ui.graphics.Color(0xFF00FF41) else CedalColors.CardBackground)
                        .border(1.dp, if (canSend) androidx.compose.ui.graphics.Color(0xFF00FF41) else CedalColors.BorderSlate, RoundedCornerShape(14.dp))
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
                scope.launch { viewModel.pinMessage(chatType = "arc", messageId = bubble.id, messageText = bubble.text).onSuccess { actionNotice = "Pinned" } }
            },
            onReport = {
                actionsFor = null
                scope.launch { viewModel.reportMessage("arc", bubble.id, bubble.text).onSuccess { actionNotice = "Reported for admin review" } }
            },
            onEdit = { editingBubble = bubble; replyTarget = null; input = bubble.text; actionsFor = null },
            onDelete = {
                actionsFor = null
                scope.launch {
                    viewModel.deleteArcMessage(bubble.id).onSuccess {
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
