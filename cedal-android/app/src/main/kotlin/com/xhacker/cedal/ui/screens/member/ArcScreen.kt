package com.xhacker.cedal.ui.screens.member

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xhacker.cedal.ui.theme.CedalColors
import com.xhacker.cedal.viewmodel.AuthViewModel

// ARC (formerly "Hack") - legal, ethical cybersecurity education. Four tabs:
// Learn (leveled lessons + quizzes, see ArcLearnScreen.kt), Labs (a real
// local-network scanner reskinned as a radar HUD, see ArcLabsScreen.kt),
// Assistant (a real AI tutor chat, see ArcAssistantScreen.kt), and Ops (the
// sandboxed "attack simulation" missions, see ArcOpsScreen.kt).
private enum class ArcTab { LEARN, LABS, ASSISTANT, OPS }

@Composable
fun ArcRouterBody(onBack: () -> Unit, highlightId: String? = null, viewModel: AuthViewModel = hiltViewModel()) {
    var activeTab by remember { mutableStateOf(if (highlightId != null) ArcTab.ASSISTANT else ArcTab.LEARN) }

    Column(modifier = Modifier.fillMaxSize().background(CedalColors.Background)) {
        Box(modifier = Modifier.weight(1f).padding(16.dp)) {
            when (activeTab) {
                ArcTab.LEARN -> ArcLearnBody(onBack = onBack, viewModel = viewModel)
                ArcTab.LABS -> ArcLabsBody(onBack = onBack)
                ArcTab.ASSISTANT -> ArcAssistantBody(onBack = onBack, highlightId = highlightId, viewModel = viewModel)
                ArcTab.OPS -> ArcOpsBody(onBack = onBack, viewModel = viewModel)
            }
        }
        // Hidden while the keyboard is up (e.g. typing in the Assistant
        // chat sub-tab) - it would otherwise still reserve its fixed height
        // even though it renders entirely behind/under the keyboard,
        // needlessly shrinking the chat area above it for no visible benefit.
        if (WindowInsets.ime.getBottom(LocalDensity.current) <= 0) {
            ArcBottomBar(activeTab) { tab -> activeTab = tab }
        }
    }
}

@Composable
private fun ArcBottomBar(active: ArcTab, onNavigateTab: (ArcTab) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .background(CedalColors.CardBackground)
            .border(width = Dp.Hairline, color = CedalColors.BorderSlate),
    ) {
        ArcTabItem("Learn", Icons.Outlined.MenuBook, active == ArcTab.LEARN) { onNavigateTab(ArcTab.LEARN) }
        ArcTabItem("Labs", Icons.Outlined.Wifi, active == ArcTab.LABS) { onNavigateTab(ArcTab.LABS) }
        ArcTabItem("Assistant", Icons.Outlined.Chat, active == ArcTab.ASSISTANT) { onNavigateTab(ArcTab.ASSISTANT) }
        ArcTabItem("Ops", Icons.Outlined.Shield, active == ArcTab.OPS) { onNavigateTab(ArcTab.OPS) }
    }
}

@Composable
private fun ArcTabItem(label: String, icon: ImageVector, active: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick),
    ) {
        Icon(icon, contentDescription = label, tint = if (active) CedalColors.Success else CedalColors.TextMuted, modifier = Modifier.size(20.dp))
        Text(label, color = if (active) CedalColors.Success else CedalColors.TextMuted, fontSize = 11.sp, modifier = Modifier.padding(top = 3.dp))
    }
}
