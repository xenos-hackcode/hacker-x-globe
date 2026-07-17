package com.xhacker.cedal.ui.screens.member

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xhacker.cedal.data.ArcMission
import com.xhacker.cedal.data.ArcMissionPrompt
import com.xhacker.cedal.ui.theme.CedalColors
import com.xhacker.cedal.ui.theme.CedalPrimaryButton
import com.xhacker.cedal.viewmodel.AuthViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ARC Ops - the "attack simulation" missions. Most "targets" are fictional,
// AI-narrated scenarios only - never real software, nothing ever touches a
// real network or device. ONE target (QuickPay Wallet) is different: a real,
// installable practice APK, built from source WE wrote (see
// ArcPracticeAppService server-side) with one deliberately simple, obvious
// flaw - the same teaching pattern as well-known open-source vulnerable
// practice apps. It's the "hard" target: no prompt ever tells you the flaw
// outright, you have to actually explore the installed app and find it
// yourself, same as real reconnaissance work. Every mission's scenario/
// prompts are freshly AI-generated per attempt either way, so replaying
// never repeats the same sequence - memorizing a pattern doesn't help.

private data class SimTarget(
    val name: String,
    val flavor: String,
    val practiceTargetId: String? = null,
    val difficulty: String = "EASY",
)

private val SIM_TARGETS = listOf(
    SimTarget(
        "QuickPay Wallet (practice)",
        "A REAL, installable practice app with a hidden flaw baked in on purpose. Nobody tells you what it is - explore it and find out yourself.",
        practiceTargetId = "quickpay-wallet",
        difficulty = "HARD",
    ),
    SimTarget("PhotoVaultPro (simulated)", "A fictional photo-backup app under a simulated credential-stuffing attempt."),
    SimTarget("CloudNotes (simulated)", "A fictional notes app with a simulated exposed admin panel."),
    SimTarget("SmartHome Hub (simulated)", "A fictional IoT hub under simulated default-credential probing."),
    SimTarget("MailGuard (simulated)", "A fictional inbox defending against simulated phishing and spoofing."),
)

// Matches the flag text hardcoded into QUICKPAY_WALLET_SOURCE server-side -
// not a real secret (it's plainly visible once you unlock the practice
// app), just the answer key for the "type what you found" step.
private const val QUICKPAY_FLAG = "ARC-FLAG-7734"

private data class PromptResult(val prompt: ArcMissionPrompt, val selectedIndex: Int?, val reactionMs: Long)

private enum class OpsPhase { PICK_TARGET, DOWNLOAD, LOADING, BRIEFING, PROMPT, FIND_FLAG, RESULTS }

@Composable
fun ArcOpsBody(onBack: () -> Unit, viewModel: AuthViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var phase by remember { mutableStateOf(OpsPhase.PICK_TARGET) }
    var currentTarget by remember { mutableStateOf<SimTarget?>(null) }
    var mission by remember { mutableStateOf<ArcMission?>(null) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var promptIndex by remember { mutableStateOf(0) }
    val results = remember { mutableStateListOf<PromptResult>() }
    var scorePercent by remember { mutableStateOf(0) }
    var expAwarded by remember { mutableStateOf<Long?>(null) }
    var flagFound by remember { mutableStateOf<Boolean?>(null) }

    fun startMission(target: SimTarget) {
        mission = null
        loadError = null
        results.clear()
        promptIndex = 0
        expAwarded = null
        phase = OpsPhase.LOADING
        scope.launch {
            viewModel.arcGenerateMission(target.name)
                .onSuccess { mission = it; phase = OpsPhase.BRIEFING }
                .onFailure { loadError = it.message ?: "Couldn't generate a mission"; phase = OpsPhase.PICK_TARGET }
        }
    }

    fun deploy(target: SimTarget) {
        currentTarget = target
        flagFound = null
        if (target.practiceTargetId != null) {
            phase = OpsPhase.DOWNLOAD
        } else {
            startMission(target)
        }
    }

    fun finishMission() {
        val total = results.size + if (currentTarget?.difficulty == "HARD") 1 else 0
        val correct = results.count { it.selectedIndex != null && it.selectedIndex == it.prompt.correctIndex } +
            if (flagFound == true) 1 else 0
        scorePercent = if (total > 0) correct * 100 / total else 0
        phase = OpsPhase.RESULTS
        scope.launch {
            viewModel.arcCompleteMission(scorePercent).onSuccess { expAwarded = it.expAwarded }
        }
    }

    fun recordAnswer(prompt: ArcMissionPrompt, selectedIndex: Int?, reactionMs: Long) {
        results.add(PromptResult(prompt, selectedIndex, reactionMs))
        val m = mission ?: return
        if (promptIndex + 1 < m.prompts.size) {
            promptIndex += 1
        } else if (currentTarget?.difficulty == "HARD") {
            phase = OpsPhase.FIND_FLAG
        } else {
            finishMission()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        MemberBackBar(title = "ARC", onBack = onBack)
        Text("OPS", color = CedalColors.TextSecondary, fontSize = 11.sp, letterSpacing = 2.sp, modifier = Modifier.padding(bottom = 6.dp))

        when (phase) {
            OpsPhase.PICK_TARGET -> TargetPickerBody(loadError, onDeploy = ::deploy)
            OpsPhase.DOWNLOAD -> {
                val target = currentTarget
                if (target?.practiceTargetId != null) {
                    DownloadPracticeAppBody(
                        target = target,
                        viewModel = viewModel,
                        context = context,
                        onInstalled = { startMission(target) },
                        onSkip = { startMission(target) },
                    )
                }
            }
            OpsPhase.LOADING -> LoadingBody()
            OpsPhase.BRIEFING -> {
                val m = mission
                val target = currentTarget
                if (m != null && target != null) {
                    BriefingBody(target = target, mission = m, onBegin = { phase = OpsPhase.PROMPT })
                }
            }
            OpsPhase.PROMPT -> {
                val m = mission
                if (m != null && promptIndex < m.prompts.size) {
                    FlashPromptBody(
                        prompt = m.prompts[promptIndex],
                        promptNumber = promptIndex + 1,
                        totalPrompts = m.prompts.size,
                        onAnswer = { selected, reactionMs -> recordAnswer(m.prompts[promptIndex], selected, reactionMs) },
                    )
                }
            }
            OpsPhase.FIND_FLAG -> {
                FindFlagBody(
                    onSubmit = { entered ->
                        flagFound = entered.trim().equals(QUICKPAY_FLAG, ignoreCase = true)
                        finishMission()
                    },
                    onSkip = {
                        flagFound = false
                        finishMission()
                    },
                )
            }
            OpsPhase.RESULTS -> {
                ResultsBody(
                    results = results,
                    scorePercent = scorePercent,
                    expAwarded = expAwarded,
                    flagFound = flagFound,
                    onReplay = { currentTarget?.let { deploy(it) } },
                    onChooseAnother = { phase = OpsPhase.PICK_TARGET },
                )
            }
        }
    }
}

@Composable
private fun TargetPickerBody(loadError: String?, onDeploy: (SimTarget) -> Unit) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Text(
            "Pick a target below - each one generates a brand-new, AI-written incident every time, so no two runs (or replays) play out the same way. Most are AI-narrated simulations that never touch a real app or device - one (marked REAL APK) is an actual installable practice app instead.",
            color = CedalColors.TextMuted, fontSize = 12.sp, modifier = Modifier.padding(bottom = 14.dp),
        )
        loadError?.let { Text(it, color = CedalColors.Error, fontSize = 12.sp, modifier = Modifier.padding(bottom = 12.dp)) }
        SIM_TARGETS.forEach { target ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(CedalColors.CardBackground)
                    .border(1.dp, if (target.practiceTargetId != null) CedalColors.BorderCyan else CedalColors.BorderSlate, RoundedCornerShape(14.dp))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onDeploy(target) }
                    .padding(14.dp),
            ) {
                Text(target.name, color = CedalColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(target.flavor, color = CedalColors.TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp, bottom = 8.dp))
                Row {
                    if (target.practiceTargetId != null) ArcLessonPillPublic("REAL APK")
                    if (target.difficulty == "HARD") ArcLessonPillPublic("HARD")
                    ArcLessonPillPublic("Up to 30 EXP")
                }
            }
        }
    }
}

@Composable
private fun LoadingBody() {
    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator(color = CedalColors.AccentCyan, modifier = Modifier.padding(top = 60.dp, bottom = 16.dp))
        Text("Briefing incoming…", color = CedalColors.TextMuted, fontSize = 13.sp)
    }
}

// Polls ArcPracticeAppService for build status - the very first player to
// ever pick this target triggers a real Gradle build (a few minutes, same
// as Code > Kotlin builds); every player after that gets the cached APK
// instantly. Actually installing is real Android install flow (same
// FileProvider + ACTION_VIEW as Code > Kotlin), so there's no way for the
// app to know for certain when you're done in the system installer -
// "Continue" just trusts you've done it, same as any real-world tool would.
@Composable
private fun DownloadPracticeAppBody(
    target: SimTarget,
    viewModel: AuthViewModel,
    context: Context,
    onInstalled: () -> Unit,
    onSkip: () -> Unit,
) {
    val targetId = target.practiceTargetId ?: return
    val scope = rememberCoroutineScope()
    var status by remember(targetId) { mutableStateOf("queued") }
    var downloadUrl by remember(targetId) { mutableStateOf<String?>(null) }
    var errorMessage by remember(targetId) { mutableStateOf<String?>(null) }
    var installing by remember { mutableStateOf(false) }
    var installError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(targetId) {
        while (status == "queued" || status == "building") {
            viewModel.arcPracticeAppStatus(targetId)
                .onSuccess {
                    status = it.status
                    downloadUrl = it.downloadUrl
                    errorMessage = it.errorMessage
                }
                .onFailure { errorMessage = it.message }
            if (status == "queued" || status == "building") delay(4000)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(target.name, color = CedalColors.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 10.dp))
        Text(target.flavor, color = CedalColors.TextSecondary, fontSize = 13.sp, modifier = Modifier.padding(bottom = 20.dp))

        when (status) {
            "queued", "building" -> {
                CircularProgressIndicator(color = CedalColors.AccentCyan, modifier = Modifier.padding(bottom = 16.dp))
                Text(
                    if (status == "queued") "Preparing the practice app…" else "Building the practice app (first time only - usually a few minutes)…",
                    color = CedalColors.TextMuted, fontSize = 12.sp, modifier = Modifier.padding(bottom = 20.dp),
                )
            }
            "done" -> {
                val url = downloadUrl
                CedalPrimaryButton(
                    text = if (installing) "Opening installer…" else "Download & Install Practice App",
                    enabled = !installing && url != null,
                    onClick = {
                        if (url == null) return@CedalPrimaryButton
                        installing = true
                        installError = null
                        scope.launch {
                            installError = downloadAndInstallApk(context, url, targetId)
                            installing = false
                        }
                    },
                    modifier = Modifier.padding(bottom = 12.dp),
                )
                installError?.let { Text(it, color = CedalColors.Error, fontSize = 12.sp, modifier = Modifier.padding(bottom = 12.dp)) }
                CedalPrimaryButton(text = "I've installed it - Continue", onClick = onInstalled, modifier = Modifier.padding(bottom = 12.dp))
            }
            else -> {
                Text(errorMessage ?: "Couldn't prepare the practice app.", color = CedalColors.Error, fontSize = 12.sp, modifier = Modifier.padding(bottom = 12.dp))
            }
        }
        CedalPrimaryButton(text = "Skip - already installed / play without it", onClick = onSkip)
    }
}

@Composable
private fun FindFlagBody(onSubmit: (String) -> Unit, onSkip: () -> Unit) {
    var flagInput by remember { mutableStateOf("") }
    Column(modifier = Modifier.fillMaxSize().imePadding()) {
        Text("ONE LAST THING", color = CedalColors.TextSecondary, fontSize = 11.sp, letterSpacing = 1.sp, modifier = Modifier.padding(bottom = 8.dp))
        Text(
            "You explored the practice app yourself for this one - no hints given. If you found something inside it that looked like a flag/code, type it below. If not, that's fine too - skip and see your score.",
            color = CedalColors.TextMuted, fontSize = 12.sp, modifier = Modifier.padding(bottom = 16.dp),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .clip(RoundedCornerShape(50))
                .background(CedalColors.CardBackground)
                .border(1.dp, CedalColors.BorderSlate, RoundedCornerShape(50))
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            if (flagInput.isEmpty()) Text("Enter what you found…", color = CedalColors.TextMuted, fontSize = 13.sp)
            BasicTextField(
                value = flagInput,
                onValueChange = { flagInput = it },
                textStyle = TextStyle(color = CedalColors.TextPrimary, fontSize = 13.sp),
                cursorBrush = SolidColor(CedalColors.AccentCyan),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        CedalPrimaryButton(text = "Submit", enabled = flagInput.isNotBlank(), onClick = { onSubmit(flagInput) }, modifier = Modifier.padding(bottom = 8.dp))
        CedalPrimaryButton(text = "Skip", onClick = onSkip)
    }
}

@Composable
private fun BriefingBody(target: SimTarget, mission: ArcMission, onBegin: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(target.name, color = CedalColors.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 10.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(CedalColors.CardBackground)
                .border(1.dp, CedalColors.BorderCyan, RoundedCornerShape(14.dp))
                .padding(14.dp),
        ) {
            Text("SCENARIO", color = CedalColors.AccentCyan, fontSize = 10.sp, letterSpacing = 1.sp, modifier = Modifier.padding(bottom = 6.dp))
            Text(mission.scenario, color = CedalColors.TextSecondary, fontSize = 13.sp, lineHeight = 19.sp)
        }
        Text(
            "${mission.prompts.size} quick calls incoming. Each one appears briefly, then disappears - answer fast and stay sharp. Missing one just counts as wrong, nothing worse.",
            color = CedalColors.TextMuted, fontSize = 12.sp, modifier = Modifier.padding(bottom = 20.dp),
        )
        CedalPrimaryButton(text = "BEGIN", onClick = onBegin)
    }
}

@Composable
private fun FlashPromptBody(prompt: ArcMissionPrompt, promptNumber: Int, totalPrompts: Int, onAnswer: (Int?, Long) -> Unit) {
    var remainingFraction by remember(promptNumber) { mutableStateOf(1f) }
    var answered by remember(promptNumber) { mutableStateOf(false) }
    val startTime = remember(promptNumber) { System.currentTimeMillis() }

    LaunchedEffect(promptNumber) {
        val totalMs = (prompt.secondsVisible * 1000).coerceAtLeast(500)
        val tickMs = 50L
        var elapsed = 0L
        while (elapsed < totalMs) {
            delay(tickMs)
            elapsed += tickMs
            remainingFraction = (1f - elapsed.toFloat() / totalMs).coerceAtLeast(0f)
        }
        if (!answered) {
            answered = true
            onAnswer(null, totalMs.toLong())
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            "CALL $promptNumber / $totalPrompts",
            color = CedalColors.TextMuted, fontSize = 11.sp, letterSpacing = 1.sp, modifier = Modifier.padding(bottom = 10.dp),
        )
        LinearProgressIndicator(
            progress = { remainingFraction },
            color = if (remainingFraction < 0.3f) CedalColors.Error else CedalColors.AccentCyan,
            trackColor = CedalColors.BorderSlate,
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50)).padding(bottom = 18.dp),
        )
        Text(prompt.text, color = CedalColors.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 18.dp))
        prompt.options.forEachIndexed { optIndex, option ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
                    .clip(RoundedCornerShape(50))
                    .background(CedalColors.CardBackground)
                    .border(1.dp, CedalColors.BorderSlate, RoundedCornerShape(50))
                    .clickable(enabled = !answered, interactionSource = remember { MutableInteractionSource() }, indication = null) {
                        if (!answered) {
                            answered = true
                            onAnswer(optIndex, System.currentTimeMillis() - startTime)
                        }
                    }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Text(option, color = CedalColors.TextPrimary, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun ResultsBody(
    results: List<PromptResult>,
    scorePercent: Int,
    expAwarded: Long?,
    flagFound: Boolean?,
    onReplay: () -> Unit,
    onChooseAnother: () -> Unit,
) {
    val answeredResults = results.filter { it.selectedIndex != null }
    val avgReactionFraction = if (answeredResults.isNotEmpty()) {
        answeredResults.map { it.reactionMs.toFloat() / (it.prompt.secondsVisible * 1000f) }.average().toFloat()
    } else 1f
    val missedCount = results.count { it.selectedIndex == null }
    val attentivenessLabel = when {
        missedCount > 0 -> "DISTRACTED"
        avgReactionFraction < 0.4f -> "LIGHTNING REFLEXES"
        avgReactionFraction < 0.7f -> "SHARP"
        else -> "STEADY"
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Text(
            "$scorePercent%",
            color = if (scorePercent >= 70) CedalColors.Success else if (scorePercent >= 40) CedalColors.AccentCyan else CedalColors.Error,
            fontSize = 40.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp),
        )
        Text("MISSION SCORE", color = CedalColors.TextMuted, fontSize = 11.sp, letterSpacing = 2.sp, modifier = Modifier.padding(bottom = 16.dp))

        Row(modifier = Modifier.padding(bottom = 20.dp)) {
            ArcLessonPillPublic(attentivenessLabel)
            expAwarded?.let { ArcLessonPillPublic("+$it EXP") }
            if (flagFound == true) ArcLessonPillPublic("Flag Found")
        }

        Text("REVIEW", color = CedalColors.TextSecondary, fontSize = 11.sp, letterSpacing = 1.sp, modifier = Modifier.padding(bottom = 8.dp))
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(results) { r ->
                val correct = r.selectedIndex != null && r.selectedIndex == r.prompt.correctIndex
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(CedalColors.CardBackground)
                        .border(1.dp, if (correct) CedalColors.Success.copy(alpha = 0.5f) else CedalColors.Error.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                        .padding(12.dp),
                ) {
                    Text(r.prompt.text, color = CedalColors.TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        if (r.selectedIndex == null) "Missed - too slow" else if (correct) "Correct" else "Answered: ${r.prompt.options.getOrNull(r.selectedIndex) ?: "?"}",
                        color = if (correct) CedalColors.Success else CedalColors.Error, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp),
                    )
                    Text(r.prompt.explanation, color = CedalColors.TextSecondary, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }

        CedalPrimaryButton(text = "Replay (new scenario)", onClick = onReplay, modifier = Modifier.padding(top = 12.dp))
        CedalPrimaryButton(text = "Choose different target", onClick = onChooseAnother, modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun ArcLessonPillPublic(text: String) {
    Box(
        modifier = Modifier
            .padding(end = 8.dp)
            .clip(RoundedCornerShape(50))
            .background(CedalColors.Background)
            .border(1.dp, CedalColors.BorderSlate, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Text(text.uppercase(), color = CedalColors.TextPrimary, fontSize = 10.sp, letterSpacing = 0.5.sp, fontWeight = FontWeight.SemiBold)
    }
}
