package bitmentor.api.repository.entity

data class PaypalDao(
        val orderId: String,
        val lessonId: Int,
        val status: String,
        val grossAmount: String,
        val netAmount: String? = null,
        val paypalFee: String? = null,
        val captureId: String? = null
)