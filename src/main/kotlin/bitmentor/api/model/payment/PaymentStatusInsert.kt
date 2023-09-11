package bitmentor.api.model.payment

data class PaymentStatusInsert(
        val status: PaymentStatus,
        val paymentId: Int
)
