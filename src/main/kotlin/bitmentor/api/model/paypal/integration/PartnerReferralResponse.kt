package bitmentor.api.model.paypal.integration

data class PartnerReferralResponse(
    val links: List<HateosLink>
)

data class HateosLink(
    val href: String,
    val rel: String,
    val method: String,
    val description: String
)