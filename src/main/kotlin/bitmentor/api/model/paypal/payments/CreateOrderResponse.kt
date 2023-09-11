package bitmentor.api.model.paypal.payments

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class CreateOrderResponse(
    val id: String,
    val status: String
)