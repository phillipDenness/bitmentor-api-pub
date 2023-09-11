package bitmentor.api.repository.entity

import java.time.ZonedDateTime

data class TutorProjectDao(
    val id: Int,
    val tutorId: Int,
    val title: String,
    val description: String,
    val link: String?,
    val lastModified: ZonedDateTime
)