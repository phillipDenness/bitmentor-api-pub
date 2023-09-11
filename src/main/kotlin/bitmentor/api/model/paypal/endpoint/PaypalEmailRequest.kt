package bitmentor.api.model.paypal.endpoint

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class PaypalEmailRequest(
    val paypalEmailAddress: String
)