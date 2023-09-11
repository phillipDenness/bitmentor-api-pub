package bitmentor.api.endpoints

import bitmentor.api.exceptions.InvalidLessonStatusChange
import bitmentor.api.exceptions.UserNotFoundException
import bitmentor.api.model.CreatedResource
import bitmentor.api.model.lesson.LessonInsert
import bitmentor.api.model.lesson.LessonStatusInsert
import bitmentor.api.model.lesson.LessonUpdate
import bitmentor.api.service.*
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.*
import mu.KotlinLogging
import java.time.ZonedDateTime

private val logger = KotlinLogging.logger {}

@KtorExperimentalAPI
fun Routing.initialiseLessonEndpoints() = apply {

    get("/lessons") {
            val start = call.parameters["start"]?.let { ZonedDateTime.parse(it) }?: throw BadRequestException("START is required")
            val end = call.parameters["end"]?.let { ZonedDateTime.parse(it) }?: throw BadRequestException("END is required")

            val enquiryId = call.parameters["enquiryId"]?.toInt()
            val resource = if (enquiryId == null) {
                logger.info { "Fetching lessons start: $start, end: $end" }
                getLessonsByDate(
                        userId = extractUserId(call),
                        start = start,
                        end = end
                )
            } else {
                logger.info { "Fetching lessons by enquiry id: $enquiryId start: $start, end: $end" }
                getLessonsByEnquiryId(
                        enquiryId = enquiryId,
                        userId = extractUserId(call),
                        start = start,
                        end = end
                )
            }
            call.respond(HttpStatusCode.OK, resource)
    }

    get("/lessons/{lessonId}") {
        try {
            val lessonId = call.parameters["lessonId"]?.toInt()
                    ?: throw BadRequestException("Lesson id required")
            call.respond(HttpStatusCode.OK,
                    getLessonsById(
                            lessonId = lessonId
                    ) ?: throw NotFoundException())
        } catch (e: NumberFormatException) {
            throw BadRequestException("Page & Size be an integer : $e")
        } catch (e: UserNotFoundException) {
            throw BadRequestException("The sender or recipient has not completed their registration.")
        }
    }

    post("/lessons") {
        val lessonInsert = call.receive<LessonInsert>()

        logger.info { "Creating lesson $lessonInsert" }

        val senderId = extractUserId(call)
        if (senderId == lessonInsert.studentId) {
            logger.warn { "user $senderId attempted to book a lesson with themself" }
            throw BadRequestException("You may not book a lesson with yourself.")
        }

        call.respond(HttpStatusCode.Created, CreatedResource(lessonInsert.create(senderId)))
    }

    post("/lessons/{lessonId}/status") {
        try {
            val lessonInsert = call.receive<LessonStatusInsert>()
            val lessonId = call.parameters["lessonId"]?.toInt()
                    ?: throw BadRequestException("Lesson id required")

            logger.info { "Creating lesson status for lesson id: $lessonId $lessonInsert" }

            val senderId = extractUserId(call)
            call.respond(HttpStatusCode.Created, CreatedResource(lessonInsert.create(senderId, lessonId)))

        } catch (e: InvalidLessonStatusChange) {
            logger.error { "Exception updating lesson status ${e.message}" }
            call.respond(HttpStatusCode.BadRequest, mapOf("errors" to listOf("This is an invalid change")))
        }
    }

    put("/lessons/{lessonId}") {
            val lessonId = call.parameters["lessonId"]?.toInt()
                    ?: throw BadRequestException("Lesson id required")
            val lessonInsert = call.receive<LessonUpdate>()

            logger.info { "updating lesson id $lessonId with payload $lessonInsert" }

            val senderId = extractUserId(call)

            call.respond(HttpStatusCode.OK, lessonInsert.update(senderId, lessonId))
    }

    delete("/lessons/{lessonId}") {
        val lessonId = call.parameters["lessonId"]?.toInt()
                ?: throw BadRequestException("Lesson id required")
        logger.info { "deleting lesson $lessonId" }

        val senderId = extractUserId(call)
        deleteLesson(
                userId = senderId,
                lessonId = lessonId
        )
        call.respond(HttpStatusCode.NoContent)
    }
}



