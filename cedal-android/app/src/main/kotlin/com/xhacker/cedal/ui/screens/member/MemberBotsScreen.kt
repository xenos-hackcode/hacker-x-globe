package com.xhacker.cedal.ui.screens.member

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xhacker.cedal.ui.theme.CedalColors
import com.xhacker.cedal.ui.theme.CedalErrorText
import com.xhacker.cedal.ui.theme.CedalPrimaryButton
import com.xhacker.cedal.ui.theme.CedalTextField

// Ported from cedal-mobile's bots.tsx (the character-sheet form). Not
// ported: the draggable "Leo" AI assistant bubble/panel that helps fill the
// form — that's a distinct AI-chat feature (like Corneal/HelpAssistant),
// its own later milestone, not something this form strictly needs.
// handleSave() there is explicitly a stub (logs + shows "(stub)" alert,
// doesn't persist anywhere) — matched here the same way.
@Composable
fun MemberBotsBody(onBack: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var character by remember { mutableStateOf("") }
    var personality by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var occupation by remember { mutableStateOf("") }
    var lifeStory by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var saved by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().background(CedalColors.Background).padding(16.dp).imePadding()) {
        MemberBackBar(title = "Bots", onBack = onBack)
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {

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
                BotField("Name", "REQUIRED", "Nova, Corneal, Ghost, etc.", name) { name = it; saved = false }
                BotField("Age", "OPTIONAL", "22, timeless, ageless entity…", age) { age = it; saved = false }
                BotField("Gender", "OPTIONAL", "Female, male, non-binary, AI, none…", gender) { gender = it; saved = false }
                BotField("Character", "REQUIRED", "Role in the story: mentor, chaos hacker, soft therapist…", character) { character = it; saved = false }
                BotField("Personality", "REQUIRED", "How they talk, react, and vibe. Calm, chaotic, playful, ruthless…", personality) { personality = it; saved = false }
                BotField("Bio", "REQUIRED", "Short bio you'd see on their profile.", bio) { bio = it; saved = false }
                BotField("Occupation", "OPTIONAL", "What they 'do' in the world: engineer, mercenary, archivist…", occupation) { occupation = it; saved = false }
                BotField("Life story", "OPTIONAL", "Key events, scars, victories, how they ended up here.", lifeStory) { lifeStory = it; saved = false }
                BotField("Description", "REQUIRED", "Visual + energy description like you'd brief an artist.", description) { description = it; saved = false }
            }

            CedalErrorText(error)
            CedalPrimaryButton(
                text = if (saved) "SAVED (STUB)" else "SAVE AI",
                modifier = Modifier.padding(top = 4.dp),
                onClick = {
                    if (name.isBlank()) { error = "Give your AI a name first."; return@CedalPrimaryButton }
                    error = null
                    saved = true // matches cedal-mobile: this is a stub, nothing is actually persisted
                },
            )
        }
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
