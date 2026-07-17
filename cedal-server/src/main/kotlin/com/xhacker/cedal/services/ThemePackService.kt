package com.xhacker.cedal.services

import com.xhacker.cedal.db.ThemePackPurchases
import com.xhacker.cedal.models.ThemePackDto
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

// Cosmetic accent-color packs, buyable with Star Coins from the Shop - see
// CedalColors.kt (client) for the actual dark/light color pairs each id
// maps to. The default cyan look is free and not in this catalog at all -
// these are alternates, not the base theme.
object ThemePackService {
    private data class Catalog(val name: String, val priceSC: Int, val accentHex: String)

    private val CATALOG = linkedMapOf(
        "crimson" to Catalog("Crimson Protocol", 500, "#F43F5E"),
        "toxic_green" to Catalog("Toxic Green", 500, "#4ADE80"),
        "violet_static" to Catalog("Violet Static", 750, "#C084FC"),
    )

    fun listPacks(userId: String): List<ThemePackDto> = transaction {
        val uid = UUID.fromString(userId)
        val owned = ThemePackPurchases.selectAll().where { ThemePackPurchases.userId eq uid }
            .map { it[ThemePackPurchases.packId] }.toSet()
        CATALOG.map { (id, pack) -> ThemePackDto(id, pack.name, pack.priceSC, pack.accentHex, owned = id in owned) }
    }

    // Returns the new SC balance - the client re-fetches listPacks() to
    // reflect ownership rather than trusting anything echoed back here.
    fun purchase(userId: String, packId: String): Int = transaction {
        val pack = CATALOG[packId] ?: throw AuthException("Unknown theme pack")
        val uid = UUID.fromString(userId)
        val alreadyOwned = ThemePackPurchases.selectAll()
            .where { (ThemePackPurchases.userId eq uid) and (ThemePackPurchases.packId eq packId) }
            .any()
        if (alreadyOwned) throw AuthException("You already own this theme pack")

        val newBalance = WalletService.spend(userId, pack.priceSC, "Theme pack: ${pack.name}")
        ThemePackPurchases.insert {
            it[ThemePackPurchases.userId] = uid
            it[ThemePackPurchases.packId] = packId
            it[purchasedAt] = System.currentTimeMillis()
        }
        newBalance
    }
}
