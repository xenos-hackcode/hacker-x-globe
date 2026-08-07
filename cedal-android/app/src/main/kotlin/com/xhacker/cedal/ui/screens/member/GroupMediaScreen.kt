package com.xhacker.cedal.ui.screens.member

import android.app.DownloadManager
import android.net.Uri
import android.os.Environment
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.xhacker.cedal.data.GroupMessageDto
import com.xhacker.cedal.ui.theme.CedalColors
import com.xhacker.cedal.viewmodel.AuthViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class MediaTab { PHOTOS, VIDEOS, STICKERS, FILES, POLLS }

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1024 * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
    bytes >= 1024 -> "%.1f KB".format(bytes / 1024.0)
    bytes > 0 -> "$bytes B"
    else -> "-"
}

// Media & Storage sub-screen, reached from Group Profile's Security tab
// (see GroupProfileScreen.kt's "MEDIA & STORAGE" row) - Round-4 feedback
// that a plain count line wasn't enough. Unlike GroupChatThreadScreen.kt
// (which loads older pages on demand as the user scrolls), this screen
// needs accurate totals and a complete browsable list up front, so
// refresh() walks every page via getGroupMessages' beforeTimestamp cursor
// and accumulates the full history before rendering.
@Composable
fun GroupMediaBody(groupId: String, onBack: () -> Unit, viewModel: AuthViewModel = hiltViewModel()) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var messages by remember { mutableStateOf<List<GroupMessageDto>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var tab by remember { mutableStateOf(MediaTab.PHOTOS) }
    var selectMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(setOf<String>()) }

    fun refresh() {
        scope.launch {
            loading = true
            val all = mutableListOf<GroupMessageDto>()
            var cursor: Long? = null
            while (true) {
                val page = viewModel.getGroupMessages(groupId, beforeTimestamp = cursor).getOrElse { break }
                if (page.isEmpty()) break
                all += page
                if (page.size < 200) break
                cursor = page.minOf { it.sentAt }
            }
            messages = all
            loading = false
        }
    }
    LaunchedEffect(Unit) { refresh() }

    val photos = messages.filter { !it.deleted && !it.isSticker && it.mediaType?.startsWith("image") == true }
    val videos = messages.filter { !it.deleted && !it.isSticker && it.mediaType?.startsWith("video") == true }
    val stickers = messages.filter { !it.deleted && it.isSticker && it.mediaUrl != null }
    val files = messages.filter { !it.deleted && !it.isSticker && it.mediaUrl != null && it.mediaType != null && !it.mediaType!!.startsWith("image") && !it.mediaType!!.startsWith("video") }
    val polls = messages.filter { !it.deleted && it.pollOptions != null }

    val current = when (tab) {
        MediaTab.PHOTOS -> photos
        MediaTab.VIDEOS -> videos
        MediaTab.STICKERS -> stickers
        MediaTab.FILES -> files
        MediaTab.POLLS -> polls
    }
    val totalBytes = current.sumOf { it.mediaSizeBytes ?: 0L }

    fun exitSelect() { selectMode = false; selectedIds = emptySet() }

    fun downloadAll(items: List<GroupMessageDto>) {
        val dm = context.getSystemService(android.content.Context.DOWNLOAD_SERVICE) as DownloadManager
        items.forEach { m ->
            val url = m.mediaUrl ?: return@forEach
            val request = DownloadManager.Request(Uri.parse(url))
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, m.fileName ?: url.substringAfterLast('/'))
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            dm.enqueue(request)
        }
    }

    fun deleteAll(items: List<GroupMessageDto>) {
        scope.launch {
            items.forEach { viewModel.deleteGroupMessage(groupId, it.id) }
            exitSelect()
            refresh()
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(CedalColors.Background)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().border(width = androidx.compose.ui.unit.Dp.Hairline, color = CedalColors.BorderSlate).padding(horizontal = 8.dp, vertical = 8.dp),
        ) {
            IconButton(onClick = { if (selectMode) exitSelect() else onBack() }) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = CedalColors.TextPrimary)
            }
            Text(
                if (selectMode) "${selectedIds.size} selected" else "Media & Storage",
                color = CedalColors.TextPrimary, fontSize = 16.sp, modifier = Modifier.weight(1f),
            )
        }

        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
            MediaTab.entries.forEach { t ->
                val selected = tab == t
                Text(
                    t.name.take(4), color = if (selected) CedalColors.Background else CedalColors.TextSecondary, fontSize = 11.sp,
                    modifier = Modifier
                        .padding(end = 6.dp)
                        .clip(RoundedCornerShape(50))
                        .background(if (selected) CedalColors.AccentCyan else Color.Transparent)
                        .border(1.dp, CedalColors.BorderSlate, RoundedCornerShape(50))
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { tab = t; exitSelect() }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
            Text(
                if (tab == MediaTab.POLLS) "${current.size} polls" else "${current.size} items - ${formatBytes(totalBytes)}",
                color = CedalColors.TextMuted, fontSize = 11.sp, modifier = Modifier.weight(1f),
            )
            if (current.isNotEmpty()) {
                if (tab != MediaTab.POLLS) {
                    Text(
                        "DOWNLOAD ALL", color = CedalColors.AccentCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(end = 12.dp).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { downloadAll(current) },
                    )
                }
                Text(
                    "DELETE ALL", color = CedalColors.Error, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { deleteAll(current) },
                )
            }
        }

        if (selectMode && current.isNotEmpty()) {
            val monthFormat = remember { SimpleDateFormat("MMM yyyy", Locale.getDefault()) }
            val months = current.map { monthFormat.format(Date(it.sentAt)) }.distinct()
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
                months.forEach { month ->
                    Text(
                        month, color = CedalColors.AccentCyan, fontSize = 10.sp,
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .clip(RoundedCornerShape(50))
                            .border(1.dp, CedalColors.AccentCyan, RoundedCornerShape(50))
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                val idsInMonth = current.filter { monthFormat.format(Date(it.sentAt)) == month }.map { it.id }.toSet()
                                selectedIds = selectedIds + idsInMonth
                            }
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                    )
                }
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            if (loading) {
                Text("Loading...", color = CedalColors.TextMuted, fontSize = 12.sp, modifier = Modifier.padding(24.dp))
            } else if (current.isEmpty()) {
                Text("Nothing here yet.", color = CedalColors.TextMuted, fontSize = 12.sp, modifier = Modifier.padding(24.dp))
            } else if (tab == MediaTab.FILES || tab == MediaTab.POLLS) {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
                    items(current, key = { it.id }) { m ->
                        MediaListRow(
                            m, selectMode, m.id in selectedIds,
                            onLongPress = { selectMode = true; selectedIds = selectedIds + m.id },
                            onClick = { if (selectMode) selectedIds = if (m.id in selectedIds) selectedIds - m.id else selectedIds + m.id },
                        )
                    }
                }
            } else {
                LazyVerticalGrid(columns = GridCells.Fixed(3), modifier = Modifier.fillMaxSize().padding(4.dp)) {
                    items(current, key = { it.id }) { m ->
                        MediaGridItem(
                            m, selectMode, m.id in selectedIds,
                            onLongPress = { selectMode = true; selectedIds = selectedIds + m.id },
                            onClick = { if (selectMode) selectedIds = if (m.id in selectedIds) selectedIds - m.id else selectedIds + m.id },
                        )
                    }
                }
            }
        }

        if (selectMode && selectedIds.isNotEmpty()) {
            Row(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                Text(
                    "DOWNLOAD", color = CedalColors.AccentCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                        downloadAll(current.filter { it.id in selectedIds })
                    },
                )
                Text(
                    "DELETE", color = CedalColors.Error, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                        deleteAll(current.filter { it.id in selectedIds })
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MediaGridItem(m: GroupMessageDto, selectMode: Boolean, selected: Boolean, onLongPress: () -> Unit, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .padding(2.dp)
            .aspectRatio(1f)
            .clip(RoundedCornerShape(6.dp))
            .background(CedalColors.CardBackground)
            .let { if (selected) it.border(2.dp, CedalColors.AccentCyan, RoundedCornerShape(6.dp)) else it }
            .combinedClickable(onLongClick = onLongPress, onClick = onClick),
    ) {
        AsyncImage(model = m.mediaUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        if (selectMode) {
            Box(
                modifier = Modifier
                    .padding(4.dp)
                    .size(16.dp)
                    .clip(RoundedCornerShape(50))
                    .background(if (selected) CedalColors.AccentCyan else Color.Black.copy(alpha = 0.4f)),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MediaListRow(m: GroupMessageDto, selectMode: Boolean, selected: Boolean, onLongPress: () -> Unit, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .let { if (selected) it.background(CedalColors.CardBackground, RoundedCornerShape(8.dp)) else it }
            .combinedClickable(onLongClick = onLongPress, onClick = onClick)
            .padding(8.dp),
    ) {
        if (m.pollOptions != null) {
            Column(modifier = Modifier.weight(1f)) {
                Text(m.text.ifBlank { "Poll" }, color = CedalColors.TextPrimary, fontSize = 13.sp)
                Text(SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(m.sentAt)), color = CedalColors.TextMuted, fontSize = 10.sp)
            }
        } else {
            Icon(Icons.Outlined.InsertDriveFile, contentDescription = null, tint = CedalColors.AccentCyan, modifier = Modifier.size(20.dp))
            Column(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
                Text(m.fileName ?: "File", color = CedalColors.TextPrimary, fontSize = 13.sp)
                Text(formatBytes(m.mediaSizeBytes ?: 0L), color = CedalColors.TextMuted, fontSize = 10.sp)
            }
        }
        if (selectMode) {
            Box(modifier = Modifier.size(16.dp).clip(RoundedCornerShape(50)).background(if (selected) CedalColors.AccentCyan else CedalColors.BorderSlate))
        }
    }
}
