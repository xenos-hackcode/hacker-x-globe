package com.xhacker.cedal.services

import com.xhacker.cedal.models.MarketAssetDetail
import com.xhacker.cedal.models.MarketAssetSummary
import com.xhacker.cedal.models.PricePoint
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// Replaces cedal-mobile's Overview/Watchlist/Trade/Asset screens, which were
// 100% hardcoded mock numbers (own comment: "later: pull this from
// Firestore / financeApi"). This pulls real crypto prices from CoinGecko's
// free public API — no key or signup needed. Stocks aren't wired up yet
// (would need a keyed provider like Finnhub/Alpha Vantage); assetType on
// the DB tables already leaves room for that later.
object MarketDataService {
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    }
    private const val BASE_URL = "https://api.coingecko.com/api/v3"

    // Top N by market cap instead of a small curated list — a real
    // simulator lets you trade from a broad universe, not 10 hand-picked
    // coins. This is still a single CoinGecko call regardless of page size,
    // so it doesn't cost extra against the free tier's per-minute limit.
    private const val MARKET_PAGE_SIZE = 100

    // Simple in-memory TTL cache — several users/screens can poll this
    // within seconds of each other, and CoinGecko's free tier is rate-limited.
    private var marketsCache: Pair<Long, List<CoinGeckoMarketItem>>? = null
    private const val CACHE_TTL_MS = 60_000L

    suspend fun listMarkets(): List<MarketAssetSummary> = fetchMarketsCached().map { it.toSummary() }

    suspend fun getPrice(coinId: String): Double {
        val cached = fetchMarketsCached().firstOrNull { it.id == coinId }
        if (cached?.currentPrice != null) return cached.currentPrice

        val response: Map<String, Map<String, Double>> = client.get("$BASE_URL/simple/price") {
            parameter("ids", coinId)
            parameter("vs_currencies", "usd")
        }.body()
        return response[coinId]?.get("usd") ?: throw AuthException("Unknown asset: $coinId")
    }

    suspend fun getDetail(coinId: String): MarketAssetDetail {
        val summary = fetchMarketsCached().firstOrNull { it.id == coinId }?.toSummary()
            ?: throw AuthException("Unknown asset: $coinId")
        val chart: CoinGeckoMarketChart = client.get("$BASE_URL/coins/$coinId/market_chart") {
            parameter("vs_currency", "usd")
            parameter("days", "7")
        }.body()
        val history = chart.prices.map { PricePoint(timestamp = it[0].toLong(), price = it[1]) }
        return MarketAssetDetail(summary.id, summary.symbol, summary.name, summary.price, summary.changePercent24h, history)
    }

    private suspend fun fetchMarketsCached(): List<CoinGeckoMarketItem> {
        val now = System.currentTimeMillis()
        val cached = marketsCache
        if (cached != null && now - cached.first < CACHE_TTL_MS) return cached.second

        val fetched: List<CoinGeckoMarketItem> = client.get("$BASE_URL/coins/markets") {
            parameter("vs_currency", "usd")
            parameter("order", "market_cap_desc")
            parameter("per_page", MARKET_PAGE_SIZE)
            parameter("page", 1)
            parameter("price_change_percentage", "24h")
        }.body()
        marketsCache = now to fetched
        return fetched
    }

    private fun CoinGeckoMarketItem.toSummary() = MarketAssetSummary(
        id = id, symbol = symbol.uppercase(), name = name,
        price = currentPrice ?: 0.0, changePercent24h = priceChangePercentage24h ?: 0.0,
    )
}

@Serializable
private data class CoinGeckoMarketItem(
    val id: String,
    val symbol: String,
    val name: String,
    @SerialName("current_price") val currentPrice: Double? = null,
    @SerialName("price_change_percentage_24h") val priceChangePercentage24h: Double? = null,
)

@Serializable
private data class CoinGeckoMarketChart(
    val prices: List<List<Double>>,
)
