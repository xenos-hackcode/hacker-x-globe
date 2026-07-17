package com.xhacker.cedal.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.xhacker.cedal.ui.theme.CedalColors

@Composable
fun LoadingScreen(onRoute: () -> Unit) {
    LaunchedEffect(Unit) { onRoute() }
    Box(
        modifier = Modifier.fillMaxSize().background(CedalColors.Background),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = CedalColors.AccentCyan)
    }
}
