package bitmentor.api.endpoints

import bitmentor.api.exceptions.LessonNotFoundException
import bitmentor.api.exceptions.UserNotFoundException
import bitmentor.api.model.payment.PaymentFormRequest
import bitmentor.api.service.extractUserId
import bitmentor.api.service.getLessonsById
import bitmentor.api.service.isLessonValid
import bitmentor.api.service.processFreeOrder
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
fun Routing.initialisePaymentEndpoints() = apply {

    post("/process-payment") {
        try {
            val lessonId = call.parameters["lessonId"]?.toInt()
                    ?: throw BadRequestException("Lesson id required")
            val paymentInsert = call.receive<PaymentFormRequest>()
            val senderId = extractUserId(call)

            logger.info { "Creating payment: $paymentInsert, userId: $senderId" }
            processFreeOrder(
                userId = senderId,
                lessonId = lessonId,
                code = paymentInsert.promoCode
            )
            call.respond(HttpStatusCode.Created)
        } catch (e: UserNotFoundException) {
            call.respond(HttpStatusCode.BadRequest, mapOf("errors" to listOf("Recipient user not found.")))
        }
    }

    get("/process-payment") {
        try {
            val lessonId = call.parameters["lessonId"]?.toInt()
                    ?: throw BadRequestException("Lesson id required")
            val senderId = extractUserId(call)

            logger.info { "Checking payment is allowed for lessonId: $lessonId, userId: $senderId" }
            
            val lesson = getLessonsById(lessonId)
                    ?: throw LessonNotFoundException().also { logger.warn {"Lesson not found when checking payment for lessonId: $lessonId"} }

            if (lesson.isLessonValid(senderId)) {
                call.respond(HttpStatusCode.OK)
            } else {
                call.respond(HttpStatusCode.BadRequest)
            }

        } catch (e: UserNotFoundException) {
            call.respond(HttpStatusCode.BadRequest, mapOf("errors" to listOf("Recipient user not found.")))
        }
    }
}



