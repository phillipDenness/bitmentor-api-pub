package bitmentor.api.model.message


data class MessagePagedResource(
        val messages: List<MessageResource>,
        val totalMessages: Int,
        val page: Int,
        val size: Int
)