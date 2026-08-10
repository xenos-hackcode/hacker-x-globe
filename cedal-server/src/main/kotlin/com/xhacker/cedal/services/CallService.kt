package com.xhacker.cedal.services

import com.xhacker.cedal.db.FriendRequests
import com.xhacker.cedal.db.PhoneShareOverrides
import com.xhacker.cedal.db.Users
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.util.UUID

// "Known" calling (Settings > Privacy > Share My Number, plus the Call tab
// and DM/Group profile Call buttons) - the native-dialer call mode, distinct
// from "Secretive" (in-app data/video calling, hides your number - see
// planner's left-to-do.md, a later round). A phone number is only ever
// handed back to a specific viewer when canCall resolves true here - same
// global-default + per-friend-override precedence as PopularityService.
object CallService {
    fun setGlobalDefault(userId: String, allowed: Boolean): Unit = transaction {
        Users.update({ Users.id eq UUID.fromString(userId) }) { it[shareNumberDefault] = allowed }
    }

    // allowed=null clears back to "use my default"; true/false always wins
    // over the global default for this one friend.
    fun setOverride(ownerId: String, friendId: String, allowed: Boolean?): Unit = transaction {
        val oid = UUID.fromString(ownerId)
        val fid = UUID.fromString(friendId)
        if (allowed == null) {
            PhoneShareOverrides.deleteWhere { (PhoneShareOverrides.userId eq oid) and (PhoneShareOverrides.friendId eq fid) }
            return@transaction
        }
        val existing = PhoneShareOverrides.selectAll()
            .where { (PhoneShareOverrides.userId eq oid) and (PhoneShareOverrides.friendId eq fid) }
            .firstOrNull()
        if (existing == null) {
            PhoneShareOverrides.insert {
                it[PhoneShareOverrides.userId] = oid
                it[PhoneShareOverrides.friendId] = fid
                it[PhoneShareOverrides.allowed] = allowed
            }
        } else {
            PhoneShareOverrides.update({ (PhoneShareOverrides.userId eq oid) and (PhoneShareOverrides.friendId eq fid) }) {
                it[PhoneShareOverrides.allowed] = allowed
            }
        }
    }

    fun getOverride(ownerId: String, friendId: String): Boolean? = transaction {
        PhoneShareOverrides.selectAll()
            .where { (PhoneShareOverrides.userId eq UUID.fromString(ownerId)) and (PhoneShareOverrides.friendId eq UUID.fromString(friendId)) }
            .firstOrNull()?.get(PhoneShareOverrides.allowed)
    }

    private fun areFriends(a: UUID, b: UUID): Boolean =
        FriendRequests.selectAll().where {
            (((FriendRequests.fromUserId eq a) and (FriendRequests.toUserId eq b)) or
                ((FriendRequests.fromUserId eq b) and (FriendRequests.toUserId eq a))) and
                (FriendRequests.status eq "accepted")
        }.any()

    // ownerId = the number's owner, viewerId = whoever wants to call them.
    // Settings > Call's three deny toggles layer on top of the base share
    // check below - see Users.denyAllCalls's doc comment for the honest
    // limitation (this only gates whether CEDAL reveals the number/shows
    // its own Call button, never a literal call-in-progress block).
    fun canCall(ownerId: UUID, viewerId: UUID): Boolean = transaction {
        if (ownerId == viewerId) return@transaction true
        val owner = Users.selectAll().where { Users.id eq ownerId }.firstOrNull() ?: return@transaction false
        if (owner[Users.denyAllCalls]) return@transaction false

        val override = PhoneShareOverrides.selectAll()
            .where { (PhoneShareOverrides.userId eq ownerId) and (PhoneShareOverrides.friendId eq viewerId) }
            .firstOrNull()
        val baseAllowed = override?.get(PhoneShareOverrides.allowed) ?: owner[Users.shareNumberDefault]
        if (!baseAllowed) return@transaction false

        if (owner[Users.denyNonFriendCalls] && !areFriends(ownerId, viewerId)) return@transaction false

        if (owner[Users.denyUnknownCallers]) {
            val viewerSharesIdentity = Users.selectAll().where { Users.id eq viewerId }.firstOrNull()?.get(Users.shareNumberDefault) ?: false
            if (!viewerSharesIdentity) return@transaction false
        }

        true
    }
}
