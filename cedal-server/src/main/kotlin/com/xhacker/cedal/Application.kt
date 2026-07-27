package com.xhacker.cedal

import com.xhacker.cedal.db.DatabaseFactory
import com.xhacker.cedal.models.ErrorResponse
import com.xhacker.cedal.plugins.configureSecurity
import com.xhacker.cedal.routes.adminRoutes
import com.xhacker.cedal.routes.aiChangeRequestRoutes
import com.xhacker.cedal.routes.developerSubmissionRoutes
import com.xhacker.cedal.routes.androidBuildRoutes
import com.xhacker.cedal.routes.appVersionRoutes
import com.xhacker.cedal.routes.appealRoutes
import com.xhacker.cedal.routes.alucardRoutes
import com.xhacker.cedal.routes.arcRoutes
import com.xhacker.cedal.routes.authRoutes
import com.xhacker.cedal.routes.chatRoutes
import com.xhacker.cedal.routes.codeRoutes
import com.xhacker.cedal.routes.cornealRoutes
import com.xhacker.cedal.routes.dailyTaskRoutes
import com.xhacker.cedal.routes.systemFeedRoutes
import com.xhacker.cedal.routes.friendRoutes
import com.xhacker.cedal.routes.guiSessionRoutes
import com.xhacker.cedal.routes.learnRoutes
import com.xhacker.cedal.routes.marketRoutes
import com.xhacker.cedal.routes.messageInteractionRoutes
import com.xhacker.cedal.routes.platformRoutes
import com.xhacker.cedal.routes.portfolioRoutes
import com.xhacker.cedal.routes.smsRelayRoutes
import com.xhacker.cedal.routes.stickerRoutes
import com.xhacker.cedal.routes.themePackRoutes
import com.xhacker.cedal.routes.tradeRoutes
import com.xhacker.cedal.routes.uploadRoutes
import com.xhacker.cedal.routes.userRoutes
import com.xhacker.cedal.routes.walletRoutes
import com.xhacker.cedal.services.AuthException
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json

fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
    embeddedServer(Netty, port = port, host = "0.0.0.0", module = Application::module).start(wait = true)
}

fun Application.module() {
    DatabaseFactory.init()

    install(CallLogging)
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
    }
    install(StatusPages) {
        exception<AuthException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, ErrorResponse(cause.message ?: "Bad request"))
        }
        exception<Throwable> { call, cause ->
            // Was previously silent - a real 500 (e.g. the android-build
            // callback's error_message once overflowing its old varchar(2000)
            // column) left zero trace anywhere except the plain "500" in
            // Cloud Run's request log, with no way to tell what actually
            // broke without this.
            call.application.log.error("Unhandled exception handling ${call.request.local.method.value} request", cause)
            call.respond(HttpStatusCode.InternalServerError, ErrorResponse(cause.message ?: "Internal error"))
        }
    }
    configureSecurity()

    routing {
        authRoutes()
        userRoutes()
        friendRoutes()
        walletRoutes()
        tradeRoutes()
        marketRoutes()
        portfolioRoutes()
        codeRoutes()
        androidBuildRoutes()
        guiSessionRoutes()
        aiChangeRequestRoutes()
        developerSubmissionRoutes()
        messageInteractionRoutes()
        learnRoutes()
        arcRoutes()
        alucardRoutes()
        dailyTaskRoutes()
        chatRoutes()
        cornealRoutes()
        systemFeedRoutes()
        uploadRoutes()
        stickerRoutes()
        themePackRoutes()
        adminRoutes()
        appealRoutes()
        smsRelayRoutes()
        platformRoutes()
        appVersionRoutes()
        get("/health") { call.respond(mapOf("status" to "ok")) }
    }
}
