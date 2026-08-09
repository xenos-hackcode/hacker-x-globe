package com.xhacker.cedal.ui.screens.member

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
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
import com.xhacker.cedal.ui.screens.PermissionBlockedDialog
import com.xhacker.cedal.ui.screens.rememberPermissionGate
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.xhacker.cedal.data.FriendRequestItem
import com.xhacker.cedal.data.SearchUserResult
import com.xhacker.cedal.ui.FriendRequestSession
import com.xhacker.cedal.ui.theme.CedalColors
import com.xhacker.cedal.ui.theme.CedalTextField
import com.xhacker.cedal.viewmodel.AuthViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class TopTab { SEARCH, REQUESTS, QR, GROUPS }
private enum class RequestFilter { ALL, PENDING, ACCEPTED, DECLINED }

// Ported from cedal-mobile's search.tsx (SearchAndRequestsRoute) + its two
// tab bodies, Search.tsx and Requests.tsx. RN backed this with Firestore
// live listeners on a "friendRequests" collection; cedal-server has no
// live-listener transport yet, so this refetches on tab switch / action
// instead of subscribing to live updates — same end behavior, pull instead
// of push.
@Composable
fun MemberSearchBody(onBack: () -> Unit, onOpenGroup: (groupId: String) -> Unit = {}, viewModel: AuthViewModel = hiltViewModel()) {
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

    Column(modifier = Modifier.fillMaxSize().background(CedalColors.Background).padding(top = 32.dp, start = 16.dp, end = 16.dp).imePadding()) {
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

            // horizontalScroll so this never wraps/overflows onto a second
            // line regardless of screen width, now that there are 4 tabs
            // sharing the row with the BACK pill - "GR" (not "Groups")
            // keeps the common case actually fitting without scrolling.
            Row(
                modifier = Modifier
                    .padding(start = 12.dp)
                    .clip(RoundedCornerShape(50))
                    .border(1.dp, CedalColors.BorderSlate, RoundedCornerShape(50))
                    .horizontalScroll(rememberScrollState())
                    .padding(2.dp),
            ) {
                TopTabButton("Search", activeTab == TopTab.SEARCH) { activeTab = TopTab.SEARCH }
                TopTabButton("Requests", activeTab == TopTab.REQUESTS, showDot = hasIncomingPending) {
                    activeTab = TopTab.REQUESTS
                    reloadRequests()
                }
                TopTabButton("QR", activeTab == TopTab.QR) { activeTab = TopTab.QR }
                TopTabButton("GR", activeTab == TopTab.GROUPS) { activeTab = TopTab.GROUPS }
            }
        }

        when (activeTab) {
            TopTab.SEARCH -> SearchTabBody(viewModel = viewModel)
            TopTab.REQUESTS -> RequestsTabBody(requests = requests, viewModel = viewModel, onChanged = { reloadRequests() })
            TopTab.QR -> QrTabBody(viewModel = viewModel)
            TopTab.GROUPS -> GroupsTabBody(viewModel = viewModel, onOpenGroup = onOpenGroup)
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

// Group search - "change wer base is to wher group search would be" (the
// original ask): a dedicated tab here rather than folding into people
// search, since a group result needs a different action (Join, not
// add-friend). Only Groups.isPublic groups are ever returned - see
// GroupChatService.searchPublicGroups - and joining one found here is
// instant (no approval), same as opening a public group's link.
@Composable
private fun GroupsTabBody(viewModel: AuthViewModel, onOpenGroup: (groupId: String) -> Unit) {
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<com.xhacker.cedal.data.GroupSearchResultDto>>(emptyList()) }
    var joiningId by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }

    LaunchedEffect(query) {
        delay(300)
        loading = true
        viewModel.searchPublicGroups(query).onSuccess { results = it }
        loading = false
    }

    Column(modifier = Modifier.fillMaxSize()) {
        CedalTextField(value = query, onValueChange = { query = it }, prefix = "›", placeholder = "Search public groups", modifier = Modifier.padding(bottom = 12.dp))
        if (loading) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = CedalColors.AccentCyan)
        }
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(results, key = { it.id }) { g ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(g.name, color = CedalColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text("${g.memberCount} members", color = CedalColors.TextMuted, fontSize = 11.sp)
                    }
                    val joining = joiningId == g.id
                    Text(
                        if (joining) "JOINING…" else "JOIN",
                        color = if (joining) CedalColors.TextMuted else CedalColors.AccentCyan,
                        fontSize = 11.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .border(1.dp, if (joining) CedalColors.BorderSlate else CedalColors.AccentCyan, RoundedCornerShape(50))
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, enabled = !joining) {
                                joiningId = g.id
                                scope.launch {
                                    viewModel.requestToJoinGroup(g.id)
                                        .onSuccess { onOpenGroup(g.id) }
                                        .onFailure { joiningId = null }
                                }
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
            }
            if (results.isEmpty() && !loading) {
                item { Text(if (query.isBlank()) "Search for a public group by name." else "No public groups found.", color = CedalColors.TextMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 16.dp)) }
            }
        }
    }
}

@Composable
private fun SearchTabBody(viewModel: AuthViewModel) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var byGender by remember { mutableStateOf(false) }
    var byOccupation by remember { mutableStateOf(false) }
    var byHobby by remember { mutableStateOf(false) }
    var byAge by remember { mutableStateOf(false) }
    var byBio by remember { mutableStateOf(false) }
    var results by remember { mutableStateOf<List<SearchUserResult>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var sentIds by remember { mutableStateOf(setOf<String>()) }

    // "From your contacts" - matches the device's contacts against Cedal
    // accounts (see FriendService.matchContacts) instead of making someone
    // search by name/email for a person they already have saved. Re-checked
    // every time this tab is opened (not just once ever) - LaunchedEffect(Unit)
    // re-fires on each fresh composition of this screen, i.e. each time ✚ is
    // tapped, so a denied prompt gets asked again next time rather than never.
    var contactMatches by remember { mutableStateOf<List<SearchUserResult>>(emptyList()) }
    fun loadContactMatches() {
        scope.launch {
            val numbers = withContext(Dispatchers.IO) { readContactPhoneNumbers(context) }
            if (numbers.isNotEmpty()) {
                viewModel.matchContacts(numbers).onSuccess { contactMatches = it }
            }
        }
    }
    // Shows an explicit "go to Settings" dialog on a "Don't ask again"
    // denial instead of silently doing nothing forever - see
    // PermissionGate.kt.
    val permissionGate = rememberPermissionGate()
    LaunchedEffect(Unit) {
        permissionGate.request(context, Manifest.permission.READ_CONTACTS) { loadContactMatches() }
    }

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

        // Contact matches only apply to the default (blank-query) view - an
        // active text/filter search means the user is deliberately looking
        // for someone specific, so it just shows plain search results.
        val showContacts = query.isBlank()
        val contactIds = if (showContacts) contactMatches.map { it.id }.toSet() else emptySet()
        // Quick Add can double up with a contact match (a mutual friend who's
        // also in your contacts) - dedupe so they only ever appear once, in
        // the contacts section.
        val quickAddResults = if (showContacts) results.filter { it.id !in contactIds } else results

        if (!showContacts) {
            Text(
                "Search results",
                color = CedalColors.TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
            )
        }

        when {
            loading -> Box(modifier = Modifier.fillMaxWidth().padding(top = 24.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = CedalColors.AccentCyan, modifier = Modifier.size(20.dp))
            }
            !showContacts && results.isEmpty() -> Text(
                "No users found for that query.",
                color = CedalColors.TextMuted, fontSize = 12.sp, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
            )
            !showContacts -> LazyColumn(modifier = Modifier.fillMaxSize()) {
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
            contactMatches.isEmpty() && quickAddResults.isEmpty() -> Text(
                "No suggestions yet — add a friend or two and mutual connections will show up here, or type above to search directly.",
                color = CedalColors.TextMuted, fontSize = 12.sp, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
            )
            else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                if (contactMatches.isNotEmpty()) {
                    item {
                        Text(
                            "From your contacts",
                            color = CedalColors.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
                        )
                    }
                    items(contactMatches, key = { "contact_${it.id}" }) { user ->
                        val alreadySent = user.id in sentIds
                        UserRow(user = user, requestSent = alreadySent) {
                            scope.launch {
                                viewModel.sendFriendRequest(user.id).onSuccess { sentIds = sentIds + user.id }
                            }
                        }
                    }
                }
                if (quickAddResults.isNotEmpty()) {
                    item {
                        Text(
                            "Quick Add",
                            color = CedalColors.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
                        )
                    }
                    items(quickAddResults, key = { "quickadd_${it.id}" }) { user ->
                        val alreadySent = user.id in sentIds
                        UserRow(user = user, requestSent = alreadySent) {
                            scope.launch {
                                viewModel.sendFriendRequest(user.id).onSuccess { sentIds = sentIds + user.id }
                            }
                        }
                    }
                }
            }
        }
    }
    permissionGate.PermissionBlockedDialog()
}

// Reads every phone number off the device's contact list (dedup'd) -
// deliberately unfiltered/unnormalized here, since FriendService.matchContacts
// does its own last-10-digit normalization server-side to line these up
// against Users.phoneNumber's stored E.164 format.
private fun readContactPhoneNumbers(context: android.content.Context): List<String> {
    val numbers = mutableSetOf<String>()
    context.contentResolver.query(
        android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
        arrayOf(android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER),
        null, null, null,
    )?.use { cursor ->
        val col = cursor.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER)
        if (col >= 0) {
            while (cursor.moveToNext()) {
                cursor.getString(col)?.let { numbers.add(it) }
            }
        }
    }
    return numbers.toList()
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

private enum class QrSubTab { MY_CODE, SCAN }

// ✚ > QR - "My Code"/"Scan" both stay mounted under this same tab (like
// WhatsApp's own QR screen) rather than being one-off dialogs. See
// QrCode.kt for the bitmap generation and FriendService.findByPublicId for
// what a successful scan actually looks up server-side.
@Composable
private fun QrTabBody(viewModel: AuthViewModel) {
    var subTab by remember { mutableStateOf(QrSubTab.MY_CODE) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .padding(bottom = 12.dp)
                .clip(RoundedCornerShape(50))
                .border(1.dp, CedalColors.BorderSlate, RoundedCornerShape(50))
                .padding(2.dp),
        ) {
            TopTabButton("My Code", subTab == QrSubTab.MY_CODE) { subTab = QrSubTab.MY_CODE }
            TopTabButton("Scan", subTab == QrSubTab.SCAN) { subTab = QrSubTab.SCAN }
        }

        if (subTab == QrSubTab.MY_CODE) MyCodeBody(viewModel = viewModel) else ScanCodeBody(viewModel = viewModel)
    }
}

@Composable
private fun MyCodeBody(viewModel: AuthViewModel) {
    var publicId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { viewModel.getProfile().onSuccess { publicId = it.publicId } }

    Column(modifier = Modifier.fillMaxWidth().padding(top = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        val id = publicId
        if (id == null) {
            CircularProgressIndicator(color = CedalColors.AccentCyan, modifier = Modifier.size(20.dp))
        } else {
            val bitmap = remember(id) { generateCedalQrBitmap(id, 720) }
            Box(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(CedalColors.CardBackground)
                    .border(1.dp, CedalColors.BorderCyan, RoundedCornerShape(20.dp))
                    .padding(20.dp),
            ) {
                Image(bitmap = bitmap.asImageBitmap(), contentDescription = "Your Cedal QR code", modifier = Modifier.fillMaxWidth().aspectRatio(1f))
            }
            Text(
                "Anyone who scans this is sent straight to a friend request with you.",
                color = CedalColors.TextSecondary, fontSize = 11.sp, textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 16.dp, start = 24.dp, end = 24.dp),
            )
            Text("YOUR CODE: $id", color = CedalColors.TextMuted, fontSize = 11.sp, letterSpacing = 1.sp, modifier = Modifier.padding(top = 6.dp))
        }
    }
}

@Composable
private fun ScanCodeBody(viewModel: AuthViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var hasCameraPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    // Shows an explicit "go to Settings" dialog on a "Don't ask again"
    // denial instead of the GRANT CAMERA ACCESS button just silently doing
    // nothing forever - see PermissionGate.kt.
    val permissionGate = rememberPermissionGate()

    var found by remember { mutableStateOf<SearchUserResult?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var requestSent by remember { mutableStateOf(false) }
    // Bumping this remounts QrScannerView with a fresh remember scope - the
    // simplest way to "resume scanning" after a hit/miss without hand-
    // rolling a reset hook through CameraX's analyzer callback.
    var scanGeneration by remember { mutableStateOf(0) }

    fun handleScan(raw: String) {
        if (!raw.startsWith(CEDAL_ADD_URI_PREFIX)) {
            error = "That's not a Cedal QR code."
            return
        }
        val code = raw.removePrefix(CEDAL_ADD_URI_PREFIX)
        scope.launch {
            viewModel.findUserByPublicId(code)
                .onSuccess { found = it }
                .onFailure { error = "No user found for that code." }
        }
    }

    Column(modifier = Modifier.fillMaxWidth().padding(top = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        when {
            !hasCameraPermission -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Camera access is needed to scan a friend's QR code.",
                    color = CedalColors.TextSecondary, fontSize = 12.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .border(1.dp, CedalColors.BorderCyan, RoundedCornerShape(50))
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                            permissionGate.request(context, Manifest.permission.CAMERA) { hasCameraPermission = true }
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Text("GRANT CAMERA ACCESS", color = CedalColors.AccentCyan, fontSize = 12.sp, letterSpacing = 0.6.sp)
                }
            }
            found != null -> {
                val user = found!!
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 24.dp)) {
                    UserRow(user = user, requestSent = requestSent) {
                        scope.launch { viewModel.sendFriendRequest(user.id).onSuccess { requestSent = true } }
                    }
                    Box(
                        modifier = Modifier
                            .padding(top = 12.dp)
                            .clip(RoundedCornerShape(50))
                            .border(1.dp, CedalColors.BorderSlate, RoundedCornerShape(50))
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                found = null; requestSent = false; error = null; scanGeneration++
                            }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    ) {
                        Text("SCAN AGAIN", color = CedalColors.TextPrimary, fontSize = 12.sp, letterSpacing = 0.6.sp)
                    }
                }
            }
            else -> {
                key(scanGeneration) {
                    QrScannerView(onScanned = ::handleScan)
                }
                error?.let {
                    Text(it, color = CedalColors.Error, fontSize = 12.sp, modifier = Modifier.padding(top = 12.dp))
                    Box(
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .clip(RoundedCornerShape(50))
                            .border(1.dp, CedalColors.BorderSlate, RoundedCornerShape(50))
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                error = null; scanGeneration++
                            }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    ) {
                        Text("TRY AGAIN", color = CedalColors.TextPrimary, fontSize = 12.sp, letterSpacing = 0.6.sp)
                    }
                }
            }
        }
    }
    permissionGate.PermissionBlockedDialog()
}

// Live camera preview + ML Kit QR decode - unbinds itself from CameraX the
// moment it leaves composition (see onDispose below), so switching back to
// "My Code" or navigating away actually releases the camera instead of
// leaking it. onScanned fires at most once per mount (see hasScanned) -
// the caller remounts this (see scanGeneration in ScanCodeBody) to scan again.
@Composable
private fun QrScannerView(onScanned: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }
    var hasScanned by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val scanner = BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build(),
        )
        val analysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
        analysis.setAnalyzer(ContextCompat.getMainExecutor(context)) { imageProxy ->
            val mediaImage = imageProxy.image
            if (mediaImage == null || hasScanned) {
                imageProxy.close()
            } else {
                val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                scanner.process(image)
                    .addOnSuccessListener { barcodes ->
                        val value = barcodes.firstOrNull()?.rawValue
                        if (value != null && !hasScanned) {
                            hasScanned = true
                            onScanned(value)
                        }
                    }
                    .addOnCompleteListener { imageProxy.close() }
            }
        }

        var provider: ProcessCameraProvider? = null
        cameraProviderFuture.addListener({
            provider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
            try {
                provider?.unbindAll()
                provider?.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
            } catch (_: Exception) {
            }
        }, ContextCompat.getMainExecutor(context))

        onDispose {
            provider?.unbindAll()
            scanner.close()
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .aspectRatio(1f)
            .clip(RoundedCornerShape(20.dp)),
    )
}
