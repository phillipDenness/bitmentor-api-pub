package bitmentor.api.model.review

data class ReviewUpdate(
        val overallRating: Int,
        val reason: String?
)
