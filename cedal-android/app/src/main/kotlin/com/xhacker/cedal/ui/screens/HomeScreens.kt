package com.xhacker.cedal.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xhacker.cedal.ui.theme.CedalCard
import com.xhacker.cedal.ui.theme.CedalColors
import com.xhacker.cedal.ui.theme.CedalGhostButton
import com.xhacker.cedal.ui.theme.CedalHeader
import com.xhacker.cedal.viewmodel.AuthViewModel

// Developer mode's real home is DeveloperHomeRoute now (see
// ui/screens/member/DeveloperScaffold.kt) - the Code/Explorer/View/Security
// tab shell. This file only still holds Owner-home, which remains a
// placeholder.

// Owner-home is still a placeholder - see AuthService.verifyNodePassword:
// role only ever becomes "owner" if it already was one going in (legacy
// rows), never freshly assigned by the developer-passcode flow anymore.
@Composable
fun OwnerHomeScreen(onLogout: () -> Unit, viewModel: AuthViewModel = hiltViewModel()) {
    Box(
        modifier = Modifier.fillMaxSize().background(CedalColors.Background).padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        CedalCard {
            CedalHeader("CEDAL NODE", "OWNER TERMINAL")
            Text(
                "Auth flow complete. Real content lands in a later milestone.",
                color = CedalColors.TextSecondary,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 16.dp),
            )
            CedalGhostButton(text = "LOG OUT", onClick = { viewModel.logout(); onLogout() })
        }
    }
}
