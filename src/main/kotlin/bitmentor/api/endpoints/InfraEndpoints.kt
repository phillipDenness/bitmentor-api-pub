package bitmentor.api.endpoints


import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.*
import mu.KotlinLogging


private val logger = KotlinLogging.logger {}

@KtorExperimentalAPI
fun Routing.initialiseInfraEndpoints() = apply {

    //    get("/public/metrics") {
//        call.respond(appMicrometerRegistry.scrape())
//    }

    get("/public/ping") {
        logger.trace("pong")
        call.respond("pong")
    }
}
