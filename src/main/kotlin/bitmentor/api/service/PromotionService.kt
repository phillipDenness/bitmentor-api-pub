package bitmentor.api.service

import bitmentor.api.model.lesson.LessonResource
import bitmentor.api.model.promotion.ApplyPromoResponse
import bitmentor.api.model.promotion.ApplyPromotionRequest
import bitmentor.api.model.promotion.PromotionRequest
import bitmentor.api.model.promotion.promotions.Loyalty25Promotion
import bitmentor.api.model.promotion.promotions.Promotion
import bitmentor.api.model.promotion.promotions.TrialPromotion
import bitmentor.api.repository.LessonRepository
import bitmentor.api.repository.TutorRepository
import io.ktor.features.*
import io.ktor.util.*
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

val availablePromotions = listOf(TrialPromotion(), Loyalty25Promotion())

fun getAllPromotions(): List<Promotion> {
    return availablePromotions
}

@KtorExperimentalAPI
fun ApplyPromotionRequest.discount(userId: Int): ApplyPromoResponse {
    val enquiry = getEnquiryById(enquiryId = enquiryId, userId = userId).takeIf { it.enquiries.isNotEmpty() }
        ?: throw BadRequestException("Enquiry $enquiryId does not exist for user $userId")

    val tutor = getTutor(enquiry.enquiries.first().tutorId)
        ?: throw BadRequestException("Could not find tutorId ${enquiry.enquiries.first().tutorId} for enquiry ${enquiry.enquiries.first()}")

    val promotion = tutor.promotions.find { it.code == code } ?: throw BadRequestException("Invalid promo code")
        .also { logger.warn { "user $userId attempted to use code $code which does not exist on tutor ${tutor.id}" } }

    val lesson = getLessonsById(lessonId = lessonId)
        ?: throw BadRequestException("Lesson not found: $lessonId")

    if (promotion.isValid(enquiryId)) {
        val discount = calculateDiscount(lessonCost = lesson.cost, discount = promotion.discount)
        logger.info { "Promotion is valid and calculating new price. Discount $discount. Total ${lesson.cost - discount}" }
        return ApplyPromoResponse(
                promotion = promotion,
                oldTotal = lesson.cost,
                discount = discount,
                total = lesson.cost - discount
        )
    } else {
        logger.warn { "user $userId attempted to use code $code which is not valid" }
        throw BadRequestException("Invalid promo code")
    }
}

@KtorExperimentalAPI
fun applyPromo(code: String, lessonResource: LessonResource, userId: Int): Double {
    val enquiry = getEnquiryById(enquiryId = lessonResource.enquiryId, userId = userId).takeIf { it.enquiries.isNotEmpty() }
            ?: throw BadRequestException("Enquiry ${lessonResource.enquiryId} does not exist for user $userId")

    val tutor = getTutor(enquiry.enquiries.first().tutorId)
            ?: throw BadRequestException("Could not find tutor for enquiry")

    val promotion = tutor.promotions.find { it.code == code } ?: throw BadRequestException("Invalid promo code")
            .also { logger.warn { "user $userId attempted to use code $code which does not exist on tutor ${tutor.id}" } }

    if (promotion.isValid(lessonResource.enquiryId)) {
        val discount = calculateDiscount(lessonCost = lessonResource.cost, discount = promotion.discount)
        logger.info { "Promotion is valid and calculating new price. Discount $discount. Total cost ${lessonResource.cost}" }
        try {
            LessonRepository.saveLessonPromo(lessonId = lessonResource.id, code = code)
        } catch (e: Exception) {
            logger.error { "Error saving used promo code: $code to lesson ${lessonResource.id}. e: $e" }
        }
        return lessonResource.cost - discount
    } else {
        logger.warn { "user $userId attempted to use code $code which is not valid" }
        throw BadRequestException("Invalid promo code")
    }
}

@KtorExperimentalAPI
fun PromotionRequest.add(userId: Int): List<Promotion> {
    val tutor = TutorRepository.findTutorByUserId(userId)
            ?: throw BadRequestException("Only tutors can add promotions")
    val promotions = tutor.promotions?.toMutableList() ?: emptyList<String>().toMutableList()

    codes.forEach { code ->
        if (availablePromotions.find { it.code == code } == null) {
            throw BadRequestException("$code is not a valid promotion")
        }
        if (promotions.contains(code)) {
            throw BadRequestException("You can only add a code once")
        }
        promotions.add(code)
    }
    TutorRepository.updatePromotions(tutorId = tutor.id, promotions = promotions)

    return getAllPromotions().filter { promotions.contains(it.code) }
}

@KtorExperimentalAPI
fun PromotionRequest.remove(userId: Int): List<Promotion> {
    val tutor = TutorRepository.findTutorByUserId(userId)
            ?: throw BadRequestException("Only tutors can remove promotions")
    val promotions = tutor.promotions?.toMutableList() ?: emptyList<String>().toMutableList()
    codes.forEach { code ->
        if (promotions.contains(code)) {
            promotions.remove(code)
        }
    }
    TutorRepository.updatePromotions(tutorId = tutor.id, promotions = promotions)

    return getAllPromotions().filter { promotions.contains(it.code) }
}

fun calculateDiscount(lessonCost: Double, discount: Int): Double {
    return lessonCost * (discount.toDouble() / 100)
}