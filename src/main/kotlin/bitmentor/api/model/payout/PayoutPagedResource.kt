package bitmentor.api.model.payout

data class PayoutPagedResource(
        val payouts: List<PayoutResource>,
        val summary: PayoutSummary,
        val total: Int,
        val page: Int,
        val size: Int
)