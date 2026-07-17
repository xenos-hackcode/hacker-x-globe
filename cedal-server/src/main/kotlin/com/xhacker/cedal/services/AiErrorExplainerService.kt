package com.xhacker.cedal.services

// Powers View's on-demand "Explain this" button — never called automatically,
// only when the user actually taps it, since every call costs real money.
// Provider fallback lives in AiProviderService, shared with CodeBackerService.
object AiErrorExplainerService {
    suspend fun explain(language: String, errorText: String): String =
        AiProviderService.ask(buildPrompt(language, errorText))

    private fun buildPrompt(language: String, errorText: String): String =
        "You're explaining a programming error to a total beginner who is just learning to code. " +
            "They ran some $language code and got this raw output/error:\n\n$errorText\n\n" +
            "In 2-4 short sentences, explain in plain, simple English what most likely went wrong and what " +
            "they should try to fix it. Avoid jargon unless you explain it in the same sentence. Don't just " +
            "repeat the raw error text back to them. Reply in plain prose only — no markdown, no headers, " +
            "no bold/italic asterisks, no bullet points; this gets shown as-is in a plain text box."
}
