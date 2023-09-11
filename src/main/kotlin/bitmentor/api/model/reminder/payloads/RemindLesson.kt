package bitmentor.api.model.reminder.payloads

import bitmentor.api.model.lesson.LessonResource
import bitmentor.api.model.reminder.ReminderType
import bitmentor.api.model.reminder.ReminderTypes

data class RemindLesson(
        val studentId: Int,
        val tutorUserId: Int,
        val lessonResource: LessonResource,
        override val type: ReminderTypes = ReminderTypes.LESSON_REMINDER
): ReminderType(type)