package bitmentor.api.model.paypal

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class RefundResponse(
    val id: String,
    val status: String
)