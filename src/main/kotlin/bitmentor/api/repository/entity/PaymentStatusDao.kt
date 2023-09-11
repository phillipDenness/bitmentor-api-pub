package bitmentor.api.repository.entity

import bitmentor.api.model.payment.PaymentStatus
import java.time.ZonedDateTime

data class PaymentStatusDao(
        val id: Int,
        val status: PaymentStatus,
        val paymentId: Int,
        val dateCreated: ZonedDateTime
)
