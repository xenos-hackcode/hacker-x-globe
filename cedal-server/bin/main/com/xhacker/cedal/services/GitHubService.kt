package com.xhacker.cedal.services

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.util.Base64

// GitHub access for the "Rules" tab AI (see AiChangeRequestService) - and
// the real safety boundary for it. GITHUB_PAT is a fine-grained token
// scoped to ONLY this repo with ONLY contents:write + pull-requests:write
// (no administration, no delete, no workflow scopes - set up by hand in
// GitHub's UI, fine-grained PATs can't be minted via API). Deliberately no
// delete-file/delete-branch method exists anywhere in this object - "the AI
// can't delete anything" is true because the capability doesn't exist to
// invoke, not because a prompt says not to.
object GitHubService {
    private const val OWNER = "Xenos-deathcode"
    private const val REPO = "hacker-x-globe"
    private const val BASE_BRANCH = "main"
    private const val API = "https://api.github.com"

    private val TOKEN = System.getenv("GITHUB_PAT") ?: ""
    private val jsonParser = Json { ignoreUnknownKeys = true }
    private val client = HttpClient(CIO) {
        install(HttpTimeout) { requestTimeoutMillis = 20_000 }
    }

    private fun io.ktor.client.request.HttpRequestBuilder.githubAuth() {
        header("Authorization", "Bearer $TOKEN")
        header("Accept", "application/vnd.github+json")
        header("X-GitHub-Api-Version", "2022-11-28")
    }

    private suspend fun latestBaseSha(): String {
        val body = client.get("$API/repos/$OWNER/$REPO/git/ref/heads/$BASE_BRANCH") { githubAuth() }.body<String>()
        val res = jsonParser.decodeFromString<RefResponse>(body)
        return res.`object`.sha
    }

    suspend fun createBranch(newBranch: String) {
        val baseSha = latestBaseSha()
        client.post("$API/repos/$OWNER/$REPO/git/refs") {
            githubAuth()
            contentType(ContentType.Application.Json)
            setBody(jsonParser.encodeToString(CreateRefRequest.serializer(), CreateRefRequest("refs/heads/$newBranch", baseSha)))
        }
    }

    // Read-only - lets AiChangeRequestService splice a small AI-generated
    // fragment into a known anchor point in the CURRENT file rather than
    // trusting the AI to regenerate (and risk subtly corrupting) the whole
    // file itself.
    suspend fun getFileContent(branch: String, path: String): String {
        val body = client.get("$API/repos/$OWNER/$REPO/contents/$path?ref=$branch") { githubAuth() }.body<String>()
        val res = jsonParser.decodeFromString<ContentsResponse>(body)
        return String(Base64.getDecoder().decode(res.content.replace("\n", "")), Charsets.UTF_8)
    }

    // Create-or-update only - GitHub's Contents API needs the current
    // blob's sha to update an existing file (a 422 otherwise), so this
    // looks that up first; a 404 just means the file is genuinely new.
    suspend fun putFile(branch: String, path: String, content: String, message: String) {
        val existingSha = runCatching {
            val body = client.get("$API/repos/$OWNER/$REPO/contents/$path?ref=$branch") { githubAuth() }.body<String>()
            jsonParser.decodeFromString<ContentsResponse>(body).sha
        }.getOrNull()

        val encoded = Base64.getEncoder().encodeToString(content.toByteArray(Charsets.UTF_8))
        client.put("$API/repos/$OWNER/$REPO/contents/$path") {
            githubAuth()
            contentType(ContentType.Application.Json)
            setBody(jsonParser.encodeToString(PutFileRequest.serializer(), PutFileRequest(message, encoded, branch, existingSha)))
        }
    }

    suspend fun openPullRequest(branch: String, title: String, body: String): PullRequestInfo {
        val rawBody = client.post("$API/repos/$OWNER/$REPO/pulls") {
            githubAuth()
            contentType(ContentType.Application.Json)
            setBody(jsonParser.encodeToString(CreatePrRequest.serializer(), CreatePrRequest(title, branch, BASE_BRANCH, body)))
        }.body<String>()
        return jsonParser.decodeFromString<PullRequestInfo>(rawBody)
    }

    suspend fun mergePullRequest(prNumber: Int): Boolean {
        val response: HttpResponse = client.put("$API/repos/$OWNER/$REPO/pulls/$prNumber/merge") {
            githubAuth()
            contentType(ContentType.Application.Json)
        }
        return response.status.isSuccess()
    }
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
private data class CreatePrRequest(val title: String, val head: String, val base: String, val body: String)

@Serializable
data class PullRequestInfo(
    val number: Int,
    @SerialName("html_url") val htmlUrl: String,
)
