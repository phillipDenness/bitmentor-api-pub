package bitmentor.api.model.payout

import java.time.ZonedDateTime

data class PayoutStatusResource(
        val status: PayoutStatus,
        val dateCreated: ZonedDateTime,
        val payoutId: Int
)