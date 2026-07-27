package com.xhacker.cedal.services

import com.xhacker.cedal.db.ChatMessages
import com.xhacker.cedal.db.FriendRequests
import com.xhacker.cedal.db.Users
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.security.SecureRandom
import java.util.UUID

// Developer mode delegation (Developer terminal > "Manage Developer
// Access") - only the app owner can grant/revoke this or issue keys. See
// Users.developerAccess/developerKey and AuthService.verifyNodePassword for
// how a delegated account actually uses a key to get in.
object DeveloperAccessService {
    private val random = SecureRandom()
    private const val KEY_CHARS = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"
    private const val TEAM_DEV_KEY_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"

    // Not private - AdminService (excluding it from the manageable-user
    // list) and FriendService (flagging it to the client so the chat thread
    // can render its read-only action panel) both need to recognize it too.
    const val TEAM_EMAIL = "team@cedalstar.org"

    fun setAccess(userId: String, access: Boolean): Unit = transaction {
        val uid = UUID.fromString(userId)
        Users.update({ Users.id eq uid }) {
            it[developerAccess] = access
            // Revoking also kills any live key outright - no dangling way
            // in once access itself is pulled.
            if (!access) it[developerKey] = null
        }
    }

    // Generates and stores a fresh one-time key, returned here ONLY for the
    // owner to see and relay themselves - never exposed to the delegated
    // account via any other endpoint. Overwrites any previous unused key
    // (only one live key per account at a time).
    fun generateKey(userId: String): String = transaction {
        val uid = UUID.fromString(userId)
        val key = (1..8).map { KEY_CHARS[random.nextInt(KEY_CHARS.length)] }.joinToString("")
        Users.update({ Users.id eq uid }) { it[developerKey] = key }
        key
    }

    // "Cedal Team" - a real Users row (not a special-cased sentinel id), so
    // every existing chat surface (list, thread, avatar/name rendering)
    // just works with zero client changes. Created lazily on first send
    // rather than at server bootstrap - most deployments never need it.
    // hideFromSearch so it can never be found/added like a normal account -
    // the only way it ever appears in someone's chats is this service
    // messaging them directly.
    private fun ensureTeamAccountId(): UUID {
        Users.selectAll().where { Users.email eq TEAM_EMAIL }.firstOrNull()?.let { return it[Users.id].value }
        val devKey = (1..7).map { TEAM_DEV_KEY_CHARS[random.nextInt(TEAM_DEV_KEY_CHARS.length)] }.joinToString("")
        return Users.insertAndGetId {
            it[email] = TEAM_EMAIL
            it[emailVerified] = true
            it[isGuest] = false
            it[Users.devKey] = devKey
            it[nickname] = "Cedal Team"
            it[handle] = "cedal_team"
            it[hideFromSearch] = true
            it[createdAt] = System.currentTimeMillis()
        }.value
    }

    // Sends the freshly-generated key as an in-app DM from "Cedal Team" -
    // see ManageDeveloperAccessScreen's SEND KEY button. Auto-friends the
    // two accounts first (straight to accepted, no request/accept dance)
    // since this is a system-initiated message, not a real user-to-user
    // friend request.
    fun sendKeyMessage(userId: String, key: String): Unit = transaction {
        val uid = UUID.fromString(userId)
        val teamId = ensureTeamAccountId()

        val alreadyFriends = FriendRequests.selectAll()
            .where {
                (((FriendRequests.fromUserId eq teamId) and (FriendRequests.toUserId eq uid)) or
                    ((FriendRequests.fromUserId eq uid) and (FriendRequests.toUserId eq teamId))) and
                    (FriendRequests.status eq "accepted")
            }.any()
        if (!alreadyFriends) {
            FriendRequests.insert {
                it[fromUserId] = teamId
                it[toUserId] = uid
                it[status] = "accepted"
                it[createdAt] = System.currentTimeMillis()
            }
        }

        ChatMessages.insert {
            it[senderId] = teamId
            it[receiverId] = uid
            it[text] = "You've been made a Cedal developer. This is your key:\n\n```key\n$key\n```\n\n" +
                "Keep it safe — we (Cedal Team) will never ask you for it, and you shouldn't give it to " +
                "anyone else either, even someone claiming to be us. For what it's worth, a key only ever " +
                "works for the account it was generated for, so it wouldn't help anyone else even if they " +
                "had it — but don't share it regardless.\n\n" +
                "This key only works for one session: once you close the app, log out, or leave developer " +
                "mode, it stops working and you'll need to come back here and generate a new one."
            it[sentAt] = System.currentTimeMillis()
        }
    }

    // Self-service "Generate Key" (Cedal Team chat's read-only action
    // panel) - lets an already-delegated developer pull a fresh session key
    // themselves instead of asking the owner to do it via Manage Developer
    // Access every time. Only works if they currently have developerAccess
    // - revoked/never-granted accounts can't mint their own way back in.
    fun generateKeyForSelf(userId: String): Unit = transaction {
        val uid = UUID.fromString(userId)
        val hasAccess = Users.selectAll().where { Users.id eq uid }.firstOrNull()?.get(Users.developerAccess) == true
        if (!hasAccess) throw AuthException("You don't have developer access")
        val key = generateKey(userId)
        sendKeyMessage(userId, key)
    }

    // Self-service "Revoke my developer account" (Cedal Team chat's other
    // action) - deliberately irreversible from here on out: the client
    // makes the user confirm before ever calling this, and this posts a
    // confirmation message back so there's a record of it having happened.
    fun revokeSelf(userId: String): Unit = transaction {
        val uid = UUID.fromString(userId)
        val hasAccess = Users.selectAll().where { Users.id eq uid }.firstOrNull()?.get(Users.developerAccess) == true
        if (!hasAccess) throw AuthException("You don't have developer access")
        setAccess(userId, false)
        val teamId = ensureTeamAccountId()
        ChatMessages.insert {
            it[senderId] = teamId
            it[receiverId] = uid
            it[text] = "Your developer access has been revoked, as you requested. This can't be undone from " +
                "here — if you need developer access again, ask the owner to grant it to you."
            it[sentAt] = System.currentTimeMillis()
        }
    }
}
