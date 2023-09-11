package bitmentor.api.endpoints

import bitmentor.api.service.getChecksum
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.*
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

@KtorExperimentalAPI
fun Routing.initialiseBbbEndpoints() = apply {
    get("/public/bbb") {
        // TODO Remove this
        call.respond(getChecksum(call.request.queryParameters["string"] ?: throw BadRequestException("")))
    }
}





