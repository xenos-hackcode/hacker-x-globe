package com.xhacker.cedal.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
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
import com.xhacker.cedal.ui.theme.CedalLinkText
import com.xhacker.cedal.ui.theme.CedalPrimaryButton
import com.xhacker.cedal.ui.theme.CedalTextField
import com.xhacker.cedal.ui.theme.CedalToggle
import com.xhacker.cedal.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

@Composable
fun SignInScreen(
    onDone: () -> Unit,
    onNavigateToSignUp: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    var email by remember { mutableStateOf(viewModel.storage.rememberEmail ?: "") }
    var password by remember { mutableStateOf("") }
    var rememberMe by remember { mutableStateOf(viewModel.storage.rememberEmail != null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    // Set only when the account has two-factor verification on — the server
    // withholds tokens until this code is confirmed via confirmLoginTwoFactor.
    var pendingTwoFactorUserId by remember { mutableStateOf<String?>(null) }
    var twoFactorCode by remember { mutableStateOf("") }
    var devCodeHint by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    fun finishLogin() {
        // Biometric unlock is an explicit opt-in from Settings > Security
        // (SecurityToggleRow), proven with an actual fingerprint/face
        // prompt there — logging in successfully doesn't turn it on by
        // itself, so the "secure your node further" nudge stays honest.
        viewModel.storage.rememberEmail = if (rememberMe) email else null
        onDone()
    }

    // No verticalScroll here - CedalCard already scrolls internally by
    // default (see CedalComponents.kt), and nesting two vertical-scroll
    // containers in the same direction crashes immediately
    // (IllegalStateException: "measured with an infinity maximum height").
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CedalColors.Background)
            .imePadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        CedalCard {
            CedalHeader("CEDAL ACCESS", "NEURAL SIGN IN PANEL")

            Text("ACCESS ID", color = CedalColors.TextSecondary, fontSize = 11.sp, letterSpacing = 2.sp, modifier = Modifier.padding(bottom = 4.dp))
            CedalTextField(
                value = email,
                onValueChange = { email = it },
                prefix = "⧉",
                placeholder = "you@cedal.app",
                keyboardType = KeyboardType.Email,
                modifier = Modifier.padding(bottom = 10.dp),
            )

            Text("PASSWORD", color = CedalColors.TextSecondary, fontSize = 11.sp, letterSpacing = 2.sp, modifier = Modifier.padding(bottom = 4.dp))
            CedalTextField(
                value = password,
                onValueChange = { password = it },
                prefix = "★",
                placeholder = "••••••••",
                isPassword = true,
                modifier = Modifier.padding(bottom = 10.dp),
            )

            Row(modifier = Modifier.padding(bottom = 10.dp)) {
                CedalToggle(checked = rememberMe, onCheckedChange = { rememberMe = it })
                Text(
                    "Remember me",
                    color = CedalColors.TextSecondary,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(start = 8.dp).align(Alignment.CenterVertically),
                )
            }

            Text(
                if (loading) "SCANNING CREDENTIALS…" else "CHANNEL READY",
                color = CedalColors.AccentIndigo,
                fontSize = 10.sp,
                letterSpacing = 1.5.sp,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            if (pendingTwoFactorUserId != null) {
                // Two-factor verification is on for this account — the
                // server withheld tokens until this code is confirmed.
                Text(
                    "Two-way verification code sent to your email.",
                    color = CedalColors.TextSecondary, fontSize = 11.sp, modifier = Modifier.padding(bottom = 6.dp),
                )
                devCodeHint?.let {
                    Text("Dev mode — code: $it", color = CedalColors.TextMuted, fontSize = 10.sp, modifier = Modifier.padding(bottom = 6.dp))
                }
                CedalTextField(
                    value = twoFactorCode,
                    onValueChange = { twoFactorCode = it },
                    prefix = "✓",
                    placeholder = "6-digit code",
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.padding(bottom = 10.dp),
                )
                CedalErrorText(error)
                CedalPrimaryButton(
                    text = if (loading) "VERIFYING…" else "CONFIRM CODE",
                    enabled = !loading,
                    loading = loading,
                    modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
                    onClick = {
                        loading = true; error = null
                        scope.launch {
                            val result = viewModel.confirmLoginTwoFactor(pendingTwoFactorUserId!!, twoFactorCode)
                            loading = false
                            result.onSuccess { finishLogin() }.onFailure { error = it.message }
                        }
                    },
                )
                CedalGhostButton(
                    text = "BACK",
                    modifier = Modifier.padding(bottom = 8.dp),
                    onClick = { pendingTwoFactorUserId = null; twoFactorCode = ""; devCodeHint = null; error = null },
                )
                return@CedalCard
            }

            CedalErrorText(error)

            CedalPrimaryButton(
                text = if (loading) "AUTHORIZING…" else "ENTER NETWORK",
                enabled = !loading,
                loading = loading,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
                onClick = {
                    loading = true; error = null
                    scope.launch {
                        val result = viewModel.login(email, password)
                        loading = false
                        result.onSuccess { res ->
                            if (res.requiresTwoFactor && res.userId != null) {
                                pendingTwoFactorUserId = res.userId
                                devCodeHint = res.devVerificationCode
                            } else {
                                finishLogin()
                            }
                        }.onFailure { error = it.message }
                    }
                },
            )

            if (viewModel.storage.biometricEnabled && viewModel.storage.accessToken != null) {
                CedalGhostButton(
                    text = "TOUCH PANEL — biometric access",
                    modifier = Modifier.padding(bottom = 8.dp),
                    onClick = {
                        val activity = context as? FragmentActivity ?: return@CedalGhostButton
                        BiometricAuth.authenticate(activity, onSuccess = onDone, onError = { error = it })
                    },
                )
            }

            CedalLinkText("create new identity", onClick = onNavigateToSignUp)
        }
    }
}
