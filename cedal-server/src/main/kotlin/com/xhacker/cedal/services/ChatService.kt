package com.xhacker.cedal.services

import com.xhacker.cedal.db.Blocks
import com.xhacker.cedal.db.ChatMessageReactions
import com.xhacker.cedal.db.ChatMessages
import com.xhacker.cedal.db.ConversationState
import com.xhacker.cedal.db.FriendRequests
import com.xhacker.cedal.db.UserReports
import com.xhacker.cedal.db.PollVotes
import com.xhacker.cedal.db.SystemFeedPosts
import com.xhacker.cedal.db.Users
import com.xhacker.cedal.models.ChatMessageDto
import com.xhacker.cedal.models.ConversationSummary
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greater
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.SqlExpressionBuilder.lessEq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.SqlExpressionBuilder.isNull
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.util.UUID

// Real 1-on-1 chat, but only ever between accepted friends - see
// sendMessage()'s check. Once accepted, a friend request has no further
// bearing here; ChatMessages rows ARE the conversation, keyed by the
// (sender, receiver) pair rather than a separate conversation id.
object ChatService {
    private const val MAX_MESSAGE_LENGTH = 2000
    // "Time Once" clamp - up to 7 days, never a hard error for going over
    // (see sendMessage's clampedDurationMs). "View Once"'s clamp mirrors
    // the 1000-view example the user gave for its own upper bound.
    private const val MAX_VIEW_ONCE_DURATION_MS = 7L * 24 * 60 * 60 * 1000
    private const val MAX_VIEW_ONCE_COUNT = 1000

    // Sentinel friendId for the synthetic "Cedal System Feed" row merged into
    // listConversations() below - not a real friend/UUID, the client routes
    // taps on this id to the System Feed screen instead of a chat thread.
    const val SYSTEM_FEED_ID = "system_feed"

    // Matches cedal-mobile's edit window - after this, a message is locked
    // as sent (deletion still works at any time, only editing is time-boxed).
    private val EDIT_WINDOW_MS = 5 * 60 * 1000L

    private fun areFriends(a: UUID, b: UUID): Boolean =
        FriendRequests.selectAll()
            .where {
                (((FriendRequests.fromUserId eq a) and (FriendRequests.toUserId eq b)) or
                    ((FriendRequests.fromUserId eq b) and (FriendRequests.toUserId eq a))) and
                    (FriendRequests.status eq "accepted")
            }
            .any()

    private fun reactionsFor(messageIds: Collection<UUID>): Map<UUID, Map<String, String>> {
        if (messageIds.isEmpty()) return emptyMap()
        val result = mutableMapOf<UUID, MutableMap<String, String>>()
        ChatMessageReactions.selectAll()
            .where { ChatMessageReactions.messageId inList messageIds }
            .forEach { row ->
                val mid = row[ChatMessageReactions.messageId].value
                result.getOrPut(mid) { mutableMapOf() }[row[ChatMessageReactions.userId].value.toString()] = row[ChatMessageReactions.emoji]
            }
        return result
    }

    private fun pollVotesFor(messageIds: Collection<UUID>): Map<UUID, Map<String, Int>> {
        if (messageIds.isEmpty()) return emptyMap()
        val result = mutableMapOf<UUID, MutableMap<String, Int>>()
        PollVotes.selectAll()
            .where { PollVotes.messageId inList messageIds }
            .forEach { row ->
                val mid = row[PollVotes.messageId].value
                result.getOrPut(mid) { mutableMapOf() }[row[PollVotes.userId].value.toString()] = row[PollVotes.optionIndex]
            }
        return result
    }

    private fun toDto(row: ResultRow, reactions: Map<String, String>, pollVotes: Map<String, Int> = emptyMap()): ChatMessageDto {
        val isDeleted = row[ChatMessages.deleted]
        val viewOnce = row[ChatMessages.viewOnce]
        val viewed = row[ChatMessages.viewedAt] != null
        val mode = row[ChatMessages.viewOnceMode]
        // A view-once message's real content is hidden from everyone -
        // including the sender - until the recipient explicitly reveals it
        // (see revealMessage()); the locked bubble is a generic textured
        // card (see MessageBubble/ViewOnceLockedCard), not a render of the
        // real content, so there's no need to deliver it early. Classic
        // "once" mode unlocks permanently in every future fetch after that
        // single reveal (matches the pre-existing behavior); the custom
        // modes never leak real content through a regular fetch at all -
        // every view, first or repeat, has to go through revealMessage()
        // itself so it can enforce the count/time limit each time.
        val hideContent = isDeleted || when {
            !viewOnce -> false
            mode == "custom_count" || mode == "custom_time" -> true
            else -> !viewed
        }
        return ChatMessageDto(
            id = row[ChatMessages.id].value.toString(),
            senderId = row[ChatMessages.senderId].value.toString(),
            receiverId = row[ChatMessages.receiverId].value.toString(),
            text = if (hideContent) "" else row[ChatMessages.text],
            sentAt = row[ChatMessages.sentAt],
            editedAt = row[ChatMessages.editedAt],
            deleted = isDeleted,
            replyToId = row[ChatMessages.replyToId]?.toString(),
            reactions = if (isDeleted) emptyMap() else reactions,
            isSticker = row[ChatMessages.isSticker],
            mediaUrl = if (hideContent) null else row[ChatMessages.mediaUrl],
            mediaType = row[ChatMessages.mediaType],
            fileName = row[ChatMessages.fileName],
            viewOnce = viewOnce,
            viewed = viewed,
            viewOnceMode = mode,
            viewOnceDurationMs = row[ChatMessages.viewOnceDurationMs],
            viewOnceMaxViews = row[ChatMessages.viewOnceMaxViews],
            viewOnceViewCount = row[ChatMessages.viewOnceViewCount],
            pollQuestion = if (isDeleted) null else row[ChatMessages.pollQuestion],
            pollOptions = if (isDeleted) null else row[ChatMessages.pollOptions]?.split("\n"),
            pollVotes = if (isDeleted) emptyMap() else pollVotes,
            translatedText = if (hideContent) null else row[ChatMessages.translatedText],
            // Only meaningful for a message FROM the other person - your own
            // sent messages are always "read" from your own POV (nothing to
            // catch up on). Drives the first-unread scroll target and the
            // incremental unread count client-side - see markRead() below,
            // which is what actually flips this now instead of getMessages()
            // unconditionally marking everything the moment the thread opens.
            read = row[ChatMessages.readAt] != null,
        )
    }

    suspend fun sendMessage(
        fromUserId: String,
        toUserId: String,
        text: String,
        replyToId: String?,
        isSticker: Boolean = false,
        mediaUrl: String? = null,
        mediaType: String? = null,
        fileName: String? = null,
        viewOnce: Boolean = false,
        viewOnceMode: String? = null,
        viewOnceDurationMs: Long? = null,
        viewOnceMaxViews: Int? = null,
        pollQuestion: String? = null,
        pollOptions: List<String>? = null,
    ): ChatMessageDto {
        val from = UUID.fromString(fromUserId)
        val to = UUID.fromString(toUserId)
        // A null mode with viewOnce=true is the classic single-reveal case -
        // defaults to "once" so old client builds (that only ever send the
        // plain boolean) keep working unchanged. Current client builds
        // always send an explicit mode now ("View Once" -> custom_count,
        // "Time Once" -> custom_time - see ViewOnceModeOverlay client-side).
        val effectiveMode = if (!viewOnce) null else (viewOnceMode ?: "once")
        if (effectiveMode == "custom_count" && (viewOnceMaxViews == null || viewOnceMaxViews < 1)) {
            throw AuthException("Custom view limit needs at least 1 view")
        }
        if (effectiveMode == "custom_time" && (viewOnceDurationMs == null || viewOnceDurationMs < 1000)) {
            throw AuthException("Custom view timer needs at least 1 second")
        }
        // Clamps, not hard errors - "if they have 7 days and even 1 minute
        // it just defaults back to 7 days" rather than rejecting the send.
        // Server-side, never trusting the client's own clamping alone.
        val clampedMaxViews = viewOnceMaxViews?.coerceAtMost(MAX_VIEW_ONCE_COUNT)
        val clampedDurationMs = viewOnceDurationMs?.coerceAtMost(MAX_VIEW_ONCE_DURATION_MS)

        val cleanOptions = pollOptions?.map { it.trim() }?.filter { it.isNotBlank() }
        val isPoll = pollQuestion != null
        if (isPoll) {
            if (pollQuestion.isBlank()) throw AuthException("Poll needs a question")
            if (cleanOptions == null || cleanOptions.size < 2 || cleanOptions.size > 4) {
                throw AuthException("Poll needs 2-4 options")
            }
        }
        // A media attachment or poll can carry an empty caption; plain text
        // (and stickers) can't be blank. Stickers/captions skip the length
        // trim - never long enough to hit MAX_MESSAGE_LENGTH anyway.
        val trimmed = if (isSticker || mediaUrl != null || isPoll) text.trim() else text.trim().take(MAX_MESSAGE_LENGTH)
        if (trimmed.isBlank() && mediaUrl == null && !isPoll) throw AuthException("Message can't be empty")

        // Validation + the language lookup both need a transaction, but the
        // actual translate() call below is a real network request (an AI
        // call) - can't happen inside a synchronous Exposed `transaction{}`
        // block, so this reads what it needs first, then closes the
        // transaction before calling out.
        data class PreCheck(val validReply: UUID?, val fromLang: String?, val toLang: String?)
        val pre = transaction {
            if (from == to) throw AuthException("Can't message yourself")
            // Checked separately from areFriends() below so a deleted
            // recipient (AccountService.deleteAccount wipes their
            // FriendRequests rows along with the Users row itself) gets its
            // own clear message instead of the generic "not friends" one -
            // see FriendService.friendStatus for the client's proactive
            // (pre-send) version of this same check.
            if (!Users.selectAll().where { Users.id eq to }.any()) throw AuthException("This account has been deleted")
            if (!areFriends(from, to)) throw AuthException("You can only message accepted friends")
            // Either direction blocks sending - the recipient blocking you
            // stops your messages same as you blocking them (see the Block
            // chat-thread menu action / Blocks table).
            val theyBlockedMe = Blocks.selectAll().where { (Blocks.blockerId eq to) and (Blocks.blockedId eq from) }.any()
            if (theyBlockedMe) throw AuthException("You have been blocked")
            val iBlockedThem = Blocks.selectAll().where { (Blocks.blockerId eq from) and (Blocks.blockedId eq to) }.any()
            if (iBlockedThem) throw AuthException("You've blocked this person - unblock them to send a message")
            val replyUuid = replyToId?.let { UUID.fromString(it) }
            // A reply must point at a real message in this same conversation -
            // silently drop a stale/foreign id rather than fail the whole send.
            val validReply = replyUuid?.takeIf { rid ->
                ChatMessages.selectAll().where {
                    (ChatMessages.id eq rid) and
                        (((ChatMessages.senderId eq from) and (ChatMessages.receiverId eq to)) or
                            ((ChatMessages.senderId eq to) and (ChatMessages.receiverId eq from)))
                }.any()
            }
            val fromLang = Users.selectAll().where { Users.id eq from }.firstOrNull()?.get(Users.preferredLanguage)
            val toLang = Users.selectAll().where { Users.id eq to }.firstOrNull()?.get(Users.preferredLanguage)
            PreCheck(validReply, fromLang, toLang)
        }

        // Only real chat text gets translated - a sticker/media caption/poll
        // question staying untranslated is fine, those aren't prose. A
        // sender who never touched Settings > Languages defaults to
        // "English" here (NOT "skip translation") - the recipient's choice
        // is what actually drives whether this fires, same as the sender
        // never needing to opt into anything for a message written in
        // plain English to reach a French-set recipient as French.
        val effectiveFromLang = pre.fromLang ?: "English"
        val translatedText = if (!isSticker && !isPoll && pre.toLang != null && effectiveFromLang != pre.toLang) {
            TranslationService.translate(trimmed, pre.toLang)
        } else {
            null
        }

        return transaction {
            val sentAt = System.currentTimeMillis()
            val id = ChatMessages.insertAndGetId {
                it[senderId] = from
                it[receiverId] = to
                it[ChatMessages.text] = trimmed
                it[ChatMessages.sentAt] = sentAt
                it[ChatMessages.replyToId] = pre.validReply
                it[ChatMessages.isSticker] = isSticker
                it[ChatMessages.mediaUrl] = mediaUrl
                it[ChatMessages.mediaType] = mediaType
                it[ChatMessages.fileName] = fileName
                it[ChatMessages.viewOnce] = viewOnce
                it[ChatMessages.viewOnceMode] = effectiveMode
                it[ChatMessages.viewOnceDurationMs] = if (effectiveMode == "custom_time") clampedDurationMs else null
                it[ChatMessages.viewOnceMaxViews] = if (effectiveMode == "custom_count") clampedMaxViews else null
                it[ChatMessages.pollQuestion] = pollQuestion
                it[ChatMessages.pollOptions] = cleanOptions?.joinToString("\n")
                it[ChatMessages.translatedText] = translatedText
            }
            ChatMessageDto(
                id.value.toString(), fromUserId, toUserId, trimmed, sentAt,
                replyToId = pre.validReply?.toString(), isSticker = isSticker,
                mediaUrl = mediaUrl, mediaType = mediaType, fileName = fileName, viewOnce = viewOnce,
                viewOnceMode = effectiveMode,
                viewOnceDurationMs = if (effectiveMode == "custom_time") clampedDurationMs else null,
                viewOnceMaxViews = if (effectiveMode == "custom_count") clampedMaxViews else null,
                pollQuestion = pollQuestion, pollOptions = cleanOptions,
                translatedText = translatedText,
            )
        }
    }

    // One vote per user, changing your pick just overwrites it - see
    // PollVotes. Anyone in the conversation (either side) can vote,
    // including the poll's own creator.
    fun voteInPoll(userId: String, messageId: String, optionIndex: Int): Map<String, Int> = transaction {
        val uid = UUID.fromString(userId)
        val mid = UUID.fromString(messageId)
        val row = ChatMessages.selectAll().where { ChatMessages.id eq mid }.firstOrNull() ?: throw AuthException("Message not found")
        val options = row[ChatMessages.pollOptions]?.split("\n") ?: throw AuthException("This message isn't a poll")
        if (optionIndex !in options.indices) throw AuthException("Invalid option")
        if (row[ChatMessages.senderId].value != uid && row[ChatMessages.receiverId].value != uid) {
            throw AuthException("You can only vote in polls in your own conversations")
        }

        PollVotes.deleteWhere { (PollVotes.messageId eq mid) and (PollVotes.userId eq uid) }
        PollVotes.insert {
            it[PollVotes.messageId] = mid
            it[PollVotes.userId] = uid
            it[PollVotes.optionIndex] = optionIndex
        }
        pollVotesFor(listOf(mid))[mid].orEmpty()
    }

    // Only the recipient can reveal a view-once message. "once" mode flips
    // the permanent viewedAt flag (see toDto's viewed field) so every
    // client stops blurring it, on this device and any other - calling it
    // again once already viewed is harmless, just returns the same DTO.
    // The custom modes instead re-check + re-enforce their limit on EVERY
    // call (see toDto's doc comment - they never leak content outside this
    // function), throwing once genuinely exhausted/expired.
    fun revealMessage(userId: String, messageId: String): ChatMessageDto = transaction {
        val uid = UUID.fromString(userId)
        val mid = UUID.fromString(messageId)
        val row = ChatMessages.selectAll().where { ChatMessages.id eq mid }.firstOrNull() ?: throw AuthException("Message not found")
        if (row[ChatMessages.receiverId].value != uid) throw AuthException("Only the recipient can reveal this message")
        if (!row[ChatMessages.viewOnce]) throw AuthException("This message isn't view-once")
        val mode = row[ChatMessages.viewOnceMode]

        when (mode) {
            "custom_count" -> {
                val max = row[ChatMessages.viewOnceMaxViews] ?: 1
                val count = row[ChatMessages.viewOnceViewCount]
                val alreadyViewed = row[ChatMessages.viewedAt] != null
                if (count >= max) throw AuthException("This message has already been viewed the maximum number of times")
                ChatMessages.update({ ChatMessages.id eq mid }) { stmt: org.jetbrains.exposed.sql.statements.UpdateStatement ->
                    stmt[ChatMessages.viewOnceViewCount] = count + 1
                    if (!alreadyViewed) stmt[ChatMessages.viewedAt] = System.currentTimeMillis()
                }
            }
            "custom_time" -> {
                val viewedAt = row[ChatMessages.viewedAt]
                val duration = row[ChatMessages.viewOnceDurationMs] ?: 0L
                if (viewedAt != null && System.currentTimeMillis() > viewedAt + duration) {
                    throw AuthException("This message has expired")
                }
                if (viewedAt == null) {
                    ChatMessages.update({ ChatMessages.id eq mid }) { stmt: org.jetbrains.exposed.sql.statements.UpdateStatement -> stmt[ChatMessages.viewedAt] = System.currentTimeMillis() }
                }
            }
            else -> {
                if (row[ChatMessages.viewedAt] == null) {
                    ChatMessages.update({ ChatMessages.id eq mid }) { stmt: org.jetbrains.exposed.sql.statements.UpdateStatement -> stmt[ChatMessages.viewedAt] = System.currentTimeMillis() }
                }
            }
        }

        // Build the DTO from the pre-update row's real content directly,
        // rather than re-reading (which would now come back stripped).
        val reactions = reactionsFor(listOf(mid))[mid].orEmpty()
        ChatMessageDto(
            id = row[ChatMessages.id].value.toString(),
            senderId = row[ChatMessages.senderId].value.toString(),
            receiverId = row[ChatMessages.receiverId].value.toString(),
            text = row[ChatMessages.text],
            sentAt = row[ChatMessages.sentAt],
            editedAt = row[ChatMessages.editedAt],
            replyToId = row[ChatMessages.replyToId]?.toString(),
            reactions = reactions,
            isSticker = row[ChatMessages.isSticker],
            mediaUrl = row[ChatMessages.mediaUrl],
            mediaType = row[ChatMessages.mediaType],
            fileName = row[ChatMessages.fileName],
            viewOnce = true,
            viewed = true,
            viewOnceMode = mode,
            viewOnceDurationMs = row[ChatMessages.viewOnceDurationMs],
            viewOnceMaxViews = row[ChatMessages.viewOnceMaxViews],
            viewOnceViewCount = if (mode == "custom_count") row[ChatMessages.viewOnceViewCount] + 1 else row[ChatMessages.viewOnceViewCount],
        )
    }

    // "It would be like it never existed" - once a view-once message is
    // fully consumed (view limit reached / time window expired), it doesn't
    // linger as a locked "no more views" card - it's hard-deleted entirely,
    // for both sides, the next time either of them leaves this chat thread
    // or the app backgrounds (see the client's lifecycle hook that calls
    // this). Only ever removes CONSUMED messages - one still within its
    // limit/window is untouched even if this fires while it's on screen.
    fun purgeConsumedViewOnce(userId: String, otherUserId: String) = transaction {
        val uid = UUID.fromString(userId)
        val other = UUID.fromString(otherUserId)
        val now = System.currentTimeMillis()
        val candidates = ChatMessages.selectAll()
            .where {
                (ChatMessages.viewOnce eq true) and
                    (((ChatMessages.senderId eq uid) and (ChatMessages.receiverId eq other)) or
                        ((ChatMessages.senderId eq other) and (ChatMessages.receiverId eq uid)))
            }
            .toList()
        val consumedIds = candidates.filter { row ->
            when (row[ChatMessages.viewOnceMode]) {
                "custom_count" -> row[ChatMessages.viewOnceViewCount] >= (row[ChatMessages.viewOnceMaxViews] ?: 1)
                "custom_time" -> {
                    val viewedAt = row[ChatMessages.viewedAt]
                    viewedAt != null && now > viewedAt + (row[ChatMessages.viewOnceDurationMs] ?: 0L)
                }
                else -> row[ChatMessages.viewedAt] != null
            }
        }.map { it[ChatMessages.id].value }
        if (consumedIds.isNotEmpty()) {
            ChatMessageReactions.deleteWhere { ChatMessageReactions.messageId inList consumedIds }
            ChatMessages.deleteWhere { ChatMessages.id inList consumedIds }
        }
    }

    fun editMessage(userId: String, messageId: String, newText: String): ChatMessageDto = transaction {
        val uid = UUID.fromString(userId)
        val mid = UUID.fromString(messageId)
        val row = ChatMessages.selectAll().where { ChatMessages.id eq mid }.firstOrNull() ?: throw AuthException("Message not found")
        if (row[ChatMessages.senderId].value != uid) throw AuthException("You can only edit your own messages")
        if (row[ChatMessages.deleted]) throw AuthException("Can't edit a deleted message")
        if (System.currentTimeMillis() - row[ChatMessages.sentAt] > EDIT_WINDOW_MS) throw AuthException("Too late to edit this message")
        val trimmed = newText.trim().take(MAX_MESSAGE_LENGTH)
        if (trimmed.isBlank()) throw AuthException("Message can't be empty")

        val editedAt = System.currentTimeMillis()
        ChatMessages.update({ ChatMessages.id eq mid }) {
            it[ChatMessages.text] = trimmed
            it[ChatMessages.editedAt] = editedAt
        }
        val updated = ChatMessages.selectAll().where { ChatMessages.id eq mid }.first()
        toDto(updated, reactionsFor(listOf(mid))[mid].orEmpty())
    }

    fun deleteMessage(userId: String, messageId: String): Unit = transaction {
        val uid = UUID.fromString(userId)
        val mid = UUID.fromString(messageId)
        val row = ChatMessages.selectAll().where { ChatMessages.id eq mid }.firstOrNull() ?: throw AuthException("Message not found")
        if (row[ChatMessages.senderId].value != uid) throw AuthException("You can only delete your own messages")
        ChatMessages.update({ ChatMessages.id eq mid }) { it[deleted] = true }
        ChatMessageReactions.deleteWhere { ChatMessageReactions.messageId eq mid }
    }

    // Tapping the same emoji you already reacted with removes it; tapping a
    // different one replaces it - one reaction per user per message, same
    // as cedal-mobile.
    fun reactToMessage(userId: String, messageId: String, emoji: String): Map<String, String> = transaction {
        val uid = UUID.fromString(userId)
        val mid = UUID.fromString(messageId)
        ChatMessages.selectAll().where { ChatMessages.id eq mid }.firstOrNull() ?: throw AuthException("Message not found")

        val existing = ChatMessageReactions.selectAll()
            .where { (ChatMessageReactions.messageId eq mid) and (ChatMessageReactions.userId eq uid) }
            .firstOrNull()
        if (existing != null && existing[ChatMessageReactions.emoji] == emoji) {
            ChatMessageReactions.deleteWhere { (ChatMessageReactions.messageId eq mid) and (ChatMessageReactions.userId eq uid) }
        } else {
            ChatMessageReactions.deleteWhere { (ChatMessageReactions.messageId eq mid) and (ChatMessageReactions.userId eq uid) }
            ChatMessageReactions.insert {
                it[ChatMessageReactions.messageId] = mid
                it[ChatMessageReactions.userId] = uid
                it[ChatMessageReactions.emoji] = emoji
            }
        }
        reactionsFor(listOf(mid))[mid].orEmpty()
    }

    // "Clear Data" cutoff for THIS viewer only (see ConversationState's doc
    // comment) - null if they've never cleared this conversation, or don't
    // have a ConversationState row for this friend at all yet.
    private fun clearedAtFor(uid: UUID, other: UUID): Long? =
        ConversationState.selectAll()
            .where { (ConversationState.userId eq uid) and (ConversationState.friendId eq other) }
            .firstOrNull()?.get(ConversationState.clearedAt)

    fun getMessages(userId: String, otherUserId: String, limit: Int = 200): List<ChatMessageDto> = transaction {
        val uid = UUID.fromString(userId)
        val other = UUID.fromString(otherUserId)
        if (!areFriends(uid, other)) throw AuthException("You can only view messages with accepted friends")
        val clearedAt = clearedAtFor(uid, other)

        val rows = ChatMessages.selectAll()
            .where {
                (((ChatMessages.senderId eq uid) and (ChatMessages.receiverId eq other)) or
                    ((ChatMessages.senderId eq other) and (ChatMessages.receiverId eq uid))) and
                    (if (clearedAt != null) ChatMessages.sentAt greater clearedAt else Op.TRUE)
            }
            .orderBy(ChatMessages.sentAt, SortOrder.DESC)
            .limit(limit)
            .toList()

        // Opening the thread no longer blanket-marks everything as read -
        // see markRead() below, called by the client as messages actually
        // scroll into view (Telegram-style), so a partially-read pile of
        // unread messages shrinks incrementally instead of being wiped out
        // the instant the thread is merely opened.
        val reactions = reactionsFor(rows.map { it[ChatMessages.id].value })
        val pollVotes = pollVotesFor(rows.filter { it[ChatMessages.pollOptions] != null }.map { it[ChatMessages.id].value })
        rows.map { row ->
            toDto(row, reactions[row[ChatMessages.id].value].orEmpty(), pollVotes[row[ChatMessages.id].value].orEmpty())
        }.reversed()
    }

    // Marks every message FROM otherUserId TO userId, up to and including
    // upToMessageId's own timestamp, as read - called as messages actually
    // scroll into view client-side (see MemberChatThreadScreen.kt), not
    // just once on thread-open. Silently ignores an id that isn't a real
    // message in this conversation rather than erroring - a stale/late
    // client call after e.g. a delete shouldn't break the read-marking flow.
    fun markRead(userId: String, otherUserId: String, upToMessageId: String) = transaction {
        val uid = UUID.fromString(userId)
        val other = UUID.fromString(otherUserId)
        val target = ChatMessages.selectAll().where { ChatMessages.id eq UUID.fromString(upToMessageId) }.firstOrNull() ?: return@transaction
        val cutoff = target[ChatMessages.sentAt]
        ChatMessages.update({
            (ChatMessages.senderId eq other) and (ChatMessages.receiverId eq uid) and
                (ChatMessages.readAt.isNull()) and (ChatMessages.sentAt lessEq cutoff)
        }) { it[readAt] = System.currentTimeMillis() }
    }

    // One unread count per sender, counting only messages TO uid that
    // haven't been read yet (deleted ones don't count - nothing left to
    // notify about), further limited by each sender's own "Clear Data"
    // cutoff (a message cleared from view shouldn't still count as unread).
    // Used by listConversations for the chat list badge.
    private fun unreadCountsFor(uid: UUID, clearedAtByFriend: Map<UUID, Long?>): Map<UUID, Int> =
        ChatMessages.selectAll()
            .where { (ChatMessages.receiverId eq uid) and (ChatMessages.readAt.isNull()) and (ChatMessages.deleted eq false) }
            .groupBy { it[ChatMessages.senderId].value }
            .mapValues { (senderId, rows) ->
                val cleared = clearedAtByFriend[senderId]
                if (cleared == null) rows.size else rows.count { it[ChatMessages.sentAt] > cleared }
            }

    // Shared by the default list, the Archived screen, and the (biometric-
    // gated) Hidden screen - mode selects which of the three buckets a
    // conversation belongs in. null = default view (excludes both);
    // "archived" = archived and not also hidden; "hidden" = hidden
    // (regardless of archived, since hidden is the stricter gate).
    private fun listConversationsInternal(userId: String, mode: String?): List<ConversationSummary> = transaction {
        val uid = UUID.fromString(userId)
        val blockedIds = Blocks.selectAll().where { Blocks.blockerId eq uid }.map { it[Blocks.blockedId].value }.toSet()
        // A blocked friend disappears from your own chat list entirely (see
        // Blocks' doc comment) - they still show as an accepted friend
        // elsewhere (friends list, profile) until formally unfriended.
        val friends = FriendService.listFriends(userId).filterNot { UUID.fromString(it.id) in blockedIds }

        val friendIds = friends.map { UUID.fromString(it.id) }.toSet()
        // Archive/pin/mute/favorite/lock/hide/clearedAt - see
        // ConversationState's own doc comment. A friend with no row here
        // just gets every flag false / clearedAt null.
        val states = ConversationState.selectAll()
            .where { ConversationState.userId eq uid }
            .associateBy { it[ConversationState.friendId].value }
        val clearedAtByFriend = friendIds.associateWith { states[it]?.get(ConversationState.clearedAt) }

        val lastByFriend = mutableMapOf<UUID, ResultRow>()
        if (friendIds.isNotEmpty()) {
            ChatMessages.selectAll()
                .where { (ChatMessages.senderId eq uid) or (ChatMessages.receiverId eq uid) }
                .orderBy(ChatMessages.sentAt, SortOrder.DESC)
                .forEach { row ->
                    val otherId = if (row[ChatMessages.senderId].value == uid) row[ChatMessages.receiverId].value else row[ChatMessages.senderId].value
                    if (otherId !in friendIds || otherId in lastByFriend) return@forEach
                    val cutoff = clearedAtByFriend[otherId]
                    if (cutoff == null || row[ChatMessages.sentAt] > cutoff) lastByFriend[otherId] = row
                }
        }
        val unread = unreadCountsFor(uid, clearedAtByFriend)

        val friendEntries = friends.mapNotNull { friend ->
            val friendUuid = UUID.fromString(friend.id)
            val state = states[friendUuid]
            val archived = state?.get(ConversationState.archived) == true
            val hidden = state?.get(ConversationState.hidden) == true
            val included = when (mode) {
                "archived" -> archived && !hidden
                "hidden" -> hidden
                else -> !archived && !hidden
            }
            if (!included) return@mapNotNull null
            val last = lastByFriend[friendUuid]
            ConversationSummary(
                friendId = friend.id,
                name = friend.name,
                email = friend.email,
                avatarUrl = friend.avatarUrl,
                // View-once previews never leak the real text here either -
                // the list screen shows a generic "sent/received a view
                // once" line instead (see lastMessageViewOnce client-side).
                lastMessage = last?.let {
                    when {
                        it[ChatMessages.deleted] -> "Message deleted"
                        it[ChatMessages.viewOnce] -> null
                        else -> it[ChatMessages.text]
                    }
                },
                lastMessageAt = last?.get(ChatMessages.sentAt),
                lastMessageFromMe = last?.let { it[ChatMessages.senderId].value == uid },
                lastMessageViewOnce = last?.get(ChatMessages.viewOnce) == true && !(last[ChatMessages.deleted]),
                unreadCount = unread[friendUuid] ?: 0,
                pinned = state?.get(ConversationState.pinned) == true,
                muted = state?.get(ConversationState.muted) == true,
                favorite = state?.get(ConversationState.favorite) == true,
                locked = state?.get(ConversationState.locked) == true,
            )
        }

        // The synthetic System Feed row only ever belongs in the default
        // view - it isn't a real friend conversation, so it can't be
        // archived/hidden/pinned etc.
        val entriesWithSystemFeed = if (mode != null) {
            friendEntries
        } else {
            val latestPost = SystemFeedPosts.selectAll()
                .orderBy(SystemFeedPosts.createdAt, SortOrder.DESC)
                .limit(1)
                .firstOrNull()
            friendEntries + ConversationSummary(
                friendId = SYSTEM_FEED_ID,
                name = "Cedal System Feed",
                lastMessage = latestPost?.get(SystemFeedPosts.text),
                lastMessageAt = latestPost?.get(SystemFeedPosts.createdAt),
                isSystemFeed = true,
                unreadCount = SystemFeedService.unreadCountFor(userId),
            )
        }

        // Pinned first (newest-pinned-first among themselves), then
        // everything else newest-first - same "pinned chats float to the
        // top" convention as every other chat app's pin feature.
        entriesWithSystemFeed.sortedWith(compareByDescending<ConversationSummary> { it.pinned }.thenByDescending { it.lastMessageAt ?: 0L })
    }

    // Groups are merged in here only (not the archived/hidden variants below
    // - groups don't support those states in v1) so the Chats tab shows one
    // unified, pinned-then-newest-first inbox across friends AND groups,
    // same precedent as the synthetic System Feed row above.
    fun listConversations(userId: String): List<ConversationSummary> =
        (listConversationsInternal(userId, null) + GroupChatService.listMyGroupSummaries(userId))
            .sortedWith(compareByDescending<ConversationSummary> { it.pinned }.thenByDescending { it.lastMessageAt ?: 0L })

    // "Archived" row pinned above the chat list (client scrolls up to reveal
    // it) - everything archived but not also hidden.
    fun listArchivedConversations(userId: String): List<ConversationSummary> = listConversationsInternal(userId, "archived")

    // Gated behind biometric/passcode client-side before this is ever
    // called (same verification as opening a locked chat) - server doesn't
    // re-check that here, matching how every other biometric gate in this
    // app (Switch Account, chat lock) is enforced client-side only.
    fun listHiddenConversations(userId: String): List<ConversationSummary> = listConversationsInternal(userId, "hidden")

    // Chat thread header ⋮ menu's Pin item needs to know the CURRENT pinned
    // state up front to show "Pin" vs "Unpin" - a single-conversation slice
    // of ConversationState rather than pulling the whole list.
    fun getPinnedState(userId: String, otherUserId: String): Boolean = transaction {
        val uid = UUID.fromString(userId)
        val other = UUID.fromString(otherUserId)
        ConversationState.selectAll()
            .where { (ConversationState.userId eq uid) and (ConversationState.friendId eq other) }
            .firstOrNull()?.get(ConversationState.pinned) == true
    }

    // The long-press multi-select menu (Delete/Archive/Clear Data/Pin/Mute/
    // Favorite/Lock/Hide) - one flexible endpoint rather than eight, since
    // it always needs to apply to potentially many selected chats at once.
    // Delete and Clear are both per-viewer only now, same as every other
    // flag here - NEITHER touches the shared ChatMessages rows, so the
    // other person's copy of the conversation is completely unaffected.
    // Delete additionally hides the conversation from your own list; Clear
    // only resets your own view of the message history, the (now-empty-to-
    // you) conversation stays visible.
    fun bulkAction(userId: String, friendIds: List<String>, action: String) = transaction {
        val uid = UUID.fromString(userId)
        friendIds.forEach { friendId ->
            val other = runCatching { UUID.fromString(friendId) }.getOrNull() ?: return@forEach
            if (!areFriends(uid, other)) return@forEach

            fun upsertState(field: org.jetbrains.exposed.sql.Column<Boolean>, value: Boolean) {
                val existing = ConversationState.selectAll().where { (ConversationState.userId eq uid) and (ConversationState.friendId eq other) }.firstOrNull()
                if (existing != null) {
                    ConversationState.update({ (ConversationState.userId eq uid) and (ConversationState.friendId eq other) }) {
                        it[field] = value
                    }
                } else {
                    ConversationState.insert {
                        it[ConversationState.userId] = uid
                        it[ConversationState.friendId] = other
                        it[field] = value
                    }
                }
            }

            fun clearForMeOnly() {
                val now = System.currentTimeMillis()
                val existing = ConversationState.selectAll().where { (ConversationState.userId eq uid) and (ConversationState.friendId eq other) }.firstOrNull()
                if (existing != null) {
                    ConversationState.update({ (ConversationState.userId eq uid) and (ConversationState.friendId eq other) }) {
                        it[clearedAt] = now
                    }
                } else {
                    ConversationState.insert {
                        it[ConversationState.userId] = uid
                        it[ConversationState.friendId] = other
                        it[clearedAt] = now
                    }
                }
            }

            when (action) {
                "delete" -> {
                    clearForMeOnly()
                    upsertState(ConversationState.hidden, true)
                }
                "clear" -> clearForMeOnly()
                "archive" -> upsertState(ConversationState.archived, true)
                "unarchive" -> upsertState(ConversationState.archived, false)
                "pin" -> upsertState(ConversationState.pinned, true)
                "unpin" -> upsertState(ConversationState.pinned, false)
                "mute" -> upsertState(ConversationState.muted, true)
                "unmute" -> upsertState(ConversationState.muted, false)
                "favorite" -> upsertState(ConversationState.favorite, true)
                "unfavorite" -> upsertState(ConversationState.favorite, false)
                "lock" -> upsertState(ConversationState.locked, true)
                "unlock" -> upsertState(ConversationState.locked, false)
                "hide" -> upsertState(ConversationState.hidden, true)
                "unhide" -> upsertState(ConversationState.hidden, false)
            }
        }
    }

    // Block/unblock (chat thread ⋮ menu "Block") - see Blocks' doc comment.
    fun setBlocked(userId: String, otherUserId: String, blocked: Boolean) = transaction {
        val uid = UUID.fromString(userId)
        val other = UUID.fromString(otherUserId)
        if (blocked) {
            val already = Blocks.selectAll().where { (Blocks.blockerId eq uid) and (Blocks.blockedId eq other) }.any()
            if (!already) {
                Blocks.insert {
                    it[blockerId] = uid
                    it[blockedId] = other
                }
                AchievementService.unlock(uid, "block_first")
                AchievementService.unlock(other, "blocked_first")
                if (AchievementService.countBlocksMade(uid) >= 20) AchievementService.unlock(uid, "block_20")
                if (AchievementService.countTimesBlocked(other) >= 20) AchievementService.unlock(other, "blocked_20")
                // Blocking also clears any friend request/friendship between
                // the two, either direction - a blocked user disappears from
                // requests entirely, not just chat/search (see
                // FriendService.search's blockedEitherDirection).
                FriendRequests.deleteWhere {
                    ((FriendRequests.fromUserId eq uid) and (FriendRequests.toUserId eq other)) or
                        ((FriendRequests.fromUserId eq other) and (FriendRequests.toUserId eq uid))
                }
            }
        } else {
            Blocks.deleteWhere { (Blocks.blockerId eq uid) and (Blocks.blockedId eq other) }
        }
    }

    fun isBlocked(userId: String, otherUserId: String): Boolean = transaction {
        val uid = UUID.fromString(userId)
        val other = UUID.fromString(otherUserId)
        Blocks.selectAll().where { (Blocks.blockerId eq uid) and (Blocks.blockedId eq other) }.any()
    }

    // "Report" (chat thread ⋮ menu) - reports the person/conversation as a
    // whole, distinct from reporting one specific message (see
    // reactToMessage's sibling reportMessage in MessageService). reason and
    // the evidence attachment are both optional - the reporter may just
    // want to flag someone with no further detail.
    fun reportUser(userId: String, otherUserId: String, reason: String?, mediaUrl: String?, mediaType: String?, fileName: String?) = transaction {
        UserReports.insert {
            it[reporterId] = UUID.fromString(userId)
            it[reportedId] = UUID.fromString(otherUserId)
            it[createdAt] = System.currentTimeMillis()
            it[UserReports.reason] = reason?.trim()?.take(2000)?.ifBlank { null }
            it[UserReports.mediaUrl] = mediaUrl
            it[UserReports.mediaType] = mediaType
            it[UserReports.fileName] = fileName
        }
    }

    // Admin Review (chat list > More) - admin-gated at the route level, see
    // requireAdmin in ChatRoutes.kt.
    fun listUserReports(): List<com.xhacker.cedal.models.UserReportDto> = transaction {
        UserReports.selectAll()
            .where { UserReports.status eq "pending" }
            .orderBy(UserReports.createdAt, SortOrder.DESC)
            .map { row ->
                val reporter = Users.selectAll().where { Users.id eq row[UserReports.reporterId] }.firstOrNull()
                val reported = Users.selectAll().where { Users.id eq row[UserReports.reportedId] }.firstOrNull()
                com.xhacker.cedal.models.UserReportDto(
                    id = row[UserReports.id].value.toString(),
                    reporterId = row[UserReports.reporterId].value.toString(),
                    reporterName = reporter?.let { displayNameFor(it) } ?: "Unknown",
                    reportedId = row[UserReports.reportedId].value.toString(),
                    reportedName = reported?.let { displayNameFor(it) } ?: "Unknown",
                    reason = row[UserReports.reason],
                    mediaUrl = row[UserReports.mediaUrl],
                    mediaType = row[UserReports.mediaType],
                    fileName = row[UserReports.fileName],
                    createdAt = row[UserReports.createdAt],
                )
            }
    }

    fun dismissUserReport(reportId: String) = transaction {
        UserReports.update({ UserReports.id eq UUID.fromString(reportId) }) { it[status] = "reviewed" }
    }

    // "Delete Chat" (header ⋮ menu) - a real hard delete of the whole
    // conversation, not a per-side hide - both accounts lose the history,
    // same as it disappearing from both phones in most chat apps' "delete
    // chat" (as opposed to WhatsApp's separate, unbuilt "delete for me").
    fun deleteConversation(userId: String, otherUserId: String) = transaction {
        val uid = UUID.fromString(userId)
        val other = UUID.fromString(otherUserId)
        if (!areFriends(uid, other)) throw AuthException("You can only manage chats with accepted friends")

        val ids = ChatMessages.selectAll()
            .where {
                ((ChatMessages.senderId eq uid) and (ChatMessages.receiverId eq other)) or
                    ((ChatMessages.senderId eq other) and (ChatMessages.receiverId eq uid))
            }
            .map { it[ChatMessages.id].value }
        if (ids.isNotEmpty()) {
            ChatMessageReactions.deleteWhere { ChatMessageReactions.messageId inList ids }
            ChatMessages.deleteWhere { ChatMessages.id inList ids }
        }
    }
}
