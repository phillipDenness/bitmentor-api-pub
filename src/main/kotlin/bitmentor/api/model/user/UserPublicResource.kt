package bitmentor.api.model.user

import java.time.LocalDate
import java.time.ZonedDateTime

data class UserPublicResource(
        val id: Int,
        val joinDate: LocalDate,
        val displayName: String?,
        val title: String?,
        val firstName: String?,
        val lastOnline: ZonedDateTime,
        val lastModified: ZonedDateTime,
        val profileImageUrl: String?
)