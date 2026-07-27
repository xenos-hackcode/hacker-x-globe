package com.xhacker.cedal.services

import com.xhacker.cedal.db.PendingPopups
import com.xhacker.cedal.models.PendingPopupDto
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.isNull
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.util.UUID

// Backs achievement-unlock and rank-up in-app popups (see AchievementService,
// RankUpService) - a queue instead of a direct push so a popup queued while
// the user is offline still shows up the next time they open the app,
// exactly once (see pollPending marking deliveredAt).
object PendingPopupService {
    fun enqueue(userId: UUID, kind: String, title: String, bigWord: String?, body: String) = transaction {
        PendingPopups.insert {
            it[PendingPopups.userId] = userId
            it[PendingPopups.kind] = kind
            it[PendingPopups.title] = title
            it[PendingPopups.bigWord] = bigWord
            it[PendingPopups.body] = body
            it[createdAt] = System.currentTimeMillis()
        }
    }

    // Polled globally by the client (see MemberScaffold) so a popup surfaces
    // no matter which screen the user is on. Every undelivered row for this
    // user is returned AND marked delivered in the same call.
    fun pollPending(userId: String): List<PendingPopupDto> = transaction {
        val uid = UUID.fromString(userId)
        val rows = PendingPopups.selectAll()
            .where { (PendingPopups.userId eq uid) and (PendingPopups.deliveredAt.isNull()) }
            .orderBy(PendingPopups.createdAt)
            .toList()
        if (rows.isNotEmpty()) {
            val now = System.currentTimeMillis()
            val ids = rows.map { it[PendingPopups.id].value }
            PendingPopups.update({ PendingPopups.id inList ids }) { it[deliveredAt] = now }
        }
        rows.map { row ->
            PendingPopupDto(
                id = row[PendingPopups.id].value.toString(),
                kind = row[PendingPopups.kind],
                title = row[PendingPopups.title],
                bigWord = row[PendingPopups.bigWord],
                body = row[PendingPopups.body],
            )
        }
    }
}
