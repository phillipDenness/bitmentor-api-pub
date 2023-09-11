package bitmentor.api.model.reminder.payloads

import bitmentor.api.model.lesson.LessonResource
import bitmentor.api.model.reminder.ReminderType
import bitmentor.api.model.reminder.ReminderTypes

data class RemindReview(
        val studentId: Int,
        val tutorUserId: Int,
        val lessonResource: LessonResource,
        override val type: ReminderTypes = ReminderTypes.REVIEW_REMINDER
): ReminderType(type)