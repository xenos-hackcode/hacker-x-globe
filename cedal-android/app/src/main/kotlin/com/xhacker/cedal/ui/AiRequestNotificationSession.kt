package com.xhacker.cedal.ui

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.xhacker.cedal.MESSAGES_NOTIFICATION_CHANNEL_ID
import com.xhacker.cedal.MainActivity
import com.xhacker.cedal.data.AiChangeRequestDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Tells the requester the moment the admin actually acts on one of their
// Code AI "add a language" requests (approved+deploying, deployed,
// rejected, or a deploy error), instead of them only finding out next time
// they happen to reopen Code > Rules - same pattern as
// FriendRequestSession/MessageNotificationSession (persistent in-memory
// singleton, polls while any member-area screen is open, no true
// background push). Deliberately does NOT notify for "pending_judgment"/
// "answered"/"pending_approval" - those already show up live in-chat while
// the user is actively composing, or (for pending_approval) are the
// "sent, now waiting" state, not something new that just happened.
object AiRequestNotificationSession {
    private val NOTIFY_STATUSES = setOf("deploying", "deployed", "rejected", "error")

    private var currentUserId: String? = null
    private var fetchFn: (suspend () -> Result<List<AiChangeRequestDto>>)? = null
    private var contextRef: Context? = null

    private val lastStatusByUser = mutableMapOf<String, MutableMap<String, String>>()
    private val baselineSeededUsers = mutableSetOf<String>()

    private var pollJob: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    fun activate(context: Context, userId: String?, fetch: suspend () -> Result<List<AiChangeRequestDto>>) {
        contextRef = context.applicationContext
        fetchFn = fetch
        if (userId != currentUserId) currentUserId = userId
        scope.launch { pollOnce() }
        if (pollJob == null) {
            pollJob = scope.launch {
                while (true) {
                    delay(15_000)
                    pollOnce()
                }
            }
        }
    }

    private suspend fun pollOnce() {
        val fetch = fetchFn ?: return
        val uid = currentUserId
        fetch().onSuccess { requests -> if (uid == currentUserId) handle(uid, requests) }
    }

    private fun handle(userId: String?, requests: List<AiChangeRequestDto>) {
        val uid = userId ?: return
        val lastStatus = lastStatusByUser.getOrPut(uid) { mutableMapOf() }
        val candidates = requests.filter { it.status in NOTIFY_STATUSES }

        if (uid !in baselineSeededUsers) {
            // First poll ever for THIS account - records whatever state
            // already existed without notifying for it.
            candidates.forEach { lastStatus[it.id] = it.status }
            baselineSeededUsers += uid
            return
        }

        val ctx = contextRef ?: return
        if (uid != currentUserId) return
        candidates.forEach { req ->
            if (lastStatus[req.id] != req.status) {
                lastStatus[req.id] = req.status
                notifyStatusChange(ctx, req)
            }
        }
    }

    private fun hasNotificationPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED

    // Same phrasing as MemberCodeScreen's toBubbles() (the in-chat text for
    // each status) so the notification and the chat bubble it's about
    // never disagree with each other.
    private fun bodyFor(req: AiChangeRequestDto): String = when (req.status) {
        "deploying" -> "Approved! ${req.summary ?: "This"} is being deployed now - almost there."
        "deployed" -> "Confirmed - ${req.summary ?: "this"} was approved and is now live. Try it!"
        "rejected" -> "This request was denied by the admin: ${req.summary ?: "no reason given"}."
        "error" -> "Something went wrong: ${req.errorMessage ?: req.summary ?: "unknown error"}"
        else -> req.summary ?: "Your request status changed."
    }

    private fun titleFor(req: AiChangeRequestDto): String = when (req.status) {
        "deploying" -> "Code AI: request approved"
        "deployed" -> "Code AI: now live"
        "rejected" -> "Code AI: request denied"
        "error" -> "Code AI: deploy failed"
        else -> "Code AI"
    }

    private fun notifyStatusChange(context: Context, req: AiChangeRequestDto) {
        if (!hasNotificationPermission(context)) return
        val notification = NotificationCompat.Builder(context, MESSAGES_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_add)
            .setContentTitle(titleFor(req))
            .setContentText(bodyFor(req))
            .setStyle(NotificationCompat.BigTextStyle().bigText(bodyFor(req)))
            .setAutoCancel(true)
            .setContentIntent(
                PendingIntent.getActivity(
                    context, req.id.hashCode(), Intent(context, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                ),
            )
            .build()
        NotificationManagerCompat.from(context).notify(4000 + (req.id.hashCode() and 0xFFFF), notification)
        com.xhacker.cedal.util.NotificationSound.playIfEnabled(context)
    }
}
