package bitmentor.api.model.notification

data class NotificationInsert(
        val userId: Int,
        val type: NotificationType
)