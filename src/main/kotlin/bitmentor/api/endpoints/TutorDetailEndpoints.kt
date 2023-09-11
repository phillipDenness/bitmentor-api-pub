package bitmentor.api.endpoints

import bitmentor.api.exceptions.TutorNotFoundException
import bitmentor.api.model.tutor_detail.UpdatePreferredPayoutRequest
import bitmentor.api.model.tutor_detail.UpdateTutorDetailRequest
import bitmentor.api.service.extractUserId
import bitmentor.api.service.getTutorDetail
import bitmentor.api.service.update
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.*
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

@KtorExperimentalAPI
fun Routing.initialiseTutorDetailEndpoints() = apply {

    get("/tutor-detail") {
        try {
            val userId = extractUserId(call)
            logger.info { "Get private tutor profile for userId $userId" }

            val payload = getTutorDetail(userId)
                    ?: TutorNotFoundException()
            call.respond(HttpStatusCode.OK, payload)
        } catch (e: TutorNotFoundException) {
            call.respond(HttpStatusCode.NotFound, mapOf("errors" to listOf("Tutor not found")))
        }
    }

    put("/tutor-detail") {
        try {
            val tutorRequest = call.receive<UpdateTutorDetailRequest>()

            logger.info { "Updating tutor details" }
            val tutorResource = tutorRequest.update(extractUserId(call))
            call.respond(HttpStatusCode.OK, tutorResource)
        } catch (e: TutorNotFoundException) {
            call.respond(HttpStatusCode.NotFound, mapOf("errors" to listOf("Tutor not found")))
        }
    }

    put("/preferred-payout-option") {
        try {
            val tutorRequest = call.receive<UpdatePreferredPayoutRequest>()

            logger.info { "Updating tutor preferred payout option to ${tutorRequest.preferredPayoutOption}" }
            tutorRequest.preferredPayoutOption.update(extractUserId(call))
            call.respond(HttpStatusCode.OK)
        } catch (e: TutorNotFoundException) {
            call.respond(HttpStatusCode.NotFound, mapOf("errors" to listOf("Tutor not found")))
        }
    }
}



