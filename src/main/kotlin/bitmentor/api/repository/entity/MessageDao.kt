package bitmentor.api.repository.entity

import org.jdbi.v3.core.mapper.reflect.ColumnName
import java.time.ZonedDateTime

data class MessageDao(
        @ColumnName("message_id")
        val id: Int,
        val senderId: Int,
        val recipientId: Int,
        val enquiryId: Int,
        val dateCreated: ZonedDateTime,
        val messageContent: String,
        val tutorUserId: Int,
        val studentUserId: Int
)