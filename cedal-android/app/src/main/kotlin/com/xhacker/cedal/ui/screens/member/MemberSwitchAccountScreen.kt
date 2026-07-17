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
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import com.xhacker.cedal.data.SavedAccount
import com.xhacker.cedal.ui.screens.BiometricAuth
import com.xhacker.cedal.ui.theme.CedalColors
import com.xhacker.cedal.ui.theme.CedalErrorText
import com.xhacker.cedal.ui.theme.CedalGhostButton
import com.xhacker.cedal.ui.theme.CedalPrimaryButton
import com.xhacker.cedal.ui.theme.CedalTextField
import com.xhacker.cedal.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

// Gmail/Instagram-style account switcher - every account ever logged into
// on this device (see SecureStorage.savedAccounts), switchable with a tap
// via a saved refresh token (see AuthViewModel.switchAccount), no password
// re-entry. "Add Another Account" hands off to a fresh sign-in instead -
// once that succeeds, it joins this same list automatically.
@Composable
fun MemberSwitchAccountBody(
    onBack: () -> Unit,
    onSwitched: () -> Unit,
    onAddAccount: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val scope = rememberCoroutineScope()
    var accounts by remember { mutableStateOf(viewModel.storage.savedAccounts) }
    val currentUserId = viewModel.storage.userId
    var switchingId by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    // Set the instant "REMOVE" is tapped, cleared on cancel/confirm - shows
    // the verify overlay below rather than removing immediately, since
    // removing a saved login (even just locally) is exactly the kind of
    // thing someone picking up an unlocked phone shouldn't be able to do.
    var pendingRemoveId by remember { mutableStateOf<String?>(null) }

    fun refresh() {
        accounts = viewModel.storage.savedAccounts
    }

    Box(modifier = Modifier.fillMaxSize()) {
    Column(modifier = Modifier.fillMaxSize().background(CedalColors.Background).padding(16.dp)) {
        MemberBackBar(title = "Switch Account", onBack = onBack)
        Text(
            "Every account you've signed into on this device. Tap one to switch instantly - no password needed.",
            color = CedalColors.TextMuted, fontSize = 12.sp, modifier = Modifier.padding(bottom = 14.dp),
        )
        CedalErrorText(error)

        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            if (accounts.isEmpty()) {
                Text("No other saved accounts yet.", color = CedalColors.TextMuted, fontSize = 12.sp)
            }
            accounts.forEach { account ->
                AccountRow(
                    account = account,
                    isActive = account.userId == currentUserId,
                    switching = switchingId == account.userId,
                    onClick = {
                        if (account.userId == currentUserId || switchingId != null) return@AccountRow
                        switchingId = account.userId
                        error = null
                        scope.launch {
                            viewModel.switchAccount(account.userId)
                                .onSuccess { switchingId = null; onSwitched() }
                                .onFailure { switchingId = null; error = it.message }
                        }
                    },
                    onRemove = {
                        pendingRemoveId = account.userId
                    },
                )
            }
        }

        CedalGhostButton(text = "+ ADD ANOTHER ACCOUNT", modifier = Modifier.padding(top = 8.dp), onClick = onAddAccount)
    }

    val removeId = pendingRemoveId
    if (removeId != null) {
        RemoveAccountVerifyOverlay(
            viewModel = viewModel,
            onVerified = {
                viewModel.removeSavedAccount(removeId)
                refresh()
                pendingRemoveId = null
            },
            onCancel = { pendingRemoveId = null },
        )
    }
    }
}

@Composable
private fun AccountRow(account: SavedAccount, isActive: Boolean, switching: Boolean, onClick: () -> Unit, onRemove: () -> Unit) {
    val displayName = account.nickname?.takeIf { it.isNotBlank() } ?: account.email ?: "Account"
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(CedalColors.CardBackground)
            .border(1.dp, if (isActive) CedalColors.Success.copy(alpha = 0.5f) else CedalColors.BorderSlate, RoundedCornerShape(14.dp))
            .clickable(enabled = !isActive && !switching, interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(CedalColors.BackgroundBlob)
                .border(1.dp, CedalColors.BorderCyan, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(displayName.firstOrNull()?.uppercaseChar()?.toString() ?: "?", color = CedalColors.AccentCyan, fontSize = 15.sp)
        }
        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
            Text(displayName, color = CedalColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            account.email?.takeIf { it != displayName }?.let {
                Text(it, color = CedalColors.TextSecondary, fontSize = 11.sp)
            }
        }
        when {
            switching -> CircularProgressIndicator(color = CedalColors.AccentCyan, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
            isActive -> Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .border(1.dp, CedalColors.Success, RoundedCornerShape(50))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text("ACTIVE", color = CedalColors.Success, fontSize = 10.sp, letterSpacing = 0.5.sp)
            }
            else -> Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(CedalColors.Background)
                    .border(1.dp, CedalColors.BorderSlate, RoundedCornerShape(50))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onRemove)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text("REMOVE", color = CedalColors.TextMuted, fontSize = 10.sp, letterSpacing = 0.4.sp)
            }
        }
    }
}

// Gates "Remove" behind proving it's really you - biometric if available
// (fired once automatically as soon as this appears), passcode always
// offered as a fallback either way. Reuses the same server-side passcode
// check ("user" mode) as the app-lock passcode screen - verifying against
// the CURRENTLY ACTIVE account, since that's whose device-unlock state is
// actually being relied on here, not the (possibly different) account
// being removed.
@Composable
private fun RemoveAccountVerifyOverlay(viewModel: AuthViewModel, onVerified: () -> Unit, onCancel: () -> Unit) {
    val activity = LocalContext.current as? FragmentActivity
    val canUseBiometric = activity != null && viewModel.storage.biometricEnabled
    var code by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        if (canUseBiometric && activity != null) {
            BiometricAuth.authenticate(activity, onSuccess = onVerified, onError = { /* fall through to passcode below, not a hard error */ })
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.75f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(CedalColors.CardBackground)
                .border(1.dp, CedalColors.BorderCyan, RoundedCornerShape(18.dp))
                .padding(20.dp),
        ) {
            Text("Verify it's you", color = CedalColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 6.dp))
            Text(
                "Removing a saved account needs your biometrics or passcode.",
                color = CedalColors.TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(bottom = 14.dp),
            )

            if (canUseBiometric && activity != null) {
                CedalGhostButton(
                    text = "USE BIOMETRICS",
                    modifier = Modifier.padding(bottom = 12.dp),
                    onClick = { error = null; BiometricAuth.authenticate(activity, onSuccess = onVerified, onError = { error = it }) },
                )
            }

            CedalTextField(
                value = code,
                onValueChange = { code = it },
                prefix = "★",
                placeholder = "passcode",
                isPassword = true,
                keyboardType = KeyboardType.Number,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            CedalErrorText(error)
            CedalPrimaryButton(
                text = if (loading) "VERIFYING…" else "CONFIRM",
                enabled = !loading && code.isNotBlank(),
                loading = loading,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
                onClick = {
                    loading = true
                    error = null
                    scope.launch {
                        val result = viewModel.verifyNodePassword(code, "user")
                        loading = false
                        result.onSuccess { res ->
                            if (res.success) onVerified() else error = res.message ?: "Incorrect passcode"
                        }.onFailure { error = it.message }
                    }
                },
            )
            CedalGhostButton(text = "CANCEL", onClick = onCancel)
        }
    }
}
