package bitmentor.api.model.review

import java.time.ZonedDateTime

data class ReviewResource(
        val id: Int,
        val tutorId: Int,
        val reason: String?,
        val studentId: Int,
        val dateCreated: ZonedDateTime,
        val studentDisplayName: String,
        val studentProfileImageUrl: String?,
        val overallRating: Int,
        val topic: String
)