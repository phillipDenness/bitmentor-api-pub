package bitmentor.api.model.order

import bitmentor.api.model.lesson.LessonResource
import bitmentor.api.repository.entity.PaymentStatusDao
import java.time.ZonedDateTime

data class OrderResource(
        val id: Int,
        val lesson: LessonResource,
        val userId: Int,
        val tutorUserId: Int,
        val externalId: String,
        val paymentId: String?,
        val amount: Long,
        val processingFee: Long,
        val refundId: String? = null,
        val statusHistory: List<PaymentStatusDao>,
        val dateUpdated: ZonedDateTime,
        val dateCreated: ZonedDateTime
)