package com.xhacker.cedal.ui.screens.member

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.TrendingDown
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xhacker.cedal.data.FriendSummary
import com.xhacker.cedal.data.NotificationItem
import com.xhacker.cedal.data.WalletTransactionItem
import com.xhacker.cedal.ui.theme.CedalColors
import com.xhacker.cedal.ui.theme.CedalErrorText
import com.xhacker.cedal.ui.theme.CedalPrimaryButton
import com.xhacker.cedal.ui.theme.CedalTextField
import com.xhacker.cedal.util.vibrate
import com.xhacker.cedal.viewmodel.AuthViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class BankTab { HOME, TASKS, SEND, TRANSACTIONS }

// Mirrors WalletService.MIN_DEBT_SC on the server — display-only here (the
// server is the source of truth and enforces it; this is just the hint text).
private const val MIN_DEBT_SC = -30

// Matches cedal-mobile's SendScreen LARGE_SPEND_THRESHOLD exactly.
private const val LARGE_SPEND_THRESHOLD_SC = 1000
private val QUICK_AMOUNTS = listOf(5, 10, 25, 50)
private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.UK)

// Ported from cedal-mobile's BankRouter.tsx + its screens (Banking/Send/
// Transactions/Debt/Trade/Notifications/Tasks) — the full Bank feature.
// "Buy SC" stays a disabled stub regardless — see REMINDER.md, real
// top-ups need payments licensing this project doesn't have yet.
@Composable
fun BankRouterBody(
    onBack: () -> Unit,
    onOpenDebt: () -> Unit,
    onOpenTrade: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    var activeTab by remember { mutableStateOf(BankTab.HOME) }
    var balance by remember { mutableStateOf<Int?>(null) }
    val scope = rememberCoroutineScope()

    fun reloadBalance() {
        scope.launch { viewModel.getWalletBalance().onSuccess { balance = it } }
    }
    LaunchedEffect(Unit) { reloadBalance() }

    Column(modifier = Modifier.fillMaxSize().background(CedalColors.Background)) {
        Box(modifier = Modifier.weight(1f)) {
            when (activeTab) {
                BankTab.HOME -> BankHomeBody(
                    balance = balance,
                    onBack = onBack,
                    onOpenSend = { activeTab = BankTab.SEND },
                    onOpenTransactions = { activeTab = BankTab.TRANSACTIONS },
                    onOpenDebt = onOpenDebt,
                    onOpenTrade = onOpenTrade,
                    onOpenNotifications = onOpenNotifications,
                    onOpenSettings = onOpenSettings,
                    viewModel = viewModel,
                )
                BankTab.TASKS -> BankTasksBody()
                BankTab.SEND -> BankSendBody(
                    balance = balance,
                    viewModel = viewModel,
                    onSent = { reloadBalance(); activeTab = BankTab.HOME },
                )
                BankTab.TRANSACTIONS -> BankTransactionsBody(viewModel = viewModel)
            }
        }
        BankBottomBar(activeTab) { activeTab = it }
    }
}

@Composable
private fun BankBottomBar(active: BankTab, onNavigateTab: (BankTab) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .background(CedalColors.CardBackground)
            .border(width = Dp.Hairline, color = CedalColors.BorderSlate),
    ) {
        BankTabItem("Home", Icons.Outlined.AccountBalance, active == BankTab.HOME) { onNavigateTab(BankTab.HOME) }
        BankTabItem("Tasks", Icons.Outlined.Checklist, active == BankTab.TASKS) { onNavigateTab(BankTab.TASKS) }
        BankTabItem("Send", Icons.Outlined.Send, active == BankTab.SEND) { onNavigateTab(BankTab.SEND) }
        BankTabItem("Transactions", Icons.Outlined.History, active == BankTab.TRANSACTIONS) { onNavigateTab(BankTab.TRANSACTIONS) }
    }
}

@Composable
private fun BankTabItem(label: String, icon: ImageVector, active: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick),
    ) {
        Icon(icon, contentDescription = label, tint = if (active) CedalColors.Success else CedalColors.TextMuted, modifier = Modifier.size(20.dp))
        Text(
            label,
            color = if (active) CedalColors.Success else CedalColors.TextMuted,
            fontSize = 11.sp,
            modifier = Modifier.padding(top = 3.dp),
        )
    }
}

@Composable
private fun BankHomeBody(
    balance: Int?,
    onBack: () -> Unit,
    onOpenSend: () -> Unit,
    onOpenTransactions: () -> Unit,
    onOpenDebt: () -> Unit,
    onOpenTrade: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: AuthViewModel,
) {
    var recent by remember { mutableStateOf<List<WalletTransactionItem>?>(null) }
    var hasNotifications by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        viewModel.listWalletTransactions().onSuccess { recent = it.take(3) }
        // Matches cedal-mobile's own check exactly — "are there any
        // notifications at all", not a per-user unread count.
        viewModel.listNotifications().onSuccess { hasNotifications = it.isNotEmpty() }
    }

    val inDebt = (balance ?: 0) < 0
    val showAlertsDot = hasNotifications && !viewModel.storage.muteBankNotifications

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.weight(1f)) {
                MemberBackBar(title = "Banking Hub", onBack = onBack)
            }
            Icon(
                Icons.Outlined.Settings,
                contentDescription = "Bank settings",
                tint = CedalColors.TextSecondary,
                modifier = Modifier
                    .padding(bottom = 12.dp)
                    .size(20.dp)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onOpenSettings),
            )
        }
        Text("Star Coins balance and transactions.", color = CedalColors.TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(bottom = 16.dp))

        // Hero balance card
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(CedalColors.CardBackground)
                .border(1.5.dp, if (inDebt) CedalColors.Error.copy(alpha = 0.7f) else CedalColors.BorderCyan, RoundedCornerShape(20.dp))
                .padding(vertical = 22.dp, horizontal = 20.dp),
        ) {
            Text("STAR COINS BALANCE", color = CedalColors.TextSecondary, fontSize = 11.sp, letterSpacing = 1.5.sp)

            Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 10.dp)) {
                Text(
                    if (balance != null) "$balance" else "…",
                    color = if (inDebt) CedalColors.Error else CedalColors.TextPrimary,
                    fontSize = 42.sp, fontWeight = FontWeight.ExtraBold,
                )
                Text(
                    " SC", color = CedalColors.TextSecondary, fontSize = 16.sp,
                    modifier = Modifier.padding(bottom = 6.dp, start = 2.dp),
                )
            }

            if (inDebt) {
                Box(
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .clip(RoundedCornerShape(50))
                        .background(CedalColors.Error.copy(alpha = 0.12f))
                        .border(1.dp, CedalColors.Error.copy(alpha = 0.6f), RoundedCornerShape(50))
                        .padding(horizontal = 10.dp, vertical = 3.dp),
                ) {
                    Text("IN DEBT — LIMIT $MIN_DEBT_SC SC", color = CedalColors.Error, fontSize = 9.sp, letterSpacing = 0.5.sp)
                }
            }

            Text("In-game currency only", color = CedalColors.TextMuted, fontSize = 11.sp, modifier = Modifier.padding(top = 10.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(top = 14.dp)
                    .clip(RoundedCornerShape(50))
                    .background(CedalColors.Background)
                    .border(1.dp, CedalColors.BorderSlate, RoundedCornerShape(50))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Icon(Icons.Outlined.Lock, contentDescription = null, tint = CedalColors.TextMuted, modifier = Modifier.size(13.dp))
                Text(
                    "Buy 50 SC (£1) — coming soon",
                    color = CedalColors.TextMuted, fontSize = 11.sp, letterSpacing = 0.3.sp,
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
        }

        // All 4 are real now — each gets its own accent color.
        Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BankActionTile("Send", Icons.Outlined.Send, accent = CedalColors.Success, modifier = Modifier.weight(1f), onClick = onOpenSend)
            BankActionTile("Debt", Icons.Outlined.TrendingDown, accent = CedalColors.Error, modifier = Modifier.weight(1f), onClick = onOpenDebt)
            BankActionTile("Trade", Icons.Outlined.SwapHoriz, accent = CedalColors.AccentIndigo, modifier = Modifier.weight(1f), onClick = onOpenTrade)
            BankActionTile(
                "Alerts", Icons.Outlined.Notifications, accent = CedalColors.AccentSky, showDot = showAlertsDot,
                modifier = Modifier.weight(1f), onClick = onOpenNotifications,
            )
        }

        // Recent transactions
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(CedalColors.CardBackground)
                .border(1.dp, CedalColors.BorderSlate, RoundedCornerShape(14.dp))
                .padding(16.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("RECENT TRANSACTIONS", color = CedalColors.TextSecondary, fontSize = 11.sp, letterSpacing = 1.sp)
                Text(
                    "See all",
                    color = CedalColors.AccentSky, fontSize = 12.sp,
                    modifier = Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onOpenTransactions),
                )
            }
            when {
                recent == null -> Box(Modifier.fillMaxWidth().padding(top = 16.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = CedalColors.AccentCyan, modifier = Modifier.size(18.dp))
                }
                recent!!.isEmpty() -> Text(
                    "When you send, receive, or buy Star Coins, transactions will show here.",
                    color = CedalColors.TextMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp),
                )
                else -> Column(modifier = Modifier.padding(top = 6.dp)) {
                    recent!!.forEachIndexed { index, tx ->
                        TransactionRow(tx)
                        if (index != recent!!.lastIndex) {
                            Box(modifier = Modifier.fillMaxWidth().padding(start = 44.dp).border(width = Dp.Hairline, color = CedalColors.BorderSlate))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BankActionTile(
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    accent: Color = CedalColors.AccentSky,
    showDot: Boolean = false,
    onClick: () -> Unit,
) {
    Box(modifier = modifier) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(accent.copy(alpha = 0.08f))
                .border(1.dp, accent.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
                .padding(vertical = 14.dp),
        ) {
            Icon(icon, contentDescription = label, tint = accent, modifier = Modifier.size(20.dp))
            Text(label, color = CedalColors.TextPrimary, fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp))
        }
        if (showDot) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 6.dp, end = 6.dp)
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(CedalColors.Error),
            )
        }
    }
}

@Composable
private fun TransactionRow(tx: WalletTransactionItem) {
    val isCredit = tx.type != "credit_out"
    val desc = when (tx.type) {
        "topup" -> "Top-up"
        "credit_out" -> "To ${tx.peerName ?: "Friend"}"
        "credit_in" -> "From ${tx.peerName ?: "Friend"}"
        else -> "Wallet activity"
    }
    val time = remember(tx.createdAt) { timeFormat.format(Date(tx.createdAt)) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(if (isCredit) CedalColors.Success.copy(alpha = 0.12f) else CedalColors.TextMuted.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (isCredit) Icons.Outlined.ArrowDownward else Icons.Outlined.ArrowUpward,
                contentDescription = null,
                tint = if (isCredit) CedalColors.Success else CedalColors.TextSecondary,
                modifier = Modifier.size(15.dp),
            )
        }
        Column(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
            Text(desc, color = CedalColors.TextPrimary, fontSize = 13.sp)
            Text(time, color = CedalColors.TextMuted, fontSize = 10.sp, modifier = Modifier.padding(top = 1.dp))
        }
        Text(
            "${if (isCredit) "+" else "-"}${tx.amount} SC",
            color = if (isCredit) CedalColors.Success else CedalColors.TextPrimary,
            fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun BankSendBody(balance: Int?, viewModel: AuthViewModel, onSent: () -> Unit) {
    var friends by remember { mutableStateOf<List<FriendSummary>?>(null) }
    var selected by remember { mutableStateOf<FriendSummary?>(null) }
    var amount by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var done by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(Unit) { viewModel.listFriends().onSuccess { friends = it } }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).imePadding()) {
        Text("SEND STAR COINS", color = CedalColors.TextSecondary, fontSize = 11.sp, letterSpacing = 2.sp)
        Text(
            "Pick a friend, then choose how many SC to send.",
            color = CedalColors.TextMuted, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp, bottom = 12.dp),
        )

        Text("RECIPIENT", color = CedalColors.TextSecondary, fontSize = 11.sp, letterSpacing = 1.sp, modifier = Modifier.padding(bottom = 6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .clip(RoundedCornerShape(14.dp))
                .background(CedalColors.CardBackground)
                .border(1.dp, CedalColors.BorderSlate, RoundedCornerShape(14.dp)),
        ) {
            when {
                friends == null -> Box(Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = CedalColors.AccentCyan, modifier = Modifier.size(20.dp))
                }
                friends!!.isEmpty() -> Text(
                    "You don't have any friends yet — find some from the ✚ button on Chats.",
                    color = CedalColors.TextMuted, fontSize = 12.sp, modifier = Modifier.padding(16.dp),
                )
                else -> LazyColumn(modifier = Modifier.padding(8.dp)) {
                    items(friends!!, key = { it.id }) { friend ->
                        FriendPickRow(friend, selected = selected?.id == friend.id, onClick = { selected = friend; done = false })
                    }
                }
            }
        }

        if (selected != null) {
            val amt = amount.toIntOrNull()
            val projected = if (amt != null && balance != null) balance - amt else null

            Text("AMOUNT (SC)", color = CedalColors.TextSecondary, fontSize = 11.sp, letterSpacing = 1.sp, modifier = Modifier.padding(top = 16.dp, bottom = 6.dp))
            CedalTextField(
                value = amount,
                onValueChange = { amount = it; done = false },
                prefix = "★",
                placeholder = "e.g. 25",
                keyboardType = KeyboardType.Number,
            )

            Row(modifier = Modifier.padding(top = 8.dp)) {
                QUICK_AMOUNTS.forEach { quick ->
                    QuickAmountChip(quick, active = amount == quick.toString()) { amount = quick.toString(); done = false }
                }
            }

            Text(
                if (projected != null) "New balance after sending: $projected SC" else "You can go up to ${-MIN_DEBT_SC} SC into debt.",
                color = if (projected != null && projected < MIN_DEBT_SC) CedalColors.Error else CedalColors.TextMuted,
                fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp),
            )
            CedalErrorText(error)
            CedalPrimaryButton(
                text = when {
                    loading -> "SENDING…"
                    done -> "SENT"
                    amount.isNotBlank() -> "SEND $amount SC"
                    else -> "SEND"
                },
                enabled = !loading,
                loading = loading,
                modifier = Modifier.padding(top = 10.dp),
                onClick = {
                    val value = amount.toIntOrNull()
                    if (value == null || value <= 0) { error = "Enter a whole number greater than 0"; return@CedalPrimaryButton }
                    // Matches cedal-mobile's SendScreen exactly — vibrates
                    // before the send completes, gated on the same setting.
                    if (viewModel.storage.bankLargeSpendAlerts && value > LARGE_SPEND_THRESHOLD_SC) {
                        vibrate(context, 200)
                    }
                    loading = true; error = null
                    scope.launch {
                        val result = viewModel.sendWallet(selected!!.id, value)
                        loading = false
                        result.onSuccess { done = true; amount = ""; onSent() }.onFailure { error = it.message }
                    }
                },
            )
        }
    }
}

@Composable
private fun QuickAmountChip(amount: Int, active: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .padding(end = 8.dp)
            .clip(RoundedCornerShape(50))
            .background(if (active) CedalColors.Success.copy(alpha = 0.15f) else CedalColors.Background)
            .border(1.dp, if (active) CedalColors.Success else CedalColors.BorderSlate, RoundedCornerShape(50))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text("$amount SC", color = if (active) CedalColors.Success else CedalColors.TextSecondary, fontSize = 11.sp)
    }
}

@Composable
private fun FriendPickRow(friend: FriendSummary, selected: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) CedalColors.Success.copy(alpha = 0.08f) else CedalColors.Background)
            .border(1.dp, if (selected) CedalColors.Success else CedalColors.BorderSlate, RoundedCornerShape(12.dp))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(CedalColors.BackgroundBlob)
                .border(1.dp, CedalColors.BorderCyan, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(friend.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?", color = CedalColors.AccentCyan, fontSize = 12.sp)
        }
        Column(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
            Text(friend.name, color = CedalColors.TextPrimary, fontSize = 13.sp)
            friend.email?.let { Text(it, color = CedalColors.TextSecondary, fontSize = 11.sp) }
        }
        if (selected) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(CedalColors.Success.copy(alpha = 0.15f))
                    .border(1.dp, CedalColors.Success, RoundedCornerShape(50))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            ) {
                Text("SELECTED", color = CedalColors.Success, fontSize = 9.sp, letterSpacing = 0.4.sp)
            }
        }
    }
}

@Composable
private fun BankTransactionsBody(viewModel: AuthViewModel) {
    var filter by remember { mutableStateOf<String?>(null) }
    var transactions by remember { mutableStateOf<List<WalletTransactionItem>?>(null) }

    LaunchedEffect(filter) {
        transactions = null
        viewModel.listWalletTransactions(filter).onSuccess { transactions = it }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("TRANSACTIONS", color = CedalColors.TextSecondary, fontSize = 11.sp, letterSpacing = 2.sp, modifier = Modifier.padding(bottom = 10.dp))
        Row(modifier = Modifier.padding(bottom = 12.dp)) {
            TxFilterChip("All", filter == null) { filter = null }
            TxFilterChip("Credit", filter == "credit") { filter = "credit" }
            TxFilterChip("Debit", filter == "debit") { filter = "debit" }
        }

        when {
            transactions == null -> Box(Modifier.fillMaxWidth().padding(top = 24.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = CedalColors.AccentCyan, modifier = Modifier.size(20.dp))
            }
            transactions!!.isEmpty() -> Text(
                "No transactions yet.", color = CedalColors.TextMuted, fontSize = 12.sp,
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            else -> Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(CedalColors.CardBackground)
                    .border(1.dp, CedalColors.BorderSlate, RoundedCornerShape(14.dp))
                    .padding(horizontal = 16.dp),
            ) {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(vertical = 8.dp)) {
                    items(transactions!!, key = { it.id }) { tx ->
                        TransactionRow(tx)
                    }
                }
            }
        }
    }
}

@Composable
private fun TxFilterChip(label: String, active: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .padding(end = 8.dp)
            .clip(RoundedCornerShape(50))
            .background(if (active) CedalColors.CardBackground else CedalColors.Background)
            .border(1.dp, if (active) CedalColors.Success else CedalColors.BorderCyan, RoundedCornerShape(50))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 5.dp),
    ) {
        Text(label.uppercase(), color = CedalColors.TextPrimary, fontSize = 11.sp, letterSpacing = 0.4.sp)
    }
}

// --- Debt overview (ported from cedal-mobile's DebtScreen.tsx — pure
// display, no writes; just reads the same balance the Home tab shows). ---

@Composable
fun BankDebtBody(onBack: () -> Unit, viewModel: AuthViewModel = hiltViewModel()) {
    var balance by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(Unit) { viewModel.getWalletBalance().onSuccess { balance = it } }

    val maxDebt = -MIN_DEBT_SC
    val bal = balance ?: 0
    val isInDebt = bal < 0
    val debt = if (isInDebt) kotlin.math.abs(bal) else 0
    val remainingRoom = if (isInDebt) maxDebt - debt else maxDebt
    val progress = when {
        bal > 0 -> (bal.toFloat() / maxDebt).coerceAtMost(1f)
        bal < 0 -> (debt.toFloat() / maxDebt).coerceAtMost(1f)
        else -> 0.5f
    }
    val barColor = when {
        bal > 0 -> CedalColors.Success
        bal < 0 -> CedalColors.Error
        else -> CedalColors.AccentSky
    }

    Column(modifier = Modifier.fillMaxSize().background(CedalColors.Background).verticalScroll(rememberScrollState()).padding(16.dp)) {
        MemberBackBar(title = "Debt overview", onBack = onBack)
        Text(
            "See how much Star Coin debt you have and your limit.",
            color = CedalColors.TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(bottom = 16.dp),
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CedalColors.CardBackground)
                .border(1.dp, CedalColors.BorderSlate, RoundedCornerShape(16.dp))
                .padding(16.dp),
        ) {
            Text("CURRENT BALANCE", color = CedalColors.TextSecondary, fontSize = 11.sp, letterSpacing = 1.sp)
            Text(
                if (balance != null) "$bal SC" else "…",
                color = if (isInDebt) CedalColors.Error else CedalColors.Success,
                fontSize = 28.sp, fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(top = 4.dp, bottom = 10.dp),
            )

            DebtStatRow("Maximum allowed debt", "-$maxDebt SC")
            DebtStatRow("Remaining before limit", "$remainingRoom SC")

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .height(8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(CedalColors.Background),
            ) {
                Box(modifier = Modifier.fillMaxWidth(progress).fillMaxHeight().clip(RoundedCornerShape(50)).background(barColor))
            }

            Text(
                "You can go down to a maximum of -$maxDebt SC. Spending from 0 will put you into debt, but you cannot go past this limit.",
                color = CedalColors.TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp),
            )
        }
    }
}

@Composable
private fun DebtStatRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = CedalColors.TextSecondary, fontSize = 12.sp)
        Text(value, color = CedalColors.TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

// --- Create Trade (ported from cedal-mobile's TradeScreen.tsx — posts a
// global "request SC" board entry that mirrors into Notifications). ---

@Composable
fun BankTradeBody(onBack: () -> Unit, viewModel: AuthViewModel = hiltViewModel()) {
    var balance by remember { mutableStateOf<Int?>(null) }
    var amount by remember { mutableStateOf("") }
    var plea by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { viewModel.getWalletBalance().onSuccess { balance = it } }

    Column(modifier = Modifier.fillMaxSize().background(CedalColors.Background).verticalScroll(rememberScrollState()).padding(16.dp).imePadding()) {
        MemberBackBar(title = "Create Trade", onBack = onBack)
        Text(
            "Post a global Star Coin trade request with an optional message.",
            color = CedalColors.TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(bottom = 12.dp),
        )

        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("YOUR BALANCE", color = CedalColors.TextSecondary, fontSize = 11.sp, letterSpacing = 1.sp)
            Text(
                if (balance != null) "$balance SC" else "…",
                color = CedalColors.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(CedalColors.CardBackground)
                .border(1.dp, CedalColors.BorderSlate, RoundedCornerShape(14.dp))
                .padding(16.dp),
        ) {
            Text("AMOUNT TO REQUEST", color = CedalColors.TextSecondary, fontSize = 11.sp, letterSpacing = 1.sp, modifier = Modifier.padding(bottom = 8.dp))
            CedalTextField(
                value = amount,
                onValueChange = { amount = it.filter(Char::isDigit) },
                prefix = "★", placeholder = "0", keyboardType = KeyboardType.Number,
            )
            Text(
                "This trade is global. You can request more than your current balance.",
                color = CedalColors.TextMuted, fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(CedalColors.CardBackground)
                .border(1.dp, CedalColors.BorderSlate, RoundedCornerShape(14.dp))
                .padding(16.dp),
        ) {
            Text("PLEA MESSAGE (OPTIONAL)", color = CedalColors.TextSecondary, fontSize = 11.sp, letterSpacing = 1.sp, modifier = Modifier.padding(bottom = 8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(CedalColors.Background)
                    .border(1.dp, CedalColors.BorderSlate, RoundedCornerShape(10.dp))
                    .padding(10.dp),
            ) {
                if (plea.isEmpty()) {
                    Text("Explain why you need these Star Coins...", color = CedalColors.TextMuted, fontSize = 12.sp)
                }
                BasicTextField(
                    value = plea,
                    onValueChange = { if (it.length <= 280) plea = it },
                    textStyle = TextStyle(color = CedalColors.TextPrimary, fontSize = 13.sp),
                    cursorBrush = SolidColor(CedalColors.AccentCyan),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 70.dp),
                )
            }
            Text(
                "${plea.length}/280", color = CedalColors.TextMuted, fontSize = 10.sp, textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            )
        }

        CedalErrorText(error)
        CedalPrimaryButton(
            text = if (loading) "POSTING…" else "POST TRADE",
            enabled = !loading && (amount.toIntOrNull() ?: 0) > 0,
            loading = loading,
            modifier = Modifier.padding(top = 14.dp),
            onClick = {
                val amt = amount.toIntOrNull()
                if (amt == null || amt <= 0) { error = "Enter an amount greater than 0"; return@CedalPrimaryButton }
                loading = true; error = null
                scope.launch {
                    val result = viewModel.postTrade(amt, plea.trim().ifBlank { null })
                    loading = false
                    result.onSuccess { onBack() }.onFailure { error = it.message }
                }
            },
        )
    }
}

// --- Notifications (ported from cedal-mobile's NotificationsScreen.tsx —
// the global feed that Trade posts mirror into). Fulfilling someone else's
// trade ("Credit") reuses the same wallet transfer as Send; the creator can
// remove their own post ("Delete"). ---

@Composable
fun BankNotificationsBody(onBack: () -> Unit, viewModel: AuthViewModel = hiltViewModel()) {
    var notifications by remember { mutableStateOf<List<NotificationItem>?>(null) }
    var processingId by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun reload() {
        scope.launch { viewModel.listNotifications().onSuccess { notifications = it } }
    }
    LaunchedEffect(Unit) { reload() }

    Column(modifier = Modifier.fillMaxSize().background(CedalColors.Background).padding(16.dp)) {
        MemberBackBar(title = "Notifications", onBack = onBack)
        Text(
            "Global trades and business updates history.",
            color = CedalColors.TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(bottom = 12.dp),
        )
        CedalErrorText(error)

        when {
            notifications == null -> Box(Modifier.fillMaxWidth().padding(top = 32.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = CedalColors.AccentCyan, modifier = Modifier.size(20.dp))
            }
            notifications!!.isEmpty() -> Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
            ) {
                Icon(Icons.Outlined.NotificationsOff, contentDescription = null, tint = CedalColors.TextMuted, modifier = Modifier.size(26.dp))
                Text("No notifications yet", color = CedalColors.TextPrimary, fontSize = 15.sp, modifier = Modifier.padding(top = 10.dp))
                Text(
                    "When global trades or business messages are created, they will appear here.",
                    color = CedalColors.TextSecondary, fontSize = 12.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(notifications!!, key = { it.id }) { item ->
                    NotificationCard(
                        item = item,
                        processing = processingId == item.id,
                        onCredit = {
                            processingId = item.id; error = null
                            scope.launch {
                                val result = viewModel.creditNotification(item.id)
                                processingId = null
                                result.onSuccess { reload() }.onFailure { error = it.message }
                            }
                        },
                        onDelete = {
                            processingId = item.id; error = null
                            scope.launch {
                                val result = viewModel.deleteNotification(item.id)
                                processingId = null
                                result.onSuccess { reload() }.onFailure { error = it.message }
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationCard(item: NotificationItem, processing: Boolean, onCredit: () -> Unit, onDelete: () -> Unit) {
    val isTrade = item.type == "trade"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(CedalColors.CardBackground)
            .border(1.dp, CedalColors.BorderSlate, RoundedCornerShape(14.dp))
            .padding(12.dp),
    ) {
        Icon(
            if (isTrade) Icons.Outlined.SwapHoriz else Icons.Outlined.Info,
            contentDescription = null,
            tint = if (isTrade) CedalColors.Success else CedalColors.AccentSky,
            modifier = Modifier.padding(top = 2.dp, end = 10.dp).size(20.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(item.title, color = CedalColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Text(item.body, color = CedalColors.TextSecondary, fontSize = 13.sp, maxLines = 2, modifier = Modifier.padding(top = 2.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
                Text(
                    buildString {
                        item.fromUserName?.let { append("$it • ") }
                        append(dateFormat.format(Date(item.createdAt)))
                        append(" · ")
                        append(timeFormat.format(Date(item.createdAt)))
                    },
                    color = CedalColors.TextMuted, fontSize = 11.sp, modifier = Modifier.weight(1f),
                )
                if (isTrade) {
                    if (!item.isMine) {
                        NotifActionPill(if (processing) "Sending…" else "Credit", CedalColors.Success, enabled = !processing, onClick = onCredit)
                    } else {
                        NotifActionPill(if (processing) "Deleting…" else "Delete", CedalColors.Error, enabled = !processing, onClick = onDelete)
                    }
                }
            }
        }
    }
}

@Composable
private fun NotifActionPill(label: String, accent: Color, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(accent.copy(alpha = 0.12f))
            .clickable(enabled = enabled, interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(label.uppercase(), color = accent, fontSize = 10.sp, letterSpacing = 0.4.sp)
    }
}

// --- Tasks ---
//
// Ported from cedal-mobile's TasksScreen.tsx — which, unlike the other Bank
// screens, is genuinely just a UI shell in the reference app too (its own
// comment says "Placeholder content area for whoever wires tasks later").
// There's no task/reward/reset data model anywhere in cedal-mobile, so
// building one here would be inventing a feature rather than porting one.
// This matches that shell exactly (same 5 categories, same empty state)
// with the same visual polish as the rest of Bank.

private enum class TaskFilter(val label: String) {
    DAILY("Daily"), WEEKLY("Weekly"), MONTHLY("Monthly"), YEARLY("Yearly"), EVENTS("Events")
}

@Composable
private fun BankTasksBody() {
    var activeFilter by remember { mutableStateOf(TaskFilter.DAILY) }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("TASKS", color = CedalColors.TextSecondary, fontSize = 11.sp, letterSpacing = 2.sp)
        Text(
            "Complete tasks to earn XP, items, and Star Coins.",
            color = CedalColors.TextMuted, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp, bottom = 14.dp),
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState()).padding(bottom = 14.dp),
        ) {
            TaskFilter.entries.forEach { filter ->
                TaskFilterPill(filter.label, active = activeFilter == filter) { activeFilter = filter }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(CedalColors.CardBackground)
                .border(1.dp, CedalColors.BorderSlate, RoundedCornerShape(14.dp))
                .padding(16.dp),
        ) {
            Text(
                "${activeFilter.label.uppercase()} TASKS",
                color = CedalColors.TextSecondary, fontSize = 11.sp, letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            Text(
                "No tasks loaded. Tasks will appear here for this category.",
                color = CedalColors.TextMuted, fontSize = 13.sp,
            )
        }
    }
}

@Composable
private fun TaskFilterPill(label: String, active: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (active) CedalColors.AccentSky.copy(alpha = 0.15f) else CedalColors.CardBackground)
            .border(1.dp, if (active) CedalColors.AccentSky else CedalColors.BorderSlate, RoundedCornerShape(50))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            label.uppercase(),
            color = if (active) CedalColors.AccentSky else CedalColors.TextSecondary,
            fontSize = 11.sp, letterSpacing = 0.5.sp,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}
