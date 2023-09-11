package bitmentor.api.endpoints

import bitmentor.api.model.topic.DisciplineResource
import bitmentor.api.model.topic.DisciplinesResource
import bitmentor.api.model.topic.TopicDao
import bitmentor.api.model.topic.TopicsResource
import bitmentor.api.service.getTopTopicByGradJobs
import bitmentor.api.service.getTopTopics
import bitmentor.api.service.getTopics
import bitmentor.api.service.getTopicsByDiscipline
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.*
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

@KtorExperimentalAPI
fun Routing.initialiseTopicEndpoints() = apply {

    get("/public/topics") {
        val discipline = call.parameters["discipline"]
        if (discipline != null) {
            logger.info { "Fetching all topics" }
            call.respond(HttpStatusCode.OK, getTopicsByDiscipline(discipline).toDisciplineResource())
        } else {
            logger.info { "Fetching all topics by discipline $discipline" }
            call.respond(HttpStatusCode.OK, getTopics().toDisciplineResource())
        }
    }

    get("/public/topics/stats") {
        val gradJobs = call.parameters["gradJobs"]?.toBoolean()

        if (gradJobs != null && gradJobs) {
            call.respond(HttpStatusCode.OK, getTopTopicByGradJobs(5).toResource())
        } else {
            call.respond(HttpStatusCode.OK, getTopTopics(10).toResource())
        }
    }
}

fun List<TopicDao>.toDisciplineResource(): DisciplinesResource {
    val topicsByDiscipline = this.distinctBy { it.discipline }.map { distinctDiscipline ->
        DisciplineResource(
            disciplineId = distinctDiscipline.disciplineId,
            discipline = distinctDiscipline.discipline,
            topics = this.filter { it.discipline == distinctDiscipline.discipline }
        )
    }
    return DisciplinesResource(
        disciplines = topicsByDiscipline,
        total = topicsByDiscipline.size
    )
}

fun List<TopicDao>.toResource(): TopicsResource {
    return TopicsResource(
            topics = this,
            total = this.size
    )
}
