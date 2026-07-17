package com.xhacker.cedal.ui.screens.member

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xhacker.cedal.data.FriendRequestItem
import com.xhacker.cedal.data.SearchUserResult
import com.xhacker.cedal.ui.FriendRequestSession
import com.xhacker.cedal.ui.theme.CedalColors
import com.xhacker.cedal.ui.theme.CedalTextField
import com.xhacker.cedal.viewmodel.AuthViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class TopTab { SEARCH, REQUESTS }
private enum class RequestFilter { ALL, PENDING, ACCEPTED, DECLINED }

// Ported from cedal-mobile's search.tsx (SearchAndRequestsRoute) + its two
// tab bodies, Search.tsx and Requests.tsx. RN backed this with Firestore
// live listeners on a "friendRequests" collection; cedal-server has no
// live-listener transport yet, so this refetches on tab switch / action
// instead of subscribing to live updates — same end behavior, pull instead
// of push.
@Composable
fun MemberSearchBody(onBack: () -> Unit, viewModel: AuthViewModel = hiltViewModel()) {
    val scope = rememberCoroutineScope()
    var activeTab by remember { mutableStateOf(TopTab.SEARCH) }
    var requests by remember { mutableStateOf<List<FriendRequestItem>>(emptyList()) }

    fun reloadRequests() {
        scope.launch {
            viewModel.listFriendRequests().onSuccess { result ->
                requests = result
                // Opening this screen at all counts as "seen" for the green
                // accepted-request indicator on the ✚ button - same idea as
                // opening a notification tray clearing its badge.
                FriendRequestSession.markAcceptedSeen(
                    result.filter { it.direction == "outgoing" && it.status == "accepted" }.map { it.id },
                )
            }
        }
    }

    LaunchedEffect(Unit) { reloadRequests() }

    val hasIncomingPending = requests.any { it.direction == "incoming" && it.status == "pending" }

    Column(modifier = Modifier.fillMaxSize().background(CedalColors.Background).padding(top = 32.dp, start = 16.dp, end = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .border(1.dp, CedalColors.BorderSlate, RoundedCornerShape(50))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onBack)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Text("BACK", color = CedalColors.TextPrimary, fontSize = 13.sp, letterSpacing = 1.5.sp)
            }

            Row(
                modifier = Modifier
                    .padding(start = 12.dp)
                    .clip(RoundedCornerShape(50))
                    .border(1.dp, CedalColors.BorderSlate, RoundedCornerShape(50))
                    .padding(2.dp),
            ) {
                TopTabButton("Search", activeTab == TopTab.SEARCH) { activeTab = TopTab.SEARCH }
                TopTabButton("Requests", activeTab == TopTab.REQUESTS, showDot = hasIncomingPending) {
                    activeTab = TopTab.REQUESTS
                    reloadRequests()
                }
            }
        }

        if (activeTab == TopTab.SEARCH) {
            SearchTabBody(viewModel = viewModel)
        } else {
            RequestsTabBody(requests = requests, viewModel = viewModel, onChanged = { reloadRequests() })
        }
    }
}

@Composable
private fun TopTabButton(label: String, active: Boolean, showDot: Boolean = false, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (active) CedalColors.CardBackground else CedalColors.Background)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                label.uppercase(),
                color = if (active) CedalColors.TextPrimary else CedalColors.TextSecondary,
                fontSize = 12.sp,
                letterSpacing = 0.8.sp,
            )
            if (showDot) {
                Box(
                    modifier = Modifier
                        .padding(start = 6.dp)
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(CedalColors.Error),
                )
            }
        }
    }
}

@Composable
private fun FilterChip(label: String, active: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .padding(end = 8.dp)
            .clip(RoundedCornerShape(50))
            .background(if (active) CedalColors.CardBackground else CedalColors.Background)
            .border(1.dp, if (active) CedalColors.Success else CedalColors.BorderCyan, RoundedCornerShape(50))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(label.uppercase(), color = CedalColors.TextPrimary, fontSize = 11.sp, letterSpacing = 0.4.sp)
    }
}

@Composable
private fun SearchTabBody(viewModel: AuthViewModel) {
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var byGender by remember { mutableStateOf(false) }
    var byOccupation by remember { mutableStateOf(false) }
    var byHobby by remember { mutableStateOf(false) }
    var byAge by remember { mutableStateOf(false) }
    var byBio by remember { mutableStateOf(false) }
    var results by remember { mutableStateOf<List<SearchUserResult>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var sentIds by remember { mutableStateOf(setOf<String>()) }

    // A blank query with no filters is a real "Quick Add" list now (mutual
    // friends, see FriendService.quickAddSuggestions server-side) - not the
    // old "every account on the server" dump.
    LaunchedEffect(query, byGender, byOccupation, byHobby, byAge, byBio) {
        loading = true
        delay(250) // small debounce so we don't hit the server on every keystroke
        viewModel.searchUsers(query, byGender, byOccupation, byHobby, byAge, byBio).onSuccess { results = it }
        loading = false
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text("Search", color = CedalColors.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        CedalTextField(
            value = query,
            onValueChange = { query = it },
            prefix = "›",
            placeholder = "Search by email, name or bio",
            modifier = Modifier.padding(top = 4.dp),
        )

        Text(
            "FILTER BY",
            color = CedalColors.TextSecondary,
            fontSize = 11.sp,
            letterSpacing = 0.4.sp,
            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
        )
        Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
            FilterChip("Gender", byGender) { byGender = !byGender }
            FilterChip("Occupation", byOccupation) { byOccupation = !byOccupation }
            FilterChip("Hobby", byHobby) { byHobby = !byHobby }
            FilterChip("Age", byAge) { byAge = !byAge }
            FilterChip("Bio", byBio) { byBio = !byBio }
        }

        Text(
            if (query.isNotBlank()) "Search results" else "Quick Add",
            color = CedalColors.TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
        )

        when {
            loading -> Box(modifier = Modifier.fillMaxWidth().padding(top = 24.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = CedalColors.AccentCyan, modifier = Modifier.size(20.dp))
            }
            results.isEmpty() && query.isBlank() -> Text(
                "No suggestions yet — add a friend or two and mutual connections will show up here, or type above to search directly.",
                color = CedalColors.TextMuted, fontSize = 12.sp, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
            )
            results.isEmpty() -> Text(
                "No users found for that query.",
                color = CedalColors.TextMuted, fontSize = 12.sp, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
            )
            else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(results, key = { it.id }) { user ->
                    val alreadySent = user.id in sentIds
                    UserRow(user = user, requestSent = alreadySent) {
                        scope.launch {
                            viewModel.sendFriendRequest(user.id).onSuccess {
                                sentIds = sentIds + user.id
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UserRow(user: SearchUserResult, requestSent: Boolean, onAdd: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(CedalColors.BackgroundBlob)
                .border(1.dp, CedalColors.BorderCyan, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(user.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?", color = CedalColors.AccentCyan, fontSize = 13.sp)
        }
        Column(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
            Text(user.name, color = CedalColors.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            user.email?.let { Text(it, color = CedalColors.TextSecondary, fontSize = 11.sp) }
            user.occupation?.let { Text(it, color = CedalColors.TextMuted, fontSize = 11.sp) }
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(if (requestSent) CedalColors.Background else CedalColors.CardBackground)
                .border(1.dp, if (requestSent) CedalColors.BorderSlate else CedalColors.Success, RoundedCornerShape(50))
                .clickable(
                    enabled = !requestSent,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onAdd,
                )
                .padding(horizontal = 12.dp, vertical = 4.dp),
        ) {
            Text(
                if (requestSent) "SENT" else "ADD",
                color = if (requestSent) CedalColors.TextMuted else CedalColors.Success,
                fontSize = 11.sp, letterSpacing = 0.4.sp,
            )
        }
    }
}

@Composable
private fun RequestsTabBody(requests: List<FriendRequestItem>, viewModel: AuthViewModel, onChanged: () -> Unit) {
    val scope = rememberCoroutineScope()
    var filter by remember { mutableStateOf(RequestFilter.ALL) }
    val filtered = requests.filter {
        when (filter) {
            RequestFilter.ALL -> true
            RequestFilter.PENDING -> it.status == "pending"
            RequestFilter.ACCEPTED -> it.status == "accepted"
            RequestFilter.DECLINED -> it.status == "declined"
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            "FILTER BY",
            color = CedalColors.TextSecondary, fontSize = 11.sp, letterSpacing = 0.4.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 4.dp),
        )
        Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
            FilterChip("All", filter == RequestFilter.ALL) { filter = RequestFilter.ALL }
            FilterChip("Pending", filter == RequestFilter.PENDING) { filter = RequestFilter.PENDING }
            FilterChip("Accepted", filter == RequestFilter.ACCEPTED) { filter = RequestFilter.ACCEPTED }
            FilterChip("Declined", filter == RequestFilter.DECLINED) { filter = RequestFilter.DECLINED }
        }

        if (filtered.isEmpty()) {
            Text(
                "No requests here yet.",
                color = CedalColors.TextMuted, fontSize = 12.sp, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(top = 8.dp)) {
                items(filtered, key = { it.id }) { req ->
                    RequestRow(
                        req = req,
                        onAccept = { scope.launch { viewModel.acceptFriendRequest(req.id).onSuccess { onChanged() } } },
                        onDecline = { scope.launch { viewModel.declineFriendRequest(req.id).onSuccess { onChanged() } } },
                        onCancel = { scope.launch { viewModel.cancelFriendRequest(req.id).onSuccess { onChanged() } } },
                    )
                }
            }
        }
    }
}

@Composable
private fun RequestRow(req: FriendRequestItem, onAccept: () -> Unit, onDecline: () -> Unit, onCancel: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(CedalColors.BackgroundBlob)
                .border(1.dp, CedalColors.BorderCyan, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(req.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?", color = CedalColors.AccentCyan, fontSize = 13.sp)
        }
        Column(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
            Text(req.name, color = CedalColors.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            req.email?.let { Text(it, color = CedalColors.TextSecondary, fontSize = 11.sp) }
            Text(
                if (req.direction == "outgoing") "You sent" else "To you",
                color = CedalColors.TextMuted, fontSize = 11.sp,
            )
        }

        when (req.status) {
            "pending" -> Column(horizontalAlignment = Alignment.End) {
                if (req.direction == "incoming") {
                    RequestActionPill("Accept", CedalColors.Success, onAccept)
                }
                Box(modifier = Modifier.padding(top = if (req.direction == "incoming") 4.dp else 0.dp)) {
                    RequestActionPill(
                        if (req.direction == "incoming") "Decline" else "Cancel",
                        CedalColors.Error,
                        if (req.direction == "incoming") onDecline else onCancel,
                    )
                }
            }
            "accepted" -> Column(horizontalAlignment = Alignment.Start) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(CedalColors.CardBackground)
                        .border(1.dp, CedalColors.Success, RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text("ACCEPTED", color = CedalColors.Success, fontSize = 11.sp, letterSpacing = 0.4.sp)
                }
                Text("Friends now", color = CedalColors.TextSecondary, fontSize = 9.sp, modifier = Modifier.padding(top = 2.dp))
            }
            else -> Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(CedalColors.CardBackground)
                    .border(1.dp, CedalColors.Error, RoundedCornerShape(50))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text("DECLINED", color = CedalColors.Error, fontSize = 11.sp, letterSpacing = 0.4.sp)
            }
        }
    }
}

@Composable
private fun RequestActionPill(label: String, accent: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(CedalColors.CardBackground)
            .border(1.dp, accent, RoundedCornerShape(50))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
        Text(label.uppercase(), color = accent, fontSize = 10.sp, letterSpacing = 0.4.sp)
    }
}
