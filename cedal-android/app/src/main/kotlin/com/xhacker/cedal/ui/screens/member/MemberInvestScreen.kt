package com.xhacker.cedal.ui.screens.member

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xhacker.cedal.data.MarketAssetDetail
import com.xhacker.cedal.data.MarketAssetSummary
import com.xhacker.cedal.data.PortfolioHoldingItem
import com.xhacker.cedal.data.PortfolioResponse
import com.xhacker.cedal.data.PortfolioTransactionItem
import com.xhacker.cedal.data.WatchlistItem
import com.xhacker.cedal.ui.theme.CedalColors
import com.xhacker.cedal.ui.theme.CedalErrorText
import com.xhacker.cedal.ui.theme.CedalGhostButton
import com.xhacker.cedal.ui.theme.CedalPrimaryButton
import com.xhacker.cedal.ui.theme.CedalTextField
import com.xhacker.cedal.viewmodel.AuthViewModel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import kotlin.math.abs

private enum class InvestTab { OVERVIEW, WATCHLIST, TRADE, LEARN }

private val usd = DecimalFormat("#,##0.00")
private val qtyFmt = DecimalFormat("#,##0.########")

// Ported from cedal-mobile's FinanceRouter.tsx + Overview/Watchlist/Trade/
// Asset/Learn screens — but upgraded from that reference's 100%-hardcoded
// mock numbers ("later: pull this from Firestore / financeApi") to real
// crypto prices via CoinGecko, with simulated buy/sell against a virtual
// USD cash balance. Stocks aren't wired up yet (would need a keyed
// provider); the server's assetType field leaves room for that later.
@Composable
fun InvestRouterBody(onBack: () -> Unit, viewModel: AuthViewModel = hiltViewModel()) {
    var activeTab by remember { mutableStateOf(InvestTab.OVERVIEW) }
    var selectedAssetId by remember { mutableStateOf<String?>(null) }

    fun goTab(tab: InvestTab) {
        activeTab = tab
        selectedAssetId = null
    }

    Column(modifier = Modifier.fillMaxSize().background(CedalColors.Background)) {
        Box(modifier = Modifier.weight(1f)) {
            val assetId = selectedAssetId
            if (assetId != null) {
                InvestAssetBody(assetId = assetId, onBack = { selectedAssetId = null }, onOpenTrade = { goTab(InvestTab.TRADE) }, viewModel = viewModel)
            } else {
                when (activeTab) {
                    InvestTab.OVERVIEW -> InvestOverviewBody(
                        onBack = onBack,
                        onOpenWatchlist = { goTab(InvestTab.WATCHLIST) },
                        onOpenAsset = { selectedAssetId = it },
                        viewModel = viewModel,
                    )
                    InvestTab.WATCHLIST -> InvestWatchlistBody(onOpenAsset = { selectedAssetId = it }, viewModel = viewModel)
                    InvestTab.TRADE -> InvestTradeBody(onOpenAsset = { selectedAssetId = it }, viewModel = viewModel)
                    InvestTab.LEARN -> InvestLearnBody(onOpenTrade = { goTab(InvestTab.TRADE) }, viewModel = viewModel)
                }
            }
        }
        InvestBottomBar(activeTab) { goTab(it) }
    }
}

@Composable
private fun InvestBottomBar(active: InvestTab, onNavigateTab: (InvestTab) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .background(CedalColors.CardBackground)
            .border(width = Dp.Hairline, color = CedalColors.BorderSlate),
    ) {
        InvestTabItem("Overview", Icons.Outlined.ShowChart, active == InvestTab.OVERVIEW) { onNavigateTab(InvestTab.OVERVIEW) }
        InvestTabItem("Watchlist", Icons.Outlined.StarBorder, active == InvestTab.WATCHLIST) { onNavigateTab(InvestTab.WATCHLIST) }
        InvestTabItem("Trade", Icons.Outlined.SwapHoriz, active == InvestTab.TRADE) { onNavigateTab(InvestTab.TRADE) }
        InvestTabItem("Learn", Icons.Outlined.Book, active == InvestTab.LEARN) { onNavigateTab(InvestTab.LEARN) }
    }
}

@Composable
private fun InvestTabItem(label: String, icon: ImageVector, active: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick),
    ) {
        Icon(icon, contentDescription = label, tint = if (active) CedalColors.Success else CedalColors.TextMuted, modifier = Modifier.size(20.dp))
        Text(label, color = if (active) CedalColors.Success else CedalColors.TextMuted, fontSize = 11.sp, modifier = Modifier.padding(top = 3.dp))
    }
}

// --- Overview ---

@Composable
private fun InvestOverviewBody(
    onBack: () -> Unit,
    onOpenWatchlist: () -> Unit,
    onOpenAsset: (String) -> Unit,
    viewModel: AuthViewModel,
) {
    var portfolio by remember { mutableStateOf<PortfolioResponse?>(null) }
    var markets by remember { mutableStateOf<List<MarketAssetSummary>?>(null) }
    var watchlist by remember { mutableStateOf<List<WatchlistItem>?>(null) }

    // The 3 calls are independent — run them concurrently instead of one
    // after another, since each is its own network round trip.
    LaunchedEffect(Unit) {
        coroutineScope {
            launch { viewModel.getPortfolio().onSuccess { portfolio = it } }
            launch { viewModel.listMarkets().onSuccess { markets = it } }
            launch { viewModel.listWatchlist().onSuccess { watchlist = it } }
        }
    }

    val gainLoss = portfolio?.holdings?.sumOf { it.gainLossValue } ?: 0.0
    val costBasisTotal = portfolio?.holdings?.sumOf { it.quantity * it.avgCostBasis } ?: 0.0
    val gainLossPct = if (costBasisTotal > 0) (gainLoss / costBasisTotal) * 100 else 0.0

    val bestMover = markets?.maxByOrNull { it.changePercent24h }
    val worstMover = markets?.minByOrNull { it.changePercent24h }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        MemberBackBar(title = "Portfolio overview", onBack = onBack)
        Text(
            "Real crypto prices, simulated trades — snapshot of your positions and movers.",
            color = CedalColors.TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(bottom = 16.dp),
        )

        // Portfolio value card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(CedalColors.CardBackground)
                .border(1.5.dp, CedalColors.BorderCyan, RoundedCornerShape(20.dp))
                .padding(vertical = 20.dp, horizontal = 20.dp),
        ) {
            Text("TOTAL PORTFOLIO VALUE", color = CedalColors.TextSecondary, fontSize = 11.sp, letterSpacing = 1.5.sp)
            Text(
                if (portfolio != null) "$${usd.format(portfolio!!.totalPortfolioValue)}" else "…",
                color = CedalColors.TextPrimary, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(top = 8.dp),
            )
            if (portfolio != null && costBasisTotal > 0) {
                Text(
                    "${if (gainLoss >= 0) "+" else "-"}$${usd.format(abs(gainLoss))} (${if (gainLossPct >= 0) "+" else ""}${usd.format(gainLossPct)}%) unrealized",
                    color = if (gainLoss >= 0) CedalColors.Success else CedalColors.Error,
                    fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp),
                )
            }
            Row(modifier = Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Cash: ${portfolio?.let { "$${usd.format(it.cashBalance)}" } ?: "…"}", color = CedalColors.TextMuted, fontSize = 11.sp)
                Text("Holdings: ${portfolio?.let { "$${usd.format(it.totalHoldingsValue)}" } ?: "…"}", color = CedalColors.TextMuted, fontSize = 11.sp)
            }
        }

        // Movers row (real 24h data from the curated coin list)
        Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MoverCard("Best mover", bestMover, modifier = Modifier.weight(1f), onClick = { bestMover?.let { onOpenAsset(it.id) } })
            MoverCard("Worst mover", worstMover, modifier = Modifier.weight(1f), onClick = { worstMover?.let { onOpenAsset(it.id) } })
        }

        // Watchlist preview
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
                Text("WATCHLIST", color = CedalColors.TextSecondary, fontSize = 11.sp, letterSpacing = 1.sp)
                Text(
                    "View all", color = CedalColors.AccentSky, fontSize = 12.sp,
                    modifier = Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onOpenWatchlist),
                )
            }
            when {
                watchlist == null -> Box(Modifier.fillMaxWidth().padding(top = 16.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = CedalColors.AccentCyan, modifier = Modifier.size(18.dp))
                }
                watchlist!!.isEmpty() -> Text(
                    "Nothing on your watchlist yet.", color = CedalColors.TextMuted, fontSize = 12.sp,
                    modifier = Modifier.padding(top = 8.dp),
                )
                else -> Column(modifier = Modifier.padding(top = 6.dp)) {
                    watchlist!!.take(3).forEach { item -> WatchlistRow(item, onClick = { onOpenAsset(item.symbol) }) }
                }
            }
        }
    }
}

@Composable
private fun MoverCard(label: String, item: MarketAssetSummary?, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val up = (item?.changePercent24h ?: 0.0) >= 0
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(CedalColors.CardBackground)
            .border(1.dp, CedalColors.BorderSlate, RoundedCornerShape(14.dp))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(14.dp),
    ) {
        Text(label, color = CedalColors.TextSecondary, fontSize = 10.sp, letterSpacing = 0.8.sp)
        Text(item?.symbol ?: "…", color = CedalColors.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 4.dp))
        if (item != null) {
            Text(
                "${if (up) "+" else ""}${usd.format(item.changePercent24h)}%",
                color = if (up) CedalColors.Success else CedalColors.Error, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

@Composable
private fun WatchlistRow(item: WatchlistItem, onClick: () -> Unit) {
    val up = item.changePercent24h >= 0
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(vertical = 8.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(item.symbol, color = CedalColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(item.name, color = CedalColors.TextMuted, fontSize = 11.sp)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("$${usd.format(item.price)}", color = CedalColors.TextPrimary, fontSize = 13.sp)
            Text(
                "${if (up) "+" else ""}${usd.format(item.changePercent24h)}%",
                color = if (up) CedalColors.Success else CedalColors.Error, fontSize = 11.sp,
            )
        }
    }
}

// --- Watchlist ---

@Composable
private fun InvestWatchlistBody(onOpenAsset: (String) -> Unit, viewModel: AuthViewModel) {
    var watchlist by remember { mutableStateOf<List<WatchlistItem>?>(null) }
    val scope = rememberCoroutineScope()

    fun reload() { scope.launch { viewModel.listWatchlist().onSuccess { watchlist = it } } }
    LaunchedEffect(Unit) { reload() }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("WATCHLIST", color = CedalColors.TextSecondary, fontSize = 11.sp, letterSpacing = 2.sp)
        Text(
            "Assets you're tracking. Open one to add it or trade.",
            color = CedalColors.TextMuted, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp, bottom = 12.dp),
        )
        when {
            watchlist == null -> Box(Modifier.fillMaxWidth().padding(top = 24.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = CedalColors.AccentCyan, modifier = Modifier.size(20.dp))
            }
            watchlist!!.isEmpty() -> Text(
                "Nothing here yet — open an asset from Trade and tap the star to add it.",
                color = CedalColors.TextMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 24.dp),
            )
            else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(watchlist!!, key = { it.symbol }) { item ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(CedalColors.CardBackground)
                            .border(1.dp, CedalColors.BorderSlate, RoundedCornerShape(14.dp))
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = { onOpenAsset(item.symbol) })
                            .padding(14.dp),
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.symbol, color = CedalColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text(item.name, color = CedalColors.TextMuted, fontSize = 11.sp)
                        }
                        Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(end = 10.dp)) {
                            Text("$${usd.format(item.price)}", color = CedalColors.TextPrimary, fontSize = 13.sp)
                            val up = item.changePercent24h >= 0
                            Text(
                                "${if (up) "+" else ""}${usd.format(item.changePercent24h)}%",
                                color = if (up) CedalColors.Success else CedalColors.Error, fontSize = 11.sp,
                            )
                        }
                        Icon(
                            Icons.Outlined.Star, contentDescription = "Remove from watchlist", tint = CedalColors.AccentSky,
                            modifier = Modifier
                                .size(18.dp)
                                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                    scope.launch { viewModel.removeFromWatchlist(item.symbol).onSuccess { reload() } }
                                },
                        )
                    }
                }
            }
        }
    }
}

// --- Trade ---

@Composable
private fun InvestTradeBody(onOpenAsset: (String) -> Unit, viewModel: AuthViewModel) {
    var markets by remember { mutableStateOf<List<MarketAssetSummary>?>(null) }
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<MarketAssetSummary?>(null) }
    var side by remember { mutableStateOf("buy") }
    var qty by remember { mutableStateOf("") }
    var cash by remember { mutableStateOf<Double?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var done by remember { mutableStateOf(false) }
    var recent by remember { mutableStateOf<List<PortfolioTransactionItem>?>(null) }
    val scope = rememberCoroutineScope()

    suspend fun reload() {
        coroutineScope {
            launch { viewModel.getPortfolio().onSuccess { cash = it.cashBalance } }
            launch { viewModel.listPortfolioTransactions().onSuccess { recent = it.take(5) } }
        }
    }
    LaunchedEffect(Unit) {
        coroutineScope {
            launch { viewModel.listMarkets().onSuccess { markets = it } }
            launch { reload() }
        }
    }

    val filteredMarkets = remember(markets, query) {
        val q = query.trim()
        if (q.isBlank()) markets.orEmpty()
        else markets.orEmpty().filter { it.symbol.contains(q, ignoreCase = true) || it.name.contains(q, ignoreCase = true) }
    }
    val quantity = qty.toDoubleOrNull()
    val estimate = if (quantity != null && selected != null) quantity * selected!!.price else null

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp).imePadding()) {
        Text("TRADE", color = CedalColors.TextSecondary, fontSize = 11.sp, letterSpacing = 2.sp)
        Text(
            "Real prices, simulated fills against your virtual cash.",
            color = CedalColors.TextMuted, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp, bottom = 4.dp),
        )
        Text(
            cash?.let { "Cash available: $${usd.format(it)}" } ?: "…",
            color = CedalColors.TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(bottom = 12.dp),
        )

        Text(
            "ASSET" + (markets?.let { " (${it.size} available)" } ?: ""),
            color = CedalColors.TextSecondary, fontSize = 11.sp, letterSpacing = 1.sp, modifier = Modifier.padding(bottom = 6.dp),
        )
        when {
            markets == null -> CircularProgressIndicator(color = CedalColors.AccentCyan, modifier = Modifier.size(20.dp))
            else -> {
                CedalTextField(
                    value = query,
                    onValueChange = { query = it },
                    prefix = "🔍",
                    placeholder = "Search by symbol or name (e.g. BTC, Solana)",
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                var changingAsset by remember { mutableStateOf(false) }
                if (selected != null && query.isBlank() && !changingAsset) {
                    // Selection stays visible as a single row instead of the
                    // whole list, so the form below doesn't jump around.
                    Column(modifier = Modifier.padding(bottom = 12.dp)) {
                        AssetPickRow(selected!!, active = true) { changingAsset = true }
                        Text(
                            "Change asset", color = CedalColors.AccentSky, fontSize = 11.sp,
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { changingAsset = true },
                        )
                    }
                } else {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp).padding(bottom = 12.dp)) {
                        if (filteredMarkets.isEmpty()) {
                            Text("No matches.", color = CedalColors.TextMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                        } else {
                            LazyColumn {
                                items(filteredMarkets, key = { it.id }) { asset ->
                                    AssetPickRow(asset, active = selected?.id == asset.id) {
                                        selected = asset; done = false; query = ""; changingAsset = false
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (selected != null) {
            Row(modifier = Modifier.padding(bottom = 12.dp)) {
                SideToggleChip("Buy", side == "buy", CedalColors.Success) { side = "buy"; done = false }
                SideToggleChip("Sell", side == "sell", CedalColors.Error) { side = "sell"; done = false }
            }

            Text("QUANTITY", color = CedalColors.TextSecondary, fontSize = 11.sp, letterSpacing = 1.sp, modifier = Modifier.padding(bottom = 6.dp))
            CedalTextField(
                value = qty,
                onValueChange = { qty = it.filter { c -> c.isDigit() || c == '.' }; done = false },
                prefix = "#", placeholder = "e.g. 0.05", keyboardType = KeyboardType.Decimal,
            )
            Text(
                "1 ${selected!!.symbol} = $${usd.format(selected!!.price)}" + (estimate?.let { " · Est. ${if (side == "buy") "cost" else "proceeds"}: $${usd.format(it)}" } ?: ""),
                color = CedalColors.TextMuted, fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp),
            )
            CedalErrorText(error)
            CedalPrimaryButton(
                text = when {
                    loading -> "PLACING…"
                    done -> "DONE"
                    else -> "${side.uppercase()} ${selected!!.symbol}"
                },
                enabled = !loading,
                loading = loading,
                modifier = Modifier.padding(top = 10.dp),
                onClick = {
                    val value = quantity
                    if (value == null || value <= 0) { error = "Enter a quantity greater than 0"; return@CedalPrimaryButton }
                    loading = true; error = null
                    scope.launch {
                        val result = if (side == "buy") {
                            viewModel.buyAsset(selected!!.id, value)
                        } else {
                            viewModel.sellAsset(selected!!.id, value)
                        }
                        loading = false
                        result.onSuccess { done = true; qty = ""; reload() }.onFailure { error = it.message }
                    }
                },
            )
        }

        Column(modifier = Modifier.fillMaxWidth().padding(top = 20.dp)) {
            Text("RECENT ORDERS", color = CedalColors.TextSecondary, fontSize = 11.sp, letterSpacing = 1.sp, modifier = Modifier.padding(bottom = 6.dp))
            when {
                recent == null -> CircularProgressIndicator(color = CedalColors.AccentCyan, modifier = Modifier.size(18.dp))
                recent!!.isEmpty() -> Text("No orders yet.", color = CedalColors.TextMuted, fontSize = 12.sp)
                else -> recent!!.forEach { tx -> InvestTransactionRow(tx) }
            }
        }
    }
}

@Composable
private fun AssetPickRow(asset: MarketAssetSummary, active: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val up = asset.changePercent24h >= 0
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (active) CedalColors.Success.copy(alpha = 0.08f) else CedalColors.CardBackground)
            .border(1.dp, if (active) CedalColors.Success else CedalColors.BorderSlate, RoundedCornerShape(12.dp))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(asset.symbol, color = CedalColors.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(asset.name, color = CedalColors.TextMuted, fontSize = 11.sp)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("$${usd.format(asset.price)}", color = CedalColors.TextPrimary, fontSize = 12.sp)
            Text(
                "${if (up) "+" else ""}${usd.format(asset.changePercent24h)}%",
                color = if (up) CedalColors.Success else CedalColors.Error, fontSize = 10.sp,
            )
        }
    }
}

@Composable
private fun SideToggleChip(label: String, active: Boolean, accent: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .padding(end = 8.dp)
            .clip(RoundedCornerShape(50))
            .background(if (active) accent.copy(alpha = 0.15f) else CedalColors.CardBackground)
            .border(1.dp, if (active) accent else CedalColors.BorderSlate, RoundedCornerShape(50))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        Text(label.uppercase(), color = if (active) accent else CedalColors.TextSecondary, fontSize = 12.sp, letterSpacing = 0.5.sp)
    }
}

@Composable
private fun InvestTransactionRow(tx: PortfolioTransactionItem) {
    val isBuy = tx.side == "buy"
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(
            tx.side.uppercase(), color = if (isBuy) CedalColors.Success else CedalColors.Error,
            fontSize = 10.sp, letterSpacing = 0.4.sp,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background((if (isBuy) CedalColors.Success else CedalColors.Error).copy(alpha = 0.12f))
                .padding(horizontal = 8.dp, vertical = 3.dp),
        )
        Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
            Text("${qtyFmt.format(tx.quantity)} ${tx.symbol.uppercase()}", color = CedalColors.TextPrimary, fontSize = 12.sp)
            Text("@ $${usd.format(tx.pricePerUnit)}", color = CedalColors.TextMuted, fontSize = 10.sp)
        }
        Text("$${usd.format(tx.totalValue)}", color = CedalColors.TextSecondary, fontSize = 12.sp)
    }
}

// --- Asset detail ---

@Composable
private fun InvestAssetBody(assetId: String, onBack: () -> Unit, onOpenTrade: () -> Unit, viewModel: AuthViewModel) {
    var detail by remember { mutableStateOf<MarketAssetDetail?>(null) }
    var watchlist by remember { mutableStateOf<List<WatchlistItem>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    suspend fun reload() {
        coroutineScope {
            launch { viewModel.getMarketDetail(assetId).onSuccess { detail = it }.onFailure { error = it.message } }
            launch { viewModel.listWatchlist().onSuccess { watchlist = it } }
        }
    }
    LaunchedEffect(assetId) { reload() }

    val isWatched = watchlist?.any { it.symbol == assetId } == true

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        MemberBackBar(title = detail?.name ?: assetId, onBack = onBack)
        CedalErrorText(error)

        if (detail == null) {
            Box(Modifier.fillMaxWidth().padding(top = 32.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = CedalColors.AccentCyan, modifier = Modifier.size(24.dp))
            }
            return
        }

        val d = detail!!
        val up = d.changePercent24h >= 0

        Text("$${usd.format(d.price)}", color = CedalColors.TextPrimary, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
        Text(
            "${if (up) "+" else ""}${usd.format(d.changePercent24h)}% past 24h",
            color = if (up) CedalColors.Success else CedalColors.Error, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp, bottom = 16.dp),
        )

        // Real 7-day price chart from CoinGecko history.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(CedalColors.CardBackground)
                .border(1.dp, CedalColors.BorderSlate, RoundedCornerShape(14.dp))
                .padding(14.dp),
        ) {
            Text("LAST 7 DAYS", color = CedalColors.TextSecondary, fontSize = 11.sp, letterSpacing = 1.sp, modifier = Modifier.padding(bottom = 8.dp))
            if (d.priceHistory.size >= 2) {
                PriceChart(d.priceHistory.map { it.price }, up)
            } else {
                Text("Not enough history yet.", color = CedalColors.TextMuted, fontSize = 12.sp)
            }
        }

        Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AssetActionButton(
                label = if (isWatched) "Watching" else "Add to watchlist",
                icon = if (isWatched) Icons.Outlined.Star else Icons.Outlined.StarBorder,
                modifier = Modifier.weight(1f),
                onClick = {
                    scope.launch {
                        if (isWatched) viewModel.removeFromWatchlist(assetId).onSuccess { reload() }
                        else viewModel.addToWatchlist(assetId).onSuccess { reload() }
                    }
                },
            )
            AssetActionButton(label = "Trade", icon = Icons.Outlined.SwapHoriz, modifier = Modifier.weight(1f), onClick = onOpenTrade)
        }
    }
}

@Composable
private fun AssetActionButton(label: String, icon: ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(CedalColors.CardBackground)
            .border(1.dp, CedalColors.BorderCyan, RoundedCornerShape(50))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(vertical = 10.dp),
    ) {
        Icon(icon, contentDescription = null, tint = CedalColors.AccentCyan, modifier = Modifier.size(16.dp))
        Text(label, color = CedalColors.TextPrimary, fontSize = 12.sp, modifier = Modifier.padding(start = 6.dp))
    }
}

@Composable
private fun PriceChart(prices: List<Double>, up: Boolean) {
    val lineColor = if (up) CedalColors.Success else CedalColors.Error
    Canvas(modifier = Modifier.fillMaxWidth().height(90.dp)) {
        val min = prices.min()
        val max = prices.max()
        val range = (max - min).takeIf { it > 0 } ?: 1.0
        val stepX = size.width / (prices.size - 1)

        val path = androidx.compose.ui.graphics.Path()
        prices.forEachIndexed { index, price ->
            val x = index * stepX
            val y = size.height - ((price - min) / range * size.height).toFloat()
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color = lineColor, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f))

        // Subtle start/end dots so the trend direction is unambiguous at a glance.
        val firstY = size.height - ((prices.first() - min) / range * size.height).toFloat()
        val lastY = size.height - ((prices.last() - min) / range * size.height).toFloat()
        drawCircle(color = lineColor, radius = 5f, center = Offset(0f, firstY))
        drawCircle(color = lineColor, radius = 5f, center = Offset(size.width, lastY))
    }
}

// --- Learn ---
//
// Rebuilt after feedback: no video (WebView YouTube embeds turned out
// unreliable — loaded but never actually played), a list-of-titles screen
// that opens a real detail page per lesson (not one long scrolling wall of
// text), everything rewritten in plain, explain-it-like-I'm-5 language with
// concrete everyday examples, and an actual interactive quiz at the end of
// each lesson plus a one-tap link into the real Trade screen so it's
// practice, not just reading.

private data class QuizQuestion(
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String,
)

private data class Lesson(
    val title: String,
    val level: String,
    val duration: String,
    val tag: String,
    val teaser: String,
    val body: String,
    val quiz: List<QuizQuestion>,
    val practiceHint: String? = null,
)

private val LEVEL_ORDER = listOf("Beginner", "Intermediate", "Advanced")

// Matches LessonService.EXP_PER_LESSON server-side - flat, same for every
// lesson across every Learn area (Invest and ARC alike).
private const val INVEST_LESSON_EXP = 50

private val LESSONS = listOf(
    Lesson(
        title = "What even is crypto?",
        level = "Beginner", duration = "3 min", tag = "Basics",
        teaser = "Totally new to this? Start here first.",
        body = "Let's start from zero. Money used to just be coins and paper bills you could hold in your hand. Then " +
            "banks started keeping money as numbers in a computer instead — when you check a bank app, you're not " +
            "looking at real bills sitting somewhere, you're looking at a number that ONE bank keeps track of for you.\n\n" +
            "Crypto (short for \"cryptocurrency\") is also just numbers in a computer. But here's the big difference: " +
            "instead of one bank keeping the numbers in one computer, THOUSANDS of computers all over the world each " +
            "keep an identical copy of the same notebook of numbers, and they constantly check each other's copy to " +
            "make sure nobody cheats. Nobody owns that notebook — everyone using it shares it together.\n\n" +
            "Bitcoin was the first crypto ever made. Since then, thousands of others have been created, each with its " +
            "own shared notebook (this app lists 100 of them, ranked by size). When you \"buy\" a coin in this app, " +
            "you're recording — in your own practice portfolio only — that you now own some of it, using REAL prices " +
            "from the real world, but with pretend money, so you can learn with zero real risk.",
        quiz = listOf(
            QuizQuestion(
                "What is crypto, in the simplest terms?",
                listOf("Numbers tracked by thousands of computers together, instead of one bank", "Real metal coins you can hold", "A type of paper money", "A video game score"),
                correctIndex = 0,
                explanation = "Crypto is just numbers — but shared and checked by thousands of computers instead of one bank.",
            ),
            QuizQuestion(
                "What makes crypto different from the numbers in your regular bank account?",
                listOf("No single bank controls it — many computers share the same record", "It's exactly the same as a bank account", "You can only ever own one coin", "It's only used for video games"),
                correctIndex = 0,
                explanation = "A regular bank account is one bank's private ledger. Crypto is shared across many computers with no single owner.",
            ),
        ),
        practiceHint = "Open Trade and just scroll the list of 100 real coins for a minute — that's the \"notebook\" this lesson is talking about.",
    ),
    Lesson(
        title = "What actually moves a crypto price",
        level = "Beginner", duration = "3 min", tag = "Basics",
        teaser = "Why does the price go up and down all day?",
        body = "Imagine a lemonade stand. If lots of people want lemonade and there's only a little left, you can " +
            "charge more per cup. If nobody wants any, you have to drop the price just to sell some. Coins work the " +
            "same way: when more people want to buy than sell, the price goes up. When more people want to sell than " +
            "buy, it goes down.\n\n" +
            "A few things make people want to buy or sell more of a coin, all at once:\n" +
            "• Big news about the coin (good or bad)\n" +
            "• Other people getting excited or scared, which spreads fast online\n" +
            "• New rules governments make about crypto\n" +
            "• How the whole economy is doing\n\n" +
            "There's no magic to it — it's just millions of tiny lemonade-stand decisions happening at once, all day.",
        quiz = listOf(
            QuizQuestion(
                "If lots of people want to buy a coin and only a few want to sell it, what usually happens to the price?",
                listOf("It goes up", "It goes down", "It stays exactly the same", "Nobody can ever know"),
                correctIndex = 0,
                explanation = "More buyers than sellers pushes the price up — just like a lemonade stand on a hot day.",
            ),
            QuizQuestion(
                "Which of these can make a coin's price move?",
                listOf("Big news about the coin", "What people had for breakfast", "The weather in a random country", "None of the above"),
                correctIndex = 0,
                explanation = "News, rumors, and how people feel about a coin can move its price fast — breakfast can't.",
            ),
        ),
        practiceHint = "Open Overview and look at the 'Best mover' card — that's a real coin moving the most right now.",
    ),
    Lesson(
        title = "Buying a little at a time (dollar-cost averaging)",
        level = "Beginner", duration = "3 min", tag = "Strategy",
        teaser = "Why buying a little bit at a time can beat buying all at once",
        body = "Say you get \$20 of pocket money every week and you want to buy a coin with it. You could save it all " +
            "up and spend it in one go — but what if that exact day the price happens to be really high?\n\n" +
            "\"Dollar-cost averaging\" just means: buy a small, fixed amount on a schedule, like every week, instead " +
            "of spending everything at once. Some weeks you'll buy when the price is high, some weeks when it's low " +
            "— but over time it evens out, so you're not betting everything on one lucky or unlucky day.\n\n" +
            "It won't always make you the most money possible. But it takes away the stress of trying to guess the " +
            "\"perfect\" day to buy, which nobody can really do anyway — not even the experts.",
        quiz = listOf(
            QuizQuestion(
                "What does \"dollar-cost averaging\" mean?",
                listOf("Buying the same amount on a regular schedule", "Buying only at the lowest price ever", "Never buying more than once", "Selling everything at once"),
                correctIndex = 0,
                explanation = "It's spreading your buys out over time on a schedule, instead of trying to time one perfect moment.",
            ),
            QuizQuestion(
                "Why might this feel less stressful than buying all at once?",
                listOf("You don't have to guess the perfect day to buy", "It guarantees you'll make more money", "It's the only legal way to buy crypto", "It makes prices go up"),
                correctIndex = 0,
                explanation = "Spreading it out means one bad entry price can't hurt your whole plan.",
            ),
        ),
        practiceHint = "In Trade, buy a small amount of a coin today. Check the price again in a few days before you buy more.",
    ),
    Lesson(
        title = "Don't put all your eggs in one basket",
        level = "Beginner", duration = "3 min", tag = "Risk",
        teaser = "Why spreading your money around protects you",
        body = "You've probably heard \"don't put all your eggs in one basket.\" Investing works exactly the same way. " +
            "If you put ALL your money into one coin and something bad happens to it, your whole portfolio has a bad day.\n\n" +
            "But if you split your money across a few different coins — ones that don't all react to the same news " +
            "in the same way — a bad day for one can be balanced out by an okay (or good) day for another.\n\n" +
            "This is called diversification. It doesn't mean you'll never lose money. It just means one bad pick " +
            "can't wipe out everything you have.",
        quiz = listOf(
            QuizQuestion(
                "What's the main idea behind diversification?",
                listOf("Spreading your money across different things so one bad pick doesn't ruin everything", "Buying only the most popular coin", "Checking prices every 5 minutes", "Never selling anything, ever"),
                correctIndex = 0,
                explanation = "Spreading risk out means no single mistake can hurt you too badly.",
            ),
            QuizQuestion(
                "If you own 5 coins that all move up and down together (same news moves all of them the same way), are you well diversified?",
                listOf("Not really — they're too similar to each other", "Yes, because you own 5 different coins", "It doesn't matter how many you own", "Diversification is only about the number of coins"),
                correctIndex = 0,
                explanation = "True diversification needs things that react differently, not just a bigger pile of similar things.",
            ),
        ),
        practiceHint = "In Trade, try buying small amounts of 2 different coins instead of putting everything into one.",
    ),
    Lesson(
        title = "A bumpy price isn't the same as a dangerous one",
        level = "Beginner", duration = "3 min", tag = "Risk",
        teaser = "Volatility and risk sound the same but aren't",
        body = "\"Volatility\" just means a price jumps around a lot — up, down, up again. \"Risk\" means the chance " +
            "you lose your money forever and it never comes back.\n\n" +
            "Think of a rollercoaster: it's bumpy (that's volatility), but it's built to bring you safely back to the " +
            "start every time. Now think of a bridge that looks calm and still — but is quietly about to collapse. " +
            "That's risk, and it doesn't need any bumpiness to be dangerous.\n\n" +
            "A coin's price can jump around wildly and still be totally fine in the long run. What actually matters " +
            "is: could this thing fail completely and never come back? That's a very different question from " +
            "\"is the price moving a lot today.\"",
        quiz = listOf(
            QuizQuestion(
                "What does \"volatility\" mean?",
                listOf("The price moves up and down a lot", "You will definitely lose money", "The coin is guaranteed to fail", "The coin's price never changes"),
                correctIndex = 0,
                explanation = "Volatility is just about how much the price wiggles — not whether you'll actually lose money.",
            ),
            QuizQuestion(
                "Can something be volatile (bumpy price) without being risky (likely to fail forever)?",
                listOf("Yes — they're different things", "No — bumpy always means dangerous", "Only true for stocks, never crypto", "Volatility and risk mean the same thing"),
                correctIndex = 0,
                explanation = "A bumpy ride doesn't mean the destination is bad. That's the whole point of this lesson.",
            ),
        ),
    ),
    Lesson(
        title = "Why a 1-cent coin isn't automatically \"cheap\"",
        level = "Beginner", duration = "3 min", tag = "Basics",
        teaser = "Price tag vs. how big a coin actually is",
        body = "\"Market cap\" = the price of one coin × how many coins exist in total. It tells you how big something " +
            "actually is, not just what its price tag says.\n\n" +
            "Imagine one pizza cut into 4 big slices, and an identical pizza cut into 100 tiny slices. The second " +
            "pizza's slices look \"cheaper\" one at a time — but it's the exact same size pizza! A coin priced at " +
            "\$0.001 with billions of coins can be worth just as much (or more) than a coin priced at \$60,000 with " +
            "only a few million coins.\n\n" +
            "So don't judge a coin by its sticker price alone. Market cap tells you its real size.",
        quiz = listOf(
            QuizQuestion(
                "If Coin A costs \$0.01 and Coin B costs \$50,000, which one is automatically \"cheaper\" to invest in?",
                listOf("Neither — you have to check market cap, not just the price", "Coin A, because the number looks smaller", "Coin B, because it's more expensive", "They're always exactly the same"),
                correctIndex = 0,
                explanation = "Price alone doesn't tell you size — market cap (price × total coins) does.",
            ),
            QuizQuestion(
                "What is market cap?",
                listOf("Price per coin multiplied by the total number of coins", "The highest price a coin has ever hit", "How many people own the coin", "How old the coin is"),
                correctIndex = 0,
                explanation = "It's a simple multiplication: price × total supply.",
            ),
        ),
        practiceHint = "In Trade, compare two coins' prices and see how different their market caps are on Overview's movers.",
    ),
    Lesson(
        title = "Reading the wiggly line on the price chart",
        level = "Beginner", duration = "3 min", tag = "Basics",
        teaser = "What that chart on the asset screen is actually telling you",
        body = "The chart is just a picture of where the price has been, read left (the past) to right (right now). " +
            "If the line is climbing, the price has been going up. If it's dropping, the price has been going down.\n\n" +
            "Don't panic over one tiny wiggle — look at the overall shape first. A line that's mostly climbing with a " +
            "couple of small dips tells a very different story than one that spikes way up and then crashes straight " +
            "back down. The second one is more of a warning sign than the first.\n\n" +
            "Real trading apps often use fancier \"candlestick\" charts, which show even more detail (the highest and " +
            "lowest price each day, not just where it ended). Same idea though: read the overall trend, don't panic " +
            "on every wiggle.",
        quiz = listOf(
            QuizQuestion(
                "On the price chart, which side shows the past?",
                listOf("The left side", "The right side", "The top", "The bottom"),
                correctIndex = 0,
                explanation = "Charts read left to right, so the left side is older and the right side is \"now.\"",
            ),
            QuizQuestion(
                "What matters more when reading a chart?",
                listOf("The overall trend, not one tiny wiggle", "Only the very last data point", "The color of the line", "Nothing — charts are random"),
                correctIndex = 0,
                explanation = "One small wiggle rarely tells you much — the bigger shape matters more.",
            ),
        ),
        practiceHint = "Open any coin's Asset page and look at its 7-day chart before you decide whether to trade it.",
    ),
    Lesson(
        title = "Crypto words, explained plainly",
        level = "Beginner", duration = "4 min", tag = "Glossary",
        teaser = "Blockchain, wallet, exchange, and the rest, in plain English",
        body = "Blockchain: think of a shared notebook that thousands of computers all keep an identical copy of. " +
            "Every time someone makes a trade, every notebook updates at the same time, so nobody can secretly " +
            "change the numbers.\n\n" +
            "Wallet: like a locked box that holds your coins. You need a special key to open it — lose the key and " +
            "nobody can get it back for you. (This app is simulated, so none of that risk applies here — but it's " +
            "the single most important real-world crypto idea to understand.)\n\n" +
            "Exchange: a shop where people swap coins for other coins, or for real money.\n\n" +
            "Market order: \"buy or sell right now, at whatever the price is this second.\" This is the only kind of " +
            "order this app lets you place.\n\n" +
            "Limit order: \"only buy or sell if the price reaches an exact number I pick.\" Real exchanges have these; " +
            "this simulator doesn't, since there's no real order book behind it.\n\n" +
            "Stablecoin: a coin (like Tether or USDC, both in this app's market list) built to always be worth about " +
            "\$1, instead of jumping around like most coins do.",
        quiz = listOf(
            QuizQuestion(
                "What's a wallet, in simple terms?",
                listOf("A locked box that holds your coins, opened with a special key", "A place to buy snacks", "A type of coin", "A chart"),
                correctIndex = 0,
                explanation = "It's just where your coins \"live\" — protected by a key only you should ever have.",
            ),
            QuizQuestion(
                "What does a \"market order\" mean?",
                listOf("Buy or sell right now at the current price", "Buy only if the price hits an exact number you pick", "A type of coin", "A chart pattern"),
                correctIndex = 0,
                explanation = "Market orders happen immediately, at whatever price is available right now.",
            ),
        ),
    ),
    Lesson(
        title = "How much of your money to risk on one coin",
        level = "Intermediate", duration = "4 min", tag = "Risk",
        teaser = "Sharing cookies teaches you this one",
        body = "Imagine you have 10 cookies to share between friends. If you give one friend all 10 cookies and they " +
            "drop them, you have zero cookies left. If you spread them 2 per friend across 5 friends, one dropped " +
            "batch only costs you 2 cookies.\n\n" +
            "\"Position sizing\" just means deciding, before you buy anything, how big a slice of your total money " +
            "you're willing to put into ONE coin. A good habit: pick a number (like \"no more than 20% of my money in " +
            "any single coin\") and stick to it — even when that coin is doing really well and tempts you to add more.\n\n" +
            "How much is \"safe\" depends on you. If a big price swing would stress you out, keep your slices smaller " +
            "while you're still learning.",
        quiz = listOf(
            QuizQuestion(
                "What is \"position sizing\"?",
                listOf("Deciding how much of your money goes into one single coin", "The physical size of the coin", "How fast you can sell", "The number of coins that exist"),
                correctIndex = 0,
                explanation = "It's about how big a slice of your total money you risk on one thing.",
            ),
            QuizQuestion(
                "Why decide your position size BEFORE buying, not after?",
                listOf("So excitement about a coin doing well doesn't push you to risk more than you meant to", "It doesn't matter when you decide", "Prices are only real before you buy", "This only matters for advanced traders"),
                correctIndex = 0,
                explanation = "Deciding ahead of time stops your emotions from changing the plan later, when it matters most.",
            ),
        ),
        practiceHint = "Before your next buy in Trade, decide out loud how much of your \$10,000 you're comfortable risking on it.",
    ),
    Lesson(
        title = "Don't chase hype or panic at drops",
        level = "Intermediate", duration = "4 min", tag = "Psychology",
        teaser = "FOMO and panic selling are the same trap, in opposite directions",
        body = "FOMO (fear of missing out) is buying something just because it's shooting up and everyone's talking " +
            "about it. Panic selling is dumping something just because it's dropping and everyone's scared.\n\n" +
            "Here's the trick: both are the SAME mistake, just pointed in opposite directions. You end up buying near " +
            "the top (right before it often drops) and selling near the bottom (right before it often recovers) — " +
            "the worst possible timing, both times.\n\n" +
            "A simple fix: decide WHY you're holding something before you're emotionally caught up in the moment. " +
            "Then, when you feel the urge to buy or sell because of a headline, ask yourself: \"does this actually " +
            "change my original reason for holding this?\" If not, it's probably just noise.",
        quiz = listOf(
            QuizQuestion(
                "What is FOMO, in investing?",
                listOf("Buying something just because it's suddenly popular and rising fast", "A type of coin", "Selling at the perfect time", "A chart pattern"),
                correctIndex = 0,
                explanation = "FOMO means chasing excitement instead of following your own plan.",
            ),
            QuizQuestion(
                "Why are FOMO buying and panic selling considered \"the same mistake\"?",
                listOf("Both let short-term emotion override your actual plan", "They're actually opposite and unrelated", "Only FOMO is a real mistake", "Panic selling is always the correct move"),
                correctIndex = 0,
                explanation = "Both replace a real plan with a reaction to the mood of the moment.",
            ),
        ),
    ),
    Lesson(
        title = "How this simulator actually works",
        level = "Beginner", duration = "3 min", tag = "Using the app",
        teaser = "Your \$10,000 pretend cash, and real prices",
        body = "Every account starts with \$10,000 of pretend money — separate from your Star Coins wallet in Bank. " +
            "The prices you see for every coin are REAL, live prices pulled from an actual crypto price service. " +
            "Only the money is fake.\n\n" +
            "When you tap Buy in Trade, you're buying at the real current price, but with your pretend \$10,000 " +
            "instead of real money. When you sell, you get pretend cash back based on the real current price too. " +
            "Every trade shows up in your order history, so you can look back and see exactly what you did, when, " +
            "and at what price — good practice for what it's actually like to invest for real, with zero real risk.",
        quiz = listOf(
            QuizQuestion(
                "Is the money in this simulator real?",
                listOf("No — it's pretend money, but prices are real", "Yes, it's real money", "Only the losses are real", "Only on weekends"),
                correctIndex = 0,
                explanation = "The cash is 100% pretend. The prices are 100% real.",
            ),
            QuizQuestion(
                "Where can you see everything you've bought and sold?",
                listOf("Your order history in Trade", "Nowhere — it isn't tracked", "Only in Watchlist", "Only in Learn"),
                correctIndex = 0,
                explanation = "Every buy and sell you make gets logged so you can review it later.",
            ),
        ),
        practiceHint = "Go make your very first trade — even \$5 of any coin — then come back and check your order history.",
    ),
)

@Composable
private fun InvestLearnBody(onOpenTrade: () -> Unit, viewModel: AuthViewModel) {
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val storage = viewModel.storage
    val scope = rememberCoroutineScope()
    val completed = remember {
        mutableStateMapOf<String, Boolean>().apply { LESSONS.forEach { put(it.title, storage.isLessonCompleted(it.title)) } }
    }
    val quizScores = remember {
        mutableStateMapOf<String, Int>().apply { LESSONS.forEach { l -> storage.getQuizScorePercent(l.title)?.let { put(l.title, it) } } }
    }

    // Awards Profile's rank exp server-side - only on the transition to
    // done, never on un-marking, and the server itself is idempotent per
    // lesson (see LessonService) so toggling a lesson off and back on never
    // double-awards even if this fires more than once.
    fun onToggle(title: String, done: Boolean) {
        completed[title] = done
        storage.setLessonCompleted(title, done)
        if (done) scope.launch { viewModel.completeLesson(title) }
    }

    val index = selectedIndex
    if (index != null) {
        val lesson = LESSONS[index]
        InvestLessonDetailBody(
            lesson = lesson,
            onBack = { selectedIndex = null },
            onOpenTrade = onOpenTrade,
            completed = completed[lesson.title] == true,
            onToggleCompleted = { done -> onToggle(lesson.title, done) },
            onQuizScored = { percent ->
                quizScores[lesson.title] = percent
                storage.setQuizScorePercent(lesson.title, percent)
            },
        )
    } else {
        InvestLearnListBody(
            completed = completed,
            quizScores = quizScores,
            onOpenLesson = { selectedIndex = it },
            onToggleCompleted = { title, done -> onToggle(title, done) },
            viewModel = viewModel,
        )
    }
}

@Composable
private fun InvestLearnListBody(
    completed: Map<String, Boolean>,
    quizScores: Map<String, Int>,
    onOpenLesson: (Int) -> Unit,
    onToggleCompleted: (String, Boolean) -> Unit,
    viewModel: AuthViewModel,
) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("LEARN", color = CedalColors.TextSecondary, fontSize = 11.sp, letterSpacing = 2.sp)
        Text(
            "Simple explanations with real examples. Tap a lesson to read it, tick the circle once you've got it.",
            color = CedalColors.TextMuted, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp, bottom = 10.dp),
        )

        DailyTaskCard(area = "invest", viewModel = viewModel)

        val grouped = LESSONS.withIndex().groupBy { it.value.level }
        val orderedLevels = LEVEL_ORDER.filter { grouped.containsKey(it) } + grouped.keys.filter { it !in LEVEL_ORDER }

        orderedLevels.forEach { level ->
            val items = grouped[level].orEmpty()
            val doneCount = items.count { completed[it.value.title] == true }
            val groupPercent = if (items.isNotEmpty()) doneCount * 100 / items.size else 0

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 10.dp, bottom = 8.dp)) {
                Text(level.uppercase(), color = CedalColors.TextSecondary, fontSize = 11.sp, letterSpacing = 2.sp, modifier = Modifier.weight(1f))
                Text(
                    "$groupPercent% done",
                    color = if (groupPercent == 100) CedalColors.Success else CedalColors.TextMuted,
                    fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                )
            }

            items.forEach { (index, lesson) ->
                val isDone = completed[lesson.title] == true
                val score = quizScores[lesson.title]
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(CedalColors.CardBackground)
                        .border(1.dp, if (isDone) CedalColors.Success.copy(alpha = 0.5f) else CedalColors.BorderSlate, RoundedCornerShape(14.dp))
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = { onOpenLesson(index) })
                        .padding(14.dp),
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(if (isDone) CedalColors.Success else CedalColors.Background)
                            .border(1.dp, if (isDone) CedalColors.Success else CedalColors.BorderSlate, CircleShape)
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = { onToggleCompleted(lesson.title, !isDone) }),
                    ) {
                        if (isDone) Text("✓", color = CedalColors.Background, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                        Text(lesson.title, color = CedalColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text(lesson.teaser, color = CedalColors.TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp, bottom = 8.dp))
                        Row {
                            LessonPill(lesson.duration)
                            LessonPill(lesson.tag)
                            LessonPill("+$INVEST_LESSON_EXP EXP")
                            if (score != null) LessonPill("Quiz $score%")
                        }
                    }
                    Text("›", color = CedalColors.TextSecondary, fontSize = 20.sp, modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }
}

@Composable
private fun InvestLessonDetailBody(
    lesson: Lesson,
    onBack: () -> Unit,
    onOpenTrade: () -> Unit,
    completed: Boolean,
    onToggleCompleted: (Boolean) -> Unit,
    onQuizScored: (Int) -> Unit,
) {
    val answers = remember(lesson.title) { mutableStateMapOf<Int, Int>() }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        MemberBackBar(title = lesson.title, onBack = onBack)
        Row(modifier = Modifier.padding(bottom = 12.dp)) {
            LessonPill(lesson.level)
            LessonPill(lesson.duration)
            LessonPill(lesson.tag)
            LessonPill("+$INVEST_LESSON_EXP EXP")
        }
        Text(lesson.body, color = CedalColors.TextSecondary, fontSize = 13.sp, lineHeight = 19.sp)

        lesson.practiceHint?.let { hint ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 18.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(CedalColors.CardBackground)
                    .border(1.dp, CedalColors.BorderCyan, RoundedCornerShape(14.dp))
                    .padding(14.dp),
            ) {
                Text("PRACTICE THIS FOR REAL", color = CedalColors.AccentCyan, fontSize = 11.sp, letterSpacing = 1.sp, modifier = Modifier.padding(bottom = 6.dp))
                Text(hint, color = CedalColors.TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(bottom = 10.dp))
                CedalGhostButton(text = "OPEN TRADE", onClick = onOpenTrade)
            }
        }

        if (lesson.quiz.isNotEmpty()) {
            Text(
                "TRY IT — QUICK CHECK",
                color = CedalColors.TextSecondary, fontSize = 11.sp, letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 20.dp, bottom = 8.dp),
            )
            lesson.quiz.forEachIndexed { qIndex, question ->
                QuizQuestionCard(
                    question = question,
                    index = qIndex,
                    selected = answers[qIndex],
                    onAnswer = { optIndex ->
                        answers[qIndex] = optIndex
                        if (answers.size == lesson.quiz.size) {
                            val correctCount = lesson.quiz.indices.count { answers[it] == lesson.quiz[it].correctIndex }
                            onQuizScored(correctCount * 100 / lesson.quiz.size)
                        }
                    },
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(CedalColors.CardBackground)
                .border(1.dp, if (completed) CedalColors.Success.copy(alpha = 0.5f) else CedalColors.BorderSlate, RoundedCornerShape(14.dp))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = { onToggleCompleted(!completed) })
                .padding(14.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(if (completed) CedalColors.Success else CedalColors.Background)
                    .border(1.dp, if (completed) CedalColors.Success else CedalColors.BorderSlate, CircleShape),
            ) {
                if (completed) Text("✓", color = CedalColors.Background, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Text(
                if (completed) "Marked as done" else "Mark this lesson as done",
                color = CedalColors.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 12.dp),
            )
        }
    }
}

@Composable
private fun QuizQuestionCard(question: QuizQuestion, index: Int, selected: Int?, onAnswer: (Int) -> Unit) {
    val answered = selected != null

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(CedalColors.CardBackground)
            .border(1.dp, CedalColors.BorderSlate, RoundedCornerShape(14.dp))
            .padding(14.dp),
    ) {
        Text(
            "${index + 1}. ${question.question}",
            color = CedalColors.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 10.dp),
        )
        question.options.forEachIndexed { optIndex, option ->
            val isCorrect = optIndex == question.correctIndex
            val isSelected = selected == optIndex
            val borderColor = when {
                !answered -> CedalColors.BorderSlate
                isCorrect -> CedalColors.Success
                isSelected -> CedalColors.Error
                else -> CedalColors.BorderSlate
            }
            val bgColor = when {
                !answered -> CedalColors.Background
                isCorrect -> CedalColors.Success.copy(alpha = 0.1f)
                isSelected -> CedalColors.Error.copy(alpha = 0.1f)
                else -> CedalColors.Background
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(bgColor)
                    .border(1.dp, borderColor, RoundedCornerShape(50))
                    .clickable(enabled = !answered, interactionSource = remember { MutableInteractionSource() }, indication = null) { onAnswer(optIndex) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Text(option, color = CedalColors.TextPrimary, fontSize = 12.sp, modifier = Modifier.weight(1f))
                if (answered && isCorrect) Text("✓", color = CedalColors.Success, fontSize = 14.sp)
                if (answered && isSelected && !isCorrect) Text("✗", color = CedalColors.Error, fontSize = 14.sp)
            }
        }
        if (answered) {
            Text(question.explanation, color = CedalColors.TextSecondary, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

@Composable
private fun LessonPill(text: String) {
    Box(
        modifier = Modifier
            .padding(end = 6.dp)
            .clip(RoundedCornerShape(50))
            .background(CedalColors.Background)
            .border(1.dp, CedalColors.BorderSlate, RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(text.uppercase(), color = CedalColors.TextMuted, fontSize = 9.sp, letterSpacing = 0.4.sp)
    }
}
