package bitmentor.api.repository.entity

import bitmentor.api.model.reminder.ReminderTypes
import java.time.ZonedDateTime

data class ReminderDao(
        val id: Int,
        val reminderType: ReminderTypes,
        val reminderPayload: String,
        val triggerDate: ZonedDateTime,
        val dateCreated: ZonedDateTime,
        val error: String?
)