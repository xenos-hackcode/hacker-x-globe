package com.xhacker.cedal.ui.screens.member

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xhacker.cedal.data.UserProfile
import com.xhacker.cedal.ui.RankTable
import com.xhacker.cedal.ui.theme.CedalColors
import com.xhacker.cedal.ui.theme.CedalErrorText
import com.xhacker.cedal.ui.theme.CedalNeonProgressBar
import com.xhacker.cedal.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

// Ported field-for-field from cedal-mobile's
// src/member/profile/{MemberProfileScreen,component/*}.tsx — same header
// pattern (pill "Back" button, saves on back rather than a separate Save
// button), same avatar block, same Node Identity field set. Activity/Groups
// panels not built (need the level/points/reputation and group-membership
// systems, both separate later milestones) - avatar upload IS real now,
// see ImageUploadService server-side.
@Composable
fun MemberProfileBody(onBack: () -> Unit, onEditNumber: () -> Unit = {}, viewModel: AuthViewModel = hiltViewModel()) {
    var profile by remember { mutableStateOf<UserProfile?>(null) }
    // "Edit Number" only does something once a number is actually verified -
    // adding a FIRST number still only happens in Settings > More > Security.
    var phoneVerified by remember { mutableStateOf(false) }
    var nickname by remember { mutableStateOf("") }
    var handle by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var occupation by remember { mutableStateOf("") }
    var hobby by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var uploadingAvatar by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    val avatarPicker = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        uploadingAvatar = true
        scope.launch {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
            if (bytes != null) {
                viewModel.uploadImage("avatar", bytes, mimeType)
                    .onSuccess { url ->
                        viewModel.updateAvatarUrl(url).onSuccess { profile = it }
                    }
                    .onFailure { error = it.message }
            }
            uploadingAvatar = false
        }
    }

    var activeBadge by remember { mutableStateOf<com.xhacker.cedal.data.AchievementDto?>(null) }

    LaunchedEffect(Unit) {
        viewModel.getProfile().onSuccess { p ->
            profile = p
            nickname = p.nickname ?: ""
            handle = p.handle ?: ""
            age = p.age?.toString() ?: ""
            occupation = p.occupation ?: ""
            hobby = p.hobby ?: ""
            gender = p.gender ?: ""
            bio = p.bio ?: ""
            val badgeKey = p.activeBadgeKey
            if (badgeKey != null) {
                viewModel.listAchievements().onSuccess { list -> activeBadge = list.firstOrNull { it.key == badgeKey } }
            }
        }
        viewModel.getPhoneStatus().onSuccess { phoneVerified = it.phoneVerified }
    }

    fun saveAndBack() {
        saving = true; error = null
        scope.launch {
            val result = viewModel.updateProfile(
                nickname = nickname.ifBlank { null },
                handle = handle.ifBlank { null },
                age = age.toIntOrNull(),
                occupation = occupation.ifBlank { null },
                hobby = hobby.ifBlank { null },
                gender = gender.ifBlank { null },
                bio = bio.ifBlank { null },
            )
            saving = false
            // On failure, stay put with the error visible instead of
            // navigating away — the error used to be set and then
            // immediately covered by the back-navigation in the same
            // coroutine, so it was never actually readable. Back only
            // really leaves once the save succeeds (or the user taps
            // back again after reading the error).
            result.onSuccess { onBack() }.onFailure { error = it.message }
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(CedalColors.Background).padding(16.dp).imePadding()) {
        // Back bar stays fixed — only the fields below scroll.
        MemberBackBar(title = "My Profile", busy = saving, onBack = ::saveAndBack)
        CedalErrorText(error)

        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {

        val displayName = nickname.ifBlank { profile?.email?.substringBefore("@") ?: "Design lab" }
        val initial = displayName.firstOrNull()?.uppercaseChar()?.toString() ?: "?"

        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(CedalColors.BackgroundBlob)
                    .border(2.dp, CedalColors.BorderCyan, CircleShape)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                        avatarPicker.launch(androidx.activity.result.PickVisualMediaRequest(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                contentAlignment = Alignment.Center,
            ) {
                val avatarUrl = profile?.avatarUrl
                if (avatarUrl != null) {
                    coil.compose.AsyncImage(
                        model = avatarUrl,
                        contentDescription = "Avatar",
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                    )
                } else {
                    Text(initial, color = CedalColors.TextPrimary, fontSize = 36.sp)
                }
                if (uploadingAvatar) {
                    Box(modifier = Modifier.fillMaxSize().background(CedalColors.Background.copy(alpha = 0.5f)), contentAlignment = Alignment.Center) {
                        androidx.compose.material3.CircularProgressIndicator(color = CedalColors.AccentCyan, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                    }
                }
            }
            Text("Tap to change photo", color = CedalColors.TextMuted, fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                Text(displayName, color = CedalColors.TextPrimary, fontSize = 18.sp)
                activeBadge?.let { badge ->
                    Text(
                        "🏆 ${badge.bigWord.uppercase()}",
                        color = CedalColors.AccentCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .clip(RoundedCornerShape(50))
                            .border(1.dp, CedalColors.AccentCyan, RoundedCornerShape(50))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
            }
            Text(
                profile?.email ?: "guest node — no email",
                color = CedalColors.TextSecondary,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 2.dp),
            )
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(CedalColors.Success))
                Text(
                    "Online in Cedal mesh",
                    color = CedalColors.Success,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
        }

        profile?.let {
            SecurityScoreCard(profile = it, biometricEnabled = viewModel.storage.biometricEnabled)
            ProfileRankCard(exp = it.exp)
        }

        Text("NODE IDENTITY", color = CedalColors.TextPrimary, fontSize = 13.sp, modifier = Modifier.padding(bottom = 8.dp))

        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
            ProfileField(label = "Node ID", value = profile?.publicId ?: "…", editable = false, modifier = Modifier.weight(1f))
            Box(modifier = Modifier.size(8.dp))
            ProfileField(
                label = "Nickname",
                value = nickname,
                onValueChange = { nickname = it },
                placeholder = "Nickname",
                modifier = Modifier.weight(1f),
            )
        }

        // Only ever enabled once a number is actually verified - adding the
        // FIRST one still only happens in Settings > More > Security, this
        // is purely a shortcut for CHANGING an existing one.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(CedalColors.CardBackground)
                .border(1.dp, if (phoneVerified) CedalColors.BorderCyan else CedalColors.BorderSlate, RoundedCornerShape(12.dp))
                .clickable(
                    enabled = phoneVerified,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onEditNumber,
                )
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Text(
                "Edit Number",
                color = if (phoneVerified) CedalColors.AccentCyan else CedalColors.TextMuted,
                fontSize = 13.sp,
            )
        }

        ProfileField(
            label = "Handle",
            value = handle,
            onValueChange = { handle = it },
            placeholder = "unique_handle",
            hint = "EDITABLE",
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        )
        ProfileField(
            label = "Age",
            value = age,
            onValueChange = { age = it },
            placeholder = "16",
            keyboardType = KeyboardType.Number,
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        )
        ProfileField(
            label = "Occupation",
            value = occupation,
            onValueChange = { occupation = it },
            placeholder = "Student, dev, etc.",
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        )
        ProfileField(
            label = "Hobby",
            value = hobby,
            onValueChange = { hobby = it },
            placeholder = "Gaming, coding, streaming…",
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        )
        ProfileField(
            label = "Gender",
            value = gender,
            onValueChange = { gender = it },
            placeholder = "e.g. Male, Female, Non-binary",
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        )

        Text("BIO", color = CedalColors.TextSecondary, fontSize = 11.sp, modifier = Modifier.padding(bottom = 3.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(CedalColors.Background)
                .border(1.dp, CedalColors.BorderSlate, RoundedCornerShape(10.dp))
                .padding(horizontal = 10.dp, vertical = 8.dp),
        ) {
            if (bio.isEmpty()) {
                Text("Tell the mesh who you are…", color = CedalColors.TextMuted, fontSize = 12.sp)
            }
            BasicTextField(
                value = bio,
                onValueChange = { bio = it },
                textStyle = TextStyle(color = CedalColors.TextSecondary, fontSize = 12.sp),
                cursorBrush = SolidColor(CedalColors.AccentCyan),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        }
    }
}

// Weighting: linking + verifying email and turning on two-way verification /
// biometric matter more than optional profile fields, but a fully filled-out
// profile still counts — "everything in your profile" feeds into this too,
// per how cedal-mobile frames account completeness as part of node trust.
private val PROFILE_COMPLETENESS_FIELD_COUNT = 7

private fun securityScore(profile: UserProfile, biometricEnabled: Boolean): Int {
    var score = 0
    if (!profile.isGuest) score += 10
    if (profile.emailVerified) score += 10
    if (profile.twoFactorEnabled) score += 25
    if (biometricEnabled) score += 20
    val filled = listOf(
        profile.nickname, profile.handle, profile.occupation,
        profile.hobby, profile.bio, profile.gender, profile.age?.toString(),
    ).count { !it.isNullOrBlank() }
    score += ((filled.toFloat() / PROFILE_COMPLETENESS_FIELD_COUNT) * 35).toInt()
    return score.coerceIn(0, 100)
}

@Composable
private fun SecurityScoreCard(profile: UserProfile, biometricEnabled: Boolean) {
    val score = securityScore(profile, biometricEnabled)
    val filled = listOf(
        profile.nickname, profile.handle, profile.occupation,
        profile.hobby, profile.bio, profile.gender, profile.age?.toString(),
    ).count { !it.isNullOrBlank() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 18.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(CedalColors.Background)
            .border(1.dp, CedalColors.BorderSlate, RoundedCornerShape(18.dp))
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "SECURITY SCORE", color = CedalColors.TextSecondary, fontSize = 11.sp, letterSpacing = 2.sp,
            modifier = Modifier.padding(bottom = 6.dp),
        )
        SecurityGauge(percent = score)

        Column(modifier = Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 8.dp)) {
            SecurityChecklistRow("Email linked", !profile.isGuest)
            SecurityChecklistRow("Email verified", profile.emailVerified)
            SecurityChecklistRow("Two-way verification", profile.twoFactorEnabled)
            SecurityChecklistRow("Biometric unlock", biometricEnabled)
            SecurityChecklistRow("Profile filled in", filled == PROFILE_COMPLETENESS_FIELD_COUNT, subtext = "$filled/$PROFILE_COMPLETENESS_FIELD_COUNT")
        }
    }
}

// Profile's own progression - Human through Godhood, earned by completing
// Invest > Learn lessons (see LessonService server-side), separate from
// Shop's real-money xp/tier. Every 3 months both this and Shop's tier decay
// by 2 ranks (floored near the bottom) - see cedal-server's DecayService.
@Composable
private fun ProfileRankCard(exp: Long) {
    val rankCount = RankTable.PROFILE_RANK_ORDER.size
    val rank = RankTable.rankForPoints(exp, rankCount)
    val maxRank = RankTable.isMaxRank(rank, rankCount)
    val pointsIntoRank = exp - RankTable.pointsAtTierStart(rank.tierIndex)
    val currentLevelStart = RankTable.levelThreshold(rank.level)
    val nextLevelStart = RankTable.levelThreshold(rank.level + 1)
    val levelSpan = nextLevelStart - currentLevelStart
    val progress = if (maxRank) 1f else (pointsIntoRank - currentLevelStart).toFloat() / levelSpan
    val expRemaining = nextLevelStart - pointsIntoRank
    val nextRankLabel = if (maxRank) null else if (rank.level < RankTable.LEVELS_PER_TIER) {
        "${RankTable.PROFILE_RANK_ORDER[rank.tierIndex]} ${rank.level + 1}"
    } else {
        "${RankTable.PROFILE_RANK_ORDER[rank.tierIndex + 1]} 1"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 18.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(CedalColors.Background)
            .border(1.dp, CedalColors.BorderSlate, RoundedCornerShape(18.dp))
            .padding(14.dp),
    ) {
        Text("RANK", color = CedalColors.TextSecondary, fontSize = 11.sp, letterSpacing = 2.sp, modifier = Modifier.padding(bottom = 6.dp))
        Text("$exp exp", color = CedalColors.TextSecondary, fontSize = 11.sp)
        Text(
            "${RankTable.PROFILE_RANK_ORDER[rank.tierIndex]} ${rank.level}",
            color = CedalColors.TextPrimary, fontSize = 20.sp, modifier = Modifier.padding(top = 2.dp, bottom = 2.dp),
        )
        Text(
            if (maxRank) "Max rank reached." else "$expRemaining exp needed to reach $nextRankLabel",
            color = CedalColors.TextSecondary, fontSize = 12.sp,
        )
        CedalNeonProgressBar(
            progress = progress,
            modifier = Modifier.padding(vertical = 8.dp),
            label = if (maxRank) "MAX" else "${pointsIntoRank - currentLevelStart} / $levelSpan exp",
        )
        Text(
            "Earned by completing lessons in Invest > Learn — every lesson gives exp toward this. Every 3 months, rank above Warrior decays back by 2 ranks, so this needs the occasional top-up to hold onto.",
            color = CedalColors.TextSecondary, fontSize = 11.sp,
        )
    }
}

@Composable
private fun SecurityGauge(percent: Int) {
    val color = when {
        percent >= 80 -> CedalColors.Success
        percent >= 50 -> CedalColors.AccentSky
        else -> CedalColors.Error
    }
    val animatedPercent by animateFloatAsState(targetValue = percent.toFloat(), label = "securityGauge")
    val diameter = 150.dp

    Box(
        modifier = Modifier
            .size(diameter, diameter / 2 + 20.dp)
            .clipToBounds(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Canvas(modifier = Modifier.size(diameter)) {
            val stroke = 14.dp.toPx()
            val arcSize = Size(size.width - stroke, size.width - stroke)
            val topLeft = Offset(stroke / 2, stroke / 2)
            drawArc(
                color = CedalColors.BorderSlate,
                startAngle = 180f, sweepAngle = 180f, useCenter = false,
                topLeft = topLeft, size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            drawArc(
                color = color,
                startAngle = 180f, sweepAngle = 180f * (animatedPercent / 100f), useCenter = false,
                topLeft = topLeft, size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 38.dp)) {
            Text("$percent%", color = CedalColors.TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun SecurityChecklistRow(label: String, done: Boolean, subtext: String? = null) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(
            if (done) "✓" else "○",
            color = if (done) CedalColors.Success else CedalColors.TextMuted,
            fontSize = 12.sp, modifier = Modifier.padding(end = 6.dp),
        )
        Text(
            label,
            color = if (done) CedalColors.TextPrimary else CedalColors.TextSecondary,
            fontSize = 12.sp, modifier = Modifier.weight(1f),
        )
        subtext?.let { Text(it, color = CedalColors.TextMuted, fontSize = 11.sp) }
    }
}

@Composable
private fun ProfileField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    editable: Boolean = true,
    onValueChange: (String) -> Unit = {},
    placeholder: String = "",
    hint: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    Column(modifier = modifier) {
        Text(label.uppercase(), color = CedalColors.TextSecondary, fontSize = 11.sp)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(top = 3.dp)
                .clip(RoundedCornerShape(50))
                .background(CedalColors.Background)
                .border(1.dp, if (hint != null) CedalColors.BorderCyan else CedalColors.BorderSlate, RoundedCornerShape(50))
                .padding(horizontal = 10.dp, vertical = 6.dp),
        ) {
            Box(modifier = Modifier.weight(1f)) {
                if (editable) {
                    if (value.isEmpty()) {
                        Text(placeholder, color = CedalColors.TextMuted, fontSize = 12.sp)
                    }
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        singleLine = true,
                        textStyle = TextStyle(color = CedalColors.TextPrimary, fontSize = 12.sp),
                        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                        cursorBrush = SolidColor(CedalColors.AccentCyan),
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    Text(value, color = CedalColors.TextPrimary, fontSize = 12.sp)
                }
            }
            hint?.let { Text(it, color = CedalColors.TextSecondary, fontSize = 11.sp) }
        }
    }
}
