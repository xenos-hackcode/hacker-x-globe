package com.xhacker.cedal.routes

import com.xhacker.cedal.models.AddGroupMemberRequest
import com.xhacker.cedal.models.CreateGroupRequest
import com.xhacker.cedal.models.EditGroupMessageRequest
import com.xhacker.cedal.models.LeaveGroupRequest
import com.xhacker.cedal.models.ReactToGroupMessageRequest
import com.xhacker.cedal.models.ReportGroupRequest
import com.xhacker.cedal.models.SendGroupMessageRequest
import com.xhacker.cedal.models.SetDmOverrideRequest
import com.xhacker.cedal.models.SetGroupRoleRequest
import com.xhacker.cedal.models.UpdateGroupInfoRequest
import com.xhacker.cedal.models.UpdateGroupSettingsRequest
import com.xhacker.cedal.models.VoteInGroupPollRequest
import com.xhacker.cedal.services.AuthException
import com.xhacker.cedal.services.GroupChatService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.groupChatRoutes() {
    route("/groups") {
        authenticate("auth-jwt") {
            post {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                val req = call.receive<CreateGroupRequest>()
                call.respond(HttpStatusCode.OK, GroupChatService.createGroup(userId, req.name, req.memberIds))
            }
            // Must come before "/{groupId}" so these aren't parsed as a groupId.
            get("/search") {
                val query = call.request.queryParameters["q"] ?: ""
                call.respond(HttpStatusCode.OK, GroupChatService.searchPublicGroups(query))
            }
            get("/by-token/{token}") {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                val token = call.parameters["token"] ?: throw AuthException("Missing token")
                call.respond(HttpStatusCode.OK, GroupChatService.getGroupByToken(token, userId))
            }
            get("/{groupId}") {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                val groupId = call.parameters["groupId"] ?: throw AuthException("Missing groupId")
                call.respond(HttpStatusCode.OK, GroupChatService.getGroup(groupId, userId))
            }
            put("/{groupId}/info") {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                val groupId = call.parameters["groupId"] ?: throw AuthException("Missing groupId")
                val req = call.receive<UpdateGroupInfoRequest>()
                call.respond(HttpStatusCode.OK, GroupChatService.updateGroupInfo(groupId, userId, req.name, req.description, req.avatarUrl, req.rules))
            }
            put("/{groupId}/settings") {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                val groupId = call.parameters["groupId"] ?: throw AuthException("Missing groupId")
                val req = call.receive<UpdateGroupSettingsRequest>()
                call.respond(
                    HttpStatusCode.OK,
                    GroupChatService.updateGroupSettings(
                        groupId, userId, req.whoCanSendMessages, req.whoCanEditInfo, req.whoCanAddMembers,
                        req.whoCanSeeGroupStats, req.whoCanSendMedia, req.shareHistoryWithNewMembers, req.isPublic,
                        req.securedMode, req.disappearingMessagesDurationMs, req.disappearingMessagesOff,
                        req.lockedSettings, req.autoDeleteDurationMs, req.autoDeleteOff, req.dmClosedByCreator, req.callsEnabled,
                    ),
                )
            }
            put("/{groupId}/members/{targetUserId}/role") {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                val groupId = call.parameters["groupId"] ?: throw AuthException("Missing groupId")
                val targetUserId = call.parameters["targetUserId"] ?: throw AuthException("Missing targetUserId")
                val req = call.receive<SetGroupRoleRequest>()
                GroupChatService.setRole(groupId, userId, targetUserId, req.role)
                call.respond(HttpStatusCode.OK, mapOf("ok" to true))
            }
            get("/{groupId}/messages") {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                val groupId = call.parameters["groupId"] ?: throw AuthException("Missing groupId")
                val beforeTimestamp = call.request.queryParameters["before"]?.toLongOrNull()
                call.respond(HttpStatusCode.OK, GroupChatService.getGroupMessages(groupId, userId, beforeTimestamp = beforeTimestamp))
            }
            post("/{groupId}/messages") {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                val groupId = call.parameters["groupId"] ?: throw AuthException("Missing groupId")
                val req = call.receive<SendGroupMessageRequest>()
                call.respond(
                    HttpStatusCode.OK,
                    GroupChatService.sendGroupMessage(
                        groupId, userId, req.text, req.replyToId, req.isSticker,
                        req.mediaUrl, req.mediaType, req.fileName, req.mediaSizeBytes,
                        req.viewOnce, req.viewOnceMode, req.viewOnceDurationMs, req.viewOnceMaxViews,
                        req.pollQuestion, req.pollOptions,
                        req.taggedUserIds, req.tagAll, req.tagPrivate,
                        req.disappearDurationMs, req.disappearSelfOnly,
                    ),
                )
            }
            post("/{groupId}/messages/{messageId}/reveal") {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                val groupId = call.parameters["groupId"] ?: throw AuthException("Missing groupId")
                val messageId = call.parameters["messageId"] ?: throw AuthException("Missing messageId")
                call.respond(HttpStatusCode.OK, GroupChatService.revealGroupMessage(groupId, userId, messageId))
            }
            post("/{groupId}/purge-consumed-view-once") {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                val groupId = call.parameters["groupId"] ?: throw AuthException("Missing groupId")
                GroupChatService.purgeConsumedGroupViewOnce(groupId, userId)
                call.respond(HttpStatusCode.OK, mapOf("ok" to true))
            }
            put("/{groupId}/messages/{messageId}") {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                val messageId = call.parameters["messageId"] ?: throw AuthException("Missing messageId")
                val req = call.receive<EditGroupMessageRequest>()
                call.respond(HttpStatusCode.OK, GroupChatService.editGroupMessage(userId, messageId, req.text))
            }
            delete("/{groupId}/messages/{messageId}") {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                val messageId = call.parameters["messageId"] ?: throw AuthException("Missing messageId")
                GroupChatService.deleteGroupMessage(userId, messageId)
                call.respond(HttpStatusCode.OK, mapOf("ok" to true))
            }
            post("/{groupId}/messages/{messageId}/keep") {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                val groupId = call.parameters["groupId"] ?: throw AuthException("Missing groupId")
                val messageId = call.parameters["messageId"] ?: throw AuthException("Missing messageId")
                GroupChatService.keepMessage(groupId, userId, messageId)
                call.respond(HttpStatusCode.OK, mapOf("ok" to true))
            }
            post("/{groupId}/messages/{messageId}/react") {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                val messageId = call.parameters["messageId"] ?: throw AuthException("Missing messageId")
                val req = call.receive<ReactToGroupMessageRequest>()
                call.respond(HttpStatusCode.OK, GroupChatService.reactToGroupMessage(userId, messageId, req.emoji))
            }
            post("/{groupId}/messages/{messageId}/vote") {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                val messageId = call.parameters["messageId"] ?: throw AuthException("Missing messageId")
                val req = call.receive<VoteInGroupPollRequest>()
                call.respond(HttpStatusCode.OK, GroupChatService.voteInGroupPoll(userId, messageId, req.optionIndex))
            }
            post("/{groupId}/members") {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                val groupId = call.parameters["groupId"] ?: throw AuthException("Missing groupId")
                val req = call.receive<AddGroupMemberRequest>()
                GroupChatService.addMember(groupId, userId, req.userId)
                call.respond(HttpStatusCode.OK, mapOf("ok" to true))
            }
            // Same endpoint for "leave" (targetUserId == caller, non-creator)
            // and creator-removes-someone-else - GroupChatService.removeMember
            // tells them apart itself. A CREATOR self-leaving is rejected here
            // and must go through POST .../leave below instead.
            delete("/{groupId}/members/{targetUserId}") {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                val groupId = call.parameters["groupId"] ?: throw AuthException("Missing groupId")
                val targetUserId = call.parameters["targetUserId"] ?: throw AuthException("Missing targetUserId")
                GroupChatService.removeMember(groupId, userId, targetUserId)
                call.respond(HttpStatusCode.OK, mapOf("ok" to true))
            }
            post("/{groupId}/leave") {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                val groupId = call.parameters["groupId"] ?: throw AuthException("Missing groupId")
                val req = call.receive<LeaveGroupRequest>()
                GroupChatService.leaveGroup(groupId, userId, req.dissolve, req.successorId, req.random, req.systemOwner, req.securedMode, req.isPublic)
                call.respond(HttpStatusCode.OK, mapOf("ok" to true))
            }
            post("/{groupId}/reset-link") {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                val groupId = call.parameters["groupId"] ?: throw AuthException("Missing groupId")
                call.respond(HttpStatusCode.OK, GroupChatService.resetInviteLink(groupId, userId))
            }
            post("/{groupId}/clear") {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                val groupId = call.parameters["groupId"] ?: throw AuthException("Missing groupId")
                GroupChatService.clearChat(groupId, userId)
                call.respond(HttpStatusCode.OK, mapOf("ok" to true))
            }
            post("/{groupId}/pin-message/{messageId}") {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                val groupId = call.parameters["groupId"] ?: throw AuthException("Missing groupId")
                val messageId = call.parameters["messageId"] ?: throw AuthException("Missing messageId")
                call.respond(HttpStatusCode.OK, GroupChatService.pinMessage(groupId, userId, messageId))
            }
            post("/{groupId}/unpin-message") {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                val groupId = call.parameters["groupId"] ?: throw AuthException("Missing groupId")
                call.respond(HttpStatusCode.OK, GroupChatService.unpinMessage(groupId, userId))
            }
            post("/{groupId}/report") {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                val groupId = call.parameters["groupId"] ?: throw AuthException("Missing groupId")
                val req = call.receive<ReportGroupRequest>()
                GroupChatService.reportGroup(userId, groupId, req.reason, req.mediaUrl, req.mediaType, req.fileName)
                call.respond(HttpStatusCode.OK, mapOf("ok" to true))
            }
            post("/{groupId}/block") {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                val groupId = call.parameters["groupId"] ?: throw AuthException("Missing groupId")
                GroupChatService.setGroupBlocked(userId, groupId, true)
                call.respond(HttpStatusCode.OK, mapOf("ok" to true))
            }
            delete("/{groupId}/block") {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                val groupId = call.parameters["groupId"] ?: throw AuthException("Missing groupId")
                GroupChatService.setGroupBlocked(userId, groupId, false)
                call.respond(HttpStatusCode.OK, mapOf("ok" to true))
            }
            post("/{groupId}/mute") {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                val groupId = call.parameters["groupId"] ?: throw AuthException("Missing groupId")
                GroupChatService.setGroupMuted(userId, groupId, true)
                call.respond(HttpStatusCode.OK, mapOf("ok" to true))
            }
            delete("/{groupId}/mute") {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                val groupId = call.parameters["groupId"] ?: throw AuthException("Missing groupId")
                GroupChatService.setGroupMuted(userId, groupId, false)
                call.respond(HttpStatusCode.OK, mapOf("ok" to true))
            }
            put("/{groupId}/dm-preference") {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                val groupId = call.parameters["groupId"] ?: throw AuthException("Missing groupId")
                val req = call.receive<SetDmOverrideRequest>()
                GroupChatService.setDmOverride(userId, groupId, req.dmOverride)
                call.respond(HttpStatusCode.OK, mapOf("ok" to true))
            }
            post("/{groupId}/join-requests") {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                val groupId = call.parameters["groupId"] ?: throw AuthException("Missing groupId")
                GroupChatService.requestToJoin(groupId, userId)
                call.respond(HttpStatusCode.OK, mapOf("ok" to true))
            }
            get("/{groupId}/join-requests") {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                val groupId = call.parameters["groupId"] ?: throw AuthException("Missing groupId")
                call.respond(HttpStatusCode.OK, GroupChatService.listJoinRequests(groupId, userId))
            }
            post("/{groupId}/join-requests/{targetUserId}/approve") {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                val groupId = call.parameters["groupId"] ?: throw AuthException("Missing groupId")
                val targetUserId = call.parameters["targetUserId"] ?: throw AuthException("Missing targetUserId")
                GroupChatService.approveJoinRequest(groupId, userId, targetUserId)
                call.respond(HttpStatusCode.OK, mapOf("ok" to true))
            }
            post("/{groupId}/join-requests/{targetUserId}/reject") {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                val groupId = call.parameters["groupId"] ?: throw AuthException("Missing groupId")
                val targetUserId = call.parameters["targetUserId"] ?: throw AuthException("Missing targetUserId")
                GroupChatService.rejectJoinRequest(groupId, userId, targetUserId)
                call.respond(HttpStatusCode.OK, mapOf("ok" to true))
            }
            get("/{groupId}/media-summary") {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                val groupId = call.parameters["groupId"] ?: throw AuthException("Missing groupId")
                call.respond(HttpStatusCode.OK, GroupChatService.getMediaSummary(groupId, userId))
            }
            delete("/{groupId}/media") {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                val groupId = call.parameters["groupId"] ?: throw AuthException("Missing groupId")
                GroupChatService.clearAllMedia(groupId, userId)
                call.respond(HttpStatusCode.OK, mapOf("ok" to true))
            }
        }
    }
}
