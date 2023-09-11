package bitmentor.api.model.review

data class ReviewInsert(
        val tutorId: Int,
        val lessonId: Int,
        val overallRating: Int,
        val reason: String?
)
