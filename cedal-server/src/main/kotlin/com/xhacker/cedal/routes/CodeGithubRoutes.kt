package com.xhacker.cedal.routes

import com.xhacker.cedal.models.GithubAuthorizeUrlDto
import com.xhacker.cedal.models.ResolveConflictRequest
import com.xhacker.cedal.models.SelectGithubRepoRequest
import com.xhacker.cedal.models.SyncStartRequest
import com.xhacker.cedal.models.SyncStartResponseDto
import com.xhacker.cedal.services.AuthException
import com.xhacker.cedal.services.CodeGithubSyncService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

// Code area "Documents" <-> the user's OWN GitHub repo - see
// CodeGithubSyncService's doc comment for why this is entirely separate
// from CodeRoutes.kt's run/explain-error endpoints (those are stateless,
// this is a per-user OAuth connection + a background sync job).
fun Route.codeGithubRoutes() {
    route("/code/github") {
        authenticate("auth-jwt") {
            get("/authorize-url") {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                call.respond(HttpStatusCode.OK, GithubAuthorizeUrlDto(CodeGithubSyncService.authorizeUrl(userId)))
            }
            get("/status") {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                call.respond(HttpStatusCode.OK, CodeGithubSyncService.status(userId))
            }
            get("/repos") {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                call.respond(HttpStatusCode.OK, CodeGithubSyncService.listRepos(userId))
            }
            post("/select-repo") {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                val req = call.receive<SelectGithubRepoRequest>()
                if (req.owner.isBlank() || req.repo.isBlank()) throw AuthException("Pick a repo first")
                call.respond(HttpStatusCode.OK, CodeGithubSyncService.selectRepo(userId, req.owner, req.repo, req.branch.ifBlank { "main" }))
            }
            post("/disconnect") {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                CodeGithubSyncService.disconnect(userId)
                call.respond(HttpStatusCode.OK, mapOf("ok" to true))
            }
            post("/sync/start") {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                val req = call.receive<SyncStartRequest>()
                val jobId = CodeGithubSyncService.startSync(userId, req.files)
                call.respond(HttpStatusCode.Accepted, SyncStartResponseDto(jobId))
            }
            get("/sync/{jobId}") {
                val jobId = call.parameters["jobId"] ?: throw AuthException("Missing jobId")
                call.respond(HttpStatusCode.OK, CodeGithubSyncService.getJob(jobId))
            }
            post("/sync/resolve") {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                val req = call.receive<ResolveConflictRequest>()
                if (req.path.isBlank()) throw AuthException("Missing path")
                call.respond(HttpStatusCode.OK, CodeGithubSyncService.resolveConflict(userId, req.path, req.keepLocal, req.localContent))
            }
        }
    }
}
