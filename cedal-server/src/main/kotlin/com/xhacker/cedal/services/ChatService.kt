package com.xhacker.cedal.services

import com.xhacker.cedal.db.ChatMessageReactions
import com.xhacker.cedal.db.ChatMessages
import com.xhacker.cedal.db.FriendRequests
import com.xhacker.cedal.db.PollVotes
import com.xhacker.cedal.db.SystemFeedPosts
import com.xhacker.cedal.db.Users
import com.xhacker.cedal.models.ChatMessageDto
import com.xhacker.cedal.models.ConversationSummary
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
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
        // A view-once message's real content is hidden from everyone -
        // including the sender - until the recipient explicitly reveals it
        // (see revealMessage()); the locked bubble is a generic textured
        // card (see MessageBubble/ViewOnceLockedCard), not a render of the
        // real content, so there's no need to deliver it early.
        val hideContent = isDeleted || (viewOnce && !viewed)
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
            pollQuestion = if (isDeleted) null else row[ChatMessages.pollQuestion],
            pollOptions = if (isDeleted) null else row[ChatMessages.pollOptions]?.split("\n"),
            pollVotes = if (isDeleted) emptyMap() else pollVotes,
        )
    }

    fun sendMessage(
        fromUserId: String,
        toUserId: String,
        text: String,
        replyToId: String?,
        isSticker: Boolean = false,
        mediaUrl: String? = null,
        mediaType: String? = null,
        fileName: String? = null,
        viewOnce: Boolean = false,
        pollQuestion: String? = null,
        pollOptions: List<String>? = null,
    ): ChatMessageDto = transaction {
        val from = UUID.fromString(fromUserId)
        val to = UUID.fromString(toUserId)
        if (from == to) throw AuthException("Can't message yourself")
        if (!areFriends(from, to)) throw AuthException("You can only message accepted friends")

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

        val sentAt = System.currentTimeMillis()
        val id = ChatMessages.insertAndGetId {
            it[senderId] = from
            it[receiverId] = to
            it[ChatMessages.text] = trimmed
            it[ChatMessages.sentAt] = sentAt
            it[ChatMessages.replyToId] = validReply
            it[ChatMessages.isSticker] = isSticker
            it[ChatMessages.mediaUrl] = mediaUrl
            it[ChatMessages.mediaType] = mediaType
            it[ChatMessages.fileName] = fileName
            it[ChatMessages.viewOnce] = viewOnce
            it[ChatMessages.pollQuestion] = pollQuestion
            it[ChatMessages.pollOptions] = cleanOptions?.joinToString("\n")
        }
        ChatMessageDto(
            id.value.toString(), fromUserId, toUserId, trimmed, sentAt,
            replyToId = validReply?.toString(), isSticker = isSticker,
            mediaUrl = mediaUrl, mediaType = mediaType, fileName = fileName, viewOnce = viewOnce,
            pollQuestion = pollQuestion, pollOptions = cleanOptions,
        )
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

    // Only the recipient can reveal a view-once message - flips the
    // permanent viewedAt flag (see toDto's viewed field) so every client
    // stops blurring it, on this device and any other. Calling it again
    // once already viewed is harmless - just returns the same DTO.
    fun revealMessage(userId: String, messageId: String): ChatMessageDto = transaction {
        val uid = UUID.fromString(userId)
        val mid = UUID.fromString(messageId)
        val row = ChatMessages.selectAll().where { ChatMessages.id eq mid }.firstOrNull() ?: throw AuthException("Message not found")
        if (row[ChatMessages.receiverId].value != uid) throw AuthException("Only the recipient can reveal this message")
        if (!row[ChatMessages.viewOnce]) throw AuthException("This message isn't view-once")

        if (row[ChatMessages.viewedAt] == null) {
            ChatMessages.update({ ChatMessages.id eq mid }) { it[viewedAt] = System.currentTimeMillis() }
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
        )
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

    fun getMessages(userId: String, otherUserId: String, limit: Int = 200): List<ChatMessageDto> = transaction {
        val uid = UUID.fromString(userId)
        val other = UUID.fromString(otherUserId)
        if (!areFriends(uid, other)) throw AuthException("You can only view messages with accepted friends")

        val rows = ChatMessages.selectAll()
            .where {
                ((ChatMessages.senderId eq uid) and (ChatMessages.receiverId eq other)) or
                    ((ChatMessages.senderId eq other) and (ChatMessages.receiverId eq uid))
            }
            .orderBy(ChatMessages.sentAt, SortOrder.DESC)
            .limit(limit)
            .toList()

        // Opening the thread is what marks their messages to you as read -
        // drives the unread-count badge back on the chat list (see
        // listConversations/unreadCountsFor). Only ever touches messages
        // FROM them TO you, obviously never your own.
        ChatMessages.update({
            (ChatMessages.senderId eq other) and (ChatMessages.receiverId eq uid) and (ChatMessages.readAt.isNull())
        }) { it[readAt] = System.currentTimeMillis() }

        val reactions = reactionsFor(rows.map { it[ChatMessages.id].value })
        val pollVotes = pollVotesFor(rows.filter { it[ChatMessages.pollOptions] != null }.map { it[ChatMessages.id].value })
        rows.map { row ->
            toDto(row, reactions[row[ChatMessages.id].value].orEmpty(), pollVotes[row[ChatMessages.id].value].orEmpty())
        }.reversed()
    }

    // One unread count per sender, counting only messages TO uid that
    // haven't been read yet (deleted ones don't count - nothing left to
    // notify about). Used by listConversations for the chat list badge.
    private fun unreadCountsFor(uid: UUID): Map<UUID, Int> =
        ChatMessages.selectAll()
            .where { (ChatMessages.receiverId eq uid) and (ChatMessages.readAt.isNull()) and (ChatMessages.deleted eq false) }
            .groupBy { it[ChatMessages.senderId].value }
            .mapValues { it.value.size }

    // One row per accepted friend plus a synthetic "Cedal System Feed" row,
    // all newest-conversation-first - friends with no messages yet still
    // show up (lastMessage null), same as opening a fresh DM in most chat
    // apps. System Feed sorts by its own latest post instead of being
    // pinned above everything, so real conversations naturally take the top
    // spot again as soon as they're more recent.
    fun listConversations(userId: String): List<ConversationSummary> = transaction {
        val uid = UUID.fromString(userId)
        val friends = FriendService.listFriends(userId)

        val friendIds = friends.map { UUID.fromString(it.id) }.toSet()
        val lastByFriend = mutableMapOf<UUID, ResultRow>()
        if (friendIds.isNotEmpty()) {
            ChatMessages.selectAll()
                .where { (ChatMessages.senderId eq uid) or (ChatMessages.receiverId eq uid) }
                .orderBy(ChatMessages.sentAt, SortOrder.DESC)
                .forEach { row ->
                    val otherId = if (row[ChatMessages.senderId].value == uid) row[ChatMessages.receiverId].value else row[ChatMessages.senderId].value
                    if (otherId in friendIds && otherId !in lastByFriend) lastByFriend[otherId] = row
                }
        }
        val unread = unreadCountsFor(uid)

        val friendEntries = friends.map { friend ->
            val last = lastByFriend[UUID.fromString(friend.id)]
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
                unreadCount = unread[UUID.fromString(friend.id)] ?: 0,
            )
        }

        val latestPost = SystemFeedPosts.selectAll()
            .orderBy(SystemFeedPosts.createdAt, SortOrder.DESC)
            .limit(1)
            .firstOrNull()
        val systemFeedEntry = ConversationSummary(
            friendId = SYSTEM_FEED_ID,
            name = "Cedal System Feed",
            lastMessage = latestPost?.get(SystemFeedPosts.text),
            lastMessageAt = latestPost?.get(SystemFeedPosts.createdAt),
            isSystemFeed = true,
            unreadCount = SystemFeedService.unreadCountFor(userId),
        )

        (friendEntries + systemFeedEntry).sortedByDescending { it.lastMessageAt ?: 0L }
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
