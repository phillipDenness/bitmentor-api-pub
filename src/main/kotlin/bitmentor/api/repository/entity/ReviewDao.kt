package bitmentor.api.repository.entity

import java.time.ZonedDateTime

data class ReviewDao(
        val id: Int,
        val tutorId: Int,
        val topic_name: String,
        val studentDisplayName: String,
        val reason: String?,
        val overallRating: Int,
        val studentId: Int,
        val dateCreated: ZonedDateTime
)
