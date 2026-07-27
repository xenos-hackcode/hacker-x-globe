package com.xhacker.cedal.ui.screens.member

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xhacker.cedal.data.DeveloperSubmissionDto
import com.xhacker.cedal.ui.theme.CedalColors
import com.xhacker.cedal.ui.theme.CedalErrorText
import com.xhacker.cedal.ui.theme.CedalGhostButton
import com.xhacker.cedal.ui.theme.CedalPrimaryButton
import com.xhacker.cedal.ui.theme.CedalTextField
import com.xhacker.cedal.viewmodel.AuthViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Owner-only (server-side enforced regardless of what's shown here - see
// DeveloperSubmissionRoutes' requireAdmin) - every submission that passed
// Alucard's automated 2-stage review lands here for the final human call.
// Approve merges the real GitHub PR and triggers a real redeploy - not
// reversible by just tapping something else afterward. Deny requires a
// reason (see DenyReasonOverlay below) - unlike AiRequestApprovalScreen's
// REJECT, there's no existing "reason-gated action" pattern to reuse
// verbatim, so this adapts GodmodeScreen's pendingAction+Dialog mechanic
// (swapping its biometric verify for a reason text field).
@Composable
fun DeveloperSubmissionApprovalBody(onBack: () -> Unit, viewModel: AuthViewModel = hiltViewModel()) {
    val scope = rememberCoroutineScope()
    var submissions by remember { mutableStateOf<List<DeveloperSubmissionDto>>(emptyList()) }
    var busyId by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var pendingDeny by remember { mutableStateOf<DeveloperSubmissionDto?>(null) }

    LaunchedEffect(Unit) {
        while (true) {
            viewModel.listPendingDeveloperSubmissions().onSuccess { submissions = it }
            delay(5000)
        }
    }

    fun approve(id: String) {
        busyId = id
        error = null
        scope.launch {
            viewModel.approveDeveloperSubmission(id)
                .onSuccess { submissions = submissions.filterNot { it.id == id } }
                .onFailure { error = it.message }
            busyId = null
        }
    }

    fun deny(id: String, reason: String) {
        busyId = id
        error = null
        scope.launch {
            viewModel.denyDeveloperSubmission(id, reason)
                .onSuccess { submissions = submissions.filterNot { it.id == id } }
                .onFailure { error = it.message }
            busyId = null
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(CedalColors.Background).padding(16.dp)) {
        MemberBackBar(title = "Submission Approvals", onBack = onBack)
        error?.let { CedalErrorText(it) }
        if (submissions.isEmpty()) {
            Text("Nothing awaiting approval.", color = CedalColors.TextMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 16.dp))
        }
        LazyColumn(modifier = Modifier.weight(1f).padding(top = 8.dp)) {
            items(submissions, key = { it.id }) { sub ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(CedalColors.CardBackground)
                        .border(1.dp, CedalColors.BorderCyan, RoundedCornerShape(14.dp))
                        .padding(14.dp),
                ) {
                    Text(sub.title, color = CedalColors.TextPrimary, fontSize = 14.sp, modifier = Modifier.padding(bottom = 2.dp))
                    Text("by ${sub.userName}", color = CedalColors.TextSecondary, fontSize = 11.sp, modifier = Modifier.padding(bottom = 4.dp))
                    Text("${sub.targetFilePath} (${sub.language})", color = CedalColors.TextMuted, fontSize = 11.sp, modifier = Modifier.padding(bottom = 10.dp))
                    if (busyId == sub.id) {
                        CircularProgressIndicator(color = CedalColors.AccentCyan, strokeWidth = 2.dp, modifier = Modifier.padding(top = 4.dp))
                    } else {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Box(modifier = Modifier.weight(1f)) {
                                CedalGhostButton(text = "DENY", onClick = { pendingDeny = sub })
                            }
                            Box(modifier = Modifier.padding(start = 10.dp).weight(1f)) {
                                CedalGhostButton(text = "APPROVE & DEPLOY", onClick = { approve(sub.id) })
                            }
                        }
                    }
                }
            }
        }
    }

    pendingDeny?.let { sub ->
        DenyReasonOverlay(
            title = sub.title,
            onConfirm = { reason -> pendingDeny = null; deny(sub.id, reason) },
            onCancel = { pendingDeny = null },
        )
    }
}

@Composable
private fun DenyReasonOverlay(title: String, onConfirm: (reason: String) -> Unit, onCancel: () -> Unit) {
    var reason by remember { mutableStateOf("") }
    androidx.compose.ui.window.Dialog(onDismissRequest = onCancel) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(CedalColors.CardBackground)
                .border(1.dp, CedalColors.BorderCyan, RoundedCornerShape(16.dp))
                .padding(20.dp),
        ) {
            Text("Deny \"$title\"", color = CedalColors.TextPrimary, fontSize = 14.sp)
            Text(
                "The developer will see this reason on their submission.",
                color = CedalColors.TextSecondary, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp, bottom = 14.dp),
            )
            CedalTextField(value = reason, onValueChange = { reason = it }, prefix = "!", placeholder = "reason")
            Row(modifier = Modifier.padding(top = 14.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    CedalGhostButton(text = "CANCEL", onClick = onCancel)
                }
                Box(modifier = Modifier.padding(start = 10.dp).weight(1f)) {
                    CedalPrimaryButton(text = "DENY", enabled = reason.isNotBlank(), onClick = { onConfirm(reason.trim()) })
                }
            }
        }
    }
}
