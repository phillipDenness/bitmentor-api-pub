package bitmentor.api.endpoints

import bitmentor.api.model.notification.NotificationType
import bitmentor.api.service.clearUserNotifications
import bitmentor.api.service.extractUserId
import bitmentor.api.service.getUserNotifications
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.*
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

@KtorExperimentalAPI
fun Routing.initialiseNotificationEndpoints() = apply {

    get("/notifications") {
        call.respond(HttpStatusCode.OK, getUserNotifications(
                userId = extractUserId(call)
        ))
    }

    delete("/notifications") {
        val deleteType = call.parameters["type"]
        val userId = extractUserId(call)

        logger.info { "deleting notifications message" }
        if (deleteType != null) {
            val type = NotificationType.valueOf(deleteType)
            if (type == NotificationType.COMPLETE_ACCOUNT) {
                call.respond(HttpStatusCode.NoContent)
            }

            clearUserNotifications(userId, type)
            call.respond(HttpStatusCode.NoContent)
        } else {
            clearUserNotifications(userId)
            call.respond(HttpStatusCode.NoContent)
        }
    }

}



