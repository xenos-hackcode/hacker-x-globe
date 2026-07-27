package com.xhacker.cedal.services

import com.xhacker.cedal.db.Appeals
import com.xhacker.cedal.models.AppealDto
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.util.UUID

// The "Appeal" action on a banned/admin-cleared user's full-screen gate
// panel (see AuthService.login's ACCOUNT_BANNED/ACCOUNT_CLEARED sentinels) -
// submitted unauthenticated (identified by email, not a userId, since that
// account either can't log in or no longer exists). Reviewed by the admin
// via Admin Review's Appeals tab.
object AppealService {
    fun submit(email: String, reason: String, message: String): Unit = transaction {
        Appeals.insert {
            it[Appeals.email] = email.trim()
            it[Appeals.reason] = reason
            it[Appeals.message] = message.trim()
            it[createdAt] = System.currentTimeMillis()
        }
    }

    fun list(): List<AppealDto> = transaction {
        Appeals.selectAll().orderBy(Appeals.createdAt, SortOrder.DESC).map { row ->
            AppealDto(
                id = row[Appeals.id].value.toString(),
                email = row[Appeals.email],
                reason = row[Appeals.reason],
                message = row[Appeals.message],
                status = row[Appeals.status],
                createdAt = row[Appeals.createdAt],
            )
        }
    }

    fun dismiss(id: String): Unit = transaction {
        Appeals.update({ Appeals.id eq UUID.fromString(id) }) { it[status] = "reviewed" }
    }
}
