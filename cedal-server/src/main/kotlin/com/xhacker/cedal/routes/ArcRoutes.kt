package com.xhacker.cedal.routes

import com.xhacker.cedal.models.AndroidBuildCallbackRequest
import com.xhacker.cedal.models.ArcChatHistoryResponse
import com.xhacker.cedal.models.ArcChatMessageDto
import com.xhacker.cedal.models.ArcChatRequest
import com.xhacker.cedal.models.ArcChatResponse
import com.xhacker.cedal.models.ArcMissionCompleteRequest
import com.xhacker.cedal.models.ArcMissionCompleteResponse
import com.xhacker.cedal.models.ArcMissionRequest
import com.xhacker.cedal.models.ArcPracticeAppStatusResponse
import com.xhacker.cedal.models.EditAiMessageRequest
import com.xhacker.cedal.services.AiChatHistoryService
import com.xhacker.cedal.services.ArcChatService
import com.xhacker.cedal.services.ArcOpsService
import com.xhacker.cedal.services.ArcPracticeAppService
import com.xhacker.cedal.services.AuthException
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

private fun AiChatHistoryService.Turn.toArcDto() = ArcChatMessageDto(id, role, content, createdAt, replyToId, editedAt, deleted, mediaUrl, mediaType, fileName)

fun Route.arcRoutes() {
    route("/arc") {
        authenticate("auth-jwt") {
            // ARC's "Assistant" tab - a real AI chat, not a fixed FAQ.
            // Hydrates the chat with the real saved conversation on open -
            // see AiChatHistoryService, replaces the old session-only state.
            get("/chat") {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                val messages = ArcChatService.history(userId).map { it.toArcDto() }
                call.respond(HttpStatusCode.OK, ArcChatHistoryResponse(messages))
            }
            post("/chat") {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                val req = call.receive<ArcChatRequest>()
                if (req.message.isBlank() && req.mediaUrl == null) throw AuthException("Say something first")
                val reply = ArcChatService.reply(userId, req.message, req.replyToId, req.mediaUrl, req.mediaType, req.fileName)
                call.respond(HttpStatusCode.OK, ArcChatResponse(reply.toArcDto()))
            }
            put("/chat/{id}") {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                val id = call.parameters["id"] ?: throw AuthException("Missing id")
                val req = call.receive<EditAiMessageRequest>()
                call.respond(HttpStatusCode.OK, AiChatHistoryService.editMessage(userId, id, req.content).toArcDto())
            }
            delete("/chat/{id}") {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                val id = call.parameters["id"] ?: throw AuthException("Missing id")
                AiChatHistoryService.deleteMessage(userId, id)
                call.respond(HttpStatusCode.OK, mapOf("deleted" to true))
            }
            // ARC Ops - generates one fresh, fictional mission per request,
            // so replaying the same target never repeats the same prompts.
            post("/ops/mission") {
                val req = call.receive<ArcMissionRequest>()
                val mission = ArcOpsService.generateMission(req.targetName)
                call.respond(HttpStatusCode.OK, mission)
            }
            // Not idempotent by design - see ArcOpsService, missions are
            // meant to be replayed and rewarded each time (small, capped).
            post("/ops/complete") {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                val req = call.receive<ArcMissionCompleteRequest>()
                val (awarded, newExp) = ArcOpsService.awardMissionExp(userId, req.scorePercent)
                call.respond(HttpStatusCode.OK, ArcMissionCompleteResponse(newExp, awarded))
            }
            // Real, installable practice-target APK (see ArcPracticeAppService)
            // - kicks off a build the first time this targetId is requested,
            // cached for everyone after that.
            get("/ops/practice-app/{targetId}") {
                val targetId = call.parameters["targetId"] ?: throw AuthException("Missing targetId")
                val status = ArcPracticeAppService.statusOrStartBuild(targetId)
                call.respond(HttpStatusCode.OK, ArcPracticeAppStatusResponse(status.status, status.downloadUrl, status.errorMessage))
            }
        }

        // android-builder calls this back on completion - same shared-secret
        // gate as Code > Kotlin's own build callback, not a user JWT.
        post("/ops/practice-app/{targetId}/callback") {
            val expected = "Bearer ${System.getenv("ANDROID_BUILDER_SECRET") ?: ""}"
            val header = call.request.headers["Authorization"] ?: ""
            if (header.isEmpty() || header != expected) {
                call.respond(HttpStatusCode.Unauthorized)
                return@post
            }
            val targetId = call.parameters["targetId"] ?: throw AuthException("Missing targetId")
            val req = call.receive<AndroidBuildCallbackRequest>()
            ArcPracticeAppService.updateStatus(targetId, req.status, req.downloadUrl, req.errorMessage)
            call.respond(HttpStatusCode.OK, mapOf("ok" to true))
        }
    }
}
