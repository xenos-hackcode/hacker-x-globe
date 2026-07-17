package com.xhacker.cedal.services

import com.xhacker.cedal.db.LessonCompletions
import com.xhacker.cedal.db.Users
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.util.UUID

// Awards Profile's rank exp for completing an Invest > Learn lesson - flat
// amount per lesson, same for every lesson (not scaled by quiz score; the
// quiz score is tracked separately, client-side only, purely informational).
// Idempotent per (user, lesson): the client lets a lesson's checkbox be
// toggled back off and on, but exp is only ever awarded the first time.
object LessonService {
    const val EXP_PER_LESSON = 50L

    // Returns the account's exp after this call - unchanged if the lesson
    // was already completed before.
    fun completeLesson(userId: String, lessonId: String): Long = transaction {
        val uid = UUID.fromString(userId)
        val currentExp = Users.selectAll().where { Users.id eq uid }.first()[Users.exp]
        val alreadyDone = LessonCompletions
            .selectAll()
            .where { (LessonCompletions.userId eq uid) and (LessonCompletions.lessonId eq lessonId) }
            .any()
        if (alreadyDone) return@transaction currentExp

        LessonCompletions.insert {
            it[LessonCompletions.userId] = uid
            it[LessonCompletions.lessonId] = lessonId
            it[completedAt] = System.currentTimeMillis()
        }
        val newExp = currentExp + EXP_PER_LESSON
        Users.update({ Users.id eq uid }) { it[exp] = newExp }
        newExp
    }
}
