package com.xhacker.cedal.routes

import com.xhacker.cedal.models.CreateStickerRequest
import com.xhacker.cedal.services.StickerService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.stickerRoutes() {
    route("/stickers") {
        authenticate("auth-jwt") {
            get {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                call.respond(HttpStatusCode.OK, StickerService.listMyStickers(userId))
            }
            post {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                val req = call.receive<CreateStickerRequest>()
                call.respond(HttpStatusCode.OK, StickerService.createSticker(userId, req.imageUrl))
            }
        }
    }
}
