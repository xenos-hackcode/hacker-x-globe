package com.xhacker.cedal.ui

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.xhacker.cedal.MESSAGES_NOTIFICATION_CHANNEL_ID
import com.xhacker.cedal.MainActivity
import com.xhacker.cedal.data.ConversationSummary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Polls every conversation (1-on-1 and group) while any member-area screen
// is open, same "persistent in-memory singleton, not a true background
// service" pattern as FriendRequestSession - see that file's own doc
// comment for why (mirrors CodeRunSession, stops once the app process
// dies - there's no real push infrastructure in this app, everything is
// pull/poll while the app is alive).
object MessageNotificationSession {
    private var currentUserId: String? = null
    private var fetchFn: (suspend () -> Result<List<ConversationSummary>>)? = null
    private var contextRef: Context? = null

    // Per-account "last message timestamp we've already notified about",
    // keyed by conversation id (friendId, or groupId when isGroup) - a
    // fresh timestamp higher than what's stored here is what "new message"
    // means. Per-account for the same Switch Account reason
    // FriendRequestSession's dedup sets are.
    private val lastNotifiedAtByUser = mutableMapOf<String, MutableMap<String, Long>>()
    private val baselineSeededUsers = mutableSetOf<String>()

    private var pollJob: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    fun activate(context: Context, userId: String?, fetch: suspend () -> Result<List<ConversationSummary>>) {
        contextRef = context.applicationContext
        fetchFn = fetch
        if (userId != currentUserId) currentUserId = userId
        scope.launch { pollOnce() }
        if (pollJob == null) {
            pollJob = scope.launch {
                while (true) {
                    // Faster than FriendRequestSession's 30s - a new message
                    // is more time-sensitive than a friend request.
                    delay(15_000)
                    pollOnce()
                }
            }
        }
    }

    private suspend fun pollOnce() {
        val fetch = fetchFn ?: return
        val uid = currentUserId
        fetch().onSuccess { conversations -> if (uid == currentUserId) handle(uid, conversations) }
    }

    private fun handle(userId: String?, conversations: List<ConversationSummary>) {
        val uid = userId ?: return
        val lastNotifiedAt = lastNotifiedAtByUser.getOrPut(uid) { mutableMapOf() }

        // Only real incoming messages are candidates at all - our own sent
        // messages (lastMessageFromMe) and muted conversations never
        // notify, same as this data already means everywhere else in the app.
        val candidates = conversations.filter { it.lastMessageFromMe != true && !it.muted && it.lastMessageAt != null }

        if (uid !in baselineSeededUsers) {
            // First poll ever for THIS account - records what already
            // existed (including any unread backlog) without notifying for
            // it, same convention as FriendRequestSession's baseline seed.
            candidates.forEach { lastNotifiedAt[it.friendId] = it.lastMessageAt!! }
            baselineSeededUsers += uid
            return
        }

        val ctx = contextRef ?: return
        if (uid != currentUserId) return
        candidates.forEach { convo ->
            val at = convo.lastMessageAt!!
            if (at > (lastNotifiedAt[convo.friendId] ?: 0L)) {
                lastNotifiedAt[convo.friendId] = at
                notifyNewMessage(ctx, convo)
            }
        }
    }

    private fun hasNotificationPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED

    // DMs deep-link straight into that chat thread via the same mechanism
    // per-chat "Add Shortcut" already uses (see OpenChatDeepLinkState) -
    // groups just open the app generically since no equivalent
    // open-this-specific-group deep link exists yet.
    private fun openChatIntent(context: Context, convo: ConversationSummary): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
        if (!convo.isGroup) {
            intent.putExtra("open_chat_friend_id", convo.friendId)
            intent.putExtra("open_chat_friend_name", convo.name)
        }
        return PendingIntent.getActivity(
            context, convo.friendId.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun notifyNewMessage(context: Context, convo: ConversationSummary) {
        if (!hasNotificationPermission(context)) return
        // Same "don't reveal the content" instinct as everywhere else
        // View Once shows up in a preview - the point of View Once is that
        // it's not just sitting there readable.
        val preview = if (convo.lastMessageViewOnce) "Sent a view-once message" else convo.lastMessage.orEmpty()
        val notification = NotificationCompat.Builder(context, MESSAGES_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_add)
            .setContentTitle(convo.name)
            .setContentText(preview)
            .setAutoCancel(true)
            .setContentIntent(openChatIntent(context, convo))
            .build()
        // Grouped per-conversation (a fixed base offset + a stable per-
        // conversation hash) so a new message from person A updates/
        // replaces their own prior notification instead of either
        // stacking indefinitely or overwriting an unrelated person B's.
        NotificationManagerCompat.from(context).notify(3000 + (convo.friendId.hashCode() and 0xFFFF), notification)
        com.xhacker.cedal.util.NotificationSound.playIfEnabled(context)
    }
}
