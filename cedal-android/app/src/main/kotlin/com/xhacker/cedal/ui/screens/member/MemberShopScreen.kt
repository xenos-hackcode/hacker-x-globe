package com.xhacker.cedal.ui.screens.member

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xhacker.cedal.ui.theme.CedalChip
import com.xhacker.cedal.ui.theme.CedalColors
import com.xhacker.cedal.ui.theme.CedalGhostButton
import com.xhacker.cedal.ui.theme.ThemeState
import com.xhacker.cedal.viewmodel.AuthViewModel

// The Arsenal (formerly "Shop") - this app was never going to charge people
// real money (see REMINDER.md), so instead of a purchase flow this is just
// a free accent-color pack per "team", equipped locally (same persistence
// spot the old paid Theme Packs used - viewModel.storage.equippedThemePackId
// / equippedThemePackHex / ThemeState.equippedAccentColor) with no server
// round-trip and nothing to buy. The old paid Tier/XP/Subscriptions/Theme
// Pack purchase flow (PurchaseXpService, ThemePackDto, purchaseThemePack)
// is untouched server-side but nothing here calls it anymore.
private data class TeamPack(val id: String, val name: String, val hex: String)

private val TEAM_PACKS = listOf(
    TeamPack("hacker", "Hacker Team", "#00FF41"),
    TeamPack("coder", "Coder Team", "#38BDF8"),
    TeamPack("cyberpunk", "Cyberpunk Team", "#FF2E9F"),
    TeamPack("ghost", "Ghost Team", "#94A3B8"),
    TeamPack("terminal", "Terminal Team", "#FFB000"),
    TeamPack("banker", "Banker Team", "#FFD700"),
    TeamPack("operator", "Operator Team", "#FF6B00"),
    TeamPack("neon", "Neon Team", "#D4FF00"),
    TeamPack("corneal", "Corneal Team", "#A78BFA"),
    TeamPack("arch", "Arch Team", "#1793D1"),
)

@Composable
fun MemberShopBody(onBack: () -> Unit, viewModel: AuthViewModel = hiltViewModel()) {
    var equippedId by remember { mutableStateOf(viewModel.storage.equippedThemePackId) }

    fun equip(team: TeamPack?) {
        equippedId = team?.id
        if (team == null) {
            viewModel.storage.equippedThemePackId = null
            viewModel.storage.equippedThemePackHex = null
            ThemeState.equippedAccentColor = null
        } else {
            viewModel.storage.equippedThemePackId = team.id
            viewModel.storage.equippedThemePackHex = team.hex
            ThemeState.equippedAccentColor = runCatching { Color(android.graphics.Color.parseColor(team.hex)) }.getOrNull()
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(CedalColors.Background).padding(16.dp)) {
        MemberBackBar(title = "Arsenal", onBack = onBack)
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            SettingsSectionCard("Teams") {
                Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                    Text(
                        "Pick a look. Every team is free - this is cosmetic only, nothing here costs anything.",
                        color = CedalColors.TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(bottom = 10.dp),
                    )
                    TeamPackRow(name = "Default Cyan", accent = CedalColors.AccentSky, equipped = equippedId == null, onEquip = { equip(null) })
                    TEAM_PACKS.forEach { team ->
                        TeamPackRow(
                            name = team.name,
                            accent = runCatching { Color(android.graphics.Color.parseColor(team.hex)) }.getOrDefault(CedalColors.AccentCyan),
                            equipped = equippedId == team.id,
                            onEquip = { equip(team) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TeamPackRow(name: String, accent: Color, equipped: Boolean, onEquip: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(accent.copy(alpha = 0.08f))
            .border(1.dp, accent.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .padding(end = 10.dp)
                .clip(CircleShape)
                .background(accent),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(name, color = CedalColors.TextPrimary, fontSize = 13.sp)
            Text("Free", color = CedalColors.TextSecondary, fontSize = 11.sp)
        }
        if (equipped) {
            CedalChip("EQUIPPED")
        } else {
            CedalGhostButton(text = "EQUIP", modifier = Modifier.fillMaxWidth(0.35f), onClick = onEquip)
        }
    }
}
