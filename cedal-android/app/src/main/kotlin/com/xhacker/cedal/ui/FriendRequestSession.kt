package com.xhacker.cedal.ui

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.xhacker.cedal.FRIENDS_NOTIFICATION_CHANNEL_ID
import com.xhacker.cedal.MainActivity
import com.xhacker.cedal.data.FriendRequestItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Polls incoming friend requests while any member-area screen is open (see
// MemberRoute), so the "+" button in Chats reflects the CURRENTLY ACTIVE
// account's real state and a real notification fires for it - mirrors
// CodeRunSession's pattern (a persistent in-memory singleton, not a true
// background service; stops once the app process itself dies).
//
// Everything here is keyed by userId, not just a single flat flag - with
// multiple accounts saved on one device (see Switch Account), state from
// whichever account you switched AWAY from must never bleed into the one
// you're on now. activate() is called every time MemberRoute (re)composes,
// which happens fresh after every account switch (it always lands on a
// brand-new member_home) - that's what resets the indicator instantly
// instead of waiting up to 30s for the next scheduled poll.
object FriendRequestSession {
    enum class Indicator { NONE, ACCEPTED, PENDING }

    // PENDING (red, someone's waiting on you) always outranks ACCEPTED
    // (green, good news but no action needed) - see handle().
    var indicator by mutableStateOf(Indicator.NONE)
        private set

    private var currentUserId: String? = null
    private var fetchFn: (suspend () -> Result<List<FriendRequestItem>>)? = null
    private var contextRef: Context? = null

    // Per-account dedup sets - a request/acceptance already notified about
    // for account A must still notify separately the first time account B
    // ever sees it (different owner, different "have I seen this" history).
    private val notifiedIncomingIds = mutableMapOf<String, MutableSet<String>>()
    private val notifiedAcceptedIds = mutableMapOf<String, MutableSet<String>>()
    private val baselineSeededUsers = mutableSetOf<String>()
    // Unlike the notification sets above, this one deliberately does NOT
    // get baseline-seeded - an accepted request that's been sitting there
    // since before this session started should still show green until you
    // actually open the "+" screen and see it (see markAcceptedSeen()).
    private val acknowledgedAcceptedIds = mutableMapOf<String, MutableSet<String>>()

    private var pollJob: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    fun activate(context: Context, userId: String?, fetch: suspend () -> Result<List<FriendRequestItem>>) {
        contextRef = context.applicationContext
        fetchFn = fetch
        if (userId != currentUserId) {
            currentUserId = userId
            indicator = Indicator.NONE // don't show the previous account's state even for an instant
        }
        scope.launch { pollOnce() }
        if (pollJob == null) {
            pollJob = scope.launch {
                while (true) {
                    delay(30_000)
                    pollOnce()
                }
            }
        }
    }

    // Called when the "+" screen (search/requests) is opened - clears the
    // green "accepted" indicator for whatever's accepted right now, same as
    // opening a notification tray clears its badge.
    fun markAcceptedSeen(acceptedRequestIds: Collection<String>) {
        val uid = currentUserId ?: return
        acknowledgedAcceptedIds.getOrPut(uid) { mutableSetOf() }.addAll(acceptedRequestIds)
        recomputeIndicator(uid)
    }

    private suspend fun pollOnce() {
        val fetch = fetchFn ?: return
        val uid = currentUserId
        fetch().onSuccess { requests -> if (uid == currentUserId) handle(uid, requests) }
    }

    private fun handle(userId: String?, requests: List<FriendRequestItem>) {
        val uid = userId ?: return
        val incomingPending = requests.filter { it.direction == "incoming" && it.status == "pending" }
        val acceptedOutgoing = requests.filter { it.direction == "outgoing" && it.status == "accepted" }

        val notifiedIncoming = notifiedIncomingIds.getOrPut(uid) { mutableSetOf() }
        val notifiedAccepted = notifiedAcceptedIds.getOrPut(uid) { mutableSetOf() }

        if (uid !in baselineSeededUsers) {
            // First poll ever for THIS account - establishes what already
            // existed, doesn't fire a notification backlog for it.
            notifiedIncoming += incomingPending.map { it.id }
            notifiedAccepted += acceptedOutgoing.map { it.id }
            baselineSeededUsers += uid
        } else {
            val ctx = contextRef
            incomingPending.filter { it.id !in notifiedIncoming }.forEach { req ->
                notifiedIncoming += req.id
                if (ctx != null && uid == currentUserId) notifyNewRequest(ctx, req)
            }
            acceptedOutgoing.filter { it.id !in notifiedAccepted }.forEach { req ->
                notifiedAccepted += req.id
                if (ctx != null && uid == currentUserId) notifyAccepted(ctx, req)
            }
        }

        // Last known set for THIS account's "still pending"/"still
        // unacknowledged" state, used by recomputeIndicator below.
        pendingIncomingByUser[uid] = incomingPending.map { it.id }.toSet()
        unacknowledgedAcceptedByUser[uid] = acceptedOutgoing.map { it.id }.toSet()
        if (uid == currentUserId) recomputeIndicator(uid)
    }

    private val pendingIncomingByUser = mutableMapOf<String, Set<String>>()
    private val unacknowledgedAcceptedByUser = mutableMapOf<String, Set<String>>()

    private fun recomputeIndicator(uid: String) {
        val hasPending = pendingIncomingByUser[uid]?.isNotEmpty() == true
        val acknowledged = acknowledgedAcceptedIds[uid].orEmpty()
        val hasUnseenAccepted = unacknowledgedAcceptedByUser[uid]?.any { it !in acknowledged } == true
        indicator = when {
            hasPending -> Indicator.PENDING
            hasUnseenAccepted -> Indicator.ACCEPTED
            else -> Indicator.NONE
        }
    }

    private const val REQUEST_NOTIFICATION_ID = 2001
    private const val ACCEPTED_NOTIFICATION_ID = 2002

    private fun hasNotificationPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED

    private fun openAppIntent(context: Context): PendingIntent = PendingIntent.getActivity(
        context, 0, Intent(context, MainActivity::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun notifyNewRequest(context: Context, req: FriendRequestItem) {
        if (!hasNotificationPermission(context)) return
        val notification = NotificationCompat.Builder(context, FRIENDS_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_add)
            .setContentTitle("New friend request")
            .setContentText("You have a new friend suggestion from ${req.name}.")
            .setAutoCancel(true)
            .setContentIntent(openAppIntent(context))
            .build()
        NotificationManagerCompat.from(context).notify(REQUEST_NOTIFICATION_ID, notification)
        com.xhacker.cedal.util.NotificationSound.playIfEnabled(context)
    }

    private fun notifyAccepted(context: Context, req: FriendRequestItem) {
        if (!hasNotificationPermission(context)) return
        val notification = NotificationCompat.Builder(context, FRIENDS_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_add)
            .setContentTitle("Friend request accepted")
            .setContentText("${req.name} accepted your friend request - you can chat now.")
            .setAutoCancel(true)
            .setContentIntent(openAppIntent(context))
            .build()
        NotificationManagerCompat.from(context).notify(ACCEPTED_NOTIFICATION_ID, notification)
        com.xhacker.cedal.util.NotificationSound.playIfEnabled(context)
    }
}
