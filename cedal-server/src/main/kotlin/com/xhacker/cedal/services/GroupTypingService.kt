package com.xhacker.cedal.services

import com.xhacker.cedal.db.GroupTypingStatus
import com.xhacker.cedal.db.Groups
import com.xhacker.cedal.db.Users
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greater
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.SqlExpressionBuilder.neq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.util.UUID

// Settings > Groups follow-up (2026-08-10) - group-chat sibling of
// TypingService, same ping/6s-freshness shape, extended with a two-gate
// check TypingService's 1-on-1 version doesn't need: a group-wide,
// Creator-only kill switch (Groups.typingIndicatorsEnabled) on top of each
// member's own personal one (Users.groupTypingIndicatorsEnabled). Both
// have to be true or ping() is a no-op - so a row existing here at all
// already means both gates passed at write time, no re-checking on read.
object GroupTypingService {
    private const val FRESHNESS_MS = 6_000L

    fun ping(userId: String, groupId: String) {
        val uid = UUID.fromString(userId)
        val gid = UUID.fromString(groupId)
        transaction {
            val groupAllows = Groups.selectAll().where { Groups.id eq gid }.firstOrNull()?.get(Groups.typingIndicatorsEnabled) ?: return@transaction
            if (!groupAllows) return@transaction
            val userAllows = Users.selectAll().where { Users.id eq uid }.firstOrNull()?.get(Users.groupTypingIndicatorsEnabled) ?: return@transaction
            if (!userAllows) return@transaction

            val existing = GroupTypingStatus.selectAll().where { (GroupTypingStatus.groupId eq gid) and (GroupTypingStatus.userId eq uid) }.firstOrNull()
            if (existing != null) {
                GroupTypingStatus.update({ (GroupTypingStatus.groupId eq gid) and (GroupTypingStatus.userId eq uid) }) {
                    it[updatedAt] = System.currentTimeMillis()
                }
            } else {
                GroupTypingStatus.insert {
                    it[GroupTypingStatus.groupId] = gid
                    it[GroupTypingStatus.userId] = uid
                    it[updatedAt] = System.currentTimeMillis()
                }
            }
        }
    }

    // For an open thread's own poll - every user (id) freshly typing in
    // this one group, excluding the caller themselves.
    fun typingInGroup(groupId: String, excludingUserId: String): List<String> = transaction {
        val gid = UUID.fromString(groupId)
        val uid = UUID.fromString(excludingUserId)
        val cutoff = System.currentTimeMillis() - FRESHNESS_MS
        GroupTypingStatus.selectAll()
            .where { (GroupTypingStatus.groupId eq gid) and (GroupTypingStatus.userId neq uid) and (GroupTypingStatus.updatedAt greater cutoff) }
            .map { it[GroupTypingStatus.userId].value.toString() }
    }

    // For the chat-list "A and B are typing" preview - every group at
    // once, resolved straight to display names (not ids) since that's all
    // ConversationSummary.typingUserNames needs, avoiding a client-side
    // lookup against member rosters it may not have loaded for every group.
    fun typingNamesAcrossGroups(userId: String, groupIds: Collection<UUID>): Map<UUID, List<String>> {
        if (groupIds.isEmpty()) return emptyMap()
        val uid = UUID.fromString(userId)
        return transaction {
            val cutoff = System.currentTimeMillis() - FRESHNESS_MS
            val rows = GroupTypingStatus.selectAll()
                .where { (GroupTypingStatus.groupId inList groupIds) and (GroupTypingStatus.userId neq uid) and (GroupTypingStatus.updatedAt greater cutoff) }
                .toList()
            if (rows.isEmpty()) return@transaction emptyMap()
            val typerIds = rows.map { it[GroupTypingStatus.userId].value }.distinct()
            val namesById = Users.selectAll().where { Users.id inList typerIds }
                .associate { it[Users.id].value to (it[Users.nickname] ?: "Someone") }
            rows.groupBy({ it[GroupTypingStatus.groupId].value }, { namesById[it[GroupTypingStatus.userId].value] ?: "Someone" })
        }
    }
}
