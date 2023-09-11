package bitmentor.api.model.promotion

data class ApplyPromotionRequest(
        val code: String,
        val enquiryId: Int,
        val lessonId: Int
)