package com.xhacker.cedal.ui.screens.member

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.xhacker.cedal.data.BotCreate
import com.xhacker.cedal.data.BotResponse
import com.xhacker.cedal.data.BotTurnDto
import com.xhacker.cedal.data.BotUpdate
import com.xhacker.cedal.ui.theme.CedalColors
import com.xhacker.cedal.ui.theme.CedalErrorText
import com.xhacker.cedal.ui.theme.CedalPrimaryButton
import com.xhacker.cedal.ui.theme.CedalTextField
import com.xhacker.cedal.viewmodel.AuthViewModel
import kotlinx.coroutines.launch
import java.io.File

private fun platformLabel(botType: String) = when (botType) {
    "telegram" -> "Telegram"
    "whatsapp" -> "WhatsApp"
    "inapp" -> "In-App"
    else -> "Telegram + WhatsApp"
}

// Member > More > Bots list — the character sheets built with
// MemberBotEditBody below. Round 1 of the "Leo" bot-builder platform: just
// real CRUD for the form cedal-mobile's bots.tsx originally shipped as a
// stub (see MemberBotEditBody's own doc comment). Leo itself, the
// converse/brain endpoint, and code generation are later rounds - see
// planner/left-to-do.md.
@Composable
fun MemberBotsListBody(onBack: () -> Unit, onOpenBot: (botId: String) -> Unit, onCreateBot: () -> Unit, viewModel: AuthViewModel = hiltViewModel()) {
    var bots by remember { mutableStateOf<List<BotResponse>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        viewModel.listBots().onSuccess { bots = it }
        loading = false
    }

    Column(modifier = Modifier.fillMaxSize().background(CedalColors.Background)) {
        MemberBackBar(title = "Bots", onBack = onBack)

        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
            CedalPrimaryButton(text = "+ NEW BOT", onClick = onCreateBot)
        }

        if (loading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = CedalColors.AccentCyan)
            }
        } else if (bots.isEmpty()) {
            Text(
                "Nothing here yet. Build a character sheet and Leo will turn it into a working bot in a later update.",
                color = CedalColors.TextMuted, fontSize = 12.sp, modifier = Modifier.padding(20.dp),
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(top = 8.dp)) {
                items(bots, key = { it.id }) { bot ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onOpenBot(bot.id) }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                    ) {
                        Box(
                            modifier = Modifier.size(44.dp).clip(CircleShape).background(CedalColors.BackgroundBlob).border(1.dp, CedalColors.BorderCyan, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            val iconUrl = bot.iconUrl
                            if (iconUrl != null) {
                                AsyncImage(model = iconUrl, contentDescription = bot.name, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(CircleShape))
                            } else {
                                Text(bot.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?", color = CedalColors.TextPrimary, fontSize = 16.sp)
                            }
                        }
                        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                            Text(bot.name, color = CedalColors.TextPrimary, fontSize = 14.sp)
                            Text(platformLabel(bot.botType), color = CedalColors.TextMuted, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

// The create/edit form — this IS cedal-mobile's bots.tsx character sheet
// (ported as-is: name/age/gender/character/personality/bio/occupation/
// life story/description), now with a platform picker + credential fields
// and a real save. Not ported: the draggable "Leo" AI assistant bubble/panel
// that helps fill the form - that's a distinct AI-chat feature, its own
// later round (see planner/left-to-do.md's Round 2/3).
@Composable
fun MemberBotEditBody(botId: String?, onBack: () -> Unit, onTestChat: (botId: String) -> Unit = {}, viewModel: AuthViewModel = hiltViewModel()) {
    var name by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var character by remember { mutableStateOf("") }
    var personality by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var occupation by remember { mutableStateOf("") }
    var lifeStory by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var iconUrl by remember { mutableStateOf<String?>(null) }
    var botType by remember { mutableStateOf("inapp") }
    var telegramToken by remember { mutableStateOf("") }
    var whatsappPhoneNumberId by remember { mutableStateOf("") }
    var whatsappAccessToken by remember { mutableStateOf("") }
    var userApiKey by remember { mutableStateOf("") }
    var hasTelegramToken by remember { mutableStateOf(false) }
    var hasWhatsappCredentials by remember { mutableStateOf(false) }
    var hasUserApiKey by remember { mutableStateOf(false) }
    var hostingMode by remember { mutableStateOf("self") }
    var isPremium by remember { mutableStateOf(false) }
    var isAdmin by remember { mutableStateOf(false) }
    var settingPremium by remember { mutableStateOf(false) }
    var downloading by remember { mutableStateOf(false) }

    var loading by remember { mutableStateOf(botId != null) }
    var saving by remember { mutableStateOf(false) }
    var uploadingIcon by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var confirmingDelete by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.getProfile().onSuccess { profile ->
            isAdmin = profile.email?.equals("hackerxenos06@gmail.com", ignoreCase = true) == true
        }
    }

    LaunchedEffect(botId) {
        if (botId == null) return@LaunchedEffect
        viewModel.getBot(botId).onSuccess { bot ->
            name = bot.name
            age = bot.age?.toString() ?: ""
            gender = bot.gender ?: ""
            character = bot.character
            personality = bot.personality
            bio = bot.bio
            occupation = bot.occupation ?: ""
            lifeStory = bot.lifeStory ?: ""
            description = bot.description
            iconUrl = bot.iconUrl
            botType = bot.botType
            hasTelegramToken = bot.hasTelegramToken
            hasWhatsappCredentials = bot.hasWhatsappCredentials
            hasUserApiKey = bot.hasUserApiKey
            hostingMode = bot.hostingMode
            isPremium = bot.isPremium
        }.onFailure { error = it.message }
        loading = false
    }

    val iconPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        uploadingIcon = true
        scope.launch {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
            if (bytes != null) {
                viewModel.uploadImage("bot_icon", bytes, mimeType)
                    .onSuccess { iconUrl = it }
                    .onFailure { error = it.message }
            }
            uploadingIcon = false
        }
    }

    fun save() {
        if (name.isBlank()) { error = "Give your AI a name first."; return }
        if (character.isBlank() || personality.isBlank() || bio.isBlank() || description.isBlank()) {
            error = "Character, Personality, Bio, and Description are required."
            return
        }
        if ((botType == "telegram" || botType == "both") && telegramToken.isBlank() && !hasTelegramToken) {
            error = "Add a Telegram bot token (from @BotFather) first."
            return
        }
        if ((botType == "whatsapp" || botType == "both") && (whatsappPhoneNumberId.isBlank() || whatsappAccessToken.isBlank()) && !hasWhatsappCredentials) {
            error = "Add your WhatsApp Cloud API Phone Number ID and access token first."
            return
        }
        if (hostingMode == "cedal" && !isPremium) {
            error = "This bot isn't premium yet - ask the admin to enable cedal hosting first."
            return
        }
        error = null
        saving = true
        scope.launch {
            val result = if (botId == null) {
                viewModel.createBot(
                    BotCreate(
                        name = name, age = age.toIntOrNull(), gender = gender.ifBlank { null },
                        character = character, personality = personality, bio = bio,
                        occupation = occupation.ifBlank { null }, lifeStory = lifeStory.ifBlank { null },
                        description = description, iconUrl = iconUrl, botType = botType,
                        telegramToken = telegramToken.ifBlank { null },
                        whatsappPhoneNumberId = whatsappPhoneNumberId.ifBlank { null },
                        whatsappAccessToken = whatsappAccessToken.ifBlank { null },
                        userApiKey = userApiKey.ifBlank { null },
                        hostingMode = hostingMode,
                    ),
                )
            } else {
                viewModel.updateBot(
                    botId,
                    BotUpdate(
                        name = name, age = age.toIntOrNull(), gender = gender.ifBlank { null },
                        character = character, personality = personality, bio = bio,
                        occupation = occupation.ifBlank { null }, lifeStory = lifeStory.ifBlank { null },
                        description = description, iconUrl = iconUrl, botType = botType,
                        telegramToken = telegramToken.ifBlank { null },
                        whatsappPhoneNumberId = whatsappPhoneNumberId.ifBlank { null },
                        whatsappAccessToken = whatsappAccessToken.ifBlank { null },
                        userApiKey = userApiKey.ifBlank { null },
                        hostingMode = hostingMode,
                    ),
                )
            }
            saving = false
            result.onSuccess { onBack() }.onFailure { error = it.message }
        }
    }

    fun delete() {
        val id = botId ?: return
        if (!confirmingDelete) { confirmingDelete = true; return }
        saving = true
        scope.launch {
            viewModel.deleteBot(id).onSuccess { onBack() }.onFailure { error = it.message; saving = false }
        }
    }

    fun setPremium(value: Boolean) {
        val id = botId ?: return
        settingPremium = true
        scope.launch {
            viewModel.setBotPremium(id, value)
                .onSuccess { isPremium = it.isPremium }
                .onFailure { error = it.message }
            settingPremium = false
        }
    }

    // Self-hosted download - fetches the zip via the existing authenticated
    // Retrofit client (needs the Bearer header, unlike the plain-URL App
    // Update APK download in MemberCodeScreen.kt's downloadAndInstallApk),
    // writes it to cacheDir, then hands it off via the same FileProvider +
    // ACTION_SEND share pattern MemberCodeScreen.kt already uses for
    // sharing a code file.
    fun downloadZip() {
        val id = botId ?: return
        downloading = true
        scope.launch {
            viewModel.downloadBot(id).onSuccess { body ->
                try {
                    val dir = File(context.cacheDir, "bot_downloads").apply { mkdirs() }
                    val file = File(dir, "${name.ifBlank { "bot" }}_bot.zip")
                    body.byteStream().use { input -> file.outputStream().use { output -> input.copyTo(output) } }
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/zip"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(intent, "Save/share bot code"))
                } catch (e: Exception) {
                    error = e.message ?: "Couldn't save that download"
                }
            }.onFailure { error = it.message }
            downloading = false
        }
    }

    if (loading) {
        Box(modifier = Modifier.fillMaxSize().background(CedalColors.Background), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = CedalColors.AccentCyan)
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize().background(CedalColors.Background).padding(16.dp).imePadding()) {
        MemberBackBar(title = if (botId == null) "New Bot" else "Edit Bot", busy = saving, onBack = ::save)
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {

            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(CedalColors.BackgroundBlob)
                        .border(2.dp, CedalColors.BorderCyan, CircleShape)
                        .clickable { iconPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    contentAlignment = Alignment.Center,
                ) {
                    val url = iconUrl
                    if (uploadingIcon) {
                        CircularProgressIndicator(color = CedalColors.AccentCyan, modifier = Modifier.size(24.dp))
                    } else if (url != null) {
                        AsyncImage(model = url, contentDescription = "Bot icon", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(CircleShape))
                    } else {
                        Text(name.firstOrNull()?.uppercaseChar()?.toString() ?: "?", color = CedalColors.TextPrimary, fontSize = 22.sp)
                    }
                }
                Text("Tap to add a picture", color = CedalColors.TextMuted, fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp))
            }

            SettingsSectionCard("") {
                Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                    Text("Build your own AI", color = CedalColors.TextPrimary, fontSize = 16.sp, modifier = Modifier.padding(bottom = 4.dp))
                    Text(
                        "Fill this out like you're writing a character sheet. The more human the details, the easier it is for the AI to stay in-role.",
                        color = CedalColors.TextSecondary, fontSize = 12.sp,
                    )
                }
            }

            SettingsSectionCard("Character sheet") {
                BotField("Name", "REQUIRED", "Nova, Corneal, Ghost, etc.", name) { name = it }
                BotField("Age", "OPTIONAL", "22, timeless, ageless entity…", age) { age = it }
                BotField("Gender", "OPTIONAL", "Female, male, non-binary, AI, none…", gender) { gender = it }
                BotField("Character", "REQUIRED", "Role in the story: mentor, chaos hacker, soft therapist…", character) { character = it }
                BotField("Personality", "REQUIRED", "How they talk, react, and vibe. Calm, chaotic, playful, ruthless…", personality) { personality = it }
                BotField("Bio", "REQUIRED", "Short bio you'd see on their profile.", bio) { bio = it }
                BotField("Occupation", "OPTIONAL", "What they 'do' in the world: engineer, mercenary, archivist…", occupation) { occupation = it }
                BotField("Life story", "OPTIONAL", "Key events, scars, victories, how they ended up here.", lifeStory) { lifeStory = it }
                BotField("Description", "REQUIRED", "Visual + energy description like you'd brief an artist.", description) { description = it }
            }

            SettingsSectionCard("Platform") {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                        PlatformChip("In-App", selected = botType == "inapp") { botType = "inapp" }
                        PlatformChip("Telegram", selected = botType == "telegram") { botType = "telegram" }
                        PlatformChip("WhatsApp", selected = botType == "whatsapp") { botType = "whatsapp" }
                        PlatformChip("Both", selected = botType == "both") { botType = "both" }
                    }
                    if (botType == "inapp") {
                        Text(
                            "No setup needed — this bot lives right here in Cedal. Save it, then hit CHAT below.",
                            color = CedalColors.TextMuted, fontSize = 10.sp, modifier = Modifier.padding(top = 10.dp),
                        )
                    }
                    if (botType == "telegram" || botType == "both") {
                        Text(
                            if (hasTelegramToken) "Telegram token on file — leave blank to keep it." else "Get a token from @BotFather on Telegram.",
                            color = CedalColors.TextMuted, fontSize = 10.sp, modifier = Modifier.padding(top = 10.dp),
                        )
                        CedalTextField(value = telegramToken, onValueChange = { telegramToken = it }, prefix = "›", placeholder = "Telegram bot token", modifier = Modifier.padding(top = 4.dp))
                    }
                    if (botType == "whatsapp" || botType == "both") {
                        Text(
                            if (hasWhatsappCredentials) "WhatsApp credentials on file — leave blank to keep them." else "From your Meta developer app's WhatsApp Cloud API.",
                            color = CedalColors.TextMuted, fontSize = 10.sp, modifier = Modifier.padding(top = 10.dp),
                        )
                        CedalTextField(value = whatsappPhoneNumberId, onValueChange = { whatsappPhoneNumberId = it }, prefix = "›", placeholder = "Phone Number ID", modifier = Modifier.padding(top = 4.dp))
                        CedalTextField(value = whatsappAccessToken, onValueChange = { whatsappAccessToken = it }, prefix = "›", placeholder = "Access token", modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }

            SettingsSectionCard("Your own AI key (optional)") {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp)) {
                    Text(
                        if (hasUserApiKey) "AI key on file — leave blank to keep it. Replies route through your own key, not Cedal's shared free tier." else "Bring your own Anthropic API key (console.anthropic.com) to skip the 1000-token free-tier cap entirely.",
                        color = CedalColors.TextMuted, fontSize = 10.sp,
                    )
                    CedalTextField(value = userApiKey, onValueChange = { userApiKey = it }, prefix = "›", placeholder = "sk-ant-…", modifier = Modifier.padding(top = 4.dp))
                }
            }

            if (botId != null && botType != "inapp") {
                SettingsSectionCard("Hosting") {
                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                            PlatformChip("Self-hosted (free)", selected = hostingMode == "self") { hostingMode = "self" }
                            PlatformChip("Cedal-hosted (premium)", selected = hostingMode == "cedal") { hostingMode = "cedal" }
                        }
                        if (hostingMode == "cedal" && !isPremium) {
                            Text(
                                "Premium required — ask the admin to enable cedal hosting for this bot.",
                                color = CedalColors.AccentSky, fontSize = 10.sp, modifier = Modifier.padding(top = 10.dp),
                            )
                        }
                        if (hostingMode == "self") {
                            Text(
                                "Free — download the bot code and run it yourself. No setup on Cedal's end.",
                                color = CedalColors.TextMuted, fontSize = 10.sp, modifier = Modifier.padding(top = 10.dp),
                            )
                            CedalPrimaryButton(text = "DOWNLOAD BOT CODE", modifier = Modifier.padding(top = 8.dp), loading = downloading, onClick = ::downloadZip)
                        }
                        if (hostingMode == "cedal" && isPremium) {
                            Text(
                                "Cedal runs this bot for you — nothing to host yourself.",
                                color = CedalColors.TextMuted, fontSize = 10.sp, modifier = Modifier.padding(top = 10.dp),
                            )
                            if (botType == "whatsapp" || botType == "both") {
                                WhatsappWebhookInfo(context)
                            }
                        }
                        if (isAdmin) {
                            CedalPrimaryButton(
                                text = if (isPremium) "ADMIN: REMOVE PREMIUM" else "ADMIN: MARK PREMIUM",
                                modifier = Modifier.padding(top = 10.dp), loading = settingPremium,
                                onClick = { setPremium(!isPremium) },
                            )
                        }
                    }
                }
            }

            CedalErrorText(error)
            CedalPrimaryButton(text = if (botId == null) "SAVE AI" else "SAVE CHANGES", modifier = Modifier.padding(top = 4.dp), loading = saving, onClick = ::save)

            if (botId != null) {
                // Round 2 - lets you actually talk to the bot's brain (the
                // /test-chat endpoint) without waiting on Round 3's
                // self-hosted generated code or a real Telegram/WhatsApp
                // connection.
                CedalPrimaryButton(text = if (botType == "inapp") "CHAT" else "TEST CHAT", modifier = Modifier.padding(top = 8.dp), onClick = { onTestChat(botId) })
            }

            if (botId != null) {
                CedalPrimaryButton(
                    text = if (confirmingDelete) "TAP AGAIN TO DELETE" else "DELETE BOT",
                    modifier = Modifier.padding(top = 8.dp),
                    onClick = ::delete,
                )
            }
        }
    }
}

// Round 2 - a plain chat with the bot's brain (BotBrainService server-
// side), owner-only, keyed by the owner's own userId as chatId so this
// never collides with a real external conversation once Round 3 exists.
// Deliberately minimal (no media/reactions/etc.) - this is a test harness
// for the persona, not a second full chat feature.
@Composable
fun MemberBotTestChatBody(botId: String, onBack: () -> Unit, viewModel: AuthViewModel = hiltViewModel()) {
    var botName by remember { mutableStateOf("Bot") }
    var turns by remember { mutableStateOf<List<BotTurnDto>>(emptyList()) }
    var input by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    LaunchedEffect(botId) {
        viewModel.getBot(botId).onSuccess { botName = it.name }
        viewModel.getBotTestChatHistory(botId).onSuccess { turns = it }
    }

    fun send() {
        val text = input.trim()
        if (text.isBlank() || sending) return
        input = ""
        error = null
        turns = turns + BotTurnDto("user", text)
        sending = true
        scope.launch {
            viewModel.sendBotTestChatMessage(botId, text)
                .onSuccess { turns = turns + BotTurnDto("assistant", it.reply) }
                .onFailure { error = it.message }
            sending = false
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(CedalColors.Background).imePadding()) {
        MemberBackBar(title = "Test Chat: $botName", onBack = onBack)
        Column(modifier = Modifier.weight(1f).verticalScroll(scrollState).padding(horizontal = 16.dp, vertical = 8.dp)) {
            if (turns.isEmpty()) {
                Text(
                    "Say hi to try out $botName's persona. This talks straight to the brain endpoint - no Telegram/WhatsApp connection needed.",
                    color = CedalColors.TextMuted, fontSize = 12.sp, modifier = Modifier.padding(vertical = 12.dp),
                )
            }
            turns.forEach { turn ->
                val isUser = turn.role == "user"
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isUser) CedalColors.AccentCyan else CedalColors.CardBackground)
                            .border(1.dp, if (isUser) CedalColors.AccentCyan else CedalColors.BorderSlate, RoundedCornerShape(14.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    ) {
                        Text(turn.content, color = if (isUser) CedalColors.Background else CedalColors.TextPrimary, fontSize = 13.sp)
                    }
                }
            }
            if (sending) {
                Box(modifier = Modifier.padding(top = 6.dp)) { CircularProgressIndicator(color = CedalColors.AccentCyan, strokeWidth = 2.dp, modifier = Modifier.size(16.dp)) }
            }
        }
        CedalErrorText(error)
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.weight(1f)) {
                CedalTextField(value = input, onValueChange = { input = it }, prefix = "›", placeholder = "Message $botName…", modifier = Modifier.fillMaxWidth())
            }
            Text(
                "SEND", color = CedalColors.AccentCyan, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .padding(start = 10.dp)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { send() }
                    .padding(8.dp),
            )
        }
    }
}

// WhatsApp's Cloud API has exactly one webhook URL per Meta App (not
// per-bot like Telegram's setWebhook - see BotRoutes.kt server-side), so
// there's nothing to fetch per bot here: this is the one shared URL +
// verify token every cedal-hosted WhatsApp bot uses, set once in the
// user's own Meta app dashboard. WHATSAPP_WEBHOOK_VERIFY_TOKEN must match
// the server's own env var of the same name.
private const val WHATSAPP_WEBHOOK_VERIFY_TOKEN = "b0a287c3b6c244849e86d2431c5b5918"

@Composable
private fun WhatsappWebhookInfo(context: android.content.Context) {
    val webhookUrl = "https://cedal-server-717899371194.us-central1.run.app/webhooks/whatsapp"
    fun copy(label: String, value: String) {
        val clipboard = context.getSystemService(ClipboardManager::class.java)
        clipboard?.setPrimaryClip(ClipData.newPlainText(label, value))
    }
    Column(modifier = Modifier.padding(top = 10.dp)) {
        Text("Paste into your Meta app's WhatsApp webhook config (once):", color = CedalColors.TextMuted, fontSize = 10.sp)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
            Text(webhookUrl, color = CedalColors.TextSecondary, fontSize = 11.sp, modifier = Modifier.weight(1f))
            Text("COPY", color = CedalColors.AccentCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { copy("Webhook URL", webhookUrl) }.padding(start = 8.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
            Text("Verify token: $WHATSAPP_WEBHOOK_VERIFY_TOKEN", color = CedalColors.TextSecondary, fontSize = 11.sp, modifier = Modifier.weight(1f))
            Text("COPY", color = CedalColors.AccentCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { copy("Verify token", WHATSAPP_WEBHOOK_VERIFY_TOKEN) }.padding(start = 8.dp))
        }
    }
}

@Composable
private fun PlatformChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .padding(end = 8.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) CedalColors.AccentCyan else CedalColors.CardBackground)
            .border(1.dp, if (selected) CedalColors.AccentCyan else CedalColors.BorderSlate, RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(label, color = if (selected) CedalColors.Background else CedalColors.TextSecondary, fontSize = 12.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
private fun BotField(label: String, badge: String, placeholder: String, value: String, onValueChange: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp)) {
        Row {
            Text(label, color = CedalColors.TextPrimary, fontSize = 12.sp)
            Text("  $badge", color = if (badge == "REQUIRED") CedalColors.AccentSky else CedalColors.TextMuted, fontSize = 10.sp)
        }
        CedalTextField(value = value, onValueChange = onValueChange, prefix = "›", placeholder = placeholder, modifier = Modifier.padding(top = 4.dp))
    }
}
