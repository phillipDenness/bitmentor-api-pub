package bitmentor.api.model.reminder.payloads

import bitmentor.api.model.lesson.LessonResource
import bitmentor.api.model.reminder.ReminderType
import bitmentor.api.model.reminder.ReminderTypes

data class RemindOpenMeeting(
        val lessonResource: LessonResource,
        val duration: Int,
        override val type: ReminderTypes = ReminderTypes.OPEN_MEETING
): ReminderType(type)