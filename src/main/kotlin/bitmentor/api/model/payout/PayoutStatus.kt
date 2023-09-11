package bitmentor.api.model.payout

enum class PayoutStatus {
    PENDING,
    AVAILABLE,
    REQUESTED,
    CANCELLED,
    DISPUTED,
    COMPLETE
}