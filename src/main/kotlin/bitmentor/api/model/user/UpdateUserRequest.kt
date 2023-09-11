package bitmentor.api.model.user

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class UpdateUserRequest(
        val title: String,
        val displayName: String,
        val firstName: String,
        val middleName: String?,
        val lastName: String
)