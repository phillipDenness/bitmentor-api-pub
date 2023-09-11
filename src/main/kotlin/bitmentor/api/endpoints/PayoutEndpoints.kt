package bitmentor.api.endpoints

import bitmentor.api.endpoints.helpers.validatePaging
import bitmentor.api.service.extractUserId
import bitmentor.api.service.getPayouts
import bitmentor.api.service.payoutRequested
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.*
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

@KtorExperimentalAPI
fun Routing.initialisePayoutEndpoints() = apply {
    get("/payouts") {
        try {
            val pageAndSize = validatePaging(call.parameters)
            val userId = extractUserId(call)
            logger.info { "Getting payouts for user: $userId page: ${pageAndSize.first} size: ${pageAndSize.second}" }

            call.respond(HttpStatusCode.OK, getPayouts(
                    userId = extractUserId(call),
                    page = pageAndSize.first,
                    size = pageAndSize.second
            ))
        } catch (e: NumberFormatException) {
            throw BadRequestException("Page & Size be an integer : $e")
        }
    }

    post("/payouts") {
        val userId = extractUserId(call)

        logger.info { "User $userId has requested payout" }
        try {
            payoutRequested(userId)
            call.respond(HttpStatusCode.OK)
        } catch (e: Exception) {
            logger.error { "Error requesting payout: $e" }
            call.respond(HttpStatusCode.InternalServerError)
        }
    }
}



