package com.xhacker.cedal.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

// The single Compose-observable source of truth for light/dark - reading
// isDark inside a composable (even indirectly, via a CedalColors property
// getter below) registers a recomposition dependency, so flipping this one
// flag is enough to re-theme every screen already on screen. Seeded from
// SecureStorage.darkThemeEnabled once at process start (see MainActivity);
// the Settings > Navigation > Theme toggle writes both at once.
object ThemeState {
    var isDark by mutableStateOf(true)

    // null = the free default cyan look. Otherwise a purchased theme pack's
    // accent color (see ThemePackService/MemberShopScreen's "Theme Packs"
    // section) - overrides AccentCyan/BorderCyan below, everywhere. Seeded
    // from SecureStorage.equippedThemePackHex at process start, same as isDark.
    var equippedAccentColor by mutableStateOf<Color?>(null)
}

// Ported from cedal-mobile's ThemeContext.tsx (dark palette) and the
// sign-up/sign-in screen stylesheets — the "HUD terminal" look. Every color
// is a computed property (not a plain val) so it can pick between the dark
// and light variant based on ThemeState.isDark at read time.
object CedalColors {
    val Background: Color get() = if (ThemeState.isDark) Color(0xFF020617) else Color(0xFFF8FAFC)
    val BackgroundBlob: Color get() = if (ThemeState.isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9)
    val CardBackground: Color get() = if (ThemeState.isDark) Color(0xF20F172A) else Color(0xF2FFFFFF)

    val AccentCyan: Color get() = ThemeState.equippedAccentColor
        ?: if (ThemeState.isDark) Color(0xFF22D3EE) else Color(0xFF0891B2)
    val AccentSky: Color get() = if (ThemeState.isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
    val AccentIndigo: Color get() = if (ThemeState.isDark) Color(0xFFA5B4FC) else Color(0xFF6366F1)

    val TextPrimary: Color get() = if (ThemeState.isDark) Color(0xFFE5E7EB) else Color(0xFF0F172A)
    val TextSecondary: Color get() = if (ThemeState.isDark) Color(0xFF9CA3AF) else Color(0xFF475569)
    val TextMuted: Color get() = if (ThemeState.isDark) Color(0xFF64748B) else Color(0xFF94A3B8)
    val TextLink: Color get() = if (ThemeState.isDark) Color(0xFF93C5FD) else Color(0xFF2563EB)

    val BorderSlate: Color get() = if (ThemeState.isDark) Color(0x9994A3B8) else Color(0x99CBD5E1)
    val BorderCyan: Color get() = (ThemeState.equippedAccentColor ?: if (ThemeState.isDark) Color(0xFF38BDF8) else Color(0xFF0891B2)).copy(alpha = 0xE5 / 255f)

    val Success: Color get() = if (ThemeState.isDark) Color(0xFF22C55E) else Color(0xFF16A34A)
    val SuccessKnob = Color(0xFFBBF7D0)
    val ToggleOffKnob = Color(0xFF64748B)

    val Error: Color get() = if (ThemeState.isDark) Color(0xFFF87171) else Color(0xFFDC2626)
}
