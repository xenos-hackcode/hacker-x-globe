package com.xhacker.cedal.routes

import com.xhacker.cedal.services.PlatformDeveloperService
import com.xhacker.cedal.services.SmsRelayService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

// Polled by the "cedal-sms-relay" companion app - either the owner's own
// phone (legacy SMS_RELAY_SECRET -> the global PendingSmsJobs queue,
// completely unchanged from before) or, now, any platform developer's own
// phone (their own issued key -> ONLY their own PlatformSmsJobs rows - see
// PlatformDeveloperService). The relay app itself needs no changes either
// way - same two routes, same Bearer-token shape, whichever kind of secret
// gets sent.
fun Route.smsRelayRoutes() {
    route("/sms-relay") {
        get("/pending") {
            val header = call.request.headers["Authorization"] ?: ""
            val expected = "Bearer ${System.getenv("SMS_RELAY_SECRET") ?: ""}"
            if (header.isNotEmpty() && header == expected) {
                call.respond(HttpStatusCode.OK, SmsRelayService.listPending())
                return@get
            }
            val developerId = header.removePrefix("Bearer ").trim().let { PlatformDeveloperService.verifyDeveloperToken(it) }
            if (developerId != null) {
                call.respond(HttpStatusCode.OK, PlatformDeveloperService.listPendingFor(developerId))
                return@get
            }
            call.respond(HttpStatusCode.Unauthorized)
        }
        post("/{id}/sent") {
            val id = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest)
            val header = call.request.headers["Authorization"] ?: ""
            val expected = "Bearer ${System.getenv("SMS_RELAY_SECRET") ?: ""}"
            if (header.isNotEmpty() && header == expected) {
                SmsRelayService.markSent(id)
                call.respond(HttpStatusCode.OK, mapOf("ok" to true))
                return@post
            }
            val developerId = header.removePrefix("Bearer ").trim().let { PlatformDeveloperService.verifyDeveloperToken(it) }
            if (developerId != null) {
                PlatformDeveloperService.markSentFor(developerId, id)
                call.respond(HttpStatusCode.OK, mapOf("ok" to true))
                return@post
            }
            call.respond(HttpStatusCode.Unauthorized)
        }
    }
}
