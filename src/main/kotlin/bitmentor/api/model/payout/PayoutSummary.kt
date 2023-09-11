package bitmentor.api.model.payout

data class PayoutSummary(
    val availablePayout: Long,
    val pendingPayout: Long,
    val disputedPayout: Long? = null
)