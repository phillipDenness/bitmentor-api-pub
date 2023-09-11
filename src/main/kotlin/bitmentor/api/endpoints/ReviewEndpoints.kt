package bitmentor.api.endpoints

import bitmentor.api.endpoints.helpers.validatePaging
import bitmentor.api.exceptions.TutorNotFoundException
import bitmentor.api.model.CreatedResource
import bitmentor.api.model.review.ReviewInsert
import bitmentor.api.model.review.ReviewUpdate
import bitmentor.api.service.*
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.*
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

@KtorExperimentalAPI
fun Routing.initialiseReviewEndpoints() = apply {

    get("/public/reviews") {
        try {
            val pageAndSize = validatePaging(call.parameters)
            val tutorId = call.parameters["tutorId"]?.toInt()
                    ?: throw BadRequestException("TutorId must be provided")
            logger.info { "Getting reviews page ${pageAndSize.first} and size ${pageAndSize.second} and tutorId $tutorId" }

            call.respond(HttpStatusCode.OK, getReviewsByTutorId(
                tutorId = tutorId,
                    page = pageAndSize.first,
                    size = pageAndSize.second
            ))
        } catch (e: NumberFormatException) {
            throw BadRequestException("Page & Size be an integer : $e")
        }
    }

    get("/reviews") {
        try {
            val pageAndSize = validatePaging(call.parameters)

            logger.info { "Getting reviews page ${pageAndSize.first} and size ${pageAndSize.second}" }

            call.respond(HttpStatusCode.OK, getReviewsByUserId(
                    userId = extractUserId(call),
                    page = pageAndSize.first,
                    size = pageAndSize.second
            ))
        } catch (e: NumberFormatException) {
            throw BadRequestException("Page & Size be an integer : $e")
        }
    }

    post("/reviews") {
        try {
            val reviewInsert = call.receive<ReviewInsert>()

            val senderId = extractUserId(call)
            logger.info { "Creating review from user $senderId" }

            call.respond(HttpStatusCode.Created, CreatedResource(reviewInsert.create(senderId)))
        } catch (e: TutorNotFoundException) {
            call.respond(HttpStatusCode.BadRequest, mapOf("errors" to listOf("Recipient user not found.")))
        }
    }

    put("/reviews/{reviewId}") {
        try {
            val reviewUpdate = call.receive<ReviewUpdate>()
            val reviewId = call.parameters["reviewId"]?.toInt()
                    ?: throw BadRequestException("ReviewId must be provided")
            logger.info { "Updating review" }

            val senderId = extractUserId(call)
            reviewUpdate.update( userId = senderId, reviewId = reviewId)
            call.respond(HttpStatusCode.OK)
        } catch (e: NumberFormatException) {
            throw BadRequestException("Page & Size be an integer : $e")
        }
    }

    delete("/reviews/{reviewId}") {
        try {
            val reviewId = call.parameters["reviewId"]?.toInt()
                    ?: throw BadRequestException("ReviewId must be provided")
            logger.info { "deleting review" }

            val senderId = extractUserId(call)
            deleteReview( userId = senderId, reviewId = reviewId)
            call.respond(HttpStatusCode.OK)
        } catch (e: NumberFormatException) {
            throw BadRequestException("Page & Size be an integer : $e")
        }
    }
}



