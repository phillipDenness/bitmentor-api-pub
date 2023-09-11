package bitmentor.api.model.starling

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class StarlingResponse(
    val payeeUid: String,
    val success: Boolean,
    val errors: List<StarlingError>
)

data class StarlingError(
    val message: String
)