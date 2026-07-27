package com.xhacker.cedal.routes

import com.xhacker.cedal.services.BanEscalationService
import com.xhacker.cedal.services.DecayService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.adminRoutes() {
    route("/admin") {
        // Triggered by a Cloud Scheduler job, not a logged-in user - gated by
        // a shared secret the same way android-builder's callback is, not a
        // user JWT. Safe to call more often than every 3 months: it only
        // actually decays an account once that account's own 3 months are
        // up (see DecayService.isDecayDue), so a daily schedule just means
        // decay happens within a day of being due, not that it double-fires.
        post("/run-decay") {
            val expected = "Bearer ${System.getenv("DECAY_SECRET") ?: ""}"
            val header = call.request.headers["Authorization"] ?: ""
            if (header.isEmpty() || header != expected) {
                call.respond(HttpStatusCode.Unauthorized)
                return@post
            }
            val affected = DecayService.runDueDecays()
            call.respond(HttpStatusCode.OK, mapOf("accountsDecayed" to affected))
        }
        // Same shared-secret pattern - see BanEscalationService's own doc
        // comment for the 24h temp-ban -> permanent rule.
        post("/run-ban-escalation") {
            val expected = "Bearer ${System.getenv("DECAY_SECRET") ?: ""}"
            val header = call.request.headers["Authorization"] ?: ""
            if (header.isEmpty() || header != expected) {
                call.respond(HttpStatusCode.Unauthorized)
                return@post
            }
            val affected = BanEscalationService.runDueEscalations()
            call.respond(HttpStatusCode.OK, mapOf("bansEscalated" to affected))
        }
    }
}
