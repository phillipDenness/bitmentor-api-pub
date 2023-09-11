package bitmentor.api.model.auth.github

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class GithubEmailResponse(
    val email: String,
    val verified: Boolean,
    val primary: Boolean
)