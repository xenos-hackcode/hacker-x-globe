package com.xhacker.cedal.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

@Composable
fun CedalTheme(content: @Composable () -> Unit) {
    // Built inside the composable (not a top-level val) so reading
    // CedalColors.* here - which itself reads ThemeState.isDark - re-runs
    // whenever the theme flips, instead of baking in whatever was current
    // the first time this object was ever constructed.
    val scheme = if (ThemeState.isDark) {
        darkColorScheme(
            primary = CedalColors.AccentCyan,
            onPrimary = CedalColors.Background,
            background = CedalColors.Background,
            onBackground = CedalColors.TextPrimary,
            surface = CedalColors.BackgroundBlob,
            onSurface = CedalColors.TextPrimary,
            onSurfaceVariant = CedalColors.TextSecondary,
            error = CedalColors.Error,
        )
    } else {
        lightColorScheme(
            primary = CedalColors.AccentCyan,
            onPrimary = CedalColors.Background,
            background = CedalColors.Background,
            onBackground = CedalColors.TextPrimary,
            surface = CedalColors.BackgroundBlob,
            onSurface = CedalColors.TextPrimary,
            onSurfaceVariant = CedalColors.TextSecondary,
            error = CedalColors.Error,
        )
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
