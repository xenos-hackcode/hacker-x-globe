package com.xhacker.cedal.services

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

// Shared provider-fallback plumbing behind every AI feature in this app
// (View's "Explain this", Pad's Backer pre-flight check, anything else that
// needs a plain prompt -> plain text reply). Reads API keys via DotEnv (real
// env vars first, falling back to the repo's root .env). Tries providers in
// order and silently falls through to the next one if a key is missing or
// the call fails (quota exhausted, invalid key, outage, deprecated model).
object AiProviderService {
    val jsonParser = Json { ignoreUnknownKeys = true }
    private val client = HttpClient(CIO) {
        install(HttpTimeout) { requestTimeoutMillis = 25_000 }
    }

    suspend fun ask(prompt: String, maxTokens: Int = 300): String {
        tryAnthropic(prompt, maxTokens)?.let { return it }
        tryOpenAiCompatible(prompt, envKey = "OPENAI_API_KEY", baseUrl = "https://api.openai.com/v1/chat/completions", model = "gpt-4o-mini")?.let { return it }
        tryOpenAiCompatible(prompt, envKey = "GROQ_API_KEY", baseUrl = "https://api.groq.com/openai/v1/chat/completions", model = "openai/gpt-oss-120b")?.let { return it }
        tryOpenAiCompatible(prompt, envKey = "OPENROUTER_API_KEY", baseUrl = "https://openrouter.ai/api/v1/chat/completions", model = "openai/gpt-4o-mini")?.let { return it }
        throw AuthException("No AI provider is available right now — none of the configured keys worked.")
    }

    // Same fallback chain as ask(), but attaches a real image (by public
    // URL - ImageUploadService already returns publicly-readable URLs, so
    // no base64 round-trip needed) as actual vision input, not just text
    // describing that one exists. Only ever called with the just-sent
    // turn's image, never older history (see CornealChatService/ArcChatService/
    // AiChangeRequestService.reply()/process() - old turns collapse to a
    // text placeholder instead). Groq's chat-completions models here aren't
    // vision-capable, so that provider is skipped for this path.
    suspend fun askWithImage(prompt: String, imageUrl: String, maxTokens: Int = 500): String {
        tryAnthropicWithImage(prompt, imageUrl, maxTokens)?.let { return it }
        tryOpenAiCompatibleWithImage(prompt, imageUrl, envKey = "OPENAI_API_KEY", baseUrl = "https://api.openai.com/v1/chat/completions", model = "gpt-4o-mini")?.let { return it }
        tryOpenAiCompatibleWithImage(prompt, imageUrl, envKey = "OPENROUTER_API_KEY", baseUrl = "https://openrouter.ai/api/v1/chat/completions", model = "openai/gpt-4o-mini")?.let { return it }
        throw AuthException("No AI provider is available right now — none of the configured keys worked.")
    }

    private suspend fun tryAnthropic(prompt: String, maxTokens: Int): String? {
        val key = DotEnv.get("ANTHROPIC_API_KEY") ?: return null
        return try {
            val bodyText = client.post("https://api.anthropic.com/v1/messages") {
                header("x-api-key", key)
                header("anthropic-version", "2023-06-01")
                contentType(ContentType.Application.Json)
                setBody(
                    jsonParser.encodeToString(
                        AnthropicRequest(
                            model = "claude-haiku-4-5-20251001",
                            maxTokens = maxTokens,
                            messages = listOf(ChatMessage(role = "user", content = prompt)),
                        ),
                    ),
                )
            }.body<String>()
            jsonParser.decodeFromString<AnthropicResponse>(bodyText).content.firstOrNull()?.text?.trim()?.takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun tryOpenAiCompatible(prompt: String, envKey: String, baseUrl: String, model: String): String? {
        val key = DotEnv.get(envKey) ?: return null
        return try {
            val bodyText = client.post(baseUrl) {
                header("Authorization", "Bearer $key")
                contentType(ContentType.Application.Json)
                setBody(
                    jsonParser.encodeToString(
                        OpenAiRequest(model = model, messages = listOf(ChatMessage(role = "user", content = prompt))),
                    ),
                )
            }.body<String>()
            jsonParser.decodeFromString<OpenAiResponse>(bodyText).choices.firstOrNull()?.message?.content?.trim()?.takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun tryAnthropicWithImage(prompt: String, imageUrl: String, maxTokens: Int): String? {
        val key = DotEnv.get("ANTHROPIC_API_KEY") ?: return null
        return try {
            val body = buildJsonObject {
                put("model", "claude-haiku-4-5-20251001")
                put("max_tokens", maxTokens)
                putJsonArray("messages") {
                    addJsonObject {
                        put("role", "user")
                        putJsonArray("content") {
                            addJsonObject {
                                put("type", "image")
                                putJsonObject("source") {
                                    put("type", "url")
                                    put("url", imageUrl)
                                }
                            }
                            addJsonObject {
                                put("type", "text")
                                put("text", prompt)
                            }
                        }
                    }
                }
            }
            val bodyText = client.post("https://api.anthropic.com/v1/messages") {
                header("x-api-key", key)
                header("anthropic-version", "2023-06-01")
                contentType(ContentType.Application.Json)
                setBody(body.toString())
            }.body<String>()
            jsonParser.decodeFromString<AnthropicResponse>(bodyText).content.firstOrNull()?.text?.trim()?.takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun tryOpenAiCompatibleWithImage(prompt: String, imageUrl: String, envKey: String, baseUrl: String, model: String): String? {
        val key = DotEnv.get(envKey) ?: return null
        return try {
            val body = buildJsonObject {
                put("model", model)
                putJsonArray("messages") {
                    addJsonObject {
                        put("role", "user")
                        putJsonArray("content") {
                            addJsonObject {
                                put("type", "text")
                                put("text", prompt)
                            }
                            addJsonObject {
                                put("type", "image_url")
                                putJsonObject("image_url") {
                                    put("url", imageUrl)
                                }
                            }
                        }
                    }
                }
            }
            val bodyText = client.post(baseUrl) {
                header("Authorization", "Bearer $key")
                contentType(ContentType.Application.Json)
                setBody(body.toString())
            }.body<String>()
            jsonParser.decodeFromString<OpenAiResponse>(bodyText).choices.firstOrNull()?.message?.content?.trim()?.takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            null
        }
    }
}

@Serializable
private data class ChatMessage(val role: String, val content: String)

@Serializable
private data class AnthropicRequest(
    val model: String,
    @SerialName("max_tokens") val maxTokens: Int,
    val messages: List<ChatMessage>,
)

@Serializable
private data class AnthropicContentBlock(val type: String = "text", val text: String = "")

@Serializable
private data class AnthropicResponse(val content: List<AnthropicContentBlock> = emptyList())

@Serializable
private data class OpenAiRequest(val model: String, val messages: List<ChatMessage>)

@Serializable
private data class OpenAiChoice(val message: ChatMessage)

@Serializable
private data class OpenAiResponse(val choices: List<OpenAiChoice> = emptyList())
