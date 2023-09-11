package bitmentor.api.repository.entity

import java.time.ZonedDateTime

data class EnquiryDao(
        val id: Int,
        val tutorUserId: Int,
        val studentUserId: Int,
        val tutorId: Int,
        val topicId: Int,
        val dateCreated: ZonedDateTime
)
