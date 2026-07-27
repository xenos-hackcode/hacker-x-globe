package com.xhacker.cedal.services

import com.xhacker.cedal.db.AppVersionConfig
import com.xhacker.cedal.db.Users
import com.xhacker.cedal.models.AppVersionDto
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.util.UUID

// Client force-update gate - see AppVersionConfig's own doc comment. The
// admin calls set() once after each real release; get() is polled by every
// client to decide whether to show the "update now" toast.
object AppVersionService {
    private const val ROW_ID = "current"

    fun get(): AppVersionDto? = transaction {
        AppVersionConfig.selectAll().where { AppVersionConfig.id eq ROW_ID }.firstOrNull()?.let { row ->
            AppVersionDto(
                versionCode = row[AppVersionConfig.versionCode],
                versionName = row[AppVersionConfig.versionName],
                apkUrl = row[AppVersionConfig.apkUrl],
            )
        }
    }

    fun set(versionCode: Int, versionName: String, apkUrl: String?): Unit = transaction {
        val existing = AppVersionConfig.selectAll().where { AppVersionConfig.id eq ROW_ID }.firstOrNull()
        if (existing == null) {
            AppVersionConfig.insert {
                it[id] = ROW_ID
                it[AppVersionConfig.versionCode] = versionCode
                it[AppVersionConfig.versionName] = versionName
                it[AppVersionConfig.apkUrl] = apkUrl
                it[updatedAt] = System.currentTimeMillis()
            }
        } else {
            AppVersionConfig.update({ AppVersionConfig.id eq ROW_ID }) {
                it[AppVersionConfig.versionCode] = versionCode
                it[AppVersionConfig.versionName] = versionName
                it[AppVersionConfig.apkUrl] = apkUrl
                it[updatedAt] = System.currentTimeMillis()
            }
        }
    }

    // Permanent audit record of dismissing the update banner - see
    // Users.declinedUpdateVersionCode's own doc comment. Overwrites any
    // previous decline with the current one; there's no corresponding
    // "clear" function anywhere in this codebase by design.
    fun recordDeclinedUpdate(userId: String, versionCode: Int): Unit = transaction {
        val uid = UUID.fromString(userId)
        Users.update({ Users.id eq uid }) {
            it[declinedUpdateVersionCode] = versionCode
            it[declinedUpdateAt] = System.currentTimeMillis()
        }
    }
}
