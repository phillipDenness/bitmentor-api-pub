package bitmentor.api.model.lesson

import java.time.ZonedDateTime

data class LessonInsert(
    val studentId: Int,
    val cost: Double,
    val lessonDate: ZonedDateTime,
    val lessonEndDate: ZonedDateTime,
    val topicId: Int,
    val enquiryId: Int
)
