package bitmentor.api.endpoints


import bitmentor.api.model.short_list.TutorShortListInsert
import bitmentor.api.service.extractUserId
import bitmentor.api.service.getTutorShortList
import bitmentor.api.service.updateTutorShortList
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.*
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

@KtorExperimentalAPI
fun Routing.initialiseTutorShortListEndpoints() = apply {

    get("/shortlist") {
        val userId = extractUserId(call)
        logger.info { "Getting user shortList: $userId" }
        call.respond(HttpStatusCode.OK, getTutorShortList(userId))
    }

    post("/shortlist") {
        val shortListInsert = call.receive<TutorShortListInsert>()
        val userId = extractUserId(call)
        logger.info { "Inserting user short $userId list $shortListInsert " }

        updateTutorShortList(userId = userId, tutorIds = shortListInsert.tutorIds)

        call.respond(HttpStatusCode.OK)
    }
}





