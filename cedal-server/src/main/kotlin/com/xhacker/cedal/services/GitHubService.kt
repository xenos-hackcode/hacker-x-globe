package com.xhacker.cedal.services

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.util.Base64

// Per-(owner, repo, token) GitHub API client - the general-purpose version
// of what GitHubService (below) wraps as one hardcoded instance for the
// Rules-tab AI flow. CodeGithubSyncService constructs one of these per user
// with THEIR OWN decrypted token/chosen repo instead, since that flow needs
// arbitrary owner/repo, not this app's one source repo. Shares one
// HttpClient connection pool across every instance (companion object) since
// a whole-folder sync issues a lot of sequential calls.
class GitHubRepoClient(
    private val owner: String,
    private val repo: String,
    private val token: String,
    private val baseBranch: String = "main",
) {
    companion object {
        private const val API = "https://api.github.com"
        private val jsonParser = Json { ignoreUnknownKeys = true }
        private val client = HttpClient(CIO) {
            install(HttpTimeout) { requestTimeoutMillis = 20_000 }
        }
    }

    private fun io.ktor.client.request.HttpRequestBuilder.githubAuth() {
        header("Authorization", "Bearer $token")
        header("Accept", "application/vnd.github+json")
        header("X-GitHub-Api-Version", "2022-11-28")
    }

    private suspend fun latestBaseSha(): String {
        val body = client.get("$API/repos/$owner/$repo/git/ref/heads/$baseBranch") { githubAuth() }.body<String>()
        val res = jsonParser.decodeFromString<RefResponse>(body)
        return res.`object`.sha
    }

    suspend fun createBranch(newBranch: String) {
        val baseSha = latestBaseSha()
        client.post("$API/repos/$owner/$repo/git/refs") {
            githubAuth()
            contentType(ContentType.Application.Json)
            setBody(jsonParser.encodeToString(CreateRefRequest.serializer(), CreateRefRequest("refs/heads/$newBranch", baseSha)))
        }
    }

    suspend fun getFileContent(branch: String, path: String): String {
        val body = client.get("$API/repos/$owner/$repo/contents/$path?ref=$branch") { githubAuth() }.body<String>()
        val res = jsonParser.decodeFromString<ContentsResponse>(body)
        return String(Base64.getDecoder().decode(res.content.replace("\n", "")), Charsets.UTF_8)
    }

    // Create-or-update (a 404 looking up existingSha just means the file is
    // genuinely new) - returns the resulting blob sha so a sync caller can
    // record the new "last known" fingerprint without a second round trip.
    suspend fun putFile(branch: String, path: String, content: String, message: String): String {
        val existingSha = runCatching {
            val body = client.get("$API/repos/$owner/$repo/contents/$path?ref=$branch") { githubAuth() }.body<String>()
            jsonParser.decodeFromString<ContentsResponse>(body).sha
        }.getOrNull()

        val encoded = Base64.getEncoder().encodeToString(content.toByteArray(Charsets.UTF_8))
        val body = client.put("$API/repos/$owner/$repo/contents/$path") {
            githubAuth()
            contentType(ContentType.Application.Json)
            setBody(jsonParser.encodeToString(PutFileRequest.serializer(), PutFileRequest(message, encoded, branch, existingSha)))
        }.body<String>()
        return jsonParser.decodeFromString<PutFileResponse>(body).content.sha
    }

    // Only ever called by the user's OWN code-sync flow against a repo THEY
    // chose - a different trust boundary than GitHubService's AI-safety
    // "no delete method exists" invariant below, so this deliberately isn't
    // exposed on that facade.
    suspend fun deleteFile(branch: String, path: String, sha: String, message: String) {
        client.delete("$API/repos/$owner/$repo/contents/$path") {
            githubAuth()
            contentType(ContentType.Application.Json)
            setBody(jsonParser.encodeToString(DeleteFileRequest.serializer(), DeleteFileRequest(message, sha, branch)))
        }
    }

    // One call for the whole repo's file listing (path + blob sha) instead
    // of walking directory-by-directory through the Contents API - read-
    // only, so there's no atomicity concern using the Git Data API here
    // even though writes still go through the simpler Contents API above.
    suspend fun getRecursiveTree(branch: String): List<GitTreeEntry> {
        val body = client.get("$API/repos/$owner/$repo/git/trees/$branch?recursive=1") { githubAuth() }.body<String>()
        return jsonParser.decodeFromString<GitTreeResponse>(body).tree.filter { it.type == "blob" }
    }

    suspend fun getBlob(sha: String): String {
        val body = client.get("$API/repos/$owner/$repo/git/blobs/$sha") { githubAuth() }.body<String>()
        val res = jsonParser.decodeFromString<GitBlobResponse>(body)
        return String(Base64.getDecoder().decode(res.content.replace("\n", "")), Charsets.UTF_8)
    }

    suspend fun openPullRequest(branch: String, title: String, body: String): PullRequestInfo {
        val rawBody = client.post("$API/repos/$owner/$repo/pulls") {
            githubAuth()
            contentType(ContentType.Application.Json)
            setBody(jsonParser.encodeToString(CreatePrRequest.serializer(), CreatePrRequest(title, branch, baseBranch, body)))
        }.body<String>()
        return jsonParser.decodeFromString<PullRequestInfo>(rawBody)
    }

    suspend fun mergePullRequest(prNumber: Int): Boolean {
        val response: HttpResponse = client.put("$API/repos/$owner/$repo/pulls/$prNumber/merge") {
            githubAuth()
            contentType(ContentType.Application.Json)
        }
        return response.status.isSuccess()
    }
}

// GitHub access for the "Rules" tab AI (see AiChangeRequestService) - and
// the real safety boundary for it. GITHUB_PAT is a fine-grained token
// scoped to ONLY this repo with ONLY contents:write + pull-requests:write
// (no administration, no delete, no workflow scopes - set up by hand in
// GitHub's UI, fine-grained PATs can't be minted via API). Deliberately no
// delete-file/delete-branch method exists anywhere in this object - "the AI
// can't delete anything" is true because the capability doesn't exist to
// invoke, not because a prompt says not to. A thin facade over one
// hardcoded GitHubRepoClient instance - see that class for the general
// per-(owner,repo,token) version CodeGithubSyncService uses instead.
object GitHubService {
    private const val OWNER = "xenos-hackcode"
    private const val REPO = "hacker-x-globe"
    private const val BASE_BRANCH = "main"
    private val TOKEN = System.getenv("GITHUB_PAT") ?: ""

    private val client = GitHubRepoClient(OWNER, REPO, TOKEN, BASE_BRANCH)

    suspend fun createBranch(newBranch: String) = client.createBranch(newBranch)

    // Read-only - lets AiChangeRequestService splice a small AI-generated
    // fragment into a known anchor point in the CURRENT file rather than
    // trusting the AI to regenerate (and risk subtly corrupting) the whole
    // file itself.
    suspend fun getFileContent(branch: String, path: String): String = client.getFileContent(branch, path)

    suspend fun putFile(branch: String, path: String, content: String, message: String): String =
        client.putFile(branch, path, content, message)

    suspend fun openPullRequest(branch: String, title: String, body: String): PullRequestInfo =
        client.openPullRequest(branch, title, body)

    suspend fun mergePullRequest(prNumber: Int): Boolean = client.mergePullRequest(prNumber)
}

@Serializable
private data class CreateRefRequest(val ref: String, val sha: String)

@Serializable
private data class RefObject(val sha: String)

@Serializable
private data class RefResponse(val `object`: RefObject)

@Serializable
private data class ContentsResponse(val sha: String, val content: String = "")

@Serializable
private data class PutFileRequest(val message: String, val content: String, val branch: String, val sha: String? = null)

@Serializable
private data class PutFileContent(val sha: String)

@Serializable
private data class PutFileResponse(val content: PutFileContent)

@Serializable
private data class DeleteFileRequest(val message: String, val sha: String, val branch: String)

@Serializable
data class GitTreeEntry(val path: String, val sha: String, val type: String)

@Serializable
private data class GitTreeResponse(val tree: List<GitTreeEntry>)

@Serializable
private data class GitBlobResponse(val content: String)

@Serializable
private data class CreatePrRequest(val title: String, val head: String, val base: String, val body: String)

@Serializable
data class PullRequestInfo(
    val number: Int,
    @SerialName("html_url") val htmlUrl: String,
)
