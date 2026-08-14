package com.xhacker.cedal.services

import com.xhacker.cedal.db.Blocks
import com.xhacker.cedal.db.ChatMessages
import com.xhacker.cedal.db.FriendRequests
import com.xhacker.cedal.db.Users
import com.xhacker.cedal.models.FriendRequestItem
import com.xhacker.cedal.models.FriendStatusResult
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
        // Block is mutual invisibility for search purposes (either direction)
        // - once blocked, neither side can find the other again this way,
        // distinct from "Delete User" below, which leaves both fully
        // re-discoverable.
        val blockedIds = blockedEitherDirection(uid)

        // A blank query with no filters is the "Quick Add" default view (see
        // MemberSearchScreen's Search tab) - real mutual-friend suggestions
        // instead of just listing every account on the server, which used to
        // mean a wall of unrelated/test accounts the moment you opened this
        // screen.
        val noFilters = !byGender && !byOccupation && !byHobby && !byAge && !byBio
        if (query.isEmpty() && noFilters) {
            return@transaction quickAddSuggestions(uid, friendIds, blockedIds)
        }

        Users.selectAll()
            .where { Users.id neq uid }
            .mapNotNull { row ->
                if (row[Users.hideFromSearch]) return@mapNotNull null
                if (row[Users.id].value in friendIds) return@mapNotNull null
                if (row[Users.id].value in blockedIds) return@mapNotNull null

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

    // QR add (chat list > ✚ > QR tab) - looks a user up by their publicId
    // directly, the same 8-char code their own QR encodes (see
    // AuthService.generatePublicId). Deliberately does NOT check
    // hideFromSearch - scanning someone's own QR is an explicit, deliberate
    // share on their part, unlike the general search list. Blocking still
    // applies either direction.
    fun findByPublicId(currentUserId: String, code: String): SearchUserResult? = transaction {
        val uid = UUID.fromString(currentUserId)
        val row = Users.selectAll().where { Users.publicId eq code.trim().uppercase() }.firstOrNull() ?: return@transaction null
        if (row[Users.id].value == uid) return@transaction null
        if (row[Users.id].value in blockedEitherDirection(uid)) return@transaction null

        SearchUserResult(
            id = row[Users.id].value.toString(),
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

    // Either side having blocked the other counts - block is mutual
    // invisibility for search, not just "the blocker can't message the
    // blocked" (see Blocks' own doc comment for the messaging-side rule,
    // which stays one-directional).
    private fun blockedEitherDirection(uid: UUID): Set<UUID> {
        val blockedByMe = Blocks.selectAll().where { Blocks.blockerId eq uid }.map { it[Blocks.blockedId].value }
        val blockedMe = Blocks.selectAll().where { Blocks.blockedId eq uid }.map { it[Blocks.blockerId].value }
        return (blockedByMe + blockedMe).toSet()
    }

    private fun acceptedFriendIds(uid: UUID): Set<UUID> =
        FriendRequests.selectAll()
            .where { ((FriendRequests.fromUserId eq uid) or (FriendRequests.toUserId eq uid)) and (FriendRequests.status eq "accepted") }
            .map { row ->
                if (row[FriendRequests.fromUserId].value == uid) row[FriendRequests.toUserId].value
                else row[FriendRequests.fromUserId].value
            }
            .toSet()

    // Chat thread's proactive "this account has been deleted" check - lets
    // the client detect a friend who no longer exists (AccountService.
    // deleteAccount wipes their FriendRequests rows along with everything
    // else) the moment the thread opens, instead of only finding out from a
    // failed send. `exists=false` always implies `isFriend=false` too.
    fun friendStatus(currentUserId: String, otherUserId: String): FriendStatusResult {
        val uid = UUID.fromString(currentUserId)
        val other = runCatching { UUID.fromString(otherUserId) }.getOrNull()
            ?: return FriendStatusResult(exists = false, isFriend = false)
        return transaction {
            val row = Users.selectAll().where { Users.id eq other }.firstOrNull()
            val exists = row != null
            val isFriend = exists && other in acceptedFriendIds(uid)
            val isCedalTeam = row?.get(Users.email)?.equals(DeveloperAccessService.TEAM_EMAIL, ignoreCase = true) == true
            FriendStatusResult(exists = exists, isFriend = isFriend, isCedalTeam = isCedalTeam)
        }
    }

    private const val QUICK_ADD_LIMIT = 20

    // Snapchat-style "Quick Add": friends of your friends, ranked by how many
    // mutual connections you share, excluding yourself, existing friends,
    // anyone with a pending request either way, and anyone hiding from
    // search. Empty (not a random fallback list) until you have at least one
    // real friend - deliberately not backfilled with arbitrary accounts,
    // which is exactly the "wall of fake people" this replaced.
    private fun quickAddSuggestions(uid: UUID, friendIds: Set<UUID>, blockedIds: Set<UUID>): List<SearchUserResult> {
        if (friendIds.isEmpty()) return emptyList()

        val pendingIds = FriendRequests.selectAll()
            .where { ((FriendRequests.fromUserId eq uid) or (FriendRequests.toUserId eq uid)) and (FriendRequests.status eq "pending") }
            .map { row -> if (row[FriendRequests.fromUserId].value == uid) row[FriendRequests.toUserId].value else row[FriendRequests.fromUserId].value }
            .toSet()
        val excluded = friendIds + pendingIds + blockedIds + uid

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

    // Compares by the last 10 digits rather than the raw string, since a
    // phone's contact list and Users.phoneNumber (stored E.164 from
    // PhoneVerification, e.g. "+447476853786") routinely disagree on
    // formatting/country-code prefix for what's really the same number
    // (contacts apps commonly store local numbers like "07476 853786").
    // Not a perfect match (collides across different countries sharing a
    // national number), but the same pragmatic tradeoff most apps make
    // without a full phone-number-parsing library.
    private fun last10Digits(raw: String): String? {
        val digits = raw.filter { it.isDigit() }
        if (digits.length < 7) return null
        return digits.takeLast(10)
    }

    // ✚ > Search's "from your contacts" prompt - matches the device's
    // contact numbers against Cedal accounts, same exclusions as Quick Add
    // (self, existing friends, pending requests either way, blocks, anyone
    // hiding from search) so it only ever surfaces genuinely addable people.
    fun matchContacts(currentUserId: String, phoneNumbers: List<String>): List<SearchUserResult> = transaction {
        val uid = UUID.fromString(currentUserId)
        val wanted = phoneNumbers.mapNotNull { last10Digits(it) }.toSet()
        if (wanted.isEmpty()) return@transaction emptyList()

        val friendIds = acceptedFriendIds(uid)
        val blockedIds = blockedEitherDirection(uid)
        val pendingIds = FriendRequests.selectAll()
            .where { ((FriendRequests.fromUserId eq uid) or (FriendRequests.toUserId eq uid)) and (FriendRequests.status eq "pending") }
            .map { row -> if (row[FriendRequests.fromUserId].value == uid) row[FriendRequests.toUserId].value else row[FriendRequests.fromUserId].value }
            .toSet()
        val excluded = friendIds + blockedIds + pendingIds + uid

        Users.selectAll()
            .where { Users.id neq uid }
            .mapNotNull { row ->
                if (row[Users.id].value in excluded) return@mapNotNull null
                if (row[Users.hideFromSearch]) return@mapNotNull null
                val phone = row[Users.phoneNumber] ?: return@mapNotNull null
                if (last10Digits(phone) !in wanted) return@mapNotNull null

                SearchUserResult(
                    id = row[Users.id].value.toString(),
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
        val toRow = Users.selectAll().where { Users.id eq to }.firstOrNull() ?: throw AuthException("User not found")
        // The well-known "Cedal System" account (see GroupChatService.ensureSystemAccountId)
        // never logs in and isn't a real friend/DM target.
        if (toRow[Users.isSystemAccount]) throw AuthException("This account can't be added as a friend")

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
        val fromName = Users.selectAll().where { Users.id eq from }.firstOrNull()?.let { displayNameFor(it) } ?: "Someone"
        PushNotificationService.send(
            userId = toUserId,
            title = "Friend Request",
            body = "$fromName sent you a friend request",
            type = "friend_request",
            notifyKey = fromUserId,
        )
    }

    // Once BOTH sides have actually sent a real message to each other, an
    // "accepted" request entry is just clutter - they're already an active
    // chat, not something that still needs to be found under Requests.
    // Pending/declined rows are unaffected either way.
    private fun hasMutualMessages(a: UUID, b: UUID): Boolean {
        val aToB = ChatMessages.selectAll().where { (ChatMessages.senderId eq a) and (ChatMessages.receiverId eq b) }.any()
        if (!aToB) return false
        return ChatMessages.selectAll().where { (ChatMessages.senderId eq b) and (ChatMessages.receiverId eq a) }.any()
    }

    fun listRequests(userId: String): List<FriendRequestItem> = transaction {
        val uid = UUID.fromString(userId)
        FriendRequests.selectAll()
            .where { (FriendRequests.fromUserId eq uid) or (FriendRequests.toUserId eq uid) }
            .filterNot { row ->
                row[FriendRequests.status] == "accepted" &&
                    hasMutualMessages(row[FriendRequests.fromUserId].value, row[FriendRequests.toUserId].value)
            }
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
                val canCall = CallService.canCall(otherId, uid)
                FriendSummary(
                    id = otherId.toString(), name = displayNameFor(otherRow), email = otherRow[Users.email], avatarUrl = otherRow[Users.avatarUrl],
                    canCall = canCall, phoneNumber = if (canCall) otherRow[Users.phoneNumber] else null,
                )
            }
    }

    fun respond(userId: String, requestId: String, accept: Boolean): Unit = transaction {
        val uid = UUID.fromString(userId)
        val rid = UUID.fromString(requestId)
        val row = FriendRequests.selectAll().where { FriendRequests.id eq rid }.firstOrNull()
            ?: throw AuthException("Request not found")
        if (row[FriendRequests.toUserId].value != uid) throw AuthException("Not your request to respond to")
        FriendRequests.update({ FriendRequests.id eq rid }) { it[status] = if (accept) "accepted" else "declined" }
        if (accept) {
            // Both sides just made a friend - unlock() is idempotent so this
            // only ever actually fires the popup on each person's first one.
            AchievementService.unlock(uid, "first_friend")
            AchievementService.unlock(row[FriendRequests.fromUserId].value, "first_friend")
            val accepterName = Users.selectAll().where { Users.id eq uid }.firstOrNull()?.let { displayNameFor(it) } ?: "Someone"
            PushNotificationService.send(
                userId = row[FriendRequests.fromUserId].value.toString(),
                title = "Friend Request Accepted",
                body = "$accepterName accepted your friend request",
                type = "friend_request_accepted",
                notifyKey = requestId,
            )
        }
    }

    fun cancel(userId: String, requestId: String): Unit = transaction {
        val uid = UUID.fromString(userId)
        val rid = UUID.fromString(requestId)
        val row = FriendRequests.selectAll().where { FriendRequests.id eq rid }.firstOrNull()
            ?: throw AuthException("Request not found")
        if (row[FriendRequests.fromUserId].value != uid) throw AuthException("Not your request to cancel")
        FriendRequests.deleteWhere { FriendRequests.id eq rid }
    }

    // "Delete User" (profile screen, distinct from Block) - clears the
    // friendship/any pending request between the two, either direction, but
    // deliberately does NOT block anything: both sides stay fully
    // searchable and can send a new friend request to each other again
    // right away. Contrast with ChatService.setBlocked, which also excludes
    // both from ever finding each other in search again.
    fun deleteUser(userId: String, otherUserId: String): Unit = transaction {
        val uid = UUID.fromString(userId)
        val other = UUID.fromString(otherUserId)
        FriendRequests.deleteWhere {
            ((FriendRequests.fromUserId eq uid) and (FriendRequests.toUserId eq other)) or
                ((FriendRequests.fromUserId eq other) and (FriendRequests.toUserId eq uid))
        }
    }
}
