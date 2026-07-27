package com.xhacker.cedal.ui.screens.member

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xhacker.cedal.data.DeveloperSubmissionDto
import com.xhacker.cedal.ui.theme.CedalColors
import com.xhacker.cedal.ui.theme.CedalErrorText
import com.xhacker.cedal.ui.theme.CedalPrimaryButton
import com.xhacker.cedal.ui.theme.CedalTextField
import com.xhacker.cedal.viewmodel.AuthViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// "Send to Alucard" - minimal standalone form for now (Phase B), since the
// real editor (Phase C's Code tab) doesn't exist yet. This same list-of-my-
// submissions section below is meant to be promoted directly into Phase
// C's Security tab once that lands, not thrown away - it's already the
// right shape (poll listMine(), show status/stage results).
@Composable
fun DeveloperSubmitBody(
    onBack: () -> Unit,
    // False when embedded as DeveloperScaffold's SECURITY tab (see that
    // file) - there's no "back" from a top-level tab, so the header there
    // would just be a dead arrow.
    showBackBar: Boolean = true,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val scope = rememberCoroutineScope()
    var title by remember { mutableStateOf("") }
    var targetFilePath by remember { mutableStateOf("") }
    var language by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var submissions by remember { mutableStateOf<List<DeveloperSubmissionDto>>(emptyList()) }

    fun refresh() {
        scope.launch { viewModel.listMyDeveloperSubmissions().onSuccess { submissions = it } }
    }

    LaunchedEffect(Unit) {
        while (true) {
            refresh()
            delay(5000)
        }
    }

    fun submit() {
        submitting = true
        error = null
        scope.launch {
            viewModel.submitDeveloperPatch(title.trim(), targetFilePath.trim(), code, language.trim().ifBlank { "text" })
                .onSuccess {
                    title = ""; targetFilePath = ""; code = ""
                    refresh()
                }
                .onFailure { error = it.message }
            submitting = false
        }
    }

    val canSubmit = !submitting && title.isNotBlank() && targetFilePath.isNotBlank() && code.isNotBlank()

    Column(modifier = Modifier.fillMaxSize().background(CedalColors.Background).padding(16.dp).imePadding()) {
        if (showBackBar) MemberBackBar(title = "Send to Alucard", onBack = onBack)
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Text(
                "Submit a patch to Cedal's own codebase. Alucard checks it in two silent stages first - any issue shows up below on this same row instead of reaching the owner. Once clean, it waits for the owner's final approve or deny.",
                color = CedalColors.TextMuted, fontSize = 11.sp, modifier = Modifier.padding(bottom = 14.dp),
            )
            CedalTextField(value = title, onValueChange = { title = it }, prefix = "✎", placeholder = "title", modifier = Modifier.padding(bottom = 8.dp))
            CedalTextField(value = targetFilePath, onValueChange = { targetFilePath = it }, prefix = "›", placeholder = "target file path, e.g. cedal-server/src/main/kotlin/...", modifier = Modifier.padding(bottom = 8.dp))
            CedalTextField(value = language, onValueChange = { language = it }, prefix = "#", placeholder = "language (e.g. kotlin)", modifier = Modifier.padding(bottom = 8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 160.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, CedalColors.BorderCyan, RoundedCornerShape(12.dp))
                    .padding(12.dp),
            ) {
                if (code.isEmpty()) {
                    Text("full new file content", color = CedalColors.TextSecondary, fontSize = 13.sp)
                }
                BasicTextField(
                    value = code,
                    onValueChange = { code = it },
                    textStyle = TextStyle(color = CedalColors.TextPrimary, fontSize = 13.sp),
                    cursorBrush = SolidColor(CedalColors.AccentCyan),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            CedalErrorText(error)
            CedalPrimaryButton(
                text = if (submitting) "SUBMITTING…" else "SUBMIT",
                enabled = canSubmit,
                loading = submitting,
                modifier = Modifier.padding(top = 12.dp, bottom = 20.dp),
                onClick = { submit() },
            )

            Text("MY SUBMISSIONS", color = CedalColors.TextSecondary, fontSize = 11.sp, letterSpacing = 1.5.sp, modifier = Modifier.padding(bottom = 8.dp))
            if (submissions.isEmpty()) {
                Text("Nothing submitted yet.", color = CedalColors.TextMuted, fontSize = 12.sp)
            }
            submissions.forEach { sub -> SubmissionStatusCard(sub) }
        }
    }
}

@Composable
private fun SubmissionStatusCard(sub: DeveloperSubmissionDto) {
    val (statusColor, statusLabel) = when (sub.status) {
        "pending_stage1" -> CedalColors.TextMuted to "Stage 1 checking…"
        "stage1_failed" -> CedalColors.Error to "Stage 1 blocked"
        "pending_stage2" -> CedalColors.TextMuted to "Stage 2 checking…"
        "stage2_failed" -> CedalColors.Error to "Stage 2 blocked"
        "pending_approval" -> CedalColors.AccentCyan to "Awaiting owner approval"
        "deploying" -> CedalColors.AccentCyan to "Deploying…"
        "approved" -> CedalColors.Success to "Approved & deployed"
        "deploy_error" -> CedalColors.Error to "Approved, but deploy failed"
        "denied" -> CedalColors.Error to "Denied"
        else -> CedalColors.TextMuted to sub.status
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(CedalColors.CardBackground)
            .border(1.dp, statusColor.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
            .padding(14.dp),
    ) {
        Text(sub.title, color = CedalColors.TextPrimary, fontSize = 13.sp)
        Text(sub.targetFilePath, color = CedalColors.TextMuted, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp, bottom = 6.dp))
        Text(statusLabel, color = statusColor, fontSize = 12.sp)
        sub.stage1Result?.takeIf { sub.status == "stage1_failed" }?.let {
            Text(it, color = CedalColors.TextSecondary, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
        }
        sub.stage2Result?.takeIf { sub.status == "stage2_failed" }?.let {
            Text(it, color = CedalColors.TextSecondary, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
        }
        sub.deniedReason?.let {
            Text(it, color = CedalColors.TextSecondary, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
        }
    }
}
