package bitmentor.api.endpoints

import bitmentor.api.exceptions.BadFileTypeException
import bitmentor.api.exceptions.FileTooLargeException
import bitmentor.api.exceptions.UserNotFoundException
import bitmentor.api.model.file.FileTypes
import bitmentor.api.service.deleteProfileImage
import bitmentor.api.service.extractUserId
import bitmentor.api.service.uploadFile
import bitmentor.api.service.uploadUserImage
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
fun Routing.initialiseFileEndpoints() = apply {

    put("/images/user") {
        try {
            val userId = extractUserId(call)
            logger.info { "Received image from userId: $userId" }
            val multipart = call.receiveMultipart()
            multipart.uploadUserImage(userId = userId)

            call.respond(HttpStatusCode.OK)

        } catch (e: UserNotFoundException) {
            call.respond(HttpStatusCode.NotFound)
        } catch (e: BadFileTypeException) {
            call.respond(HttpStatusCode.BadRequest, mapOf("errors" to listOf(e.message)))
        } catch (e: FileTooLargeException) {
            call.respond(HttpStatusCode.BadRequest, mapOf("errors" to listOf(e.message)))
        }
    }

    delete("/images/user") {
        val userId = extractUserId(call)
        logger.info { "Deleting image for userId: $userId" }

        deleteProfileImage(userId)

        call.respond(HttpStatusCode.OK)
    }

    post("/files") {
        try {
            val userId = extractUserId(call)
            val fileTypeName = call.parameters["type"]
                    ?: throw BadRequestException("type is required")

            val fileType = FileTypes.valueOf(fileTypeName)

            logger.info { "Received file from userId: $userId, type: $fileType" }
            val multipart = call.receiveMultipart()
            multipart.uploadFile(userId = userId, fileType = fileType)

            call.respond(HttpStatusCode.Created)

        } catch (e: BadFileTypeException) {
            call.respond(HttpStatusCode.BadRequest, mapOf("errors" to listOf(e.message)))
        } catch (e: FileTooLargeException) {
            call.respond(HttpStatusCode.BadRequest, mapOf("errors" to listOf(e.message)))
        } catch (e: IllegalArgumentException) {
            call.respond(HttpStatusCode.BadRequest, mapOf("errors" to listOf("not a valid file type")))
        }
    }
}








