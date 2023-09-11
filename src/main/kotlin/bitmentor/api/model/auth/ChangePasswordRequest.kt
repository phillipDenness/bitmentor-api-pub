package bitmentor.api.model.auth

data class ChangePasswordRequest(
        val password: String,
        val currentPassword: String
)
