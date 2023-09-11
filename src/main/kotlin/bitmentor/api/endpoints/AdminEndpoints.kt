package bitmentor.api.endpoints

import bitmentor.api.model.admin.AdminResource
import bitmentor.api.service.*
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.*
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

@KtorExperimentalAPI
fun Routing.initialiseAdminEndpoints() = apply {
    get("/admin") {
        try {
            val userId = extractUserId(call)
            logger.info { "Check is admin called by userId $userId" }

            val isAdmin = userId.isAdmin()
            call.respond(HttpStatusCode.OK, AdminResource(
                    isAdmin = isAdmin,
                    userId = userId
            ))

        } catch (e: Exception) {
            logger.error { "Exception admin ${e.message}" }
            call.respond(HttpStatusCode.InternalServerError, mapOf("errors" to listOf("${e.message}")))
        }
    }

    put("/admin") {
        try {
            val userId = extractUserId(call)
            logger.info { "Admin called by userId $userId" }

            if (userId.isAdmin()) {
                val isIdVerify = call.parameters["isIdVerify"]?.toBoolean() ?: false
                val isDbsVerify = call.parameters["isDbsVerify"]?.toBoolean() ?: false
                val isPayoutComplete = call.parameters["isPayoutComplete"]?.toBoolean() ?: false

                if (isIdVerify) {
                    val tutorId = call.parameters["tutorId"]?.toInt()
                            ?: throw BadRequestException("id verification requires tutor id")

                    approveTutorId(tutorId)
                } else if (isDbsVerify) {
                    val tutorId = call.parameters["tutorId"]?.toInt()
                        ?: throw BadRequestException("DBS verification requires tutor id")

                    approveTutorDsb(tutorId)
                } else if (isPayoutComplete) {
                    val payoutIds = call.request.queryParameters.getAll("payoutIds") ?: throw BadRequestException("Payout ids required")
                    logger.info { "payout ids to complete: $payoutIds" }

                    markPayoutsComplete(payoutIds = payoutIds)
                }

                call.respond(HttpStatusCode.OK)
            } else {
                logger.warn { "User $userId attempted access admin without authorization" }
                call.respond(HttpStatusCode.Unauthorized)
            }

        } catch (e: Exception) {
            logger.error { "Exception admin ${e.message}" }
            call.respond(HttpStatusCode.InternalServerError, mapOf("errors" to listOf("${e.message}")))
        }
    }
}
