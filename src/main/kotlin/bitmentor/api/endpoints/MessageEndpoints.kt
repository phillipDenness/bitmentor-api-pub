package bitmentor.api.endpoints

import bitmentor.api.endpoints.helpers.validatePaging
import bitmentor.api.exceptions.UserNotFoundException
import bitmentor.api.model.CreatedResource
import bitmentor.api.model.message.MessageInsert
import bitmentor.api.service.create
import bitmentor.api.service.extractUserId
import bitmentor.api.service.getMessagesByEnquiryId
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
fun Routing.initialiseMessageEndpoints() = apply {

    get("/messages") {
        try {
            val enquiryId = call.parameters["enquiryId"]?.toInt()
                    ?: throw BadRequestException("Must provide enquiryId")
            val pageAndSize = validatePaging(call.parameters)

            val userId = extractUserId(call)
            logger.info { "Getting messages for user: $userId, enquiryId: $enquiryId page: ${pageAndSize.first} size: ${pageAndSize.second}" }

            call.respond(HttpStatusCode.OK, getMessagesByEnquiryId(
                    enquiryId = enquiryId,
                    userId = userId,
                    page = pageAndSize.first,
                    size = pageAndSize.second
            ))
        } catch (e: NumberFormatException) {
            throw BadRequestException("Page & Size be an integer : $e")
        } catch (e: UserNotFoundException) {
            throw BadRequestException("Message send failed because the sender or recipient has not completed registration")
        }
    }

    post("/messages") {
        try {
            val messageInsert = call.receive<MessageInsert>()
            val senderId = extractUserId(call)
            logger.info { "Creating message from userId: $senderId, $messageInsert" }

            if (senderId == messageInsert.recipientUserId) {
                throw BadRequestException("You may not send a message to yourself")
            }
            if (messageInsert.enquiryId == null) {
                throw BadRequestException("enquiryId is required")
            }
            call.respond(HttpStatusCode.Created, CreatedResource(messageInsert.create(senderId)))
        } catch (e: UserNotFoundException) {
            call.respond(HttpStatusCode.BadRequest, mapOf("errors" to listOf("Recipient user not found")))
        }
    }

}



