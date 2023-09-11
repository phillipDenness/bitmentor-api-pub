package bitmentor.api.email

import bitmentor.api.config.Properties
import bitmentor.api.exceptions.EmailSendException
import com.sendgrid.Method
import com.sendgrid.Request
import com.sendgrid.Response
import com.sendgrid.SendGrid
import com.sendgrid.helpers.mail.Mail
import com.sendgrid.helpers.mail.objects.Content
import com.sendgrid.helpers.mail.objects.Email

private val htmlTemplate = EmailTemplate::class.java.getResource("/email/template.html").readText()

data class EmailTemplate(val message: String,
                         val subject: String) {
    fun send(to: String) {
        val toEmail = Email(to)
        val from = Email(Properties.fromEmailAddress)
        val content = Content("text/html", htmlTemplate.replace("{EMAIL_BODY}", message))
        val mail = Mail(from, subject, toEmail, content)

        val sg = SendGrid(Properties.sendGridApiKey)
        val request = Request()
        request.method = Method.POST
        request.endpoint = "mail/send"
        request.body = mail.build()
        val response: Response = sg.api(request)

        if (response.statusCode != 202) {
            throw EmailSendException(response.body)
        }
    }
}