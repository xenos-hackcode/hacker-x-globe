package com.xhacker.cedal.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xhacker.cedal.data.TermsConfig
import com.xhacker.cedal.ui.theme.CedalCard
import com.xhacker.cedal.ui.theme.CedalColors
import com.xhacker.cedal.ui.theme.CedalErrorText
import com.xhacker.cedal.ui.theme.CedalHeader
import com.xhacker.cedal.ui.theme.CedalPrimaryButton
import com.xhacker.cedal.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

@Composable
fun TermsGateScreen(
    onAccepted: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize().background(CedalColors.Background).padding(24.dp)) {
        CedalCard(modifier = Modifier.fillMaxSize(), scrollable = false) {
            CedalHeader("CEDAL NODE", "TERMS & CONDITIONS")

            Text(
                TermsConfig.TEXT,
                color = CedalColors.TextSecondary,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 12.dp),
            )

            CedalErrorText(error)

            CedalPrimaryButton(
                text = if (loading) "BINDING…" else "ACCEPT & CONTINUE",
                enabled = !loading,
                loading = loading,
                modifier = Modifier.padding(top = 6.dp),
                onClick = {
                    loading = true; error = null
                    scope.launch {
                        val result = viewModel.acceptTerms(TermsConfig.CURRENT_VERSION)
                        loading = false
                        result.onSuccess { onAccepted() }.onFailure { error = it.message }
                    }
                },
            )
        }
    }
}
