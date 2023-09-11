package bitmentor.api.model.auth

data class UserLoginRequest(
        val email: String,
        val password: String
)
