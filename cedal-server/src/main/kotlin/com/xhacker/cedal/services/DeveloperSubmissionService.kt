package com.xhacker.cedal.services

import com.xhacker.cedal.db.DeveloperSubmissions
import com.xhacker.cedal.db.Users
import com.xhacker.cedal.models.DeveloperSubmissionDto
import com.xhacker.cedal.models.SubmitDeveloperPatchRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.util.UUID

// Developer Mode's submit -> Alucard review -> owner approve/deny -> deploy
// pipeline. Deliberately a separate, parallel system from
// AiChangeRequestService (the "Rules" tab's new-language proposal flow),
// not a generalization of it, per the owner's own call - keeps this from
// risking the existing (already fragile, see DeployService's own doc
// comment) language-proposal path.
//
// status walks: pending_stage1 -> stage1_failed | pending_stage2 ->
// stage2_failed | pending_approval -> deploying -> approved | deploy_error,
// or pending_approval -> denied. A submission never advances past a failed
// stage - the developer fixes their code and submits a fresh row.
//
// No generic notification delivery here - see Notifications' own doc
// comment (it's a genuinely GLOBAL, unfiltered feed with no recipient
// column at all, currently only safe for the public "someone wants to
// trade" use case it was built for). A private per-developer approval/
// denial notice instead just lives on the submission row itself
// (status/stage1Result/stage2Result/deniedReason), which the developer
// polls via listMine() - same "poll the domain object's own status field"
// pattern every other async flow in this codebase already uses.
object DeveloperSubmissionService {
    private val scope = CoroutineScope(Dispatchers.IO)

    @Serializable
    private data class ReviewResult(val hasIssue: Boolean, val reason: String = "")

    private fun ResultRow.toDto(): DeveloperSubmissionDto {
        val uid = this[DeveloperSubmissions.userId].value
        val userRow = Users.selectAll().where { Users.id eq uid }.firstOrNull()
        return DeveloperSubmissionDto(
            id = this[DeveloperSubmissions.id].value.toString(),
            userId = uid.toString(),
            userName = userRow?.let { displayNameFor(it) } ?: "Unknown",
            title = this[DeveloperSubmissions.title],
            targetFilePath = this[DeveloperSubmissions.targetFilePath],
            language = this[DeveloperSubmissions.language],
            status = this[DeveloperSubmissions.status],
            stage1Result = this[DeveloperSubmissions.stage1Result],
            stage2Result = this[DeveloperSubmissions.stage2Result],
            deniedReason = this[DeveloperSubmissions.deniedReason],
            prUrl = this[DeveloperSubmissions.prUrl],
            createdAt = this[DeveloperSubmissions.createdAt],
            updatedAt = this[DeveloperSubmissions.updatedAt],
        )
    }

    private fun getOrThrow(id: UUID): DeveloperSubmissionDto = transaction {
        DeveloperSubmissions.selectAll().where { DeveloperSubmissions.id eq id }.first().toDto()
    }

    // Inserts the row immediately (so the client gets something back right
    // away) and kicks the actual AI review off in the background - same
    // "insert now, process after" shape AiChangeRequestService.request()
    // already uses for its own judge call, since a real AI round-trip is
    // too slow to hold the HTTP response open for.
    fun submit(userId: String, req: SubmitDeveloperPatchRequest): DeveloperSubmissionDto {
        val uid = UUID.fromString(userId)
        val now = System.currentTimeMillis()
        val newId = transaction {
            DeveloperSubmissions.insertAndGetId {
                it[DeveloperSubmissions.userId] = uid
                it[title] = req.title
                it[targetFilePath] = req.targetFilePath
                it[code] = req.code
                it[language] = req.language
                it[status] = "pending_stage1"
                it[createdAt] = now
                it[updatedAt] = now
            }
        }.value
        scope.launch { processReview(newId) }
        return getOrThrow(newId)
    }

    fun get(id: String): DeveloperSubmissionDto? = transaction {
        DeveloperSubmissions.selectAll().where { DeveloperSubmissions.id eq UUID.fromString(id) }.firstOrNull()?.toDto()
    }

    fun listMine(userId: String): List<DeveloperSubmissionDto> = transaction {
        DeveloperSubmissions.selectAll().where { DeveloperSubmissions.userId eq UUID.fromString(userId) }
            .orderBy(DeveloperSubmissions.createdAt, SortOrder.DESC)
            .map { it.toDto() }
    }

    // Admin-only (requireAdmin in the route) - every delegated developer's
    // pending submissions in one queue, not just the owner's own.
    fun listPendingApprovals(): List<DeveloperSubmissionDto> = transaction {
        DeveloperSubmissions.selectAll().where { DeveloperSubmissions.status eq "pending_approval" }
            .orderBy(DeveloperSubmissions.createdAt, SortOrder.ASC)
            .map { it.toDto() }
    }

    private suspend fun processReview(id: UUID) {
        val loaded = transaction {
            DeveloperSubmissions.selectAll().where { DeveloperSubmissions.id eq id }.firstOrNull()
                ?.let { it[DeveloperSubmissions.code] to it[DeveloperSubmissions.language] }
        } ?: return
        val (code, language) = loaded

        val stage1 = review(stage1Prompt(language, code))
        if (stage1.hasIssue) {
            transaction {
                DeveloperSubmissions.update({ DeveloperSubmissions.id eq id }) {
                    it[status] = "stage1_failed"
                    it[stage1Result] = stage1.reason.ifBlank { "Issue found." }
                    it[updatedAt] = System.currentTimeMillis()
                }
            }
            return
        }
        transaction {
            DeveloperSubmissions.update({ DeveloperSubmissions.id eq id }) {
                it[stage1Result] = "No issues found."
                it[status] = "pending_stage2"
                it[updatedAt] = System.currentTimeMillis()
            }
        }

        val stage2 = review(stage2Prompt(language, code))
        if (stage2.hasIssue) {
            transaction {
                DeveloperSubmissions.update({ DeveloperSubmissions.id eq id }) {
                    it[status] = "stage2_failed"
                    it[stage2Result] = stage2.reason.ifBlank { "Issue found." }
                    it[updatedAt] = System.currentTimeMillis()
                }
            }
            return
        }
        transaction {
            DeveloperSubmissions.update({ DeveloperSubmissions.id eq id }) {
                it[stage2Result] = "No issues found."
                it[status] = "pending_approval"
                it[updatedAt] = System.currentTimeMillis()
            }
        }
    }

    private suspend fun review(prompt: String): ReviewResult {
        val raw = AiProviderService.ask(prompt, maxTokens = 1000)
        // The prompt asks for bare JSON, but models sometimes wrap it in a
        // markdown code fence anyway - strip that defensively before
        // parsing, same as CodeBackerService's identical review call.
        val cleaned = raw.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        return try {
            AiProviderService.jsonParser.decodeFromString<ReviewResult>(cleaned)
        } catch (e: Exception) {
            // A parsing hiccup shouldn't permanently strand a submission -
            // same "treat as no issue" fallback CodeBackerService uses. A
            // genuine problem still gets caught at the human-approval stage
            // either way.
            ReviewResult(hasIssue = false)
        }
    }

    private fun stage1Prompt(language: String, code: String) =
        "You are Alucard, Cedal's security reviewer. Stage 1: scan this $language code for anything " +
            "MALICIOUS - backdoors, data exfiltration, destructive/irreversible operations, credential/secret " +
            "theft, privilege escalation, or code deliberately designed to harm the system it runs on or its " +
            "users. This is NOT a general code-quality review - only flag genuine malicious intent or " +
            "dangerous capability, not style/bugs/inefficiency.\n\nCode:\n$code\n\n" +
            "Reply with ONLY valid JSON, no markdown, no code fences, in exactly this shape: " +
            "{\"hasIssue\": true or false, \"reason\": \"one or two short plain-English sentences if true, empty string if false\"}."

    private fun stage2Prompt(language: String, code: String) =
        "You are Alucard, Cedal's security reviewer. Stage 2 (this code already passed the malicious-content " +
            "scan): do a deeper internal review of this $language code for security anti-patterns, bugs that " +
            "could break the live app, injection risks, unsafe input handling, or anything that could " +
            "destabilize production if this were deployed as-is.\n\nCode:\n$code\n\n" +
            "Reply with ONLY valid JSON, no markdown, no code fences, in exactly this shape: " +
            "{\"hasIssue\": true or false, \"reason\": \"one or two short plain-English sentences if true, empty string if false\"}."

    // Admin-only (requireAdmin in the route). Opens a branch, writes the
    // target file, PRs it, merges, then redeploys cedal-server for real -
    // GitHubService/DeployService are both already fully generic, no
    // changes needed beyond calling them with this submission's data.
    suspend fun approve(id: String): DeveloperSubmissionDto {
        val uid = UUID.fromString(id)
        val row = transaction { DeveloperSubmissions.selectAll().where { DeveloperSubmissions.id eq uid }.firstOrNull() }
            ?: throw AuthException("Submission not found")
        if (row[DeveloperSubmissions.status] != "pending_approval") throw AuthException("Not awaiting approval")

        val branch = "dev-submission-${uid.toString().take(8)}-${System.currentTimeMillis()}"
        GitHubService.createBranch(branch)
        GitHubService.putFile(branch, row[DeveloperSubmissions.targetFilePath], row[DeveloperSubmissions.code], "Developer submission: ${row[DeveloperSubmissions.title]}")
        val pr = GitHubService.openPullRequest(branch, row[DeveloperSubmissions.title], "Submitted via Developer Mode, approved by the owner after passing Alucard's automated review.")
        GitHubService.mergePullRequest(pr.number)

        // The merge above is already real and permanent by this point - a
        // deploy failure past here shouldn't read as "never approved" (see
        // AiChangeRequestService's identical merge-then-deploy ordering for
        // its own approve(), including not rolling the merge back on a
        // deploy failure).
        transaction {
            DeveloperSubmissions.update({ DeveloperSubmissions.id eq uid }) {
                it[status] = "deploying"
                it[prUrl] = pr.htmlUrl
                it[updatedAt] = System.currentTimeMillis()
            }
        }
        try {
            DeployService.redeployCedalServer()
            transaction {
                DeveloperSubmissions.update({ DeveloperSubmissions.id eq uid }) {
                    it[status] = "approved"
                    it[updatedAt] = System.currentTimeMillis()
                }
            }
        } catch (e: Exception) {
            transaction {
                DeveloperSubmissions.update({ DeveloperSubmissions.id eq uid }) {
                    it[status] = "deploy_error"
                    it[deniedReason] = "Merged but redeploy failed: ${e.message}"
                    it[updatedAt] = System.currentTimeMillis()
                }
            }
            throw e
        }
        return getOrThrow(uid)
    }

    fun deny(id: String, reason: String): DeveloperSubmissionDto {
        val uid = UUID.fromString(id)
        transaction {
            DeveloperSubmissions.update({ DeveloperSubmissions.id eq uid }) {
                it[status] = "denied"
                it[deniedReason] = reason
                it[updatedAt] = System.currentTimeMillis()
            }
        }
        return getOrThrow(uid)
    }
}
