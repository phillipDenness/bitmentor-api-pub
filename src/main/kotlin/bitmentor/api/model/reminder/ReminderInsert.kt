package bitmentor.api.model.reminder

import java.time.ZonedDateTime

data class ReminderInsert(
        val reminderType: ReminderTypes,
        val reminderPayload: ReminderType,
        val triggerDate: ZonedDateTime
)