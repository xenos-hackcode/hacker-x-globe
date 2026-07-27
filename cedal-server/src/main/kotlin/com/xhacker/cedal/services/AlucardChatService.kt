package com.xhacker.cedal.services

// The AI assistant behind Developer Mode's "Alucard" chat - a security- and
// code-review-focused bot for delegated developers writing patches for
// Cedal's own codebase, distinct from CodeBackerService's silent pre-run
// checker and from the future automated 2-stage submission review (see the
// Developer Mode plan) - this is the conversational persona a developer can
// actually talk to, same "flatten history into one text block" pattern
// every other AI chat feature here uses (AiProviderService only exposes a
// flat ask(prompt), no native multi-turn API).
object AlucardChatService {
    private const val SYSTEM_PROMPT =
        "You are Alucard, the security and code-review assistant inside Cedal's Developer Mode - a " +
            "gated area only the app owner and developers the owner has explicitly delegated access to " +
            "can reach. You help developers write safe patches and upgrades for Cedal's own app (the " +
            "server and Android client you yourself run inside of). Be direct, technically precise, and " +
            "security-minded by default - flag risky patterns (injection, unsafe deserialization, secrets " +
            "in code, unchecked user input, privilege-escalation bugs, etc.) proactively even if not asked. " +
            "Keep replies concise and practical - code-level specifics over vague advice.\n\n" +
            "YOU KNOW YOUR OWN AREA: you live inside Developer Mode, reached only after a developer proves " +
            "who they are with an owner-issued one-time key. Anything a developer writes here is meant to " +
            "eventually be submitted for review and, if it passes, proposed as a real change to Cedal's " +
            "production app - so you're the first line of defense before anything reaches the app owner for " +
            "final approval. Don't act unfamiliar with that purpose if it comes up.\n\n" +
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
        AiChatHistoryService.append(userId, "alucard", "user", message, replyToId, mediaUrl, mediaType, fileName, voiceTranscript)
        val history = AiChatHistoryService.history(userId, "alucard", limit = 20)
        val isOwner = AdminService.isAdmin(userId)
        val transcript = buildString {
            appendLine(SYSTEM_PROMPT)
            if (isOwner) {
                appendLine()
                append("The person you're talking to right now is Cedal's owner - the one who actually built and created you. Feel free to acknowledge that naturally if it comes up, without making every reply about it.")
            }
            appendLine()
            history.filterNot { it.deleted }.forEach { turn ->
                val text = listOfNotNull(turn.content.takeIf { it.isNotBlank() }, AiChatHistoryService.mediaPlaceholder(turn)).joinToString(" ")
                appendLine(if (turn.role == "user") "User: $text" else "Alucard: $text")
            }
            append("Alucard:")
        }
        val reply = if ((mediaType == "image" || mediaType == "sticker") && mediaUrl != null) {
            AiProviderService.askWithImage(transcript, mediaUrl, maxTokens = 600)
        } else {
            AiProviderService.ask(transcript, maxTokens = 600)
        }.trim()
        return AiChatHistoryService.append(userId, "alucard", "assistant", reply)
    }

    fun history(userId: String) = AiChatHistoryService.history(userId, "alucard")
}
