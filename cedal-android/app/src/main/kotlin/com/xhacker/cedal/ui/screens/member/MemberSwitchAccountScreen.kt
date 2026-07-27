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
    onSwitchToDev: () -> Unit = {},
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val scope = rememberCoroutineScope()
    var accounts by remember { mutableStateOf(viewModel.storage.savedAccounts) }
    val currentUserId = viewModel.storage.userId
    var switchingId by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    // Same eligibility as EnterPasscodeScreen's own dev-mode chip - only the
    // app owner and accounts the owner has delegated developer access to
    // ever see a way into developer mode at all.
    var canSeeDeveloperMode by remember { mutableStateOf(false) }
    var pendingDevSwitch by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        viewModel.getProfile().onSuccess {
            canSeeDeveloperMode = it.email?.equals("hackerxenos06@gmail.com", ignoreCase = true) == true || it.developerAccess
        }
    }
    // Set the instant "DELETE" is tapped, cleared on cancel/confirm - shows
    // the verify overlay below rather than deleting immediately. "Remove"
    // here means a REAL, permanent server-side account delete (see
    // AuthViewModel.deleteSavedAccount), not just forgetting the login on
    // this device - exactly the kind of thing someone picking up an
    // unlocked phone shouldn't be able to trigger.
    var pendingRemoveId by remember { mutableStateOf<String?>(null) }
    // Same reasoning, now for switching TO an account too - see
    // AccountVerifyOverlay's doc comment.
    var pendingSwitchId by remember { mutableStateOf<String?>(null) }

    fun refresh() {
        accounts = viewModel.storage.savedAccounts
    }

    Box(modifier = Modifier.fillMaxSize()) {
    Column(modifier = Modifier.fillMaxSize().background(CedalColors.Background).padding(16.dp)) {
        MemberBackBar(title = "Switch Account", onBack = onBack)
        Text(
            "Every account you've signed into on this device. Tap one to switch instantly - no password needed. DELETE permanently deletes that account - you'd have to sign up fresh to use it again.",
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
                        pendingSwitchId = account.userId
                    },
                    onRemove = {
                        pendingRemoveId = account.userId
                    },
                )
            }
        }

        // Settings > More > Security - capped at
        // AuthViewModel.MAX_SAVED_ACCOUNTS (4); remove one to make room for
        // another rather than growing the list unbounded.
        if (accounts.size >= AuthViewModel.MAX_SAVED_ACCOUNTS) {
            Text(
                "You've reached the ${AuthViewModel.MAX_SAVED_ACCOUNTS}-account limit on this device. Remove one above to add another.",
                color = CedalColors.TextMuted, fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp),
            )
        } else {
            CedalGhostButton(text = "+ ADD ANOTHER ACCOUNT", modifier = Modifier.padding(top = 8.dp), onClick = onAddAccount)
        }

        if (canSeeDeveloperMode) {
            CedalGhostButton(
                text = "SWITCH TO DEVELOPER MODE",
                modifier = Modifier.padding(top = 8.dp),
                onClick = { pendingDevSwitch = true },
            )
        }
    }

    val removeId = pendingRemoveId
    if (removeId != null) {
        AccountVerifyOverlay(
            viewModel = viewModel,
            message = "Deleting this account is permanent and needs your biometrics or passcode.",
            onVerified = {
                pendingRemoveId = null
                scope.launch {
                    viewModel.deleteSavedAccount(removeId)
                        .onSuccess { refresh() }
                        .onFailure { error = it.message }
                }
            },
            onCancel = { pendingRemoveId = null },
        )
    }

    val switchId = pendingSwitchId
    if (switchId != null) {
        AccountVerifyOverlay(
            viewModel = viewModel,
            message = "Switching accounts needs your biometrics or passcode.",
            onVerified = {
                pendingSwitchId = null
                switchingId = switchId
                error = null
                scope.launch {
                    viewModel.switchAccount(switchId)
                        .onSuccess { switchingId = null; onSwitched() }
                        .onFailure {
                            // Also refresh on failure: a stale account with an
                            // invalid token gets dropped from local storage as
                            // part of the failed attempt (see
                            // AuthViewModel.switchAccount) - re-reading here is
                            // what makes it actually disappear from this list.
                            switchingId = null; error = it.message; refresh()
                        }
                }
            },
            onCancel = { pendingSwitchId = null },
        )
    }

    if (pendingDevSwitch) {
        AccountVerifyOverlay(
            viewModel = viewModel,
            message = "Switching to developer mode needs your biometrics or passcode.",
            onVerified = { pendingDevSwitch = false; onSwitchToDev() },
            onCancel = { pendingDevSwitch = false },
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
                Text("DELETE", color = CedalColors.Error, fontSize = 10.sp, letterSpacing = 0.4.sp)
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
// Generalized from what used to be remove-only (RemoveAccountVerifyOverlay)
// - now also gates switching TO another saved account, not just removing
// one. Instant, no-password account switching is convenient, but it also
// means anyone holding an unlocked phone could silently hop into a
// different saved login; requiring the SAME verification switching already
// required for removal closes that gap.
@Composable
fun AccountVerifyOverlay(
    viewModel: AuthViewModel,
    message: String,
    onVerified: () -> Unit,
    onCancel: () -> Unit,
    // Godmode's ban/unban/clear-data (see GodmodeScreen) - "no passcode,
    // only fingerprint" per the app owner's own words: a passcode is
    // something anyone holding the phone could be told/forced to type, a
    // fingerprint isn't. Checks the device's actual biometric hardware
    // directly (BiometricAuth.authenticate already does this) rather than
    // gating on Settings > Security > "Biometric Unlock" being toggled on -
    // that setting is about the passcode-fallback flows below, which don't
    // exist in this mode.
    biometricOnly: Boolean = false,
) {
    val activity = LocalContext.current as? FragmentActivity
    val canUseBiometric = activity != null && (biometricOnly || viewModel.storage.biometricEnabled)
    var code by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        if (canUseBiometric && activity != null) {
            BiometricAuth.authenticate(activity, onSuccess = onVerified, onError = { msg -> if (biometricOnly) error = msg })
        }
    }

    if (biometricOnly) {
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
                Text(message, color = CedalColors.TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(bottom = 14.dp))
                CedalErrorText(error)
                CedalPrimaryButton(
                    text = "USE FINGERPRINT",
                    modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
                    onClick = {
                        error = null
                        activity?.let { BiometricAuth.authenticate(it, onSuccess = onVerified, onError = { msg -> error = msg }) }
                    },
                )
                CedalGhostButton(text = "CANCEL", onClick = onCancel)
            }
        }
        return
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.75f)).imePadding(),
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
                message,
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
