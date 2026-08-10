package com.xhacker.cedal.services

import com.xhacker.cedal.db.BotConversationTurns
import com.xhacker.cedal.db.Bots
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.util.UUID

// Round 2 of the Bots/"Leo" platform (2026-08-10) - the brain endpoint
// Round 3's self-hosted generated code will eventually call, reusing
// AiProviderService the same way CornealChatService does. Also reachable
// via a JWT-authenticated owner-only path (see BotRoutes' /test-chat) so a
// bot can actually be tried out in-app before Round 3's code generation
// exists at all - not in the original Round 2 scope, added because there
// was otherwise no way to verify this round actually works.
object BotBrainService {
    private const val FREE_TOKEN_CAP = 1000

    // AiProviderService.ask()'s multi-provider fallback chain doesn't
    // surface real usage numbers (each provider reports them differently,
    // and several silently fail through to the next) - this is a rough
    // chars/4 approximation (a standard rule of thumb for English text),
    // applied to the full prompt + reply. Not exact, but real enough to
    // make the free-tier cap mean something instead of being unenforceable.
    private fun estimateTokens(text: String): Int = (text.length / 4).coerceAtLeast(1)

    private data class PreparedTurn(val transcript: String, val onFreeTier: Boolean, val freeTokensUsed: Int, val userApiKey: String?)

    // chatId is an opaque per-conversation key from whatever's calling this
    // - a Telegram chat id, a WhatsApp number, or the owner's own userId
    // for the in-app test-chat path. Two different chatIds for the same
    // bot are two completely independent conversations.
    //
    // The actual AiProviderService.ask() call has to happen OUTSIDE any
    // Exposed transaction{} block (it's a suspend network call, and
    // transaction{} isn't a coroutine context) - so this reads+writes the
    // DB in two separate transactions bracketing that one suspend call,
    // rather than one single block like every other service's synchronous
    // DB-only functions.
    suspend fun converse(botId: String, chatId: String, message: String): String {
        val bid = UUID.fromString(botId)

        val prepared = transaction {
            val bot = Bots.selectAll().where { Bots.id eq bid }.firstOrNull() ?: throw AuthException("Bot not found")
            val isPremium = bot[Bots.isPremium]
            val userApiKey = bot[Bots.userApiKey]
            val freeTokensUsed = bot[Bots.freeTokensUsed]
            val onFreeTier = !isPremium && userApiKey == null
            if (onFreeTier && freeTokensUsed >= FREE_TOKEN_CAP) {
                throw AuthException("This bot has used its free 1000-token trial - add your own API key or ask the owner to upgrade to keep chatting.")
            }

            appendTurn(bid, chatId, "user", message)
            val history = recentTurns(bid, chatId, limit = 20)
            PreparedTurn(buildTranscript(bot, history), onFreeTier, freeTokensUsed, bot[Bots.userApiKey])
        }

        // A bot with its own userApiKey routes straight through that key
        // (own quota/cost, no fallback to Cedal's shared keys - see
        // AiProviderService.askWithKey's doc comment) and skips the
        // free-tier cap entirely, since it was never using Cedal's shared
        // quota to begin with.
        val reply = (
            prepared.userApiKey?.let { AiProviderService.askWithKey(prepared.transcript, maxTokens = 400, apiKey = it) }
                ?: AiProviderService.ask(prepared.transcript, maxTokens = 400)
            ).trim()

        transaction {
            appendTurn(bid, chatId, "assistant", reply)
            if (prepared.onFreeTier) {
                val used = estimateTokens(prepared.transcript) + estimateTokens(reply)
                Bots.update({ Bots.id eq bid }) { it[Bots.freeTokensUsed] = prepared.freeTokensUsed + used }
            }
        }

        return reply
    }

    fun history(botId: String, chatId: String): List<Pair<String, String>> = transaction {
        recentTurns(UUID.fromString(botId), chatId, limit = 50)
    }

    private fun buildTranscript(bot: ResultRow, history: List<Pair<String, String>>): String = buildString {
        append(systemPrompt(bot))
        appendLine()
        appendLine()
        history.forEach { (role, content) -> appendLine("${if (role == "user") "User" else bot[Bots.name]}: $content") }
        append("${bot[Bots.name]}:")
    }

    private fun systemPrompt(bot: ResultRow): String = buildString {
        appendLine("You are ${bot[Bots.name]}, an AI character. Stay fully in character at all times - never break character, never mention you're an AI model or language model.")
        appendLine("The lines below are formatted as \"Name: message\" purely so you can tell who said what - that's not a format to imitate. Reply with ONLY your own message text: no \"${bot[Bots.name]}:\" prefix, no name label of any kind, and no markdown formatting (no *asterisks*, no **bold**, no _underscores_) since your reply is sent as plain text on Telegram/WhatsApp and won't render it.")
        appendLine("Character/role: ${bot[Bots.character]}")
        appendLine("Personality: ${bot[Bots.personality]}")
        appendLine("Bio: ${bot[Bots.bio]}")
        bot[Bots.occupation]?.takeIf { it.isNotBlank() }?.let { appendLine("Occupation: $it") }
        bot[Bots.lifeStory]?.takeIf { it.isNotBlank() }?.let { appendLine("Life story: $it") }
        appendLine("Visual/energy description: ${bot[Bots.description]}")
        bot[Bots.age]?.let { appendLine("Age: $it") }
        bot[Bots.gender]?.takeIf { it.isNotBlank() }?.let { appendLine("Gender: $it") }
    }

    private fun appendTurn(botId: UUID, chatId: String, role: String, content: String) {
        BotConversationTurns.insert {
            it[BotConversationTurns.botId] = botId
            it[BotConversationTurns.chatId] = chatId
            it[BotConversationTurns.role] = role
            it[BotConversationTurns.content] = content
            it[createdAt] = System.currentTimeMillis()
        }
    }

    private fun recentTurns(botId: UUID, chatId: String, limit: Int): List<Pair<String, String>> =
        BotConversationTurns.selectAll()
            .where { (BotConversationTurns.botId eq botId) and (BotConversationTurns.chatId eq chatId) }
            .orderBy(BotConversationTurns.createdAt, SortOrder.DESC)
            .limit(limit)
            .map { it[BotConversationTurns.role] to it[BotConversationTurns.content] }
            .reversed()
}
