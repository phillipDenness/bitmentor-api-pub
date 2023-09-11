package bitmentor.api.endpoints

import bitmentor.api.endpoints.helpers.validatePaging
import bitmentor.api.exceptions.TutorNotFoundException
import bitmentor.api.exceptions.UserNotFoundException
import bitmentor.api.model.CreatedResource
import bitmentor.api.model.enquiry.EnquiryInsert
import bitmentor.api.service.create
import bitmentor.api.service.extractUserId
import bitmentor.api.service.getEnquiriesByUserId
import bitmentor.api.service.getEnquiryById
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
fun Routing.initialiseEnquiriesEndpoints() = apply {

    get("/enquiries") {
        try {
            val pageAndSize = validatePaging(call.parameters)
            val isTutor = call.parameters["isTutor"]?.toBoolean()
                    ?: throw BadRequestException("isTutor is missing")
            logger.info { "Getting messages page: ${pageAndSize.first}, size: ${pageAndSize.second}, isTutor: $isTutor" }

            call.respond(HttpStatusCode.OK, getEnquiriesByUserId(
                userId = extractUserId(call),
                page = pageAndSize.first,
                size = pageAndSize.second,
                isTutor = isTutor
            ))
        } catch (e: NumberFormatException) {
            throw BadRequestException("Page & Size be an integer : $e")
        } catch (e: UserNotFoundException) {
            logger.error { "The sender or recipient has not completed their registration." }
            throw BadRequestException("The sender or recipient has not completed their registration.")
        }
    }


    get("/enquiries/{enquiryId}") {
        try {
            val enquiryId = call.parameters["enquiryId"]?.toInt()
                    ?: throw BadRequestException("Must provide enquiry id")
            logger.info { "Getting message for enquiry id: $enquiryId" }

            call.respond(HttpStatusCode.OK, getEnquiryById(
                    userId = extractUserId(call),
                    enquiryId = enquiryId
            ))
        } catch (e: NumberFormatException) {
            throw BadRequestException("Enquiry id must be a number : $e")
        } catch (e: UserNotFoundException) {
            logger.error { "The sender or recipient has not completed their registration." }
            throw BadRequestException("The sender or recipient has not completed their registration.")
        }
    }

    post("/enquiries") {
        try {
            val enquiryInsert = call.receive<EnquiryInsert>()

            logger.info { "Creating a new enquiry $enquiryInsert" }

            val senderId = extractUserId(call)
            when {
                senderId == enquiryInsert.message.recipientUserId ->
                    throw BadRequestException("You may not send a message to yourself.")
                enquiryInsert.message.enquiryId !== null ->
                    throw BadRequestException("The first message of a enquiry must not belong to another enquiry")
                else -> call.respond(HttpStatusCode.Created, CreatedResource(enquiryInsert.create(senderId)))
            }
        } catch (e: UserNotFoundException) {
            call.respond(HttpStatusCode.BadRequest, mapOf("errors" to listOf("Recipient was not found.")))
        } catch (e: TutorNotFoundException) {
            call.respond(HttpStatusCode.BadRequest, mapOf("errors" to listOf("The tutor / tutorUserId combination does not exist")))
        }
    }
}



