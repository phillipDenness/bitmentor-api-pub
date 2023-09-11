package bitmentor.api.model.auth

data class UserLoginResponse(
        val token: String,
        val user: UserSession
)

data class UserSession (
    val id: Int,
    val tutorId: Int?,
    val completedRegistration: Boolean,
    val loginType: LoginTypes?
)

data class UserInfo (
        val userId: Int,
        val tutorId: Int?,
        val completedRegistration: Boolean
)
