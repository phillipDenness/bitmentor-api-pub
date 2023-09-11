package bitmentor.api.service

import bitmentor.api.model.notification.NotificationResource
import bitmentor.api.model.notification.NotificationType
import bitmentor.api.repository.NotificationRepository
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

fun getUserNotifications(userId: Int): NotificationResource {
    val notifications = NotificationRepository.getNotifications(userId)

    return NotificationResource(
            notifications = notifications,
            totalNotifications = notifications.size
    )
}

fun clearUserNotifications(userId: Int) {
    NotificationRepository.deleteNotification(userId)
}

fun clearUserNotifications(userId: Int, type: NotificationType) {
    NotificationRepository.deleteNotification(userId, type)
}