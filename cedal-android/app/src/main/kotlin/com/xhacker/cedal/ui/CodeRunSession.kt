package com.xhacker.cedal.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.xhacker.cedal.data.AndroidBuildJob
import com.xhacker.cedal.data.CodeRunResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Survives navigating away from and back to Code (Home -> Code -> Home ->
// Code) within the same app process - unlike plain `remember` state in
// MemberCodeBody, which resets whenever that composable leaves composition
// (e.g. tapping Back pops the "member_work_code" nav entry). Mirrors
// AppLockState's pattern (a simple in-memory singleton). This is NOT
// SavedStateHandle/process-death survival - the file and its actual code
// content are separately safe regardless, via real disk storage
// (CodeStorage/SAF with auto-save); this singleton only covers "what you
// last ran and its result," which would otherwise vanish for no good reason
// just from switching tabs and coming back.
object CodeRunSession {
    var result by mutableStateOf<CodeRunResult?>(null)
    var runError by mutableStateOf<String?>(null)
    var viewMessage by mutableStateOf<String?>(null)
    var running by mutableStateOf(false)

    var androidBuildJob by mutableStateOf<AndroidBuildJob?>(null)
    var androidBuildError by mutableStateOf<String?>(null)
    var installing by mutableStateOf(false)
    var installError by mutableStateOf<String?>(null)

    // When the current build actually started watching - lets the UI show an
    // estimated percentage (elapsed / a typical build's duration) instead of
    // just a plain spinner, since a build with no feedback at all for
    // minutes reads as "stuck" even when it's working fine. It's an
    // estimate, not a real measurement of Gradle's actual progress - see
    // ESTIMATED_BUILD_DURATION_MS in AndroidBuildStatus.
    var buildStartedAtMillis by mutableStateOf<Long?>(null)

    // A red-dot badge on the Code card back in Work Mode Selector, set the
    // moment a build finishes and cleared once View is actually opened. Backs
    // up the real system notification for cases that don't fire one - the
    // permission was denied, or the app process died while backgrounded (see
    // startWatchingBuild's comment) - since this is plain in-app UI state,
    // not a real Android notification.
    var hasUnseenBuildResult by mutableStateOf(false)

    fun markBuildResultSeen() {
        hasUnseenBuildResult = false
    }

    // "Backer" - an automatic pre-flight AI check that runs before every
    // Run, across every language (see CodeBackerService server-side).
    // backerReason/backerFixedCode being non-null means a likely issue was
    // found and is waiting on the user to either apply the fix or run
    // anyway - never applied without that explicit tap.
    var backerChecking by mutableStateOf(false)
    var backerReason by mutableStateOf<String?>(null)
    var backerFixedCode by mutableStateOf<String?>(null)

    fun clearBackerIssue() {
        backerReason = null
        backerFixedCode = null
    }

    private var backerCheckJob: Job? = null

    // The manual "Check Code" button - unlike Run's own automatic Backer
    // check (a plain scope.launch in MemberCodeBody, cancelled the instant
    // you navigate away), this runs on watchScope below so it survives
    // leaving Code entirely, and onFinished fires a real notification if you
    // do - matching the same pattern already proven for Kotlin builds.
    fun startManualBackerCheck(review: suspend () -> BackerCheckOutcome, onFinished: (hasIssue: Boolean) -> Unit) {
        backerCheckJob?.cancel()
        backerChecking = true
        backerReason = null
        backerFixedCode = null
        backerCheckJob = watchScope.launch {
            val outcome = review()
            backerChecking = false
            if (outcome.hasIssue) {
                backerReason = outcome.reason
                backerFixedCode = outcome.fixedCode.takeIf { it.isNotBlank() }
            }
            onFinished(outcome.hasIssue)
        }
    }

    data class BackerCheckOutcome(val hasIssue: Boolean, val reason: String, val fixedCode: String)

    // Outlives MemberCodeBody's own rememberCoroutineScope (which gets
    // cancelled the moment you navigate away from Code) so a build's
    // progress/notification keeps working even after leaving the screen -
    // as long as the app process itself is still alive, not a true
    // background service surviving a full app kill.
    private val watchScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var watchJob: Job? = null

    fun clearRunOutputs() {
        result = null
        runError = null
        viewMessage = null
        androidBuildJob = null
        androidBuildError = null
        installError = null
        hasUnseenBuildResult = false
        buildStartedAtMillis = null
        backerCheckJob?.cancel()
        backerCheckJob = null
        backerChecking = false
        backerReason = null
        backerFixedCode = null
    }

    fun startWatchingBuild(jobId: String, poll: suspend (String) -> Result<AndroidBuildJob>, onFinished: (AndroidBuildJob) -> Unit) {
        watchJob?.cancel()
        running = true
        buildStartedAtMillis = System.currentTimeMillis()
        watchJob = watchScope.launch {
            while (true) {
                delay(4000)
                val res = poll(jobId)
                val job = res.getOrNull()
                if (job == null) {
                    androidBuildError = res.exceptionOrNull()?.message ?: "Lost track of the build"
                    running = false
                    return@launch
                }
                androidBuildJob = job
                if (job.status == "done" || job.status == "error") {
                    running = false
                    hasUnseenBuildResult = true
                    onFinished(job)
                    return@launch
                }
            }
        }
    }

    // Client-side only: stops polling/watching. The build itself, if still
    // running server-side, isn't killed - there's no cancel endpoint on
    // android-builder yet to actually stop an in-flight Gradle build.
    fun cancelWatching() {
        watchJob?.cancel()
        watchJob = null
        running = false
        buildStartedAtMillis = null
        androidBuildJob = androidBuildJob?.copy(status = "error", errorMessage = "Cancelled")
    }
}
