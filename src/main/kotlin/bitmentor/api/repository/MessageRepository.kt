package bitmentor.api.repository

import bitmentor.api.config.SharedJdbi
import bitmentor.api.exceptions.UserAccountIncompleteException
import bitmentor.api.exceptions.UserAuthenticationException
import bitmentor.api.exceptions.UserNotFoundException
import bitmentor.api.model.message.MessageInsert
import bitmentor.api.model.message.MessageResource
import bitmentor.api.model.notification.NotificationInsert
import bitmentor.api.model.notification.NotificationType
import bitmentor.api.repository.entity.MessageDao
import mu.KotlinLogging
import org.jdbi.v3.core.Handle

object MessageRepository {
    private val logger = KotlinLogging.logger {}

    fun createMessage(
        messageInsert: MessageInsert,
        senderId: Int,
        handle: Handle
    ): Int {
        try {
            return handle.createUpdate(
                    """INSERT INTO bitmentor.message(
                                        recipient_id, 
                                        sender_id, 
                                        message_content,
                                        date_created,
                                        enquiry_id
                                )VALUES(
                                        :recipient_id, 
                                        :sender_id, 
                                        :message_content,
                                        now(),
                                        :enquiry_id
                                )"""
                )
                    .bind("recipient_id", messageInsert.recipientUserId)
                    .bind("sender_id", senderId)
                    .bind("message_content", messageInsert.messageContent)
                    .bind("enquiry_id", messageInsert.enquiryId)
                    .executeAndReturnGeneratedKeys("id")
                    .mapTo(Int::class.java).first().also {
                        NotificationRepository.createNotification(
                            notificationInsert =  NotificationInsert(
                                userId = messageInsert.recipientUserId,
                                type = NotificationType.MESSAGE
                            ),
                            handle = handle
                        )
                }
        } catch (e: Exception) {
            logger.error { "An error occurred saving the message ${e.message}" }
            throw Exception("An error has occurred saving the message")
        }
    }

    fun getMessagesByEnquiryId(page: Int, size: Int, enquiryId: Int, userId: Int): List<MessageResource> {
        return SharedJdbi.jdbi().inTransaction<List<MessageResource>, Exception> { handle ->
            val messages = handle.createQuery(
                    """SELECT
                                t.id as message_id,
                                   sender_id,
                                   recipient_id,
                                   enquiry_id,
                                   t.date_created,
                                   message_content,
                                   student_user_id,
                                   tutor_user_id
                            FROM bitmentor.message as t
                            JOIN bitmentor.enquiry ua ON ua.id = enquiry_id
                            WHERE enquiry_id = :enquiry_id
                            ORDER BY t.date_created DESC LIMIT $size OFFSET ${page * size};"""
            )
                    .bind("enquiry_id", enquiryId)
                    .mapTo(MessageDao::class.java)
                    .list()

            messages.firstOrNull { it.senderId != userId && it.recipientId != userId }
                ?.let {
                    logger.warn { "User is not a sender or recipient of a message in the enquiry" }
                    throw UserAuthenticationException("User is not authorised for this enquiry")
                }

            mapUserData(messages = messages, handle = handle)
        }
    }

    fun countMessagesByEnquiryId(enquiryId: Int): Int {
        return SharedJdbi.jdbi().inTransaction<Int, Exception> { handle ->
            handle.createQuery(
                    """SELECT COUNT(*)
                            FROM bitmentor.message
                            WHERE enquiry_id = :enquiry_id;
                        """
            )
                    .bind("enquiry_id", enquiryId)
                    .mapTo(Int::class.java)
                    .first()
        }
    }

    private fun mapUserData(messages: List<MessageDao>,
                            handle: Handle): List<MessageResource> {
        return messages.map {message ->
            val recipientUser = UserRepository.getUser(message.recipientId, handle)
                ?: throw UserNotFoundException()
            val senderUser = UserRepository.getUser(message.senderId, handle)
                ?: throw UserNotFoundException()

            MessageResource(
                id = message.id,
                senderUserId = message.senderId,
                recipientUserId = message.recipientId,
                senderDisplayName = senderUser.displayName ?: throw UserAccountIncompleteException(),
                recipientDisplayName = recipientUser.displayName ?: throw UserAccountIncompleteException(),
                senderProfileUrl = senderUser.profileImageUrl,
                enquiryId = message.enquiryId,
                dateCreated = message.dateCreated,
                messageContent = message.messageContent,
                tutorUserId = message.tutorUserId,
                studentUserId = message.studentUserId,
                tutorId = message.enquiryId,
                tutorDisplayName = senderUser.displayName.takeIf { senderUser.id == message.tutorUserId } ?: recipientUser.displayName,
                studentDisplayName = senderUser.displayName.takeIf { senderUser.id != message.tutorUserId } ?: recipientUser.displayName
            )
        }
    }
}
