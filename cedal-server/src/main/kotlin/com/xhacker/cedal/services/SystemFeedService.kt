package com.xhacker.cedal.services

import com.xhacker.cedal.db.SystemFeedPosts
import com.xhacker.cedal.db.SystemFeedReactions
import com.xhacker.cedal.db.SystemFeedReads
import com.xhacker.cedal.db.Users
import com.xhacker.cedal.models.SystemFeedPostDto
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greater
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.util.UUID

// Cedal System Feed - the "Cedal System Feed" entry every Chats list has.
// Only the admin account can post; everyone else can only react. This is a
// real, hardcoded owner-account rule (not a role/permission system) since
// there's exactly one admin, not a set of them.
object SystemFeedService {
    private const val ADMIN_EMAIL = "hackerxenos06@gmail.com"

    private fun isAdmin(userId: UUID): Boolean {
        val email = Users.selectAll().where { Users.id eq userId }.firstOrNull()?.get(Users.email) ?: return false
        return email.equals(ADMIN_EMAIL, ignoreCase = true)
    }

    private fun reactionsFor(postIds: Collection<UUID>): Map<UUID, Map<String, String>> {
        if (postIds.isEmpty()) return emptyMap()
        val result = mutableMapOf<UUID, MutableMap<String, String>>()
        SystemFeedReactions.selectAll()
            .where { SystemFeedReactions.postId inList postIds }
            .forEach { row ->
                val pid = row[SystemFeedReactions.postId].value
                result.getOrPut(pid) { mutableMapOf() }[row[SystemFeedReactions.userId].value.toString()] = row[SystemFeedReactions.emoji]
            }
        return result
    }

    private fun toDto(row: ResultRow, authorName: String, reactions: Map<String, String>) = SystemFeedPostDto(
        id = row[SystemFeedPosts.id].value.toString(),
        authorName = authorName,
        text = row[SystemFeedPosts.text],
        createdAt = row[SystemFeedPosts.createdAt],
        reactions = reactions,
    )

    fun createPost(userId: String, text: String): SystemFeedPostDto = transaction {
        val uid = UUID.fromString(userId)
        if (!isAdmin(uid)) throw AuthException("Only the admin can post to the System Feed")
        val trimmed = text.trim()
        if (trimmed.isEmpty()) throw AuthException("Post can't be empty")

        val id = SystemFeedPosts.insertAndGetId {
            it[authorId] = uid
            it[SystemFeedPosts.text] = trimmed
            it[createdAt] = System.currentTimeMillis()
        }

        val row = SystemFeedPosts.selectAll().where { SystemFeedPosts.id eq id }.first()
        toDto(row, displayNameFor(Users.selectAll().where { Users.id eq uid }.first()), emptyMap())
    }

    fun reactToPost(userId: String, postId: String, emoji: String): Map<String, String> = transaction {
        val uid = UUID.fromString(userId)
        val pid = UUID.fromString(postId)
        SystemFeedPosts.selectAll().where { SystemFeedPosts.id eq pid }.firstOrNull() ?: throw AuthException("Post not found")

        val existing = SystemFeedReactions.selectAll()
            .where { (SystemFeedReactions.postId eq pid) and (SystemFeedReactions.userId eq uid) }
            .firstOrNull()
        if (existing != null && existing[SystemFeedReactions.emoji] == emoji) {
            SystemFeedReactions.deleteWhere { (SystemFeedReactions.postId eq pid) and (SystemFeedReactions.userId eq uid) }
        } else {
            SystemFeedReactions.deleteWhere { (SystemFeedReactions.postId eq pid) and (SystemFeedReactions.userId eq uid) }
            SystemFeedReactions.insert {
                it[SystemFeedReactions.postId] = pid
                it[SystemFeedReactions.userId] = uid
                it[SystemFeedReactions.emoji] = emoji
            }
        }
        reactionsFor(listOf(pid))[pid].orEmpty()
    }

    fun listPosts(limit: Int = 200): List<SystemFeedPostDto> = transaction {
        val rows = SystemFeedPosts.selectAll()
            .orderBy(SystemFeedPosts.createdAt to SortOrder.ASC)
            .limit(limit)
            .toList()
        val reactions = reactionsFor(rows.map { it[SystemFeedPosts.id].value })
        val names = mutableMapOf<UUID, String>()
        rows.map { row ->
            val authorId = row[SystemFeedPosts.authorId].value
            val name = names.getOrPut(authorId) {
                Users.selectAll().where { Users.id eq authorId }.firstOrNull()?.let { displayNameFor(it) } ?: "Cedal"
            }
            toDto(row, name, reactions[row[SystemFeedPosts.id].value].orEmpty())
        }
    }

    fun isAdmin(userId: String): Boolean = isAdmin(UUID.fromString(userId))

    // Drives the red unread-count badge on the Chats list's System Feed row
    // (see ChatService.listConversations). No row yet = never opened, so
    // every existing post counts as unread until their first visit.
    fun unreadCountFor(userId: String): Int = transaction {
        val uid = UUID.fromString(userId)
        val lastSeenAt = SystemFeedReads.selectAll().where { SystemFeedReads.userId eq uid }.firstOrNull()?.get(SystemFeedReads.lastSeenAt) ?: 0L
        SystemFeedPosts.selectAll().where { SystemFeedPosts.createdAt greater lastSeenAt }.count().toInt()
    }

    fun markSeen(userId: String) {
        transaction {
            val uid = UUID.fromString(userId)
            val now = System.currentTimeMillis()
            val updated = SystemFeedReads.update({ SystemFeedReads.userId eq uid }) { it[lastSeenAt] = now }
            if (updated == 0) {
                SystemFeedReads.insert {
                    it[SystemFeedReads.userId] = uid
                    it[SystemFeedReads.lastSeenAt] = now
                }
            }
        }
    }
}
