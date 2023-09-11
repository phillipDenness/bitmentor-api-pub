package bitmentor.api.model.paypal.integration

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class MerchantIntegrationResponse(
    val merchant_id: String,
    val tracking_id: String,
    val payments_receivable: Boolean,
    val primary_email_confirmed: Boolean
)