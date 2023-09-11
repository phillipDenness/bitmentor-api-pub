package bitmentor.api.model.promotion.promotions

import bitmentor.api.repository.EnquiryRepository

data class TrialPromotion(
        override val description: String = "Your first lesson is free",
        override val code: String = "TRIAL",
        override val discount: Int = 100
): Promotion(code) {
    companion object {
    }

    override fun isValid(id: Int): Boolean {
        return EnquiryRepository.countConfirmedLessons(id) < 1
    }
}