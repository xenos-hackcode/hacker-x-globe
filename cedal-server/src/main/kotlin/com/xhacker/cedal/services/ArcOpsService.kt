package com.xhacker.cedal.services

import com.xhacker.cedal.db.Users
import com.xhacker.cedal.models.ArcMission
import com.xhacker.cedal.models.ArcMissionPrompt
import kotlinx.serialization.decodeFromString
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.util.UUID
import kotlin.math.roundToLong

// ARC Ops - the "attack simulation" missions. Every mission is a fictional,
// entirely sandboxed scenario (no real network/device is ever touched) built
// fresh by AI each time a target is picked, specifically so the sequence of
// prompts is never the same twice - the whole point is that memorizing a
// pattern doesn't help you, same as real incident response.
object ArcOpsService {
    // Unlike LessonService's one-time-ever award, missions are meant to be
    // replayed - so this is NOT idempotent by design. Deliberately small and
    // capped (max 30 for a perfect run) so grinding easy replays isn't a
    // meaningful way to inflate exp compared to real activities elsewhere in
    // the app.
    private const val MAX_EXP_PER_MISSION = 30.0

    suspend fun generateMission(targetName: String): ArcMission {
        val raw = try {
            AiProviderService.ask(buildPrompt(targetName), maxTokens = 1200)
        } catch (e: Exception) {
            return fallbackMission(targetName)
        }
        val cleaned = raw.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        return try {
            val mission = AiProviderService.jsonParser.decodeFromString<ArcMission>(cleaned)
            if (mission.prompts.isEmpty()) fallbackMission(targetName) else mission
        } catch (e: Exception) {
            fallbackMission(targetName)
        }
    }

    fun awardMissionExp(userId: String, scorePercent: Int): Pair<Long, Long> = transaction {
        val uid = UUID.fromString(userId)
        val clampedScore = scorePercent.coerceIn(0, 100)
        val expAwarded = (MAX_EXP_PER_MISSION * clampedScore / 100.0).roundToLong()
        val currentExp = Users.selectAll().where { Users.id eq uid }.first()[Users.exp]
        val newExp = currentExp + expAwarded
        Users.update({ Users.id eq uid }) { it[exp] = newExp }
        expAwarded to newExp
    }

    private fun buildPrompt(targetName: String): String =
        "Generate a short, fictional cybersecurity incident-response training scenario for a simulated " +
            "target called \"$targetName\" (this is entirely make-believe - no real system, network, or " +
            "company). Write it for a beginner, simply enough a curious 10-year-old could follow, but " +
            "technically sensible. Include a 1-2 sentence \"scenario\" describing what's happening " +
            "(e.g. suspicious login attempts, a phishing email, an open port), then 4 to 6 \"prompts\" - " +
            "quick multiple-choice judgment calls a defender would need to make fast (e.g. \"Is this login " +
            "attempt suspicious?\"), each with 2-4 short answer options, exactly one correct, a one-sentence " +
            "explanation, and secondsVisible between 3 and 6 (how long the prompt should stay on screen " +
            "before it's treated as missed - shorter for things that need snap judgment).\n\n" +
            "Reply with ONLY valid JSON, no markdown, no code fences, no extra text, in exactly this shape: " +
            "{\"scenario\": \"...\", \"prompts\": [{\"text\": \"...\", \"options\": [\"...\", \"...\"], " +
            "\"correctIndex\": 0, \"explanation\": \"...\", \"secondsVisible\": 4}]}."

    private fun fallbackMission(targetName: String): ArcMission = ArcMission(
        scenario = "$targetName is reporting unusual activity. React fast - the log keeps scrolling.",
        prompts = listOf(
            ArcMissionPrompt(
                text = "A login succeeds from a new country 2 seconds after the real user's last login from home. Suspicious?",
                options = listOf("Yes - flag it", "No - ignore it"),
                correctIndex = 0,
                explanation = "A login from a far-away location right after a normal one is a classic sign of a stolen password.",
                secondsVisible = 5,
            ),
            ArcMissionPrompt(
                text = "An email claims to be IT asking for a password 'to fix an urgent issue'. What do you do?",
                options = listOf("Report it as phishing", "Reply with the password"),
                correctIndex = 0,
                explanation = "Real IT never needs your actual password - this is a classic phishing move.",
                secondsVisible = 5,
            ),
            ArcMissionPrompt(
                text = "A server has port 23 (Telnet, unencrypted remote login) open to the whole internet. Action?",
                options = listOf("Close it / restrict it", "Leave it - it's probably fine"),
                correctIndex = 0,
                explanation = "Telnet sends everything, including passwords, in plain text - it should never be open to the public internet.",
                secondsVisible = 5,
            ),
            ArcMissionPrompt(
                text = "Ten failed login attempts in one second, then a success. Normal typing speed?",
                options = listOf("No - looks automated (a bot)", "Yes - just a fast typer"),
                correctIndex = 0,
                explanation = "No human types that fast - that pattern is a script guessing passwords automatically.",
                secondsVisible = 4,
            ),
        ),
    )
}
