package bitmentor.api.model.message

data class MessageInsert(
        val messageContent: String,
        val recipientUserId: Int,
        val enquiryId: Int?
)
