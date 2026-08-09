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

// Chat list > More > Godmode - admin-only, bypasses Friend Hider entirely
// (see AdminService.listAllUsers). "Ban"/"Clear Data" are real, destructive
// admin powers - both gated behind the same biometric/passcode re-verify as
// Delete Account, so a leaked/stolen admin session token alone still can't
// silently wield them (see AccountVerifyOverlay).
@Composable
fun GodmodeBody(onBack: () -> Unit, viewModel: AuthViewModel = hiltViewModel()) {
    var users by remember { mutableStateOf<List<GodmodeUserDto>>(emptyList()) }
    var query by remember { mutableStateOf("") }
    var pendingAction by remember { mutableStateOf<Pair<GodmodeUserDto, String>?>(null) } // user to (ban|unban|clear)
    val scope = rememberCoroutineScope()

    fun refresh() {
        scope.launch { viewModel.listGodmodeUsers().onSuccess { users = it } }
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
            Text("Godmode", color = CedalColors.TextPrimary, fontSize = 16.sp, modifier = Modifier.weight(1f))
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
                GodmodeUserRow(
                    user = user,
                    onBan = { pendingAction = user to "ban" },
                    onUnban = { pendingAction = user to "unban" },
                    onPermanentBan = { pendingAction = user to "permanent_ban" },
                    onClearData = { pendingAction = user to "clear" },
                )
            }
        }
    }

    val pending = pendingAction
    if (pending != null) {
        val (user, action) = pending
        // Rendered directly, NOT wrapped in a Dialog - AccountVerifyOverlay
        // already draws its own full-screen scrim, and an extra Dialog
        // window here was blocking BiometricPrompt's own window/fragment
        // attachment from ever showing (button did nothing on tap - see
        // MemberSwitchAccountScreen's usage, which renders the same overlay
        // directly with no such issue).
        AccountVerifyOverlay(
            viewModel = viewModel,
            biometricOnly = true,
            message = when (action) {
                "ban" -> "Banning ${user.name} needs your fingerprint."
                "unban" -> "Unbanning ${user.name} needs your fingerprint."
                "permanent_ban" -> "Permanently banning ${user.name} skips the 24h appeal window - PERMANENT and needs your fingerprint."
                else -> "Clearing ${user.name}'s data is PERMANENT and needs your fingerprint."
            },
            onVerified = {
                pendingAction = null
                scope.launch {
                    when (action) {
                        "ban" -> viewModel.banUser(user.id)
                        "unban" -> viewModel.unbanUser(user.id)
                        "permanent_ban" -> viewModel.permanentBanUser(user.id)
                        else -> viewModel.clearUserData(user.id)
                    }
                    refresh()
                }
            },
            onCancel = { pendingAction = null },
        )
    }
}

@Composable
private fun GodmodeUserRow(user: GodmodeUserDto, onBan: () -> Unit, onUnban: () -> Unit, onPermanentBan: () -> Unit, onClearData: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(CedalColors.CardBackground)
            .border(1.dp, if (user.banned) CedalColors.Error else CedalColors.BorderSlate, RoundedCornerShape(14.dp))
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(user.name, color = CedalColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                user.email?.let { Text(it, color = CedalColors.TextSecondary, fontSize = 11.sp) }
            }
            if (user.banned) {
                Text("BANNED", color = CedalColors.Error, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
        // Permanent audit trail (see Users.declinedUpdateVersionCode) - not
        // editable/clearable from anywhere in this screen or any other.
        user.declinedUpdateAt?.let { declinedAt ->
            Text(
                "⚠ Declined update to build ${user.declinedUpdateVersionCode} on ${java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.getDefault()).format(java.util.Date(declinedAt))}",
                color = CedalColors.Error, fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp),
            )
        }
        Row(modifier = Modifier.padding(top = 8.dp)) {
            Text(
                if (user.banned) "UNBAN" else "BAN",
                color = CedalColors.AccentCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = if (user.banned) onUnban else onBan)
                    .padding(end = 16.dp, top = 4.dp, bottom = 4.dp),
            )
            Text(
                "PERMANENT BAN",
                color = CedalColors.Error, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onPermanentBan)
                    .padding(end = 16.dp, top = 4.dp, bottom = 4.dp),
            )
            Text(
                "CLEAR DATA",
                color = CedalColors.Error, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClearData)
                    .padding(top = 4.dp, bottom = 4.dp),
            )
        }
    }
}
