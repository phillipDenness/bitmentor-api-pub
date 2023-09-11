package bitmentor.api.service

import bitmentor.api.config.Properties
import bitmentor.api.email.EmailTemplate
import bitmentor.api.exceptions.EmailSendException
import bitmentor.api.model.tutor.VerificationState
import bitmentor.api.repository.AdminRepository
import bitmentor.api.repository.TutorRepository
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
fun Int.isAdmin(): Boolean {
    return AdminRepository.isAdmin(this)
}

@KtorExperimentalAPI
fun approveTutorId(tutorId: Int) {
    TutorRepository.updateIdState(
            tutorState = VerificationState.VERIFIED,
            tutorId = tutorId
    ).also {
        getTutor(tutorId)?.let {
            sendIDVerifiedEmail(it.tutorUserId)
        }
    }
}

@KtorExperimentalAPI
fun approveTutorDsb(tutorId: Int) {
    TutorRepository.updateDbsState(
        tutorState = VerificationState.VERIFIED,
        tutorId = tutorId
    ).also {
        getTutor(tutorId)?.let {
            sendDbsVerifiedEmail(it.tutorUserId)
        }
    }
}

private fun sendIDVerifiedEmail(recipientId: Int) {
    UserRepository.getUser(recipientId)?.let { userAccount ->
        EmailTemplate(
                message = """
    <p>Hi ${userAccount.firstName},</p>
    <p>We have verified your account.</p>
    <p>To publish your tutor profile please go to <a href="${Properties.clientUrl}/portal">tutor profile</a>.</p>
    """,
                subject = "Identity verified"
        ).send(userAccount.email)
    }
}

private fun sendDbsVerifiedEmail(recipientId: Int) {
    UserRepository.getUser(recipientId)?.let { userAccount ->
        EmailTemplate(
                message = """
    <p>Hi ${userAccount.firstName},</p>
    <p>We have verified your DBS/CRB.</p>
    <p>You're tutor profile may now appear more prominently in searches.</p>
    """,
                subject = "DBS/CRB approved"
        ).send(userAccount.email)
    }
}