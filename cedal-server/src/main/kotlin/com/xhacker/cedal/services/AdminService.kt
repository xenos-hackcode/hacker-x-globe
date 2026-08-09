package com.xhacker.cedal.services

import com.xhacker.cedal.db.Users
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
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

    @Serializable
    data class GodmodeUserDto(
        val id: String,
        val name: String,
        val email: String? = null,
        val publicId: String? = null,
        val avatarUrl: String? = null,
        val banned: Boolean,
        val banPermanent: Boolean = false,
        val createdAt: Long,
        // Only meaningful on the Developer terminal's "Manage Developer
        // Access" screen - Godmode itself ignores these. See
        // Users.developerAccess/developerKey.
        val developerAccess: Boolean = false,
        val hasActiveDeveloperKey: Boolean = false,
        // Permanent audit trail, not a live preference - see
        // Users.declinedUpdateVersionCode's own doc comment.
        val declinedUpdateVersionCode: Int? = null,
        val declinedUpdateAt: Long? = null,
    )

    // Godmode (chat list > More, admin-only) - every OTHER user on the app,
    // bypassing hideFromSearch entirely (unlike FriendService.search, which
    // deliberately respects it) - admin needs to be able to find someone
    // who's opted out of being found by everyone else too. The admin's own
    // row is deliberately excluded - there's no legitimate reason to ban/
    // clear-data/grant-developer-access against yourself, so it shouldn't
    // even be selectable here. The "Cedal Team" system account (see
    // DeveloperAccessService.TEAM_EMAIL) is excluded for the same reason -
    // it's not a real user, so banning/clearing/granting-developer-access
    // against it makes no sense either. Also reused as-is by
    // DeveloperAccessService's /admin/developer/users endpoint.
    fun listAllUsers(): List<GodmodeUserDto> = transaction {
        Users.selectAll()
            .orderBy(Users.createdAt, org.jetbrains.exposed.sql.SortOrder.DESC)
            .filterNot { it[Users.email]?.equals(ADMIN_EMAIL, ignoreCase = true) == true }
            .filterNot { it[Users.email]?.equals(DeveloperAccessService.TEAM_EMAIL, ignoreCase = true) == true }
            .map { row ->
            GodmodeUserDto(
                id = row[Users.id].value.toString(),
                name = displayNameFor(row),
                email = row[Users.email],
                publicId = row[Users.publicId],
                avatarUrl = row[Users.avatarUrl],
                banned = row[Users.banned],
                banPermanent = row[Users.banPermanent],
                createdAt = row[Users.createdAt],
                developerAccess = row[Users.developerAccess],
                hasActiveDeveloperKey = row[Users.developerKey] != null,
                declinedUpdateVersionCode = row[Users.declinedUpdateVersionCode],
                declinedUpdateAt = row[Users.declinedUpdateAt],
            )
        }
    }

    // Godmode "Clear Data" - snapshot the identity BEFORE
    // AccountService.deleteAccount removes the Users row, so a later login
    // attempt with the same email can show "Admin has deleted your account"
    // instead of a generic "invalid email or password" - see
    // AdminClearedIdentities' own doc comment. Self-service Delete Account
    // does NOT call this (only the admin-triggered path does).
    fun recordClearedTombstone(userId: String) = transaction {
        val uid = UUID.fromString(userId)
        val row = Users.selectAll().where { Users.id eq uid }.firstOrNull() ?: return@transaction
        val email = row[Users.email]
        val phone = row[Users.phoneNumber]
        if (email != null || phone != null) {
            com.xhacker.cedal.db.AdminClearedIdentities.insert {
                it[com.xhacker.cedal.db.AdminClearedIdentities.email] = email
                it[com.xhacker.cedal.db.AdminClearedIdentities.phoneNumber] = phone
                it[clearedAt] = System.currentTimeMillis()
            }
        }
    }

    // Godmode "Ban" - starts as a TEMPORARY ban (banPermanent=false): the
    // user can appeal, and BanEscalationService auto-flips it permanent 24h
    // later unless Unban happens first (see that service's own doc
    // comment). Does NOT touch BannedIdentities yet - that permanent
    // email/phone lockout only ever gets recorded once a ban is (or
    // becomes) permanent, see setPermanentBan/BanEscalationService.
    fun setBanned(userId: String, banned: Boolean) = transaction {
        val uid = UUID.fromString(userId)
        Users.update({ Users.id eq uid }) {
            it[Users.banned] = banned
            it[bannedAt] = if (banned) System.currentTimeMillis() else null
            if (banned) it[banPermanent] = false
        }
    }

    // Godmode "Permanent Ban" (new, admin tools) - skips the 24h grace/
    // appeal window entirely and records the identity lockout immediately.
    fun setPermanentBan(userId: String) = transaction {
        val uid = UUID.fromString(userId)
        Users.update({ Users.id eq uid }) {
            it[Users.banned] = true
            it[bannedAt] = System.currentTimeMillis()
            it[banPermanent] = true
        }
        recordBannedIdentity(uid)
    }

    // Permanent, separate from Users.banned/banPermanent - see
    // BannedIdentities' own doc comment. Shared by setPermanentBan above
    // and BanEscalationService's 24h auto-escalation.
    fun recordBannedIdentity(userId: UUID): Unit = transaction {
        val row = Users.selectAll().where { Users.id eq userId }.firstOrNull() ?: return@transaction
        val email = row[Users.email]
        val phone = row[Users.phoneNumber]
        if (email != null || phone != null) {
            com.xhacker.cedal.db.BannedIdentities.insert {
                it[com.xhacker.cedal.db.BannedIdentities.email] = email
                it[com.xhacker.cedal.db.BannedIdentities.phoneNumber] = phone
                it[bannedAt] = System.currentTimeMillis()
            }
        }
    }
}
