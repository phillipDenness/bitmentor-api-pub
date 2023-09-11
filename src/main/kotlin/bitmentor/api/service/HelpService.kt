package bitmentor.api.service


import bitmentor.api.exceptions.InvalidLessonStatusChange
import bitmentor.api.model.help.HelpInsert
import bitmentor.api.model.help.HelpTelegram
import bitmentor.api.model.help.RefundInsert
import bitmentor.api.model.help.SupportInsert
import bitmentor.api.model.lesson.LessonStates
import bitmentor.api.model.lesson.LessonStatusInsert
import bitmentor.api.model.payment.PaymentStatusInsert
import bitmentor.api.model.payment.PaymentStatus
import bitmentor.api.repository.PaymentRepository
import io.ktor.features.*
import io.ktor.util.*
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

suspend fun HelpInsert.create(userId: Int) {
    getUser(userId).let { user ->
        logger.info { "Fetching user details for user $userId" }
        val payload = HelpTelegram(
                message = message,
                category = category,
                userId = userId,
                email = user.email,
                displayName = user.displayName ?: ""
        )

        sendBotMessage(payload.format())
    }
    // TODO Send courtesy email
}


@KtorExperimentalAPI
suspend fun SupportInsert.create(userId: Int) {
    logger.info { "Support ticket $this for user $userId" }
    val lesson = getLessonsById(lessonId) ?: throw BadRequestException("Lesson not found for support ticket")
    if (lesson.tutorUserId != userId && lesson.studentId != userId) {
        logger.info { "User not associated with lesson and cannot make support ticket" }
        throw BadRequestException("")
    }

    if (lesson.studentId == userId) {
        val orderId = getOrderByLessonId(lessonId)?.let { order ->
            logger.info { "User is the student. Attempt to automatically dispute and block payout" }
            order.disputeOrder()
            order.id
        }
        getUser(userId).let { user ->
            logger.info { "Fetching user details for user $userId" }
            val payload = HelpTelegram(
                    message = message,
                    category = "STUDENT_SUPPORT_LESSON",
                    userId = userId,
                    email = user.email,
                    displayName = user.displayName ?: "",
                    lessonId = lessonId,
                    isOrderIdDisputed = orderId
            )
            sendBotMessage(payload.format())
        }
    } else {
        getUser(userId).let { user ->
            logger.info { "Fetching user details for user $userId" }
            val payload = HelpTelegram(
                    message = message,
                    category = "TUTOR_SUPPORT_LESSON",
                    userId = userId,
                    email = user.email,
                    displayName = user.displayName ?: "",
                    lessonId = lessonId
            )
            sendBotMessage(payload.format())
        }
    }
    // TODO Send courtesy email
}

@KtorExperimentalAPI
suspend fun RefundInsert.create(userId: Int) {
    getOrderByLessonId(lessonId)?.let { order ->
        if (order.userId != userId) {
            logger.warn { "Order user id does not match requesting user" }
            throw BadRequestException("You can only request a refund for payments you made")
        }
        try {
            LessonStatusInsert(
                    status = LessonStates.REJECTED
            ).create(userId, lessonId)
            logger.info { "Lesson updated to state: rejected. Refund complete" }
        } catch (e: InvalidLessonStatusChange) {
            logger.warn { "Attempted automatic cancellation but failed with $e" }
            order.disputeOrder()
            getUser(userId).let { user ->
                val payload = HelpTelegram(
                        message = message,
                        category = PaymentStatus.REFUND_REQUESTED.toString(),
                        userId = userId,
                        email = user.email,
                        displayName = user.displayName ?: ""
                )
                sendBotMessage(payload.format())
            }
        }

    } ?: throw BadRequestException("No order found for lessonId: $lessonId. Please check lesson and try again.")
    // TODO Send courtesy email
}

fun HelpTelegram.format(): String {
    return """
        <b>Category: $category</b>
        <i>Email: $email</i>
        <i>Display Name: $displayName</i>
        <i>UserId: $userId</i>
        <pre>message: $message</pre>
        <pre>lessonId: $lessonId</pre>
    """
}
