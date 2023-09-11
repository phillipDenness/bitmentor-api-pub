package bitmentor.api.endpoints

import bitmentor.api.endpoints.helpers.validatePaging
import bitmentor.api.exceptions.TutorAlreadyRegisteredException
import bitmentor.api.exceptions.TutorNotFoundException
import bitmentor.api.exceptions.TutorNotVerifiedException
import bitmentor.api.exceptions.UserNotFoundException
import bitmentor.api.model.tutor.TutorInsert
import bitmentor.api.model.tutor.TutorSort
import bitmentor.api.model.tutor.UpdateTutorRequest
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
fun Routing.initialiseTutorEndpoints() = apply {

    get("/public/tutors") {
        try {

            val disciplineId = call.parameters["disciplineId"]?.toInt()
            val topicId = call.parameters["topicId"]?.toInt()
            val pageAndSize = validatePaging(call.parameters)
            val dbsVerified = call.parameters["dbsVerified"]?.toBoolean()
            val sortString = call.parameters["sort"]
            val sort = sortString?.let { TutorSort.valueOf(it) } ?: TutorSort.JOIN_DATE_DESC

            val resource = if (topicId != null && topicId != 0) {
                logger.info { "Getting tutors for topicId: $topicId page: ${pageAndSize.first} and size: ${pageAndSize.second}, dbsVerified $dbsVerified, sort: $sort" }
                getTutorsByTopicId(
                    topicId = topicId,
                    page = pageAndSize.first,
                    size = pageAndSize.second,
                    sort = sort,
                    dbsVerified = dbsVerified
                )
            } else if (disciplineId != null && disciplineId != 0) {
                logger.info { "Getting tutors for disciplineId: $disciplineId page: ${pageAndSize.first} and size: ${pageAndSize.second}, dbsVerified $dbsVerified, sort: $sort" }
                getTutorsByDisciplineId(
                    disciplineId = disciplineId,
                    page = pageAndSize.first,
                    size = pageAndSize.second,
                    sort = sort,
                    dbsVerified = dbsVerified
                )
            } else {
                logger.info { "Getting tutors, page ${pageAndSize.first} and size ${pageAndSize.second}, dbsVerified $dbsVerified, sort: $sort" }
                getTutors(
                    page = pageAndSize.first,
                    size = pageAndSize.second,
                    sort = sort,
                    dbsVerified = dbsVerified
                )
            }

            call.respond(HttpStatusCode.OK, resource)
        } catch (e: NumberFormatException) {
            throw BadRequestException("Tutor id must be an integer : $e")
        }
    }

    get("/public/tutors/{tutorId}") {
        try {
            val tutorId = call.parameters["tutorId"]?.toInt()
                ?: throw BadRequestException("Tutor id must be provided")
                        .also { logger.warn { "Tutor id must be provided" } }

            logger.info { "Getting tutor for id $tutorId" }
            val tutorResource = getTutor(tutorId) ?: throw TutorNotFoundException()
            call.respond(HttpStatusCode.OK, tutorResource)
        } catch (e: NumberFormatException) {
            throw BadRequestException("Tutor id must be an integer : $e")
        } catch (e: TutorNotFoundException) {
            call.respond(HttpStatusCode.NotFound, mapOf("errors" to listOf("Tutor not found")))
        }
    }

    post("/tutors") {
        try {
            val tutorRequest = call.receive<TutorInsert>()

            logger.info { "Creating tutor profile $tutorRequest" }

            call.respond(HttpStatusCode.Created, tutorRequest.createTutor(extractUserId(call)))
        } catch (e: UserNotFoundException) {
            call.respond(HttpStatusCode.BadRequest, mapOf("errors" to listOf("User not found")))
        } catch (e: TutorAlreadyRegisteredException) {
            call.respond(HttpStatusCode.BadRequest,mapOf("errors" to listOf("User already registered as a tutor")))
        }
    }

    put("/tutors") {
        try {
            val tutorRequest = call.receive<UpdateTutorRequest>()

            logger.info { "Updating tutor profile" }
            val tutorResource = tutorRequest.updateTutor(extractUserId(call))
            call.respond(HttpStatusCode.OK, tutorResource)
        } catch (e: NumberFormatException) {
            throw BadRequestException("Tutor id must be an integer : $e")
        } catch (e: TutorNotFoundException) {
            call.respond(HttpStatusCode.NotFound, mapOf("errors" to listOf("Tutor not found")))
        } catch (e: TutorNotVerifiedException) {
            call.respond(HttpStatusCode.BadRequest, mapOf("errors" to listOf("Tutor must be verified before becoming active")))
        }
    }
}



