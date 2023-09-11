package bitmentor.api.repository.entity

import java.time.ZonedDateTime

data class OrderDao(
        val id: Int,
        val lessonId: Int,
        val userId: Int,
        val tutorUserId: Int,
        val externalId: String,
        val paymentId: String?,
        val amount: Long,
        val processingFee: Long,
        val refundId: String? = null,
        val dateUpdated: ZonedDateTime,
        val dateCreated: ZonedDateTime
)