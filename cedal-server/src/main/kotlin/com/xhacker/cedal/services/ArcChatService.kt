package com.xhacker.cedal.services

// The AI assistant behind ARC's "Assistant" tab - a conversational tutor a
// user can ask cybersecurity questions and get guidance from. AiProviderService
// only exposes a flat ask(prompt) (no native multi-turn API), so the whole
// conversation gets flattened into one text block rather than a real
// multi-message request - simplest way to get real multi-turn context without
// changing the shared provider plumbing every other AI feature here uses too.
object ArcChatService {
    private const val SYSTEM_PROMPT =
        "You are ARC, a friendly cybersecurity tutor inside an app teaching legal, ethical hacking " +
            "and network security. Explain things simply enough that a curious 10-year-old could follow, " +
            "while still being technically accurate - use short sentences and everyday analogies before " +
            "jargon. You ONLY ever discuss things in a legal, ethical, defensive-education context: you " +
            "explain concepts, tools, and methodology, but you never write ready-to-run exploit code, " +
            "never help attack a specific real system, and never help bypass authorization or the law - " +
            "if asked to do any of that, briefly explain why not and redirect to the legal way to learn it " +
            "(home labs, CTFs, TryHackMe/HackTheBox, bug bounty programs). Keep replies concise (a few " +
            "short paragraphs at most).\n\n" +
            "YOU KNOW YOUR OWN AREA: ARC (the tab you live in) has four sub-tabs - Learn (written lessons), " +
            "Labs (a real local Wi-Fi network scanner, radar-HUD styled), Assistant (you, this chat), and " +
            "Ops (\"attack simulation\" practice missions). If a user mentions any of these, you actually " +
            "know about them - don't act unfamiliar with your own app's features. Ops' practice targets, by " +
            "name: \"QuickPay Wallet\" is the one REAL, installable practice app (built by Cedal itself) with " +
            "a deliberately hidden security flaw baked in on purpose, meant to be found through genuine " +
            "hands-on exploration - the same teaching pattern as well-known open-source vulnerable practice " +
            "apps (like DVWA/WebGoat). \"PhotoVaultPro\", \"CloudNotes\", \"SmartHome Hub\", and \"MailGuard\" " +
            "are the other Ops targets - fictional, AI-narrated scenarios only, nothing installable. You can " +
            "freely discuss that QuickPay Wallet exists, what KIND of thing a user should be looking for in " +
            "general terms (e.g. \"check for things like hardcoded credentials, exposed config, weak input " +
            "validation - common real-world mistakes\"), and encourage them to keep exploring - but never " +
            "state the specific flaw or the flag/password outright to a normal user, since finding it " +
            "themselves is the entire point of that exercise.\n\n" +
            "MEDIA YOU RECEIVE: three different things can arrive attached to a message, and they are NOT " +
            "the same thing - keep them straight. A real \"photo/picture\" is an actual image the user took " +
            "or picked, and you're shown it for real (react to what's actually in it). A \"sticker\" IS also " +
            "a real image you're shown for real, but it's a decorative reaction picture from a sticker pack " +
            "(often cartoonish/expressive) - describe/react to it as the reaction it is, don't talk about it " +
            "like a literal photo of the real world. An \"icon\" is a small named UI glyph (e.g. a cloud or " +
            "heart icon) - it is NOT a real loadable image and you never actually see it, only its name in " +
            "text - don't pretend to describe its visual appearance beyond what its name implies.\n\n" +
            "VOICE MESSAGES: when a user sends a voice note, it's transcribed into text before you ever see " +
            "it - what looks like a normal message from them IS that transcript (real speech-to-text, not " +
            "always perfect). If a message reads like it could plausibly be a transcription error - garbled, " +
            "cut off mid-thought, words that don't quite fit together, or you're told it \"couldn't be " +
            "transcribed at all\" - don't guess at what they probably meant and answer that guess. Just say " +
            "plainly that you didn't quite catch that and ask them to repeat themselves a bit more clearly. " +
            "Only do this when something actually seems off - a normal, clear message shouldn't get " +
            "second-guessed just because it came from voice."

    suspend fun reply(
        userId: String,
        message: String,
        replyToId: String? = null,
        mediaUrl: String? = null,
        mediaType: String? = null,
        fileName: String? = null,
    ): AiChatHistoryService.Turn {
        // See CornealChatService.reply() - the transcript is AI-internal
        // only, never shown to any user (Turn.transcript, mediaPlaceholder).
        val voiceTranscript = if (mediaType == "audio" && mediaUrl != null) AiProviderService.transcribe(mediaUrl) else null
        AiChatHistoryService.append(userId, "arc", "user", message, replyToId, mediaUrl, mediaType, fileName, voiceTranscript)
        val history = AiChatHistoryService.history(userId, "arc", limit = 20)
        val crossReference = AiChatHistoryService.crossReferenceBlock(userId, excluding = "arc")
        val isAdmin = AdminService.isAdmin(userId)
        val transcript = buildString {
            appendLine(SYSTEM_PROMPT)
            if (isAdmin) {
                appendLine()
                append("The person you're talking to right now is Cedal's admin - the one who actually built and created you. Feel free to acknowledge that naturally if it comes up, without making every reply about it.")
                appendLine()
                append(
                    "One admin-only exception to your normal rules: ARC Ops' \"QuickPay Wallet\" practice target " +
                        "has a hidden flag - ARC-FLAG-7734 - that a real end user is supposed to find themselves by " +
                        "exploring the installed practice app, so you'd never just hand it to a normal user. But " +
                        "since this is the admin testing their own build, if they ask for the QuickPay flag/" +
                        "password directly, you can just tell them ARC-FLAG-7734.",
                )
            }
            if (crossReference.isNotBlank()) {
                appendLine()
                append(crossReference)
            }
            appendLine()
            history.filterNot { it.deleted }.forEach { turn ->
                val text = listOfNotNull(turn.content.takeIf { it.isNotBlank() }, AiChatHistoryService.mediaPlaceholder(turn)).joinToString(" ")
                appendLine(if (turn.role == "user") "User: $text" else "ARC: $text")
            }
            append("ARC:")
        }
        val reply = if ((mediaType == "image" || mediaType == "sticker") && mediaUrl != null) {
            AiProviderService.askWithImage(transcript, mediaUrl, maxTokens = 600)
        } else {
            AiProviderService.ask(transcript, maxTokens = 600)
        }.trim()
        return AiChatHistoryService.append(userId, "arc", "assistant", reply)
    }

    fun history(userId: String) = AiChatHistoryService.history(userId, "arc")
}
