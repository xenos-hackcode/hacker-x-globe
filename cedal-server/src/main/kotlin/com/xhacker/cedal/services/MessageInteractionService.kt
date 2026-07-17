package com.xhacker.cedal.services

import com.xhacker.cedal.db.MessagePins
import com.xhacker.cedal.db.MessageReports
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.util.UUID

// Cross-chat-type pin/report - a message can live in ChatMessages, AiMessages
// (corneal/arc), or AiChangeRequests (code, addressed as "{id}#user" /
// "{id}#assistant" since that table bundles a whole exchange per row) - see
// MessagePins/MessageReports in Tables.kt for why this references messages
// loosely by (chatType, messageId) rather than a real FK.
object MessageInteractionService {
    @Serializable
    data class PinDto(
        val id: String,
        val chatType: String,
        val messageId: String,
        val chatKey: String? = null,
        val messageText: String,
        val pinnedAt: Long,
    )

    @Serializable
    data class ReportDto(
        val id: String,
        val chatType: String,
        val messageId: String,
        val messageText: String,
        val status: String,
        val createdAt: Long,
    )

    fun pin(userId: String, chatType: String, messageId: String, chatKey: String?, messageText: String): PinDto = transaction {
        val uid = UUID.fromString(userId)
        val already = MessagePins.selectAll()
            .where { (MessagePins.userId eq uid) and (MessagePins.chatType eq chatType) and (MessagePins.messageId eq messageId) }
            .firstOrNull()
        if (already != null) {
            return@transaction PinDto(
                already[MessagePins.id].value.toString(), chatType, messageId,
                already[MessagePins.chatKey], already[MessagePins.messageText], already[MessagePins.pinnedAt],
            )
        }
        val pinnedAt = System.currentTimeMillis()
        val id = MessagePins.insertAndGetId {
            it[MessagePins.userId] = uid
            it[MessagePins.chatType] = chatType
            it[MessagePins.messageId] = messageId
            it[MessagePins.chatKey] = chatKey
            it[MessagePins.messageText] = messageText
            it[MessagePins.pinnedAt] = pinnedAt
        }
        PinDto(id.value.toString(), chatType, messageId, chatKey, messageText, pinnedAt)
    }

    fun unpin(userId: String, pinId: String) {
        transaction {
            val uid = UUID.fromString(userId)
            val pid = UUID.fromString(pinId)
            val row = MessagePins.selectAll().where { MessagePins.id eq pid }.firstOrNull() ?: throw AuthException("Pin not found")
            if (row[MessagePins.userId].value != uid) throw AuthException("Not your pin")
            MessagePins.deleteWhere { MessagePins.id eq pid }
        }
    }

    fun listPins(userId: String): List<PinDto> = transaction {
        MessagePins.selectAll()
            .where { MessagePins.userId eq UUID.fromString(userId) }
            .orderBy(MessagePins.pinnedAt, SortOrder.DESC)
            .map {
                PinDto(
                    it[MessagePins.id].value.toString(),
                    it[MessagePins.chatType],
                    it[MessagePins.messageId],
                    it[MessagePins.chatKey],
                    it[MessagePins.messageText],
                    it[MessagePins.pinnedAt],
                )
            }
    }

    fun report(reporterUserId: String, chatType: String, messageId: String, messageText: String): ReportDto = transaction {
        val createdAt = System.currentTimeMillis()
        val id = MessageReports.insertAndGetId {
            it[MessageReports.reporterUserId] = UUID.fromString(reporterUserId)
            it[MessageReports.chatType] = chatType
            it[MessageReports.messageId] = messageId
            it[MessageReports.messageText] = messageText
            it[MessageReports.createdAt] = createdAt
        }
        ReportDto(id.value.toString(), chatType, messageId, messageText, "pending", createdAt)
    }

    // Admin-only, see AdminService - reviewed newest-first, same convention
    // as AiChangeRequests' pending_approval queue.
    fun listReports(): List<ReportDto> = transaction {
        MessageReports.selectAll()
            .where { MessageReports.status eq "pending" }
            .orderBy(MessageReports.createdAt, SortOrder.DESC)
            .map {
                ReportDto(
                    it[MessageReports.id].value.toString(),
                    it[MessageReports.chatType],
                    it[MessageReports.messageId],
                    it[MessageReports.messageText],
                    it[MessageReports.status],
                    it[MessageReports.createdAt],
                )
            }
    }

    fun dismissReport(reportId: String) {
        transaction {
            MessageReports.update({ MessageReports.id eq UUID.fromString(reportId) }) { it[status] = "reviewed" }
        }
    }
}
