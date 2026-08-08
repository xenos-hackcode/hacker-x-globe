package com.xhacker.cedal.services

import com.xhacker.cedal.db.BlockedGroups
import com.xhacker.cedal.db.GroupConversationState
import com.xhacker.cedal.db.GroupJoinRequests
import com.xhacker.cedal.db.GroupMembers
import com.xhacker.cedal.db.GroupMessageReactions
import com.xhacker.cedal.db.GroupMessageViews
import com.xhacker.cedal.db.GroupMessages
import com.xhacker.cedal.db.GroupPollVotes
import com.xhacker.cedal.db.GroupRejoinCooldowns
import com.xhacker.cedal.db.GroupReports
import com.xhacker.cedal.db.Groups
import com.xhacker.cedal.db.Users
import com.xhacker.cedal.models.ConversationSummary
import com.xhacker.cedal.models.GroupDto
import com.xhacker.cedal.models.GroupJoinRequestDto
import com.xhacker.cedal.models.GroupLinkPreviewDto
import com.xhacker.cedal.models.GroupMemberDto
import com.xhacker.cedal.models.GroupMessageDto
import com.xhacker.cedal.models.GroupSearchResultDto
import com.xhacker.cedal.models.MediaSummaryDto
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.SqlExpressionBuilder.isNotNull
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.SqlExpressionBuilder.neq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.util.UUID

// Group chat - deliberately a fully separate schema/service from
// ChatService/ChatMessages (see Groups' own doc comment in Tables.kt)
// rather than a retrofit, so this can't destabilize 1-on-1 chat. Roles are
// "CREATOR" | "VICE_CREATOR" | "ADMIN" | "MEMBER" (GroupMembers.role).
// Kick matrix: CREATOR removes anyone but self; VICE_CREATOR removes ADMIN
// or MEMBER; ADMIN removes MEMBER only; MEMBER removes nobody. Nobody can
// remove the CREATOR - the only way that row goes away is the creator
// leaving, which triggers succession (see removeMember). Promote/demote
// matrix (setRole): CREATOR can set anyone to VICE_CREATOR/ADMIN/MEMBER;
// VICE_CREATOR can only move a member between ADMIN and MEMBER (can't
// appoint another VICE_CREATOR or touch the CREATOR). At most one
// VICE_CREATOR at a time - appointing a new one auto-demotes the old one to
// ADMIN. New members added later see full prior history - no per-member
// join-cutoff exists yet. Per-recipient translation is out of scope for v1
// (a group has many recipients with potentially different
// preferredLanguage, so a single translatedText column doesn't generalize
// the way it does for a single 1-on-1 recipient).
object GroupChatService {
    private const val MAX_MESSAGE_LENGTH = 2000
    private val EDIT_WINDOW_MS = 5 * 60 * 1000L
    private const val MAX_VIEW_ONCE_DURATION_MS = 7L * 24 * 60 * 60 * 1000
    private const val MAX_VIEW_ONCE_COUNT = 1000
    private val ADMIN_TIER = setOf("CREATOR", "VICE_CREATOR", "ADMIN")
    // Numeric rank for the 4-tier threshold settings (whoCanSendMessages/
    // whoCanEditInfo/whoCanAddMembers/whoCanSeeGroupStats/whoCanSendMedia) -
    // generalizes what used to be a plain ALL/ADMINS_ONLY binary. A setting
    // of "ADMIN" is satisfied by ADMIN, VICE_CREATOR, and CREATOR (anyone
    // whose rank is >= the threshold's rank).
    private val RANK = mapOf("MEMBER" to 0, "ADMIN" to 1, "VICE_CREATOR" to 2, "CREATOR" to 3)
    private fun roleRank(role: String?): Int = RANK[role] ?: 0
    private fun meetsThreshold(actorRole: String?, threshold: String): Boolean = roleRank(actorRole) >= roleRank(threshold)
    private val RANK_SETTING_KEYS = setOf("whoCanSendMessages", "whoCanEditInfo", "whoCanAddMembers", "whoCanSeeGroupStats", "whoCanSendMedia")
    private const val MIN_AUTO_DELETE_MS = 24L * 60 * 60 * 1000
    private const val MAX_AUTO_DELETE_MS = 365L * 24 * 60 * 60 * 1000
    private const val SYSTEM_ACCOUNT_HANDLE = "cedal.system"
    // Same flat-award convention as LessonService.EXP_PER_LESSON - creating
    // a group chat is a real, meaningful, one-time-per-group action.
    private const val EXP_CREATE_GROUP = 50L
    // Round 5 anti-spam: kicked-or-left, wait this long before being able to
    // rejoin the same group - see recordRejoinCooldown/addMember/approveJoinRequest.
    private const val REJOIN_COOLDOWN_MS = 24L * 60 * 60 * 1000

    private fun parseLockedSettings(csv: String?): List<String> =
        csv?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()

    private fun recordRejoinCooldown(groupId: UUID, userId: UUID) {
        val availableAt = System.currentTimeMillis() + REJOIN_COOLDOWN_MS
        val existing = GroupRejoinCooldowns.selectAll()
            .where { (GroupRejoinCooldowns.groupId eq groupId) and (GroupRejoinCooldowns.userId eq userId) }.firstOrNull()
        if (existing == null) {
            GroupRejoinCooldowns.insert {
                it[GroupRejoinCooldowns.groupId] = groupId
                it[GroupRejoinCooldowns.userId] = userId
                it[GroupRejoinCooldowns.availableAt] = availableAt
            }
        } else {
            GroupRejoinCooldowns.update({ (GroupRejoinCooldowns.groupId eq groupId) and (GroupRejoinCooldowns.userId eq userId) }) {
                it[GroupRejoinCooldowns.availableAt] = availableAt
            }
        }
    }

    private fun checkRejoinCooldown(groupId: UUID, userId: UUID) {
        val row = GroupRejoinCooldowns.selectAll()
            .where { (GroupRejoinCooldowns.groupId eq groupId) and (GroupRejoinCooldowns.userId eq userId) }.firstOrNull() ?: return
        if (row[GroupRejoinCooldowns.availableAt] > System.currentTimeMillis()) {
            throw AuthException("This user left or was removed recently - they can rejoin in 24 hours")
        }
    }

    private fun isMember(groupId: UUID, userId: UUID): Boolean =
        GroupMembers.selectAll().where { (GroupMembers.groupId eq groupId) and (GroupMembers.userId eq userId) }.any()

    private fun memberIdsOf(groupId: UUID): List<UUID> =
        GroupMembers.selectAll().where { GroupMembers.groupId eq groupId }.map { it[GroupMembers.userId].value }

    private fun roleOf(groupId: UUID, userId: UUID): String? =
        GroupMembers.selectAll().where { (GroupMembers.groupId eq groupId) and (GroupMembers.userId eq userId) }
            .firstOrNull()?.get(GroupMembers.role)

    private fun membersOf(groupId: UUID): List<GroupMemberDto> =
        GroupMembers.selectAll().where { GroupMembers.groupId eq groupId }
            .orderBy(GroupMembers.joinedAt, SortOrder.ASC)
            .map { GroupMemberDto(it[GroupMembers.userId].value.toString(), it[GroupMembers.role], it[GroupMembers.joinedAt]) }

    // Precedence when `fromUserId` wants to DM `toUserId` via a group's
    // "Message" action: toUserId's own global Users.dmClosed always wins;
    // else the group's Groups.dmClosedByCreator always wins; else toUserId's
    // own per-group GroupConversationState.dmOverride ("CLOSED" blocks,
    // "OPEN"/null allows). Deliberately scoped to just that one button - see
    // this plan's Round-3 design decision, doesn't touch ChatService at all.
    private fun canDm(groupId: UUID, fromUserId: UUID, toUserId: UUID, dmClosedByCreator: Boolean): Boolean {
        if (fromUserId == toUserId) return false
        val toDmClosed = Users.selectAll().where { Users.id eq toUserId }.firstOrNull()?.get(Users.dmClosed) ?: false
        if (toDmClosed) return false
        if (dmClosedByCreator) return false
        val override = GroupConversationState.selectAll()
            .where { (GroupConversationState.userId eq toUserId) and (GroupConversationState.groupId eq groupId) }
            .firstOrNull()?.get(GroupConversationState.dmOverride)
        return override != "CLOSED"
    }

    private fun buildGroupDto(groupId: UUID, viewerId: UUID): GroupDto {
        val group = Groups.selectAll().where { Groups.id eq groupId }.firstOrNull() ?: throw AuthException("Group not found")
        val viewerRole = roleOf(groupId, viewerId)
        val statsThreshold = group[Groups.whoCanSeeGroupStats]
        val dmClosedByCreator = group[Groups.dmClosedByCreator]
        val members = if (meetsThreshold(viewerRole, statsThreshold)) {
            membersOf(groupId).map { m ->
                val mid = UUID.fromString(m.userId)
                val canCall = mid != viewerId && CallService.canCall(mid, viewerId)
                m.copy(
                    canDm = canDm(groupId, viewerId, mid, dmClosedByCreator),
                    canCall = canCall,
                    phoneNumber = if (canCall) Users.selectAll().where { Users.id eq mid }.firstOrNull()?.get(Users.phoneNumber) else null,
                )
            }
        } else {
            emptyList()
        }
        val myState = GroupConversationState.selectAll()
            .where { (GroupConversationState.userId eq viewerId) and (GroupConversationState.groupId eq groupId) }
            .firstOrNull()
        // Lazily generated the first time any group needs one - same "no
        // migration, just seed on first use" convention as everything else
        // in this codebase. Both public and private groups get a token, but
        // who's allowed to actually SEE it differs: public groups show it to
        // every member (also discoverable/joinable via search - no approval
        // needed either way), while a private group's link is admin-tier
        // only and joining through it still requires an admin-tier approval
        // (see requestToJoin/getGroupByToken below) - it's a manual-invite
        // channel, not a way around the group staying unsearchable.
        val storedToken = group[Groups.inviteToken] ?: run {
            val token = java.util.UUID.randomUUID().toString().replace("-", "")
            Groups.update({ Groups.id eq groupId }) { it[Groups.inviteToken] = token }
            token
        }
        val inviteToken = if (group[Groups.isPublic] || viewerRole in ADMIN_TIER) storedToken else null
        return GroupDto(
            id = groupId.toString(),
            name = group[Groups.name],
            creatorId = group[Groups.creatorId].value.toString(),
            avatarUrl = group[Groups.avatarUrl],
            description = group[Groups.description],
            whoCanSendMessages = group[Groups.whoCanSendMessages],
            whoCanEditInfo = group[Groups.whoCanEditInfo],
            whoCanAddMembers = group[Groups.whoCanAddMembers],
            whoCanSeeGroupStats = statsThreshold,
            whoCanSendMedia = group[Groups.whoCanSendMedia],
            shareHistoryWithNewMembers = group[Groups.shareHistoryWithNewMembers],
            isPublic = group[Groups.isPublic],
            pinnedMessageId = group[Groups.pinnedMessageId]?.toString(),
            pinnedByRole = group[Groups.pinnedByRole],
            securedMode = group[Groups.securedMode],
            disappearingMessagesDurationMs = group[Groups.disappearingMessagesDurationMs],
            muted = myState?.get(GroupConversationState.muted) ?: false,
            lockedSettings = parseLockedSettings(group[Groups.lockedSettings]),
            rules = group[Groups.rules],
            autoDeleteAt = group[Groups.autoDeleteAt],
            dmClosedByCreator = dmClosedByCreator,
            callsEnabled = group[Groups.callsEnabled],
            myDmOverride = myState?.get(GroupConversationState.dmOverride),
            inviteToken = inviteToken,
            members = members,
            createdAt = group[Groups.createdAt],
        )
    }

    fun createGroup(creatorId: String, name: String, memberIds: List<String>): GroupDto = transaction {
        val creator = UUID.fromString(creatorId)
        val trimmedName = name.trim().take(100)
        if (trimmedName.isBlank()) throw AuthException("Group needs a name")
        val members = (memberIds.map { UUID.fromString(it) } + creator).distinct()
        if (members.size < 3) throw AuthException("A group needs at least 2 other members")

        val now = System.currentTimeMillis()
        val groupId = Groups.insertAndGetId {
            it[Groups.name] = trimmedName
            it[Groups.creatorId] = creator
            it[Groups.createdAt] = now
        }.value
        members.forEach { memberId ->
            GroupMembers.insert {
                it[GroupMembers.groupId] = groupId
                it[GroupMembers.userId] = memberId
                it[role] = if (memberId == creator) "CREATOR" else "MEMBER"
                it[joinedAt] = now
            }
        }
        // Same flat-award-plus-rank-check pattern as LessonService/
        // ArcOpsService/DailyTaskService - creating a group is a real,
        // one-time, meaningful action worth Profile rank exp. Deliberately
        // NOT idempotent-checked against a "first group ever" table -
        // every group creation awards it, same as every lesson completion
        // does (LessonService IS idempotent per-lesson because the same
        // lesson can be re-toggled; there's no equivalent replay risk here).
        val currentExp = Users.selectAll().where { Users.id eq creator }.first()[Users.exp]
        val newExp = currentExp + EXP_CREATE_GROUP
        Users.update({ Users.id eq creator }) { it[Users.exp] = newExp }
        RankUpService.checkRankUp(creator, currentExp, newExp)
        buildGroupDto(groupId, creator)
    }

    fun getGroup(groupId: String, userId: String): GroupDto = transaction {
        val gid = UUID.fromString(groupId)
        val uid = UUID.fromString(userId)
        if (!isMember(gid, uid)) throw AuthException("You're not a member of this group")
        val autoDeleteAt = Groups.selectAll().where { Groups.id eq gid }.first()[Groups.autoDeleteAt]
        if (autoDeleteAt != null && System.currentTimeMillis() > autoDeleteAt) {
            deleteGroupFully(gid)
            throw AuthException("This group has been deleted")
        }
        buildGroupDto(gid, uid)
    }

    fun updateGroupInfo(groupId: String, actingUserId: String, name: String?, description: String?, avatarUrl: String?, rules: String?): GroupDto = transaction {
        val gid = UUID.fromString(groupId)
        val actor = UUID.fromString(actingUserId)
        val group = Groups.selectAll().where { Groups.id eq gid }.firstOrNull() ?: throw AuthException("Group not found")
        val actorRole = roleOf(gid, actor) ?: throw AuthException("You're not a member of this group")
        if (!meetsThreshold(actorRole, group[Groups.whoCanEditInfo])) {
            throw AuthException("Only group admins can edit group info")
        }
        val trimmedName = name?.trim()?.take(100)
        if (trimmedName != null && trimmedName.isBlank()) throw AuthException("Group needs a name")
        val trimmedDescription = description?.trim()?.take(500)
        val trimmedRules = rules?.trim()?.take(1000)

        Groups.update({ Groups.id eq gid }) { stmt ->
            trimmedName?.let { stmt[Groups.name] = it }
            if (description != null) stmt[Groups.description] = trimmedDescription
            if (avatarUrl != null) stmt[Groups.avatarUrl] = avatarUrl
            if (rules != null) stmt[Groups.rules] = trimmedRules
        }
        buildGroupDto(gid, actor)
    }

    fun updateGroupSettings(
        groupId: String,
        actingUserId: String,
        whoCanSendMessages: String?,
        whoCanEditInfo: String?,
        whoCanAddMembers: String?,
        whoCanSeeGroupStats: String?,
        whoCanSendMedia: String?,
        shareHistoryWithNewMembers: Boolean?,
        isPublic: Boolean?,
        securedMode: Boolean?,
        disappearingMessagesDurationMs: Long?,
        disappearingMessagesOff: Boolean,
        lockedSettings: List<String>?,
        autoDeleteDurationMs: Long?,
        autoDeleteOff: Boolean,
        dmClosedByCreator: Boolean?,
        callsEnabled: Boolean?,
    ): GroupDto = transaction {
        val gid = UUID.fromString(groupId)
        val actor = UUID.fromString(actingUserId)
        val group = Groups.selectAll().where { Groups.id eq gid }.firstOrNull() ?: throw AuthException("Group not found")
        val actorRole = roleOf(gid, actor) ?: throw AuthException("You're not a member of this group")
        if (actorRole !in ADMIN_TIER) throw AuthException("Only group admins can change group settings")

        val currentLocks = parseLockedSettings(group[Groups.lockedSettings]).toMutableSet()
        val rankChanges = mapOf(
            "whoCanSendMessages" to whoCanSendMessages, "whoCanEditInfo" to whoCanEditInfo,
            "whoCanAddMembers" to whoCanAddMembers, "whoCanSeeGroupStats" to whoCanSeeGroupStats,
            "whoCanSendMedia" to whoCanSendMedia,
        )
        rankChanges.forEach { (key, value) ->
            if (value == null) return@forEach
            if (value !in RANK.keys) throw AuthException("Invalid setting value")
            // Round 5: editing group info (name/avatar/description/rules)
            // is narrower than the other 4 rank-threshold settings - can
            // only ever be handed to VICE_CREATOR or CREATOR, never
            // ADMIN/MEMBER, regardless of who's setting it.
            if (key == "whoCanEditInfo" && value !in setOf("VICE_CREATOR", "CREATOR")) {
                throw AuthException("Who can edit group info can only be set to Vice-Creator or Creator")
            }
            if (key in currentLocks && roleRank(actorRole) < RANK.getValue("VICE_CREATOR")) {
                throw AuthException("This setting is locked - only the Creator or Vice-Creator can change it")
            }
        }
        if (lockedSettings != null && roleRank(actorRole) < RANK.getValue("VICE_CREATOR")) {
            throw AuthException("Only the Creator or Vice-Creator can lock or unlock settings")
        }
        if (dmClosedByCreator != null && actorRole != "CREATOR") throw AuthException("Only the Creator can change the group DM setting")
        if (callsEnabled != null && actorRole != "CREATOR") throw AuthException("Only the Creator can lock Group Calls")

        var autoDeleteAt: Long? = null
        if (autoDeleteDurationMs != null) {
            if (actorRole != "CREATOR") throw AuthException("Only the Creator can set group auto-delete")
            if (autoDeleteDurationMs < MIN_AUTO_DELETE_MS || autoDeleteDurationMs > MAX_AUTO_DELETE_MS) {
                throw AuthException("Auto-delete must be between 1 day and 1 year")
            }
            autoDeleteAt = System.currentTimeMillis() + autoDeleteDurationMs
        }
        if (autoDeleteOff && actorRole != "CREATOR") throw AuthException("Only the Creator can change group auto-delete")

        if (lockedSettings != null) {
            currentLocks.clear()
            currentLocks.addAll(lockedSettings.filter { it in RANK_SETTING_KEYS })
        }

        Groups.update({ Groups.id eq gid }) { stmt ->
            whoCanSendMessages?.let { stmt[Groups.whoCanSendMessages] = it }
            whoCanEditInfo?.let { stmt[Groups.whoCanEditInfo] = it }
            whoCanAddMembers?.let { stmt[Groups.whoCanAddMembers] = it }
            whoCanSeeGroupStats?.let { stmt[Groups.whoCanSeeGroupStats] = it }
            whoCanSendMedia?.let { stmt[Groups.whoCanSendMedia] = it }
            shareHistoryWithNewMembers?.let { stmt[Groups.shareHistoryWithNewMembers] = it }
            isPublic?.let { stmt[Groups.isPublic] = it }
            securedMode?.let { stmt[Groups.securedMode] = it }
            if (disappearingMessagesOff) stmt[Groups.disappearingMessagesDurationMs] = null
            else disappearingMessagesDurationMs?.let { stmt[Groups.disappearingMessagesDurationMs] = it }
            if (lockedSettings != null) stmt[Groups.lockedSettings] = currentLocks.joinToString(",").ifBlank { null }
            if (autoDeleteOff) stmt[Groups.autoDeleteAt] = null
            else autoDeleteAt?.let { stmt[Groups.autoDeleteAt] = it }
            dmClosedByCreator?.let { stmt[Groups.dmClosedByCreator] = it }
            callsEnabled?.let { stmt[Groups.callsEnabled] = it }
        }
        buildGroupDto(gid, actor)
    }

    fun addMember(groupId: String, actingUserId: String, newMemberId: String): Unit = transaction {
        val gid = UUID.fromString(groupId)
        val actor = UUID.fromString(actingUserId)
        val newMember = UUID.fromString(newMemberId)
        val group = Groups.selectAll().where { Groups.id eq gid }.firstOrNull() ?: throw AuthException("Group not found")
        val actorRole = roleOf(gid, actor) ?: throw AuthException("You're not a member of this group")
        if (!meetsThreshold(actorRole, group[Groups.whoCanAddMembers])) {
            throw AuthException("Only group admins can add members to this group")
        }
        if (isMember(gid, newMember)) throw AuthException("Already a member of this group")
        if (isGroupBlocked(gid, newMember)) throw AuthException("This user has blocked this group")
        checkRejoinCooldown(gid, newMember)
        GroupMembers.insert {
            it[GroupMembers.groupId] = gid
            it[GroupMembers.userId] = newMember
            it[role] = "MEMBER"
            it[joinedAt] = System.currentTimeMillis()
        }
    }

    // Handles both "leave" (actor removes themselves) and someone with
    // sufficient authority removing another member - the only two ways a
    // membership row can go away. If the CREATOR leaves and other members
    // remain, ownership transfers (VICE_CREATOR > longest-tenured ADMIN >
    // longest-tenured MEMBER) instead of the group dying with them. The
    // group is only actually deleted once the last member is gone.
    fun removeMember(groupId: String, actingUserId: String, targetUserId: String): Unit = transaction {
        val gid = UUID.fromString(groupId)
        val actor = UUID.fromString(actingUserId)
        val target = UUID.fromString(targetUserId)
        Groups.selectAll().where { Groups.id eq gid }.firstOrNull() ?: throw AuthException("Group not found")
        val targetRole = roleOf(gid, target) ?: throw AuthException("Not a member of this group")
        val selfLeaving = actor == target

        if (!selfLeaving) {
            val actorRole = roleOf(gid, actor) ?: throw AuthException("You're not a member of this group")
            val allowed = when (actorRole) {
                "CREATOR" -> targetRole != "CREATOR"
                "VICE_CREATOR" -> targetRole == "ADMIN" || targetRole == "MEMBER"
                "ADMIN" -> targetRole == "MEMBER"
                else -> false
            }
            if (!allowed) throw AuthException("You don't have permission to remove this member")
        }

        // Creator-self-leave no longer auto-picks a successor by tenure -
        // see leaveGroup, which requires an explicit choice all the way
        // through the rank chain (Round-2 "Creator-leave rework").
        if (selfLeaving && targetRole == "CREATOR") {
            throw AuthException("Use the Leave Group flow to choose what happens to the group")
        }

        GroupMembers.deleteWhere { (GroupMembers.groupId eq gid) and (GroupMembers.userId eq target) }
        // Anti-spam (Round 5): applies whether this was a kick or a
        // voluntary leave - see checkRejoinCooldown in addMember/approveJoinRequest.
        recordRejoinCooldown(gid, target)

        if (memberIdsOf(gid).isEmpty()) {
            deleteGroupFully(gid)
        }
    }

    // Full teardown - messages/reactions/poll-votes/views, ALL memberships,
    // and every Round-2 per-group table, then the group row itself. Reused
    // by: this file's own empty-group cleanup, leaveGroup(dissolve=true),
    // and the auto-delete lazy-purge in getGroup/listMyGroupSummaries. NOT
    // used by clearChat, which deliberately keeps the group/members.
    private fun deleteGroupFully(gid: UUID) {
        val messageIds = GroupMessages.selectAll().where { GroupMessages.groupId eq gid }.map { it[GroupMessages.id].value }
        if (messageIds.isNotEmpty()) {
            GroupMessageReactions.deleteWhere { GroupMessageReactions.messageId inList messageIds }
            GroupPollVotes.deleteWhere { GroupPollVotes.messageId inList messageIds }
            GroupMessageViews.deleteWhere { GroupMessageViews.messageId inList messageIds }
            GroupMessages.deleteWhere { GroupMessages.id inList messageIds }
        }
        GroupMembers.deleteWhere { GroupMembers.groupId eq gid }
        GroupJoinRequests.deleteWhere { GroupJoinRequests.groupId eq gid }
        GroupReports.deleteWhere { GroupReports.groupId eq gid }
        BlockedGroups.deleteWhere { BlockedGroups.groupId eq gid }
        GroupConversationState.deleteWhere { GroupConversationState.groupId eq gid }
        // Cooldown rows for a group that no longer exists are harmless
        // dangling data, but untidy - clean them up here so a dissolved/
        // auto-deleted group doesn't leave rejoin cooldowns behind forever.
        GroupRejoinCooldowns.deleteWhere { GroupRejoinCooldowns.groupId eq gid }
        Groups.deleteWhere { Groups.id eq gid }
    }

    // The Creator-only entry point for leaving - see this plan's Round-2
    // "Creator-leave rework" design decision for the full branching
    // rationale. Plain members/admins keep using removeMember(actor==target).
    fun leaveGroup(
        groupId: String,
        userId: String,
        dissolve: Boolean,
        successorId: String? = null,
        random: Boolean = false,
        systemOwner: Boolean = false,
        securedMode: Boolean? = null,
        isPublic: Boolean? = null,
    ): Unit = transaction {
        val gid = UUID.fromString(groupId)
        val actor = UUID.fromString(userId)
        val group = Groups.selectAll().where { Groups.id eq gid }.firstOrNull() ?: throw AuthException("Group not found")
        if (group[Groups.creatorId].value != actor) throw AuthException("Only the group's Creator uses this")

        if (dissolve) {
            deleteGroupFully(gid)
            return@transaction
        }

        val others = GroupMembers.selectAll().where { (GroupMembers.groupId eq gid) and (GroupMembers.userId neq actor) }.toList()
        val successor: UUID = when {
            others.any { it[GroupMembers.role] == "VICE_CREATOR" } ->
                others.first { it[GroupMembers.role] == "VICE_CREATOR" }[GroupMembers.userId].value
            others.any { it[GroupMembers.role] == "ADMIN" } -> {
                val admins = others.filter { it[GroupMembers.role] == "ADMIN" }
                when {
                    random -> admins.random()[GroupMembers.userId].value
                    successorId != null -> {
                        val chosen = UUID.fromString(successorId)
                        if (admins.none { it[GroupMembers.userId].value == chosen }) throw AuthException("Chosen successor isn't an admin of this group")
                        chosen
                    }
                    else -> throw AuthException("Choose an admin to become the new creator, or pick Random")
                }
            }
            others.isNotEmpty() -> when {
                random -> others.random()[GroupMembers.userId].value
                successorId != null -> {
                    val chosen = UUID.fromString(successorId)
                    if (others.none { it[GroupMembers.userId].value == chosen }) throw AuthException("Chosen successor isn't a member of this group")
                    chosen
                }
                else -> throw AuthException("Choose who becomes the new creator, or pick Random")
            }
            systemOwner -> ensureSystemAccountId()
            else -> throw AuthException("Choose Dissolve Group or System Owner - there's nobody left to hand this group to")
        }

        GroupMembers.deleteWhere { (GroupMembers.groupId eq gid) and (GroupMembers.userId eq actor) }
        recordRejoinCooldown(gid, actor)
        if (isMember(gid, successor)) {
            GroupMembers.update({ (GroupMembers.groupId eq gid) and (GroupMembers.userId eq successor) }) { it[role] = "CREATOR" }
        } else {
            GroupMembers.insert {
                it[GroupMembers.groupId] = gid
                it[GroupMembers.userId] = successor
                it[role] = "CREATOR"
                it[joinedAt] = System.currentTimeMillis()
            }
        }
        Groups.update({ Groups.id eq gid }) { stmt ->
            stmt[creatorId] = successor
            securedMode?.let { stmt[Groups.securedMode] = it }
            isPublic?.let { stmt[Groups.isPublic] = it }
        }
    }

    // Lazily seeds the one well-known "Cedal System" account the first time
    // a group needs it as a fallback owner (see leaveGroup's systemOwner
    // branch) - null email/passwordHash already means "can never log in" in
    // this schema, nothing extra needed. Same lazy-seed convention this
    // codebase uses everywhere else instead of a startup migration.
    private fun ensureSystemAccountId(): UUID {
        Users.selectAll().where { Users.isSystemAccount eq true }.firstOrNull()?.let { return it[Users.id].value }
        return Users.insertAndGetId {
            it[Users.isSystemAccount] = true
            it[Users.nickname] = "Cedal System"
            it[Users.handle] = SYSTEM_ACCOUNT_HANDLE
            it[Users.devKey] = "SYSTEM0"
            // Reuses the existing "Friend Hider" flag (see its own doc
            // comment in Tables.kt) to keep this out of every friend
            // search/suggestion result - no separate guard needed, this IS
            // what that flag is for.
            it[Users.hideFromSearch] = true
            it[Users.createdAt] = System.currentTimeMillis()
        }.value
    }

    // Promote/demote - see this file's own doc comment for the exact
    // matrix. Can't be used to set/clear CREATOR (that only ever happens via
    // group creation or removeMember's succession logic).
    fun setRole(groupId: String, actingUserId: String, targetUserId: String, newRole: String): Unit = transaction {
        val gid = UUID.fromString(groupId)
        val actor = UUID.fromString(actingUserId)
        val target = UUID.fromString(targetUserId)
        if (newRole !in setOf("VICE_CREATOR", "ADMIN", "MEMBER")) throw AuthException("Invalid role")
        if (actor == target) throw AuthException("You can't change your own role")

        val actorRole = roleOf(gid, actor) ?: throw AuthException("You're not a member of this group")
        val targetRole = roleOf(gid, target) ?: throw AuthException("Not a member of this group")
        if (targetRole == "CREATOR") throw AuthException("Can't change the creator's role")

        when (actorRole) {
            "CREATOR" -> {}
            "VICE_CREATOR" -> {
                if (newRole == "VICE_CREATOR") throw AuthException("Only the creator can appoint a vice-creator")
                if (targetRole != "ADMIN" && targetRole != "MEMBER") throw AuthException("You don't have permission to change this member's role")
            }
            else -> throw AuthException("You don't have permission to change roles")
        }

        if (newRole == "VICE_CREATOR") {
            GroupMembers.update({ (GroupMembers.groupId eq gid) and (GroupMembers.role eq "VICE_CREATOR") }) { it[role] = "ADMIN" }
        }
        GroupMembers.update({ (GroupMembers.groupId eq gid) and (GroupMembers.userId eq target) }) { it[role] = newRole }
    }

    private fun reactionsFor(messageIds: Collection<UUID>): Map<UUID, Map<String, String>> {
        if (messageIds.isEmpty()) return emptyMap()
        val result = mutableMapOf<UUID, MutableMap<String, String>>()
        GroupMessageReactions.selectAll()
            .where { GroupMessageReactions.messageId inList messageIds }
            .forEach { row ->
                val mid = row[GroupMessageReactions.messageId].value
                result.getOrPut(mid) { mutableMapOf() }[row[GroupMessageReactions.userId].value.toString()] = row[GroupMessageReactions.emoji]
            }
        return result
    }

    private fun pollVotesFor(messageIds: Collection<UUID>): Map<UUID, Map<String, Int>> {
        if (messageIds.isEmpty()) return emptyMap()
        val result = mutableMapOf<UUID, MutableMap<String, Int>>()
        GroupPollVotes.selectAll()
            .where { GroupPollVotes.messageId inList messageIds }
            .forEach { row ->
                val mid = row[GroupPollVotes.messageId].value
                result.getOrPut(mid) { mutableMapOf() }[row[GroupPollVotes.userId].value.toString()] = row[GroupPollVotes.optionIndex]
            }
        return result
    }

    // Which of messageIds this ONE viewer has revealed at least once - see
    // toDto's hideContent computation and GroupMessageViews' doc comment.
    private fun viewedMessageIds(messageIds: Collection<UUID>, viewerId: UUID): Set<UUID> {
        if (messageIds.isEmpty()) return emptySet()
        return GroupMessageViews.selectAll()
            .where { (GroupMessageViews.messageId inList messageIds) and (GroupMessageViews.userId eq viewerId) }
            .map { it[GroupMessageViews.messageId].value }
            .toSet()
    }

    // viewerId-aware, unlike 1-on-1's toDto: a view-once message's real
    // content is hidden from every non-sender viewer until THEY personally
    // reveal it (see revealGroupMessage) - the sender always sees their own
    // content, which is the one deliberate deviation from ChatService's
    // "hidden from everyone including the sender" behavior, since a group
    // has many independent viewers and the sender obviously already knows
    // what they sent. Classic "once" mode unlocks permanently for a viewer
    // after their first reveal; the custom modes never leak real content
    // through a regular fetch - every view has to go through
    // revealGroupMessage so it can enforce the count/time limit each time.
    private fun toDto(
        row: ResultRow,
        viewerId: UUID,
        reactions: Map<String, String>,
        pollVotes: Map<String, Int> = emptyMap(),
        hasViewed: Boolean = false,
    ): GroupMessageDto {
        val isDeleted = row[GroupMessages.deleted]
        val isViewOnce = row[GroupMessages.viewOnce]
        val isSender = row[GroupMessages.senderId].value == viewerId
        val mode = row[GroupMessages.viewOnceMode]
        val hideViewOnce = !isDeleted && isViewOnce && !isSender && when (mode) {
            "custom_count", "custom_time" -> true
            else -> !hasViewed
        }
        // Private tag hide is permanent (no reveal/consume/expiry, unlike
        // View Once) - just omitted from the DTO for every non-sender,
        // non-tagged viewer. See Round-2/4 "Private tags" design decisions.
        val taggedUuids = row[GroupMessages.taggedUserIds]?.split(",")?.filter { it.isNotBlank() }?.map { UUID.fromString(it) } ?: emptyList()
        val isTagPrivate = !isDeleted && row[GroupMessages.tagPrivate]
        val tagHidden = isTagPrivate && !isSender && viewerId !in taggedUuids
        val hideContent = hideViewOnce || tagHidden
        return GroupMessageDto(
            id = row[GroupMessages.id].value.toString(),
            groupId = row[GroupMessages.groupId].value.toString(),
            senderId = row[GroupMessages.senderId].value.toString(),
            text = if (isDeleted || hideContent) "" else row[GroupMessages.text],
            sentAt = row[GroupMessages.sentAt],
            editedAt = row[GroupMessages.editedAt],
            deleted = isDeleted,
            replyToId = row[GroupMessages.replyToId]?.toString(),
            reactions = if (isDeleted) emptyMap() else reactions,
            isSticker = row[GroupMessages.isSticker],
            mediaUrl = if (isDeleted || hideContent) null else row[GroupMessages.mediaUrl],
            mediaType = row[GroupMessages.mediaType],
            fileName = row[GroupMessages.fileName],
            mediaSizeBytes = row[GroupMessages.mediaSizeBytes],
            viewOnce = isViewOnce,
            viewed = isSender || hasViewed,
            viewOnceMode = mode,
            viewOnceDurationMs = row[GroupMessages.viewOnceDurationMs],
            viewOnceMaxViews = row[GroupMessages.viewOnceMaxViews],
            kept = row[GroupMessages.kept],
            taggedUserIds = taggedUuids.map { it.toString() },
            tagAll = row[GroupMessages.tagAll],
            tagPrivate = row[GroupMessages.tagPrivate],
            tagHidden = tagHidden,
            pollQuestion = if (isDeleted || hideContent) null else row[GroupMessages.pollQuestion],
            pollOptions = if (isDeleted || hideContent) null else row[GroupMessages.pollOptions]?.split("\n"),
            pollVotes = if (isDeleted) emptyMap() else pollVotes,
        )
    }

    fun sendGroupMessage(
        groupId: String,
        fromUserId: String,
        text: String,
        replyToId: String?,
        isSticker: Boolean = false,
        mediaUrl: String? = null,
        mediaType: String? = null,
        fileName: String? = null,
        mediaSizeBytes: Long? = null,
        viewOnce: Boolean = false,
        viewOnceMode: String? = null,
        viewOnceDurationMs: Long? = null,
        viewOnceMaxViews: Int? = null,
        pollQuestion: String? = null,
        pollOptions: List<String>? = null,
        taggedUserIds: List<String> = emptyList(),
        tagAll: Boolean = false,
        tagPrivate: Boolean = false,
        disappearDurationMs: Long? = null,
        disappearSelfOnly: Boolean = false,
    ): GroupMessageDto = transaction {
        val gid = UUID.fromString(groupId)
        val from = UUID.fromString(fromUserId)
        val group = Groups.selectAll().where { Groups.id eq gid }.firstOrNull() ?: throw AuthException("Group not found")
        val fromRole = roleOf(gid, from) ?: throw AuthException("You're not a member of this group")
        if (!meetsThreshold(fromRole, group[Groups.whoCanSendMessages])) {
            throw AuthException("Only group admins can send messages in this group")
        }
        if (mediaUrl != null && !meetsThreshold(fromRole, group[Groups.whoCanSendMedia])) {
            throw AuthException("You don't have permission to send media in this group")
        }
        val taggedUuids = taggedUserIds.map { UUID.fromString(it) }.distinct()
        taggedUuids.forEach { uid ->
            if (!isMember(gid, uid)) throw AuthException("That user isn't a member of this group")
            val targetNoTag = Users.selectAll().where { Users.id eq uid }.firstOrNull()?.get(Users.noTag) ?: false
            if (targetNoTag) throw AuthException("This member doesn't accept tags")
        }
        // The composer's hide choice only holds if EVERY tagged user allows
        // it (Users.hiderEnabled) - one holdout silently downgrades the
        // whole message to public rather than rejecting the send (Round-4
        // "Hider" precedence - a message has one shared body, so true
        // per-recipient mixed visibility isn't representable).
        val allTaggedAllowHiding = taggedUuids.isNotEmpty() && taggedUuids.all { uid ->
            Users.selectAll().where { Users.id eq uid }.firstOrNull()?.get(Users.hiderEnabled) ?: true
        }
        val effectiveTagPrivate = tagPrivate && allTaggedAllowHiding && !tagAll

        val cleanOptions = pollOptions?.map { it.trim() }?.filter { it.isNotBlank() }
        val isPoll = pollQuestion != null
        if (isPoll) {
            if (pollQuestion.isBlank()) throw AuthException("Poll needs a question")
            if (cleanOptions == null || cleanOptions.size < 2 || cleanOptions.size > 4) {
                throw AuthException("Poll needs 2-4 options")
            }
        }
        val trimmed = if (isSticker || mediaUrl != null || isPoll) text.trim() else text.trim().take(MAX_MESSAGE_LENGTH)
        if (trimmed.isBlank() && mediaUrl == null && !isPoll) throw AuthException("Message can't be empty")

        val replyUuid = replyToId?.let { UUID.fromString(it) }
        val validReply = replyUuid?.takeIf { rid ->
            GroupMessages.selectAll().where { (GroupMessages.id eq rid) and (GroupMessages.groupId eq gid) }.any()
        }

        // null defaults to "once" when viewOnce=true, matching 1-on-1's
        // ChatService.sendMessage. Duration/count are always clamped
        // server-side, never trusting what the client sent.
        val effectiveMode = if (!viewOnce) null else (viewOnceMode ?: "once")
        val clampedMaxViews = viewOnceMaxViews?.coerceAtMost(MAX_VIEW_ONCE_COUNT)
        val clampedDurationMs = viewOnceDurationMs?.coerceAtMost(MAX_VIEW_ONCE_DURATION_MS)
        val clampedDisappearMs = disappearDurationMs?.coerceIn(60_000L, MAX_AUTO_DELETE_MS)

        val sentAt = System.currentTimeMillis()
        val id = GroupMessages.insertAndGetId {
            it[GroupMessages.groupId] = gid
            it[senderId] = from
            it[GroupMessages.text] = trimmed
            it[GroupMessages.sentAt] = sentAt
            it[GroupMessages.replyToId] = validReply
            it[GroupMessages.isSticker] = isSticker
            it[GroupMessages.mediaUrl] = mediaUrl
            it[GroupMessages.mediaType] = mediaType
            it[GroupMessages.fileName] = fileName
            it[GroupMessages.mediaSizeBytes] = mediaSizeBytes
            it[GroupMessages.viewOnce] = viewOnce
            it[GroupMessages.viewOnceMode] = effectiveMode
            it[GroupMessages.viewOnceDurationMs] = clampedDurationMs
            it[GroupMessages.viewOnceMaxViews] = clampedMaxViews
            it[GroupMessages.pollQuestion] = pollQuestion
            it[GroupMessages.pollOptions] = cleanOptions?.joinToString("\n")
            it[GroupMessages.taggedUserIds] = taggedUuids.joinToString(",").ifBlank { null }
            it[GroupMessages.tagAll] = tagAll
            it[GroupMessages.tagPrivate] = effectiveTagPrivate
            it[GroupMessages.disappearAt] = clampedDisappearMs?.let { d -> sentAt + d }
            it[GroupMessages.disappearSelfOnly] = disappearSelfOnly
        }
        GroupMessageDto(
            id.value.toString(), groupId, fromUserId, trimmed, sentAt,
            replyToId = validReply?.toString(), isSticker = isSticker,
            mediaUrl = mediaUrl, mediaType = mediaType, fileName = fileName,
            viewOnce = viewOnce, viewed = true, viewOnceMode = effectiveMode,
            viewOnceDurationMs = clampedDurationMs, viewOnceMaxViews = clampedMaxViews,
            taggedUserIds = taggedUuids.map { it.toString() }, tagAll = tagAll, tagPrivate = effectiveTagPrivate,
            mediaSizeBytes = mediaSizeBytes,
            pollQuestion = pollQuestion, pollOptions = cleanOptions,
        )
    }

    // Purges any non-kept, non-deleted message older than `durationMs` -
    // same opportunistic "cleanup on read" idea as purgeConsumedGroupViewOnce,
    // called from getGroupMessages on every fetch. No-op when durationMs is
    // null (disappearing messages off for this group).
    private fun purgeExpiredGroupMessages(gid: UUID, durationMs: Long?) {
        val now = System.currentTimeMillis()
        val groupWideExpired = if (durationMs == null) {
            emptyList()
        } else {
            val cutoff = now - durationMs
            GroupMessages.selectAll()
                .where {
                    (GroupMessages.groupId eq gid) and (GroupMessages.sentAt less cutoff) and
                        (GroupMessages.kept eq false) and (GroupMessages.deleted eq false)
                }
                .map { it[GroupMessages.id].value }
        }
        // Per-message "For Everyone" disappearing (Round 5) - independent of
        // the group-wide setting above. "Custom"/self-only rows are
        // deliberately NOT included here - see getGroupMessages' per-viewer
        // filter instead, since that mode never deletes the row.
        val perMessageExpired = GroupMessages.selectAll()
            .where {
                (GroupMessages.groupId eq gid) and (GroupMessages.disappearAt.isNotNull()) and
                    (GroupMessages.disappearAt less now) and (GroupMessages.disappearSelfOnly eq false) and
                    (GroupMessages.deleted eq false)
            }
            .map { it[GroupMessages.id].value }
        val expiredIds = (groupWideExpired + perMessageExpired).distinct()
        if (expiredIds.isEmpty()) return
        GroupMessageReactions.deleteWhere { GroupMessageReactions.messageId inList expiredIds }
        GroupPollVotes.deleteWhere { GroupPollVotes.messageId inList expiredIds }
        GroupMessageViews.deleteWhere { GroupMessageViews.messageId inList expiredIds }
        GroupMessages.deleteWhere { GroupMessages.id inList expiredIds }
    }

    // Cursor-based pagination: pass `beforeTimestamp` (the sentAt of the
    // oldest message the client already has) to fetch the page before it.
    // null = first page (most recent messages). The client loops this to
    // load older history on scroll-up, so a group with thousands of messages
    // doesn't have to load all of them at once - see GroupChatThreadScreen's
    // loadMoreMessages. The 200 default stays the same for the initial load.
    fun getGroupMessages(groupId: String, userId: String, limit: Int = 200, beforeTimestamp: Long? = null): List<GroupMessageDto> = transaction {
        val gid = UUID.fromString(groupId)
        val uid = UUID.fromString(userId)
        val group = Groups.selectAll().where { Groups.id eq gid }.firstOrNull() ?: throw AuthException("Group not found")
        val member = GroupMembers.selectAll().where { (GroupMembers.groupId eq gid) and (GroupMembers.userId eq uid) }.firstOrNull()
            ?: throw AuthException("You're not a member of this group")
        val myRole = member[GroupMembers.role]

        purgeExpiredGroupMessages(gid, group[Groups.disappearingMessagesDurationMs])

        val filterHistory = !meetsThreshold(myRole, "ADMIN") && !group[Groups.shareHistoryWithNewMembers]
        val rows = (
            if (filterHistory) {
                if (beforeTimestamp != null) {
                    GroupMessages.selectAll().where {
                        (GroupMessages.groupId eq gid) and (GroupMessages.sentAt greaterEq member[GroupMembers.joinedAt]) and (GroupMessages.sentAt less beforeTimestamp)
                    }
                } else {
                    GroupMessages.selectAll().where { (GroupMessages.groupId eq gid) and (GroupMessages.sentAt greaterEq member[GroupMembers.joinedAt]) }
                }
            } else {
                if (beforeTimestamp != null) {
                    GroupMessages.selectAll().where { (GroupMessages.groupId eq gid) and (GroupMessages.sentAt less beforeTimestamp) }
                } else {
                    GroupMessages.selectAll().where { GroupMessages.groupId eq gid }
                }
            }
            )
            .orderBy(GroupMessages.sentAt, SortOrder.DESC)
            .limit(limit)
            .toList()
        // "Custom"/self-only disappearing (Round 5) - never deletes the row,
        // just stops showing it to the sender specifically once expired;
        // every other viewer keeps seeing it normally.
        val now = System.currentTimeMillis()
        val visibleRows = rows.filter { row ->
            val disappearAt = row[GroupMessages.disappearAt]
            !(row[GroupMessages.disappearSelfOnly] && disappearAt != null && now > disappearAt && row[GroupMessages.senderId].value == uid)
        }
        val ids = visibleRows.map { it[GroupMessages.id].value }
        val reactions = reactionsFor(ids)
        val pollVotes = pollVotesFor(visibleRows.filter { it[GroupMessages.pollOptions] != null }.map { it[GroupMessages.id].value })
        val viewed = viewedMessageIds(ids, uid)
        visibleRows.map { row ->
            val mid = row[GroupMessages.id].value
            toDto(row, uid, reactions[mid].orEmpty(), pollVotes[mid].orEmpty(), mid in viewed)
        }.reversed()
    }

    fun editGroupMessage(userId: String, messageId: String, newText: String): GroupMessageDto = transaction {
        val uid = UUID.fromString(userId)
        val mid = UUID.fromString(messageId)
        val row = GroupMessages.selectAll().where { GroupMessages.id eq mid }.firstOrNull() ?: throw AuthException("Message not found")
        if (row[GroupMessages.senderId].value != uid) throw AuthException("You can only edit your own messages")
        if (row[GroupMessages.deleted]) throw AuthException("Can't edit a deleted message")
        if (System.currentTimeMillis() - row[GroupMessages.sentAt] > EDIT_WINDOW_MS) throw AuthException("Too late to edit this message")
        val trimmed = newText.trim().take(MAX_MESSAGE_LENGTH)
        if (trimmed.isBlank()) throw AuthException("Message can't be empty")

        val editedAt = System.currentTimeMillis()
        GroupMessages.update({ GroupMessages.id eq mid }) {
            it[GroupMessages.text] = trimmed
            it[GroupMessages.editedAt] = editedAt
        }
        val updated = GroupMessages.selectAll().where { GroupMessages.id eq mid }.first()
        toDto(updated, uid, reactionsFor(listOf(mid))[mid].orEmpty())
    }

    fun deleteGroupMessage(userId: String, messageId: String): Unit = transaction {
        val uid = UUID.fromString(userId)
        val mid = UUID.fromString(messageId)
        val row = GroupMessages.selectAll().where { GroupMessages.id eq mid }.firstOrNull() ?: throw AuthException("Message not found")
        if (row[GroupMessages.senderId].value != uid) throw AuthException("You can only delete your own messages")
        GroupMessages.update({ GroupMessages.id eq mid }) { it[deleted] = true }
        GroupMessageReactions.deleteWhere { GroupMessageReactions.messageId eq mid }
    }

    fun reactToGroupMessage(userId: String, messageId: String, emoji: String): Map<String, String> = transaction {
        val uid = UUID.fromString(userId)
        val mid = UUID.fromString(messageId)
        GroupMessages.selectAll().where { GroupMessages.id eq mid }.firstOrNull() ?: throw AuthException("Message not found")

        val existing = GroupMessageReactions.selectAll()
            .where { (GroupMessageReactions.messageId eq mid) and (GroupMessageReactions.userId eq uid) }
            .firstOrNull()
        if (existing != null && existing[GroupMessageReactions.emoji] == emoji) {
            GroupMessageReactions.deleteWhere { (GroupMessageReactions.messageId eq mid) and (GroupMessageReactions.userId eq uid) }
        } else {
            GroupMessageReactions.deleteWhere { (GroupMessageReactions.messageId eq mid) and (GroupMessageReactions.userId eq uid) }
            GroupMessageReactions.insert {
                it[GroupMessageReactions.messageId] = mid
                it[GroupMessageReactions.userId] = uid
                it[GroupMessageReactions.emoji] = emoji
            }
        }
        reactionsFor(listOf(mid))[mid].orEmpty()
    }

    fun voteInGroupPoll(userId: String, messageId: String, optionIndex: Int): Map<String, Int> = transaction {
        val uid = UUID.fromString(userId)
        val mid = UUID.fromString(messageId)
        val row = GroupMessages.selectAll().where { GroupMessages.id eq mid }.firstOrNull() ?: throw AuthException("Message not found")
        val options = row[GroupMessages.pollOptions]?.split("\n") ?: throw AuthException("This message isn't a poll")
        if (optionIndex !in options.indices) throw AuthException("Invalid option")
        if (!isMember(row[GroupMessages.groupId].value, uid)) throw AuthException("You're not a member of this group")

        GroupPollVotes.deleteWhere { (GroupPollVotes.messageId eq mid) and (GroupPollVotes.userId eq uid) }
        GroupPollVotes.insert {
            it[GroupPollVotes.messageId] = mid
            it[GroupPollVotes.userId] = uid
            it[GroupPollVotes.optionIndex] = optionIndex
        }
        pollVotesFor(listOf(mid))[mid].orEmpty()
    }

    // Only a non-sender member can reveal, matching 1-on-1's "only the
    // recipient can reveal" rule generalized to N recipients. Per-viewer
    // state lives in GroupMessageViews since (unlike 1-on-1) many members
    // may reveal the same message independently.
    fun revealGroupMessage(groupId: String, userId: String, messageId: String): GroupMessageDto = transaction {
        val gid = UUID.fromString(groupId)
        val uid = UUID.fromString(userId)
        val mid = UUID.fromString(messageId)
        if (!isMember(gid, uid)) throw AuthException("You're not a member of this group")
        val row = GroupMessages.selectAll().where { (GroupMessages.id eq mid) and (GroupMessages.groupId eq gid) }.firstOrNull()
            ?: throw AuthException("Message not found")
        if (row[GroupMessages.senderId].value == uid) throw AuthException("You already know what you sent")
        if (!row[GroupMessages.viewOnce]) throw AuthException("This message isn't view-once")
        val mode = row[GroupMessages.viewOnceMode]
        val existing = GroupMessageViews.selectAll()
            .where { (GroupMessageViews.messageId eq mid) and (GroupMessageViews.userId eq uid) }
            .firstOrNull()

        when (mode) {
            "custom_count" -> {
                val max = row[GroupMessages.viewOnceMaxViews] ?: 1
                val count = existing?.get(GroupMessageViews.viewCount) ?: 0
                if (count >= max) throw AuthException("This message has already been viewed the maximum number of times")
                if (existing == null) {
                    GroupMessageViews.insert {
                        it[GroupMessageViews.messageId] = mid
                        it[GroupMessageViews.userId] = uid
                        it[viewedAt] = System.currentTimeMillis()
                        it[viewCount] = 1
                    }
                } else {
                    GroupMessageViews.update({ (GroupMessageViews.messageId eq mid) and (GroupMessageViews.userId eq uid) }) {
                        it[viewCount] = count + 1
                    }
                }
            }
            "custom_time" -> {
                val existingViewedAt = existing?.get(GroupMessageViews.viewedAt)
                val duration = row[GroupMessages.viewOnceDurationMs] ?: 0L
                if (existingViewedAt != null && System.currentTimeMillis() > existingViewedAt + duration) {
                    throw AuthException("This message has expired")
                }
                if (existing == null) {
                    GroupMessageViews.insert {
                        it[GroupMessageViews.messageId] = mid
                        it[GroupMessageViews.userId] = uid
                        it[viewedAt] = System.currentTimeMillis()
                        it[viewCount] = 1
                    }
                }
            }
            else -> {
                if (existing == null) {
                    GroupMessageViews.insert {
                        it[GroupMessageViews.messageId] = mid
                        it[GroupMessageViews.userId] = uid
                        it[viewedAt] = System.currentTimeMillis()
                        it[viewCount] = 1
                    }
                }
            }
        }
        toDto(row, uid, reactionsFor(listOf(mid))[mid].orEmpty(), pollVotesFor(listOf(mid))[mid].orEmpty(), hasViewed = true)
    }

    // A group view-once message is only purge-eligible once EVERY non-
    // sender member has exhausted their own view - stricter than 1-on-1's
    // two-party check since a group has N recipients. Deferred to
    // thread-exit/app-background (see MemberChatThreadScreen's equivalent
    // trigger), not immediately on consumption.
    fun purgeConsumedGroupViewOnce(groupId: String, userId: String): Unit = transaction {
        val gid = UUID.fromString(groupId)
        val uid = UUID.fromString(userId)
        if (!isMember(gid, uid)) return@transaction
        val now = System.currentTimeMillis()
        val candidates = GroupMessages.selectAll()
            .where { (GroupMessages.groupId eq gid) and (GroupMessages.viewOnce eq true) }
            .toList()
        if (candidates.isEmpty()) return@transaction

        val memberIds = memberIdsOf(gid)
        val candidateIds = candidates.map { it[GroupMessages.id].value }
        val viewsByMessage = GroupMessageViews.selectAll()
            .where { GroupMessageViews.messageId inList candidateIds }
            .groupBy { it[GroupMessageViews.messageId].value }

        val consumedIds = candidates.filter { row ->
            val mid = row[GroupMessages.id].value
            val sender = row[GroupMessages.senderId].value
            val recipients = memberIds.filter { it != sender }
            if (recipients.isEmpty()) return@filter true
            val rowsByUser = viewsByMessage[mid].orEmpty().associateBy { it[GroupMessageViews.userId].value }
            recipients.all { recipient ->
                val v = rowsByUser[recipient] ?: return@all false
                when (row[GroupMessages.viewOnceMode]) {
                    "custom_count" -> v[GroupMessageViews.viewCount] >= (row[GroupMessages.viewOnceMaxViews] ?: 1)
                    "custom_time" -> now > v[GroupMessageViews.viewedAt] + (row[GroupMessages.viewOnceDurationMs] ?: 0L)
                    else -> true
                }
            }
        }.map { it[GroupMessages.id].value }

        if (consumedIds.isNotEmpty()) {
            GroupMessageReactions.deleteWhere { GroupMessageReactions.messageId inList consumedIds }
            GroupPollVotes.deleteWhere { GroupPollVotes.messageId inList consumedIds }
            GroupMessageViews.deleteWhere { GroupMessageViews.messageId inList consumedIds }
            GroupMessages.deleteWhere { GroupMessages.id inList consumedIds }
        }
    }

    // Merged into ChatService.listConversations - see that function. Groups
    // don't support archive/hide/pin in v1 (always "included"), so this has
    // no mode param unlike listConversationsInternal's friend equivalent.
    fun listMyGroupSummaries(userId: String): List<ConversationSummary> = transaction {
        val uid = UUID.fromString(userId)
        val allMyGroupIds = GroupMembers.selectAll().where { GroupMembers.userId eq uid }.map { it[GroupMembers.groupId].value }
        if (allMyGroupIds.isEmpty()) return@transaction emptyList()

        // Lazy auto-delete purge - same "no job scheduler" convention as
        // disappearing messages, just checked here (and in getGroup) since
        // this is the other most-common read path.
        val now = System.currentTimeMillis()
        val expiredIds = Groups.selectAll().where { Groups.id inList allMyGroupIds }
            .filter { row -> row[Groups.autoDeleteAt]?.let { now > it } == true }
            .map { it[Groups.id].value }
        expiredIds.forEach { deleteGroupFully(it) }
        val myGroupIds = allMyGroupIds - expiredIds.toSet()
        if (myGroupIds.isEmpty()) return@transaction emptyList()

        val groups = Groups.selectAll().where { Groups.id inList myGroupIds }.associateBy { it[Groups.id].value }
        val membersByGroup = GroupMembers.selectAll().where { GroupMembers.groupId inList myGroupIds }
            .groupBy({ it[GroupMembers.groupId].value }, { it[GroupMembers.userId].value })
        val avatarByUser = Users.selectAll()
            .where { Users.id inList membersByGroup.values.flatten().distinct() }
            .associate { it[Users.id].value to it[Users.avatarUrl] }
        val lastMessageByGroup = GroupMessages.selectAll()
            .where { GroupMessages.groupId inList myGroupIds }
            .orderBy(GroupMessages.sentAt, SortOrder.DESC)
            .groupBy { it[GroupMessages.groupId].value }
            .mapValues { (_, rows) -> rows.first() }

        myGroupIds.mapNotNull { groupId ->
            val group = groups[groupId] ?: return@mapNotNull null
            val last = lastMessageByGroup[groupId]
            val memberAvatars = membersByGroup[groupId].orEmpty().filterNot { it == uid }
                .mapNotNull { avatarByUser[it] }.take(3)
            ConversationSummary(
                friendId = groupId.toString(),
                name = group[Groups.name],
                avatarUrl = group[Groups.avatarUrl],
                // View-once previews never leak the real text here either -
                // the list screen shows a generic "View Once" line instead
                // (see lastMessageViewOnce client-side), mirroring
                // ChatService.listConversationsInternal.
                lastMessage = last?.let {
                    when {
                        it[GroupMessages.deleted] -> "Message deleted"
                        it[GroupMessages.viewOnce] -> null
                        else -> it[GroupMessages.text]
                    }
                },
                lastMessageAt = last?.get(GroupMessages.sentAt),
                lastMessageFromMe = last?.let { it[GroupMessages.senderId].value == uid },
                lastMessageViewOnce = last?.get(GroupMessages.viewOnce) == true && last[GroupMessages.deleted] != true,
                isGroup = true,
                memberAvatarUrls = memberAvatars,
            )
        }
    }

    // ---- Round 2/3: moderation, discovery, DM/tag preferences ----

    // Message-only wipe (reactions/poll-votes/views/messages) - the group
    // and its members stay intact, unlike deleteGroupFully/leaveGroup(dissolve).
    fun clearChat(groupId: String, actingUserId: String): Unit = transaction {
        val gid = UUID.fromString(groupId)
        val actor = UUID.fromString(actingUserId)
        val group = Groups.selectAll().where { Groups.id eq gid }.firstOrNull() ?: throw AuthException("Group not found")
        if (group[Groups.creatorId].value != actor) throw AuthException("Only the group creator can clear the chat")
        val messageIds = GroupMessages.selectAll().where { GroupMessages.groupId eq gid }.map { it[GroupMessages.id].value }
        if (messageIds.isNotEmpty()) {
            GroupMessageReactions.deleteWhere { GroupMessageReactions.messageId inList messageIds }
            GroupPollVotes.deleteWhere { GroupPollVotes.messageId inList messageIds }
            GroupMessageViews.deleteWhere { GroupMessageViews.messageId inList messageIds }
            GroupMessages.deleteWhere { GroupMessages.id inList messageIds }
        }
        Groups.update({ Groups.id eq gid }) { it[pinnedMessageId] = null; it[pinnedByRole] = null }
    }

    fun pinMessage(groupId: String, actingUserId: String, messageId: String): GroupDto = transaction {
        val gid = UUID.fromString(groupId)
        val actor = UUID.fromString(actingUserId)
        val mid = UUID.fromString(messageId)
        val actorRole = roleOf(gid, actor) ?: throw AuthException("You're not a member of this group")
        if (actorRole !in ADMIN_TIER) throw AuthException("Only group admins can pin a message")
        GroupMessages.selectAll().where { (GroupMessages.id eq mid) and (GroupMessages.groupId eq gid) }.firstOrNull()
            ?: throw AuthException("Message not found")
        Groups.update({ Groups.id eq gid }) { it[pinnedMessageId] = mid; it[pinnedByRole] = actorRole }
        buildGroupDto(gid, actor)
    }

    fun unpinMessage(groupId: String, actingUserId: String): GroupDto = transaction {
        val gid = UUID.fromString(groupId)
        val actor = UUID.fromString(actingUserId)
        val group = Groups.selectAll().where { Groups.id eq gid }.firstOrNull() ?: throw AuthException("Group not found")
        val actorRole = roleOf(gid, actor) ?: throw AuthException("You're not a member of this group")
        if (actorRole !in ADMIN_TIER) throw AuthException("Only group admins can unpin a message")
        val currentPinnedByRole = group[Groups.pinnedByRole]
        if (currentPinnedByRole != null && roleRank(actorRole) < roleRank(currentPinnedByRole)) {
            throw AuthException("You can't unpin a message pinned by someone with a higher rank")
        }
        Groups.update({ Groups.id eq gid }) { it[Groups.pinnedMessageId] = null; it[Groups.pinnedByRole] = null }
        buildGroupDto(gid, actor)
    }

    fun reportGroup(reporterId: String, groupId: String, reason: String?, mediaUrl: String?, mediaType: String?, fileName: String?): Unit = transaction {
        val gid = UUID.fromString(groupId)
        val reporter = UUID.fromString(reporterId)
        Groups.selectAll().where { Groups.id eq gid }.firstOrNull() ?: throw AuthException("Group not found")
        GroupReports.insert {
            it[GroupReports.reporterId] = reporter
            it[GroupReports.groupId] = gid
            it[GroupReports.createdAt] = System.currentTimeMillis()
            it[GroupReports.reason] = reason?.trim()?.take(1000)
            it[GroupReports.mediaUrl] = mediaUrl
            it[GroupReports.mediaType] = mediaType
            it[GroupReports.fileName] = fileName
        }
    }

    private fun isGroupBlocked(groupId: UUID, userId: UUID): Boolean =
        BlockedGroups.selectAll().where { (BlockedGroups.userId eq userId) and (BlockedGroups.groupId eq groupId) }.any()

    fun setGroupBlocked(userId: String, groupId: String, blocked: Boolean): Unit = transaction {
        val uid = UUID.fromString(userId)
        val gid = UUID.fromString(groupId)
        if (blocked) {
            if (!isGroupBlocked(gid, uid)) {
                BlockedGroups.insert {
                    it[BlockedGroups.userId] = uid
                    it[BlockedGroups.groupId] = gid
                    it[BlockedGroups.blockedAt] = System.currentTimeMillis()
                }
            }
        } else {
            BlockedGroups.deleteWhere { (BlockedGroups.userId eq uid) and (BlockedGroups.groupId eq gid) }
        }
    }

    fun setGroupMuted(userId: String, groupId: String, muted: Boolean): Unit = transaction {
        val uid = UUID.fromString(userId)
        val gid = UUID.fromString(groupId)
        val existing = GroupConversationState.selectAll()
            .where { (GroupConversationState.userId eq uid) and (GroupConversationState.groupId eq gid) }.firstOrNull()
        if (existing == null) {
            GroupConversationState.insert {
                it[GroupConversationState.userId] = uid
                it[GroupConversationState.groupId] = gid
                it[GroupConversationState.muted] = muted
            }
        } else {
            GroupConversationState.update({ (GroupConversationState.userId eq uid) and (GroupConversationState.groupId eq gid) }) {
                it[GroupConversationState.muted] = muted
            }
        }
    }

    // This member's own override of Groups.dmClosedByCreator's default for
    // themself specifically in this group - see canDm's precedence.
    fun setDmOverride(userId: String, groupId: String, dmOverride: String?): Unit = transaction {
        val uid = UUID.fromString(userId)
        val gid = UUID.fromString(groupId)
        if (!isMember(gid, uid)) throw AuthException("You're not a member of this group")
        if (dmOverride != null && dmOverride != "OPEN" && dmOverride != "CLOSED") throw AuthException("Invalid value")
        val existing = GroupConversationState.selectAll()
            .where { (GroupConversationState.userId eq uid) and (GroupConversationState.groupId eq gid) }.firstOrNull()
        if (existing == null) {
            GroupConversationState.insert {
                it[GroupConversationState.userId] = uid
                it[GroupConversationState.groupId] = gid
                it[GroupConversationState.dmOverride] = dmOverride
            }
        } else {
            GroupConversationState.update({ (GroupConversationState.userId eq uid) and (GroupConversationState.groupId eq gid) }) {
                it[GroupConversationState.dmOverride] = dmOverride
            }
        }
    }

    fun searchPublicGroups(query: String, limit: Int = 30): List<GroupSearchResultDto> = transaction {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return@transaction emptyList()
        Groups.selectAll().where { Groups.isPublic eq true }
            .filter { it[Groups.name].contains(trimmed, ignoreCase = true) }
            .take(limit)
            .map { row ->
                val gid = row[Groups.id].value
                GroupSearchResultDto(
                    id = gid.toString(), name = row[Groups.name], avatarUrl = row[Groups.avatarUrl],
                    description = row[Groups.description], memberCount = memberIdsOf(gid).size,
                )
            }
    }

    // Round 5 "Link" tab. Regenerating invalidates every previously-shared
    // link/QR - old tokens simply stop matching any row.
    fun resetInviteLink(groupId: String, actingUserId: String): GroupDto = transaction {
        val gid = UUID.fromString(groupId)
        val actor = UUID.fromString(actingUserId)
        val actorRole = roleOf(gid, actor) ?: throw AuthException("You're not a member of this group")
        if (actorRole !in ADMIN_TIER) throw AuthException("Only group admins can reset the invite link")
        val token = java.util.UUID.randomUUID().toString().replace("-", "")
        Groups.update({ Groups.id eq gid }) { it[inviteToken] = token }
        buildGroupDto(gid, actor)
    }

    // What a scanned QR / opened link resolves to before the viewer is a
    // member - deliberately a smaller DTO than GroupDto (no member list etc)
    // since this is reachable by someone who isn't in the group yet.
    fun getGroupByToken(token: String, viewerId: String): GroupLinkPreviewDto = transaction {
        val uid = UUID.fromString(viewerId)
        val group = Groups.selectAll().where { Groups.inviteToken eq token }.firstOrNull()
            ?: throw AuthException("This link is no longer valid")
        val gid = group[Groups.id].value
        GroupLinkPreviewDto(
            id = gid.toString(), name = group[Groups.name], avatarUrl = group[Groups.avatarUrl],
            description = group[Groups.description], memberCount = memberIdsOf(gid).size,
            isPublic = group[Groups.isPublic],
            alreadyMember = isMember(gid, uid),
            alreadyRequested = GroupJoinRequests.selectAll().where { (GroupJoinRequests.groupId eq gid) and (GroupJoinRequests.userId eq uid) }.any(),
        )
    }

    // Public groups: instant join, no approval - reachable either by finding
    // the group in search or by opening its link (both are open to anyone,
    // per the "public" toggle's own description). Private groups: no search
    // path exists at all (searchPublicGroups is isPublic-only), so this is
    // only ever reached via a private group's admin-tier-only link - that
    // still requires an admin-tier member to approve via the existing
    // join-requests flow below, same as the old public-only behavior.
    fun requestToJoin(groupId: String, userId: String): Unit = transaction {
        val gid = UUID.fromString(groupId)
        val uid = UUID.fromString(userId)
        val group = Groups.selectAll().where { Groups.id eq gid }.firstOrNull() ?: throw AuthException("Group not found")
        if (isMember(gid, uid)) throw AuthException("Already a member of this group")
        if (isGroupBlocked(gid, uid)) throw AuthException("You've blocked this group")
        if (group[Groups.isPublic]) {
            checkRejoinCooldown(gid, uid)
            GroupMembers.insert {
                it[GroupMembers.groupId] = gid
                it[GroupMembers.userId] = uid
                it[role] = "MEMBER"
                it[joinedAt] = System.currentTimeMillis()
            }
        } else {
            val existing = GroupJoinRequests.selectAll().where { (GroupJoinRequests.groupId eq gid) and (GroupJoinRequests.userId eq uid) }.any()
            if (existing) throw AuthException("You've already requested to join")
            GroupJoinRequests.insert {
                it[GroupJoinRequests.groupId] = gid
                it[GroupJoinRequests.userId] = uid
                it[GroupJoinRequests.requestedAt] = System.currentTimeMillis()
            }
        }
    }

    fun listJoinRequests(groupId: String, actingUserId: String): List<GroupJoinRequestDto> = transaction {
        val gid = UUID.fromString(groupId)
        val actor = UUID.fromString(actingUserId)
        val actorRole = roleOf(gid, actor) ?: throw AuthException("You're not a member of this group")
        if (actorRole !in ADMIN_TIER) throw AuthException("Only group admins can view join requests")
        GroupJoinRequests.selectAll().where { GroupJoinRequests.groupId eq gid }
            .map { GroupJoinRequestDto(it[GroupJoinRequests.userId].value.toString(), it[GroupJoinRequests.requestedAt]) }
    }

    fun approveJoinRequest(groupId: String, actingUserId: String, targetUserId: String): Unit = transaction {
        val gid = UUID.fromString(groupId)
        val actor = UUID.fromString(actingUserId)
        val target = UUID.fromString(targetUserId)
        val actorRole = roleOf(gid, actor) ?: throw AuthException("You're not a member of this group")
        if (actorRole !in ADMIN_TIER) throw AuthException("Only group admins can approve join requests")
        val requested = GroupJoinRequests.selectAll().where { (GroupJoinRequests.groupId eq gid) and (GroupJoinRequests.userId eq target) }.any()
        if (!requested) throw AuthException("No pending join request from this user")
        GroupJoinRequests.deleteWhere { (GroupJoinRequests.groupId eq gid) and (GroupJoinRequests.userId eq target) }
        if (isGroupBlocked(gid, target)) throw AuthException("This user has blocked this group")
        checkRejoinCooldown(gid, target)
        if (!isMember(gid, target)) {
            GroupMembers.insert {
                it[GroupMembers.groupId] = gid
                it[GroupMembers.userId] = target
                it[role] = "MEMBER"
                it[joinedAt] = System.currentTimeMillis()
            }
        }
    }

    fun rejectJoinRequest(groupId: String, actingUserId: String, targetUserId: String): Unit = transaction {
        val gid = UUID.fromString(groupId)
        val actor = UUID.fromString(actingUserId)
        val target = UUID.fromString(targetUserId)
        val actorRole = roleOf(gid, actor) ?: throw AuthException("You're not a member of this group")
        if (actorRole !in ADMIN_TIER) throw AuthException("Only group admins can reject join requests")
        GroupJoinRequests.deleteWhere { (GroupJoinRequests.groupId eq gid) and (GroupJoinRequests.userId eq target) }
    }

    // Count-only summary (not byte-size) - see this plan's "Manage storage"
    // design decision for why precise sizes are out of scope for now.
    fun getMediaSummary(groupId: String, actingUserId: String): MediaSummaryDto = transaction {
        val gid = UUID.fromString(groupId)
        val actor = UUID.fromString(actingUserId)
        if (!isMember(gid, actor)) throw AuthException("You're not a member of this group")
        val rows = GroupMessages.selectAll()
            .where { (GroupMessages.groupId eq gid) and (GroupMessages.deleted eq false) }
            .toList()
        val stickerRows = rows.filter { it[GroupMessages.isSticker] }
        val mediaRows = rows.filter { !it[GroupMessages.isSticker] && it[GroupMessages.mediaUrl] != null }
        val imageRows = mediaRows.filter { it[GroupMessages.mediaType]?.startsWith("image") == true }
        val videoRows = mediaRows.filter { it[GroupMessages.mediaType]?.startsWith("video") == true }
        val fileRows = mediaRows - imageRows.toSet() - videoRows.toSet()
        fun sumBytes(list: List<ResultRow>) = list.sumOf { it[GroupMessages.mediaSizeBytes] ?: 0L }
        MediaSummaryDto(
            images = imageRows.size, videos = videoRows.size, files = fileRows.size, stickers = stickerRows.size,
            imagesBytes = sumBytes(imageRows), videosBytes = sumBytes(videoRows), filesBytes = sumBytes(fileRows), stickersBytes = sumBytes(stickerRows),
        )
    }

    // WhatsApp's "Keep" action equivalent - exempts one message from the
    // disappearing-messages lazy purge (see purgeExpiredGroupMessages).
    fun keepMessage(groupId: String, userId: String, messageId: String): Unit = transaction {
        val gid = UUID.fromString(groupId)
        val uid = UUID.fromString(userId)
        val mid = UUID.fromString(messageId)
        if (!isMember(gid, uid)) throw AuthException("You're not a member of this group")
        GroupMessages.selectAll().where { (GroupMessages.id eq mid) and (GroupMessages.groupId eq gid) }.firstOrNull()
            ?: throw AuthException("Message not found")
        GroupMessages.update({ GroupMessages.id eq mid }) { it[kept] = true }
    }

    fun clearAllMedia(groupId: String, actingUserId: String): Unit = transaction {
        val gid = UUID.fromString(groupId)
        val actor = UUID.fromString(actingUserId)
        val actorRole = roleOf(gid, actor) ?: throw AuthException("You're not a member of this group")
        if (actorRole !in ADMIN_TIER) throw AuthException("Only group admins can clear media")
        GroupMessages.update({ (GroupMessages.groupId eq gid) and (GroupMessages.mediaUrl.isNotNull()) }) {
            it[mediaUrl] = null
            it[mediaType] = null
            it[fileName] = null
        }
    }
}
