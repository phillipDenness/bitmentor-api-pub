package bitmentor.api.model.reminder

import java.time.ZonedDateTime

data class ReminderResource (
        val id: Int,
        val reminderType: ReminderTypes,
        val reminderPayload: ReminderType,
        val triggerDate: ZonedDateTime,
        val dateCreated: ZonedDateTime
)