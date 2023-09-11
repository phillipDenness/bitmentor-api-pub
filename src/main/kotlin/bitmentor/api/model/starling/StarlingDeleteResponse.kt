package bitmentor.api.model.starling

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class StarlingDeleteResponse(
    val success: Boolean,
    val errors: List<StarlingError>
)
