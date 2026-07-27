package com.xhacker.cedal.ui.screens.member

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.xhacker.cedal.data.UserProfile
import com.xhacker.cedal.ui.theme.CedalColors
import com.xhacker.cedal.ui.theme.CedalErrorText
import com.xhacker.cedal.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

// Read-only counterpart to MemberProfileBody (which is always the signed-in
// user's own, editable profile) - reached by tapping a friend's name in the
// chat header. Shows only the public-facing fields, not account internals
// like email/2FA that MemberProfileBody's own-profile view exposes.
@Composable
fun MemberFriendProfileBody(userId: String, onBack: () -> Unit, viewModel: AuthViewModel = hiltViewModel()) {
    var profile by remember { mutableStateOf<UserProfile?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var deleteConfirming by remember { mutableStateOf(false) }
    var deleted by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(userId) {
        viewModel.getProfileFor(userId).onSuccess { profile = it }.onFailure { error = it.message }
    }

    Column(modifier = Modifier.fillMaxSize().background(CedalColors.Background).padding(16.dp)) {
        MemberBackBar(title = "Profile", onBack = onBack)
        CedalErrorText(error)

        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            val p = profile
            if (p == null) {
                Text("Loading…", color = CedalColors.TextMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 24.dp))
                return@Column
            }
            val displayName = p.nickname?.takeIf { it.isNotBlank() } ?: p.email?.substringBefore("@") ?: "Cedal user"
            val initial = displayName.firstOrNull()?.uppercaseChar()?.toString() ?: "?"

            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(CedalColors.BackgroundBlob)
                        .border(2.dp, CedalColors.BorderCyan, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    val avatarUrl = p.avatarUrl
                    if (avatarUrl != null) {
                        AsyncImage(
                            model = avatarUrl,
                            contentDescription = "Avatar",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                        )
                    } else {
                        Text(initial, color = CedalColors.TextPrimary, fontSize = 36.sp)
                    }
                }
                Text(displayName, color = CedalColors.TextPrimary, fontSize = 18.sp, modifier = Modifier.padding(top = 8.dp))
                p.handle?.takeIf { it.isNotBlank() }?.let {
                    Text("@$it", color = CedalColors.TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
                }
            }

            ProfileFieldRow("Occupation", p.occupation)
            ProfileFieldRow("Hobby", p.hobby)
            ProfileFieldRow("Bio", p.bio)

            if (deleted) {
                Text("User removed.", color = CedalColors.TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 12.dp))
            } else {
                // Unlike Block, this leaves both sides fully searchable/
                // re-addable afterward - see FriendService.deleteUser's
                // own doc comment for the exact contrast.
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    if (deleteConfirming) {
                        Text(
                            "Remove this connection? You can still find and re-add each other later - this isn't Block.",
                            color = CedalColors.TextSecondary, fontSize = 11.sp, modifier = Modifier.padding(bottom = 8.dp),
                        )
                        Row {
                            Text(
                                "CONFIRM", color = CedalColors.Error, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .border(1.dp, CedalColors.Error, RoundedCornerShape(50))
                                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                        scope.launch { viewModel.deleteUser(userId).onSuccess { deleted = true }.onFailure { error = it.message } }
                                    }
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                            )
                            Text(
                                "CANCEL", color = CedalColors.TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .padding(start = 10.dp)
                                    .clip(RoundedCornerShape(50))
                                    .border(1.dp, CedalColors.BorderSlate, RoundedCornerShape(50))
                                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { deleteConfirming = false }
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                            )
                        }
                    } else {
                        Text(
                            "DELETE USER", color = CedalColors.Error, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .border(1.dp, CedalColors.Error, RoundedCornerShape(50))
                                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { deleteConfirming = true }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileFieldRow(label: String, value: String?) {
    if (value.isNullOrBlank()) return
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)) {
        Text(label.uppercase(), color = CedalColors.TextSecondary, fontSize = 10.sp, letterSpacing = 1.sp)
        Text(value, color = CedalColors.TextPrimary, fontSize = 14.sp, modifier = Modifier.padding(top = 2.dp))
    }
}
