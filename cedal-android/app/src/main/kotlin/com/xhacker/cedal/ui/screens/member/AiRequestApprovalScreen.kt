package com.xhacker.cedal.ui.screens.member

import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xhacker.cedal.data.AiChangeRequestDto
import com.xhacker.cedal.ui.theme.CedalColors
import com.xhacker.cedal.ui.theme.CedalErrorText
import com.xhacker.cedal.ui.theme.CedalGhostButton
import com.xhacker.cedal.viewmodel.AuthViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Admin-only (server-side enforced regardless of what's shown here - see
// AiChangeRequestRoutes.requireAdmin) - every code-needed request from the
// Code screen's AI chat lands here, and nothing deploys until one of these
// two buttons is tapped. Approve merges the real GitHub PR and triggers a
// real redeploy - not reversible by just tapping something else afterward.
@Composable
fun AiRequestApprovalBody(onBack: () -> Unit, viewModel: AuthViewModel = hiltViewModel()) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var requests by remember { mutableStateOf<List<AiChangeRequestDto>>(emptyList()) }
    var busyId by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        while (true) {
            viewModel.listPendingAiRequests().onSuccess { requests = it }
            delay(5000)
        }
    }

    fun approve(id: String) {
        busyId = id
        error = null
        scope.launch {
            viewModel.approveAiRequest(id)
                .onSuccess { requests = requests.filterNot { it.id == id } }
                .onFailure { error = it.message }
            busyId = null
        }
    }

    fun reject(id: String) {
        busyId = id
        error = null
        scope.launch {
            viewModel.rejectAiRequest(id)
                .onSuccess { requests = requests.filterNot { it.id == id } }
                .onFailure { error = it.message }
            busyId = null
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(CedalColors.Background).padding(16.dp)) {
        MemberBackBar(title = "AI Requests", onBack = onBack)
        error?.let { CedalErrorText(it) }
        if (requests.isEmpty()) {
            Text("Nothing pending.", color = CedalColors.TextMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 16.dp))
        }
        LazyColumn(modifier = Modifier.weight(1f).padding(top = 8.dp)) {
            items(requests, key = { it.id }) { req ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(CedalColors.CardBackground)
                        .border(1.dp, CedalColors.BorderCyan, RoundedCornerShape(14.dp))
                        .padding(14.dp),
                ) {
                    Text(req.summary ?: "Untitled request", color = CedalColors.TextPrimary, fontSize = 14.sp, modifier = Modifier.padding(bottom = 6.dp))
                    req.prUrl?.let { url ->
                        Text(
                            url, color = CedalColors.AccentCyan, fontSize = 11.sp,
                            modifier = Modifier
                                .padding(bottom = 10.dp)
                                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                },
                        )
                    }
                    if (busyId == req.id) {
                        CircularProgressIndicator(color = CedalColors.AccentCyan, strokeWidth = 2.dp, modifier = Modifier.padding(top = 4.dp))
                    } else {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Box(modifier = Modifier.weight(1f)) {
                                CedalGhostButton(text = "REJECT", onClick = { reject(req.id) })
                            }
                            Box(modifier = Modifier.padding(start = 10.dp).weight(1f)) {
                                CedalGhostButton(text = "APPROVE & DEPLOY", onClick = { approve(req.id) })
                            }
                        }
                    }
                }
            }
        }
    }
}
