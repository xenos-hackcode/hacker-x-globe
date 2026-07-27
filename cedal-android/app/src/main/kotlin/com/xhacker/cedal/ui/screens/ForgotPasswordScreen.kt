package com.xhacker.cedal.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import kotlinx.coroutines.launch

private enum class ForgotPasswordStep { REQUEST, RESET }

// SignInScreen > "Forgot password?" - two identity checks stack before any
// reset key goes out: the node passcode (see AuthService.forgotPassword),
// plus proving control of whichever channel (SMS/email) the key was sent
// through. The server never reveals which part was wrong on a bad attempt -
// this screen always shows the same "if that's right, a code is on its
// way" message and moves to the RESET step regardless, matching that.
@Composable
fun ForgotPasswordScreen(onDone: () -> Unit, onBack: () -> Unit, viewModel: AuthViewModel = hiltViewModel()) {
    var step by remember { mutableStateOf(ForgotPasswordStep.REQUEST) }
    var email by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var passcode by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var showChannelPicker by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val hasIdentifier = email.isNotBlank() || phoneNumber.isNotBlank()

    fun requestCode(via: String) {
        showChannelPicker = false
        loading = true; error = null
        scope.launch {
            viewModel.forgotPassword(email.ifBlank { null }, phoneNumber.ifBlank { null }, passcode, via)
            loading = false
            step = ForgotPasswordStep.RESET
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(CedalColors.Background).imePadding().padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        CedalCard {
            CedalHeader("CEDAL NODE", "PASSWORD RECOVERY")

            when (step) {
                ForgotPasswordStep.REQUEST -> {
                    CedalSectionLabel("PRIMARY LINK", "EMAIL — FILL EITHER THIS OR PHONE")
                    CedalTextField(
                        value = email,
                        onValueChange = { email = it },
                        prefix = "⧉",
                        placeholder = "your access email",
                        keyboardType = KeyboardType.Email,
                        modifier = Modifier.padding(bottom = 10.dp),
                    )

                    CedalSectionLabel("PHONE NUMBER", "OR FILL THIS INSTEAD")
                    CedalTextField(
                        value = phoneNumber,
                        onValueChange = { phoneNumber = it },
                        prefix = "☏",
                        placeholder = "+14155551234",
                        keyboardType = KeyboardType.Phone,
                        modifier = Modifier.padding(bottom = 10.dp),
                    )

                    CedalSectionLabel("NODE PASSCODE", "REQUIRED")
                    CedalTextField(
                        value = passcode,
                        onValueChange = { passcode = it },
                        prefix = "★",
                        placeholder = "your passcode",
                        isPassword = true,
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.padding(bottom = 10.dp),
                    )

                    Text(
                        "Your passcode is checked alongside a key sent to your email or phone - both are required to reset your password.",
                        color = CedalColors.TextSecondary, fontSize = 11.sp,
                        modifier = Modifier.padding(bottom = 10.dp),
                    )

                    CedalErrorText(error)

                    CedalPrimaryButton(
                        text = if (loading) "SENDING…" else "SEND RESET KEY",
                        enabled = !loading && hasIdentifier && passcode.isNotBlank(),
                        loading = loading,
                        modifier = Modifier.padding(top = 6.dp, bottom = 8.dp),
                        onClick = { showChannelPicker = true },
                    )

                    CedalGhostButton(text = "BACK TO SIGN IN", onClick = onBack)
                }

                ForgotPasswordStep.RESET -> {
                    Text(
                        "If that email and passcode matched, a reset key is on its way. Enter it below along with your new password.",
                        color = CedalColors.TextSecondary, fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 10.dp),
                    )
                    CedalTextField(
                        value = code,
                        onValueChange = { code = it },
                        prefix = "#",
                        placeholder = "reset key",
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.padding(bottom = 10.dp),
                    )
                    CedalSectionLabel("NEW MASTER KEY", "MIN 6 CHARS")
                    CedalTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        prefix = "★",
                        placeholder = "create a new pattern",
                        isPassword = true,
                        modifier = Modifier.padding(bottom = 10.dp),
                    )
                    CedalErrorText(error)
                    CedalPrimaryButton(
                        text = if (loading) "RESETTING…" else "RESET PASSWORD",
                        enabled = !loading,
                        loading = loading,
                        modifier = Modifier.padding(top = 6.dp, bottom = 8.dp),
                        onClick = {
                            loading = true; error = null
                            scope.launch {
                                val result = viewModel.resetPassword(email.ifBlank { null }, phoneNumber.ifBlank { null }, code, newPassword)
                                loading = false
                                result.onSuccess { onDone() }.onFailure { error = it.message }
                            }
                        },
                    )
                    CedalGhostButton(text = "BACK", onClick = { step = ForgotPasswordStep.REQUEST; error = null })
                }
            }
        }
    }

    if (showChannelPicker) {
        VerifyChannelDialog(
            title = "Send reset key via",
            onDismiss = { showChannelPicker = false },
            onChoose = { via -> requestCode(via) },
        )
    }
}
