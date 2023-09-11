package bitmentor.api.repository.entity

import java.time.ZonedDateTime

data class LessonDao(
        val id: Int,
        val tutorUserId: Int,
        val studentId: Int,
        val dateLesson: ZonedDateTime,
        val endDateLesson: ZonedDateTime,
        val cost: Double,
        val topicId: Int,
        val dateCreated: ZonedDateTime,
        val enquiryId: Int,
        val promoUsed: String? = null
)
