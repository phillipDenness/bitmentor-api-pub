package bitmentor.api.model.lesson

import java.time.ZonedDateTime

data class LessonUpdate(
    val cost: Double,
    val lessonDate: ZonedDateTime,
    val lessonEndDate: ZonedDateTime,
    val topicId: Int
)
