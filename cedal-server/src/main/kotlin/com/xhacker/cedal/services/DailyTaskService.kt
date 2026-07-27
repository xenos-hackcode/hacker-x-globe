package com.xhacker.cedal.services

import com.xhacker.cedal.db.DailyTaskCompletions
import com.xhacker.cedal.db.DailyTasks
import com.xhacker.cedal.db.Users
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.time.LocalDate
import java.util.UUID

// "Today's Task" for Invest and ARC - a fresh AI-written task per (area,
// date), generated lazily by whoever asks for it first that day and cached
// for everyone else for the rest of the day (no separate cron/scheduler
// needed for this to be functionally "daily"). Deliberately never labeled
// as AI-generated to the client - it's presented as ordinary built-in
// content, same as every hardcoded lesson elsewhere in the app.
object DailyTaskService {
    const val MIN_EXP = 15L
    const val MAX_EXP = 35L

    @Serializable
    private data class GeneratedTask(val title: String, val description: String)

    data class TodayTask(val title: String, val description: String, val expReward: Long, val completed: Boolean)

    // suspend, and deliberately does NOT wrap the whole thing in one
    // transaction { } - Exposed's transaction block is blocking/synchronous,
    // so a slow outbound AI call (generate()) has no business happening
    // while it holds a DB connection open. Instead: check for today's row,
    // generate outside any transaction if missing, then insertIgnore (so two
    // simultaneous "first request of the day" callers can't both try to
    // insert the same (area, date) primary key) and re-select either way.
    suspend fun today(area: String, userId: String): TodayTask {
        val date = LocalDate.now().toString()
        val existing = transaction {
            DailyTasks.selectAll().where { (DailyTasks.area eq area) and (DailyTasks.taskDate eq date) }.firstOrNull()
        }
        if (existing == null) {
            val generated = generate(area)
            val expReward = (MIN_EXP..MAX_EXP).random()
            transaction {
                DailyTasks.insertIgnore {
                    it[DailyTasks.area] = area
                    it[taskDate] = date
                    it[title] = generated.title
                    it[description] = generated.description
                    it[DailyTasks.expReward] = expReward
                }
            }
        }
        return transaction {
            val row = DailyTasks.selectAll().where { (DailyTasks.area eq area) and (DailyTasks.taskDate eq date) }.first()
            val uid = UUID.fromString(userId)
            val completed = DailyTaskCompletions
                .selectAll()
                .where { (DailyTaskCompletions.userId eq uid) and (DailyTaskCompletions.area eq area) and (DailyTaskCompletions.taskDate eq date) }
                .any()
            TodayTask(row[DailyTasks.title], row[DailyTasks.description], row[DailyTasks.expReward], completed)
        }
    }

    // Idempotent per (user, area, date) - same pattern as LessonService.
    fun complete(area: String, userId: String): Long = transaction {
        val date = LocalDate.now().toString()
        val uid = UUID.fromString(userId)
        val currentExp = Users.selectAll().where { Users.id eq uid }.first()[Users.exp]
        val alreadyDone = DailyTaskCompletions
            .selectAll()
            .where { (DailyTaskCompletions.userId eq uid) and (DailyTaskCompletions.area eq area) and (DailyTaskCompletions.taskDate eq date) }
            .any()
        if (alreadyDone) return@transaction currentExp

        val task = DailyTasks.selectAll().where { (DailyTasks.area eq area) and (DailyTasks.taskDate eq date) }.firstOrNull()
            ?: throw AuthException("No task generated for today yet")
        DailyTaskCompletions.insert {
            it[DailyTaskCompletions.userId] = uid
            it[DailyTaskCompletions.area] = area
            it[taskDate] = date
            it[completedAt] = System.currentTimeMillis()
        }
        val newExp = currentExp + task[DailyTasks.expReward]
        Users.update({ Users.id eq uid }) { it[exp] = newExp }
        RankUpService.checkRankUp(uid, currentExp, newExp)
        newExp
    }

    private suspend fun generate(area: String): GeneratedTask {
        val topic = if (area == "invest") {
            "everyday crypto/investing habits (e.g. checking a coin's chart, comparing two coins' market caps, practicing a small simulated trade, reviewing order history)"
        } else {
            "legal, ethical cybersecurity practice (e.g. scanning your own Wi-Fi in Labs, trying a lesson's quiz, chatting with the Assistant about a concept, playing an ARC Ops mission)"
        }
        val prompt = "Write ONE short daily challenge task for a learning app, themed around $topic. It must be " +
            "completable in a couple of minutes using only features that already exist in the app (don't invent " +
            "new features). Keep it simple enough a beginner (even a curious 10-year-old) could understand " +
            "immediately. Reply with ONLY valid JSON, no markdown, no code fences, no extra text, in exactly " +
            "this shape: {\"title\": \"a short punchy title, 5 words max\", \"description\": \"one or two " +
            "friendly sentences telling the user exactly what to go do\"}."
        return try {
            val raw = AiProviderService.ask(prompt, maxTokens = 250)
            val cleaned = raw.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            AiProviderService.jsonParser.decodeFromString<GeneratedTask>(cleaned)
        } catch (e: Exception) {
            fallbackTask(area)
        }
    }

    private fun fallbackTask(area: String): GeneratedTask = if (area == "invest") {
        GeneratedTask("Check a chart", "Open Overview and check the 7-day chart on any one coin before you do anything else today.")
    } else {
        GeneratedTask("Scan your network", "Open ARC Labs and run a scan of your own Wi-Fi network - see how many devices show up.")
    }
}
