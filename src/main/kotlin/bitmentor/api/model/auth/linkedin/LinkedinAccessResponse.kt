package bitmentor.api.model.auth.linkedin

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class LinkedinAccessResponse(
        val access_token: String
)