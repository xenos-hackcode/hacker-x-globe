package com.xhacker.cedal.routes

import com.xhacker.cedal.models.FriendRequestCreate
import com.xhacker.cedal.services.FriendService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.friendRoutes() {
    route("/friends") {
        authenticate("auth-jwt") {
            get {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                call.respond(HttpStatusCode.OK, FriendService.listFriends(userId))
            }
            get("/search") {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                val q = call.request.queryParameters["q"]
                val byGender = call.request.queryParameters["byGender"].toBoolean()
                val byOccupation = call.request.queryParameters["byOccupation"].toBoolean()
                val byHobby = call.request.queryParameters["byHobby"].toBoolean()
                val byAge = call.request.queryParameters["byAge"].toBoolean()
                val byBio = call.request.queryParameters["byBio"].toBoolean()
                // (String?.toBoolean() is a valid nullable-receiver extension in
                // Kotlin's stdlib — returns false for null/anything but "true".)
                call.respond(HttpStatusCode.OK, FriendService.search(userId, q, byGender, byOccupation, byHobby, byAge, byBio))
            }
            post("/request") {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                val req = call.receive<FriendRequestCreate>()
                FriendService.sendRequest(userId, req.toUserId)
                call.respond(HttpStatusCode.OK, mapOf("sent" to true))
            }
            get("/requests") {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                call.respond(HttpStatusCode.OK, FriendService.listRequests(userId))
            }
            post("/requests/{id}/accept") {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                FriendService.respond(userId, call.parameters["id"]!!, accept = true)
                call.respond(HttpStatusCode.OK, mapOf("updated" to true))
            }
            post("/requests/{id}/decline") {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                FriendService.respond(userId, call.parameters["id"]!!, accept = false)
                call.respond(HttpStatusCode.OK, mapOf("updated" to true))
            }
            delete("/requests/{id}") {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                FriendService.cancel(userId, call.parameters["id"]!!)
                call.respond(HttpStatusCode.OK, mapOf("cancelled" to true))
            }
        }
    }
}
