package com.xhacker.cedal.ui.screens

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

object BiometricAuth {
    fun authenticate(activity: FragmentActivity, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val canAuth = BiometricManager.from(activity)
            .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)
        if (canAuth != BiometricManager.BIOMETRIC_SUCCESS) {
            onError("Biometric unlock not available on this device")
            return
        }

        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    onError(errString.toString())
                }
            },
        )

        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Cedal node")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
            .setNegativeButtonText("Cancel")
            .build()

        prompt.authenticate(info)
    }
}
