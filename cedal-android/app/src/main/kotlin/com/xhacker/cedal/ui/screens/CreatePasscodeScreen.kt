package com.xhacker.cedal.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.xhacker.cedal.ui.theme.CedalCard
import com.xhacker.cedal.ui.theme.CedalColors
import com.xhacker.cedal.ui.theme.CedalErrorText
import com.xhacker.cedal.ui.theme.CedalHeader
import com.xhacker.cedal.ui.theme.CedalPrimaryButton
import com.xhacker.cedal.ui.theme.CedalSectionLabel
import com.xhacker.cedal.ui.theme.CedalTextField
import com.xhacker.cedal.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

@Composable
fun CreatePasscodeScreen(
    onDone: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    var code by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var favoriteColor by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier.fillMaxSize().background(CedalColors.Background).padding(24.dp).imePadding(),
        contentAlignment = Alignment.Center,
    ) {
        CedalCard {
            CedalHeader("CEDAL NODE", "BIND DEVICE PASSCODE")

            CedalSectionLabel("NODE PASSCODE", "4-6 DIGITS")
            CedalTextField(
                value = code,
                onValueChange = { code = it },
                prefix = "★",
                placeholder = "set your passcode",
                isPassword = true,
                keyboardType = KeyboardType.Number,
                modifier = Modifier.padding(bottom = 10.dp),
            )

            CedalSectionLabel("CONFIRM PASSCODE", "REPEAT")
            CedalTextField(
                value = confirm,
                onValueChange = { confirm = it },
                prefix = "★",
                placeholder = "confirm your passcode",
                isPassword = true,
                keyboardType = KeyboardType.Number,
                modifier = Modifier.padding(bottom = 10.dp),
            )

            CedalSectionLabel("RECOVERY AGE", "REQUIRED")
            CedalTextField(
                value = age,
                onValueChange = { age = it },
                prefix = "⧉",
                placeholder = "your age",
                keyboardType = KeyboardType.Number,
                modifier = Modifier.padding(bottom = 10.dp),
            )

            CedalSectionLabel("RECOVERY COLOR", "REQUIRED")
            CedalTextField(
                value = favoriteColor,
                onValueChange = { favoriteColor = it },
                prefix = "⧉",
                placeholder = "favorite color",
                modifier = Modifier.padding(bottom = 10.dp),
            )

            CedalErrorText(error)

            CedalPrimaryButton(
                text = if (loading) "BINDING…" else "SAVE",
                enabled = !loading,
                loading = loading,
                modifier = Modifier.padding(top = 6.dp),
                onClick = {
                    if (code.length !in 4..6) { error = "Passcode must be 4-6 digits"; return@CedalPrimaryButton }
                    if (code != confirm) { error = "Passcodes do not match"; return@CedalPrimaryButton }
                    if (age.isBlank() || favoriteColor.isBlank()) { error = "Recovery fields are required"; return@CedalPrimaryButton }
                    loading = true; error = null
                    scope.launch {
                        val result = viewModel.createPasscode(code, age.toIntOrNull() ?: 0, favoriteColor)
                        loading = false
                        result.onSuccess { onDone() }.onFailure { error = it.message }
                    }
                },
            )
        }
    }
}
