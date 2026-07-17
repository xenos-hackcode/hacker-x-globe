package com.xhacker.cedal.ui.screens.member

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xhacker.cedal.ui.theme.CedalColors
import com.xhacker.cedal.viewmodel.AuthViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

// ARC's Learn tab, rebuilt to match Invest > Learn's real pattern (see
// MemberInvestScreen.kt's LESSONS/InvestLearnBody): leveled lessons with a
// quiz and completion tracking that awards Profile's rank exp server-side
// via the same LessonService every other Learn tab uses (see AuthViewModel.
// completeLesson) - lessonId is prefixed "arc:" so titles can never collide
// with Invest's lesson keyspace in LessonCompletions.

private data class ArcQuizQuestion(val question: String, val options: List<String>, val correctIndex: Int, val explanation: String)

private data class ArcLesson(
    val id: String,
    val title: String,
    val level: String,
    val duration: String,
    val tag: String,
    val teaser: String,
    val body: String,
    val quiz: List<ArcQuizQuestion> = emptyList(),
    val practiceHint: String? = null,
    val hasPacketDemo: Boolean = false,
)

private val ARC_LEVEL_ORDER = listOf("Beginner", "Intermediate", "Advanced")

private fun lessonId(title: String) = "arc:$title"

// Matches LessonService.EXP_PER_LESSON server-side - flat, same for every
// lesson across every Learn area (Invest and ARC alike).
private const val ARC_LESSON_EXP = 50

private val ARC_LESSONS = listOf(
    ArcLesson(
        id = "legal-boundaries",
        title = "What Ethical Hacking Actually Means",
        level = "Beginner", duration = "3 min", tag = "Basics",
        teaser = "Authorization is the whole difference between a career and a crime.",
        body = "\"Ethical hacking\" (also called penetration testing, or white-hat hacking) means finding " +
            "security weaknesses in a system with the owner's explicit, written permission, so they can be " +
            "fixed before someone with bad intentions finds them first.\n\n" +
            "The single line that separates this from a crime is authorization. The exact same technical " +
            "action - scanning a server, testing a login form, checking if a Wi-Fi network is weak - is a " +
            "paid, respected job when the owner asked for it in writing, and a serious criminal offense when " +
            "they didn't.\n\n" +
            "Real engagements start with a signed scope agreement covering exactly what you're allowed to " +
            "test, when, and how far you can go. Professionals keep that document on hand for the entire " +
            "engagement.\n\n" +
            "Curiosity about how systems break is a great instinct - it's the foundation of this whole " +
            "field. The only rule is: get permission first, in writing, every time.",
        quiz = listOf(
            ArcQuizQuestion(
                "What's the one thing that makes hacking legal instead of a crime?",
                listOf("Written permission from the owner beforehand", "Not causing any damage", "Only looking, not changing anything", "Being really good at it"),
                correctIndex = 0,
                explanation = "Authorization is everything - the exact same action is legal or illegal purely based on whether you were invited to do it.",
            ),
            ArcQuizQuestion(
                "What does a real security engagement start with?",
                listOf("A signed scope agreement", "A verbal \"sure, go ahead\"", "Nothing - just start testing", "A subscription fee"),
                correctIndex = 0,
                explanation = "Professionals get the rules of engagement in writing before touching anything.",
            ),
        ),
    ),
    ArcLesson(
        id = "the-law",
        title = "The Law: What Actually Makes It Illegal",
        level = "Beginner", duration = "3 min", tag = "Legal",
        teaser = "\"I didn't cause damage\" isn't a defense - here's why.",
        body = "Most countries have a law built around one core idea: accessing a computer system without " +
            "authorization is illegal, whether or not you cause any damage, and whether or not you take " +
            "anything.\n\n" +
            "In the UK, that's the Computer Misuse Act 1990. In the US, it's the Computer Fraud and Abuse " +
            "Act (CFAA). Both work the same way: unauthorized access itself is the crime, full stop.\n\n" +
            "A few things that trip people up:\n" +
            "• \"I only looked\" is not a defense - looking without permission is already the crime.\n" +
            "• Testing a friend's device or account without their explicit permission still counts.\n" +
            "• Scanning a network you don't own, even just to \"see what's there,\" can qualify too.\n\n" +
            "The practical takeaway: always get written permission before testing anything that isn't " +
            "100% yours.",
        quiz = listOf(
            ArcQuizQuestion(
                "Is \"I didn't cause any damage\" a legal defense for unauthorized access?",
                listOf("No - unauthorized access is the crime itself", "Yes, always", "Only in the US", "Only if it was an accident"),
                correctIndex = 0,
                explanation = "The access itself is what's illegal - damage isn't required for it to be a crime.",
            ),
            ArcQuizQuestion(
                "Can you legally test a friend's phone or account without asking them first?",
                listOf("No - you need their explicit permission too", "Yes, since you know them", "Only if you mean well", "Only on weekends"),
                correctIndex = 0,
                explanation = "Good intentions don't grant authorization - only the owner explicitly saying yes does.",
            ),
        ),
    ),
    ArcLesson(
        id = "methodology",
        title = "The Standard Methodology",
        level = "Beginner", duration = "4 min", tag = "Process",
        teaser = "Recon, scanning, enumeration, reporting - the real shape of a pentest.",
        body = "Professional penetration tests follow a repeatable structure, not random poking around:\n\n" +
            "1. Reconnaissance - gathering public information (domain registration, technology stack), " +
            "nothing touches the target yet.\n\n" +
            "2. Scanning & Enumeration - actively probing agreed-upon systems to find live hosts and open " +
            "services (this is what Labs' scanner is a safe taste of).\n\n" +
            "3. Vulnerability Assessment - matching what was found against known weaknesses.\n\n" +
            "4. Exploitation (only within agreed scope) - carefully proving a weakness is real, without " +
            "causing damage.\n\n" +
            "5. Reporting - the actual deliverable. A report lists every finding, how severe it is, and how " +
            "to fix it. This is the part that actually makes systems safer.",
        quiz = listOf(
            ArcQuizQuestion(
                "What's the very first step of a professional pentest?",
                listOf("Reconnaissance - passive info gathering", "Exploitation", "Writing the report", "Scanning ports"),
                correctIndex = 0,
                explanation = "Recon comes first and doesn't touch the target at all - it's just gathering public information.",
            ),
            ArcQuizQuestion(
                "What's the actual deliverable that makes a pentest valuable to the client?",
                listOf("The report, with findings and fixes", "The exploit code", "The scan results alone", "A verbal summary"),
                correctIndex = 0,
                explanation = "Everything before the report is just gathering evidence - the report is what actually gets things fixed.",
            ),
        ),
    ),
    ArcLesson(
        id = "networking-basics",
        title = "Networking Fundamentals for Security",
        level = "Beginner", duration = "4 min", tag = "Networking",
        teaser = "IP addresses, subnets, and ports - the vocabulary everything else builds on.",
        body = "Every device on a network has an IP address, a number like 192.168.1.14 that identifies it. " +
            "On a typical home Wi-Fi network, every device connected shares the same first three numbers - " +
            "the \"subnet.\"\n\n" +
            "A port is a numbered \"door\" a device leaves open for a specific kind of connection - port 80 " +
            "for unencrypted web traffic, 443 for encrypted (HTTPS), 22 for secure remote login (SSH).\n\n" +
            "\"Scanning a network\" - what Labs does - just means checking which IPs on a subnet currently " +
            "have something responding. It's the digital version of walking down a street noting which " +
            "houses have their lights on. Doing this on your own home network is completely normal - doing " +
            "it against a network you don't own is where it becomes illegal (see the previous lesson).\n\n" +
            "TCP double-checks every packet arrived correctly (web pages, file transfers). UDP just sends " +
            "without checking (video calls, where a dropped packet matters less than a delay).",
        quiz = listOf(
            ArcQuizQuestion(
                "What does an IP address do?",
                listOf("Identifies one device on a network", "Encrypts your data", "Blocks hackers automatically", "Speeds up Wi-Fi"),
                correctIndex = 0,
                explanation = "It's just a number that identifies a specific device, like a house address.",
            ),
            ArcQuizQuestion(
                "Which port is used for encrypted web traffic (HTTPS)?",
                listOf("443", "80", "22", "8080"),
                correctIndex = 0,
                explanation = "443 is HTTPS. 80 is unencrypted HTTP, and 22 is SSH remote login.",
            ),
        ),
        practiceHint = "Open Labs and scan your own Wi-Fi network - that's exactly what this lesson is describing, live.",
    ),
    ArcLesson(
        id = "common-weaknesses",
        title = "Common Vulnerabilities (Conceptually)",
        level = "Intermediate", duration = "4 min", tag = "Risk",
        teaser = "The recurring mistakes real audits find, at an awareness level.",
        body = "You don't need exploit code to understand why these keep showing up in real security " +
            "reports:\n\n" +
            "• Weak or reused passwords - the single most common finding in real audits, by far.\n" +
            "• Unpatched software - fixes get published constantly; systems that don't update stay exposed.\n" +
            "• Open, unnecessary services - a port left open \"just in case\" is one more thing to probe.\n" +
            "• Default credentials - devices often ship with a well-known default password nobody changes.\n" +
            "• Social engineering - tricking a person, not a machine, into handing over access. Often " +
            "easier than any technical attack.\n\n" +
            "Every one of these is fixed by boring, unglamorous discipline: unique passwords, prompt " +
            "updates, minimal open services, changed defaults, and staff awareness training.",
        quiz = listOf(
            ArcQuizQuestion(
                "What's the single most common finding in real security audits?",
                listOf("Weak or reused passwords", "Alien hackers", "Broken keyboards", "Too much RAM"),
                correctIndex = 0,
                explanation = "By far the most common issue - one reused password can compromise ten accounts at once.",
            ),
            ArcQuizQuestion(
                "What is social engineering?",
                listOf("Tricking a person into handing over access", "A type of firewall", "A programming language", "A kind of encryption"),
                correctIndex = 0,
                explanation = "It targets people, not machines - often easier and more effective than any technical exploit.",
            ),
        ),
    ),
    ArcLesson(
        id = "wireshark-packets",
        title = "Reading Real Network Traffic (Wireshark-Style)",
        level = "Intermediate", duration = "5 min", tag = "Tools",
        teaser = "What a real packet analyzer like Wireshark actually shows you.",
        body = "Every single thing your phone or computer does online - loading a webpage, sending a " +
            "message, checking the time - happens as tiny bursts of data called packets, each one labeled " +
            "with where it came from, where it's going, and what kind of traffic it is.\n\n" +
            "Wireshark is the most famous tool for watching these packets fly by in real time, one line per " +
            "packet. It looks intimidating at first - a huge scrolling wall of numbers - but every single " +
            "line is just answering three questions: WHO is talking (source), WHO they're talking TO " +
            "(destination), and WHAT KIND of conversation it is (the protocol - DNS, TLS, ARP, and so on).\n\n" +
            "Below is a simulated, real-time feed showing exactly that - the same shape of information " +
            "Wireshark shows, in plain English underneath. This is illustrative data, not your device's " +
            "actual live traffic - real packet capture needs special low-level access this app deliberately " +
            "doesn't request, since normal apps have no business reading your unrelated network traffic.\n\n" +
            "Once you can read a line like \"192.168.1.14 → 8.8.8.8, DNS\" and immediately think \"my phone " +
            "is asking a name-lookup server for an address,\" you've learned the actual skill - everything " +
            "else is just recognizing more protocol names.",
        quiz = listOf(
            ArcQuizQuestion(
                "In a packet, what does the \"destination\" tell you?",
                listOf("Where the data is going", "How big the file is", "The Wi-Fi password", "The device's owner's name"),
                correctIndex = 0,
                explanation = "Source = who's sending it, destination = who it's going to.",
            ),
            ArcQuizQuestion(
                "What does DNS traffic usually mean is happening?",
                listOf("A device is looking up the address for a website name", "A virus is installing", "A file is being deleted", "The Wi-Fi password is being sent in plain text"),
                correctIndex = 0,
                explanation = "DNS is the internet's \"phone book\" lookup - turning names like google.com into an IP address.",
            ),
        ),
        hasPacketDemo = true,
    ),
    ArcLesson(
        id = "practicing-legally",
        title = "Getting Practice - Legally",
        level = "Intermediate", duration = "3 min", tag = "Getting Started",
        teaser = "Where to actually try this hands-on, with permission built in from the start.",
        body = "You don't need to touch a real, unauthorized system to build real skills:\n\n" +
            "• CTFs (Capture The Flag competitions) - purpose-built vulnerable challenges you're explicitly " +
            "invited to break.\n" +
            "• TryHackMe and Hack The Box - guided, legal practice labs for learners, beginner to advanced.\n" +
            "• Bug bounty programs (HackerOne, Bugcrowd) - companies explicitly invite testing of their " +
            "real systems, within a published scope, and pay for valid findings.\n" +
            "• Your own home lab - an intentionally vulnerable device or VM you fully own.\n" +
            "• This app's Labs and Ops tabs - safe first steps, no special setup required.\n\n" +
            "Every path above shares the same property as the very first lesson: someone said yes, in " +
            "writing, before any testing started.",
        quiz = listOf(
            ArcQuizQuestion(
                "What do bug bounty programs actually authorize?",
                listOf("Testing a real company's systems, within a published scope", "Testing any system you want", "Only testing your own phone", "Nothing - they're just for show"),
                correctIndex = 0,
                explanation = "Bug bounties are real, paid authorization - a company inviting testing within specific, published rules.",
            ),
        ),
        practiceHint = "Try ARC Ops next - a fully sandboxed simulation of exactly this kind of decision-making, safe to replay as much as you want.",
    ),
)

@Composable
fun ArcLearnBody(onBack: () -> Unit, viewModel: AuthViewModel) {
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val storage = viewModel.storage
    val scope = rememberCoroutineScope()
    val completed = remember {
        mutableStateMapOf<String, Boolean>().apply { ARC_LESSONS.forEach { put(it.title, storage.isLessonCompleted(lessonId(it.title))) } }
    }
    val quizScores = remember {
        mutableStateMapOf<String, Int>().apply { ARC_LESSONS.forEach { l -> storage.getQuizScorePercent(lessonId(l.title))?.let { put(l.title, it) } } }
    }

    fun onToggle(title: String, done: Boolean) {
        completed[title] = done
        storage.setLessonCompleted(lessonId(title), done)
        if (done) scope.launch { viewModel.completeLesson(lessonId(title)) }
    }

    val index = selectedIndex
    if (index != null) {
        val lesson = ARC_LESSONS[index]
        ArcLessonDetailBody(
            lesson = lesson,
            onBack = { selectedIndex = null },
            completed = completed[lesson.title] == true,
            onToggleCompleted = { done -> onToggle(lesson.title, done) },
            onQuizScored = { percent ->
                quizScores[lesson.title] = percent
                storage.setQuizScorePercent(lessonId(lesson.title), percent)
            },
        )
    } else {
        ArcLearnListBody(
            onBack = onBack,
            completed = completed,
            quizScores = quizScores,
            onOpenLesson = { selectedIndex = it },
            viewModel = viewModel,
        )
    }
}

@Composable
private fun ArcLearnListBody(
    onBack: () -> Unit,
    completed: Map<String, Boolean>,
    quizScores: Map<String, Int>,
    onOpenLesson: (Int) -> Unit,
    viewModel: AuthViewModel,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        MemberBackBar(title = "ARC", onBack = onBack)
        Text("LEARN", color = CedalColors.TextSecondary, fontSize = 11.sp, letterSpacing = 2.sp, modifier = Modifier.padding(bottom = 6.dp))
        Text(
            "Legal, ethical cybersecurity - simple explanations, real depth. Tap a lesson to read it.",
            color = CedalColors.TextMuted, fontSize = 12.sp, modifier = Modifier.padding(bottom = 10.dp),
        )

        val grouped = ARC_LESSONS.withIndex().groupBy { it.value.level }
        val orderedLevels = ARC_LEVEL_ORDER.filter { grouped.containsKey(it) } + grouped.keys.filter { it !in ARC_LEVEL_ORDER }

        LazyColumn(modifier = Modifier.weight(1f)) {
            item(key = "daily-task") { DailyTaskCard(area = "arc", viewModel = viewModel) }
            orderedLevels.forEach { level ->
                val items = grouped[level].orEmpty()
                item(key = "header-$level") {
                    val doneCount = items.count { completed[it.value.title] == true }
                    val groupPercent = if (items.isNotEmpty()) doneCount * 100 / items.size else 0
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 10.dp, bottom = 8.dp)) {
                        Text(level.uppercase(), color = CedalColors.TextSecondary, fontSize = 11.sp, letterSpacing = 2.sp, modifier = Modifier.weight(1f))
                        Text(
                            "$groupPercent% done",
                            color = if (groupPercent == 100) CedalColors.Success else CedalColors.TextMuted,
                            fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
                items(items, key = { it.value.id }) { (idx, lesson) ->
                    val isDone = completed[lesson.title] == true
                    val score = quizScores[lesson.title]
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(CedalColors.CardBackground)
                            .border(1.dp, if (isDone) CedalColors.Success.copy(alpha = 0.5f) else CedalColors.BorderSlate, RoundedCornerShape(14.dp))
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onOpenLesson(idx) }
                            .padding(14.dp),
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(if (isDone) CedalColors.Success else CedalColors.Background)
                                .border(1.dp, if (isDone) CedalColors.Success else CedalColors.BorderSlate, CircleShape),
                        ) {
                            if (isDone) Text("✓", color = CedalColors.Background, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                            Text(lesson.title, color = CedalColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text(lesson.teaser, color = CedalColors.TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp, bottom = 8.dp))
                            Row {
                                ArcLessonPill(lesson.duration)
                                ArcLessonPill(lesson.tag)
                                ArcLessonPill("+$ARC_LESSON_EXP EXP")
                                if (score != null) ArcLessonPill("Quiz $score%")
                            }
                        }
                        Text("›", color = CedalColors.TextSecondary, fontSize = 20.sp, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ArcLessonDetailBody(
    lesson: ArcLesson,
    onBack: () -> Unit,
    completed: Boolean,
    onToggleCompleted: (Boolean) -> Unit,
    onQuizScored: (Int) -> Unit,
) {
    val answers = remember(lesson.id) { mutableStateMapOf<Int, Int>() }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        MemberBackBar(title = lesson.title, onBack = onBack)
        Row(modifier = Modifier.padding(bottom = 12.dp)) {
            ArcLessonPill(lesson.level)
            ArcLessonPill(lesson.duration)
            ArcLessonPill(lesson.tag)
            ArcLessonPill("+$ARC_LESSON_EXP EXP")
        }
        Text(lesson.body, color = CedalColors.TextSecondary, fontSize = 13.sp, lineHeight = 19.sp)

        if (lesson.hasPacketDemo) {
            WiresharkStyleDemo()
        }

        lesson.practiceHint?.let { hint ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 18.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(CedalColors.CardBackground)
                    .border(1.dp, CedalColors.BorderCyan, RoundedCornerShape(14.dp))
                    .padding(14.dp),
            ) {
                Text("PRACTICE THIS FOR REAL", color = CedalColors.AccentCyan, fontSize = 11.sp, letterSpacing = 1.sp, modifier = Modifier.padding(bottom = 6.dp))
                Text(hint, color = CedalColors.TextSecondary, fontSize = 12.sp)
            }
        }

        if (lesson.quiz.isNotEmpty()) {
            Text(
                "TRY IT — QUICK CHECK",
                color = CedalColors.TextSecondary, fontSize = 11.sp, letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 20.dp, bottom = 8.dp),
            )
            lesson.quiz.forEachIndexed { qIndex, question ->
                ArcQuizCard(
                    question = question,
                    index = qIndex,
                    selected = answers[qIndex],
                    onAnswer = { optIndex ->
                        answers[qIndex] = optIndex
                        if (answers.size == lesson.quiz.size) {
                            val correctCount = lesson.quiz.indices.count { answers[it] == lesson.quiz[it].correctIndex }
                            onQuizScored(correctCount * 100 / lesson.quiz.size)
                        }
                    },
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(CedalColors.CardBackground)
                .border(1.dp, if (completed) CedalColors.Success.copy(alpha = 0.5f) else CedalColors.BorderSlate, RoundedCornerShape(14.dp))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = { onToggleCompleted(!completed) })
                .padding(14.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(if (completed) CedalColors.Success else CedalColors.Background)
                    .border(1.dp, if (completed) CedalColors.Success else CedalColors.BorderSlate, CircleShape),
            ) {
                if (completed) Text("✓", color = CedalColors.Background, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Text(
                if (completed) "Marked as done" else "Mark this lesson as done",
                color = CedalColors.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 12.dp),
            )
        }
    }
}

@Composable
private fun ArcQuizCard(question: ArcQuizQuestion, index: Int, selected: Int?, onAnswer: (Int) -> Unit) {
    val answered = selected != null
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(CedalColors.CardBackground)
            .border(1.dp, CedalColors.BorderSlate, RoundedCornerShape(14.dp))
            .padding(14.dp),
    ) {
        Text(
            "${index + 1}. ${question.question}",
            color = CedalColors.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 10.dp),
        )
        question.options.forEachIndexed { optIndex, option ->
            val isCorrect = optIndex == question.correctIndex
            val isSelected = selected == optIndex
            val borderColor = when {
                !answered -> CedalColors.BorderSlate
                isCorrect -> CedalColors.Success
                isSelected -> CedalColors.Error
                else -> CedalColors.BorderSlate
            }
            val bgColor = when {
                !answered -> CedalColors.Background
                isCorrect -> CedalColors.Success.copy(alpha = 0.1f)
                isSelected -> CedalColors.Error.copy(alpha = 0.1f)
                else -> CedalColors.Background
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(bgColor)
                    .border(1.dp, borderColor, RoundedCornerShape(50))
                    .clickable(enabled = !answered, interactionSource = remember { MutableInteractionSource() }, indication = null) { onAnswer(optIndex) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Text(option, color = CedalColors.TextPrimary, fontSize = 12.sp, modifier = Modifier.weight(1f))
                if (answered && isCorrect) Text("✓", color = CedalColors.Success, fontSize = 14.sp)
                if (answered && isSelected && !isCorrect) Text("✗", color = CedalColors.Error, fontSize = 14.sp)
            }
        }
        if (answered) {
            Text(question.explanation, color = CedalColors.TextSecondary, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

@Composable
private fun ArcLessonPill(text: String) {
    Box(
        modifier = Modifier
            .padding(end = 6.dp)
            .clip(RoundedCornerShape(50))
            .background(CedalColors.Background)
            .border(1.dp, CedalColors.BorderSlate, RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(text.uppercase(), color = CedalColors.TextMuted, fontSize = 9.sp, letterSpacing = 0.4.sp)
    }
}

// --- Simulated real-time "Wireshark" packet demo ---

private data class SimPacket(val src: String, val dst: String, val proto: String, val note: String)

private val PACKET_TEMPLATES = listOf(
    SimPacket("192.168.1.14", "8.8.8.8", "DNS", "Your phone is asking \"what's the address for google.com?\""),
    SimPacket("192.168.1.14", "142.250.80.14", "TLS", "Starting a secure, encrypted connection to a Google server."),
    SimPacket("192.168.1.1", "192.168.1.14", "ARP", "Your router asking \"who has this address?\" to find devices on the network."),
    SimPacket("192.168.1.14", "17.248.140.13", "TLS", "Your phone checking for app updates over an encrypted connection."),
    SimPacket("192.168.1.22", "192.168.1.1", "DHCP", "A device just joined the Wi-Fi and is asking the router for an IP address."),
    SimPacket("192.168.1.14", "104.16.132.229", "HTTPS", "Loading a secure webpage - the padlock-icon kind of connection."),
    SimPacket("192.168.1.14", "192.168.1.1", "DNS", "Asking your own router to look up a website name (a common home setup)."),
    SimPacket("192.168.1.30", "239.255.255.250", "SSDP", "A smart-home device announcing itself to other devices on the network."),
    SimPacket("192.168.1.14", "13.107.42.14", "TLS", "A background app quietly syncing data over an encrypted channel."),
    SimPacket("192.168.1.1", "192.168.1.14", "ICMP", "A basic \"are you still there?\" ping between your router and your phone."),
)

@Composable
private fun WiresharkStyleDemo() {
    val listState = rememberLazyListState()
    val lines = remember { mutableStateOf(listOf<Pair<Int, SimPacket>>()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        var counter = 0
        while (true) {
            delay(1100)
            counter++
            val next = counter to PACKET_TEMPLATES[Random.nextInt(PACKET_TEMPLATES.size)]
            lines.value = (lines.value + next).takeLast(30)
            scope.launch { listState.animateScrollToItem((lines.value.size - 1).coerceAtLeast(0)) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(CedalColors.Background)
            .border(1.dp, CedalColors.Success.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
            .padding(12.dp),
    ) {
        Text(
            "SIMULATED LIVE CAPTURE",
            color = CedalColors.Success, fontSize = 10.sp, letterSpacing = 1.sp,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        LazyColumn(state = listState, modifier = Modifier.fillMaxWidth().size(220.dp)) {
            items(lines.value, key = { it.first }) { (id, packet) ->
                Column(modifier = Modifier.padding(bottom = 8.dp)) {
                    Text(
                        "${packet.src} → ${packet.dst}   [${packet.proto}]",
                        color = CedalColors.Success, fontSize = 11.sp, fontFamily = FontFamily.Monospace,
                    )
                    Text(packet.note, color = CedalColors.TextMuted, fontSize = 10.sp, modifier = Modifier.padding(top = 1.dp))
                }
            }
        }
        Text(
            "Illustrative only - not your device's actual traffic (real packet capture needs low-level access this app deliberately doesn't request).",
            color = CedalColors.TextMuted, fontSize = 9.sp, modifier = Modifier.padding(top = 4.dp),
        )
    }
}
