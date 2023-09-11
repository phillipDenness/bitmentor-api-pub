package bitmentor.api.model.auth

data class ResetPasswordRequest(
        val password: String,
        val token: String
)
