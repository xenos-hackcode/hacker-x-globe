package com.xhacker.cedal.services

import com.xhacker.cedal.db.ArcPracticeApps
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

// ARC Ops "practice targets" - real, installable APKs, but built from FIXED
// source we write ourselves (never user-submitted, never real malware) via
// the same android-builder Cloud Run service Code > Kotlin already uses.
// Every practice target's source contains one deliberately simple,
// documented flaw (e.g. a hardcoded weak PIN) - the same teaching pattern as
// well-known open-source vulnerable practice apps (DVWA, InsecureBankv2,
// OWASP's DVIA), just tiny and self-contained. Built ONCE per targetId and
// cached (see ArcPracticeApps table) - every player downloads the same
// APK, no per-attempt Gradle build cost, and no Mythic-10 gate like
// arbitrary user Kotlin builds have (this isn't user-submitted, arbitrary
// compute - it's one fixed app, built a handful of times total).
object ArcPracticeAppService {
    private const val ANDROID_BUILDER_URL = "https://android-builder-717899371194.us-central1.run.app"
    private val ANDROID_BUILDER_SECRET = System.getenv("ANDROID_BUILDER_SECRET") ?: ""
    private val CALLBACK_BASE_URL = System.getenv("PUBLIC_BASE_URL")
        ?: "https://cedal-server-717899371194.us-central1.run.app"

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        install(HttpTimeout) { requestTimeoutMillis = 10_000 }
    }

    data class Status(val status: String, val downloadUrl: String?, val errorMessage: String?)

    val TARGETS: Map<String, String> = mapOf(
        "quickpay-wallet" to QUICKPAY_WALLET_SOURCE,
    )

    // Returns the current status, kicking off a build the first time this
    // targetId is ever requested (queued -> building -> done/error, same
    // states Code > Kotlin's own build job uses).
    suspend fun statusOrStartBuild(targetId: String): Status {
        val source = TARGETS[targetId] ?: throw AuthException("Unknown practice target")
        val existing = transaction {
            ArcPracticeApps.selectAll().where { ArcPracticeApps.targetId eq targetId }.firstOrNull()
        }
        if (existing != null) {
            return Status(existing[ArcPracticeApps.status], existing[ArcPracticeApps.downloadUrl], existing[ArcPracticeApps.errorMessage])
        }
        transaction {
            ArcPracticeApps.insertIgnore {
                it[ArcPracticeApps.targetId] = targetId
                it[status] = "queued"
                it[updatedAt] = System.currentTimeMillis()
            }
        }
        CoroutineScope(Dispatchers.IO).launch { triggerBuild(targetId, source) }
        return Status("queued", null, null)
    }

    private suspend fun triggerBuild(targetId: String, source: String) {
        try {
            client.post("$ANDROID_BUILDER_URL/build") {
                contentType(ContentType.Application.Json)
                header("Authorization", "Bearer $ANDROID_BUILDER_SECRET")
                setBody(
                    PracticeBuildTriggerRequest(
                        jobId = targetId,
                        uid = "arc-practice-app",
                        code = source,
                        callbackUrl = "$CALLBACK_BASE_URL/arc/ops/practice-app/$targetId/callback",
                    ),
                )
            }
        } catch (e: Exception) {
            updateStatus(targetId, "error", errorMessage = "Failed to start build: ${e.message}")
        }
    }

    fun updateStatus(targetId: String, status: String, downloadUrl: String? = null, errorMessage: String? = null) {
        transaction {
            ArcPracticeApps.update({ ArcPracticeApps.targetId eq targetId }) {
                it[ArcPracticeApps.status] = status
                if (downloadUrl != null) it[ArcPracticeApps.downloadUrl] = downloadUrl
                if (errorMessage != null) it[ArcPracticeApps.errorMessage] = errorMessage
                it[updatedAt] = System.currentTimeMillis()
            }
        }
    }
}

@Serializable
private data class PracticeBuildTriggerRequest(val jobId: String, val uid: String, val code: String, val callbackUrl: String)

// A deliberately, obviously weak PIN check ("0000") - the flaw a player is
// meant to find by exploring the installed app themselves (see Ops' hard
// mode). No network access, no data collection, nothing beyond a local
// Compose UI - this is a teaching prop, not a functioning wallet.
private val QUICKPAY_WALLET_SOURCE = """
package com.example.quickpaywallet

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { QuickPayWalletApp() }
    }
}

@Composable
fun QuickPayWalletApp() {
    var pin by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    // Intentionally weak, for ARC Ops practice only - not a real app.
    val correctPin = "0000"

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("QuickPay Wallet", style = MaterialTheme.typography.headlineSmall)
        Text("ARC practice target - not a real wallet", style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(value = pin, onValueChange = { pin = it }, label = { Text("Enter PIN") })
        Spacer(Modifier.height(12.dp))
        Button(onClick = { message = if (pin == correctPin) "Unlocked! Flag: ARC-FLAG-7734" else "Incorrect PIN - try again" }) {
            Text("Unlock")
        }
        Spacer(Modifier.height(16.dp))
        Text(message)
    }
}
""".trimIndent()
