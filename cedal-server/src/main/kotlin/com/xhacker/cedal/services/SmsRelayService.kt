package com.xhacker.cedal.services

import com.xhacker.cedal.db.PendingSmsJobs
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.isNull
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.util.UUID

// Real SMS delivery via a phone the app owner controls (their own SIM),
// instead of Twilio - see PendingSmsJobs' own doc comment. cedal-server just
// queues a job here; the small standalone "cedal-sms-relay" companion app
// (installed on that phone) polls listPending()/markSent() via the
// SMS_RELAY_SECRET-gated routes in SmsRelayRoutes.kt.
object SmsRelayService {
    @Serializable
    data class SmsJobDto(val id: String, val phoneNumber: String, val message: String)

    fun enqueue(phoneNumber: String, message: String): Unit = transaction {
        PendingSmsJobs.insert {
            it[PendingSmsJobs.phoneNumber] = phoneNumber
            it[PendingSmsJobs.message] = message
            it[createdAt] = System.currentTimeMillis()
        }
    }

    fun listPending(): List<SmsJobDto> = transaction {
        PendingSmsJobs.selectAll()
            .where { PendingSmsJobs.sentAt.isNull() }
            .orderBy(PendingSmsJobs.createdAt, SortOrder.ASC)
            .map { row ->
                SmsJobDto(
                    id = row[PendingSmsJobs.id].value.toString(),
                    phoneNumber = row[PendingSmsJobs.phoneNumber],
                    message = row[PendingSmsJobs.message],
                )
            }
    }

    fun markSent(id: String): Unit = transaction {
        PendingSmsJobs.update({ PendingSmsJobs.id eq UUID.fromString(id) }) { it[sentAt] = System.currentTimeMillis() }
    }
}
