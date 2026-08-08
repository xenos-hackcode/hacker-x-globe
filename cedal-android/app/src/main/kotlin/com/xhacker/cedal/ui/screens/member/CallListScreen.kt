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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.xhacker.cedal.data.FriendSummary
import com.xhacker.cedal.ui.theme.CedalColors
import com.xhacker.cedal.viewmodel.AuthViewModel

// "Known" calling's home tab (replaces the old empty Base tab - see
// MemberScaffold.kt's MemberTab enum). Deliberately scoped to DM contacts
// only, same population as viewModel.listFriends() (the Bank "Send" picker
// reuses the same call) - "only people in dm are the people u can call" was
// the explicit ask, so this never falls back to a global user search.
@Composable
fun CallListBody(onOpenProfile: (String) -> Unit, viewModel: AuthViewModel = hiltViewModel()) {
    var friends by remember { mutableStateOf<List<FriendSummary>>(emptyList()) }
    var query by remember { mutableStateOf("") }
    val context = LocalContext.current

    LaunchedEffect(Unit) { viewModel.listFriends().onSuccess { friends = it } }

    val filtered = friends.filter { it.name.contains(query, ignoreCase = true) }

    Column(modifier = Modifier.fillMaxSize().background(CedalColors.Background)) {
        Text(
            "Call", color = CedalColors.TextPrimary, fontSize = 18.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(CedalColors.CardBackground)
                .border(1.dp, CedalColors.BorderSlate, RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            BasicTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(color = CedalColors.TextPrimary, fontSize = 13.sp),
                cursorBrush = SolidColor(CedalColors.AccentCyan),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    if (query.isEmpty()) {
                        Text("Search people to call", color = CedalColors.TextSecondary, fontSize = 13.sp)
                    }
                    inner()
                },
            )
        }

        if (friends.isEmpty()) {
            Text(
                "Nothing here yet. Only people you have a DM with show up here.",
                color = CedalColors.TextMuted, fontSize = 12.sp, modifier = Modifier.padding(20.dp),
            )
        } else if (filtered.isEmpty()) {
            Text("No match.", color = CedalColors.TextMuted, fontSize = 12.sp, modifier = Modifier.padding(20.dp))
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(top = 8.dp)) {
                items(filtered, key = { it.id }) { friend ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onOpenProfile(friend.id) }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                    ) {
                        Box(
                            modifier = Modifier.size(44.dp).clip(CircleShape).background(CedalColors.BackgroundBlob).border(1.dp, CedalColors.BorderCyan, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            val avatarUrl = friend.avatarUrl
                            if (avatarUrl != null) {
                                AsyncImage(model = avatarUrl, contentDescription = "Avatar", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(CircleShape))
                            } else {
                                Text(friend.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?", color = CedalColors.TextPrimary, fontSize = 16.sp)
                            }
                        }
                        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                            Text(friend.name, color = CedalColors.TextPrimary, fontSize = 14.sp)
                            Text(
                                if (friend.canCall) "Known call available" else "Hasn't shared their number with you",
                                color = CedalColors.TextMuted, fontSize = 11.sp,
                            )
                        }
                        val phoneNumber = friend.phoneNumber
                        if (friend.canCall && phoneNumber != null) {
                            IconButton(onClick = { launchDialer(context, phoneNumber) }) {
                                Icon(Icons.Outlined.Call, contentDescription = "Call ${friend.name}", tint = CedalColors.AccentCyan)
                            }
                        }
                    }
                }
            }
        }
    }
}
