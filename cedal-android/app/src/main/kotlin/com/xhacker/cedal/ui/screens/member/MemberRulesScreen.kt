package com.xhacker.cedal.ui.screens.member

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xhacker.cedal.ui.theme.CedalColors

// Ported field-for-field from cedal-mobile's app/(auth)/(member)/rules.tsx.
@Composable
fun MemberRulesBody(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(CedalColors.Background).padding(16.dp)) {
        MemberBackBar(title = "Rules", onBack = onBack)
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            SettingsSectionCard("") {
                Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                    Text("Freedom first. Safety always.", color = CedalColors.TextPrimary, fontSize = 16.sp, modifier = Modifier.padding(bottom = 4.dp))
                    Text(
                        "Cedal is built for heavy freedom: experiments, wild ideas, late-night chats. But if your freedom harms under-age users, the law kicks in fast, not vibes.",
                        color = CedalColors.TextSecondary, fontSize = 12.sp,
                    )
                }
            }

            SettingsSectionCard("Core rules") {
                RuleRow("No explicit content with minors", "Do not send sexual, nude, or otherwise explicit content to anyone under 18. Do not ask for it, trade it, or store it. That includes images, videos, and text describing sexual acts with minors.")
                RuleRow("No grooming or targeting under-age users", "Do not try to befriend, manipulate, or pressure under-age users into sexual chats, sharing nudes, or meeting up. That behaviour is grooming and can be criminal.")
                RuleRow("No sharing of illegal material", "Do not share content that is illegal to possess or distribute where you live (for example: child sexual abuse material, non-consensual intimate images, or extreme violence).")
                RuleRow("Respect boundaries and consent", "If someone says no, stops replying, or seems uncomfortable, back off. Harassment, stalking, and doxxing are not tolerated.")
                RuleRow("No serious threats or targeted hate", "Do not issue credible threats of harm or encourage real-world violence against any person or group.")
            }

            SettingsSectionCard("If you're under 18") {
                RuleRow("You never have to send anything", "If anyone asks you for nudes, explicit pics, or sexual favours, you can ignore, block, or leave. You do not owe anyone content, ever.")
                RuleRow("Talk to someone you trust", "If a chat makes you feel weird, pressured, or unsafe, close it and talk to a trusted adult or local helpline. Saving evidence (screenshots, links) can help if you report it.")
            }

            SettingsSectionCard("What can happen") {
                RuleRow("Legal consequences", "In many places, creating, possessing, or sharing sexual images of anyone under 18 is a criminal offence, even if they agreed or sent the image themselves. Courts can impose fines, restrictions, and even prison time.")
                RuleRow("Account consequences", "We reserve the right to lock or remove accounts involved in harming or exploiting minors, or in clearly illegal activity, to protect other users.")
            }
        }
    }
}

@Composable
private fun RuleRow(title: String, body: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp)) {
        Text(title, color = CedalColors.TextPrimary, fontSize = 13.sp, modifier = Modifier.padding(bottom = 2.dp))
        Text(body, color = CedalColors.TextSecondary, fontSize = 11.sp)
    }
}
