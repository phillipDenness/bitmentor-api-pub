package bitmentor.api.model.reminder

import bitmentor.api.model.reminder.payloads.RemindAvailablePayout
import bitmentor.api.model.reminder.payloads.RemindLesson
import bitmentor.api.model.reminder.payloads.RemindOpenMeeting
import bitmentor.api.model.reminder.payloads.RemindReview
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    property = "type",
    include = JsonTypeInfo.As.EXISTING_PROPERTY
) @JsonSubTypes(
    value = [
        JsonSubTypes.Type(value = RemindOpenMeeting::class, name = "OPEN_MEETING"),
        JsonSubTypes.Type(value = RemindLesson::class, name = "LESSON_REMINDER"),
        JsonSubTypes.Type(value = RemindReview::class, name = "REVIEW_REMINDER"),
        JsonSubTypes.Type(value = RemindAvailablePayout::class, name = "AVAILABLE_PAYOUT")
    ]
)
abstract class ReminderType(
    open val type: ReminderTypes
)