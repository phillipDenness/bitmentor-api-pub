package bitmentor.api.service

import bitmentor.api.config.Properties
import bitmentor.api.config.SharedJdbi
import bitmentor.api.email.EmailTemplate
import bitmentor.api.exceptions.UserNotFoundException
import bitmentor.api.model.message.MessageInsert
import bitmentor.api.model.message.MessagePagedResource
import bitmentor.api.repository.MessageRepository
import bitmentor.api.repository.UserRepository
import io.ktor.util.*
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

@KtorExperimentalAPI
fun MessageInsert.create(senderId: Int): Int {
    return SharedJdbi.jdbi().inTransaction<Int, Exception> { handle ->
        MessageRepository.createMessage(this, senderId, handle)
            .also { sendMessageEmail(this.recipientUserId, senderId) }
    }
}

fun getMessagesByEnquiryId(page: Int, size: Int, enquiryId: Int, userId: Int): MessagePagedResource {
    val messages = MessageRepository.getMessagesByEnquiryId(page = page, size = size, enquiryId = enquiryId, userId = userId)

    val totalMessages = MessageRepository.countMessagesByEnquiryId(enquiryId)
    return MessagePagedResource(
        messages = messages,
        totalMessages = totalMessages,
        size = size,
        page = page
    )
}

private fun sendMessageEmail(recipientId: Int, senderId: Int) {
    val senderDetails = UserRepository.getUser(senderId) ?: throw UserNotFoundException()
            .also { logger.error { "Could not find user id $senderId when sending message alert email" } }

    UserRepository.getUser(recipientId)?.let { userAccount ->
        EmailTemplate(
                message = """
    <p>Hi ${userAccount.firstName},</p>
    <b>We are contacting you because ${senderDetails.displayName} has messaged you.</b>
    <p>To retrieve the message please go to <a href="${Properties.clientUrl}/messages">My Messages</a></p>
    """,
                subject = "${senderDetails.displayName} has contacted you"
        ).send(userAccount.email)
    }
}
