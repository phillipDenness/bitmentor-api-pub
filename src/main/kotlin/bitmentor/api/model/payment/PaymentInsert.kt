package bitmentor.api.model.payment

data class PaymentInsert(
        val lessonId: Int,
        val userId: Int,
        val tutorUserId: Int,
        val externalId: String,
        val paymentId: String?,
        val amount: Long,
        val processingFee: Long
)