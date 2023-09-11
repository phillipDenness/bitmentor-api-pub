package bitmentor.api.repository.entity

import bitmentor.api.model.lesson.LessonStates
import java.time.ZonedDateTime

data class LessonStateDao(
        val id: Int,
        val status: LessonStates,
        val dateCreated: ZonedDateTime
)
