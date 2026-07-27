package com.xhacker.cedal.ui.screens.member

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xhacker.cedal.data.PopularitySettingsDto
import com.xhacker.cedal.ui.theme.CedalColors
import com.xhacker.cedal.ui.theme.CedalErrorText
import com.xhacker.cedal.ui.theme.CedalGhostButton
import com.xhacker.cedal.ui.theme.CedalPrimaryButton
import com.xhacker.cedal.ui.theme.CedalTextField
import com.xhacker.cedal.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

// "1 account per phone number" - a verified phone number is unique across
// every account server-side (see SecurityService/Users.phoneNumber), and
// combined with MemberSwitchAccountScreen's 4-saved-account cap, this is
// what actually limits how many accounts one person can keep going on this
// device: each of the (at most 4) saved accounts needs its own distinct
// verified number.
@Composable
fun MemberSecurityBody(onBack: () -> Unit, viewModel: AuthViewModel = hiltViewModel()) {
    val scope = rememberCoroutineScope()
    var phoneNumber by remember { mutableStateOf<String?>(null) }
    var phoneVerified by remember { mutableStateOf(false) }
    var input by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var codeSent by remember { mutableStateOf(false) }
    var devCode by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    // "Change Number" needs the same biometric/passcode gate as everything
    // else identity-sensitive in this app - see AccountVerifyOverlay.
    var pendingChangeVerify by remember { mutableStateOf(false) }

    fun refresh() {
        scope.launch {
            viewModel.getPhoneStatus().onSuccess {
                phoneNumber = it.phoneNumber
                phoneVerified = it.phoneVerified
            }
        }
    }
    LaunchedEffect(Unit) { refresh() }

    Column(modifier = Modifier.fillMaxSize().background(CedalColors.Background).padding(16.dp).imePadding()) {
        MemberBackBar(title = "Security", onBack = onBack)
        Text(
            "Cedal enforces one account per phone number, and up to ${AuthViewModel.MAX_SAVED_ACCOUNTS} saved accounts on this device - verify your number to keep your account secured.",
            color = CedalColors.TextMuted, fontSize = 12.sp, modifier = Modifier.padding(bottom = 16.dp),
        )
        CedalErrorText(error)

        val verifiedNumber = phoneNumber
        if (phoneVerified && verifiedNumber != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(CedalColors.CardBackground)
                    .border(1.dp, CedalColors.BorderCyan, RoundedCornerShape(14.dp))
                    .padding(16.dp),
            ) {
                Text("Verified phone number", color = CedalColors.TextSecondary, fontSize = 11.sp)
                Text(verifiedNumber, color = CedalColors.TextPrimary, fontSize = 16.sp, modifier = Modifier.padding(top = 2.dp, bottom = 4.dp))
                Text("✓ Verified", color = CedalColors.Success, fontSize = 12.sp)
            }
            CedalGhostButton(
                text = "CHANGE PHONE NUMBER",
                modifier = Modifier.padding(top = 12.dp),
                onClick = { pendingChangeVerify = true },
            )
        } else if (!codeSent) {
            CedalTextField(
                value = input,
                onValueChange = { input = it },
                prefix = "📱",
                placeholder = "+14155551234",
                keyboardType = KeyboardType.Phone,
            )
            CedalPrimaryButton(
                text = "SEND CODE",
                loading = loading,
                modifier = Modifier.padding(top = 12.dp),
                onClick = {
                    error = null
                    loading = true
                    scope.launch {
                        viewModel.requestPhoneCode(input.trim())
                            .onSuccess { dev -> devCode = dev; codeSent = true }
                            .onFailure { error = it.message }
                        loading = false
                    }
                },
            )
        } else {
            Text(
                "Enter the 6-digit code sent to $input",
                color = CedalColors.TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(bottom = 10.dp),
            )
            devCode?.let {
                Text(
                    "DEV BUILD — no SMS provider configured yet, your code is: $it",
                    color = CedalColors.AccentSky, fontSize = 12.sp, modifier = Modifier.padding(bottom = 10.dp),
                )
            }
            CedalTextField(
                value = code,
                onValueChange = { if (it.length <= 6) code = it },
                prefix = "#",
                placeholder = "verification code",
                keyboardType = KeyboardType.Number,
            )
            CedalPrimaryButton(
                text = "VERIFY",
                loading = loading,
                modifier = Modifier.padding(top = 12.dp),
                onClick = {
                    error = null
                    loading = true
                    scope.launch {
                        viewModel.verifyPhoneCode(code.trim())
                            .onSuccess { refresh(); codeSent = false; code = ""; devCode = null }
                            .onFailure { error = it.message }
                        loading = false
                    }
                },
            )
            CedalGhostButton(
                text = "USE A DIFFERENT NUMBER",
                modifier = Modifier.padding(top = 8.dp),
                onClick = { codeSent = false; code = ""; devCode = null; error = null },
            )
        }

        PopularitySection(viewModel)
    }

    if (pendingChangeVerify) {
        AccountVerifyOverlay(
            viewModel = viewModel,
            message = "Changing your phone number needs your biometrics or passcode.",
            onVerified = {
                pendingChangeVerify = false
                phoneVerified = false; phoneNumber = null; input = ""; codeSent = false; code = ""; devCode = null; error = null
            },
            onCancel = { pendingChangeVerify = false },
        )
    }
}

// Settings > Security > Popularity - lets a user filter which of their own
// profile fields (name/pfp/age/rank) other people can see. Global, applies
// to every viewer - see MemberChatThreadScreen's per-chat override for a
// narrower, single-viewer version of the same toggles.
@Composable
private fun PopularitySection(viewModel: AuthViewModel) {
    var settings by remember { mutableStateOf(PopularitySettingsDto()) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.getPopularitySettings().onSuccess { settings = it }
    }

    fun update(next: PopularitySettingsDto) {
        if (loading) return
        val previous = settings
        settings = next
        loading = true
        scope.launch {
            viewModel.setPopularitySettings(next).onFailure { settings = previous }
            loading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 20.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(CedalColors.CardBackground)
            .border(1.dp, CedalColors.BorderSlate, RoundedCornerShape(14.dp)),
    ) {
        Text(
            "Popularity",
            color = CedalColors.TextPrimary, fontSize = 13.sp,
            modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 2.dp),
        )
        Text(
            "Choose what other people can see on your profile.",
            color = CedalColors.TextMuted, fontSize = 11.sp,
            modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 6.dp),
        )
        SettingsToggleRow("Name", "Your nickname/handle.", settings.showName) { update(settings.copy(showName = it)) }
        SettingsToggleRow("Profile Picture", "Your avatar.", settings.showPfp) { update(settings.copy(showPfp = it)) }
        SettingsToggleRow("Age", "Your age.", settings.showAge) { update(settings.copy(showAge = it)) }
        SettingsToggleRow("Rank", "Your Human-Godhood rank.", settings.showRank) { update(settings.copy(showRank = it)) }
        SettingsToggleRow("Occupation", "Your occupation.", settings.showOccupation) { update(settings.copy(showOccupation = it)) }
        SettingsToggleRow("Hobby", "Your hobby.", settings.showHobby) { update(settings.copy(showHobby = it)) }
        SettingsToggleRow("Bio", "Your bio.", settings.showBio) { update(settings.copy(showBio = it)) }
        SettingsToggleRow("Gender", "Your gender.", settings.showGender) { update(settings.copy(showGender = it)) }
    }
}
