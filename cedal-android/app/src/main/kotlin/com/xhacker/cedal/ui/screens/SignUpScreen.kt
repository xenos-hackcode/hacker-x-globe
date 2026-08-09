package com.xhacker.cedal.ui.screens

import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.xhacker.cedal.ui.theme.CedalCard
import com.xhacker.cedal.ui.theme.CedalChip
import com.xhacker.cedal.ui.theme.CedalColors
import com.xhacker.cedal.ui.theme.CedalErrorText
import com.xhacker.cedal.ui.theme.CedalGhostButton
import com.xhacker.cedal.ui.theme.CedalHeader
import com.xhacker.cedal.ui.theme.CedalLinkText
import com.xhacker.cedal.ui.theme.CedalPrimaryButton
import com.xhacker.cedal.ui.theme.CedalSectionLabel
import com.xhacker.cedal.ui.theme.CedalTextField
import com.xhacker.cedal.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

private enum class SignUpStep { FORM, VERIFY_EMAIL }

@Composable
fun SignUpScreen(
    onDone: () -> Unit,
    onNavigateToSignIn: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    var step by remember { mutableStateOf(SignUpStep.FORM) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    // Split so the user picks their country code separately from the rest
    // of their number (which they'll naturally type with its local leading
    // digit, e.g. UK's "07..."). Combined into E.164 below - the leading
    // "0" only makes sense within its own country, so it's dropped when
    // building the number the server actually stores/dials.
    var countryCode by remember { mutableStateOf("") }
    var localNumber by remember { mutableStateOf("") }
    val phoneNumber by remember {
        derivedStateOf {
            val cc = countryCode.trim().trimStart('+').filter { it.isDigit() }
            val local = localNumber.trim().trimStart('0').filter { it.isDigit() }
            if (cc.isBlank() || local.isBlank()) "" else "+$cc$local"
        }
    }
    var name by remember { mutableStateOf("") }
    var handle by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var devCode by remember { mutableStateOf<String?>(null) }
    // Which channel the account-activation key actually went out through -
    // set once VerifyChannelDialog is answered, shown on the VERIFY_EMAIL
    // step so the "code sent to..." text matches reality.
    var verifyVia by remember { mutableStateOf<String?>(null) }
    var showChannelPicker by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val deviceId = remember { Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) }

    // Best-effort — name/handle are optional, so a failure here (e.g. handle
    // already taken) shouldn't block finishing sign-up. Can be set later.
    suspend fun saveOptionalProfile() {
        if (name.isNotBlank() || handle.isNotBlank()) {
            viewModel.updateProfile(name.ifBlank { null }, handle.ifBlank { null })
        }
    }

    fun doSignup(via: String) {
        showChannelPicker = false
        verifyVia = via
        loading = true; error = null
        scope.launch {
            val result = viewModel.signup(email, password, guest = false, phoneNumber = phoneNumber, verifyVia = via)
            loading = false
            result.onSuccess { res ->
                if (res.emailVerificationRequired) {
                    devCode = res.devVerificationCode
                    step = SignUpStep.VERIFY_EMAIL
                } else {
                    saveOptionalProfile()
                    onDone()
                }
            }.onFailure { error = it.message }
        }
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
            CedalHeader("CEDAL NODE", "PROFILE CREATION CONSOLE")

            when (step) {
                SignUpStep.FORM -> {
                    CedalSectionLabel("PRIMARY LINK", "EMAIL")
                    CedalTextField(
                        value = email,
                        onValueChange = { email = it },
                        prefix = "⧉",
                        placeholder = "enter your access email",
                        keyboardType = KeyboardType.Email,
                        modifier = Modifier.padding(bottom = 10.dp),
                    )

                    CedalSectionLabel("MASTER KEY", "MIN 6 CHARS")
                    CedalTextField(
                        value = password,
                        onValueChange = { password = it },
                        prefix = "★",
                        placeholder = "create a secure pattern",
                        isPassword = true,
                        modifier = Modifier.padding(bottom = 10.dp),
                    )

                    CedalSectionLabel("PHONE NUMBER", "REQUIRED")
                    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                        CedalTextField(
                            value = countryCode,
                            onValueChange = { countryCode = it },
                            prefix = "+",
                            placeholder = "44",
                            keyboardType = KeyboardType.Phone,
                            modifier = Modifier.weight(0.32f).padding(end = 8.dp),
                        )
                        CedalTextField(
                            value = localNumber,
                            onValueChange = { localNumber = it },
                            prefix = "☏",
                            placeholder = "07123456789",
                            keyboardType = KeyboardType.Phone,
                            modifier = Modifier.weight(0.68f),
                        )
                    }

                    CedalSectionLabel("DISPLAY NAME", "OPTIONAL")
                    CedalTextField(
                        value = name,
                        onValueChange = { name = it },
                        prefix = "⧉",
                        placeholder = "how others see you",
                        modifier = Modifier.padding(bottom = 10.dp),
                    )

                    CedalSectionLabel("HANDLE", "OPTIONAL")
                    CedalTextField(
                        value = handle,
                        onValueChange = { handle = it },
                        prefix = "@",
                        placeholder = "unique_handle",
                        modifier = Modifier.padding(bottom = 10.dp),
                    )

                    Row(modifier = Modifier.padding(bottom = 10.dp)) {
                        CedalChip(if (loading) "WRITING NODE SEED…" else "NODE SEED READY")
                    }

                    CedalErrorText(error)

                    CedalPrimaryButton(
                        text = if (loading) "SPAWNING NODE…" else "CREATE NODE",
                        enabled = !loading,
                        loading = loading,
                        modifier = Modifier.padding(top = 6.dp, bottom = 8.dp),
                        onClick = { showChannelPicker = true },
                    )

                    CedalGhostButton(
                        text = "SPAWN GUEST NODE",
                        enabled = !loading,
                        modifier = Modifier.padding(bottom = 8.dp),
                        onClick = {
                            loading = true; error = null
                            scope.launch {
                                val result = viewModel.signup(null, null, guest = true, deviceId = deviceId)
                                loading = false
                                result.onSuccess {
                                    saveOptionalProfile()
                                    onDone()
                                }.onFailure { error = it.message }
                            }
                        },
                    )

                    CedalLinkText("Already have a node? Access console", onClick = onNavigateToSignIn)
                }

                SignUpStep.VERIFY_EMAIL -> {
                    Text(
                        if (verifyVia == "sms") "Enter the 6-digit code texted to your phone" else "Enter the 6-digit code sent to your email",
                        color = CedalColors.TextSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 10.dp),
                    )
                    devCode?.let {
                        Text(
                            "DEV BUILD — no ${if (verifyVia == "sms") "SMS relay" else "email provider"} wired up yet, your code is: $it",
                            color = CedalColors.AccentSky,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(bottom = 10.dp),
                        )
                    }
                    CedalTextField(
                        value = code,
                        onValueChange = { code = it },
                        prefix = "#",
                        placeholder = "verification code",
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.padding(bottom = 10.dp),
                    )
                    CedalErrorText(error)
                    CedalPrimaryButton(
                        text = if (loading) "VERIFYING…" else "VERIFY",
                        enabled = !loading,
                        loading = loading,
                        modifier = Modifier.padding(top = 6.dp),
                        onClick = {
                            loading = true; error = null
                            scope.launch {
                                val uid = viewModel.storage.userId!!
                                val verifyResult = viewModel.verifyEmail(uid, code)
                                verifyResult.onSuccess {
                                    val loginResult = viewModel.login(email, password)
                                    loading = false
                                    loginResult.onSuccess {
                                        saveOptionalProfile()
                                        onDone()
                                    }.onFailure { error = it.message }
                                }.onFailure {
                                    loading = false
                                    error = it.message
                                }
                            }
                        },
                    )
                }
            }
        }
    }

    if (showChannelPicker) {
        VerifyChannelDialog(
            title = "Verify your new node",
            onDismiss = { showChannelPicker = false },
            onChoose = { via -> doSignup(via) },
        )
    }
}
