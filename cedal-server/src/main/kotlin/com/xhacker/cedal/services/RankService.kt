package com.xhacker.cedal.services

import kotlin.math.max

// The full rank ladder math shared by two separate progressions:
//  - Shop's F-through-M tier (17 tiers), powered by xp (real money purchases)
//  - Profile's Human-through-Godhood rank (10 ranks), powered by exp (lesson
//    completions) - see LessonService.
// Both use the exact same triangular xp curve within a tier/rank (100, 300,
// 600, 1000, 1500, 2100, 2800, 3600, 4500, 5500 cumulative to clear levels
// 1-10, same shape the old VIP system used), reset fresh at the start of
// every tier/rank. Both also decay every 3 months by 2 tiers/ranks, with a
// floor near the bottom - see decayedShopTierIndex/decayedProfileRankIndex.
object RankService {
    const val LEVELS_PER_TIER = 10

    // Cumulative-within-tier points needed to REACH level n (n=1..10).
    private fun levelThreshold(level: Int): Long = 100L * (level - 1) * level / 2

    // The cost to fully clear all 10 levels of one tier/rank and advance to
    // the next one's level 1 - i.e. levelThreshold(11).
    val TIER_TOTAL_POINTS: Long = levelThreshold(LEVELS_PER_TIER + 1)

    data class Rank(val tierIndex: Int, val level: Int) {
        override fun toString() = "tier $tierIndex level $level"
    }

    fun rankForPoints(points: Long, tierCount: Int): Rank {
        var remaining = points
        var tierIndex = 0
        while (tierIndex < tierCount - 1 && remaining >= TIER_TOTAL_POINTS) {
            remaining -= TIER_TOTAL_POINTS
            tierIndex++
        }
        var level = 1
        for (n in LEVELS_PER_TIER downTo 1) {
            if (remaining >= levelThreshold(n)) {
                level = n
                break
            }
        }
        return Rank(tierIndex, level)
    }

    // The points value at which a given tier's level 1 starts - used both to
    // show "xp remaining to next tier" and to compute a decay's landing xp.
    fun pointsAtTierStart(tierIndex: Int): Long = tierIndex * TIER_TOTAL_POINTS

    // --- Shop: F, E, D, C, B, A, AA, AAA, S, SS, SSS, Legend, Mythic, Instinct, EX, L, M ---

    val SHOP_TIER_ORDER = listOf(
        "F", "E", "D", "C", "B",
        "A", "AA", "AAA",
        "S", "SS", "SSS",
        "Legend", "Mythic", "Instinct", "EX", "L", "M",
    )

    fun shopRank(xp: Long): Rank = rankForPoints(xp, SHOP_TIER_ORDER.size)

    fun meetsShopRank(xp: Long, requiredTier: String, requiredLevel: Int): Boolean {
        val r = shopRank(xp)
        val reqIndex = SHOP_TIER_ORDER.indexOf(requiredTier)
        return r.tierIndex > reqIndex || (r.tierIndex == reqIndex && r.level >= requiredLevel)
    }

    // Every 3 months: drop 2 tiers, landing on level 1 of the destination.
    // F and E (indices 0-1) are exempt entirely. D (index 2) would naturally
    // fall all the way to F, but is capped at E instead - decay should never
    // be able to push someone all the way to the very bottom tier.
    fun decayedShopTierIndex(tierIndex: Int): Int =
        if (tierIndex <= 1) tierIndex else max(tierIndex - 2, 1)

    // --- Profile: Human, Warrior, King, Emperor, Ancient, Immortal, Sovereign, Void, Xenos, Godhood ---

    val PROFILE_RANK_ORDER = listOf(
        "Human", "Warrior", "King", "Emperor", "Ancient",
        "Immortal", "Sovereign", "Void", "Xenos", "Godhood",
    )

    fun profileRank(exp: Long): Rank = rankForPoints(exp, PROFILE_RANK_ORDER.size)

    // Only Human and Warrior (indices 0-1) are exempt - unlike Shop, no
    // separate "capped" case is needed since Human is already index 0, the
    // natural floor (nothing exists below it to overshoot into).
    fun decayedProfileRankIndex(rankIndex: Int): Int =
        if (rankIndex <= 1) rankIndex else max(rankIndex - 2, 0)
}
