package bitmentor.api.endpoints

import bitmentor.api.model.help.HelpInsert
import bitmentor.api.model.help.RefundInsert
import bitmentor.api.model.help.SupportInsert
import bitmentor.api.service.create
import bitmentor.api.service.extractUserId
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.*
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

@KtorExperimentalAPI
fun Routing.initialiseHelpEndpoints() = apply {

    post("/help") {
            val helpInsert = call.receive<HelpInsert>()

            val userId = extractUserId(call)
            logger.info { "Received help insert $helpInsert from user $userId" }

            call.respond(HttpStatusCode.Created, helpInsert.create(userId))
    }

    post("/support") {
            val supportInsert = call.receive<SupportInsert>()

            val userId = extractUserId(call)
            logger.info { "Received support insert $supportInsert from user $userId" }

            supportInsert.create(userId)
            call.respond(HttpStatusCode.Created)
    }

    post("/refund") {
            val refundInsert = call.receive<RefundInsert>()

            val userId = extractUserId(call)
            logger.info { "Received refund insert $refundInsert from user $userId" }
            refundInsert.create(userId)
            call.respond(HttpStatusCode.Created)
    }
}



