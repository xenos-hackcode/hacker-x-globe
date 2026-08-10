package com.xhacker.cedal.services

import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.storage.BlobId
import com.google.cloud.storage.BlobInfo
import com.google.cloud.storage.StorageOptions
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
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

// Triggers a real redeploy once an AI change request is approved (see
// AiChangeRequestRoutes) - a merged PR alone doesn't ship anything until
// this actually runs. No pre-wired Cloud Build <-> GitHub connection is
// assumed (that needs a one-time manual "Connect Repository" click-through
// in Cloud Console) - instead this relays the merged repo's tarball
// straight from GitHub into the same Cloud Storage bucket already used for
// avatars/stickers (see ImageUploadService), then calls Cloud Build and
// Cloud Run's REST APIs directly using this container's own Application
// Default Credentials (the identity Cloud Run already runs it as).
//
// LEAST proven part of this whole feature - the exact Cloud Build/Cloud Run
// Admin request shapes below are written from documentation, not verified
// against a real approved request yet. Treat the first real approval as
// the actual test, not this compiling cleanly.
object DeployService {
    private const val PROJECT_ID = "cedal-fd4a2"
    private const val REGION = "us-central1"
    private const val BUCKET = "cedal-fd4a2.firebasestorage.app"
    // Matches GitHubService's OWNER - was previously "Xenos-deathcode" here
    // (a stale/wrong value that would 404 on every downloadMergedTarball()
    // call, silently breaking every approved deploy) vs. the real
    // "xenos-hackcode" GitHubService already used correctly.
    private const val OWNER = "xenos-hackcode"
    private const val REPO = "hacker-x-globe"
    private val GITHUB_TOKEN = System.getenv("GITHUB_PAT") ?: ""

    private val storage by lazy { StorageOptions.getDefaultInstance().service }
    private val jsonParser = Json { ignoreUnknownKeys = true }
    private val client = HttpClient(CIO) {
        install(HttpTimeout) { requestTimeoutMillis = 15 * 60 * 1000 } // real builds take minutes
    }

    private fun accessToken(): String {
        val creds = GoogleCredentials.getApplicationDefault()
            .createScoped(listOf("https://www.googleapis.com/auth/cloud-platform"))
        creds.refresh()
        return creds.accessToken.tokenValue
    }

    // Every AI-generated new-language request always touches all three
    // known files (Dockerfile, server.js, LANGUAGES list - see
    // AiChangeRequestService's fixed anchors), so approval always redeploys
    // both services - no per-request "which files changed" branching
    // needed. code-runner is a real 3-region deployment (see
    // cedal-mobile/runner/deploy.sh) - build once, deploy that same image
    // to every region it actually runs in.
    // "cedal-mobile/runner" was wrong - same stale path as
    // AiChangeRequestService's DOCKERFILE_PATH/SERVER_JS_PATH (fixed
    // 2026-08-09), just a second, separate hardcoded occurrence this
    // service never got the same fix. Confirmed via a real failed build
    // (`cd */cedal-mobile/runner: No such file or directory`) after the
    // Kotlin-support PR merged successfully but this redeploy step choked
    // on it - the PR was real and correct, this path was the only thing
    // broken. Real path is cedal-services/runner (confirmed via the GitHub
    // API), same as the other file.
    suspend fun redeployApprovedLanguageChange() {
        redeployFromMain("code-runner", "cedal-services/runner", listOf("europe-west2", "europe-west3", "us-east1"))
        redeployFromMain("cedal-server", "cedal-server", listOf(REGION))
    }

    // DeveloperSubmissionService's approve() - a developer's merged patch,
    // scoped to server-side files only (see that service's own doc comment
    // for why an Android-side submission can't auto-deploy the same way).
    // redeployFromMain is already fully generic (target/subdirectory/
    // regions), so this is just the one relevant call for this use case.
    suspend fun redeployCedalServer() {
        redeployFromMain("cedal-server", "cedal-server", listOf(REGION))
    }

    private suspend fun redeployFromMain(target: String, subdirectory: String, regions: List<String>) {
        val token = accessToken()
        val tarBytes = downloadMergedTarball()
        val objectPath = "deploy-source/${target}-${System.currentTimeMillis()}.tar.gz"
        storage.create(BlobInfo.newBuilder(BlobId.of(BUCKET, objectPath)).setContentType("application/gzip").build(), tarBytes)

        val buildId = triggerBuild(token, target, subdirectory, objectPath)
        val imageUrl = pollBuildForImage(token, buildId)
        regions.forEach { region -> deployToCloudRun(token, target, imageUrl, region) }
    }

    private suspend fun downloadMergedTarball(): ByteArray {
        val response: HttpResponse = client.get("https://api.github.com/repos/$OWNER/$REPO/tarball/main") {
            header("Authorization", "Bearer $GITHUB_TOKEN")
            header("Accept", "application/vnd.github+json")
        }
        return response.body()
    }

    // GitHub's tarball always wraps everything in one unpredictable
    // "{owner}-{repo}-{shortsha}/" directory - rather than trying to parse
    // and strip that in Kotlin, the build step below just globs past it
    // with a shell wildcard, since Cloud Build always extracts a
    // storageSource to exactly one directory in /workspace.
    private suspend fun triggerBuild(token: String, target: String, subdirectory: String, objectPath: String): String {
        val image = "gcr.io/$PROJECT_ID/$target:ai-${System.currentTimeMillis()}"
        val buildConfig = buildJsonObject {
            put("source", buildJsonObject {
                put("storageSource", buildJsonObject {
                    put("bucket", BUCKET)
                    put("object", objectPath)
                })
            })
            putJsonArray("steps") {
                add(buildJsonObject {
                    put("name", "gcr.io/cloud-builders/docker")
                    put("entrypoint", "bash")
                    putJsonArray("args") {
                        add(JsonPrimitive("-c"))
                        add(JsonPrimitive("cd */$subdirectory && docker build -t $image ."))
                    }
                })
            }
            putJsonArray("images") { add(JsonPrimitive(image)) }
        }

        val response: HttpResponse = client.post("https://cloudbuild.googleapis.com/v1/projects/$PROJECT_ID/builds") {
            header("Authorization", "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(buildConfig.toString())
        }
        val body: String = response.body()
        val metadata = jsonParser.parseToJsonElement(body).jsonObject
        return metadata["metadata"]?.jsonObject?.get("build")?.jsonObject?.get("id")?.jsonPrimitive?.content
            ?: error("Cloud Build didn't return a build id: $body")
    }

    private suspend fun pollBuildForImage(token: String, buildId: String): String {
        repeat(90) { // up to ~15 min at 10s intervals
            delay(10.seconds)
            val response: HttpResponse = client.get("https://cloudbuild.googleapis.com/v1/projects/$PROJECT_ID/builds/$buildId") {
                header("Authorization", "Bearer $token")
            }
            val body: String = response.body()
            val build = jsonParser.parseToJsonElement(body).jsonObject
            when (build["status"]?.jsonPrimitive?.content) {
                "SUCCESS" -> return build["images"]?.let { it as? JsonArray }?.firstOrNull()?.jsonPrimitive?.content
                    ?: error("Build succeeded but returned no image")
                "FAILURE", "TIMEOUT", "CANCELLED", "EXPIRED", "INTERNAL_ERROR" -> error("Build $buildId ended with status ${build["status"]}")
                else -> {} // QUEUED / WORKING - keep polling
            }
        }
        error("Build $buildId didn't finish within the poll budget")
    }

    // Knative-style services need the FULL existing object round-tripped
    // back on update (resourceVersion included) - a from-scratch partial
    // body gets rejected. Fetches the live service, rewrites only the
    // image inside its existing container spec (leaving env vars, secrets,
    // concurrency, everything else exactly as already configured), and
    // PUTs that back.
    private suspend fun deployToCloudRun(token: String, service: String, image: String, region: String) {
        val url = "https://$region-run.googleapis.com/apis/serving.knative.dev/v1/namespaces/$PROJECT_ID/services/$service"
        val current: String = client.get(url) { header("Authorization", "Bearer $token") }.body()
        val currentJson = jsonParser.parseToJsonElement(current).jsonObject

        val spec = currentJson["spec"]!!.jsonObject
        val template = spec["template"]!!.jsonObject
        val templateSpec = template["spec"]!!.jsonObject
        val containers = templateSpec["containers"]!!.jsonArray
        val updatedContainers = buildJsonArray {
            containers.forEach { c ->
                add(JsonObject(c.jsonObject.toMutableMap().apply { put("image", JsonPrimitive(image)) }))
            }
        }
        val updatedTemplateSpec = JsonObject(templateSpec.toMutableMap().apply { put("containers", updatedContainers) })
        val updatedTemplate = JsonObject(template.toMutableMap().apply { put("spec", updatedTemplateSpec) })
        val updatedSpec = JsonObject(spec.toMutableMap().apply { put("template", updatedTemplate) })
        val updatedBody = JsonObject(currentJson.toMutableMap().apply { put("spec", updatedSpec) })

        client.put(url) {
            header("Authorization", "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(updatedBody.toString())
        }
    }
}
