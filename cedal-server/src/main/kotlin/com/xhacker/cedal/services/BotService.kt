package com.xhacker.cedal.services

import com.xhacker.cedal.db.Bots
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

// Member > More > Bots (MemberBotsScreen.kt) - Round 1 of the "Leo"
// bot-builder platform. Owner-scoped CRUD for the character-sheet form only;
// the brain endpoint (/bots/{id}/converse), code generation, and the
// token-cap/premium gate are later rounds - see planner/left-to-do.md.
object BotService {

    fun create(ownerId: String, req: BotCreate): BotResponse = transaction {
        val owner = UUID.fromString(ownerId)
        val now = System.currentTimeMillis()
        val id = Bots.insert {
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
            it[secretToken] = UUID.randomUUID().toString().replace("-", "")
            it[createdAt] = now
            it[updatedAt] = now
        } get Bots.id

        toResponse(Bots.selectAll().where { Bots.id eq id }.first())
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

    fun update(ownerId: String, botId: String, req: BotUpdate): BotResponse? = transaction {
        val owner = UUID.fromString(ownerId)
        val bid = UUID.fromString(botId)
        val existing = Bots.selectAll().where { (Bots.id eq bid) and (Bots.ownerUserId eq owner) }.firstOrNull()
            ?: return@transaction null

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
            it[updatedAt] = System.currentTimeMillis()
        }

        toResponse(Bots.selectAll().where { Bots.id eq bid }.first())
    }

    fun delete(ownerId: String, botId: String): Boolean = transaction {
        val owner = UUID.fromString(ownerId)
        val bid = UUID.fromString(botId)
        Bots.deleteWhere { (Bots.id eq bid) and (Bots.ownerUserId eq owner) } > 0
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
        createdAt = row[Bots.createdAt],
        updatedAt = row[Bots.updatedAt],
    )
}
