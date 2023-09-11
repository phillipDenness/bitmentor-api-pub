package bitmentor.api.repository.entity

import bitmentor.api.model.student.AreasOfInterest
import java.time.ZonedDateTime

data class StudentPreferenceDao(
    val id: Int,
    val userId: Int,
    val topicIds: List<Int>,
    val interests: List<AreasOfInterest>,
    val other: String?,
    val dateCreated: ZonedDateTime
)
