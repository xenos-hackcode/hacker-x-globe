package com.xhacker.cedal.services

import com.xhacker.cedal.db.CodeGithubConnections
import com.xhacker.cedal.db.CodeSyncFiles
import com.xhacker.cedal.db.CodeSyncJobs
import com.xhacker.cedal.db.PendingCodeGithubOAuth
import com.xhacker.cedal.models.CodeSyncFileEntry
import com.xhacker.cedal.models.GithubRepoDto
import com.xhacker.cedal.models.GithubStatusDto
import com.xhacker.cedal.models.SyncConflictDto
import com.xhacker.cedal.models.SyncJobDto
import com.xhacker.cedal.models.ResolveConflictResponseDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID

// Code area "Documents" folder <-> a user's OWN GitHub repo, two-way sync.
// Deliberately separate from GitHubService/AiChangeRequestService's flow -
// that one is app-repo-only, one shared fine-grained PAT, no user tokens
// ever involved; this one is arbitrary-repo, per-user OAuth token, and can
// delete files (see GitHubRepoClient.deleteFile) - a completely different
// trust boundary, which is exactly why it only ever uses the lower-level
// GitHubRepoClient class, never the GitHubService object.
//
// Conflict model: CodeSyncFiles holds, per (user, path), the fingerprint
// (GitHub blob sha + local content hash) as of the last successful sync of
// that file. A sync run compares CURRENT local/remote state against that
// stored fingerprint - unchanged-on-both-sides is a no-op, changed-on-one-
// side is a push or pull, changed-on-BOTH is a conflict left completely
// alone (not merged, not auto-resolved) until the user picks a side via
// resolveConflict. Deliberately not a CRDT/merge system - a whole-file
// last-known-good fingerprint is the simplest thing that's actually correct
// for a personal code folder.
object CodeGithubSyncService {
    private val random = SecureRandom()
    private const val PENDING_STATE_TTL_MS = 10L * 60 * 1000

    private val CLIENT_ID = System.getenv("GITHUB_OAUTH_CLIENT_ID") ?: ""
    private val CLIENT_SECRET = System.getenv("GITHUB_OAUTH_CLIENT_SECRET") ?: ""

    private val jsonParser = Json { ignoreUnknownKeys = true }
    private val client = HttpClient(CIO) {
        install(HttpTimeout) { requestTimeoutMillis = 20_000 }
    }

    // A whole-folder sync is a loop of sequential GitHub API calls - too
    // slow for one blocking mobile request, so it runs here and the route
    // just polls CodeSyncJobs (same job-row shape as AndroidBuildService).
    // SupervisorJob so one bad sync's exception can't take down anything
    // else sharing this scope.
    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private fun randomState(): String {
        val b = ByteArray(24)
        random.nextBytes(b)
        return b.joinToString("") { "%02x".format(it) }
    }

    private fun sha256Hex(text: String): String =
        MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }

    // Step 1 (JWT-authenticated route CodeGithubRoutes calls) - mints a
    // state, remembers which user requested it, hands back the URL to
    // launch in a browser. Reuses PlatformDeveloperService's REDIRECT_URI
    // verbatim - GitHub OAuth Apps (classic) have exactly one registered
    // callback URL, but scope IS chosen per-authorization, so this can
    // request "repo" against the same app that flow requests "read:user"
    // against.
    fun authorizeUrl(userId: String): String {
        val state = randomState()
        transaction {
            PendingCodeGithubOAuth.insert {
                it[PendingCodeGithubOAuth.state] = state
                it[PendingCodeGithubOAuth.userId] = UUID.fromString(userId)
                it[expiresAt] = System.currentTimeMillis() + PENDING_STATE_TTL_MS
            }
        }
        return "https://github.com/login/oauth/authorize?client_id=$CLIENT_ID&redirect_uri=${PlatformDeveloperService.REDIRECT_URI}&scope=repo&state=$state"
    }

    // Step 2 (unauthenticated callback, routes/PlatformRoutes.kt) - resolves
    // state back to a userId. Single-use: the row is deleted whether or not
    // it's still valid, so a replayed state never resolves twice.
    fun resolvePendingState(state: String?): String? {
        if (state == null) return null
        return transaction {
            val row = PendingCodeGithubOAuth.selectAll().where { PendingCodeGithubOAuth.state eq state }.firstOrNull()
                ?: return@transaction null
            PendingCodeGithubOAuth.deleteWhere { PendingCodeGithubOAuth.state eq state }
            if (row[PendingCodeGithubOAuth.expiresAt] < System.currentTimeMillis()) return@transaction null
            row[PendingCodeGithubOAuth.userId].value.toString()
        }
    }

    @Serializable
    private data class GitHubTokenResponse(val access_token: String? = null, val error: String? = null)

    @Serializable
    private data class GitHubUserResponse(val id: Long, val login: String)

    // Separate from PlatformDeveloperService.handleGitHubCallback - that one
    // deliberately DISCARDS the token after reading /user (its flow only
    // ever needed a proof of identity). This one is the whole point: keep
    // the token, encrypt it, and store it.
    suspend fun completeOAuth(userId: String, code: String): Boolean {
        val tokenBody = client.post("https://github.com/login/oauth/access_token") {
            contentType(ContentType.Application.Json)
            header("Accept", "application/json")
            setBody("""{"client_id":"$CLIENT_ID","client_secret":"$CLIENT_SECRET","code":"$code","redirect_uri":"${PlatformDeveloperService.REDIRECT_URI}"}""")
        }.body<String>()
        val token = jsonParser.decodeFromString<GitHubTokenResponse>(tokenBody).access_token ?: return false

        val userBody = client.get("https://api.github.com/user") {
            header("Authorization", "Bearer $token")
            header("Accept", "application/vnd.github+json")
        }.body<String>()
        val user = jsonParser.decodeFromString<GitHubUserResponse>(userBody)

        transaction {
            val uid = UUID.fromString(userId)
            CodeGithubConnections.deleteWhere { CodeGithubConnections.userId eq uid }
            CodeGithubConnections.insert {
                it[CodeGithubConnections.userId] = uid
                it[githubId] = user.id.toString()
                it[githubLogin] = user.login
                it[accessTokenEnc] = CryptoService.encrypt(token)
                it[connectedAt] = System.currentTimeMillis()
            }
        }
        return true
    }

    fun status(userId: String): GithubStatusDto = transaction {
        val row = CodeGithubConnections.selectAll().where { CodeGithubConnections.userId eq UUID.fromString(userId) }.firstOrNull()
            ?: return@transaction GithubStatusDto(connected = false)
        GithubStatusDto(
            connected = true,
            githubLogin = row[CodeGithubConnections.githubLogin],
            selectedOwner = row[CodeGithubConnections.selectedOwner],
            selectedRepo = row[CodeGithubConnections.selectedRepo],
            selectedBranch = row[CodeGithubConnections.selectedBranch],
        )
    }

    @Serializable
    private data class GithubApiRepoOwner(val login: String)

    @Serializable
    private data class GithubApiRepo(
        val name: String,
        val owner: GithubApiRepoOwner,
        @SerialName("default_branch") val defaultBranch: String,
        val private: Boolean,
    )

    suspend fun listRepos(userId: String): List<GithubRepoDto> {
        val token = requireToken(userId)
        val body = client.get("https://api.github.com/user/repos?per_page=100&sort=updated") {
            header("Authorization", "Bearer $token")
            header("Accept", "application/vnd.github+json")
        }.body<String>()
        return jsonParser.decodeFromString<List<GithubApiRepo>>(body)
            .map { GithubRepoDto(owner = it.owner.login, name = it.name, defaultBranch = it.defaultBranch, private = it.private) }
    }

    // Switching repos invalidates every prior "last known synced" fingerprint
    // - the next sync starts from a clean baseline against the newly chosen repo.
    fun selectRepo(userId: String, owner: String, repo: String, branch: String): GithubStatusDto {
        val uid = UUID.fromString(userId)
        transaction {
            val updated = CodeGithubConnections.update({ CodeGithubConnections.userId eq uid }) {
                it[selectedOwner] = owner
                it[selectedRepo] = repo
                it[selectedBranch] = branch
            }
            if (updated == 0) throw AuthException("Connect GitHub first")
            CodeSyncFiles.deleteWhere { CodeSyncFiles.userId eq uid }
        }
        return status(userId)
    }

    fun disconnect(userId: String) {
        val uid = UUID.fromString(userId)
        transaction {
            CodeGithubConnections.deleteWhere { CodeGithubConnections.userId eq uid }
            CodeSyncFiles.deleteWhere { CodeSyncFiles.userId eq uid }
        }
    }

    private fun requireToken(userId: String): String = transaction {
        val row = CodeGithubConnections.selectAll().where { CodeGithubConnections.userId eq UUID.fromString(userId) }.firstOrNull()
            ?: throw AuthException("Connect GitHub first")
        CryptoService.decrypt(row[CodeGithubConnections.accessTokenEnc])
    }

    private data class RepoContext(val client: GitHubRepoClient, val branch: String)

    private fun requireRepoContext(userId: String): RepoContext = transaction {
        val row = CodeGithubConnections.selectAll().where { CodeGithubConnections.userId eq UUID.fromString(userId) }.firstOrNull()
            ?: throw AuthException("Connect GitHub first")
        val owner = row[CodeGithubConnections.selectedOwner] ?: throw AuthException("Choose a repo first")
        val repo = row[CodeGithubConnections.selectedRepo] ?: throw AuthException("Choose a repo first")
        val branch = row[CodeGithubConnections.selectedBranch] ?: "main"
        val token = CryptoService.decrypt(row[CodeGithubConnections.accessTokenEnc])
        RepoContext(GitHubRepoClient(owner, repo, token, branch), branch)
    }

    // A conflict is deliberately left untouched by runSync (see class doc
    // comment) until the user picks a side here. Re-running a normal sync
    // afterward would NOT be correct on its own - the stored fingerprint is
    // still the stale pre-conflict one, so both sides would still look
    // "changed" relative to it even after the user's choice is applied
    // locally. This updates the fingerprint directly instead. Returns the
    // content the caller should make sure is on disk (for "keep GitHub",
    // re-fetched fresh rather than trusting client-supplied remote content,
    // in case it changed again since the original sync).
    suspend fun resolveConflict(userId: String, path: String, keepLocal: Boolean, localContent: String): ResolveConflictResponseDto {
        val uid = UUID.fromString(userId)
        val (client, branch) = requireRepoContext(userId)
        return if (keepLocal) {
            val newSha = client.putFile(branch, path, localContent, "Cedal Code sync: resolve conflict (keep local) $path")
            recordFingerprint(uid, path, newSha, sha256Hex(localContent))
            ResolveConflictResponseDto(path, localContent)
        } else {
            val sha = client.getRecursiveTree(branch).firstOrNull { it.path == path }?.sha
                ?: throw AuthException("That file no longer exists on GitHub")
            val content = client.getBlob(sha)
            recordFingerprint(uid, path, sha, sha256Hex(content))
            ResolveConflictResponseDto(path, content)
        }
    }

    // Kicks off a background sync and returns immediately with a jobId to
    // poll - see the class doc comment for why this can't just be one
    // blocking request/response.
    fun startSync(userId: String, files: List<CodeSyncFileEntry>): String {
        val uid = UUID.fromString(userId)
        val jobId = transaction {
            CodeSyncJobs.insertAndGetId {
                it[CodeSyncJobs.userId] = uid
                it[status] = "running"
                it[totalFiles] = files.size
                it[createdAt] = System.currentTimeMillis()
                it[updatedAt] = System.currentTimeMillis()
            }
        }.value.toString()

        syncScope.launch {
            try {
                runSync(jobId, userId, files)
            } catch (e: Exception) {
                finishJobError(jobId, e.message ?: "Sync failed")
            }
        }
        return jobId
    }

    fun getJob(jobId: String): SyncJobDto = transaction {
        val id = UUID.fromString(jobId)
        val row = CodeSyncJobs.selectAll().where { CodeSyncJobs.id eq id }.firstOrNull()
            ?: throw AuthException("Sync job not found")
        SyncJobDto(
            id = jobId,
            status = row[CodeSyncJobs.status],
            totalFiles = row[CodeSyncJobs.totalFiles],
            processedFiles = row[CodeSyncJobs.processedFiles],
            pushed = row[CodeSyncJobs.pushedJson]?.let { jsonParser.decodeFromString<List<String>>(it) } ?: emptyList(),
            pulled = row[CodeSyncJobs.pulledJson]?.let { jsonParser.decodeFromString<List<CodeSyncFileEntry>>(it) } ?: emptyList(),
            deletedRemote = row[CodeSyncJobs.deletedRemoteJson]?.let { jsonParser.decodeFromString<List<String>>(it) } ?: emptyList(),
            deletedLocal = row[CodeSyncJobs.deletedLocalJson]?.let { jsonParser.decodeFromString<List<String>>(it) } ?: emptyList(),
            conflicts = row[CodeSyncJobs.conflictsJson]?.let { jsonParser.decodeFromString<List<SyncConflictDto>>(it) } ?: emptyList(),
            errorMessage = row[CodeSyncJobs.errorMessage],
        )
    }

    private fun finishJobError(jobId: String, message: String) {
        transaction {
            CodeSyncJobs.update({ CodeSyncJobs.id eq UUID.fromString(jobId) }) {
                it[status] = "error"
                it[errorMessage] = message
                it[updatedAt] = System.currentTimeMillis()
            }
        }
    }

    private fun updateProgress(jobId: String, processed: Int) {
        transaction {
            CodeSyncJobs.update({ CodeSyncJobs.id eq UUID.fromString(jobId) }) {
                it[processedFiles] = processed
                it[updatedAt] = System.currentTimeMillis()
            }
        }
    }

    private data class KnownFingerprint(val sha: String?, val hash: String?)

    private fun recordFingerprint(userId: UUID, path: String, sha: String?, hash: String?) {
        transaction {
            CodeSyncFiles.deleteWhere { (CodeSyncFiles.userId eq userId) and (CodeSyncFiles.filePath eq path) }
            CodeSyncFiles.insert {
                it[CodeSyncFiles.userId] = userId
                it[filePath] = path
                it[lastSyncedSha] = sha
                it[lastSyncedContentHash] = hash
                it[lastSyncedAt] = System.currentTimeMillis()
            }
        }
    }

    private fun removeFingerprint(userId: UUID, path: String) {
        transaction {
            CodeSyncFiles.deleteWhere { (CodeSyncFiles.userId eq userId) and (CodeSyncFiles.filePath eq path) }
        }
    }

    private suspend fun runSync(jobId: String, userId: String, localFiles: List<CodeSyncFileEntry>) {
        val uid = UUID.fromString(userId)
        val repoRow = transaction {
            CodeGithubConnections.selectAll().where { CodeGithubConnections.userId eq uid }.firstOrNull()
        }
        if (repoRow == null) { finishJobError(jobId, "Connect GitHub first"); return }
        val owner = repoRow[CodeGithubConnections.selectedOwner]
        val repo = repoRow[CodeGithubConnections.selectedRepo]
        if (owner == null || repo == null) { finishJobError(jobId, "Choose a repo first"); return }
        val branch = repoRow[CodeGithubConnections.selectedBranch] ?: "main"
        val token = CryptoService.decrypt(repoRow[CodeGithubConnections.accessTokenEnc])
        val repoClient = GitHubRepoClient(owner, repo, token, branch)

        val remoteTree = repoClient.getRecursiveTree(branch).associate { it.path to it.sha }
        val known = transaction {
            CodeSyncFiles.selectAll().where { CodeSyncFiles.userId eq uid }
                .associate { it[CodeSyncFiles.filePath] to KnownFingerprint(it[CodeSyncFiles.lastSyncedSha], it[CodeSyncFiles.lastSyncedContentHash]) }
        }
        val localByPath = localFiles.associateBy { it.path }
        val allPaths = localByPath.keys + remoteTree.keys + known.keys

        val pushed = mutableListOf<String>()
        val pulled = mutableListOf<CodeSyncFileEntry>()
        val deletedRemote = mutableListOf<String>()
        val deletedLocal = mutableListOf<String>()
        val conflicts = mutableListOf<SyncConflictDto>()
        var processed = 0

        for (path in allPaths) {
            val local = localByPath[path]
            val localExists = local != null
            val localHash = local?.let { sha256Hex(it.content) }
            val remoteSha = remoteTree[path]
            val remoteExists = remoteSha != null
            val fp = known[path]
            val localChanged = localHash != fp?.hash
            val remoteChanged = remoteSha != fp?.sha

            when {
                // Gone from both sides (independently deleted, or a stale
                // fingerprint for something that no longer exists anywhere)
                // - just drop any leftover fingerprint, nothing to sync.
                !localExists && !remoteExists -> {
                    if (fp != null) removeFingerprint(uid, path)
                }
                !localChanged && !remoteChanged -> { /* matches last sync on both sides */ }
                // A REAL conflict: both sides moved away from the last
                // synced fingerprint. Left untouched - see class doc comment.
                localChanged && remoteChanged -> {
                    val remoteContent = if (remoteExists) repoClient.getBlob(remoteSha!!) else ""
                    conflicts.add(SyncConflictDto(path, local?.content ?: "", remoteContent))
                }
                // Local deleted since last sync, remote unchanged -> delete remote.
                localChanged && !localExists -> {
                    val sha = fp?.sha ?: remoteSha
                    if (sha != null) repoClient.deleteFile(branch, path, sha, "Cedal Code sync: delete $path")
                    removeFingerprint(uid, path)
                    deletedRemote.add(path)
                }
                // Local created/edited, remote unchanged -> push.
                localChanged && localExists -> {
                    val newSha = repoClient.putFile(branch, path, local!!.content, "Cedal Code sync: update $path")
                    recordFingerprint(uid, path, newSha, localHash)
                    pushed.add(path)
                }
                // Remote deleted since last sync, local unchanged -> delete local.
                remoteChanged && !remoteExists -> {
                    removeFingerprint(uid, path)
                    deletedLocal.add(path)
                }
                // Remote created/edited, local unchanged -> pull.
                remoteChanged && remoteExists -> {
                    val content = repoClient.getBlob(remoteSha!!)
                    pulled.add(CodeSyncFileEntry(path, content))
                    recordFingerprint(uid, path, remoteSha, sha256Hex(content))
                }
            }
            processed++
            updateProgress(jobId, processed)
        }

        transaction {
            CodeSyncJobs.update({ CodeSyncJobs.id eq UUID.fromString(jobId) }) {
                it[status] = "done"
                it[pushedJson] = jsonParser.encodeToString(pushed)
                it[pulledJson] = jsonParser.encodeToString(pulled)
                it[deletedRemoteJson] = jsonParser.encodeToString(deletedRemote)
                it[deletedLocalJson] = jsonParser.encodeToString(deletedLocal)
                it[conflictsJson] = jsonParser.encodeToString(conflicts)
                it[updatedAt] = System.currentTimeMillis()
            }
        }
    }
}
