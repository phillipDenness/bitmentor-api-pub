package bitmentor.api.model.payout

import bitmentor.api.model.lesson.LessonResource
import java.time.ZonedDateTime

data class PayoutResource(
        val id: Int,
        val tutorUserId: Int,
        val amount: Long,
        val processingFee: Long,
        val lesson: LessonResource,
        val dateCreated: ZonedDateTime,
        val paymentId: Int,
        val statuses: List<PayoutStatusResource>
)