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
            "short paragraphs at most)."

    suspend fun reply(
        userId: String,
        message: String,
        replyToId: String? = null,
        mediaUrl: String? = null,
        mediaType: String? = null,
        fileName: String? = null,
    ): AiChatHistoryService.Turn {
        AiChatHistoryService.append(userId, "arc", "user", message, replyToId, mediaUrl, mediaType, fileName)
        val history = AiChatHistoryService.history(userId, "arc", limit = 20)
        val crossReference = AiChatHistoryService.crossReferenceBlock(userId, excluding = "arc")
        val isAdmin = AdminService.isAdmin(userId)
        val transcript = buildString {
            appendLine(SYSTEM_PROMPT)
            if (isAdmin) {
                appendLine()
                append("The person you're talking to right now is Cedal's admin - the one who actually built and created you. Feel free to acknowledge that naturally if it comes up, without making every reply about it.")
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
        val reply = if (mediaType == "image" && mediaUrl != null) {
            AiProviderService.askWithImage(transcript, mediaUrl, maxTokens = 600)
        } else {
            AiProviderService.ask(transcript, maxTokens = 600)
        }.trim()
        return AiChatHistoryService.append(userId, "arc", "assistant", reply)
    }

    fun history(userId: String) = AiChatHistoryService.history(userId, "arc")
}
