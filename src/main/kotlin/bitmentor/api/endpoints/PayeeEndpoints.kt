package bitmentor.api.endpoints

import bitmentor.api.model.payee.PayeeInsert
import bitmentor.api.service.create
import bitmentor.api.service.deletePayee
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
fun Routing.initialisePayeeEndpoints() = apply {
    post("/payee") {
            val userId = extractUserId(call)
            val payeeInsert = call.receive<PayeeInsert>()
            logger.info { "Saving new payee for user: $userId" }

            payeeInsert.create(userId)
            call.respond(HttpStatusCode.Created)
    }

    delete("/payee" ) {
        val userId = extractUserId(call)

        logger.info { "Deleting payee for user: $userId" }

        deletePayee(userId)
        call.respond(HttpStatusCode.NoContent)
    }
}



