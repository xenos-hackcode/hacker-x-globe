package com.xhacker.cedal.services

import com.xhacker.cedal.db.SavedMessages
import com.xhacker.cedal.models.SavedMessageDto
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

// "Saved Messages" - a personal conversation-with-yourself, see
// SavedMessages' own doc comment in Tables.kt for why this is deliberately
// NOT built on ChatService/ChatMessages. Reached via the "Save" long-press
// action on a group message (GroupChatThreadScreen.kt), hidden entirely when
// the source group has securedMode on.
object SavedMessagesService {
    private fun toDto(row: org.jetbrains.exposed.sql.ResultRow): SavedMessageDto = SavedMessageDto(
        id = row[SavedMessages.id].value.toString(),
        sourceLabel = row[SavedMessages.sourceLabel],
        text = row[SavedMessages.text],
        mediaUrl = row[SavedMessages.mediaUrl],
        mediaType = row[SavedMessages.mediaType],
        fileName = row[SavedMessages.fileName],
        savedAt = row[SavedMessages.savedAt],
    )

    fun save(userId: String, sourceLabel: String?, text: String, mediaUrl: String?, mediaType: String?, fileName: String?): SavedMessageDto = transaction {
        val uid = UUID.fromString(userId)
        val trimmed = text.trim()
        if (trimmed.isBlank() && mediaUrl == null) throw AuthException("Nothing to save")
        val id = SavedMessages.insertAndGetId {
            it[SavedMessages.userId] = uid
            it[SavedMessages.sourceLabel] = sourceLabel?.take(200)
            it[SavedMessages.text] = trimmed
            it[SavedMessages.mediaUrl] = mediaUrl
            it[SavedMessages.mediaType] = mediaType
            it[SavedMessages.fileName] = fileName
            it[SavedMessages.savedAt] = System.currentTimeMillis()
        }
        SavedMessageDto(id.value.toString(), sourceLabel, trimmed, mediaUrl, mediaType, fileName, System.currentTimeMillis())
    }

    fun list(userId: String): List<SavedMessageDto> = transaction {
        val uid = UUID.fromString(userId)
        SavedMessages.selectAll().where { SavedMessages.userId eq uid }
            .orderBy(SavedMessages.savedAt, SortOrder.DESC)
            .map { toDto(it) }
    }

    fun delete(userId: String, id: String): Unit = transaction {
        val uid = UUID.fromString(userId)
        val mid = UUID.fromString(id)
        SavedMessages.deleteWhere { (SavedMessages.id eq mid) and (SavedMessages.userId eq uid) }
    }
}
