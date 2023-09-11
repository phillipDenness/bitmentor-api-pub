package bitmentor.api.model.paypal.integration

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class MerchantIntegrationQuery(
    val merchant_id: String,
    val tracking_id: String
)