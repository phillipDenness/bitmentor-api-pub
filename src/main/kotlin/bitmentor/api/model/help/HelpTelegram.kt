package bitmentor.api.model.help

data class HelpTelegram(
        val message: String,
        val userId: Int,
        val email: String,
        val displayName: String,
        val category: String,
        val lessonId: Int? = null,
        val isOrderIdDisputed: Int? = null
)