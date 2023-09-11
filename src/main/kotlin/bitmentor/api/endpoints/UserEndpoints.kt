package bitmentor.api.endpoints

import bitmentor.api.endpoints.helpers.validate
import bitmentor.api.exceptions.UserNotFoundException
import bitmentor.api.model.user.UpdateUserRequest
import bitmentor.api.service.*
import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
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
fun Routing.initialiseUserEndpoints() = apply {
    get("/users") {
        try {
            call.respond(getUser(extractUserId(call)))
        } catch (e: NumberFormatException) {
            throw BadRequestException("User id must be an integer : $e")
        } catch (e: UserNotFoundException) {
            call.respond(HttpStatusCode.NotFound)
        }
    }

    get("/public/users/{userId}") {
        try {
            val userId = call.parameters["userId"]?.toInt()
                ?: throw BadRequestException("user id is required")
            logger.info { "Getting user public $userId" }

            val user = getUserPublic(userId) ?: throw UserNotFoundException()
            call.respond(user)
        } catch (e: NumberFormatException) {
            throw BadRequestException("User id must be an integer : $e")
        } catch (e: UserNotFoundException) {
            call.respond(HttpStatusCode.NotFound)
        }
    }

    put("/users"){
        try {
            val userUpdate = call.receive<UpdateUserRequest>()

            userUpdate.validate()

            val userId = extractUserId(call)
            logger.info { "Updating user info $userUpdate for $userId" }
            call.respond(HttpStatusCode.OK, userUpdate.update(userId))
        } catch (e: MissingKotlinParameterException) {
            logger.warn { "Malformed payload: $e" }
            call.respond(HttpStatusCode.BadRequest, "Malformed payload")
        } catch (e: UserNotFoundException) {
            call.respond(HttpStatusCode.NotFound)
        }
    }

    delete("/users") {
        try {
            val userId = extractUserId(call)
            logger.info { "UserId: $userId is deleting account" }

            deleteUser(userId)
            call.respond(HttpStatusCode.OK)
        } catch (e: Exception) {
            logger.error { "Encountered exception deleting account e: $e" }
            call.respond(HttpStatusCode.InternalServerError)
        }
    }
}





