package com.xhacker.cedal.services

import com.xhacker.cedal.db.Stickers
import com.xhacker.cedal.models.StickerDto
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

// Personal custom stickers - private to whoever created them (see Stickers
// table comment), uploaded via ImageUploadService then registered here.
object StickerService {
    fun createSticker(userId: String, imageUrl: String): StickerDto = transaction {
        val uid = UUID.fromString(userId)
        val now = System.currentTimeMillis()
        val id = Stickers.insertAndGetId {
            it[ownerId] = uid
            it[Stickers.imageUrl] = imageUrl
            it[createdAt] = now
        }
        StickerDto(id.value.toString(), imageUrl, now)
    }

    fun listMyStickers(userId: String): List<StickerDto> = transaction {
        val uid = UUID.fromString(userId)
        Stickers.selectAll()
            .where { Stickers.ownerId eq uid }
            .orderBy(Stickers.createdAt, SortOrder.DESC)
            .map { StickerDto(it[Stickers.id].value.toString(), it[Stickers.imageUrl], it[Stickers.createdAt]) }
    }
}
