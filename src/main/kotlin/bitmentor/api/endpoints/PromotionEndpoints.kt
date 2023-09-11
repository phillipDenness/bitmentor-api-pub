package bitmentor.api.endpoints

import bitmentor.api.model.promotion.ApplyPromotionRequest
import bitmentor.api.model.promotion.PromotionRequest
import bitmentor.api.model.promotion.PromotionsResource
import bitmentor.api.model.promotion.promotions.Promotion
import bitmentor.api.service.*
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.*
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

@KtorExperimentalAPI
fun Routing.initialisePromotionEndpoints() = apply {
    get("/promotions") {
        val userId = extractUserId(call)
        call.respond(HttpStatusCode.OK, getAllPromotions().toResource())
    }

    post("/apply-promotion") {
        val userId = extractUserId(call)
        val applyPromo = call.receive<ApplyPromotionRequest>()

        logger.info { "applying promotion $applyPromo to user $userId" }

        call.respond(HttpStatusCode.OK, applyPromo.discount(userId))
    }


    post("/promotions") {
        val userId = extractUserId(call)
        val promotionRequest = call.receive<PromotionRequest>()

        logger.info { "Adding promotion $promotionRequest to user $userId" }

        call.respond(HttpStatusCode.OK, promotionRequest.add(userId).toResource())
    }


    put("/promotions") {
        val userId = extractUserId(call)
        val promotionRequest = call.receive<PromotionRequest>()

        logger.info { "removing promotion $promotionRequest from user $userId" }

        call.respond(HttpStatusCode.OK, promotionRequest.remove(userId).toResource())
    }
}

fun List<Promotion>.toResource(): PromotionsResource {
    return PromotionsResource(
            promotions = this
    )
}