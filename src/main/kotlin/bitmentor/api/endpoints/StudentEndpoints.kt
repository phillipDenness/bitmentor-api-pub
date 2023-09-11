package bitmentor.api.endpoints


import bitmentor.api.exceptions.UserNotFoundException
import bitmentor.api.model.student.StudentPreferenceInsert
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
fun Routing.initialiseStudentEndpoints() = apply {

    post("/student-preference") {
        try {
            val studentPreferenceInsert = call.receive<StudentPreferenceInsert>()

            logger.info { "Creating student preference $studentPreferenceInsert" }

            call.respond(HttpStatusCode.Created, studentPreferenceInsert.create(extractUserId(call)))
        } catch (e: UserNotFoundException) {
            call.respond(HttpStatusCode.BadRequest, mapOf("errors" to listOf("User not found")))
        }
    }
}



