package bitmentor.api.model.message

import java.time.ZonedDateTime

data class MessageResource(
        val id: Int,
        val enquiryId: Int,
        val senderUserId: Int,
        val recipientUserId: Int,
        val senderDisplayName: String,
        val recipientDisplayName: String,
        val senderProfileUrl: String?,
        val dateCreated: ZonedDateTime,
        val messageContent: String,
        val tutorUserId: Int,
        val studentUserId: Int,
        val studentDisplayName: String,
        val tutorDisplayName: String,
        val tutorId: Int
)
