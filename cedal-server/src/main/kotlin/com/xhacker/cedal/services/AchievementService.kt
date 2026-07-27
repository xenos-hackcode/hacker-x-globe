package com.xhacker.cedal.services

import com.xhacker.cedal.db.UserAchievements
import com.xhacker.cedal.models.AchievementDto
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.util.UUID

// Chat list > More > Achievements. A fixed, extensible catalog of one-time
// unlocks - each fires an in-app popup (never a system notification, per
// spec) the first and only time its key unlocks for a user, tracked in
// UserAchievements. More keys get added here as new achievements are
// specified; unlocking an unrecognized key is a silent no-op (defensive,
// shouldn't happen since every call site names a real CATALOG key).
object AchievementService {
    data class Def(val key: String, val title: String, val bigWord: String, val body: String)

    private val CATALOG: Map<String, Def> = listOf(
        Def("welcome", "Welcome to ur world", "Welcome", "You just stepped into Cedal for the first time."),
        Def("first_friend", "Wow, my first friend", "Friendship", "You made your very first friend on Cedal."),
        Def("blocked_first", "Guess u been blocked", "Blocked", "Someone blocked you for the first time."),
        Def("block_first", "Wow, there's a first for everyone", "Blocker", "You blocked someone for the first time."),
        Def("block_20", "People just know how to get on my nerve", "Nerve", "You've blocked 20 people."),
        Def("blocked_20", "And I thought I would start things great", "Saddened", "You've been blocked by 20 people."),
        // Coding/AI/security - unlock() calls added this batch.
        Def("first_code", "Wow, my first code", "Beginner", "You created your first file in Code."),
        Def("first_corneal_chat", "Wow, my first AI conversation", "Curious", "You sent your first message to Corneal."),
        Def("first_arc_chat", "Wow, my first ARC conversation", "Learner", "You sent your first message to ARC's Assistant."),
        // Settings > Security > Popularity set to fully private (see
        // checkPrivate).
        Def("private", "I am known by no one, but I know all", "Private", "You set your profile to maximum privacy."),
        // Two-factor + passcode + a verified phone number, all on (see
        // checkSecure).
        Def("secure", "No one can get me", "Secure", "You locked your account down with two-factor, a passcode, and a verified phone number."),
        // Meta - auto-unlocked once BOTH "private" and "secure" are held
        // (see checkSecureAndPrivateCombo).
        Def("secure_and_private", "Secure and private, I am the better", "Elite", "You unlocked both Secure and Private."),
        // Meta-achievement - auto-unlocked by unlock() the first time ANY
        // other key unlocks for a user, never called directly by other code.
        Def("first_achievement", "YES let go", "First!", "Your very first achievement on Cedal."),
    ).associateBy { it.key }

    // Achievement keys a signed-in user can trigger themselves via POST
    // /users/{id}/achievements/trigger (see UserRoutes) - deliberately a
    // narrow allowlist of harmless "first time doing X" self-attested
    // badges, not every CATALOG key (block/blocked-20, rank-ups, etc. stay
    // server-computed only).
    val CLIENT_TRIGGERABLE = setOf("first_code", "first_corneal_chat", "first_arc_chat")

    // Big rank-ups (crossing into a new named league, e.g. Human -> Warrior)
    // get their own achievement per rank name, generated on demand rather
    // than hand-listed in CATALOG since RankService.PROFILE_RANK_ORDER is
    // the source of truth for rank names. Each league past Warrior gets its
    // own distinct title (the app owner's own words) - "ain't a noob
    // anymore" is Warrior-specific now, not reused for every later league.
    private val RANK_UP_TITLES = mapOf(
        "Warrior" to "Well, I guess you ain't a noob anymore",
        "King" to "I finally made it",
        "Emperor" to "I dominate",
        "Ancient" to "I see through it all",
        "Immortal" to "Thou hast lived till ages",
        "Sovereign" to "I rule over all",
        "Void" to "Before time, I was",
        "Xenos" to "Man of technology",
        "Godhood" to "God above all",
    )

    fun rankUpDef(rankName: String): Def = Def(
        key = "rank_up_${rankName.lowercase()}",
        title = RANK_UP_TITLES[rankName] ?: "Well, I guess you ain't a noob anymore",
        bigWord = rankName,
        body = "You've climbed all the way into $rankName.",
    )

    fun unlock(userId: UUID, key: String) {
        val def = CATALOG[key] ?: return
        unlock(userId, def)
    }

    fun unlock(userId: UUID, def: Def): Unit = transaction {
        val already = UserAchievements.selectAll()
            .where { (UserAchievements.userId eq userId) and (UserAchievements.key eq def.key) }
            .any()
        if (already) return@transaction

        UserAchievements.insert {
            it[UserAchievements.userId] = userId
            it[key] = def.key
            it[title] = def.title
            it[bigWord] = def.bigWord
            it[body] = def.body
            it[unlockedAt] = System.currentTimeMillis()
        }
        PendingPopupService.enqueue(userId, "achievement", def.title, def.bigWord, def.body)

        if (def.key != "first_achievement") {
            val totalUnlocked = UserAchievements.selectAll().where { UserAchievements.userId eq userId }.count()
            if (totalUnlocked == 1L) {
                unlock(userId, CATALOG.getValue("first_achievement"))
            }
        }
    }

    // Every achievement that COULD exist (fixed CATALOG + one rank-up entry
    // per named league past Human, since Human is the starting rank, never
    // something you "achieve") - locked ones show unlocked=false/no
    // unlockedAt, so the Achievements screen can render the whole catalog
    // dimmed, glowing only the ones this user actually has.
    fun listAll(userId: String): List<AchievementDto> = transaction {
        val uid = UUID.fromString(userId)
        val unlockedByKey = UserAchievements.selectAll()
            .where { UserAchievements.userId eq uid }
            .associateBy { it[UserAchievements.key] }

        val allDefs = CATALOG.values.toMutableList()
        RankService.PROFILE_RANK_ORDER.drop(1).forEach { rankName -> allDefs.add(rankUpDef(rankName)) }

        allDefs.map { def ->
            val row = unlockedByKey[def.key]
            AchievementDto(
                key = def.key,
                title = def.title,
                bigWord = def.bigWord,
                body = def.body,
                unlocked = row != null,
                unlockedAt = row?.get(UserAchievements.unlockedAt),
            )
        }
    }

    // Chat list > More > Achievements "USE" - equips an unlocked
    // achievement as the small badge shown next to the user's name on
    // Profile. null clears it. Silently ignores a key the user hasn't
    // actually unlocked (defensive - the client only ever offers USE on
    // unlocked entries).
    fun setActiveBadge(userId: String, key: String?): Unit = transaction {
        val uid = UUID.fromString(userId)
        if (key != null) {
            val owns = UserAchievements.selectAll().where { (UserAchievements.userId eq uid) and (UserAchievements.key eq key) }.any()
            if (!owns) return@transaction
        }
        com.xhacker.cedal.db.Users.update({ com.xhacker.cedal.db.Users.id eq uid }) { it[activeBadgeKey] = key }
    }

    // "Secure" - two-factor + a passcode + a verified phone number, all on.
    // Called after any of the three changes (confirmTwoFactorSetup,
    // updatePasscode, verifyPhoneCode).
    fun checkSecure(userId: String): Unit = transaction {
        val uid = UUID.fromString(userId)
        val row = com.xhacker.cedal.db.Users.selectAll().where { com.xhacker.cedal.db.Users.id eq uid }.firstOrNull() ?: return@transaction
        val secure = row[com.xhacker.cedal.db.Users.twoFactorEnabled] &&
            row[com.xhacker.cedal.db.Users.passcode] != null &&
            row[com.xhacker.cedal.db.Users.phoneVerified]
        if (secure) {
            unlock(uid, "secure")
            checkSecureAndPrivateCombo(uid)
        }
    }

    // "Private" - Settings > Security > Popularity set so NOTHING (name,
    // pfp, age, rank) shows to other people. Called after
    // PopularityService.setSettings.
    fun checkPrivate(userId: String): Unit = transaction {
        val uid = UUID.fromString(userId)
        val settings = com.xhacker.cedal.db.PopularitySettings.selectAll()
            .where { com.xhacker.cedal.db.PopularitySettings.userId eq uid }
            .firstOrNull() ?: return@transaction
        val private = !settings[com.xhacker.cedal.db.PopularitySettings.showName] &&
            !settings[com.xhacker.cedal.db.PopularitySettings.showPfp] &&
            !settings[com.xhacker.cedal.db.PopularitySettings.showAge] &&
            !settings[com.xhacker.cedal.db.PopularitySettings.showRank]
        if (private) {
            unlock(uid, "private")
            checkSecureAndPrivateCombo(uid)
        }
    }

    private fun checkSecureAndPrivateCombo(userId: UUID): Unit = transaction {
        val hasSecure = UserAchievements.selectAll().where { (UserAchievements.userId eq userId) and (UserAchievements.key eq "secure") }.any()
        val hasPrivate = UserAchievements.selectAll().where { (UserAchievements.userId eq userId) and (UserAchievements.key eq "private") }.any()
        if (hasSecure && hasPrivate) unlock(userId, "secure_and_private")
    }

    // Counts for the block/blocked-20 thresholds - see ChatService.setBlocked.
    fun countBlocksMade(userId: UUID): Long = transaction {
        com.xhacker.cedal.db.Blocks.selectAll().where { com.xhacker.cedal.db.Blocks.blockerId eq userId }.count()
    }

    fun countTimesBlocked(userId: UUID): Long = transaction {
        com.xhacker.cedal.db.Blocks.selectAll().where { com.xhacker.cedal.db.Blocks.blockedId eq userId }.count()
    }
}
