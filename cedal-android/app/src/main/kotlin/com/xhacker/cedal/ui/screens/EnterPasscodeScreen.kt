package com.xhacker.cedal.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import com.xhacker.cedal.ui.theme.CedalCard
import com.xhacker.cedal.ui.theme.CedalColors
import com.xhacker.cedal.ui.theme.CedalErrorText
import com.xhacker.cedal.ui.theme.CedalGhostButton
import com.xhacker.cedal.ui.theme.CedalHeader
import com.xhacker.cedal.ui.theme.CedalPrimaryButton
import com.xhacker.cedal.ui.theme.CedalSectionLabel
import com.xhacker.cedal.ui.theme.CedalTextField
import com.xhacker.cedal.viewmodel.AuthViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun EnterPasscodeScreen(
    onSuccess: (role: String) -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    var mode by remember { mutableStateOf("user") }
    var code by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var lockUntil by remember { mutableStateOf<Long?>(null) }
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Biometric unlock replaces typing the passcode entirely — it doesn't
    // re-verify against the server, it just resumes the session already
    // proven at sign-in, the same way a phone's own lock screen re-admits
    // you to apps you're already signed into. Only offered for "user" mode:
    // stepping up to developer still requires the dev key every time.
    val canUseBiometric = mode == "user" && viewModel.storage.biometricEnabled && viewModel.storage.accessToken != null
    var autoPromptShown by remember { mutableStateOf(false) }

    // Fires the fingerprint/face prompt the instant this screen appears —
    // "enter straight away" instead of needing an extra tap first. A
    // cancel/failure just falls back to typing the passcode; it's silent
    // (no error banner) so a stray dismiss doesn't look like a failure.
    LaunchedEffect(canUseBiometric) {
        if (canUseBiometric && !autoPromptShown) {
            autoPromptShown = true
            val activity = context as? FragmentActivity
            activity?.let {
                BiometricAuth.authenticate(
                    it,
                    onSuccess = { onSuccess(viewModel.storage.role ?: "user") },
                    onError = { },
                )
            }
        }
    }

    LaunchedEffect(lockUntil) {
        while (lockUntil != null && now < lockUntil!!) {
            delay(1000)
            now = System.currentTimeMillis()
        }
    }

    val locked = lockUntil != null && now < lockUntil!!

    Box(
        modifier = Modifier.fillMaxSize().background(CedalColors.Background).padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        CedalCard {
            CedalHeader("CEDAL NODE", "NODE PASSWORD")

            CedalSectionLabel("ACCESS MODE", mode)
            Row(modifier = Modifier.padding(bottom = 10.dp)) {
                Box(modifier = Modifier.padding(end = 8.dp)) {
                    ModeChip(label = "USER", selected = mode == "user") { mode = "user" }
                }
                ModeChip(label = "DEVELOPER", selected = mode == "developer") { mode = "developer" }
            }

            CedalTextField(
                value = code,
                onValueChange = { code = it },
                prefix = "★",
                placeholder = if (mode == "developer") "dev key" else "passcode",
                isPassword = true,
                keyboardType = if (mode == "developer") KeyboardType.Text else KeyboardType.Number,
                enabled = !locked,
                modifier = Modifier.padding(bottom = 10.dp),
            )

            if (locked) {
                val remainingSec = ((lockUntil!! - now) / 1000).coerceAtLeast(0)
                Text(
                    "Locked out. Try again in ${remainingSec / 60}m ${remainingSec % 60}s",
                    color = CedalColors.Error,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            CedalErrorText(error)

            CedalPrimaryButton(
                text = if (loading) "VERIFYING…" else "CONFIRM",
                enabled = !loading && !locked,
                loading = loading,
                modifier = Modifier.padding(top = 6.dp),
                onClick = {
                    loading = true; error = null
                    scope.launch {
                        val result = viewModel.verifyNodePassword(code, mode)
                        loading = false
                        result.onSuccess { res ->
                            if (res.success && res.role != null) {
                                onSuccess(res.role)
                            } else {
                                error = res.message
                                lockUntil = res.lockUntil
                            }
                        }.onFailure { error = it.message }
                    }
                },
            )

            if (canUseBiometric) {
                CedalGhostButton(
                    text = "TOUCH PANEL — skip passcode",
                    modifier = Modifier.padding(top = 8.dp),
                    onClick = {
                        val activity = context as? FragmentActivity ?: return@CedalGhostButton
                        BiometricAuth.authenticate(
                            activity,
                            onSuccess = { onSuccess(viewModel.storage.role ?: "user") },
                            onError = { error = it },
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun ModeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) CedalColors.AccentCyan else CedalColors.BackgroundBlob)
            .border(1.dp, if (selected) CedalColors.AccentCyan else CedalColors.BorderSlate, RoundedCornerShape(50))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(
            label,
            color = if (selected) CedalColors.Background else CedalColors.TextSecondary,
            fontSize = 11.sp,
            letterSpacing = 1.5.sp,
        )
    }
}
