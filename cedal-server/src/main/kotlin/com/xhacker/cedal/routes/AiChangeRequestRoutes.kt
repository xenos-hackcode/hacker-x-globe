package com.xhacker.cedal.routes

import com.xhacker.cedal.services.AiChangeRequestService
import com.xhacker.cedal.services.AuthException
import com.xhacker.cedal.services.DeployService
import com.xhacker.cedal.services.GitHubService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
private data class AiRequestBody(
    val text: String,
    val treePaths: List<String> = emptyList(),
    val currentFile: AiChangeRequestService.CurrentFileDto? = null,
    val replyToId: String? = null,
    val mediaUrl: String? = null,
    val mediaType: String? = null,
    val fileName: String? = null,
    val linkedFiles: List<AiChangeRequestService.CurrentFileDto> = emptyList(),
)

@Serializable
private data class EditAiRequestBody(val text: String)

fun Route.aiChangeRequestRoutes() {
    route("/code/ai-request") {
        authenticate("auth-jwt") {
            // Hydrates the chat with the real saved conversation on open -
            // AiChangeRequests rows already ARE Code AI's turn-by-turn
            // history, see AiChangeRequestService.listHistory.
            get("/history") {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                call.respond(HttpStatusCode.OK, AiChangeRequestService.listHistory(userId))
            }
            // Judging + (sometimes) generating files + opening a PR is a
            // real AI call plus a GitHub round trip - not sub-second, so
            // this acks with the row id immediately and the client polls
            // GET .../{id}, same shape as Android builds.
            post {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                val req = call.receive<AiRequestBody>()
                if (req.text.isBlank() && req.mediaUrl == null) throw AuthException("Ask something first")
                // An image-only send still needs some text for the judge
                // prompt's "A user said: ..." line to read sensibly.
                val text = req.text.ifBlank { "(no caption - see attached image)" }
                val id = AiChangeRequestService.submitAndGetId(userId, text, req.replyToId, req.mediaUrl, req.mediaType, req.fileName)
                CoroutineScope(Dispatchers.IO).launch {
                    AiChangeRequestService.process(id, userId, text, req.treePaths, req.currentFile, req.mediaUrl, req.mediaType, req.linkedFiles)
                }
                call.respond(HttpStatusCode.Accepted, AiChangeRequestService.getDto(id))
            }
            get("/{id}") {
                val id = call.parameters["id"] ?: throw AuthException("Missing id")
                call.respond(HttpStatusCode.OK, AiChangeRequestService.getDto(id))
            }
            // Called once the client has actually performed the fileAction
            // described by this row's fileActionJson - see that column's
            // doc comment in Tables.kt for why this matters.
            post("/{id}/file-action-executed") {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                val id = call.parameters["id"] ?: throw AuthException("Missing id")
                AiChangeRequestService.markFileActionExecuted(userId, id)
                call.respond(HttpStatusCode.OK, AiChangeRequestService.getDto(id))
            }
            put("/{id}") {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                val id = call.parameters["id"] ?: throw AuthException("Missing id")
                val req = call.receive<EditAiRequestBody>()
                call.respond(HttpStatusCode.OK, AiChangeRequestService.editRequest(userId, id, req.text))
            }
            delete("/{id}") {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                val id = call.parameters["id"] ?: throw AuthException("Missing id")
                AiChangeRequestService.deleteRequest(userId, id)
                call.respond(HttpStatusCode.OK, mapOf("deleted" to true))
            }
            // "Delete chat history" - bulk version of the single-message
            // delete above, own rows only.
            delete("/history") {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                AiChangeRequestService.deleteAllHistory(userId)
                call.respond(HttpStatusCode.OK, mapOf("deleted" to true))
            }
        }
    }

    route("/admin/ai-requests") {
        authenticate("auth-jwt") {
            get {
                requireAdmin(call)
                call.respond(HttpStatusCode.OK, AiChangeRequestService.listPending())
            }
            post("/{id}/approve") {
                requireAdmin(call)
                val id = call.parameters["id"] ?: throw AuthException("Missing id")
                val prNumber = AiChangeRequestService.prNumberFor(id) ?: throw AuthException("No PR on this request")
                AiChangeRequestService.markDeploying(id)
                try {
                    val merged = GitHubService.mergePullRequest(prNumber)
                    if (!merged) throw AuthException("GitHub wouldn't merge this PR - check it directly")
                    DeployService.redeployApprovedLanguageChange()
                    AiChangeRequestService.markDeployed(id)
                } catch (e: Exception) {
                    AiChangeRequestService.markError(id, e.message ?: "Deploy failed")
                    throw AuthException("Approved and merged, but the deploy failed: ${e.message}")
                }
                call.respond(HttpStatusCode.OK, AiChangeRequestService.getDto(id))
            }
            post("/{id}/reject") {
                requireAdmin(call)
                val id = call.parameters["id"] ?: throw AuthException("Missing id")
                AiChangeRequestService.reject(id)
                call.respond(HttpStatusCode.OK, mapOf("ok" to true))
            }
        }
    }
}

// Enforced here, not just hidden client-side - same principle as System
// Feed's admin-only posting. Throws (not a manual call.respond) so the
// existing global AuthException -> 400 StatusPages handler is the only
// thing that ever writes a response for this - a respond-then-throw here
// would try to write the response twice.
private fun requireAdmin(call: ApplicationCall) {
    val userId = call.principal<JWTPrincipal>()!!.payload.subject
    if (!AiChangeRequestService.isAdmin(userId)) {
        throw AuthException("Admins only")
    }
}
