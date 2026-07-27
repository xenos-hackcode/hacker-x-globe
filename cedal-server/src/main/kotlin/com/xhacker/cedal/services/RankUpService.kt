package com.xhacker.cedal.services

import java.util.UUID

// Detects a Profile rank change (RankService.PROFILE_RANK_ORDER) whenever a
// user's exp increases, and queues the right in-app popup - called from
// every Users.exp mutation site (LessonService, ArcOpsService,
// DailyTaskService; DecayService only ever decreases exp so this is a
// harmless no-op there, but it still routes through here for consistency).
//
// A "small" rank-up (level-up within the same named rank, e.g. Human 1 ->
// Human 2) just queues a one-off "Congrats" popup. A "big" rank-up (crossing
// into the next named league, e.g. Human -> Warrior) queues the balloon
// popup AND unlocks a per-rank achievement (AchievementService.rankUpDef) -
// unlocking is idempotent per rank name, so re-crossing the same threshold
// (shouldn't normally happen, but decay could theoretically drop and a later
// re-climb could cross it again) never re-fires it.
object RankUpService {
    fun checkRankUp(userId: UUID, oldExp: Long, newExp: Long) {
        if (newExp <= oldExp) return
        val oldRank = RankService.profileRank(oldExp)
        val newRank = RankService.profileRank(newExp)
        if (newRank.tierIndex == oldRank.tierIndex && newRank.level == oldRank.level) return

        val rankName = RankService.PROFILE_RANK_ORDER[newRank.tierIndex]
        if (newRank.tierIndex > oldRank.tierIndex) {
            val def = AchievementService.rankUpDef(rankName)
            AchievementService.unlock(userId, def)
            PendingPopupService.enqueue(
                userId = userId,
                kind = "rank_up_big",
                title = def.title,
                bigWord = def.bigWord,
                body = "Can't believe am growing! ${def.body}",
            )
        } else {
            PendingPopupService.enqueue(
                userId = userId,
                kind = "rank_up",
                title = "Congrats on reaching $rankName ${newRank.level}",
                bigWord = null,
                body = "You've reached $rankName level ${newRank.level}.",
            )
        }
    }
}
