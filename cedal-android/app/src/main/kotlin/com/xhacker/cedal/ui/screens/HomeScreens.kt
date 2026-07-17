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

// Placeholder role-home screens for developer/owner — real content is a
// later milestone. The member home is now the real MemberRoute/ChatsListBody
// shell (see ui/screens/member/), since the member area is what we're
// actively building out now.

@Composable
fun DeveloperHomeScreen(onLogout: () -> Unit) = RoleHomePlaceholder("CEDAL NODE", "DEVELOPER TERMINAL", onLogout)

@Composable
fun OwnerHomeScreen(onLogout: () -> Unit) = RoleHomePlaceholder("CEDAL NODE", "OWNER TERMINAL", onLogout)

@Composable
private fun RoleHomePlaceholder(
    title: String,
    subtitle: String,
    onLogout: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    Box(
        modifier = Modifier.fillMaxSize().background(CedalColors.Background).padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        CedalCard {
            CedalHeader(title, subtitle)
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
