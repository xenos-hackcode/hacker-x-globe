package com.xhacker.cedal.services

import com.xhacker.cedal.db.CallOutRejectedSpans
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

// "Call Out" (Settings > Corneal AI > Call Out, text-based) - Corneal reads
// the user's currently open Code file (see CodeContextDto) and can circle/
// highlight a specific snippet it believes is a mistake (see
// CornealChatService's CALLOUT_ tag parsing). This just tracks which
// snippets a user has already said "no, that's not it" to, per file, so the
// same wrong guess isn't repeated - see CornealChatService.reply's prompt.
object CallOutService {
    fun listRejected(userId: String, filePath: String): List<String> = transaction {
        val uid = UUID.fromString(userId)
        CallOutRejectedSpans.selectAll()
            .where { (CallOutRejectedSpans.userId eq uid) and (CallOutRejectedSpans.filePath eq filePath) }
            .map { it[CallOutRejectedSpans.snippet] }
    }

    fun reject(userId: String, filePath: String, snippet: String): Unit = transaction {
        val uid = UUID.fromString(userId)
        val already = CallOutRejectedSpans.selectAll()
            .where { (CallOutRejectedSpans.userId eq uid) and (CallOutRejectedSpans.filePath eq filePath) and (CallOutRejectedSpans.snippet eq snippet) }
            .any()
        if (!already) {
            CallOutRejectedSpans.insert {
                it[CallOutRejectedSpans.userId] = uid
                it[CallOutRejectedSpans.filePath] = filePath
                it[CallOutRejectedSpans.snippet] = snippet
                it[rejectedAt] = System.currentTimeMillis()
            }
        }
    }
}
