package com.xhacker.cedal.services

import com.xhacker.cedal.db.Bots
import com.xhacker.cedal.db.BotConversationTurns
import com.xhacker.cedal.db.Users
import com.xhacker.cedal.models.BotCreate
import com.xhacker.cedal.models.BotResponse
import com.xhacker.cedal.models.BotUpdate
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.util.UUID

// Member > More > Bots (MemberBotsScreen.kt) - Round 1 owner-scoped CRUD,
// Round 2 the brain endpoint, Round 3 (2026-08-10) real Telegram/WhatsApp
// connectivity. create/update/delete are suspend now (Round 3) since they
// may need to register/unregister a Telegram webhook - a suspend network
// call, so each splits into a synchronous transaction{} for the DB write
// bracketing that call, same shape BotBrainService.converse already uses.
object BotService {
    private const val ADMIN_EMAIL = "hackerxenos06@gmail.com"

    private fun isAdmin(userId: UUID): Boolean {
        val email = Users.selectAll().where { Users.id eq userId }.firstOrNull()?.get(Users.email) ?: return false
        return email.equals(ADMIN_EMAIL, ignoreCase = true)
    }

    private fun telegramHostedEligible(botType: String, hostingMode: String, telegramToken: String?): Boolean =
        hostingMode == "cedal" && telegramToken != null && (botType == "telegram" || botType == "both")

    suspend fun create(ownerId: String, req: BotCreate): BotResponse {
        if (req.hostingMode == "cedal") {
            throw AuthException("A bot can't start out cedal-hosted - save it first, ask the admin to mark it premium, then switch hosting mode.")
        }
        val owner = UUID.fromString(ownerId)
        val now = System.currentTimeMillis()
        val id = transaction {
            Bots.insert {
                it[ownerUserId] = owner
                it[name] = req.name
                it[age] = req.age
                it[gender] = req.gender
                it[character] = req.character
                it[personality] = req.personality
                it[bio] = req.bio
                it[occupation] = req.occupation
                it[lifeStory] = req.lifeStory
                it[description] = req.description
                it[iconUrl] = req.iconUrl
                it[botType] = req.botType
                it[telegramToken] = req.telegramToken
                it[whatsappPhoneNumberId] = req.whatsappPhoneNumberId
                it[whatsappAccessToken] = req.whatsappAccessToken
                it[userApiKey] = req.userApiKey
                it[hostingMode] = "self"
                it[secretToken] = UUID.randomUUID().toString().replace("-", "")
                it[createdAt] = now
                it[updatedAt] = now
            } get Bots.id
        }
        return transaction { toResponse(Bots.selectAll().where { Bots.id eq id }.first()) }
    }

    fun list(ownerId: String): List<BotResponse> = transaction {
        val owner = UUID.fromString(ownerId)
        Bots.selectAll().where { Bots.ownerUserId eq owner }
            .orderBy(Bots.createdAt)
            .map { toResponse(it) }
    }

    fun get(ownerId: String, botId: String): BotResponse? = transaction {
        val owner = UUID.fromString(ownerId)
        val bid = UUID.fromString(botId)
        Bots.selectAll().where { (Bots.id eq bid) and (Bots.ownerUserId eq owner) }
            .firstOrNull()
            ?.let { toResponse(it) }
    }

    // Round 2's external /converse path (and Round 3's Telegram webhook)
    // have no Cedal user JWT at all - authenticated by this instead.
    // Deliberately doesn't touch/return anything else about the bot - the
    // secretToken IS the auth for that path, not a lookup key for more data.
    fun verifySecretToken(botId: String, secretToken: String): Boolean = transaction {
        val bid = runCatching { UUID.fromString(botId) }.getOrNull() ?: return@transaction false
        Bots.selectAll().where { (Bots.id eq bid) and (Bots.secretToken eq secretToken) }.any()
    }

    // Owner-only. Round 3 - self-hosted download needs the raw secret
    // embedded in the generated code, and cedal-hosted WhatsApp setup needs
    // to show it to the user as the Meta webhook verify token. Never
    // returned from any other endpoint (BotResponse omits it entirely).
    fun revealSecret(ownerId: String, botId: String): String? = transaction {
        val owner = UUID.fromString(ownerId)
        val bid = UUID.fromString(botId)
        Bots.selectAll().where { (Bots.id eq bid) and (Bots.ownerUserId eq owner) }
            .firstOrNull()?.get(Bots.secretToken)
    }

    // Owner-only. The self-hosted download (BotTemplateService) needs the
    // bot's real credentials embedded in the generated code - unlike every
    // other read path, this is a deliberate one-time reveal the owner
    // explicitly triggered, not a bulk response leak.
    data class DownloadCredentials(val botType: String, val telegramToken: String?, val whatsappPhoneNumberId: String?, val whatsappAccessToken: String?)
    fun getCredentialsForDownload(ownerId: String, botId: String): DownloadCredentials? = transaction {
        val owner = UUID.fromString(ownerId)
        val bid = UUID.fromString(botId)
        Bots.selectAll().where { (Bots.id eq bid) and (Bots.ownerUserId eq owner) }.firstOrNull()?.let {
            DownloadCredentials(it[Bots.botType], it[Bots.telegramToken], it[Bots.whatsappPhoneNumberId], it[Bots.whatsappAccessToken])
        }
    }

    // Round 3's Telegram webhook route - already secretToken-verified by
    // the time this is called, no owner scoping needed (Telegram has no
    // notion of a Cedal userId). Just enough to send the reply back.
    fun getTelegramToken(botId: String): String? = transaction {
        val bid = runCatching { UUID.fromString(botId) }.getOrNull() ?: return@transaction null
        Bots.selectAll().where { Bots.id eq bid }.firstOrNull()?.get(Bots.telegramToken)
    }

    // Round 3's /webhooks/whatsapp route - Meta has exactly one webhook URL
    // per app, not per bot, so incoming messages are routed back to the
    // owning bot by the phoneNumberId embedded in every payload instead.
    data class WhatsappBotLookup(val id: String, val accessToken: String, val hostingMode: String)
    fun findByWhatsappPhoneNumberId(phoneNumberId: String): WhatsappBotLookup? = transaction {
        val row = Bots.selectAll().where { Bots.whatsappPhoneNumberId eq phoneNumberId }.firstOrNull() ?: return@transaction null
        val accessToken = row[Bots.whatsappAccessToken] ?: return@transaction null
        WhatsappBotLookup(row[Bots.id].value.toString(), accessToken, row[Bots.hostingMode])
    }

    // Admin-only, not owner-scoped - the admin can mark any bot premium,
    // mirroring SystemFeedService.isAdmin's single-hardcoded-account model
    // rather than inventing a role system for one admin.
    fun setPremium(callerId: String, botId: String, isPremium: Boolean): BotResponse? = transaction {
        val caller = UUID.fromString(callerId)
        if (!isAdmin(caller)) throw AuthException("Only the admin can change premium status")
        val bid = UUID.fromString(botId)
        val updated = Bots.update({ Bots.id eq bid }) { it[Bots.isPremium] = isPremium }
        if (updated == 0) return@transaction null
        toResponse(Bots.selectAll().where { Bots.id eq bid }.first())
    }

    private data class TelegramHostingTransition(val wasEligible: Boolean, val isNowEligible: Boolean, val token: String?, val secretToken: String)

    suspend fun update(ownerId: String, botId: String, req: BotUpdate): BotResponse? {
        val owner = UUID.fromString(ownerId)
        val bid = UUID.fromString(botId)

        val transition = transaction {
            val existing = Bots.selectAll().where { (Bots.id eq bid) and (Bots.ownerUserId eq owner) }.firstOrNull()
                ?: return@transaction null

            if (req.hostingMode == "cedal" && !existing[Bots.isPremium]) {
                throw AuthException("This bot isn't premium yet - ask the admin to enable cedal hosting first.")
            }

            val wasEligible = telegramHostedEligible(existing[Bots.botType], existing[Bots.hostingMode], existing[Bots.telegramToken])
            val newToken = req.telegramToken ?: existing[Bots.telegramToken]
            val isNowEligible = telegramHostedEligible(req.botType, req.hostingMode, newToken)

            Bots.update({ (Bots.id eq bid) and (Bots.ownerUserId eq owner) }) {
                it[name] = req.name
                it[age] = req.age
                it[gender] = req.gender
                it[character] = req.character
                it[personality] = req.personality
                it[bio] = req.bio
                it[occupation] = req.occupation
                it[lifeStory] = req.lifeStory
                it[description] = req.description
                it[iconUrl] = req.iconUrl
                it[botType] = req.botType
                // Only overwrite a credential when the request explicitly sent one -
                // otherwise keep whatever's already stored (see BotUpdate's doc comment).
                if (req.telegramToken != null) it[telegramToken] = req.telegramToken
                if (req.whatsappPhoneNumberId != null) it[whatsappPhoneNumberId] = req.whatsappPhoneNumberId
                if (req.whatsappAccessToken != null) it[whatsappAccessToken] = req.whatsappAccessToken
                if (req.userApiKey != null) it[userApiKey] = req.userApiKey
                it[hostingMode] = req.hostingMode
                it[updatedAt] = System.currentTimeMillis()
            }
            TelegramHostingTransition(wasEligible, isNowEligible, newToken, existing[Bots.secretToken])
        } ?: return null

        if (transition.isNowEligible && transition.token != null) {
            TelegramBotService.registerWebhook(botId, transition.token, transition.secretToken)
        } else if (transition.wasEligible && !transition.isNowEligible && transition.token != null) {
            TelegramBotService.unregisterWebhook(transition.token)
        }

        return transaction { Bots.selectAll().where { Bots.id eq bid }.firstOrNull() }?.let { toResponse(it) }
    }

    suspend fun delete(ownerId: String, botId: String): Boolean {
        val owner = UUID.fromString(ownerId)
        val bid = UUID.fromString(botId)

        val toUnregister = transaction {
            val existing = Bots.selectAll().where { (Bots.id eq bid) and (Bots.ownerUserId eq owner) }.firstOrNull()
                ?: return@transaction null
            if (telegramHostedEligible(existing[Bots.botType], existing[Bots.hostingMode], existing[Bots.telegramToken])) {
                existing[Bots.telegramToken]
            } else {
                null
            }
        }
        toUnregister?.let { TelegramBotService.unregisterWebhook(it) }

        return transaction {
            // BotConversationTurns.botId references Bots with no cascade -
            // any bot that's ever been messaged (even just via Test Chat)
            // has rows here, and deleting Bots first hits a real FK
            // violation (found 2026-08-10 - Delete Bot failing on a bot
            // with conversation history). Same ordering discipline
            // AccountService.deleteAccount already uses for this same pair.
            BotConversationTurns.deleteWhere { BotConversationTurns.botId eq bid }
            Bots.deleteWhere { (Bots.id eq bid) and (Bots.ownerUserId eq owner) } > 0
        }
    }

    private fun toResponse(row: ResultRow) = BotResponse(
        id = row[Bots.id].value.toString(),
        name = row[Bots.name],
        age = row[Bots.age],
        gender = row[Bots.gender],
        character = row[Bots.character],
        personality = row[Bots.personality],
        bio = row[Bots.bio],
        occupation = row[Bots.occupation],
        lifeStory = row[Bots.lifeStory],
        description = row[Bots.description],
        iconUrl = row[Bots.iconUrl],
        botType = row[Bots.botType],
        hasTelegramToken = row[Bots.telegramToken] != null,
        hasWhatsappCredentials = row[Bots.whatsappPhoneNumberId] != null && row[Bots.whatsappAccessToken] != null,
        freeTokensUsed = row[Bots.freeTokensUsed],
        isPremium = row[Bots.isPremium],
        hasUserApiKey = row[Bots.userApiKey] != null,
        hostingMode = row[Bots.hostingMode],
        createdAt = row[Bots.createdAt],
        updatedAt = row[Bots.updatedAt],
    )
}
