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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xhacker.cedal.data.AppealDto
import com.xhacker.cedal.data.MessageReportDto
import com.xhacker.cedal.data.UserReportDto
import com.xhacker.cedal.ui.theme.CedalColors
import com.xhacker.cedal.viewmodel.AuthViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Chat list > More > Admin Review - admin-only (server also re-checks via
// AdminService.isAdmin on every route, this UI just isn't offered to
// non-admins). Two tabs: person-level reports (ChatService.reportUser) and
// single-message reports (MessageInteractionService.report) - separate
// tables/flows server-side, shown together here since they're both "things
// admin needs to look at."
@Composable
fun AdminReviewBody(onBack: () -> Unit, viewModel: AuthViewModel = hiltViewModel()) {
    var tab by remember { mutableStateOf("user") } // "user" | "message" | "appeal"
    var userReports by remember { mutableStateOf<List<UserReportDto>>(emptyList()) }
    var messageReports by remember { mutableStateOf<List<MessageReportDto>>(emptyList()) }
    var appeals by remember { mutableStateOf<List<AppealDto>>(emptyList()) }
    val scope = rememberCoroutineScope()

    fun refresh() {
        scope.launch {
            viewModel.listUserReports().onSuccess { userReports = it }
            viewModel.listMessageReports().onSuccess { messageReports = it }
            viewModel.listAppeals().onSuccess { appeals = it }
        }
    }
    LaunchedEffect(Unit) { refresh() }

    Column(modifier = Modifier.fillMaxSize().background(CedalColors.Background)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().border(width = Dp.Hairline, color = CedalColors.BorderSlate).padding(horizontal = 8.dp, vertical = 8.dp),
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = CedalColors.TextPrimary)
            }
            Text("Admin Review", color = CedalColors.TextPrimary, fontSize = 16.sp, modifier = Modifier.weight(1f))
        }

        Row(modifier = Modifier.padding(16.dp)) {
            AdminReviewTab(label = "People (${userReports.size})", active = tab == "user", onClick = { tab = "user" })
            AdminReviewTab(label = "Messages (${messageReports.size})", active = tab == "message", modifier = Modifier.padding(start = 8.dp), onClick = { tab = "message" })
            AdminReviewTab(label = "Appeals (${appeals.size})", active = tab == "appeal", modifier = Modifier.padding(start = 8.dp), onClick = { tab = "appeal" })
        }

        if (tab == "user") {
            if (userReports.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(24.dp)) {
                    Text("No pending person reports.", color = CedalColors.TextSecondary, fontSize = 13.sp)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                    items(userReports, key = { it.id }) { report ->
                        UserReportCard(report, onDismiss = { scope.launch { viewModel.dismissUserReport(report.id); refresh() } })
                    }
                }
            }
        } else if (tab == "message") {
            if (messageReports.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(24.dp)) {
                    Text("No pending message reports.", color = CedalColors.TextSecondary, fontSize = 13.sp)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                    items(messageReports, key = { it.id }) { report ->
                        MessageReportCard(report, onDismiss = { scope.launch { viewModel.dismissMessageReport(report.id); refresh() } })
                    }
                }
            }
        } else {
            if (appeals.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(24.dp)) {
                    Text("No pending appeals.", color = CedalColors.TextSecondary, fontSize = 13.sp)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                    items(appeals, key = { it.id }) { appeal ->
                        AppealCard(appeal, onDismiss = { scope.launch { viewModel.dismissAppeal(appeal.id); refresh() } })
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminReviewTab(label: String, active: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Text(
        label, color = if (active) CedalColors.Background else CedalColors.TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(if (active) CedalColors.AccentCyan else Color.Transparent)
            .border(1.dp, CedalColors.BorderSlate, RoundedCornerShape(50))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    )
}

@Composable
private fun UserReportCard(report: UserReportDto, onDismiss: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(CedalColors.CardBackground)
            .border(1.dp, CedalColors.BorderSlate, RoundedCornerShape(14.dp))
            .padding(14.dp),
    ) {
        Text("${report.reporterName} reported ${report.reportedName}", color = CedalColors.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Text(formatReportTime(report.createdAt), color = CedalColors.TextMuted, fontSize = 10.sp, modifier = Modifier.padding(top = 2.dp, bottom = 6.dp))
        report.reason?.let { Text(it, color = CedalColors.TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(bottom = 6.dp)) }
        report.mediaUrl?.let { url ->
            Text("Evidence: ${report.fileName ?: report.mediaType ?: "attachment"}", color = CedalColors.AccentCyan, fontSize = 11.sp, modifier = Modifier.padding(bottom = 6.dp))
        }
        Text(
            "DISMISS", color = CedalColors.AccentCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss)
                .padding(4.dp),
        )
    }
}

@Composable
private fun MessageReportCard(report: MessageReportDto, onDismiss: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(CedalColors.CardBackground)
            .border(1.dp, CedalColors.BorderSlate, RoundedCornerShape(14.dp))
            .padding(14.dp),
    ) {
        Text(report.chatType.uppercase(), color = CedalColors.TextMuted, fontSize = 10.sp)
        Text(report.messageText, color = CedalColors.TextPrimary, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp, bottom = 6.dp))
        Text(formatReportTime(report.createdAt), color = CedalColors.TextMuted, fontSize = 10.sp, modifier = Modifier.padding(bottom = 6.dp))
        Text(
            "DISMISS", color = CedalColors.AccentCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss)
                .padding(4.dp),
        )
    }
}

@Composable
private fun AppealCard(appeal: AppealDto, onDismiss: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(CedalColors.CardBackground)
            .border(1.dp, CedalColors.BorderSlate, RoundedCornerShape(14.dp))
            .padding(14.dp),
    ) {
        Text(appeal.email, color = CedalColors.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Text(
            if (appeal.reason == "banned") "Appealing a ban" else "Appealing a data clear",
            color = CedalColors.TextMuted, fontSize = 10.sp, modifier = Modifier.padding(top = 2.dp, bottom = 6.dp),
        )
        Text(appeal.message, color = CedalColors.TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(bottom = 6.dp))
        Text(formatReportTime(appeal.createdAt), color = CedalColors.TextMuted, fontSize = 10.sp, modifier = Modifier.padding(bottom = 6.dp))
        Text(
            "DISMISS", color = CedalColors.AccentCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss)
                .padding(4.dp),
        )
    }
}

private fun formatReportTime(epochMs: Long): String = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(epochMs))
