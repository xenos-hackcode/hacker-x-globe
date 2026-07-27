package com.xhacker.cedal.services

import com.xhacker.cedal.db.ChatPopularityOverrides
import com.xhacker.cedal.db.PopularitySettings
import com.xhacker.cedal.models.ChatPopularityOverrideDto
import com.xhacker.cedal.models.PopularitySettingsDto
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.util.UUID

// Settings > Security > Popularity (global) + each chat's ⋮ menu > Popularity
// (per-viewer override) - lets a user filter which of their own profile
// fields (name/pfp/age/rank) show up when someone else views their profile.
// See AuthService.getProfile's redaction of the fields below when a viewer
// other than the profile owner requests it.
object PopularityService {
    data class Effective(
        val showName: Boolean,
        val showPfp: Boolean,
        val showAge: Boolean,
        val showRank: Boolean,
        val showOccupation: Boolean,
        val showHobby: Boolean,
        val showBio: Boolean,
        val showGender: Boolean,
    )

    fun getSettings(userId: String): PopularitySettingsDto = transaction {
        val uid = UUID.fromString(userId)
        val row = PopularitySettings.selectAll().where { PopularitySettings.userId eq uid }.firstOrNull()
            ?: return@transaction PopularitySettingsDto()
        PopularitySettingsDto(
            showName = row[PopularitySettings.showName],
            showPfp = row[PopularitySettings.showPfp],
            showAge = row[PopularitySettings.showAge],
            showRank = row[PopularitySettings.showRank],
            showOccupation = row[PopularitySettings.showOccupation],
            showHobby = row[PopularitySettings.showHobby],
            showBio = row[PopularitySettings.showBio],
            showGender = row[PopularitySettings.showGender],
        )
    }

    fun setSettings(userId: String, req: PopularitySettingsDto): Unit = transaction {
        val uid = UUID.fromString(userId)
        val existing = PopularitySettings.selectAll().where { PopularitySettings.userId eq uid }.firstOrNull()
        if (existing == null) {
            PopularitySettings.insert {
                it[PopularitySettings.userId] = uid
                it[showName] = req.showName
                it[showPfp] = req.showPfp
                it[showAge] = req.showAge
                it[showRank] = req.showRank
                it[showOccupation] = req.showOccupation
                it[showHobby] = req.showHobby
                it[showBio] = req.showBio
                it[showGender] = req.showGender
            }
        } else {
            PopularitySettings.update({ PopularitySettings.userId eq uid }) {
                it[showName] = req.showName
                it[showPfp] = req.showPfp
                it[showAge] = req.showAge
                it[showRank] = req.showRank
                it[showOccupation] = req.showOccupation
                it[showHobby] = req.showHobby
                it[showBio] = req.showBio
                it[showGender] = req.showGender
            }
        }
        AchievementService.checkPrivate(userId)
    }

    fun getOverride(userId: String, friendId: String): ChatPopularityOverrideDto = transaction {
        val uid = UUID.fromString(userId)
        val fid = UUID.fromString(friendId)
        val row = ChatPopularityOverrides.selectAll()
            .where { (ChatPopularityOverrides.userId eq uid) and (ChatPopularityOverrides.friendId eq fid) }
            .firstOrNull() ?: return@transaction ChatPopularityOverrideDto()
        ChatPopularityOverrideDto(
            showName = row[ChatPopularityOverrides.showName],
            showPfp = row[ChatPopularityOverrides.showPfp],
            showAge = row[ChatPopularityOverrides.showAge],
            showRank = row[ChatPopularityOverrides.showRank],
            showOccupation = row[ChatPopularityOverrides.showOccupation],
            showHobby = row[ChatPopularityOverrides.showHobby],
            showBio = row[ChatPopularityOverrides.showBio],
            showGender = row[ChatPopularityOverrides.showGender],
        )
    }

    fun setOverride(userId: String, friendId: String, req: ChatPopularityOverrideDto): Unit = transaction {
        val uid = UUID.fromString(userId)
        val fid = UUID.fromString(friendId)
        val existing = ChatPopularityOverrides.selectAll()
            .where { (ChatPopularityOverrides.userId eq uid) and (ChatPopularityOverrides.friendId eq fid) }
            .firstOrNull()
        if (existing == null) {
            ChatPopularityOverrides.insert {
                it[ChatPopularityOverrides.userId] = uid
                it[ChatPopularityOverrides.friendId] = fid
                it[showName] = req.showName
                it[showPfp] = req.showPfp
                it[showAge] = req.showAge
                it[showRank] = req.showRank
                it[showOccupation] = req.showOccupation
                it[showHobby] = req.showHobby
                it[showBio] = req.showBio
                it[showGender] = req.showGender
            }
        } else {
            ChatPopularityOverrides.update({ (ChatPopularityOverrides.userId eq uid) and (ChatPopularityOverrides.friendId eq fid) }) {
                it[showName] = req.showName
                it[showPfp] = req.showPfp
                it[showAge] = req.showAge
                it[showRank] = req.showRank
                it[showOccupation] = req.showOccupation
                it[showHobby] = req.showHobby
                it[showBio] = req.showBio
                it[showGender] = req.showGender
            }
        }
    }

    // Global setting, overlaid by a per-viewer override where one exists -
    // called by AuthService.getProfile whenever the requester isn't the
    // profile owner themself.
    fun effectiveFor(ownerId: UUID, viewerId: UUID): Effective = transaction {
        val global = PopularitySettings.selectAll().where { PopularitySettings.userId eq ownerId }.firstOrNull()
        val override = ChatPopularityOverrides.selectAll()
            .where { (ChatPopularityOverrides.userId eq ownerId) and (ChatPopularityOverrides.friendId eq viewerId) }
            .firstOrNull()
        Effective(
            showName = override?.get(ChatPopularityOverrides.showName) ?: global?.get(PopularitySettings.showName) ?: true,
            showPfp = override?.get(ChatPopularityOverrides.showPfp) ?: global?.get(PopularitySettings.showPfp) ?: true,
            showAge = override?.get(ChatPopularityOverrides.showAge) ?: global?.get(PopularitySettings.showAge) ?: true,
            showRank = override?.get(ChatPopularityOverrides.showRank) ?: global?.get(PopularitySettings.showRank) ?: true,
            showOccupation = override?.get(ChatPopularityOverrides.showOccupation) ?: global?.get(PopularitySettings.showOccupation) ?: true,
            showHobby = override?.get(ChatPopularityOverrides.showHobby) ?: global?.get(PopularitySettings.showHobby) ?: true,
            showBio = override?.get(ChatPopularityOverrides.showBio) ?: global?.get(PopularitySettings.showBio) ?: true,
            showGender = override?.get(ChatPopularityOverrides.showGender) ?: global?.get(PopularitySettings.showGender) ?: true,
        )
    }
}
