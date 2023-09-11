package bitmentor.api.repository.entity

import org.jdbi.v3.core.mapper.reflect.ColumnName
import java.time.LocalDate
import java.time.ZonedDateTime

data class UserAccountDao(
        val id: Int,
        val email: String,

        val title: String?,
        @ColumnName("join_date")
        val joinDate: LocalDate,
        @ColumnName("display_name")
        val displayName: String?,
        @ColumnName("first_name")
        val firstName: String?,
        val middleName: String?,
        @ColumnName("last_name")
        val lastName: String?,
        @ColumnName("last_online")
        val lastOnline: ZonedDateTime,
        @ColumnName("last_modified")
        val lastModified: ZonedDateTime,
        @ColumnName("profile_image_url")
        val profileImageUrl: String?,

        val shortListTutors: Set<Int>?
)