package bitmentor.api.model.promotion.promotions

import bitmentor.api.repository.EnquiryRepository

data class Loyalty25Promotion(
        override val description: String = "Your third lesson is 25% off",
        override val code: String = "LOYALTY25",
        override val discount: Int = 25
): Promotion(code) {
    companion object {
    }

    override fun isValid(id: Int): Boolean {
        return EnquiryRepository.countConfirmedLessons(id) == 2
    }
}