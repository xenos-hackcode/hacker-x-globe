package com.xhacker.cedal.services

import com.xhacker.cedal.db.TypingStatus
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greater
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.util.UUID

// WhatsApp-style "X is typing…" - the client pings this (throttled, every
// couple seconds while actively typing) instead of an explicit "stopped
// typing" signal; a ping older than FRESHNESS_MS just stops counting on its
// own, which also means an app killed mid-type never leaves a friend stuck
// looking permanently "typing…".
object TypingService {
    private const val FRESHNESS_MS = 6_000L

    fun ping(userId: String, friendId: String) {
        val uid = UUID.fromString(userId)
        val fid = UUID.fromString(friendId)
        transaction {
            val existing = TypingStatus.selectAll().where { (TypingStatus.userId eq uid) and (TypingStatus.friendId eq fid) }.firstOrNull()
            if (existing != null) {
                TypingStatus.update({ (TypingStatus.userId eq uid) and (TypingStatus.friendId eq fid) }) {
                    it[updatedAt] = System.currentTimeMillis()
                }
            } else {
                TypingStatus.insert {
                    it[TypingStatus.userId] = uid
                    it[TypingStatus.friendId] = fid
                    it[updatedAt] = System.currentTimeMillis()
                }
            }
        }
    }

    // Every friend currently (freshly) typing TO this user - one query, so
    // both the chat list (every friend at once) and a single open thread
    // (checking membership) can share it without N separate calls.
    fun typingToMe(userId: String): List<String> = transaction {
        val uid = UUID.fromString(userId)
        val cutoff = System.currentTimeMillis() - FRESHNESS_MS
        TypingStatus.selectAll()
            .where { (TypingStatus.friendId eq uid) and (TypingStatus.updatedAt greater cutoff) }
            .map { it[TypingStatus.userId].value.toString() }
    }
}
