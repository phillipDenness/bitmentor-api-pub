package bitmentor.api.model.user

import java.time.LocalDate
import java.time.ZonedDateTime

data class UserResource(
        val id: Int,
        val email: String,
        val joinDate: LocalDate,
        val title: String?,
        val displayName: String?,
        val firstName: String?,
        val middleName: String?,
        val lastName: String?,
        val lastOnline: ZonedDateTime,
        val lastModified: ZonedDateTime,
        val profileImageUrl: String?
)