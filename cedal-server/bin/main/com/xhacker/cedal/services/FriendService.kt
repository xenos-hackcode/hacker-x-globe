package com.xhacker.cedal.services

import com.xhacker.cedal.db.FriendRequests
import com.xhacker.cedal.db.Users
import com.xhacker.cedal.models.FriendRequestItem
import com.xhacker.cedal.models.FriendSummary
import com.xhacker.cedal.models.SearchUserResult
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.neq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.util.UUID

// Ported from cedal-mobile's search.tsx / Search.tsx / Requests.tsx (a
// Firestore "friendRequests" collection with live listeners). Here it's a
// plain table + polling from the client instead of push updates — same
// end result (search other nodes, send/accept/decline requests), just
// without a live-listener transport, which cedal-server doesn't have yet.
object FriendService {

    fun search(
        currentUserId: String,
        q: String?,
        byGender: Boolean,
        byOccupation: Boolean,
        byHobby: Boolean,
        byAge: Boolean,
        byBio: Boolean,
    ): List<SearchUserResult> = transaction {
        val uid = UUID.fromString(currentUserId)
        val query = q?.trim()?.lowercase().orEmpty()

        // Users already friends with (accepted, either direction) are hidden —
        // the closest equivalent to cedal-mobile's "already have a room" skip.
        val friendIds = acceptedFriendIds(uid)

        // A blank query with no filters is the "Quick Add" default view (see
        // MemberSearchScreen's Search tab) - real mutual-friend suggestions
        // instead of just listing every account on the server, which used to
        // mean a wall of unrelated/test accounts the moment you opened this
        // screen.
        val noFilters = !byGender && !byOccupation && !byHobby && !byAge && !byBio
        if (query.isEmpty() && noFilters) {
            return@transaction quickAddSuggestions(uid, friendIds)
        }

        Users.selectAll()
            .where { Users.id neq uid }
            .mapNotNull { row ->
                if (row[Users.hideFromSearch]) return@mapNotNull null
                if (row[Users.id].value in friendIds) return@mapNotNull null

                val name = displayNameFor(row)
                val email = row[Users.email]
                val occupation = row[Users.occupation]
                val bio = row[Users.bio]
                val gender = row[Users.gender]
                val hobby = row[Users.hobby]
                val age = row[Users.age]

                val matchesText = query.isEmpty() ||
                    name.lowercase().contains(query) ||
                    (email?.lowercase()?.contains(query) == true) ||
                    (occupation?.lowercase()?.contains(query) == true) ||
                    (bio?.lowercase()?.contains(query) == true)
                if (!matchesText) return@mapNotNull null

                if (byGender && gender.isNullOrBlank()) return@mapNotNull null
                if (byOccupation && occupation.isNullOrBlank()) return@mapNotNull null
                if (byHobby && hobby.isNullOrBlank()) return@mapNotNull null
                if (byAge && age == null) return@mapNotNull null
                if (byBio && bio.isNullOrBlank()) return@mapNotNull null

                SearchUserResult(
                    id = row[Users.id].value.toString(),
                    name = name,
                    email = email,
                    avatarUrl = row[Users.avatarUrl],
                    occupation = occupation,
                    bio = bio,
                    gender = gender,
                    hobby = hobby,
                    age = age,
                )
            }
    }

    private fun acceptedFriendIds(uid: UUID): Set<UUID> =
        FriendRequests.selectAll()
            .where { ((FriendRequests.fromUserId eq uid) or (FriendRequests.toUserId eq uid)) and (FriendRequests.status eq "accepted") }
            .map { row ->
                if (row[FriendRequests.fromUserId].value == uid) row[FriendRequests.toUserId].value
                else row[FriendRequests.fromUserId].value
            }
            .toSet()

    private const val QUICK_ADD_LIMIT = 20

    // Snapchat-style "Quick Add": friends of your friends, ranked by how many
    // mutual connections you share, excluding yourself, existing friends,
    // anyone with a pending request either way, and anyone hiding from
    // search. Empty (not a random fallback list) until you have at least one
    // real friend - deliberately not backfilled with arbitrary accounts,
    // which is exactly the "wall of fake people" this replaced.
    private fun quickAddSuggestions(uid: UUID, friendIds: Set<UUID>): List<SearchUserResult> {
        if (friendIds.isEmpty()) return emptyList()

        val pendingIds = FriendRequests.selectAll()
            .where { ((FriendRequests.fromUserId eq uid) or (FriendRequests.toUserId eq uid)) and (FriendRequests.status eq "pending") }
            .map { row -> if (row[FriendRequests.fromUserId].value == uid) row[FriendRequests.toUserId].value else row[FriendRequests.fromUserId].value }
            .toSet()
        val excluded = friendIds + pendingIds + uid

        val mutualCounts = mutableMapOf<UUID, Int>()
        friendIds.forEach { friendId ->
            acceptedFriendIds(friendId).forEach { candidate ->
                if (candidate !in excluded) mutualCounts[candidate] = (mutualCounts[candidate] ?: 0) + 1
            }
        }

        return mutualCounts.entries
            .sortedByDescending { it.value }
            .take(QUICK_ADD_LIMIT)
            .mapNotNull { (candidateId, _) ->
                val row = Users.selectAll().where { Users.id eq candidateId }.firstOrNull() ?: return@mapNotNull null
                if (row[Users.hideFromSearch]) return@mapNotNull null
                SearchUserResult(
                    id = candidateId.toString(),
                    name = displayNameFor(row),
                    email = row[Users.email],
                    avatarUrl = row[Users.avatarUrl],
                    occupation = row[Users.occupation],
                    bio = row[Users.bio],
                    gender = row[Users.gender],
                    hobby = row[Users.hobby],
                    age = row[Users.age],
                )
            }
    }

    fun sendRequest(fromUserId: String, toUserId: String): Unit = transaction {
        val from = UUID.fromString(fromUserId)
        val to = UUID.fromString(toUserId)
        if (from == to) throw AuthException("Cannot send a request to yourself")
        Users.selectAll().where { Users.id eq to }.firstOrNull() ?: throw AuthException("User not found")

        val existing = FriendRequests.selectAll()
            .where {
                (((FriendRequests.fromUserId eq from) and (FriendRequests.toUserId eq to)) or
                    ((FriendRequests.fromUserId eq to) and (FriendRequests.toUserId eq from))) and
                    (FriendRequests.status neq "declined")
            }.firstOrNull()
        if (existing != null) throw AuthException("A request already exists between you and this user")

        FriendRequests.insert {
            it[FriendRequests.fromUserId] = from
            it[FriendRequests.toUserId] = to
            it[status] = "pending"
            it[createdAt] = System.currentTimeMillis()
        }
    }

    fun listRequests(userId: String): List<FriendRequestItem> = transaction {
        val uid = UUID.fromString(userId)
        FriendRequests.selectAll()
            .where { (FriendRequests.fromUserId eq uid) or (FriendRequests.toUserId eq uid) }
            .map { row ->
                val direction = if (row[FriendRequests.fromUserId].value == uid) "outgoing" else "incoming"
                val otherId = if (direction == "outgoing") row[FriendRequests.toUserId].value else row[FriendRequests.fromUserId].value
                val otherRow = Users.selectAll().where { Users.id eq otherId }.firstOrNull()
                val name = otherRow?.let { displayNameFor(it) } ?: "Unknown"

                FriendRequestItem(
                    id = row[FriendRequests.id].value.toString(),
                    fromUserId = row[FriendRequests.fromUserId].value.toString(),
                    toUserId = row[FriendRequests.toUserId].value.toString(),
                    status = row[FriendRequests.status],
                    direction = direction,
                    name = name,
                    email = otherRow?.get(Users.email),
                    avatarUrl = otherRow?.get(Users.avatarUrl),
                )
            }
    }

    // Backs the Bank "Send" recipient picker — your actual friends list
    // instead of the RN version's raw chat-list scrape.
    fun listFriends(userId: String): List<FriendSummary> = transaction {
        val uid = UUID.fromString(userId)
        FriendRequests.selectAll()
            .where { ((FriendRequests.fromUserId eq uid) or (FriendRequests.toUserId eq uid)) and (FriendRequests.status eq "accepted") }
            .mapNotNull { row ->
                val otherId = if (row[FriendRequests.fromUserId].value == uid) row[FriendRequests.toUserId].value else row[FriendRequests.fromUserId].value
                val otherRow = Users.selectAll().where { Users.id eq otherId }.firstOrNull() ?: return@mapNotNull null
                FriendSummary(id = otherId.toString(), name = displayNameFor(otherRow), email = otherRow[Users.email], avatarUrl = otherRow[Users.avatarUrl])
            }
    }

    fun respond(userId: String, requestId: String, accept: Boolean): Unit = transaction {
        val uid = UUID.fromString(userId)
        val rid = UUID.fromString(requestId)
        val row = FriendRequests.selectAll().where { FriendRequests.id eq rid }.firstOrNull()
            ?: throw AuthException("Request not found")
        if (row[FriendRequests.toUserId].value != uid) throw AuthException("Not your request to respond to")
        FriendRequests.update({ FriendRequests.id eq rid }) { it[status] = if (accept) "accepted" else "declined" }
    }

    fun cancel(userId: String, requestId: String): Unit = transaction {
        val uid = UUID.fromString(userId)
        val rid = UUID.fromString(requestId)
        val row = FriendRequests.selectAll().where { FriendRequests.id eq rid }.firstOrNull()
            ?: throw AuthException("Request not found")
        if (row[FriendRequests.fromUserId].value != uid) throw AuthException("Not your request to cancel")
        FriendRequests.deleteWhere { FriendRequests.id eq rid }
    }
}
