package com.xhacker.cedal.ui.screens.member

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xhacker.cedal.data.FriendSummary
import com.xhacker.cedal.data.GroupDto
import com.xhacker.cedal.data.GroupMemberDto
import com.xhacker.cedal.data.GroupMessageDto
import com.xhacker.cedal.ui.theme.CedalColors
import com.xhacker.cedal.viewmodel.AuthViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Group chat thread - a parallel screen to MemberChatThreadScreen.kt, not a
// refactor of it (that file's friendId/friendName threading is pervasive -
// see the plan doc). Reuses everything actually party-agnostic from that
// file/ChatActionComponents.kt (ChatInputBar, StickerPickerOverlay,
// AttachmentSheetOverlay, PollComposerOverlay, ChatBubbleActionsOverlay,
// ForwardMessageSheet, MediaAttachment, ChatMessageContent) rather than
// duplicating it - but its role-permission UI, view-once flow, and
// SimpleConfirmOverlay-style dialogs ARE duplicated locally (Group-prefixed
// below), matching this codebase's "keep 1-on-1 and group chat fully
// separate" convention (see GroupChatService's own doc comment). Scope:
// text/media/stickers/polls/replies/reactions/edit/delete/view-once - still
// no per-member read receipts, in-app camera capture, or voice notes yet
// (gallery/file picker only).
@Composable
fun GroupChatThreadBody(
    groupId: String,
    groupNameArg: String,
    onBack: () -> Unit,
    onOpenGroupProfile: () -> Unit = {},
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val scope = rememberCoroutineScope()
    val myUserId = viewModel.storage.userId
    val listState = rememberLazyListState()

    // Chat Lock (Group Profile's "Lock This Chat", per-group/per-viewer,
    // local-only) - gates the WHOLE thread behind a biometric prompt before
    // any of its state/effects below run, same "unlock before rendering"
    // idea as the app-wide lock, just scoped to one group.
    var chatUnlocked by remember { mutableStateOf(!viewModel.storage.isGroupLocked(groupId)) }
    if (!chatUnlocked) {
        val lockContext = androidx.compose.ui.platform.LocalContext.current
        GroupChatLockGate(
            onUnlock = {
                val activity = lockContext as? androidx.fragment.app.FragmentActivity
                if (activity != null) {
                    com.xhacker.cedal.ui.screens.BiometricAuth.authenticate(activity, onSuccess = { chatUnlocked = true }, onError = {})
                }
            },
            onBack = onBack,
        )
        return
    }

    var groupName by remember { mutableStateOf(groupNameArg) }
    var group by remember { mutableStateOf<GroupDto?>(null) }
    var friends by remember { mutableStateOf<List<FriendSummary>>(emptyList()) }
    val nameFor = { id: String -> if (id == myUserId) "You" else friends.firstOrNull { it.id == id }?.name ?: id.take(8) }
    var messages by remember { mutableStateOf<List<GroupMessageDto>>(emptyList()) }
    var input by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    var replyTarget by remember { mutableStateOf<GroupMessageDto?>(null) }
    var editingMessage by remember { mutableStateOf<GroupMessageDto?>(null) }
    var actionsForMessage by remember { mutableStateOf<GroupMessageDto?>(null) }
    var forwardingMessage by remember { mutableStateOf<GroupMessageDto?>(null) }

    var attachSheetOpen by remember { mutableStateOf(false) }
    var emojiChoiceOpen by remember { mutableStateOf(false) }
    var stickerPickerOpen by remember { mutableStateOf(false) }
    var stickerPickerInitialTab by remember { mutableStateOf(StickerPanelTab.EMOJI) }
    var pollComposerOpen by remember { mutableStateOf(false) }
    var uploadingAttachment by remember { mutableStateOf(false) }
    var myStickers by remember { mutableStateOf<List<com.xhacker.cedal.data.StickerDto>>(emptyList()) }

    var headerMenuOpen by remember { mutableStateOf(false) }

    // Same per-message toggle as 1-on-1 chat's header menu - applies to the
    // NEXT message sent, resets after each send. See ViewOnceModeOverlay /
    // GroupChatService.sendGroupMessage's clamp logic server-side.
    var viewOnceMode by remember { mutableStateOf<String?>(null) }
    var viewOnceDurationMs by remember { mutableStateOf<Long?>(null) }
    var viewOnceMaxViews by remember { mutableStateOf<Int?>(null) }
    var viewOnceOverlayOpen by remember { mutableStateOf(false) }
    // Round 5 - per-message disappearing, applies to the NEXT message sent
    // (same "armed state" pattern as viewOnceMode above), independent of
    // the group-wide Security-tab disappearing setting. "everyone" = real
    // delete for all after disappearDurationMs; "custom" = hidden only from
    // the sender's own view, everyone else keeps seeing it.
    var disappearMode by remember { mutableStateOf<String?>(null) }
    var disappearDurationMs by remember { mutableStateOf<Long?>(null) }
    var disappearOverlayOpen by remember { mutableStateOf(false) }
    var rulesSheetOpen by remember { mutableStateOf(false) }
    // Multi-tag compose state (Round 4) - a message can tag more than one
    // person ("#mike #leo"). tagPrivateDecided is asked ONCE per message,
    // the first time a # tag is added - see pendingHideAskFor below -
    // Round 5 simplified this: "#" always means private, "@" always means
    // public - no more per-message ask. tagHasPublicPick flips true the
    // moment an "@" pick happens and, since one message has one shared
    // body, forces the WHOLE message public even if "#" tags were already
    // queued (same "any-forces-public" precedent as the Hider downgrade
    // server-side).
    var tagTargets by remember { mutableStateOf(listOf<GroupMemberDto>()) }
    var tagAll by remember { mutableStateOf(false) }
    var tagHasPublicPick by remember { mutableStateOf(false) }
    // Active @/# picker - null when not composing a mention/tag. Set by
    // scanning `input` for a trailing unclosed "@partial" or "#partial" on
    // every keystroke (see the ChatInputBar onInputChange below).
    var mentionTrigger by remember { mutableStateOf<Char?>(null) }
    var mentionQuery by remember { mutableStateOf("") }

    val context = androidx.compose.ui.platform.LocalContext.current

    fun refreshMessages() {
        scope.launch {
            viewModel.getGroupMessages(groupId).onSuccess { fetched ->
                // Keep a message this session already revealed (view-once)
                // visible instead of letting the next poll re-strip it back
                // to locked - same fix as MemberChatThreadScreen.kt's poll loop.
                messages = fetched.map { incoming ->
                    val existing = messages.firstOrNull { it.id == incoming.id }
                    if (incoming.viewOnce && incoming.viewed && existing != null &&
                        (existing.text.isNotEmpty() || existing.mediaUrl != null) && !existing.deleted
                    ) {
                        existing
                    } else {
                        incoming
                    }
                }
            }
        }
    }

    fun revealAndShow(message: GroupMessageDto) {
        scope.launch {
            viewModel.revealGroupMessage(groupId, message.id)
                .onSuccess { revealed -> messages = messages.map { if (it.id == revealed.id) revealed else it } }
                .onFailure { error = it.message ?: "This message can no longer be viewed" }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.getGroup(groupId).onSuccess {
            group = it; groupName = it.name
            if (it.rules?.isNotBlank() == true && !viewModel.storage.hasSeenGroupRules(groupId)) rulesSheetOpen = true
        }
        viewModel.listFriends().onSuccess { friends = it }
    }
    LaunchedEffect(Unit) {
        while (true) {
            refreshMessages()
            delay(3_000)
        }
    }

    // Purge a consumed view-once group message once every non-sender member
    // has exhausted it, same "leave the thread and it's like it never
    // existed" trigger as 1-on-1 chat - onDispose (nav away) and ON_STOP
    // (backgrounding) since this app is one Activity/NavHost. See
    // GroupChatService.purgeConsumedGroupViewOnce.
    androidx.compose.runtime.DisposableEffect(groupId) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_STOP) {
                viewModel.purgeConsumedGroupViewOnceFireAndForget(groupId)
            }
        }
        androidx.lifecycle.ProcessLifecycleOwner.get().lifecycle.addObserver(observer)
        onDispose {
            androidx.lifecycle.ProcessLifecycleOwner.get().lifecycle.removeObserver(observer)
            viewModel.purgeConsumedGroupViewOnceFireAndForget(groupId)
        }
    }

    // Whole-window FLAG_SECURE while this thread holds any view-once
    // message (locked or revealed) - Android can't secure a single
    // composable. Same behavior/rationale as MemberChatThreadScreen.kt.
    val hasViewOnceContent = messages.any { it.viewOnce }
    val secureFlagContext = androidx.compose.ui.platform.LocalContext.current
    androidx.compose.runtime.DisposableEffect(hasViewOnceContent) {
        val activity = secureFlagContext as? android.app.Activity
        if (hasViewOnceContent) {
            activity?.window?.setFlags(
                android.view.WindowManager.LayoutParams.FLAG_SECURE,
                android.view.WindowManager.LayoutParams.FLAG_SECURE,
            )
        }
        onDispose {
            activity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    val galleryPicker = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val mime = context.contentResolver.getType(uri) ?: "image/*"
        val mediaType = if (mime.startsWith("video/")) "video" else "image"
        uploadingAttachment = true
        val mode = viewOnceMode; val durationMs = viewOnceDurationMs; val maxViews = viewOnceMaxViews
        viewOnceMode = null; viewOnceDurationMs = null; viewOnceMaxViews = null
        scope.launch {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            if (bytes != null) {
                viewModel.uploadImage("chat_media", bytes, mime).onSuccess { url ->
                    viewModel.sendGroupMessage(
                        groupId, "", mediaUrl = url, mediaType = mediaType, mediaSizeBytes = bytes.size.toLong(),
                        viewOnce = mode != null, viewOnceMode = mode, viewOnceDurationMs = durationMs, viewOnceMaxViews = maxViews,
                    ).onSuccess { refreshMessages() }
                }
            }
            uploadingAttachment = false
        }
    }
    val filePicker = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val mime = context.contentResolver.getType(uri) ?: "application/octet-stream"
        val name = queryDisplayName(context, uri)
        uploadingAttachment = true
        scope.launch {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            if (bytes != null) {
                viewModel.uploadImage("chat_file", bytes, mime).onSuccess { url ->
                    viewModel.sendGroupMessage(groupId, "", mediaUrl = url, mediaType = "file", fileName = name, mediaSizeBytes = bytes.size.toLong()).onSuccess { refreshMessages() }
                }
            }
            uploadingAttachment = false
        }
    }

    fun send() {
        val text = input.trim()
        if (text.isBlank() || sending) return
        val editing = editingMessage
        val replyId = replyTarget?.id
        val mode = if (editing == null) viewOnceMode else null
        val durationMs = if (editing == null) viewOnceDurationMs else null
        val maxViews = if (editing == null) viewOnceMaxViews else null
        val sendTaggedUserIds = if (editing == null) tagTargets.map { it.userId } else emptyList()
        val sendTagAll = if (editing == null) tagAll else false
        val sendTagPrivate = if (editing == null) (tagTargets.isNotEmpty() && !tagHasPublicPick && !tagAll) else false
        val sendDisappearDurationMs = if (editing == null) disappearDurationMs else null
        val sendDisappearSelfOnly = if (editing == null) disappearMode == "custom" else false
        input = ""; replyTarget = null; editingMessage = null
        viewOnceMode = null; viewOnceDurationMs = null; viewOnceMaxViews = null
        tagTargets = emptyList(); tagAll = false; tagHasPublicPick = false
        disappearMode = null; disappearDurationMs = null
        sending = true
        error = null
        scope.launch {
            val result = if (editing != null) {
                viewModel.editGroupMessage(groupId, editing.id, text)
            } else {
                viewModel.sendGroupMessage(
                    groupId, text, replyToId = replyId,
                    viewOnce = mode != null, viewOnceMode = mode, viewOnceDurationMs = durationMs, viewOnceMaxViews = maxViews,
                    taggedUserIds = sendTaggedUserIds, tagAll = sendTagAll, tagPrivate = sendTagPrivate,
                    disappearDurationMs = sendDisappearDurationMs, disappearSelfOnly = sendDisappearSelfOnly,
                )
            }
            result.onSuccess { refreshMessages() }.onFailure { error = it.message ?: "Couldn't send" }
            sending = false
        }
    }

    fun sendSticker(imageUrlOrEmoji: String, isEmoji: Boolean) {
        stickerPickerOpen = false
        scope.launch {
            viewModel.sendGroupMessage(groupId, imageUrlOrEmoji, isSticker = true).onSuccess { refreshMessages() }
        }
    }

    // imePadding(): MainActivity uses enableEdgeToEdge(), so without this the
    // keyboard overlaps the input bar instead of pushing it up - see
    // MemberChatThreadScreen.kt's identical fix. Was imported but never
    // actually applied here, which is exactly this bug.
    Column(modifier = Modifier.fillMaxSize().background(CedalColors.Background).padding(16.dp).imePadding()) {
        GroupChatHeader(
            groupName = groupName,
            memberCount = group?.members?.size ?: 0,
            onBack = onBack,
            onOpenMenu = { headerMenuOpen = true },
        )

        Box(modifier = Modifier.weight(1f)) {
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                itemsIndexed(messages, key = { _, msg -> msg.id }) { _, msg ->
                    val isMine = msg.senderId == myUserId
                    val replyTo = msg.replyToId?.let { rid -> messages.firstOrNull { it.id == rid } }
                    GroupMessageBubble(
                        message = msg,
                        isMine = isMine,
                        replyTo = replyTo,
                        myUserId = myUserId,
                        nameFor = nameFor,
                        viewModel = viewModel,
                        onLongPress = { actionsForMessage = msg },
                        onRevealViewOnce = { revealAndShow(msg) },
                        onVote = { optionIndex ->
                            scope.launch { viewModel.voteInGroupPoll(groupId, msg.id, optionIndex).onSuccess { refreshMessages() } }
                        },
                    )
                }
                if (messages.isEmpty()) {
                    item {
                        Text("No messages yet - say hi 👋", color = CedalColors.TextMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 24.dp))
                    }
                }
            }
        }

        error?.let { Text(it, color = CedalColors.Error, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp)) }

        (editingMessage ?: replyTarget)?.let { target ->
            ComposerContextBanner(
                label = if (editingMessage != null) "Editing message" else "Replying to ${if (target.senderId == myUserId) "yourself" else "member"}",
                snippet = target.text,
                onCancel = { editingMessage = null; replyTarget = null; input = "" },
            )
        }

        if (uploadingAttachment) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 6.dp)) {
                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = CedalColors.AccentCyan)
                Text("Uploading…", color = CedalColors.TextMuted, fontSize = 12.sp, modifier = Modifier.padding(start = 8.dp))
            }
        }

        if (editingMessage == null && (tagAll || tagTargets.isNotEmpty() || viewOnceMode != null)) {
            // Composer dot - red = View Once armed, yellow = a private (#)
            // tag is queued, blue = normal/public (@) - see the TAG_PRIVATE_YELLOW
            // doc comment below for the full Round-5 color rule.
            val composerIsPrivate = tagTargets.isNotEmpty() && !tagHasPublicPick && !tagAll
            val dotColor = when {
                viewOnceMode != null -> CedalColors.Error
                composerIsPrivate -> TAG_PRIVATE_YELLOW
                else -> CedalColors.AccentCyan
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 6.dp)) {
                Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(50)).background(dotColor))
                val label = when {
                    tagAll -> "@Everyone"
                    tagTargets.isNotEmpty() -> tagTargets.joinToString(" ") { "${if (composerIsPrivate) "#" else "@"}${nameFor(it.userId)}" }
                    else -> "View Once armed"
                }
                Text(label, color = CedalColors.TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f).padding(start = 6.dp))
                if (tagAll || tagTargets.isNotEmpty()) {
                    Text(
                        "CLEAR", color = CedalColors.TextMuted, fontSize = 10.sp,
                        modifier = Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { tagTargets = emptyList(); tagAll = false; tagHasPublicPick = false },
                    )
                }
            }
        }

        // @/# mention-and-tag picker, rendered directly above the input bar
        // (not a full-screen overlay) - live-filtered by whatever's typed
        // after the trigger character. "#" always tags privately, "@"
        // always tags publicly (Round 5 - no more per-message ask).
        if (mentionTrigger != null && editingMessage == null) {
            MentionPickerPanel(
                members = group?.members.orEmpty().filterNot { it.userId == myUserId },
                nameFor = nameFor,
                query = mentionQuery,
                showEveryoneOption = mentionTrigger == '@',
                onPickEveryone = {
                    val triggerIdx = input.lastIndexOf(mentionTrigger!!)
                    input = input.substring(0, triggerIdx) + "@Everyone "
                    tagAll = true; tagTargets = emptyList(); tagHasPublicPick = false
                    mentionTrigger = null; mentionQuery = ""
                },
                onPick = { member ->
                    val trigger = mentionTrigger!!
                    val triggerIdx = input.lastIndexOf(trigger)
                    input = input.substring(0, triggerIdx) + "$trigger${nameFor(member.userId)} "
                    if (!tagAll && member.userId !in tagTargets.map { it.userId }) {
                        tagTargets = tagTargets + member
                        if (trigger == '@') tagHasPublicPick = true
                    }
                    mentionTrigger = null; mentionQuery = ""
                },
                onDismiss = { mentionTrigger = null; mentionQuery = "" },
            )
        }

        ChatInputBar(
            input = input,
            onInputChange = { v ->
                input = v
                val triggerIdx = maxOf(v.lastIndexOf('@'), v.lastIndexOf('#'))
                mentionTrigger = if (triggerIdx == -1) null else {
                    val after = v.substring(triggerIdx + 1)
                    if (after.contains(' ') || after.contains('\n')) null else { mentionQuery = after; v[triggerIdx] }
                }
                if (mentionTrigger == null) mentionQuery = ""
            },
            sending = sending,
            viewOnceMode = viewOnceMode != null,
            onSend = { send() },
            onOpenAttachSheet = { attachSheetOpen = true },
            onOpenRecordSheet = { emojiChoiceOpen = true }, // "›" record slot repurposed - groups have no voice notes yet, so this opens Emoji/Sticker instead of a dead button.
        )
    }

    if (rulesSheetOpen) {
        val g = group
        GroupSimpleConfirmOverlay(
            title = "${groupName}'s rules",
            body = g?.rules.orEmpty(),
            confirmLabel = "GOT IT",
            onConfirm = { rulesSheetOpen = false; viewModel.storage.markGroupRulesSeen(groupId) },
            onDismiss = { rulesSheetOpen = false; viewModel.storage.markGroupRulesSeen(groupId) },
        )
    }

    if (attachSheetOpen) {
        AttachmentSheetOverlay(
            onCamera = { attachSheetOpen = false; galleryPicker.launch(androidx.activity.result.PickVisualMediaRequest(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageAndVideo)) },
            onEmoji = { attachSheetOpen = false; emojiChoiceOpen = true },
            onFolder = { attachSheetOpen = false; filePicker.launch(arrayOf("*/*")) },
            onDismiss = { attachSheetOpen = false },
        )
    }

    if (emojiChoiceOpen) {
        EmojiChoiceOverlay(
            onEmoji = { emojiChoiceOpen = false; stickerPickerInitialTab = StickerPanelTab.EMOJI; stickerPickerOpen = true; scope.launch { viewModel.listMyStickers().onSuccess { myStickers = it } } },
            onSticker = { emojiChoiceOpen = false; stickerPickerInitialTab = StickerPanelTab.STICKERS; stickerPickerOpen = true; scope.launch { viewModel.listMyStickers().onSuccess { myStickers = it } } },
            onIcon = { emojiChoiceOpen = false; stickerPickerInitialTab = StickerPanelTab.ICON; stickerPickerOpen = true },
            onDismiss = { emojiChoiceOpen = false },
        )
    }

    if (stickerPickerOpen) {
        StickerPickerOverlay(
            myStickers = myStickers,
            uploading = false,
            initialTab = stickerPickerInitialTab,
            onPickEmoji = { emoji -> input += emoji; stickerPickerOpen = false },
            onPickSticker = { url -> sendSticker(url, isEmoji = false) },
            onUploadSticker = { error = "Upload a new sticker from a regular chat first - then it'll show up here too." },
            onDismiss = { stickerPickerOpen = false },
        )
    }

    if (pollComposerOpen) {
        PollComposerOverlay(
            sending = sending,
            onCreate = { question, options ->
                sending = true
                scope.launch {
                    viewModel.sendGroupMessage(groupId, "", pollQuestion = question, pollOptions = options)
                        .onSuccess { refreshMessages(); pollComposerOpen = false }
                        .onFailure { error = it.message }
                    sending = false
                }
            },
            onDismiss = { pollComposerOpen = false },
        )
    }

    actionsForMessage?.let { msg ->
        val isMine = msg.senderId == myUserId
        val g = group
        val myRole = g?.members?.firstOrNull { it.userId == myUserId }?.role
        ChatBubbleActionsOverlay(
            showReactions = !msg.deleted,
            canEdit = isMine && !msg.deleted,
            canDelete = isMine && !msg.deleted,
            onReact = { emoji ->
                actionsForMessage = null
                scope.launch { viewModel.reactToGroupMessage(groupId, msg.id, emoji).onSuccess { refreshMessages() } }
            },
            onReply = { replyTarget = msg; editingMessage = null; actionsForMessage = null },
            onCopy = {
                actionsForMessage = null
                val cm = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                cm.setPrimaryClip(android.content.ClipData.newPlainText("message", msg.text))
            },
            onForward = { forwardingMessage = msg; actionsForMessage = null },
            onPin = {
                actionsForMessage = null
                scope.launch { viewModel.pinMessage("group", msg.id, msg.text, chatKey = groupId) }
            },
            onReport = {
                actionsForMessage = null
                scope.launch { viewModel.reportMessage("group", msg.id, msg.text) }
            },
            onEdit = { editingMessage = msg; replyTarget = null; input = msg.text; actionsForMessage = null },
            onDelete = {
                actionsForMessage = null
                scope.launch { viewModel.deleteGroupMessage(groupId, msg.id).onSuccess { refreshMessages() } }
            },
            onDismiss = { actionsForMessage = null },
            onSave = if (g?.securedMode != true && !msg.deleted) {
                {
                    actionsForMessage = null
                    scope.launch { viewModel.saveMessage(msg.text, sourceLabel = groupName, mediaUrl = msg.mediaUrl, mediaType = msg.mediaType, fileName = msg.fileName) }
                }
            } else null,
            onPinForEveryone = if (isAdminTierRole(myRole) && !msg.deleted) {
                { actionsForMessage = null; scope.launch { viewModel.pinGroupMessage(groupId, msg.id).onSuccess { group = it } } }
            } else null,
            onKeep = if (g?.disappearingMessagesDurationMs != null && !msg.kept && !msg.deleted) {
                { actionsForMessage = null; scope.launch { viewModel.keepGroupMessage(groupId, msg.id).onSuccess { refreshMessages() } } }
            } else null,
        )
    }

    forwardingMessage?.let { msg ->
        ForwardMessageSheet(
            onPick = { _, targetKey ->
                val fwdText = msg.text
                scope.launch {
                    when {
                        targetKey == "corneal" -> viewModel.cornealChat(fwdText, null)
                        targetKey == "arc" -> viewModel.arcChat(fwdText, null)
                        targetKey.startsWith("friend:") -> viewModel.sendMessage(targetKey.removePrefix("friend:"), fwdText, null)
                        targetKey.startsWith("group:") -> viewModel.sendGroupMessage(targetKey.removePrefix("group:"), fwdText)
                    }
                }
                forwardingMessage = null
            },
            onDismiss = { forwardingMessage = null },
            viewModel = viewModel,
        )
    }

    if (headerMenuOpen) {
        GroupHeaderMenuOverlay(
            onGroupInfo = { headerMenuOpen = false; onOpenGroupProfile() },
            onToggleViewOnce = { headerMenuOpen = false; viewOnceOverlayOpen = true },
            onToggleDisappearing = { headerMenuOpen = false; disappearOverlayOpen = true },
            onDismiss = { headerMenuOpen = false },
        )
    }

    if (disappearOverlayOpen) {
        GroupDisappearModeOverlay(
            onPickEveryone = { durationMs -> disappearOverlayOpen = false; disappearMode = "everyone"; disappearDurationMs = durationMs },
            onPickCustom = { durationMs -> disappearOverlayOpen = false; disappearMode = "custom"; disappearDurationMs = durationMs },
            onTurnOff = { disappearOverlayOpen = false; disappearMode = null; disappearDurationMs = null },
            onDismiss = { disappearOverlayOpen = false },
        )
    }

    if (viewOnceOverlayOpen) {
        GroupViewOnceModeOverlay(
            onPickCustomTime = { durationMs ->
                viewOnceMode = "custom_time"; viewOnceDurationMs = durationMs; viewOnceMaxViews = null
                viewOnceOverlayOpen = false
            },
            onPickCustomCount = { maxViews ->
                viewOnceMode = "custom_count"; viewOnceMaxViews = maxViews; viewOnceDurationMs = null
                viewOnceOverlayOpen = false
            },
            onTurnOff = {
                viewOnceMode = null; viewOnceDurationMs = null; viewOnceMaxViews = null
                viewOnceOverlayOpen = false
            },
            onDismiss = { viewOnceOverlayOpen = false },
        )
    }
}

@Composable
private fun GroupChatHeader(groupName: String, memberCount: Int, onBack: () -> Unit, onOpenMenu: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
        Text(
            "‹", color = CedalColors.AccentCyan, fontSize = 28.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onBack).padding(end = 10.dp),
        )
        Box(
            modifier = Modifier.size(40.dp).clip(CircleShape).background(CedalColors.BackgroundBlob).border(1.dp, CedalColors.BorderCyan, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.Groups, contentDescription = groupName, tint = CedalColors.AccentCyan, modifier = Modifier.size(18.dp))
        }
        Column(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
            Text(groupName, color = CedalColors.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Text("$memberCount members", color = CedalColors.TextMuted, fontSize = 11.sp)
        }
        Text(
            "⋮", color = CedalColors.TextPrimary, fontSize = 20.sp,
            modifier = Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onOpenMenu).padding(8.dp),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GroupMessageBubble(
    message: GroupMessageDto,
    isMine: Boolean,
    replyTo: GroupMessageDto?,
    myUserId: String?,
    nameFor: (String) -> String,
    viewModel: AuthViewModel,
    onLongPress: () -> Unit,
    onRevealViewOnce: () -> Unit,
    onVote: (Int) -> Unit,
) {
    // Final tag color rule (Round 5 - "# always private, @ always public, no
    // more per-message ask"): yellow = privately tagged; green instead if
    // that SAME message is also View Once (both the badge and the text) -
    // View Once alone needs no color of its own, its lock-card already
    // makes that obvious.
    val tagColor = when {
        message.tagPrivate && message.viewOnce -> CedalColors.Success
        message.tagPrivate -> TAG_PRIVATE_YELLOW
        else -> null
    }
    Box(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp), contentAlignment = if (isMine) Alignment.CenterEnd else Alignment.CenterStart) {
        Column(horizontalAlignment = if (isMine) Alignment.End else Alignment.Start, modifier = Modifier.widthIn(max = 280.dp)) {
            // Sender label above non-mine bubbles - the one genuinely new
            // piece of bubble UI vs 1-on-1, since there's more than one
            // possible "them" in a group.
            if (!isMine) {
                Text(
                    nameFor(message.senderId),
                    color = CedalColors.AccentCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 4.dp, bottom = 2.dp),
                )
            }

            // Tag badge - "#Name" for a specific-user (private) tag,
            // "@Everyone" for a broadcast tag (always public - see
            // GroupMessages.tagAll/tagPrivate). A tagHidden message means
            // the server already omitted its real content for this viewer -
            // see GroupChatService.toDto.
            if (message.tagAll || message.taggedUserIds.isNotEmpty()) {
                Text(
                    if (message.tagAll) "@Everyone" else message.taggedUserIds.joinToString(" ") { "#${nameFor(it)}" },
                    color = tagColor ?: CedalColors.AccentCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 4.dp, bottom = 2.dp),
                )
            }
            if (message.tagHidden) {
                Box(
                    modifier = Modifier
                        .clip(if (isMine) BUBBLE_MINE_SHAPE else BUBBLE_THEIRS_SHAPE)
                        .background(CedalColors.CardBackground)
                        .border(1.dp, CedalColors.BorderSlate, if (isMine) BUBBLE_MINE_SHAPE else BUBBLE_THEIRS_SHAPE)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                ) {
                    Text("Private tag - not visible to you", color = CedalColors.TextMuted, fontSize = 12.sp, fontStyle = FontStyle.Italic)
                }
                return@Column
            }

            // View-once, still locked - server withholds text/mediaUrl until
            // revealed (see GroupChatService.toDto), so an empty body plus
            // the viewOnce flag means "not yet opened by this viewer", not
            // "actually blank". Only a non-sender can tap to reveal; the
            // sender's own copy is never stripped server-side.
            val viewOnceLocked = message.viewOnce && !message.deleted && message.text.isEmpty() && message.mediaUrl == null
            if (viewOnceLocked) {
                GroupViewOnceLockedCard(
                    isMine = isMine,
                    remainingViews = message.viewOnceMaxViews,
                    onClick = { if (!isMine) onRevealViewOnce() },
                    onLongClick = onLongPress,
                )
                GroupMessageFooter(message, onOpenActions = onLongPress)
                return@Column
            }

            if (message.mediaUrl != null && !message.deleted) {
                MediaAttachment(message.mediaUrl!!, message.mediaType, message.fileName, isMine, onLongPress)
                GroupMessageFooter(message, onOpenActions = onLongPress)
                return@Column
            }

            val pollOptions = message.pollOptions
            if (pollOptions != null && !message.deleted) {
                val myVote = message.pollVotes[myUserId]
                val totalVotes = message.pollVotes.size
                Column(
                    modifier = Modifier
                        .widthIn(min = 220.dp)
                        .clip(if (isMine) BUBBLE_MINE_SHAPE else BUBBLE_THEIRS_SHAPE)
                        .background(if (isMine) CedalColors.AccentCyan else CedalColors.CardBackground)
                        .let { if (isMine) it else it.border(1.dp, CedalColors.BorderSlate, BUBBLE_THEIRS_SHAPE) }
                        .combinedClickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {}, onLongClick = onLongPress)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                ) {
                    Text(
                        "📊 ${message.text.ifBlank { "Poll" }}",
                        color = if (isMine) CedalColors.Background else CedalColors.TextPrimary,
                        fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp),
                    )
                    pollOptions.forEachIndexed { index, option ->
                        val count = message.pollVotes.values.count { it == index }
                        val fraction = if (totalVotes > 0) count.toFloat() / totalVotes else 0f
                        val picked = myVote == index
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isMine) CedalColors.Background.copy(alpha = 0.15f) else CedalColors.Background)
                                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onVote(index) }
                                .padding(8.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                Text(if (picked) "🔘" else "⚪", fontSize = 12.sp)
                                Text(option, color = if (isMine) CedalColors.Background else CedalColors.TextPrimary, fontSize = 12.sp, modifier = Modifier.weight(1f).padding(start = 6.dp))
                                Text("$count", color = if (isMine) CedalColors.Background.copy(alpha = 0.7f) else CedalColors.TextMuted, fontSize = 11.sp)
                            }
                            Box(modifier = Modifier.padding(top = 4.dp).fillMaxWidth().size(4.dp).clip(RoundedCornerShape(50)).background(if (isMine) CedalColors.Background.copy(alpha = 0.2f) else CedalColors.BorderSlate)) {
                                Box(modifier = Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).size(4.dp).clip(RoundedCornerShape(50)).background(if (isMine) CedalColors.Background else CedalColors.Success))
                            }
                        }
                    }
                    Text("$totalVotes vote${if (totalVotes == 1) "" else "s"}", color = if (isMine) CedalColors.Background.copy(alpha = 0.7f) else CedalColors.TextMuted, fontSize = 10.sp)
                }
                GroupMessageFooter(message, onOpenActions = onLongPress)
                return@Column
            }

            if (message.isSticker && !message.deleted) {
                val stickerModifier = Modifier
                    .combinedClickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {}, onLongClick = onLongPress)
                    .padding(4.dp)
                if (message.text.startsWith("http")) {
                    coil.compose.AsyncImage(model = message.text, contentDescription = "Sticker", contentScale = androidx.compose.ui.layout.ContentScale.Fit, modifier = stickerModifier.size(120.dp))
                } else {
                    Text(message.text, fontSize = 64.sp, modifier = stickerModifier)
                }
                GroupMessageFooter(message, onOpenActions = onLongPress)
                return@Column
            }

            Box(
                modifier = Modifier
                    .clip(if (isMine) BUBBLE_MINE_SHAPE else BUBBLE_THEIRS_SHAPE)
                    .background(if (isMine) CedalColors.AccentCyan else CedalColors.CardBackground)
                    .let { if (isMine) it else it.border(1.dp, CedalColors.BorderSlate, BUBBLE_THEIRS_SHAPE) }
                    .combinedClickable(interactionSource = remember { MutableInteractionSource() }, indication = null, enabled = !message.deleted, onClick = {}, onLongClick = onLongPress)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Column {
                    if (replyTo != null) {
                        Row(modifier = Modifier.padding(bottom = 6.dp)) {
                            Box(modifier = Modifier.size(width = 3.dp, height = 28.dp).background(CedalColors.Success))
                            Column(modifier = Modifier.padding(start = 6.dp)) {
                                Text(
                                    if (replyTo.senderId == myUserId) "You" else nameFor(replyTo.senderId),
                                    color = if (isMine) CedalColors.Background else CedalColors.Success, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    if (replyTo.deleted) "Message deleted" else replyTo.text,
                                    color = if (isMine) CedalColors.Background.copy(alpha = 0.7f) else CedalColors.TextMuted, fontSize = 11.sp, maxLines = 1,
                                )
                            }
                        }
                    }
                    if (message.deleted) {
                        Text("This message was deleted", color = if (isMine) CedalColors.Background.copy(alpha = 0.7f) else CedalColors.TextMuted, fontSize = 13.sp, fontStyle = FontStyle.Italic)
                    } else {
                        ChatMessageContent(message.text, tagColor ?: (if (isMine) CedalColors.Background else CedalColors.TextPrimary), viewModel, fontSize = 14.sp)
                    }
                }
            }
            GroupMessageFooter(message, onOpenActions = onLongPress)
        }
    }
}

@Composable
private fun GroupMessageFooter(message: GroupMessageDto, onOpenActions: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
        if (message.reactions.isNotEmpty() && !message.deleted) {
            val counts = message.reactions.values.groupingBy { it }.eachCount()
            Row(modifier = Modifier.padding(end = 6.dp)) {
                counts.forEach { (emoji, count) -> Text("$emoji${if (count > 1) " $count" else ""}", fontSize = 11.sp, modifier = Modifier.padding(end = 4.dp)) }
            }
        }
        Text(formatGroupMessageTime(message.sentAt), color = CedalColors.TextMuted, fontSize = 10.sp)
        if (message.editedAt != null && !message.deleted) {
            Text(" · edited", color = CedalColors.TextMuted, fontSize = 10.sp)
        }
    }
}

private fun formatGroupMessageTime(millis: Long): String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(millis))

// Duplicated rather than reused from MemberChatThreadScreen.kt - that file's
// BUBBLE_MINE_SHAPE/BUBBLE_THEIRS_SHAPE are private (file-scoped), matching
// how CornealChatScreen.kt/ArcAssistantScreen.kt/AlucardChatScreen.kt each
// already keep their own private copy rather than sharing one.
// Round-5 tag color: yellow for a private (#) tag, green instead when that
// same message is also View Once - see GroupMessageBubble's tagColor.
private val TAG_PRIVATE_YELLOW = Color(0xFFFFC107)

private val BUBBLE_MINE_SHAPE = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 2.dp)
private val BUBBLE_THEIRS_SHAPE = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 2.dp, bottomEnd = 16.dp)

// Role-aware helper - "admin-tier" is ADMIN+VICE_CREATOR+CREATOR per
// GroupChatService's permission matrix. internal (not private) -
// GroupProfileScreen.kt's own admin-tier check reuses this instead of a
// second inline copy of the same three-role check.
internal fun isAdminTierRole(role: String?): Boolean = role == "CREATOR" || role == "VICE_CREATOR" || role == "ADMIN"

// Mirrors GroupChatService.roleRank server-side - the 4-tier rank behind
// every whoCan* setting (whoCanSendMessages/whoCanEditInfo/whoCanAddMembers/
// whoCanSeeGroupStats/whoCanSendMedia) plus lock enforcement.
internal fun groupRoleRank(role: String?): Int = when (role) {
    "CREATOR" -> 3
    "VICE_CREATOR" -> 2
    "ADMIN" -> 1
    else -> 0
}
internal fun groupMeetsThreshold(role: String?, threshold: String): Boolean = groupRoleRank(role) >= groupRoleRank(threshold)

// Group management (rename/add members/members list/leave) all live in
// Group Profile now, reached via "Group Info" below - this menu only holds
// what's actually specific to being inside the thread itself.
@Composable
private fun GroupHeaderMenuOverlay(
    onGroupInfo: () -> Unit,
    onToggleViewOnce: () -> Unit,
    onToggleDisappearing: () -> Unit,
    onDismiss: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
        contentAlignment = Alignment.TopEnd,
    ) {
        Column(
            modifier = Modifier
                .padding(top = 60.dp, end = 16.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(CedalColors.CardBackground)
                .border(1.dp, CedalColors.BorderCyan, RoundedCornerShape(14.dp))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {})
                .padding(8.dp),
        ) {
            ActionMenuRow(label = "Group Info", onClick = onGroupInfo)
            ActionMenuRow(label = "View Once", onClick = onToggleViewOnce)
            ActionMenuRow(label = "Disappearing Message", onClick = onToggleDisappearing)
        }
    }
}

// Per-message disappearing (Round 5) - armed for the NEXT message sent,
// same pattern as GroupViewOnceModeOverlay. "For Everyone" really deletes
// the message for all viewers after the duration; "Custom" only hides it
// from the SENDER's own view - see GroupChatService.getGroupMessages'
// per-viewer filter.
@Composable
private fun GroupDisappearModeOverlay(
    onPickEveryone: (durationMs: Long) -> Unit,
    onPickCustom: (durationMs: Long) -> Unit,
    onTurnOff: () -> Unit,
    onDismiss: () -> Unit,
) {
    var screen by remember { mutableStateOf("menu") } // "menu" | "everyone" | "custom"
    var amountText by remember { mutableStateOf("1") }
    var unit by remember { mutableStateOf("Hours") } // Minutes | Hours | Days

    fun durationMs(): Long {
        val amount = (amountText.toIntOrNull() ?: 1).coerceAtLeast(1)
        val minutes = when (unit) { "Days" -> amount * 24 * 60; "Hours" -> amount * 60; else -> amount }
        return minutes.toLong() * 60 * 1000
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .padding(28.dp)
                .width(280.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(CedalColors.CardBackground)
                .border(1.dp, CedalColors.BorderCyan, RoundedCornerShape(18.dp))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {})
                .padding(18.dp),
        ) {
            Text("Disappearing Message", color = CedalColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
            when (screen) {
                "menu" -> {
                    Text(
                        "Independent of the group's own disappearing-messages setting - just for this one message.",
                        color = CedalColors.TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(bottom = 14.dp),
                    )
                    GroupViewOnceChoiceRow(label = "For Everyone", description = "Deletes for every member after the duration.", onClick = { screen = "everyone" })
                    GroupViewOnceChoiceRow(label = "Custom", description = "Only disappears from YOUR OWN view - everyone else keeps seeing it.", onClick = { screen = "custom" })
                    GroupViewOnceChoiceRow(label = "Turn Off", description = "Send normally.", onClick = onTurnOff)
                }
                else -> {
                    Text("Disappears after:", color = CedalColors.TextSecondary, fontSize = 11.sp, modifier = Modifier.padding(bottom = 8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).border(1.dp, CedalColors.BorderSlate, RoundedCornerShape(10.dp)).padding(horizontal = 10.dp, vertical = 8.dp),
                        ) {
                            BasicTextField(
                                value = amountText,
                                onValueChange = { if (it.length <= 3 && it.all(Char::isDigit)) amountText = it },
                                singleLine = true,
                                textStyle = TextStyle(color = CedalColors.TextPrimary, fontSize = 14.sp),
                                cursorBrush = SolidColor(CedalColors.AccentCyan),
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                            )
                        }
                        listOf("Minutes", "Hours", "Days").forEach { u ->
                            Text(
                                u.take(3), color = if (unit == u) CedalColors.Background else CedalColors.TextPrimary, fontSize = 11.sp,
                                modifier = Modifier
                                    .padding(start = 4.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(if (unit == u) CedalColors.AccentCyan else Color.Transparent)
                                    .border(1.dp, CedalColors.BorderSlate, RoundedCornerShape(50))
                                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { unit = u }
                                    .padding(horizontal = 7.dp, vertical = 6.dp),
                            )
                        }
                    }
                    Row(modifier = Modifier.padding(top = 16.dp)) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .border(1.dp, CedalColors.BorderSlate, RoundedCornerShape(10.dp))
                                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { screen = "menu" }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center,
                        ) { Text("BACK", color = CedalColors.TextPrimary, fontSize = 12.sp) }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 10.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(CedalColors.AccentCyan)
                                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                    if (screen == "everyone") onPickEveryone(durationMs()) else onPickCustom(durationMs())
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center,
                        ) { Text("SET", color = CedalColors.Background, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }
    }
}

// internal (not private) - GroupProfileScreen.kt's own "Add Members" action
// reuses this rather than a second copy of the same friend-picker sheet.
@Composable
internal fun AddGroupMemberSheet(currentMemberIds: Set<String>, viewModel: AuthViewModel, onAdd: (String) -> Unit, onDismiss: () -> Unit) {
    var friends by remember { mutableStateOf<List<FriendSummary>>(emptyList()) }
    LaunchedEffect(Unit) { viewModel.listFriends().onSuccess { friends = it } }
    val eligible = friends.filterNot { it.id in currentMemberIds }

    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .padding(32.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(CedalColors.CardBackground)
                .border(1.dp, CedalColors.BorderCyan, RoundedCornerShape(18.dp))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {})
                .padding(16.dp),
        ) {
            Text("Add members", color = CedalColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 10.dp))
            if (eligible.isEmpty()) {
                Text("Everyone you're friends with is already in this group.", color = CedalColors.TextMuted, fontSize = 12.sp)
            }
            eligible.forEach { friend ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onAdd(friend.id) }.padding(vertical = 10.dp),
                ) {
                    Icon(Icons.Outlined.Person, contentDescription = null, tint = CedalColors.AccentCyan, modifier = Modifier.size(20.dp))
                    Text(friend.name, color = CedalColors.TextPrimary, fontSize = 13.sp, modifier = Modifier.padding(start = 10.dp))
                }
            }
        }
    }
}

// Kick matrix from GroupChatService.removeMember: CREATOR can kick anyone
// but self (self-leave is a separate path); VICE_CREATOR can kick
// ADMIN/MEMBER only; ADMIN can kick MEMBER only; MEMBER can kick nobody.
// Client-side only for what to show - server re-validates regardless.
// internal (not private) - GroupProfileScreen.kt's member action sheet needs
// this exact matrix too; duplicating permission logic (vs. UI styling) risks
// two copies silently drifting apart, so this one's shared instead.
internal fun canKickRole(myRole: String?, targetRole: String, isSelf: Boolean): Boolean {
    if (isSelf) return false
    return when (myRole) {
        "CREATOR" -> true
        "VICE_CREATOR" -> targetRole == "ADMIN" || targetRole == "MEMBER"
        "ADMIN" -> targetRole == "MEMBER"
        else -> false
    }
}

internal fun groupRoleLabel(role: String): String = when (role) {
    "CREATOR" -> "Creator"
    "VICE_CREATOR" -> "Vice-Creator"
    "ADMIN" -> "Admin"
    else -> "Member"
}

// Duplicated rather than reused from MemberChatThreadScreen.kt - that file's
// SimpleConfirmOverlay/ViewOnce* composables are file-private, and this
// codebase's established convention is to keep 1-on-1 and group chat fully
// separate rather than couple them (see GroupChatService's own doc comment).
// internal (not private) - GroupProfileScreen.kt's own leave-group confirm
// reuses this one rather than a third near-identical copy.
@Composable
internal fun GroupSimpleConfirmOverlay(title: String, body: String, confirmLabel: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .padding(32.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(CedalColors.CardBackground)
                .border(1.dp, CedalColors.BorderCyan, RoundedCornerShape(18.dp))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {})
                .padding(20.dp),
        ) {
            Text(title, color = CedalColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(body, color = CedalColors.TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp, bottom = 16.dp))
            Row {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, CedalColors.BorderSlate, RoundedCornerShape(10.dp))
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss)
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) { Text("CANCEL", color = CedalColors.TextPrimary, fontSize = 12.sp) }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 10.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(CedalColors.Error)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onConfirm)
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) { Text(confirmLabel, color = CedalColors.Background, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

// Resolves a display name down to the first character that's a plain A-Z
// letter, via Unicode NFKD compatibility decomposition - this is what
// actually maps most "stylized" letters (fullwidth, mathematical
// bold/fraktur/etc unicode blocks) back to their plain ASCII equivalent, and
// incidentally also skips any leading non-letter symbol (e.g. "✝Mike" -> 'm')
// since it just scans for the first resolvable letter, not strictly index 0.
// Null means genuinely no resolvable letter anywhere in the name (emoji/
// symbol-only names) - see MentionPickerPanel's Default/Custom split.
private fun indexLetter(name: String): Char? {
    val normalized = java.text.Normalizer.normalize(name, java.text.Normalizer.Form.NFKD)
    return normalized.firstOrNull { it in 'a'..'z' || it in 'A'..'Z' }?.lowercaseChar()
}

private fun matchesMentionQuery(name: String, query: String): Boolean {
    if (query.isBlank()) return true
    val normalizedName = java.text.Normalizer.normalize(name, java.text.Normalizer.Form.NFKD).lowercase()
    return normalizedName.contains(query.lowercase())
}

// The @/# picker panel itself - rendered inline (directly above
// ChatInputBar, see GroupChatThreadBody), not as a full-screen overlay.
// Two sub-tabs: DEFAULT (names with a resolvable letter - live-filtered by
// `query`) and CUSTOM (no resolvable letter at all - browse/scroll only,
// query can't match anything so it's ignored there). Same panel serves both
// the "@" plain-text-mention trigger and the "#" structured-tag trigger -
// the caller (GroupChatThreadBody) decides what picking someone actually
// does via onPick.
@Composable
private fun MentionPickerPanel(
    members: List<GroupMemberDto>,
    nameFor: (String) -> String,
    query: String,
    showEveryoneOption: Boolean,
    onPickEveryone: () -> Unit,
    onPick: (GroupMemberDto) -> Unit,
    onDismiss: () -> Unit,
) {
    var subTab by remember { mutableStateOf("DEFAULT") }
    val named = members.map { it to nameFor(it.userId) }
    val defaultPeople = named.filter { indexLetter(it.second) != null }
    val customPeople = named.filter { indexLetter(it.second) == null }
    val visible = if (subTab == "DEFAULT") defaultPeople.filter { matchesMentionQuery(it.second, query) } else customPeople

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(CedalColors.CardBackground)
            .border(1.dp, CedalColors.BorderCyan, RoundedCornerShape(14.dp))
            .padding(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            listOf("DEFAULT", "CUSTOM").forEach { t ->
                val selected = subTab == t
                Text(
                    t, color = if (selected) CedalColors.Background else CedalColors.TextSecondary, fontSize = 10.sp,
                    modifier = Modifier
                        .padding(end = 6.dp)
                        .clip(RoundedCornerShape(50))
                        .background(if (selected) CedalColors.AccentCyan else Color.Transparent)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { subTab = t }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
            Text(
                "✕", color = CedalColors.TextMuted, fontSize = 12.sp,
                modifier = Modifier.weight(1f).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
                textAlign = androidx.compose.ui.text.style.TextAlign.End,
            )
        }
        Column(modifier = Modifier.heightIn(max = 180.dp).verticalScroll(rememberScrollState())) {
            if (showEveryoneOption) {
                ActionMenuRow(label = "#Everyone", onClick = onPickEveryone)
            }
            if (visible.isEmpty()) {
                Text(
                    if (subTab == "CUSTOM") "No custom-named members." else "No matches.",
                    color = CedalColors.TextMuted, fontSize = 11.sp, modifier = Modifier.padding(vertical = 8.dp),
                )
            }
            visible.forEach { (m, name) -> ActionMenuRow(label = name, onClick = { onPick(m) }) }
        }
    }
}

// Full-screen gate shown instead of the thread while a locked group hasn't
// been unlocked yet this composition - see GroupChatThreadBody's early
// return above. Purely local (SecureStorage.isGroupLocked), no server call.
@Composable
private fun GroupChatLockGate(onUnlock: () -> Unit, onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(CedalColors.Background).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "BACK", color = CedalColors.TextMuted, fontSize = 12.sp,
            modifier = Modifier.fillMaxWidth().padding(bottom = 40.dp).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onBack),
        )
        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("This chat is locked", color = CedalColors.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
            Text("Unlock with your device biometrics or passcode to open it.", color = CedalColors.TextMuted, fontSize = 12.sp)
            Text(
                "UNLOCK", color = CedalColors.Background, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .padding(top = 24.dp)
                    .clip(RoundedCornerShape(50))
                    .background(CedalColors.AccentCyan)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onUnlock)
                    .padding(horizontal = 24.dp, vertical = 12.dp),
            )
        }
    }
}

// Same textured "still locked" card as 1-on-1 chat's ViewOnceLockedCard,
// duplicated for the same file-separation reason as GroupSimpleConfirmOverlay
// above. remainingViews is always null here - GroupMessageDto doesn't expose
// a per-viewer view count client-side (that lives in GroupMessageViews rows
// server-side), unlike 1-on-1's single-recipient viewOnceViewCount field.
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GroupViewOnceLockedCard(isMine: Boolean, remainingViews: Int? = null, onClick: () -> Unit, onLongClick: () -> Unit) {
    val sparkles = remember { List(70) { kotlin.random.Random.nextFloat() to kotlin.random.Random.nextFloat() } }
    Box(
        modifier = Modifier
            .size(width = 150.dp, height = 170.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(
                androidx.compose.ui.graphics.Brush.linearGradient(
                    if (isMine) listOf(CedalColors.AccentCyan, CedalColors.Background) else listOf(CedalColors.CardBackground, CedalColors.Background),
                ),
            )
            .border(1.dp, CedalColors.BorderCyan, RoundedCornerShape(18.dp))
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick,
            ),
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.matchParentSize()) {
            sparkles.forEach { (fx, fy) ->
                drawCircle(color = Color.White.copy(alpha = 0.4f), radius = 1.4.dp.toPx(), center = androidx.compose.ui.geometry.Offset(fx * size.width, fy * size.height))
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.Black.copy(alpha = 0.35f))
                .padding(horizontal = 6.dp, vertical = 3.dp),
        ) {
            Icon(Icons.Filled.Replay, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
            Text((remainingViews ?: 1).coerceAtLeast(0).toString(), color = Color.White, fontSize = 11.sp, modifier = Modifier.padding(start = 3.dp))
        }
        Icon(
            Icons.Filled.LocalFireDepartment,
            contentDescription = if (isMine) "You sent a view once" else "Tap to view once",
            tint = Color.White,
            modifier = Modifier.align(Alignment.Center).size(36.dp),
        )
    }
}

@Composable
private fun GroupViewOnceModeOverlay(
    onPickCustomTime: (durationMs: Long) -> Unit,
    onPickCustomCount: (maxViews: Int) -> Unit,
    onTurnOff: () -> Unit,
    onDismiss: () -> Unit,
) {
    var screen by remember { mutableStateOf("menu") } // "menu" | "count" | "time"
    var countText by remember { mutableStateOf("1") }
    var amountText by remember { mutableStateOf("30") }
    var unit by remember { mutableStateOf("Seconds") } // Seconds | Minutes | Hours | Days
    val maxDurationMs = 7L * 24 * 60 * 60 * 1000

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .padding(28.dp)
                .width(280.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(CedalColors.CardBackground)
                .border(1.dp, CedalColors.BorderCyan, RoundedCornerShape(18.dp))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {})
                .padding(18.dp),
        ) {
            Text("View Once", color = CedalColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))

            when (screen) {
                "menu" -> {
                    Text(
                        "Choose how this message disappears after it's sent.",
                        color = CedalColors.TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(bottom = 14.dp),
                    )
                    GroupViewOnceChoiceRow(label = "View Once", description = "Set how many times it can be viewed.", onClick = { screen = "count" })
                    GroupViewOnceChoiceRow(label = "Time Once", description = "Set how long it stays viewable, up to 7 days.", onClick = { screen = "time" })
                    GroupViewOnceChoiceRow(label = "Turn Off", description = "Send normally, no view-once.", onClick = onTurnOff)
                }
                "count" -> {
                    Text("How many times can it be viewed?", color = CedalColors.TextSecondary, fontSize = 11.sp, modifier = Modifier.padding(bottom = 8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.dp, CedalColors.BorderSlate, RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                    ) {
                        BasicTextField(
                            value = countText,
                            onValueChange = { if (it.length <= 4 && it.all(Char::isDigit)) countText = it },
                            singleLine = true,
                            textStyle = TextStyle(color = CedalColors.TextPrimary, fontSize = 14.sp),
                            cursorBrush = SolidColor(CedalColors.AccentCyan),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        )
                    }
                    Row(modifier = Modifier.padding(top = 16.dp)) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .border(1.dp, CedalColors.BorderSlate, RoundedCornerShape(10.dp))
                                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { screen = "menu" }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center,
                        ) { Text("BACK", color = CedalColors.TextPrimary, fontSize = 12.sp) }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 10.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(CedalColors.AccentCyan)
                                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                    val amount = (countText.toIntOrNull() ?: 1).coerceIn(1, 1000)
                                    onPickCustomCount(amount)
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center,
                        ) { Text("SET", color = CedalColors.Background, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    }
                }
                else -> {
                    Text("Disappears this long after it's first opened (max 7 days).", color = CedalColors.TextSecondary, fontSize = 11.sp, modifier = Modifier.padding(bottom = 8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .border(1.dp, CedalColors.BorderSlate, RoundedCornerShape(10.dp))
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                        ) {
                            BasicTextField(
                                value = amountText,
                                onValueChange = { if (it.length <= 4 && it.all(Char::isDigit)) amountText = it },
                                singleLine = true,
                                textStyle = TextStyle(color = CedalColors.TextPrimary, fontSize = 14.sp),
                                cursorBrush = SolidColor(CedalColors.AccentCyan),
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                            )
                        }
                        listOf("Seconds", "Minutes", "Hours", "Days").forEach { u ->
                            Text(
                                u.take(3), color = if (unit == u) CedalColors.Background else CedalColors.TextPrimary,
                                fontSize = 11.sp,
                                modifier = Modifier
                                    .padding(start = 4.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(if (unit == u) CedalColors.AccentCyan else Color.Transparent)
                                    .border(1.dp, CedalColors.BorderSlate, RoundedCornerShape(50))
                                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { unit = u }
                                    .padding(horizontal = 7.dp, vertical = 6.dp),
                            )
                        }
                    }
                    Row(modifier = Modifier.padding(top = 16.dp)) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .border(1.dp, CedalColors.BorderSlate, RoundedCornerShape(10.dp))
                                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { screen = "menu" }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center,
                        ) { Text("BACK", color = CedalColors.TextPrimary, fontSize = 12.sp) }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 10.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(CedalColors.AccentCyan)
                                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                    val amount = amountText.toIntOrNull() ?: return@clickable
                                    if (amount < 1) return@clickable
                                    val multiplier = when (unit) { "Minutes" -> 60_000L; "Hours" -> 3_600_000L; "Days" -> 86_400_000L; else -> 1_000L }
                                    onPickCustomTime((amount * multiplier).coerceAtMost(maxDurationMs))
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center,
                        ) { Text("SET", color = CedalColors.Background, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupViewOnceChoiceRow(label: String, description: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(vertical = 8.dp),
    ) {
        Text(label, color = CedalColors.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Text(description, color = CedalColors.TextMuted, fontSize = 11.sp)
    }
}
