package bitmentor.api.repository.entity

import bitmentor.api.model.notification.NotificationType
import java.time.ZonedDateTime

data class NotificationDao(
        val id: Int,
        val userId: Int,
        val type: NotificationType,
        val dateCreated: ZonedDateTime
)