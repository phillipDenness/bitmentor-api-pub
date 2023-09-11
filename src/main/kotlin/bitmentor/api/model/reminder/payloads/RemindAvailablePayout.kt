package bitmentor.api.model.reminder.payloads

import bitmentor.api.model.order.OrderResource
import bitmentor.api.model.reminder.ReminderType
import bitmentor.api.model.reminder.ReminderTypes

data class RemindAvailablePayout(
        val orderResource: OrderResource,
        override val type: ReminderTypes = ReminderTypes.AVAILABLE_PAYOUT
): ReminderType(type)