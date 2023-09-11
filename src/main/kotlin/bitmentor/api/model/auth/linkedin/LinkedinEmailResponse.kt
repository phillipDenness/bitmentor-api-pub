package bitmentor.api.model.auth.linkedin

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class LinkedinEmailResponse(
    val elements: List<LinkedinHandle>
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class LinkedinHandle(
        @JsonProperty("handle~")
        val handle: LinkedinHandleEmail
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class LinkedinHandleEmail(
        val emailAddress: String
)