package com.xhacker.cedal.services

import com.xhacker.cedal.db.GuiSessions
import com.xhacker.cedal.models.GuiSessionJob
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.util.UUID

// Live interactive GUI (Xvfb+VNC-over-websocket) sessions for the Code
// screen's Python runner - see gui-runner/server.js. Unlike Android builds,
// gui-runner's /session/start is synchronous (it waits for the display/VNC
// chain to actually come up, a few seconds at most) so there's no
// queued->building->done poll loop here - the caller gets viewUrl back
// directly from startSession. Each gui-runner instance only ever runs one
// session (containerConcurrency=1 - a session needs a whole instance to
// itself), but up to 10 instances can now exist concurrently (see its
// deploy.sh) with --session-affinity pinning a client's later requests back
// to the instance that's actually running its session. That affinity
// cookie is set on THIS service's response to /session/start, not on
// anything the eventual Android client ever talks to directly - so it has
// to be captured here and relayed one hop further (see affinityCookies on
// GuiSessionJob) for the client's own page load + websocket upgrade to
// land on the right instance.
object GuiSessionService {
    private const val GUI_RUNNER_URL = "https://gui-runner-717899371194.us-central1.run.app"
    private val GUI_RUNNER_SECRET = System.getenv("GUI_RUNNER_SECRET") ?: ""

    // Mirrors gui-runner's own SESSION_TIMEOUT_MS - used here only to decide
    // whether a "starting"/"active" row is stale (e.g. the container died
    // without us hearing about it) rather than a real guarantee of when
    // gui-runner itself tears a session down.
    private const val SESSION_TIMEOUT_MS = 10 * 60 * 1000L

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        install(HttpTimeout) { requestTimeoutMillis = 15_000 }
    }

    suspend fun startSession(userId: String, code: String): GuiSessionJob {
        val uid = UUID.fromString(userId)
        val cutoff = System.currentTimeMillis() - SESSION_TIMEOUT_MS
        // Scoped to this user only now that gui-runner can host several
        // sessions at once (different instances) - one person still
        // shouldn't stack two of their own, but it no longer blocks anyone
        // else's.
        val alreadyOpen = transaction {
            GuiSessions.selectAll()
                .where {
                    (GuiSessions.userId eq uid) and
                        (GuiSessions.status inList listOf("starting", "active")) and
                        (GuiSessions.createdAt greater cutoff)
                }
                .any()
        }
        if (alreadyOpen) {
            throw AuthException("You already have a live session open - close it before starting another.")
        }

        val jobId = transaction {
            GuiSessions.insertAndGetId {
                it[GuiSessions.userId] = uid
                it[status] = "starting"
                it[createdAt] = System.currentTimeMillis()
            }
        }.value.toString()

        try {
            val httpResponse: HttpResponse = client.post("$GUI_RUNNER_URL/session/start") {
                contentType(ContentType.Application.Json)
                header("Authorization", "Bearer $GUI_RUNNER_SECRET")
                setBody(GuiRunnerStartRequest(sessionId = jobId, code = code))
            }
            val response: GuiRunnerStartResponse = httpResponse.body()
            val affinityCookies = httpResponse.headers.getAll(HttpHeaders.SetCookie) ?: emptyList()

            val viewUrl = "$GUI_RUNNER_URL${response.path}"
            transaction {
                GuiSessions.update({ GuiSessions.id eq UUID.fromString(jobId) }) {
                    it[status] = "active"
                    it[GuiSessions.viewUrl] = viewUrl
                }
            }
            return GuiSessionJob(jobId, "active", viewUrl, affinityCookies = affinityCookies)
        } catch (e: Exception) {
            transaction {
                GuiSessions.update({ GuiSessions.id eq UUID.fromString(jobId) }) {
                    it[status] = "error"
                    it[errorMessage] = "Failed to start session: ${e.message}"
                }
            }
            throw AuthException("Couldn't start the live session - try again in a moment.")
        }
    }

    suspend fun stopSession(userId: String, jobId: String) {
        val uid = UUID.fromString(userId)
        val id = UUID.fromString(jobId)
        val owns = transaction {
            GuiSessions.selectAll().where { (GuiSessions.id eq id) and (GuiSessions.userId eq uid) }.any()
        }
        if (!owns) throw AuthException("Session not found")

        transaction {
            GuiSessions.update({ GuiSessions.id eq id }) { it[status] = "ended" }
        }
        try {
            client.post("$GUI_RUNNER_URL/session/$jobId/stop") {
                header("Authorization", "Bearer $GUI_RUNNER_SECRET")
            }
        } catch (_: Exception) {
            // Best-effort - gui-runner's own hard timeout is the real
            // backstop if this call fails for any reason.
        }
    }

    fun getJob(jobId: String): GuiSessionJob = transaction {
        val id = UUID.fromString(jobId)
        val row = GuiSessions.selectAll().where { GuiSessions.id eq id }.firstOrNull()
            ?: throw AuthException("Session not found")
        GuiSessionJob(jobId, row[GuiSessions.status], row[GuiSessions.viewUrl], row[GuiSessions.errorMessage])
    }
}

@Serializable
private data class GuiRunnerStartRequest(val sessionId: String, val code: String)

@Serializable
private data class GuiRunnerStartResponse(val sessionId: String, val path: String, val expiresInMs: Long = 0)
