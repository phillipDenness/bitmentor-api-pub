package bitmentor.api.endpoints

import bitmentor.api.config.GenericObjectMapper
import bitmentor.api.config.Properties
import bitmentor.api.model.paypal.endpoint.PaypalCreatedOrder
import bitmentor.api.model.paypal.endpoint.PaypalEmailRequest
import bitmentor.api.model.paypal.endpoint.PaypalFormRequest
import bitmentor.api.model.paypal.webhook.PaypalDispute
import bitmentor.api.service.*
import com.paypal.api.payments.Event
import com.paypal.base.Constants
import com.paypal.base.rest.APIContext
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
fun Routing.initialisePaypalEndpoints() = apply {

    post("/paypal-order") {
        val lessonId = call.parameters["lessonId"]?.toInt()
            ?: throw BadRequestException("Lesson id required")

        val paymentInsert = call.receive<PaypalFormRequest>()
        val userId = extractUserId(call)
        logger.info { "Creating paypal order for lessonId $lessonId and userId $userId" }

        call.respond(HttpStatusCode.Created, PaypalCreatedOrder(processPaypalOrder(lessonId = lessonId, userId = userId, code = paymentInsert.promoCode)))
    }

    post("/paypal-order/{orderId}/capture") {
        val userId = extractUserId(call)
        val orderId = call.parameters["orderId"] ?: throw BadRequestException("Order id is missing")
        logger.info { "Capturing paypal orderId: $orderId for user $userId" }
        handleOrderApproval(orderId = orderId, userId = userId)
        call.respond(HttpStatusCode.Created)
    }

    get("/paypal-order/{orderId}") {
        val userId = extractUserId(call)
        val orderId = call.parameters["orderId"] ?: throw BadRequestException("Order id is missing")
        logger.info { "Getting paypal orderId: $orderId for user $userId" }

        val order = getPaypalOrder(orderId, userId)
        if (order == null) {
            call.respond(HttpStatusCode.NotFound)
        } else {
            call.respond(HttpStatusCode.OK, order)
        }
    }

    post("/paypal-payout") {
        val userId = extractUserId(call)
        val emailInsert = call.receive<PaypalEmailRequest>()

        logger.info { "Creating new paypal email for $userId $emailInsert" }
        createPaypalEmail(emailInsert.paypalEmailAddress, userId)

        call.respond(HttpStatusCode.OK)
    }

    delete("/paypal-payout") {
        val userId = extractUserId(call)
        logger.info { "Deleting paypal email for $userId" }
        deletePaypalEmail(userId)

        call.respond(HttpStatusCode.OK)
    }

    post("/public/dispute-webhook") {
        val webhookDisputeBody = call.receive<String>()

        val apiContext = APIContext(Properties.paypalClientId, Properties.paypalSecret, if (Properties.paypalSandbox) { "sandbox"} else { "live"})
        apiContext.addConfiguration(Constants.PAYPAL_WEBHOOK_ID, Properties.paypalDisputeWebhook)
        val map: Map<String, String> = call.request.headers.toMap().mapValues { it.value[0] }

        if (Event.validateReceivedEvent(apiContext, map, webhookDisputeBody)) {
            val webhookDispute = GenericObjectMapper.getMapper().readValue(webhookDisputeBody, PaypalDispute::class.java)
            if (webhookDispute.event_type == "CUSTOMER.DISPUTE.CREATED") {
                handlePaypalDispute(webhookDispute)
                call.respond(HttpStatusCode.OK)
            } else {
                call.respond(HttpStatusCode.OK)
            }
        } else {
            logger.warn { "Event is not validated. Will not process" }
            call.respond(HttpStatusCode.Unauthorized)
        }
    }
}



