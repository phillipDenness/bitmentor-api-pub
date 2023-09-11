package bitmentor.api.model.user

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class LocationPublic(
        val city: String,
        val country: String
)