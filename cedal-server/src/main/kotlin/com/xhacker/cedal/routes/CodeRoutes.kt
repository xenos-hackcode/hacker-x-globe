package com.xhacker.cedal.routes

import com.xhacker.cedal.models.BackerReviewRequest
import com.xhacker.cedal.models.BackerReviewResponse
import com.xhacker.cedal.models.CodeRunRequest
import com.xhacker.cedal.models.ExplainErrorRequest
import com.xhacker.cedal.models.ExplainErrorResponse
import com.xhacker.cedal.services.AiErrorExplainerService
import com.xhacker.cedal.services.AuthException
import com.xhacker.cedal.services.CodeBackerService
import com.xhacker.cedal.services.CodeExecutionService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.codeRoutes() {
    route("/code") {
        authenticate("auth-jwt") {
            get("/languages") {
                call.respond(HttpStatusCode.OK, CodeExecutionService.listLanguages())
            }
            post("/run") {
                val req = call.receive<CodeRunRequest>()
                if (req.code.isBlank()) throw AuthException("Write some code first")
                call.respond(HttpStatusCode.OK, CodeExecutionService.run(req.language, req.code, req.stdin, req.extraFiles, req.packages))
            }
            post("/explain-error") {
                val req = call.receive<ExplainErrorRequest>()
                if (req.errorText.isBlank()) throw AuthException("Nothing to explain")
                val explanation = AiErrorExplainerService.explain(req.language, req.errorText)
                call.respond(HttpStatusCode.OK, ExplainErrorResponse(explanation))
            }
            // Pad's "Backer" - an automatic pre-flight check before every
            // Run (see CodeBackerService). Real cost per call like
            // explain-error above, just triggered by Run itself instead of
            // an explicit button - that tradeoff was a deliberate choice,
            // not an oversight.
            post("/backer-review") {
                val req = call.receive<BackerReviewRequest>()
                if (req.code.isBlank()) throw AuthException("Nothing to check")
                val result = CodeBackerService.review(req.language, req.code)
                call.respond(HttpStatusCode.OK, BackerReviewResponse(result.hasIssue, result.reason, result.fixedCode))
            }
        }
    }
}
