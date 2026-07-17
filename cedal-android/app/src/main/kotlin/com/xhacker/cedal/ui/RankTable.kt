package com.xhacker.cedal.ui

// Mirrors cedal-server's RankService exactly - keep in lockstep if the curve
// or tier/rank lists ever change server-side. Two separate ladders share
// this same triangular-curve math (100, 300, 600, 1000, ... 5500 cumulative
// points to clear levels 1-10 of a tier, reset fresh each tier):
//  - Shop's F-M tier, powered by xp (real money purchases)
//  - Profile's Human-Godhood rank, powered by exp (lesson completions)
object RankTable {
    const val LEVELS_PER_TIER = 10

    // Cumulative-within-tier points needed to REACH level n (n=1..10) -
    // public since the UI needs it to show "xp remaining to next level".
    fun levelThreshold(level: Int): Long = 100L * (level - 1) * level / 2

    // Cost to fully clear all 10 levels of one tier/rank and advance to the
    // next one's level 1.
    val TIER_TOTAL_POINTS: Long = levelThreshold(LEVELS_PER_TIER + 1)

    data class Rank(val tierIndex: Int, val level: Int)

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

    fun pointsAtTierStart(tierIndex: Int): Long = tierIndex * TIER_TOTAL_POINTS

    fun isMaxRank(rank: Rank, tierCount: Int): Boolean = rank.tierIndex >= tierCount - 1 && rank.level >= LEVELS_PER_TIER

    val SHOP_TIER_ORDER = listOf(
        "F", "E", "D", "C", "B",
        "A", "AA", "AAA",
        "S", "SS", "SSS",
        "Legend", "Mythic", "Instinct", "EX", "L", "M",
    )

    val PROFILE_RANK_ORDER = listOf(
        "Human", "Warrior", "King", "Emperor", "Ancient",
        "Immortal", "Sovereign", "Void", "Xenos", "Godhood",
    )
}
