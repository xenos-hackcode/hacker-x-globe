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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xhacker.cedal.data.GodmodeUserDto
import com.xhacker.cedal.ui.theme.CedalColors
import com.xhacker.cedal.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

// Developer terminal > Manage Developer Access - owner-only (same account
// hackerxenos06@gmail.com that has the fixed "cedalstar" passcode). Grants/
// revokes Users.developerAccess and issues the one-time developerKey a
// delegated account needs to enter Developer mode - see
// DeveloperAccessService server-side and EnterPasscodeScreen's
// needsKeyFromOwner client-side.
@Composable
fun ManageDeveloperAccessBody(onBack: () -> Unit, viewModel: AuthViewModel = hiltViewModel()) {
    var users by remember { mutableStateOf<List<GodmodeUserDto>>(emptyList()) }
    var query by remember { mutableStateOf("") }
    var generatedKey by remember { mutableStateOf<Pair<GodmodeUserDto, String>?>(null) }
    val scope = rememberCoroutineScope()

    fun refresh() {
        scope.launch { viewModel.listDeveloperAccessUsers().onSuccess { users = it } }
    }
    LaunchedEffect(Unit) { refresh() }

    val visible = users.filter { query.isBlank() || it.name.contains(query, ignoreCase = true) || it.email?.contains(query, ignoreCase = true) == true }

    Column(modifier = Modifier.fillMaxSize().background(CedalColors.Background).imePadding()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().border(width = Dp.Hairline, color = CedalColors.BorderSlate).padding(horizontal = 8.dp, vertical = 8.dp),
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = CedalColors.TextPrimary)
            }
            Text("Manage Developer Access", color = CedalColors.TextPrimary, fontSize = 16.sp, modifier = Modifier.weight(1f))
            Text("${users.size} users", color = CedalColors.TextMuted, fontSize = 11.sp)
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clip(RoundedCornerShape(50))
                .border(1.dp, CedalColors.BorderSlate, RoundedCornerShape(50))
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            BasicTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                textStyle = TextStyle(color = CedalColors.TextPrimary, fontSize = 13.sp),
                cursorBrush = SolidColor(CedalColors.AccentCyan),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    if (query.isEmpty()) Text("Search every user (name or email)", color = CedalColors.TextSecondary, fontSize = 13.sp)
                    inner()
                },
            )
        }

        LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            items(visible, key = { it.id }) { user ->
                DeveloperAccessUserRow(
                    user = user,
                    onToggleAccess = {
                        scope.launch {
                            if (user.developerAccess) viewModel.revokeDeveloperAccess(user.id) else viewModel.grantDeveloperAccess(user.id)
                            refresh()
                        }
                    },
                    onGenerateKey = {
                        scope.launch {
                            viewModel.generateDeveloperKey(user.id).onSuccess { key ->
                                generatedKey = user to key
                                refresh()
                            }
                        }
                    },
                )
            }
        }
    }

    // The key is shown here exactly once - SEND KEY relays it in-app as a
    // "Cedal Team" DM (see DeveloperAccessService.sendKeyMessage); CANCEL
    // just closes without sending, e.g. if you generated it for the wrong
    // person. Either way, nothing else in the app ever shows this value
    // again once the dialog closes.
    val pending = generatedKey
    if (pending != null) {
        val (user, key) = pending
        var sending by remember(pending) { mutableStateOf(false) }
        var sendError by remember(pending) { mutableStateOf<String?>(null) }
        androidx.compose.ui.window.Dialog(onDismissRequest = { if (!sending) generatedKey = null }) {
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(CedalColors.CardBackground)
                    .border(1.dp, CedalColors.BorderCyan, RoundedCornerShape(16.dp))
                    .padding(20.dp),
            ) {
                Text("Key for ${user.name}", color = CedalColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(
                    "One-time - it's spent the instant they use it, and won't be shown again after this.",
                    color = CedalColors.TextSecondary, fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp, bottom = 14.dp),
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, CedalColors.BorderSlate, RoundedCornerShape(10.dp))
                        .padding(14.dp),
                ) {
                    Text(key, color = CedalColors.AccentCyan, fontSize = 18.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                }
                if (sendError != null) {
                    Text(sendError!!, color = CedalColors.Error, fontSize = 11.sp, modifier = Modifier.padding(top = 10.dp))
                }
                Row(modifier = Modifier.padding(top = 16.dp)) {
                    Text(
                        "CANCEL", color = CedalColors.TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable(enabled = !sending, interactionSource = remember { MutableInteractionSource() }, indication = null) { generatedKey = null }
                            .padding(vertical = 4.dp, horizontal = 8.dp),
                    )
                    Text(
                        if (sending) "SENDING…" else "SEND KEY", color = CedalColors.AccentCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .padding(start = 12.dp)
                            .clickable(enabled = !sending, interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                sending = true
                                sendError = null
                                scope.launch {
                                    viewModel.sendDeveloperKey(user.id, key)
                                        .onSuccess { generatedKey = null }
                                        .onFailure { sendError = it.message; sending = false }
                                }
                            }
                            .padding(vertical = 4.dp, horizontal = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun DeveloperAccessUserRow(user: GodmodeUserDto, onToggleAccess: () -> Unit, onGenerateKey: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(CedalColors.CardBackground)
            .border(1.dp, if (user.developerAccess) CedalColors.AccentCyan else CedalColors.BorderSlate, RoundedCornerShape(14.dp))
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(user.name, color = CedalColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                user.email?.let { Text(it, color = CedalColors.TextSecondary, fontSize = 11.sp) }
            }
            if (user.developerAccess) {
                Text(
                    if (user.hasActiveDeveloperKey) "KEY READY" else "NO ACTIVE KEY",
                    color = if (user.hasActiveDeveloperKey) CedalColors.Success else CedalColors.TextMuted,
                    fontSize = 10.sp, fontWeight = FontWeight.Bold,
                )
            }
        }
        Row(modifier = Modifier.padding(top = 8.dp)) {
            Text(
                if (user.developerAccess) "REVOKE ACCESS" else "GRANT ACCESS",
                color = if (user.developerAccess) CedalColors.Error else CedalColors.AccentCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onToggleAccess)
                    .padding(end = 16.dp, top = 4.dp, bottom = 4.dp),
            )
            if (user.developerAccess) {
                Text(
                    "GENERATE KEY",
                    color = CedalColors.AccentCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onGenerateKey)
                        .padding(top = 4.dp, bottom = 4.dp),
                )
            }
        }
    }
}
