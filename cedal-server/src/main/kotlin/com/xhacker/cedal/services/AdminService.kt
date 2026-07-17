package com.xhacker.cedal.services

import com.xhacker.cedal.db.Users
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

// The single shared "is this the app's one admin account" check - there's
// exactly one admin (hackerxenos06@gmail.com), not a set of them. Several
// services already had their own private copy of this exact check
// (SystemFeedService, AiChangeRequestService) before this existed; new
// callers (CornealChatService, ArcChatService) use this one instead of
// adding a fourth copy.
object AdminService {
    const val ADMIN_EMAIL = "hackerxenos06@gmail.com"

    fun isAdmin(userId: String): Boolean = transaction {
        val email = Users.selectAll().where { Users.id eq UUID.fromString(userId) }.firstOrNull()?.get(Users.email) ?: return@transaction false
        email.equals(ADMIN_EMAIL, ignoreCase = true)
    }
}
