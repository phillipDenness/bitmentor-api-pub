package bitmentor.api.endpoints

import bitmentor.api.endpoints.helpers.validatePaging
import bitmentor.api.service.extractUserId
import bitmentor.api.service.getOrders
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.*
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

@KtorExperimentalAPI
fun Routing.initialiseOrderEndpoints() = apply {

    get("/orders") {
        try {
            val pageAndSize = validatePaging(call.parameters)

            val userId = extractUserId(call)
            logger.info { "Getting orders for user: $userId page: ${pageAndSize.first} size: ${pageAndSize.second}" }

            call.respond(HttpStatusCode.OK, getOrders(
                    userId = extractUserId(call),
                    page = pageAndSize.first,
                    size = pageAndSize.second
            ))
        } catch (e: NumberFormatException) {
            throw BadRequestException("Page & Size be an integer : $e")
        }
    }
}



