package com.xhacker.cedal.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xhacker.cedal.ui.AccountGateState
import com.xhacker.cedal.ui.theme.CedalCard
import com.xhacker.cedal.ui.theme.CedalColors
import com.xhacker.cedal.ui.theme.CedalErrorText
import com.xhacker.cedal.ui.theme.CedalGhostButton
import com.xhacker.cedal.ui.theme.CedalHeader
import com.xhacker.cedal.ui.theme.CedalPrimaryButton
import com.xhacker.cedal.ui.theme.CedalTextField
import androidx.compose.material3.Text
import com.xhacker.cedal.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

// Live in-session ban/clear-data gate - rendered as a full-screen overlay at
// the very top of CedalNavGraph (same pattern as AppLockState's
// EnterPasscodeScreen), covering whatever screen was on-screen when
// BalloonPopupOverlay's poll noticed AccountGateState.active. "Everything in
// the app should disappear" per the app owner's own words - this is why it's
// a full opaque overlay rather than a normal nav destination.
@Composable
fun AccountGateScreen(onCancel: () -> Unit, viewModel: AuthViewModel = hiltViewModel()) {
    var email by remember { mutableStateOf<String?>(null) }
    var appealOpen by remember { mutableStateOf(false) }
    var appealText by remember { mutableStateOf("") }
    var appealSent by remember { mutableStateOf(false) }
    var appealSending by remember { mutableStateOf(false) }
    var appealError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.getProfile().onSuccess { email = it.email }
    }

    val permanent = AccountGateState.permanent

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CedalColors.Background)
            .imePadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        CedalCard {
            CedalHeader("ACCESS DENIED", "NEURAL SIGN IN PANEL")
            Text(
                "Your account has been deleted.",
                color = CedalColors.Error, fontSize = 16.sp, modifier = Modifier.padding(top = 8.dp, bottom = 10.dp),
            )

            if (!permanent) {
                Text(
                    "If this can be restored, you can make a request. After 24 hours with no response, your account will be permanently removed.",
                    color = CedalColors.TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(bottom = 10.dp),
                )
            }

            if (!permanent) {
                if (appealSent) {
                    Text(
                        "Your appeal has been sent to Cedal's admin.",
                        color = CedalColors.Success, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp),
                    )
                } else if (appealOpen) {
                    CedalTextField(
                        value = appealText,
                        onValueChange = { appealText = it },
                        prefix = "✎",
                        placeholder = "Why should this be reconsidered?",
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                    CedalErrorText(appealError)
                    CedalPrimaryButton(
                        text = if (appealSending) "SENDING…" else "SUBMIT APPEAL",
                        enabled = !appealSending && appealText.isNotBlank() && email != null,
                        loading = appealSending,
                        modifier = Modifier.padding(bottom = 8.dp),
                        onClick = {
                            val addr = email ?: return@CedalPrimaryButton
                            appealSending = true
                            appealError = null
                            scope.launch {
                                viewModel.submitAppeal(addr, "banned", appealText)
                                    .onSuccess { appealSent = true }
                                    .onFailure { appealError = it.message }
                                appealSending = false
                            }
                        },
                    )
                } else {
                    CedalPrimaryButton(text = "APPEAL", modifier = Modifier.padding(bottom = 8.dp), onClick = { appealOpen = true })
                }
            }

            CedalGhostButton(
                text = "CANCEL",
                modifier = Modifier.padding(top = 4.dp),
                onClick = {
                    viewModel.storage.clearSession()
                    AccountGateState.active = false
                    onCancel()
                },
            )
        }
    }
}
