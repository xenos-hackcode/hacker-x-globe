package com.xhacker.cedal.ui.screens.member

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.xhacker.cedal.data.ChatMessageDto
import com.xhacker.cedal.data.UserProfile
import com.xhacker.cedal.ui.theme.CedalColors
import com.xhacker.cedal.ui.theme.CedalErrorText
import com.xhacker.cedal.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

// Read-only counterpart to MemberProfileBody (which is always the signed-in
// user's own, editable profile) - reached by tapping a friend's name in the
// chat header. Shows only the public-facing fields, not account internals
// like email/2FA that MemberProfileBody's own-profile view exposes.
//
// ACTIONS section below mirrors the chat thread's own header ⋮ menu
// (ChatHeaderMenuOverlay in MemberChatThreadScreen.kt) minus View Once and
// Pin (both meaningfully tied to being inside the thread itself, not
// something you'd reach for from a profile) - same overlays/viewModel calls
// reused directly (widened to internal there) rather than duplicated, since
// this is the exact same friendId/action, just a second entry point.
@Composable
fun MemberFriendProfileBody(userId: String, onBack: () -> Unit, onNavigate: (String) -> Unit = {}, viewModel: AuthViewModel = hiltViewModel()) {
    var profile by remember { mutableStateOf<UserProfile?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var deleteConfirming by remember { mutableStateOf(false) }
    var deleted by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var isBlocked by remember { mutableStateOf(false) }
    var searchOpen by remember { mutableStateOf(false) }
    var reportConfirmOpen by remember { mutableStateOf(false) }
    var blockConfirmOpen by remember { mutableStateOf(false) }
    var deleteChatConfirmOpen by remember { mutableStateOf(false) }
    var actionNotice by remember { mutableStateOf<String?>(null) }
    var messages by remember { mutableStateOf<List<ChatMessageDto>>(emptyList()) }
    var messagesLoaded by remember { mutableStateOf(false) }
    // What this friend has chosen to show via Settings > Security >
    // Popularity - not an action, actual display content, so it's rendered
    // inline below rather than living in the ACTIONS menu (that menu's own
    // "Popularity" row, in the chat thread's ⋮ menu, is the settings editor
    // for what YOU show THEM - a different direction entirely).
    var activeBadge by remember { mutableStateOf<com.xhacker.cedal.data.AchievementDto?>(null) }

    LaunchedEffect(userId) {
        viewModel.getProfileFor(userId).onSuccess { p ->
            profile = p
            val badgeKey = p.activeBadgeKey
            if (badgeKey != null) {
                viewModel.listAchievements().onSuccess { list -> activeBadge = list.firstOrNull { it.key == badgeKey } }
            }
        }.onFailure { error = it.message }
        viewModel.isBlocked(userId).onSuccess { isBlocked = it }
    }

    // Search/Export need the actual message history, which (unlike the chat
    // thread itself) this screen has no other reason to load - fetched once,
    // lazily, the first time either action is used.
    suspend fun ensureMessagesLoaded(): List<ChatMessageDto> {
        if (!messagesLoaded) {
            viewModel.getMessages(userId).onSuccess { messages = it; messagesLoaded = true }
        }
        return messages
    }

    val displayNameForActions = profile?.nickname?.takeIf { it.isNotBlank() } ?: profile?.email?.substringBefore("@") ?: "Cedal user"

    // Same shape as MemberChatThreadScreen.kt's own exportChat() - not
    // reused directly since that one closes over the thread's local
    // `messages`/`friendName` state rather than taking them as params.
    fun exportChat() {
        val transcript = messages
            .filterNot { it.deleted }
            .joinToString("\n") { msg ->
                val who = if (msg.senderId == viewModel.storage.userId) "You" else displayNameForActions
                val body = when {
                    msg.mediaType != null -> "[${msg.mediaType}]" + if (msg.text.isNotBlank()) " ${msg.text}" else ""
                    else -> msg.text
                }
                "[${formatMessageTime(msg.sentAt)}] $who: $body"
            }
        val dir = java.io.File(context.cacheDir, "chat_exports").apply { mkdirs() }
        val file = java.io.File(dir, "chat_with_${displayNameForActions.replace(Regex("[^A-Za-z0-9]"), "_")}.txt")
        file.writeText(transcript)
        val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(android.content.Intent.createChooser(intent, "Export chat"))
    }

    fun addShortcut() {
        val shortcutIntent = android.content.Intent(context, com.xhacker.cedal.MainActivity::class.java).apply {
            action = android.content.Intent.ACTION_VIEW
            putExtra("open_chat_friend_id", userId)
            putExtra("open_chat_friend_name", displayNameForActions)
        }
        val shortcut = androidx.core.content.pm.ShortcutInfoCompat.Builder(context, "chat_$userId")
            .setShortLabel(displayNameForActions)
            .setIcon(androidx.core.graphics.drawable.IconCompat.createWithResource(context, android.R.drawable.sym_action_chat))
            .setIntent(shortcutIntent)
            .build()
        androidx.core.content.pm.ShortcutManagerCompat.requestPinShortcut(context, shortcut, null)
    }

    Column(modifier = Modifier.fillMaxSize().background(CedalColors.Background).padding(16.dp)) {
        MemberBackBar(title = "Profile", onBack = onBack)
        CedalErrorText(error)

        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            val p = profile
            if (p == null) {
                Text("Loading…", color = CedalColors.TextMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 24.dp))
                return@Column
            }
            val displayName = p.nickname?.takeIf { it.isNotBlank() } ?: p.email?.substringBefore("@") ?: "Cedal user"
            val initial = displayName.firstOrNull()?.uppercaseChar()?.toString() ?: "?"

            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(CedalColors.BackgroundBlob)
                        .border(2.dp, CedalColors.BorderCyan, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    val avatarUrl = p.avatarUrl
                    if (avatarUrl != null) {
                        AsyncImage(
                            model = avatarUrl,
                            contentDescription = "Avatar",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                        )
                    } else {
                        Text(initial, color = CedalColors.TextPrimary, fontSize = 36.sp)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                    Text(displayName, color = CedalColors.TextPrimary, fontSize = 18.sp)
                    activeBadge?.let { badge ->
                        Text(
                            "🏆 ${badge.bigWord.uppercase()}",
                            color = CedalColors.AccentCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .clip(RoundedCornerShape(50))
                                .border(1.dp, CedalColors.AccentCyan, RoundedCornerShape(50))
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                }
                p.handle?.takeIf { it.isNotBlank() }?.let {
                    Text("@$it", color = CedalColors.TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
                }
            }

            // Rank card is simply absent (not a "hidden" placeholder) when
            // they've turned off Popularity > rank for you - same silent-
            // omission behavior ProfileFieldRow below already uses for
            // occupation/hobby/bio.
            if (p.rankVisible) {
                ProfileRankCard(exp = p.exp)
            }

            ProfileFieldRow("Occupation", p.occupation)
            ProfileFieldRow("Hobby", p.hobby)
            ProfileFieldRow("Bio", p.bio)

            Text(
                "ACTIONS", color = CedalColors.TextSecondary, fontSize = 10.sp, letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(CedalColors.CardBackground)
                    .border(1.dp, CedalColors.BorderSlate, RoundedCornerShape(14.dp))
                    .padding(vertical = 4.dp),
            ) {
                ActionMenuRow(label = "Search") { scope.launch { ensureMessagesLoaded(); searchOpen = true } }
                ActionMenuRow(label = "Export Chat") { scope.launch { ensureMessagesLoaded(); exportChat() } }
                ActionMenuRow(label = "Add Shortcut") { addShortcut() }
                ActionMenuRow(label = "Report", color = CedalColors.Error) { reportConfirmOpen = true }
                ActionMenuRow(label = if (isBlocked) "Unblock" else "Block", color = CedalColors.Error) { blockConfirmOpen = true }
                ActionMenuRow(label = "Delete Chat", color = CedalColors.Error) { deleteChatConfirmOpen = true }
            }
            actionNotice?.let {
                Text(it, color = CedalColors.TextMuted, fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp))
            }

            if (deleted) {
                Text("User removed.", color = CedalColors.TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 12.dp))
            } else {
                // Unlike Block, this leaves both sides fully searchable/
                // re-addable afterward - see FriendService.deleteUser's
                // own doc comment for the exact contrast.
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    if (deleteConfirming) {
                        Text(
                            "Remove this connection? You can still find and re-add each other later - this isn't Block.",
                            color = CedalColors.TextSecondary, fontSize = 11.sp, modifier = Modifier.padding(bottom = 8.dp),
                        )
                        Row {
                            Text(
                                "CONFIRM", color = CedalColors.Error, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .border(1.dp, CedalColors.Error, RoundedCornerShape(50))
                                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                        scope.launch { viewModel.deleteUser(userId).onSuccess { deleted = true }.onFailure { error = it.message } }
                                    }
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                            )
                            Text(
                                "CANCEL", color = CedalColors.TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .padding(start = 10.dp)
                                    .clip(RoundedCornerShape(50))
                                    .border(1.dp, CedalColors.BorderSlate, RoundedCornerShape(50))
                                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { deleteConfirming = false }
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                            )
                        }
                    } else {
                        Text(
                            "DELETE USER", color = CedalColors.Error, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .border(1.dp, CedalColors.Error, RoundedCornerShape(50))
                                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { deleteConfirming = true }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                        )
                    }
                }
            }
        }
    }

    if (searchOpen) {
        ChatSearchOverlay(
            messages = messages.filterNot { it.deleted },
            onResultTap = { msg -> searchOpen = false; onNavigate("member_chat/$userId?name=${java.net.URLEncoder.encode(displayNameForActions, "UTF-8")}&highlightId=${msg.id}") },
            onDismiss = { searchOpen = false },
        )
    }

    if (reportConfirmOpen) {
        ReportOverlay(
            friendName = displayNameForActions,
            viewModel = viewModel,
            onSubmit = { reason, mediaUrl, mediaType, fileName ->
                reportConfirmOpen = false
                scope.launch {
                    viewModel.reportFriend(userId, reason, mediaUrl, mediaType, fileName)
                        .onSuccess { actionNotice = "Reported for admin review" }
                }
            },
            onDismiss = { reportConfirmOpen = false },
        )
    }

    if (blockConfirmOpen) {
        SimpleConfirmOverlay(
            title = if (isBlocked) "Unblock $displayNameForActions?" else "Block $displayNameForActions?",
            body = if (isBlocked) "$displayNameForActions will be able to message you again." else "$displayNameForActions won't be able to message you, and this chat will disappear from your list.",
            confirmLabel = if (isBlocked) "UNBLOCK" else "BLOCK",
            onConfirm = {
                blockConfirmOpen = false
                val next = !isBlocked
                isBlocked = next
                scope.launch {
                    if (next) viewModel.blockFriend(userId) else viewModel.unblockFriend(userId)
                    actionNotice = if (next) "Blocked" else "Unblocked"
                }
            },
            onDismiss = { blockConfirmOpen = false },
        )
    }

    if (deleteChatConfirmOpen) {
        DeleteChatConfirmOverlay(
            onConfirm = {
                deleteChatConfirmOpen = false
                scope.launch {
                    viewModel.deleteConversation(userId).onSuccess { actionNotice = "Chat deleted" }.onFailure { error = it.message }
                }
            },
            onDismiss = { deleteChatConfirmOpen = false },
        )
    }
}

@Composable
private fun ProfileFieldRow(label: String, value: String?) {
    if (value.isNullOrBlank()) return
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)) {
        Text(label.uppercase(), color = CedalColors.TextSecondary, fontSize = 10.sp, letterSpacing = 1.sp)
        Text(value, color = CedalColors.TextPrimary, fontSize = 14.sp, modifier = Modifier.padding(top = 2.dp))
    }
}
