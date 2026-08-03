package com.xhacker.cedal.routes

import com.xhacker.cedal.models.SetAppVersionRequest
import com.xhacker.cedal.services.AdminService
import com.xhacker.cedal.services.AppVersionService
import com.xhacker.cedal.services.AuthException
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

// Client force-update gate - see AppVersionConfig's own doc comment.
// Unauthenticated GET so even a logged-out/gated client can still check.
fun Route.appVersionRoutes() {
    get("/app-version") {
        // versionCode=0 (no config set yet) is always "up to date" client-
        // side - any real installed app has a versionCode >= 1 - so this
        // never accidentally gates anyone before the admin has actually
        // set a real minimum once.
        val current = AppVersionService.get() ?: com.xhacker.cedal.models.AppVersionDto(versionCode = 0, versionName = "")
        call.respond(HttpStatusCode.OK, current)
    }
    // Fired when a user dismisses the update banner ("✕") - a permanent
    // audit record (see Users.declinedUpdateVersionCode), not a real
    // preference toggle, so this is intentionally the only way to set it
    // and there is no corresponding un-decline/clear endpoint.
    authenticate("auth-jwt") {
        post("/app-version/decline") {
            val userId = call.principal<JWTPrincipal>()!!.payload.subject
            val req = call.receive<com.xhacker.cedal.models.DeclineUpdateRequest>()
            AppVersionService.recordDeclinedUpdate(userId, req.versionCode)
            call.respond(HttpStatusCode.OK, mapOf("ok" to true))
        }
    }
    route("/admin/app-version") {
        authenticate("auth-jwt") {
            post {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                if (!AdminService.isAdmin(userId)) throw AuthException("Admins only")
                val req = call.receive<SetAppVersionRequest>()
                AppVersionService.set(req.versionCode, req.versionName, req.apkUrl, req.changelog)
                call.respond(HttpStatusCode.OK, mapOf("ok" to true))
            }
        }
    }
}
