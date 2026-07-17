package com.xhacker.cedal.services

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString

// Pad's "Backer" - an automatic pre-flight check that runs before every Run,
// across every language (not the same thing as Kotlin's own structural gate
// - see AndroidBuildService/RankService for that one, which is a free,
// deterministic regex check, not an AI call). Looks for likely bugs an AI
// can catch before wasting a real build/run on them. Only ever applies a fix
// with the user's explicit tap in the client - never silently rewrites
// their code itself.
object CodeBackerService {
    @Serializable
    data class BackerResult(
        val hasIssue: Boolean,
        val reason: String = "",
        val fixedCode: String = "",
    )

    suspend fun review(language: String, code: String): BackerResult {
        val raw = AiProviderService.ask(buildPrompt(language, code), maxTokens = 2000)
        // The prompt asks for bare JSON, but models sometimes wrap it in a
        // markdown code fence anyway - strip that defensively before parsing.
        val cleaned = raw.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        return try {
            AiProviderService.jsonParser.decodeFromString<BackerResult>(cleaned)
        } catch (e: Exception) {
            // A parsing hiccup shouldn't block every single Run - treat it
            // as "no issue found" and let a real bug surface normally via
            // the actual build/run failing, same as if Backer weren't here.
            BackerResult(hasIssue = false)
        }
    }

    private fun buildPrompt(language: String, code: String): String =
        "You're reviewing $language code for likely bugs BEFORE it gets compiled or run - things like " +
            "syntax errors, missing imports/declarations, undefined references, or obvious logic mistakes " +
            "that would make it fail. The person is a beginner.\n\n" +
            "Code:\n$code\n\n" +
            "Reply with ONLY valid JSON, no markdown, no code fences, no extra text before or after, in " +
            "exactly this shape: {\"hasIssue\": true or false, \"reason\": \"one or two short plain-English " +
            "sentences explaining the issue, empty string if none\", \"fixedCode\": \"the complete corrected " +
            "code, empty string if none\"}. Only set hasIssue to true if you're genuinely confident something " +
            "is wrong - don't invent issues in code that looks fine."
}
