package com.xhacker.cedal.routes

import com.xhacker.cedal.models.UploadImageResponse
import com.xhacker.cedal.services.AuthException
import com.xhacker.cedal.services.ImageUploadService
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.uploadRoutes() {
    route("/uploads") {
        authenticate("auth-jwt") {
            // kind is just a folder prefix, see ImageUploadService.VALID_KINDS
            // (the real source of truth for what's allowed). The caller
            // decides what to do with the returned URL (updateProfile for
            // avatars, POST /stickers for custom stickers, a chat message's
            // mediaUrl for chat_media/chat_file) - this route only ever
            // stores bytes. This used to hardcode just "avatar"/"sticker",
            // silently rejecting chat_media/chat_file before they ever
            // reached the service - meaning camera/gallery/file chat
            // attachments never actually worked.
            post("/image") {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject
                val kind = call.request.queryParameters["kind"]?.takeIf { it in ImageUploadService.VALID_KINDS }
                    ?: throw AuthException("Unknown upload kind")

                var bytes: ByteArray? = null
                var contentType: String? = null
                call.receiveMultipart().forEachPart { part ->
                    if (part is PartData.FileItem && bytes == null) {
                        bytes = part.streamProvider().readBytes()
                        contentType = part.contentType?.toString() ?: "image/jpeg"
                    }
                    part.dispose()
                }
                val fileBytes = bytes ?: throw AuthException("No file uploaded")
                val url = ImageUploadService.upload(userId, kind, fileBytes, contentType ?: "image/jpeg")
                call.respond(HttpStatusCode.OK, UploadImageResponse(url))
            }
        }
    }
}
