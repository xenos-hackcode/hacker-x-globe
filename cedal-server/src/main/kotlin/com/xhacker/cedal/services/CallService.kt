package com.xhacker.cedal.services

import com.xhacker.cedal.db.PhoneShareOverrides
import com.xhacker.cedal.db.Users
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
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

    // ownerId = the number's owner, viewerId = whoever wants to call them.
    fun canCall(ownerId: UUID, viewerId: UUID): Boolean = transaction {
        if (ownerId == viewerId) return@transaction true
        val override = PhoneShareOverrides.selectAll()
            .where { (PhoneShareOverrides.userId eq ownerId) and (PhoneShareOverrides.friendId eq viewerId) }
            .firstOrNull()
        override?.get(PhoneShareOverrides.allowed)
            ?: Users.selectAll().where { Users.id eq ownerId }.firstOrNull()?.get(Users.shareNumberDefault)
            ?: false
    }
}
