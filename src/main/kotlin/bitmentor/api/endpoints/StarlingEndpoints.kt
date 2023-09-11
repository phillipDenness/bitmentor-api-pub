package bitmentor.api.endpoints

import bitmentor.api.service.isStarlingPayloadValid
import bitmentor.api.service.sendBotMessage
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.*
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

@KtorExperimentalAPI
fun Routing.initialiseStarlingEndpoints() = apply {
    post("/public/starling") {
        try {

            val payload = call.receive<String>()
            logger.info { "Recieved payload: $payload" }
            val signature = call.request.headers["X-Hook-Signature"].toString()

            if (isStarlingPayloadValid(signature = signature, payload = payload)) {
                logger.info { "Validated payload" }
                sendBotMessage(payload)
            } else {
                sendBotMessage("$payload: failed validation")
            }
            call.respond(HttpStatusCode.OK)

        } catch (e: Exception) {
            logger.error { "Exception ${e.message}" }
            call.respond(HttpStatusCode.InternalServerError, mapOf("errors" to listOf("${e.message}")))
        }
    }
}
