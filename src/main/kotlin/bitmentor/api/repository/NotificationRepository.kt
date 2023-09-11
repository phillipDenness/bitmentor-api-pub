package bitmentor.api.repository

import bitmentor.api.config.SharedJdbi
import bitmentor.api.model.notification.NotificationInsert
import bitmentor.api.model.notification.NotificationType
import bitmentor.api.repository.entity.NotificationDao
import mu.KotlinLogging
import org.jdbi.v3.core.Handle

object NotificationRepository {
    private val logger = KotlinLogging.logger {}

    private const val NOTIFICATION_DAO_QUERY: String = """
                        SELECT 
                            id,
                            user_id, 
                            type, 
                            date_created
                        FROM 
                            bitmentor.notification"""

    fun getNotifications(userId: Int): List<NotificationDao> {
        return SharedJdbi.jdbi().inTransaction<List<NotificationDao>, Exception> { handle ->
            getNotifications(userId = userId, handle = handle)
        }
    }

    fun createNotification(
            notificationInsert: NotificationInsert,
            handle: Handle
    ): Int {
        return handle.createUpdate(
                    """INSERT INTO bitmentor.notification(
                                user_id, 
                                type, 
                                date_created
                        )VALUES(
                                :user_id, 
                                :type,
                                now()
                        )"""
            )
                    .bind("user_id", notificationInsert.userId)
                    .bind("type", notificationInsert.type)
                    .executeAndReturnGeneratedKeys("id")
                    .mapTo(Int::class.java).first()
    }

    fun deleteNotification(userId: Int, type: NotificationType, handle: Handle) {
        try {
                handle.createUpdate(
                    """DELETE FROM 
                                bitmentor.notification
                            WHERE user_id = :user_id AND type = :type;"""
                )
                    .bind("user_id", userId)
                    .bind("type", type)
                    .execute()
        } catch (e: Exception) {
            logger.error { "An error has occurred deleting the notification ${e.message}" }
            throw Exception("An error has occurred deleting the notification")
        }
    }

    fun deleteNotification(userId: Int) {
        return SharedJdbi.jdbi().inTransaction<Unit, Exception> { handle ->
            try {
                handle.createUpdate(
                        """DELETE FROM 
                                bitmentor.notification
                            WHERE user_id = :user_id"""
                )
                        .bind("user_id", userId)
                        .execute()
            } catch (e: Exception) {
                logger.error { "An error has occurred deleting the notification ${e.message}" }
                throw Exception("An error has occurred deleting the notification")
            }
        }
    }

    fun deleteNotification(userId: Int, type: NotificationType) {
        return SharedJdbi.jdbi().inTransaction<Unit, Exception> { handle ->
            deleteNotification(userId, type, handle)
        }
    }

    private fun getNotifications(userId: Int, handle: Handle): List<NotificationDao> {
        return handle.createQuery(
            """$NOTIFICATION_DAO_QUERY WHERE user_id = :user_id"""
        )
            .bind("user_id", userId)
            .mapTo(NotificationDao::class.java)
            .list()
    }
}
