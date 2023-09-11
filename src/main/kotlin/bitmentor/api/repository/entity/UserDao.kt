package bitmentor.api.repository.entity

import org.jdbi.v3.core.mapper.reflect.ColumnName

data class UserDao(
        @ColumnName("email")
        val email: String,

        @ColumnName("id")
        val id: Int,

        @ColumnName("display_name")
        val displayName: String?
)
