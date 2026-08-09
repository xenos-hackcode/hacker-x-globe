package com.xhacker.cedal.services

import com.xhacker.cedal.models.CodeFile
import com.xhacker.cedal.models.CodeLanguageItem
import com.xhacker.cedal.models.CodeRunResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// The "Code" work mode's run button — real, sandboxed execution via the
// cedal-mobile project's Cloud Run runner services (Docker containers, hard
// timeouts, no shared filesystem between runs) instead of Wandbox/Kotlin
// Playground's free public APIs (rate limits/uptime we don't control) or a
// local unsandboxed subprocess (a real RCE risk the moment this server is
// reachable from anywhere but one dev machine - which it now is, on Cloud
// Run). Same interpreted/compiled split as that project's EditorPanel.tsx.
object CodeExecutionService {
    private val jsonParser = Json { ignoreUnknownKeys = true }
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) { json(jsonParser) }
        install(HttpTimeout) { requestTimeoutMillis = 45_000 }
    }

    private val RUNNER_SECRET = System.getenv("RUNNER_SERVICE_SECRET") ?: ""

    // us-east1 - closest of the runner services' 3 regions to cedal-server's
    // own us-central1 deployment. Region-picking only matters for a
    // latency-sensitive end user client; server-to-server this is just one
    // fixed, nearby call.
    private const val INTERPRETED_URL = "https://code-runner-717899371194.us-east1.run.app"
    private const val COMPILED_URL = "https://code-runner-compiled-717899371194.us-east1.run.app"

    private val COMPILED_LANGUAGES = setOf("C", "C++", "Go", "Rust", "Java", "C#")

    // Not private - AiChangeRequestService reads this for its judge prompt's
    // "here's what's already supported" context. A newly-approved language
    // only actually shows up here once its PR is merged and this service
    // itself redeploys with the literal new line added - no runtime
    // mutation, which would just be inconsistent across instances until
    // that real redeploy finishes anyway.
    val LANGUAGES = listOf(
        "JavaScript", "TypeScript", "Python", "PHP", "Ruby", "Lua", "Bash",
        "C", "C++", "Go", "Rust", "Java", "C#", "Kotlin",
    )

    // language IS the version here - the runner services dispatch on the
    // exact language name, there's no separate compiler-slug concept like
    // Wandbox had.
    suspend fun listLanguages(): List<CodeLanguageItem> =
        LANGUAGES.map { CodeLanguageItem(language = it, version = it) }

    suspend fun run(language: String, code: String, stdin: String, extraFiles: List<CodeFile> = emptyList(), packages: List<String> = emptyList()): CodeRunResult {
        val baseUrl = if (language in COMPILED_LANGUAGES) COMPILED_URL else INTERPRETED_URL

        val text: String = client.post("$baseUrl/run") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $RUNNER_SECRET")
            setBody(
                RunnerRequest(
                    language = language,
                    code = code,
                    stdin = stdin,
                    extraFiles = extraFiles.map { RunnerFile(it.name, it.content) },
                    packages = packages,
                ),
            )
        }.body()
        val response: RunnerResponse = jsonParser.decodeFromString(text)

        return CodeRunResult(
            stdout = response.stdout,
            stderr = if (response.timedOut) response.stderr + "\n[timed out]" else response.stderr,
            exitCode = response.exitCode,
            compileOutput = null,
            language = language,
            version = language,
        )
    }
}

@Serializable
private data class RunnerFile(val name: String, val content: String)

@Serializable
private data class RunnerRequest(
    val language: String,
    val code: String,
    val stdin: String = "",
    val extraFiles: List<RunnerFile> = emptyList(),
    val packages: List<String> = emptyList(),
)

@Serializable
private data class RunnerResponse(
    val stdout: String = "",
    val stderr: String = "",
    val exitCode: Int = 0,
    val timedOut: Boolean = false,
    val durationMs: Long = 0,
)
