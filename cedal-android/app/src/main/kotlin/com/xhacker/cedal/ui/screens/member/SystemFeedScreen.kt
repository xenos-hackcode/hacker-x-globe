package com.xhacker.cedal.ui.screens.member

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xhacker.cedal.data.SystemFeedPostDto
import com.xhacker.cedal.ui.theme.CedalColors
import com.xhacker.cedal.viewmodel.AuthViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Cedal System Feed - a real, shared broadcast channel every account sees.
// Only the admin account can post (enforced server-side, see
// SystemFeedService.ADMIN_EMAIL) - everyone else can only react. The
// isAdmin check here is purely to decide whether to SHOW the composer; the
// actual enforcement is the server rejecting a non-admin's post regardless.
// QUICK_REACTIONS is shared - see ChatActionComponents.kt.

@Composable
fun SystemFeedBody(onBack: () -> Unit, viewModel: AuthViewModel = hiltViewModel()) {
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var posts by remember { mutableStateOf<List<SystemFeedPostDto>>(emptyList()) }
    var input by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var isAdmin by remember { mutableStateOf(false) }
    var reactingFor by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.getProfile().onSuccess { profile ->
            isAdmin = profile.email?.equals("hackerxenos06@gmail.com", ignoreCase = true) == true
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            viewModel.listFeedPosts().onSuccess { posts = it }
            delay(10_000)
        }
    }

    LaunchedEffect(posts.size) {
        if (posts.isNotEmpty()) listState.animateScrollToItem(posts.size - 1)
    }

    fun send() {
        val text = input.trim()
        if (text.isBlank() || sending) return
        input = ""
        sending = true
        error = null
        scope.launch {
            viewModel.createFeedPost(text)
                .onSuccess { post -> posts = posts + post }
                .onFailure { error = it.message ?: "Couldn't post" }
            sending = false
        }
    }

    fun react(postId: String, emoji: String) {
        reactingFor = null
        scope.launch {
            viewModel.reactToFeedPost(postId, emoji).onSuccess { reactions ->
                posts = posts.map { if (it.id == postId) it.copy(reactions = reactions) else it }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).imePadding()) {
        MemberBackBar(title = "Cedal System Feed", onBack = onBack)

        LazyColumn(state = listState, modifier = Modifier.weight(1f)) {
            itemsIndexed(posts, key = { _, post -> post.id }) { _, post ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 2.dp, bottomEnd = 16.dp))
                        .background(CedalColors.CardBackground)
                        .border(1.dp, CedalColors.BorderSlate, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 2.dp, bottomEnd = 16.dp))
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { reactingFor = post.id }
                        .padding(14.dp),
                ) {
                    Text(post.authorName, color = CedalColors.AccentCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(post.text, color = CedalColors.TextPrimary, fontSize = 15.sp, lineHeight = 21.sp, modifier = Modifier.padding(top = 4.dp))
                    Text(formatFeedTime(post.createdAt), color = CedalColors.TextMuted, fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp))

                    if (post.reactions.isNotEmpty()) {
                        val counts = post.reactions.values.groupingBy { it }.eachCount()
                        Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            counts.forEach { (emoji, count) ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(CedalColors.Background)
                                        .border(1.dp, CedalColors.BorderSlate, RoundedCornerShape(50))
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                ) {
                                    Text("$emoji $count", color = CedalColors.TextSecondary, fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    if (reactingFor == post.id) {
                        Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            QUICK_REACTIONS.forEach { emoji ->
                                Text(
                                    emoji,
                                    fontSize = 20.sp,
                                    modifier = Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                        react(post.id, emoji)
                                    },
                                )
                            }
                        }
                    }
                }
            }
            if (posts.isEmpty()) {
                item {
                    Text("No announcements yet.", color = CedalColors.TextMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 24.dp))
                }
            }
        }

        error?.let { Text(it, color = CedalColors.Error, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp)) }

        if (isAdmin) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
                Text("›", color = CedalColors.AccentCyan, fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 8.dp))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(50))
                        .background(CedalColors.CardBackground)
                        .border(1.dp, CedalColors.BorderCyan, RoundedCornerShape(50))
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                ) {
                    if (input.isEmpty()) {
                        Text("Post an announcement…", color = CedalColors.TextMuted, fontSize = 15.sp, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    }
                    BasicTextField(
                        value = input,
                        onValueChange = { input = it },
                        textStyle = TextStyle(color = CedalColors.TextPrimary, fontSize = 15.sp),
                        cursorBrush = SolidColor(CedalColors.AccentCyan),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                val canSend = input.isNotBlank() && !sending
                // Matches every other chat's send button (real chat thread,
                // Corneal) - same size/glow/icon everywhere.
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
                        .clickable(enabled = canSend, interactionSource = remember { MutableInteractionSource() }, indication = null) { send() },
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Post",
                        tint = if (canSend) CedalColors.Background else CedalColors.TextMuted,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        } else {
            Text(
                "Only the admin can post here - tap a post to react.",
                color = CedalColors.TextMuted,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

private fun formatFeedTime(millis: Long): String = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(millis))
