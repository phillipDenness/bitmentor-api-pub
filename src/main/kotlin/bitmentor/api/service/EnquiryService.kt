package bitmentor.api.service

import bitmentor.api.config.Properties
import bitmentor.api.email.EmailTemplate
import bitmentor.api.exceptions.EmailSendException
import bitmentor.api.exceptions.UserNotFoundException
import bitmentor.api.model.enquiry.EnquiryInsert
import bitmentor.api.model.enquiry.EnquiryPagedResource
import bitmentor.api.repository.EnquiryRepository
import bitmentor.api.repository.UserRepository
import com.sendgrid.Method
import com.sendgrid.Request
import com.sendgrid.Response
import com.sendgrid.SendGrid
import com.sendgrid.helpers.mail.Mail
import com.sendgrid.helpers.mail.objects.Content
import com.sendgrid.helpers.mail.objects.Email
import io.ktor.util.*
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

@KtorExperimentalAPI
fun EnquiryInsert.create(senderId: Int): Int {
    return EnquiryRepository.create(
        tutorUserId = tutorUserId,
        studentUserId = senderId,
        tutorId = tutorId,
        topicId = topicId,
        messageInsert = message,
        senderId = senderId
    ).also {
        sendMessageEmail(this.tutorUserId, senderId, it)
    }
}

@KtorExperimentalAPI
fun getEnquiriesByUserId(page: Int, size: Int, userId: Int, isTutor: Boolean): EnquiryPagedResource {
    val total = EnquiryRepository.countByUserId(userId, isTutor)
    val messages = EnquiryRepository.getPagedByUserId(
            page = page,
            size = size,
            userId = userId,
            isTutor = isTutor
    )
    return EnquiryPagedResource(
        enquiries = messages,
        page = page,
        size = size,
        total = total
    )
}

@KtorExperimentalAPI
fun getEnquiryById(enquiryId: Int, userId: Int): EnquiryPagedResource {
    val enquiries = EnquiryRepository.getById(enquiryId, userId)?.let { listOf(it) } ?: emptyList()
    return EnquiryPagedResource(
            enquiries = enquiries,
            page = 0,
            size = 1,
            total = enquiries.size
    )
}

private fun sendMessageEmail(recipientId: Int, senderId: Int, enquiryId: Int) {
    val senderDetails = UserRepository.getUser(senderId) ?: throw UserNotFoundException()
            .also { logger.error { "Could not find user id $senderId when sending message alert email" } }

    UserRepository.getUser(recipientId)?.let { userAccount ->
        EmailTemplate(
                message = """
    <p>Hi ${userAccount.firstName},</p>
    <p>We are contacting you because ${senderDetails.displayName} you have a new enquiry.</p>
    <p>To retrieve the message please go to <a href="${Properties.clientUrl}/messages?enquiryId=$enquiryId">My Messages</a></p>
    """,
                subject = "New Enquiry from ${senderDetails.displayName}"
        ).send(userAccount.email)
    }
}
