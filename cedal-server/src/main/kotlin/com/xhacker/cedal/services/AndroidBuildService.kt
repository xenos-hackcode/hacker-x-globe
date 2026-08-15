package com.xhacker.cedal.services

import com.xhacker.cedal.db.AndroidBuilds
import com.xhacker.cedal.db.Users
import com.xhacker.cedal.models.AndroidBuildJob
import io.ktor.client.HttpClient
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
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.util.UUID

// Kotlin -> real installable APK, via cedal-mobile's already-deployed
// android-builder Cloud Run service (a Gradle/Android SDK build server -
// far too heavy to run inline in a request/response cycle; builds take
// 1-5+ minutes). Not gated behind Star Coins - a build shouldn't touch the
// wallet at all; access is controlled elsewhere (rank-based gating).
object AndroidBuildService {
    private const val ANDROID_BUILDER_URL = "https://android-builder-717899371194.us-central1.run.app"
    private val ANDROID_BUILDER_SECRET = System.getenv("ANDROID_BUILDER_SECRET") ?: ""
    // Where android-builder calls back on completion. Override via env if
    // cedal-server is ever deployed somewhere other than this fixed Cloud Run URL.
    private val CALLBACK_BASE_URL = System.getenv("PUBLIC_BASE_URL")
        ?: "https://cedal-server-717899371194.us-central1.run.app"

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        install(HttpTimeout) { requestTimeoutMillis = 10_000 }
    }

    // Was gated behind Shop rank D1 (real-money xp) - removed so anyone can
    // build/run their own Kotlin code without paying first; a real build
    // still costs real Cloud Run compute time, just not a per-user paywall.

    // Creates the job row - separate from triggerBuild so the route can
    // respond to the client immediately after this, without waiting on the
    // outbound call to android-builder.
    fun requestBuild(userId: String): String = transaction {
        val uid = UUID.fromString(userId)
        Users.selectAll().where { Users.id eq uid }.firstOrNull() ?: throw AuthException("User not found")

        val jobId = AndroidBuilds.insertAndGetId {
            it[AndroidBuilds.userId] = uid
            it[status] = "queued"
            it[createdAt] = System.currentTimeMillis()
        }
        jobId.value.toString()
    }

    suspend fun triggerBuild(jobId: String, userId: String, code: String) {
        try {
            client.post("$ANDROID_BUILDER_URL/build") {
                contentType(ContentType.Application.Json)
                header("Authorization", "Bearer $ANDROID_BUILDER_SECRET")
                setBody(
                    BuildTriggerRequest(
                        jobId = jobId,
                        uid = userId,
                        code = code,
                        callbackUrl = "$CALLBACK_BASE_URL/code/android-build/$jobId/callback",
                    ),
                )
            }
        } catch (e: Exception) {
            updateStatus(jobId, "error", errorMessage = "Failed to start build: ${e.message}")
        }
    }

    fun getJob(jobId: String): AndroidBuildJob = transaction {
        val id = UUID.fromString(jobId)
        val row = AndroidBuilds.selectAll().where { AndroidBuilds.id eq id }.firstOrNull()
            ?: throw AuthException("Build not found")
        AndroidBuildJob(
            jobId = jobId,
            status = row[AndroidBuilds.status],
            downloadUrl = row[AndroidBuilds.downloadUrl],
            errorMessage = row[AndroidBuilds.errorMessage],
        )
    }

    fun updateStatus(jobId: String, status: String, downloadUrl: String? = null, errorMessage: String? = null) {
        transaction {
            val id = UUID.fromString(jobId)
            AndroidBuilds.update({ AndroidBuilds.id eq id }) {
                it[AndroidBuilds.status] = status
                if (downloadUrl != null) it[AndroidBuilds.downloadUrl] = downloadUrl
                if (errorMessage != null) it[AndroidBuilds.errorMessage] = errorMessage
            }
        }
    }
}

@Serializable
private data class BuildTriggerRequest(val jobId: String, val uid: String, val code: String, val callbackUrl: String)
