package bitmentor.api.repository.entity

import java.time.ZonedDateTime

data class PayoutDao(
        val id: Int,
        val tutorUserId: Int,
        val amount: Long,
        val processingFee: Long,
        val lessonId: Int,
        val dateCreated: ZonedDateTime,
        val paymentId: Int
)