package com.xhacker.cedal.routes

import com.xhacker.cedal.models.LinkEmailRequest
import com.xhacker.cedal.models.NumberShareOverrideResponse
import com.xhacker.cedal.models.PhoneStatusResponse
import com.xhacker.cedal.models.RequestPhoneCodeRequest
import com.xhacker.cedal.models.RequestPhoneCodeResponse
import com.xhacker.cedal.models.SetNumberShareOverrideRequest
import com.xhacker.cedal.models.TermsUpdateRequest
import com.xhacker.cedal.models.TwoFactorConfirmRequest
import com.xhacker.cedal.models.UpdatePasscodeRequest
import com.xhacker.cedal.models.UpdateProfileRequest
import com.xhacker.cedal.models.VerifyPhoneCodeRequest
import com.xhacker.cedal.services.AccountService
import com.xhacker.cedal.services.AuthService
import com.xhacker.cedal.services.CallService
import com.xhacker.cedal.services.SecurityService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.selectAll

fun Route.userRoutes() {
    route("/users") {
        authenticate("auth-jwt") {
            get("/{id}") {
                val id = call.parameters["id"]!!
                val viewerId = call.principal<JWTPrincipal>()!!.payload.subject
                call.respond(HttpStatusCode.OK, AuthService.getProfile(id, viewerId))
            }
            put("/{id}") {
                val id = call.parameters["id"]!!
                val principal = call.principal<JWTPrincipal>()!!
                if (principal.payload.subject != id) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Cannot edit another user's profile"))
                    return@put
                }
                val req = call.receive<UpdateProfileRequest>()
                call.respond(HttpStatusCode.OK, AuthService.updateProfile(id, req))
            }
            // "Known" calling per-friend override - id here is the FRIEND
            // being granted/revoked access, not the caller (contrast every
            // other PUT /users/{id} route above, which is strictly self-only)
            // - see CallService.setOverride/getOverride.
            get("/{id}/number-share") {
                val friendId = call.parameters["id"]!!
                val ownerId = call.principal<JWTPrincipal>()!!.payload.subject
                call.respond(HttpStatusCode.OK, NumberShareOverrideResponse(CallService.getOverride(ownerId, friendId)))
            }
            put("/{id}/number-share") {
                val friendId = call.parameters["id"]!!
                val ownerId = call.principal<JWTPrincipal>()!!.payload.subject
                val req = call.receive<SetNumberShareOverrideRequest>()
                CallService.setOverride(ownerId, friendId, req.allowed)
                call.respond(HttpStatusCode.OK, mapOf("ok" to true))
            }
            put("/{id}/terms") {
                val id = call.parameters["id"]!!
                val principal = call.principal<JWTPrincipal>()!!
                if (principal.payload.subject != id) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Cannot edit another user's profile"))
                    return@put
                }
                val req = call.receive<TermsUpdateRequest>()
                call.respond(HttpStatusCode.OK, AuthService.updateTermsAcceptance(id, req.version))
            }
            put("/{id}/passcode") {
                val id = call.parameters["id"]!!
                val principal = call.principal<JWTPrincipal>()!!
                if (principal.payload.subject != id) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Cannot edit another user's profile"))
                    return@put
                }
                val req = call.receive<UpdatePasscodeRequest>()
                AuthService.updatePasscode(id, req.code)
                call.respond(HttpStatusCode.OK, mapOf("updated" to true))
            }
            put("/{id}/link-email") {
                val id = call.parameters["id"]!!
                val principal = call.principal<JWTPrincipal>()!!
                if (principal.payload.subject != id) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Cannot edit another user's profile"))
                    return@put
                }
                val req = call.receive<LinkEmailRequest>()
                call.respond(HttpStatusCode.OK, AuthService.linkGuestToEmail(id, req.email, req.password))
            }
            post("/{id}/2fa/request") {
                val id = call.parameters["id"]!!
                val principal = call.principal<JWTPrincipal>()!!
                if (principal.payload.subject != id) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Cannot edit another user's profile"))
                    return@post
                }
                val code = AuthService.requestTwoFactorSetup(id)
                call.respond(HttpStatusCode.OK, mapOf("devVerificationCode" to code))
            }
            post("/{id}/2fa/confirm") {
                val id = call.parameters["id"]!!
                val principal = call.principal<JWTPrincipal>()!!
                if (principal.payload.subject != id) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Cannot edit another user's profile"))
                    return@post
                }
                val req = call.receive<TwoFactorConfirmRequest>()
                call.respond(HttpStatusCode.OK, AuthService.confirmTwoFactorSetup(id, req.code))
            }
            post("/{id}/2fa/disable") {
                val id = call.parameters["id"]!!
                val principal = call.principal<JWTPrincipal>()!!
                if (principal.payload.subject != id) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Cannot edit another user's profile"))
                    return@post
                }
                call.respond(HttpStatusCode.OK, AuthService.disableTwoFactor(id))
            }
            delete("/{id}") {
                val id = call.parameters["id"]!!
                val principal = call.principal<JWTPrincipal>()!!
                if (principal.payload.subject != id) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Cannot delete another user's account"))
                    return@delete
                }
                AccountService.deleteAccount(id)
                call.respond(HttpStatusCode.OK, mapOf("deleted" to true))
            }
            // Settings > More > Security - "1 account per phone number".
            // See SecurityService/PhoneVerifications.
            get("/{id}/phone") {
                val id = call.parameters["id"]!!
                val principal = call.principal<JWTPrincipal>()!!
                if (principal.payload.subject != id) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Cannot view another user's security info"))
                    return@get
                }
                val (phone, verified) = SecurityService.getPhoneStatus(id)
                call.respond(HttpStatusCode.OK, PhoneStatusResponse(phone, verified))
            }
            post("/{id}/phone/request-code") {
                val id = call.parameters["id"]!!
                val principal = call.principal<JWTPrincipal>()!!
                if (principal.payload.subject != id) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Cannot edit another user's profile"))
                    return@post
                }
                val req = call.receive<RequestPhoneCodeRequest>()
                val devCode = SecurityService.requestPhoneCode(id, req.phoneNumber)
                call.respond(HttpStatusCode.OK, RequestPhoneCodeResponse(devCode))
            }
            post("/{id}/phone/verify") {
                val id = call.parameters["id"]!!
                val principal = call.principal<JWTPrincipal>()!!
                if (principal.payload.subject != id) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Cannot edit another user's profile"))
                    return@post
                }
                val req = call.receive<VerifyPhoneCodeRequest>()
                SecurityService.verifyPhoneCode(id, req.code)
                call.respond(HttpStatusCode.OK, mapOf("verified" to true))
            }
            // Chat list > More > Achievements.
            get("/{id}/achievements") {
                val id = call.parameters["id"]!!
                val principal = call.principal<JWTPrincipal>()!!
                if (principal.payload.subject != id) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Cannot view another user's achievements"))
                    return@get
                }
                call.respond(HttpStatusCode.OK, com.xhacker.cedal.services.AchievementService.listAll(id))
            }
            // Self-attested "first time doing X" achievements (see
            // AchievementService.CLIENT_TRIGGERABLE) - anything not on that
            // allowlist is silently ignored.
            post("/{id}/achievements/trigger") {
                val id = call.parameters["id"]!!
                val principal = call.principal<JWTPrincipal>()!!
                if (principal.payload.subject != id) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Cannot trigger another user's achievements"))
                    return@post
                }
                val req = call.receive<com.xhacker.cedal.models.TriggerAchievementRequest>()
                if (req.key in com.xhacker.cedal.services.AchievementService.CLIENT_TRIGGERABLE) {
                    com.xhacker.cedal.services.AchievementService.unlock(java.util.UUID.fromString(id), req.key)
                }
                call.respond(HttpStatusCode.OK, mapOf("ok" to true))
            }
            // "USE" on an unlocked achievement (see AchievementsBody client-side).
            put("/{id}/active-badge") {
                val id = call.parameters["id"]!!
                val principal = call.principal<JWTPrincipal>()!!
                if (principal.payload.subject != id) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Cannot edit another user's profile"))
                    return@put
                }
                val req = call.receive<com.xhacker.cedal.models.SetActiveBadgeRequest>()
                com.xhacker.cedal.services.AchievementService.setActiveBadge(id, req.key)
                call.respond(HttpStatusCode.OK, AuthService.getProfile(id))
            }
            // Settings > Security > Popularity (global).
            get("/{id}/popularity") {
                val id = call.parameters["id"]!!
                val principal = call.principal<JWTPrincipal>()!!
                if (principal.payload.subject != id) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Cannot view another user's popularity settings"))
                    return@get
                }
                call.respond(HttpStatusCode.OK, com.xhacker.cedal.services.PopularityService.getSettings(id))
            }
            put("/{id}/popularity") {
                val id = call.parameters["id"]!!
                val principal = call.principal<JWTPrincipal>()!!
                if (principal.payload.subject != id) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Cannot edit another user's popularity settings"))
                    return@put
                }
                val req = call.receive<com.xhacker.cedal.models.PopularitySettingsDto>()
                com.xhacker.cedal.services.PopularityService.setSettings(id, req)
                call.respond(HttpStatusCode.OK, req)
            }
            // Per-chat override (each chat thread's ⋮ menu > Popularity).
            get("/{id}/popularity/{friendId}") {
                val id = call.parameters["id"]!!
                val friendId = call.parameters["friendId"]!!
                val principal = call.principal<JWTPrincipal>()!!
                if (principal.payload.subject != id) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Cannot view another user's popularity settings"))
                    return@get
                }
                call.respond(HttpStatusCode.OK, com.xhacker.cedal.services.PopularityService.getOverride(id, friendId))
            }
            put("/{id}/popularity/{friendId}") {
                val id = call.parameters["id"]!!
                val friendId = call.parameters["friendId"]!!
                val principal = call.principal<JWTPrincipal>()!!
                if (principal.payload.subject != id) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Cannot edit another user's popularity settings"))
                    return@put
                }
                val req = call.receive<com.xhacker.cedal.models.ChatPopularityOverrideDto>()
                com.xhacker.cedal.services.PopularityService.setOverride(id, friendId, req)
                call.respond(HttpStatusCode.OK, req)
            }
            // Polled globally (see MemberScaffold) for achievement/rank-up
            // popups - offline-safe, exactly-once (see PendingPopupService).
            get("/{id}/popups/pending") {
                val id = call.parameters["id"]!!
                val principal = call.principal<JWTPrincipal>()!!
                if (principal.payload.subject != id) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Cannot view another user's popups"))
                    return@get
                }
                call.respond(HttpStatusCode.OK, com.xhacker.cedal.services.PendingPopupService.pollPending(id))
            }
            // Live in-session ban detection - polled globally (see
            // MemberScaffold) so a live session gets shut down within one
            // poll cycle of an admin action, not just on next login.
            get("/{id}/account-status") {
                val id = call.parameters["id"]!!
                val principal = call.principal<JWTPrincipal>()!!
                if (principal.payload.subject != id) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Cannot view another user's account status"))
                    return@get
                }
                val row = org.jetbrains.exposed.sql.transactions.transaction {
                    com.xhacker.cedal.db.Users.selectAll()
                        .where { com.xhacker.cedal.db.Users.id eq java.util.UUID.fromString(id) }
                        .firstOrNull()
                }
                val status = when {
                    // Clear Data already removed the row entirely - nothing
                    // left to restore, so this is always "permanent" for
                    // gate purposes (no appeal countdown).
                    row == null -> com.xhacker.cedal.models.AccountStatusDto(gated = true, permanent = true)
                    row[com.xhacker.cedal.db.Users.banned] -> com.xhacker.cedal.models.AccountStatusDto(
                        gated = true,
                        permanent = row[com.xhacker.cedal.db.Users.banPermanent],
                        bannedAt = row[com.xhacker.cedal.db.Users.bannedAt],
                    )
                    else -> com.xhacker.cedal.models.AccountStatusDto(gated = false, permanent = false)
                }
                call.respond(HttpStatusCode.OK, status)
            }
        }
    }

    // Godmode (chat list > More, admin-only) - client also gates "Ban"/
    // "Clear Data" behind biometric/passcode re-verification before ever
    // calling these, same pattern as Settings' Delete Account.
    route("/admin/godmode") {
        authenticate("auth-jwt") {
            get("/users") {
                requireAdmin(call)
                call.respond(HttpStatusCode.OK, com.xhacker.cedal.services.AdminService.listAllUsers())
            }
            post("/{id}/ban") {
                requireAdmin(call)
                val id = call.parameters["id"] ?: throw com.xhacker.cedal.services.AuthException("Missing id")
                com.xhacker.cedal.services.AdminService.setBanned(id, true)
                call.respond(HttpStatusCode.OK, mapOf("ok" to true))
            }
            post("/{id}/unban") {
                requireAdmin(call)
                val id = call.parameters["id"] ?: throw com.xhacker.cedal.services.AuthException("Missing id")
                com.xhacker.cedal.services.AdminService.setBanned(id, false)
                call.respond(HttpStatusCode.OK, mapOf("ok" to true))
            }
            // New admin tool - skips the 24h grace/appeal window entirely
            // (see AdminService.setPermanentBan/BanEscalationService).
            post("/{id}/permanent-ban") {
                requireAdmin(call)
                val id = call.parameters["id"] ?: throw com.xhacker.cedal.services.AuthException("Missing id")
                com.xhacker.cedal.services.AdminService.setPermanentBan(id)
                call.respond(HttpStatusCode.OK, mapOf("ok" to true))
            }
            post("/{id}/clear-data") {
                requireAdmin(call)
                val id = call.parameters["id"] ?: throw com.xhacker.cedal.services.AuthException("Missing id")
                com.xhacker.cedal.services.AdminService.recordClearedTombstone(id)
                AccountService.deleteAccount(id)
                call.respond(HttpStatusCode.OK, mapOf("ok" to true))
            }
        }
    }

    // Developer terminal > "Manage Developer Access" - owner-only (same
    // requireAdmin gate, since there's exactly one owner - see
    // DeveloperAccessService's own doc comment).
    route("/admin/developer") {
        authenticate("auth-jwt") {
            get("/users") {
                requireAdmin(call)
                call.respond(HttpStatusCode.OK, com.xhacker.cedal.services.AdminService.listAllUsers())
            }
            post("/{id}/grant") {
                requireAdmin(call)
                val id = call.parameters["id"] ?: throw com.xhacker.cedal.services.AuthException("Missing id")
                com.xhacker.cedal.services.DeveloperAccessService.setAccess(id, true)
                call.respond(HttpStatusCode.OK, mapOf("ok" to true))
            }
            post("/{id}/revoke") {
                requireAdmin(call)
                val id = call.parameters["id"] ?: throw com.xhacker.cedal.services.AuthException("Missing id")
                com.xhacker.cedal.services.DeveloperAccessService.setAccess(id, false)
                call.respond(HttpStatusCode.OK, mapOf("ok" to true))
            }
            // Returned ONLY here, to the owner - the delegated account
            // never sees this value through any endpoint of their own.
            post("/{id}/generate-key") {
                requireAdmin(call)
                val id = call.parameters["id"] ?: throw com.xhacker.cedal.services.AuthException("Missing id")
                val key = com.xhacker.cedal.services.DeveloperAccessService.generateKey(id)
                call.respond(HttpStatusCode.OK, mapOf("key" to key))
            }
            // "SEND KEY" on ManageDeveloperAccessScreen's generated-key
            // dialog - relays it as an in-app DM from "Cedal Team" instead
            // of the owner having to text/email it manually.
            post("/{id}/send-key") {
                requireAdmin(call)
                val id = call.parameters["id"] ?: throw com.xhacker.cedal.services.AuthException("Missing id")
                val req = call.receive<com.xhacker.cedal.models.SendDeveloperKeyRequest>()
                com.xhacker.cedal.services.DeveloperAccessService.sendKeyMessage(id, req.key)
                call.respond(HttpStatusCode.OK, mapOf("ok" to true))
            }
        }
    }

    // Self-service developer-access actions - the "Cedal Team" chat
    // thread's read-only action panel (see FriendStatusResult.isCedalTeam),
    // not admin-gated: any account with developerAccess already granted can
    // call these on itself. DeveloperAccessService rejects the call if the
    // caller doesn't currently have developer access.
    route("/developer/access") {
        authenticate("auth-jwt") {
            post("/generate-key") {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                com.xhacker.cedal.services.DeveloperAccessService.generateKeyForSelf(userId)
                call.respond(HttpStatusCode.OK, mapOf("ok" to true))
            }
            post("/revoke") {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                com.xhacker.cedal.services.DeveloperAccessService.revokeSelf(userId)
                call.respond(HttpStatusCode.OK, mapOf("ok" to true))
            }
        }
    }
}

private fun requireAdmin(call: ApplicationCall) {
    val userId = call.principal<JWTPrincipal>()!!.payload.subject
    if (!com.xhacker.cedal.services.AdminService.isAdmin(userId)) {
        throw com.xhacker.cedal.services.AuthException("Admins only")
    }
}
