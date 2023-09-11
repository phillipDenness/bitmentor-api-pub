package bitmentor.api.repository.entity

data class PayoutSummaryDao(
    val gross: Long,
    val fees: Long
)