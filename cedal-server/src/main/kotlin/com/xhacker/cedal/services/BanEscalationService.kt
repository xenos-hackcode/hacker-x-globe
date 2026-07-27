package com.xhacker.cedal.services

import com.xhacker.cedal.db.Users
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

// Godmode "Ban" starts temporary and appealable - this is the 24h clock. Any
// account still banned (Users.banned=true), still only temporary
// (banPermanent=false), and past its 24h mark auto-escalates to permanent.
// The ONLY thing that stops this is Unban within the 24h window - per the
// app owner's own call, dismissing an appeal in Admin Review is just a read
// receipt, it doesn't save or condemn the account either way; only ignoring
// it (or explicitly deciding not to unban) lets the clock run out. Triggered
// by a Cloud Scheduler job hitting /admin/run-ban-escalation, same shared-
// secret pattern as /admin/run-decay - safe to run more often than every 24h
// since it only actually acts on an account once ITS OWN 24h are up.
object BanEscalationService {
    private val TWENTY_FOUR_HOURS_MS = 24L * 60 * 60 * 1000

    fun runDueEscalations(): Int = transaction {
        val now = System.currentTimeMillis()
        val cutoff = now - TWENTY_FOUR_HOURS_MS
        val due = Users.selectAll()
            .where { (Users.banned eq true) and (Users.banPermanent eq false) and (Users.bannedAt less cutoff) }
            .map { it[Users.id].value }
        due.forEach { uid ->
            Users.update({ Users.id eq uid }) { it[banPermanent] = true }
            AdminService.recordBannedIdentity(uid)
        }
        due.size
    }
}
