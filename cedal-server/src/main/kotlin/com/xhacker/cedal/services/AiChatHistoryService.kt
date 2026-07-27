package com.xhacker.cedal.services

import com.xhacker.cedal.db.AiChangeRequests
import com.xhacker.cedal.db.AiMessages
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.util.UUID

// Backs persistence for Corneal and ARC's Assistant chats - both used to
// live only in Compose `remember` state, resending the whole conversation
// on every turn and losing it the moment you navigated away or restarted
// the app. The Code AI's own turn-by-turn history reuses AiChangeRequests
// directly rather than a third lane here.
//
// crossReferenceBlock is what lets one assistant actually verify a claim
// about what another one said ("Corneal told me X") instead of just taking
// the user's word for it - it's real transcript excerpts from the other
// lanes, injected into the prompt as reference context, not a merged
// memory. The three assistants stay logically separate conversations.
object AiChatHistoryService {
    data class Turn(
        val id: String,
        val role: String,
        val content: String,
        val createdAt: Long,
        val replyToId: String? = null,
        val editedAt: Long? = null,
        val deleted: Boolean = false,
        val mediaUrl: String? = null,
        val mediaType: String? = null,
        val fileName: String? = null,
        // Speech-to-text of a mediaType="audio" voice note - AI-internal
        // only. Never included in any client-facing DTO or rendered as a
        // chat bubble; only mediaPlaceholder (below) ever reads it, to fold
        // the actual words into the flattened text prompt an assistant
        // reasons over. content itself stays exactly what the user typed.
        val transcript: String? = null,
    )

    // Text stand-in for a media turn, used when building the flattened
    // transcript prompt (see CornealChatService/ArcChatService.reply()) so
    // history reads sensibly even for turns that aren't the current vision
    // call - only the just-sent turn's image (if any) gets real vision.
    fun mediaPlaceholder(turn: Turn): String? = when (turn.mediaType) {
        "image" -> "[a real photo/picture attached]"
        // A real uploaded sticker image (see ChatMediaOrIcon client-side) -
        // a decorative reaction picture the user picked from their sticker
        // pack, not a literal photo of something - worth real vision (it IS
        // a real loadable image), just framed differently than "image".
        "sticker" -> "[sent a sticker - a decorative reaction image, not a real photo]"
        // The real transcript, if transcription succeeded - never shown to
        // any user, only folded in here for the AI's own reasoning.
        "audio" -> turn.transcript?.let { "(voice message transcript: \"$it\")" }
            ?: "[sent a voice message that couldn't be transcribed at all - audio may have been silent or unclear]"
        "video" -> "[video attached]"
        // A decorative Icon-pack pick (see ChatMediaOrIcon client-side) -
        // a fake "icon:Name" pseudo-URL, not a real loadable image, so
        // deliberately never sent to vision - naming which icon at least
        // keeps its identity from being lost entirely.
        "icon" -> "[sent an icon" + (turn.mediaUrl?.removePrefix("icon:")?.takeIf { it.isNotBlank() }?.let { " named \"$it\"" } ?: "") + " - a decorative UI icon, not a real photo]"
        null -> null
        else -> "[file attached: ${turn.fileName ?: "file"}]"
    }

    // Matches ChatService's edit window - after this, a message is locked
    // as sent (deletion still works at any time, only editing is time-boxed).
    private val EDIT_WINDOW_MS = 5 * 60 * 1000L

    // "code" lives in AiChangeRequests. "alucard" (Developer Mode's
    // security/code-review bot) is deliberately excluded - it's
    // developer-only and shouldn't leak into/from user-facing Corneal/ARC
    // context, or vice versa.
    private val LANES = listOf("corneal", "arc")

    private fun toTurn(row: ResultRow): Turn {
        val isDeleted = row[AiMessages.deleted]
        return Turn(
            id = row[AiMessages.id].value.toString(),
            role = row[AiMessages.role],
            content = if (isDeleted) "" else row[AiMessages.content],
            createdAt = row[AiMessages.createdAt],
            replyToId = row[AiMessages.replyToId]?.toString(),
            editedAt = row[AiMessages.editedAt],
            deleted = isDeleted,
            mediaUrl = if (isDeleted) null else row[AiMessages.mediaUrl],
            mediaType = if (isDeleted) null else row[AiMessages.mediaType],
            fileName = if (isDeleted) null else row[AiMessages.fileName],
            transcript = if (isDeleted) null else row[AiMessages.transcript],
        )
    }

    fun history(userId: String, assistant: String, limit: Int = 50): List<Turn> = transaction {
        AiMessages.selectAll()
            .where { (AiMessages.userId eq UUID.fromString(userId)) and (AiMessages.assistant eq assistant) }
            .orderBy(AiMessages.createdAt, SortOrder.DESC)
            .limit(limit)
            .map { toTurn(it) }
            .reversed()
    }

    fun append(
        userId: String,
        assistant: String,
        role: String,
        content: String,
        replyToId: String? = null,
        mediaUrl: String? = null,
        mediaType: String? = null,
        fileName: String? = null,
        transcript: String? = null,
    ): Turn = transaction {
        val validReply = replyToId?.let { runCatching { UUID.fromString(it) }.getOrNull() }?.takeIf { rid ->
            AiMessages.selectAll().where { (AiMessages.id eq rid) and (AiMessages.userId eq UUID.fromString(userId)) }.any()
        }
        val id = AiMessages.insertAndGetId {
            it[AiMessages.userId] = UUID.fromString(userId)
            it[AiMessages.assistant] = assistant
            it[AiMessages.role] = role
            it[AiMessages.content] = content
            it[AiMessages.replyToId] = validReply
            it[AiMessages.mediaUrl] = mediaUrl
            it[AiMessages.mediaType] = mediaType
            it[AiMessages.fileName] = fileName
            it[AiMessages.transcript] = transcript
            it[createdAt] = System.currentTimeMillis()
        }
        toTurn(AiMessages.selectAll().where { AiMessages.id eq id }.first())
    }

    fun editMessage(userId: String, messageId: String, newContent: String): Turn = transaction {
        val uid = UUID.fromString(userId)
        val mid = UUID.fromString(messageId)
        val row = AiMessages.selectAll().where { AiMessages.id eq mid }.firstOrNull() ?: throw AuthException("Message not found")
        if (row[AiMessages.userId].value != uid) throw AuthException("You can only edit your own messages")
        if (row[AiMessages.role] != "user") throw AuthException("You can only edit your own messages")
        if (row[AiMessages.deleted]) throw AuthException("Can't edit a deleted message")
        if (System.currentTimeMillis() - row[AiMessages.createdAt] > EDIT_WINDOW_MS) throw AuthException("Too late to edit this message")
        val trimmed = newContent.trim()
        if (trimmed.isBlank()) throw AuthException("Message can't be empty")

        AiMessages.update({ AiMessages.id eq mid }) {
            it[content] = trimmed
            it[editedAt] = System.currentTimeMillis()
        }
        toTurn(AiMessages.selectAll().where { AiMessages.id eq mid }.first())
    }

    fun deleteMessage(userId: String, messageId: String) {
        transaction {
            val uid = UUID.fromString(userId)
            val mid = UUID.fromString(messageId)
            val row = AiMessages.selectAll().where { AiMessages.id eq mid }.firstOrNull() ?: throw AuthException("Message not found")
            if (row[AiMessages.userId].value != uid) throw AuthException("You can only delete your own messages")
            AiMessages.update({ AiMessages.id eq mid }) { it[deleted] = true }
        }
    }

    // "Clear history" (Settings > AI) - same tombstone semantics as a
    // single-message delete, applied to every one of this user's own rows
    // in this one lane at once. Corneal/ARC are separate conversations
    // (different `assistant` value) so clearing one never touches the other.
    fun deleteAllHistory(userId: String, assistant: String) {
        transaction {
            AiMessages.update({ (AiMessages.userId eq UUID.fromString(userId)) and (AiMessages.assistant eq assistant) }) { it[deleted] = true }
        }
    }

    // Recent turns from the OTHER lanes only - excludes whichever assistant
    // is asking, since it already has its own full history separately.
    // Kept short (a handful of turns each) to stay a cheap prompt addition,
    // not a second full transcript.
    fun crossReferenceBlock(userId: String, excluding: String, limitPerLane: Int = 6): String = transaction {
        val block = StringBuilder()

        LANES.filter { it != excluding }.forEach { lane ->
            val turns = history(userId, lane, limitPerLane).filterNot { it.deleted }
            if (turns.isNotEmpty()) {
                val label = when (lane) { "corneal" -> "Corneal"; "arc" -> "ARC"; else -> lane }
                turns.forEach { turn ->
                    val text = listOfNotNull(turn.content.takeIf { it.isNotBlank() }, mediaPlaceholder(turn)).joinToString(" ")
                    block.appendLine("[$label] ${if (turn.role == "user") "User" else label}: $text")
                }
            }
        }

        if (excluding != "code") {
            val codeTurns = AiChangeRequests.selectAll()
                .where { AiChangeRequests.requesterUserId eq UUID.fromString(userId) }
                .orderBy(AiChangeRequests.createdAt, SortOrder.DESC)
                .limit(limitPerLane)
                .toList()
                .reversed()
            codeTurns.forEach { row ->
                if (!row[AiChangeRequests.requestDeleted]) {
                    val placeholder = when (row[AiChangeRequests.mediaType]) {
                        "image" -> " [a real photo/picture attached]"
                        "sticker" -> " [sent a sticker - a decorative reaction image, not a real photo]"
                        "audio" -> row[AiChangeRequests.transcript]?.let { " (voice message transcript: \"$it\")" }
                            ?: " [sent a voice message that couldn't be transcribed at all]"
                        "video" -> " [video attached]"
                        "icon" -> " [sent an icon" + (row[AiChangeRequests.mediaUrl]?.removePrefix("icon:")?.takeIf { it.isNotBlank() }?.let { " named \"$it\"" } ?: "") + " - a decorative UI icon, not a real photo]"
                        null -> ""
                        else -> " [file attached: ${row[AiChangeRequests.fileName] ?: "file"}]"
                    }
                    block.appendLine("[Code AI] User: ${row[AiChangeRequests.requestText]}$placeholder")
                }
                val reply = row[AiChangeRequests.answerText] ?: row[AiChangeRequests.summary]
                if (reply != null) block.appendLine("[Code AI] Code AI: $reply")
            }
        }

        if (block.isEmpty()) {
            ""
        } else {
            "Recent context from Cedal's other AI assistants (for reference only - if the user " +
                "claims one of them said something, check here before confirming or correcting; " +
                "don't volunteer this unless it's relevant to what's being asked):\n$block"
        }
    }
}
