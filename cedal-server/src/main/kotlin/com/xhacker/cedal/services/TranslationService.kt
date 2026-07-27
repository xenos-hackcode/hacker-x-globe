package com.xhacker.cedal.services

// Real chat-message translation - each user picks a language (Settings),
// and ChatService auto-translates every message into the RECIPIENT's
// language at send time if it differs from the sender's. Full display
// names (not ISO codes) since the AI translates more reliably given the
// actual language name than a bare "fr"/"yo" code, and it's what the
// client's language picker shows too - one shared source of truth for
// both. This only ever affects chat message *content*; the rest of the
// app's own UI (buttons/menus/screens) stays English - a full app-wide
// localization pass is a separate, much larger effort.
object TranslationService {
    val LANGUAGES = listOf(
        "English", "French", "Yoruba", "German", "Spanish", "Chinese", "Japanese",
        "Portuguese", "Italian", "Russian", "Arabic", "Hindi", "Korean", "Swahili",
        "Igbo", "Hausa", "Dutch", "Turkish", "Vietnamese", "Polish",
    )

    suspend fun translate(text: String, toLanguage: String): String? {
        if (text.isBlank()) return null
        val prompt = "Translate the following chat message into $toLanguage. Reply with ONLY the translated " +
            "text - no quotes, no explanation, no language name, nothing else. Preserve the casual tone " +
            "(this is a casual chat message, not a formal document) and keep any emoji as-is:\n\n$text"
        return try {
            AiProviderService.ask(prompt, maxTokens = 400).trim().takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            null
        }
    }
}
