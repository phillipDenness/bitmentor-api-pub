package bitmentor.api.model.notification

import bitmentor.api.repository.entity.NotificationDao

data class NotificationResource(
        val notifications: List<NotificationDao>,
        val totalNotifications: Int
)