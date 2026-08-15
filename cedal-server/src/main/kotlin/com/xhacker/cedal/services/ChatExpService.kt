package com.xhacker.cedal.services

import com.xhacker.cedal.db.ChatExpAwards
import com.xhacker.cedal.db.Users
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.time.LocalDate
import java.util.UUID

// Chatting now also feeds Profile's rank exp, alongside Learn lessons/daily
// tasks - a flat award for having chatted at all today, not per-message, so
// spamming messages can't be farmed for exp. Idempotent per (user, date) via
// ChatExpAwards, same pattern as DailyTaskCompletions/LessonCompletions.
object ChatExpService {
    const val EXP_PER_DAY = 20L

    // Called from ChatService/GroupChatService right after a message send
    // succeeds. Silently a no-op past the first call of the day - callers
    // don't need to check anything first.
    fun awardForChattingToday(userId: String) = transaction {
        val uid = UUID.fromString(userId)
        val date = LocalDate.now().toString()
        val alreadyAwarded = ChatExpAwards
            .selectAll()
            .where { (ChatExpAwards.userId eq uid) and (ChatExpAwards.awardDate eq date) }
            .any()
        if (alreadyAwarded) return@transaction

        ChatExpAwards.insert {
            it[ChatExpAwards.userId] = uid
            it[awardDate] = date
            it[awardedAt] = System.currentTimeMillis()
        }
        val currentExp = Users.selectAll().where { Users.id eq uid }.first()[Users.exp]
        val newExp = currentExp + EXP_PER_DAY
        Users.update({ Users.id eq uid }) { it[exp] = newExp }
        RankUpService.checkRankUp(uid, currentExp, newExp)
    }
}
