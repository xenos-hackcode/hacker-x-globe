package com.xhacker.cedal.services

import com.xhacker.cedal.db.Users
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

// Every 3 months, both progressions (Shop's xp/tier and Profile's exp/rank)
// decay by 2 tiers/ranks - see RankService.decayedShopTierIndex /
// decayedProfileRankIndex for the exact floors/exemptions. Triggered by a
// Cloud Scheduler job hitting DecayRoutes periodically (daily is plenty,
// since this only actually *acts* on an account once its own 3 months are
// up - see isDecayDue), not a fixed calendar date shared by every account.
object DecayService {
    private val THREE_MONTHS_MILLIS = 90L * 24 * 60 * 60 * 1000

    private fun isDecayDue(createdAt: Long, lastDecayAt: Long?, now: Long): Boolean =
        now - (lastDecayAt ?: createdAt) >= THREE_MONTHS_MILLIS

    private fun decayedXp(currentXp: Long): Long {
        val rank = RankService.shopRank(currentXp)
        if (rank.tierIndex <= 1) return currentXp // F/E exempt, unchanged
        return RankService.pointsAtTierStart(RankService.decayedShopTierIndex(rank.tierIndex))
    }

    private fun decayedExp(currentExp: Long): Long {
        val rank = RankService.profileRank(currentExp)
        if (rank.tierIndex <= 1) return currentExp // Human/Warrior exempt
        return RankService.pointsAtTierStart(RankService.decayedProfileRankIndex(rank.tierIndex))
    }

    // Returns how many accounts actually had decay applied this run.
    fun runDueDecays(): Int = transaction {
        val now = System.currentTimeMillis()
        var affected = 0
        Users.selectAll().forEach { row ->
            val createdAt = row[Users.createdAt]
            val lastDecayAt = row[Users.lastDecayAt]
            if (!isDecayDue(createdAt, lastDecayAt, now)) return@forEach

            val newXp = decayedXp(row[Users.xp])
            val newExp = decayedExp(row[Users.exp])
            Users.update({ Users.id eq row[Users.id] }) {
                it[xp] = newXp
                it[exp] = newExp
                it[Users.lastDecayAt] = now
            }
            affected++
        }
        affected
    }
}
