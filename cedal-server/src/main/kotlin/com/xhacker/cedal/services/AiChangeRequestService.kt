package com.xhacker.cedal.services

import com.xhacker.cedal.db.AiChangeRequests
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.SqlExpressionBuilder.neq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.util.UUID

// The Code screen's AI tab. Three unrelated things can happen from one
// message, decided by a single judge call:
// - "answer": a plain Q&A reply, nothing else happens.
// - "newLanguage": the narrow, admin-approval-gated GitHub PR pipeline
//   below - the only code change it can ever propose is "add a new
//   interpreted language to runner/Dockerfile + runner/server.js +
//   CodeExecutionService.kt's LANGUAGES list," never "edit anything in the
//   repo." Always ends up "pending_approval" - never auto-merged, see
//   AiChangeRequestRoutes/DeployService for the human gate.
// - "fileAction": a direct create/edit/delete/run against the user's OWN
//   on-device Code area files - resolved immediately (status "answered"),
//   never a GitHub PR, never approval-gated, since it only ever touches
//   that one user's own sandboxed files (the server can't reach them at
//   all - see fileActionJson's execution in MemberCodeBody client-side).
object AiChangeRequestService {
    private val jsonParser = Json { ignoreUnknownKeys = true }

    private const val DOCKERFILE_PATH = "cedal-mobile/runner/Dockerfile"
    private const val SERVER_JS_PATH = "cedal-mobile/runner/server.js"
    private const val LANGUAGES_PATH = "cedal-server/src/main/kotlin/com/xhacker/cedal/services/CodeExecutionService.kt"

    // Known-stable anchors in the CURRENT (freshly fetched, not assumed)
    // file content - the AI only ever fills in the small piece that gets
    // spliced at these exact points, never regenerates the surrounding
    // file, so there's no risk of it subtly corrupting unrelated code.
    private const val DOCKERFILE_ANCHOR = "WORKDIR /app"
    private const val SERVER_JS_ANCHOR = "    } else {\n      return res.status(400).json({ error: \"unsupported language\" });\n    }"
    private const val LANGUAGES_ANCHOR = "\"C\", \"C++\", \"Go\", \"Rust\", \"Java\", \"C#\","

    @Serializable
    data class CurrentFileDto(val path: String, val content: String)

    @Serializable
    data class FileActionDto(val action: String, val path: String, val content: String? = null)

    @Serializable
    private data class JudgeResult(val kind: String = "answer", val answer: String = "", val summary: String = "")

    @Serializable
    private data class BuildResult(
        val languageName: String = "",
        val dockerfileLines: String = "",
        val serverJsBranch: String = "",
        val summary: String = "",
    )

    @Serializable
    private data class FileActionResult(
        val action: String = "",
        val path: String = "",
        val content: String = "",
        val reply: String = "",
    )

    @Serializable
    data class AiChangeRequestDto(
        val id: String,
        val status: String,
        val requestText: String? = null,
        val answerText: String? = null,
        val summary: String? = null,
        val prUrl: String? = null,
        val errorMessage: String? = null,
        val fileAction: FileActionDto? = null,
        val createdAt: Long = 0,
        val replyToId: String? = null,
        val requestEditedAt: Long? = null,
        val requestDeleted: Boolean = false,
        val mediaUrl: String? = null,
        val mediaType: String? = null,
        val fileName: String? = null,
    )

    // Matches ChatService/AiChatHistoryService's edit window.
    private val EDIT_WINDOW_MS = 5 * 60 * 1000L

    fun submitAndGetId(
        userId: String,
        requestText: String,
        replyToId: String? = null,
        mediaUrl: String? = null,
        mediaType: String? = null,
        fileName: String? = null,
    ): String = transaction {
        AiChangeRequests.insertAndGetId {
            it[requesterUserId] = UUID.fromString(userId)
            it[AiChangeRequests.requestText] = requestText
            it[status] = "pending_judgment"
            it[AiChangeRequests.replyToId] = replyToId
            it[AiChangeRequests.mediaUrl] = mediaUrl
            it[AiChangeRequests.mediaType] = mediaType
            it[AiChangeRequests.fileName] = fileName
            it[createdAt] = System.currentTimeMillis()
        }.value.toString()
    }

    // Edit/delete apply only to the user's own half of a row (requestText) -
    // the assistant's half is never user-editable, same rule as every other
    // chat surface ("if it is our chat text").
    fun editRequest(userId: String, requestId: String, newText: String): AiChangeRequestDto = transaction {
        val uid = UUID.fromString(userId)
        val rid = UUID.fromString(requestId)
        val row = AiChangeRequests.selectAll().where { AiChangeRequests.id eq rid }.firstOrNull() ?: throw AuthException("Request not found")
        if (row[AiChangeRequests.requesterUserId].value != uid) throw AuthException("You can only edit your own messages")
        if (row[AiChangeRequests.requestDeleted]) throw AuthException("Can't edit a deleted message")
        if (System.currentTimeMillis() - row[AiChangeRequests.createdAt] > EDIT_WINDOW_MS) throw AuthException("Too late to edit this message")
        val trimmed = newText.trim()
        if (trimmed.isBlank()) throw AuthException("Message can't be empty")

        AiChangeRequests.update({ AiChangeRequests.id eq rid }) {
            it[AiChangeRequests.requestText] = trimmed
            it[requestEditedAt] = System.currentTimeMillis()
        }
        toDto(requestId, AiChangeRequests.selectAll().where { AiChangeRequests.id eq rid }.first())
    }

    fun deleteRequest(userId: String, requestId: String) {
        transaction {
            val uid = UUID.fromString(userId)
            val rid = UUID.fromString(requestId)
            val row = AiChangeRequests.selectAll().where { AiChangeRequests.id eq rid }.firstOrNull() ?: throw AuthException("Request not found")
            if (row[AiChangeRequests.requesterUserId].value != uid) throw AuthException("You can only delete your own messages")
            AiChangeRequests.update({ AiChangeRequests.id eq rid }) { it[requestDeleted] = true }
        }
    }

    // "Delete chat history" - same tombstone semantics as a single-message
    // delete, applied to every one of this user's own rows at once. Never
    // touches answerText/summary/prUrl/status - a language request already
    // sent to the admin stays exactly as real and pending as it was; this
    // only clears what the user sees of their own side of the conversation.
    fun deleteAllHistory(userId: String) {
        transaction {
            AiChangeRequests.update({ AiChangeRequests.requesterUserId eq UUID.fromString(userId) }) { it[requestDeleted] = true }
        }
    }

    // Runs the judge, and whichever of the three flows above it picks -
    // split from submitAndGetId so the route can respond with the row
    // immediately and let this run in the background (an AI call plus,
    // sometimes, a GitHub round trip is not a sub-second operation).
    suspend fun process(requestId: String, userId: String, requestText: String, treePaths: List<String>, currentFile: CurrentFileDto?, mediaUrl: String? = null, mediaType: String? = null) {
        try {
            val languages = CodeExecutionService.LANGUAGES.joinToString(", ")
            val crossReference = AiChatHistoryService.crossReferenceBlock(userId, excluding = "code")
            val treeDescription = treePaths.takeIf { it.isNotEmpty() }?.joinToString(", ") ?: "(no files yet)"
            val currentFileDescription = currentFile?.let { "They currently have this file open in the editor:\nPath: ${it.path}\nContent:\n${it.content}" }
                ?: "No file is currently open in the editor."
            val adminLine = if (isAdmin(userId)) {
                "\nThe person asking right now is Cedal's admin - the one who actually built and created you. Feel free to acknowledge that naturally if it comes up, without making every reply about it."
            } else {
                ""
            }

            val judgePrompt = """
                You're the assistant behind Cedal's Code screen. It currently supports these languages: $languages.
                Every supported language already lets the user install extra packages live (pip/npm/gem/composer as appropriate) via a "packages" field - that already works today, so a request for a package MANAGER TOOL (poetry, yarn, pnpm, bundler, etc.) for an ALREADY-supported language does not need a code change, only a request for a genuinely NEW language does.
                $adminLine

                The user's own Code area currently has these files (paths only): $treeDescription
                $currentFileDescription
                ${if (crossReference.isNotBlank()) "\n$crossReference" else ""}

                A user said: "$requestText"

                Decide what kind of request this is:
                - "fileAction": they want you to directly create, edit, delete, or run a file in THEIR OWN Code area above - not a request to add a new language to the app itself.
                - "newLanguage": they're asking to add support for a genuinely new programming language to the app.
                - "answer": anything else - a question you can just answer directly (including confirming or correcting a claim about what one of Cedal's other AI assistants said, using the reference context above if given).

                Reply with ONLY valid JSON, no markdown, no code fences, exactly this shape:
                {"kind": "fileAction" or "newLanguage" or "answer", "answer": "your direct answer if kind is answer, empty string otherwise", "summary": "one short line describing the language to add if kind is newLanguage, empty string otherwise"}
            """.trimIndent()

            val judgeReply = if (mediaType == "image" && mediaUrl != null) {
                AiProviderService.askWithImage(judgePrompt, mediaUrl, maxTokens = 500)
            } else {
                AiProviderService.ask(judgePrompt, maxTokens = 500)
            }
            val judge = parseJson<JudgeResult>(judgeReply) ?: JudgeResult()

            when (judge.kind) {
                "fileAction" -> {
                    processFileAction(requestId, requestText, treeDescription, currentFileDescription)
                    return
                }
                "newLanguage" -> {
                    processNewLanguage(requestId, judge.summary)
                    return
                }
                else -> {
                    updateStatus(requestId, status = "answered", answerText = judge.answer.ifBlank { "I couldn't work out a clear answer to that - try rephrasing." })
                    return
                }
            }
        } catch (e: Exception) {
            updateStatus(requestId, status = "error", errorMessage = "Failed: ${e.message}")
        }
    }

    private suspend fun processFileAction(requestId: String, requestText: String, treeDescription: String, currentFileDescription: String) {
        val actionPrompt = """
            The user asked: "$requestText"
            Their Code area file tree (paths only): $treeDescription
            $currentFileDescription

            Decide the single best file action to satisfy this request. Produce ONLY valid JSON, no markdown, no code fences, exactly this shape:
            {"action": "createFile" or "editFile" or "deleteFile" or "runFile", "path": "a relative path like utils/helper.py", "content": "the COMPLETE file content for createFile/editFile - not a diff, empty string for deleteFile/runFile", "reply": "one or two short sentences telling the user what you did"}
            For editFile/deleteFile/runFile, path must reference a real file from the tree above. For createFile, use a sensible new path (create any needed folders as part of the path).
        """.trimIndent()

        val action = parseJson<FileActionResult>(AiProviderService.ask(actionPrompt, maxTokens = 2000))
        if (action == null || action.action.isBlank() || action.path.isBlank()) {
            updateStatus(requestId, status = "error", errorMessage = "Couldn't work out a file action for that - try being more specific about the file name.")
            return
        }

        val fileActionJson = jsonParser.encodeToString(FileActionDto(action.action, action.path, action.content))
        transaction {
            AiChangeRequests.update({ AiChangeRequests.id eq UUID.fromString(requestId) }) {
                it[status] = "answered"
                it[answerText] = action.reply.ifBlank { "Done." }
                it[AiChangeRequests.fileActionJson] = fileActionJson
            }
        }
    }

    private suspend fun processNewLanguage(requestId: String, summary: String) {
        val buildPrompt = """
            You're adding support for a new language to Cedal's code runner. Follow this EXACT existing pattern - Bash is the most recently added language, shown here as your example:

            Dockerfile installs system packages like this (a plain apt-get RUN line):
            RUN apt-get update && apt-get install -y --no-install-recommends bash && rm -rf /var/lib/apt/lists/*

            server.js dispatches each language like this:
            } else if (language === "Bash") {
              const file = path.join(workDir, "main.sh");
              await fs.writeFile(file, code);
              result = await runProcess("bash", [file], workDir, stdin);
            }

            The new language to add, per this summary: "$summary"

            Produce ONLY valid JSON, no markdown, no code fences, exactly this shape:
            {"languageName": "the exact display name for the LANGUAGES list, e.g. \"Elixir\"", "dockerfileLines": "one or more RUN lines installing the interpreter/runtime - nothing else", "serverJsBranch": "the complete `} else if (language === \"X\") { ... }` block body, following the Bash example exactly", "summary": "one line describing what this adds"}
        """.trimIndent()

        val build = parseJson<BuildResult>(AiProviderService.ask(buildPrompt, maxTokens = 1500))
        if (build == null || build.languageName.isBlank() || build.serverJsBranch.isBlank()) {
            updateStatus(requestId, status = "error", errorMessage = "The AI couldn't produce a usable proposal for this request.")
            return
        }

        // Dedup across ALL users - if someone else already requested (and it's
        // still pending, or already shipped) essentially the same language,
        // don't open a second PR for the admin to review twice. Matched on
        // languageName appearing in another row's summary - simple substring
        // check, good enough since the build prompt always names the language
        // plainly (e.g. "Add Elixir support").
        val existing = transaction {
            AiChangeRequests.selectAll()
                .where {
                    (AiChangeRequests.id neq UUID.fromString(requestId)) and
                        (AiChangeRequests.status inList listOf("pending_approval", "deploying", "deployed"))
                }
                .firstOrNull { it[AiChangeRequests.summary]?.contains(build.languageName, ignoreCase = true) == true }
        }
        if (existing != null) {
            val already = existing[AiChangeRequests.status]
            val message = if (already == "deployed") {
                "${build.languageName} support was already added by another request - it should already work, try it!"
            } else {
                "Someone already requested ${build.languageName} support and it's already been sent to the admin for review - no need to send it twice, I'll let you know here once it's decided."
            }
            updateStatus(requestId, status = "answered", answerText = message)
            return
        }

        val branch = "ai-request-${requestId.take(8)}"
        GitHubService.createBranch(branch)

        val dockerfile = GitHubService.getFileContent(branch, DOCKERFILE_PATH)
        GitHubService.putFile(
            branch, DOCKERFILE_PATH,
            dockerfile.replaceFirst(DOCKERFILE_ANCHOR, "${build.dockerfileLines.trim()}\n\n$DOCKERFILE_ANCHOR"),
            "Add ${build.languageName} runtime",
        )

        val serverJs = GitHubService.getFileContent(branch, SERVER_JS_PATH)
        GitHubService.putFile(
            branch, SERVER_JS_PATH,
            serverJs.replaceFirst(SERVER_JS_ANCHOR, "${build.serverJsBranch.trim()}\n$SERVER_JS_ANCHOR"),
            "Dispatch ${build.languageName} in runner",
        )

        val languagesFile = GitHubService.getFileContent(branch, LANGUAGES_PATH)
        GitHubService.putFile(
            branch, LANGUAGES_PATH,
            languagesFile.replaceFirst(LANGUAGES_ANCHOR, "$LANGUAGES_ANCHOR \"${build.languageName}\","),
            "Register ${build.languageName} in the language list",
        )

        val pr = GitHubService.openPullRequest(
            branch = branch,
            title = "Add ${build.languageName} support (AI-generated, request $requestId)",
            body = "Requested via the Code screen's AI. ${build.summary}\n\nGenerated automatically - review before approving in the app.",
        )

        val filesJson = jsonParser.encodeToString(listOf(DOCKERFILE_PATH, SERVER_JS_PATH, LANGUAGES_PATH))
        transaction {
            AiChangeRequests.update({ AiChangeRequests.id eq UUID.fromString(requestId) }) {
                it[status] = "pending_approval"
                it[AiChangeRequests.summary] = build.summary.ifBlank { "Add ${build.languageName} support" }
                it[AiChangeRequests.filesJson] = filesJson
                it[prUrl] = pr.htmlUrl
            }
        }
    }

    fun listPending(): List<AiChangeRequestDto> = transaction {
        AiChangeRequests.selectAll()
            .where { AiChangeRequests.status eq "pending_approval" }
            .map {
                AiChangeRequestDto(
                    id = it[AiChangeRequests.id].value.toString(),
                    status = it[AiChangeRequests.status],
                    summary = it[AiChangeRequests.summary],
                    prUrl = it[AiChangeRequests.prUrl],
                )
            }
    }

    // Code AI's own chat history - AiChangeRequests rows already ARE its
    // turn-by-turn conversation (requestText = what the user asked,
    // answerText/summary = the reply), so this needs no separate table,
    // unlike Corneal/ARC (see AiChatHistoryService). Own rows only, oldest
    // first so the client can render it like a normal chat transcript.
    // fileAction is intentionally included here too - the client only ever
    // executes it right after a fresh submit, never on hydration replay.
    fun listHistory(userId: String, limit: Int = 50): List<AiChangeRequestDto> = transaction {
        AiChangeRequests.selectAll()
            .where { AiChangeRequests.requesterUserId eq UUID.fromString(userId) }
            .orderBy(AiChangeRequests.createdAt, SortOrder.DESC)
            .limit(limit)
            .map { toDto(it[AiChangeRequests.id].value.toString(), it) }
            .reversed()
    }

    fun getDto(requestId: String): AiChangeRequestDto = transaction {
        val row = AiChangeRequests.selectAll().where { AiChangeRequests.id eq UUID.fromString(requestId) }.firstOrNull()
            ?: throw AuthException("Request not found")
        toDto(requestId, row)
    }

    private fun toDto(requestId: String, row: ResultRow): AiChangeRequestDto {
        val isDeleted = row[AiChangeRequests.requestDeleted]
        return AiChangeRequestDto(
            id = requestId,
            status = row[AiChangeRequests.status],
            requestText = if (isDeleted) "" else row[AiChangeRequests.requestText],
            answerText = row[AiChangeRequests.answerText],
            summary = row[AiChangeRequests.summary],
            prUrl = row[AiChangeRequests.prUrl],
            errorMessage = row[AiChangeRequests.errorMessage],
            fileAction = row[AiChangeRequests.fileActionJson]?.let { parseJson<FileActionDto>(it) },
            createdAt = row[AiChangeRequests.createdAt],
            replyToId = row[AiChangeRequests.replyToId],
            requestEditedAt = row[AiChangeRequests.requestEditedAt],
            requestDeleted = isDeleted,
            mediaUrl = if (isDeleted) null else row[AiChangeRequests.mediaUrl],
            mediaType = if (isDeleted) null else row[AiChangeRequests.mediaType],
            fileName = if (isDeleted) null else row[AiChangeRequests.fileName],
        )
    }

    fun isAdmin(userId: String): Boolean = AdminService.isAdmin(userId)

    fun reject(requestId: String) {
        updateStatus(requestId, status = "rejected")
    }

    // Parsed from the stored PR URL (https://github.com/OWNER/REPO/pull/N)
    // rather than a separate column - the URL is already the single source
    // of truth for which PR this row is about.
    fun prNumberFor(requestId: String): Int? = transaction {
        val url = AiChangeRequests.selectAll().where { AiChangeRequests.id eq UUID.fromString(requestId) }.firstOrNull()
            ?.get(AiChangeRequests.prUrl) ?: return@transaction null
        url.substringAfterLast("/").toIntOrNull()
    }

    fun markDeploying(requestId: String) = updateStatus(requestId, status = "deploying")
    fun markDeployed(requestId: String) = updateStatus(requestId, status = "deployed")
    fun markError(requestId: String, message: String) = updateStatus(requestId, status = "error", errorMessage = message)

    private fun updateStatus(requestId: String, status: String, answerText: String? = null, errorMessage: String? = null) {
        transaction {
            AiChangeRequests.update({ AiChangeRequests.id eq UUID.fromString(requestId) }) {
                it[AiChangeRequests.status] = status
                if (answerText != null) it[AiChangeRequests.answerText] = answerText
                if (errorMessage != null) it[AiChangeRequests.errorMessage] = errorMessage
            }
        }
    }

    private inline fun <reified T> parseJson(raw: String): T? {
        val cleaned = raw.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        return try {
            jsonParser.decodeFromString<T>(cleaned)
        } catch (e: Exception) {
            null
        }
    }
}
