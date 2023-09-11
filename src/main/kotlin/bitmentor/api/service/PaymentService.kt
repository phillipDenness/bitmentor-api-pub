package bitmentor.api.service

import bitmentor.api.config.SharedJdbi
import bitmentor.api.email.EmailTemplate
import bitmentor.api.exceptions.LessonNotFoundException
import bitmentor.api.exceptions.UserNotFoundException
import bitmentor.api.model.lesson.LessonResource
import bitmentor.api.model.lesson.LessonStates
import bitmentor.api.model.order.OrderResource
import bitmentor.api.model.payment.PaymentInsert
import bitmentor.api.model.payout.PayoutStatus
import bitmentor.api.repository.PaymentRepository
import bitmentor.api.repository.PayoutRepository
import bitmentor.api.repository.UserRepository
import bitmentor.api.util.formatPenceToPoundCurrency
import io.ktor.features.*
import io.ktor.util.*
import mu.KotlinLogging
import java.math.RoundingMode
import java.text.DecimalFormat
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*

private val logger = KotlinLogging.logger {}

@KtorExperimentalAPI
fun processFreeOrder(lessonId: Int, userId: Int, code: String?) {
    val lesson = getLessonsById(lessonId) ?: throw LessonNotFoundException()

    if (lesson.isLessonValid(userId)) {
        val df = DecimalFormat("#.##")
        df.roundingMode = RoundingMode.CEILING
        val amount = code?.let { (applyPromo(code = code, lessonResource = lesson, userId = userId)) }
            ?: lesson.cost

        if (amount <= 0.0) {
            val externalId = UUID.randomUUID().toString()
            val orderResource = PaymentRepository.create(
                paymentInsert = PaymentInsert(
                    lessonId = lesson.id,
                    externalId = externalId,
                    paymentId = null,
                    amount = 0,
                    processingFee = 0,
                    tutorUserId = lesson.tutorUserId,
                    userId = lesson.studentId
                ),
                userId = userId
            )
            sendOrderConfirmationEmail(userId = userId, orderResource = orderResource, last4 = null, cardType = null)
        } else {
            throw BadRequestException("Order is not free")
        }
    } else {
        logger.warn {
            "Must be a student ${lesson.studentId} " +
                    "but user is $userId to pay for the lesson ${lesson.id} " +
                    "and state must be ${LessonStates.CONFIRMED} " +
                    "but it was ${lesson.lessonStates.first().status}"
        }
        throw BadRequestException("Payment not taken. Lesson is not valid")
    }
}


@KtorExperimentalAPI
fun OrderResource.disputeOrder() {
    logger.info { "Created dispute for order: $id" }
    SharedJdbi.jdbi().inTransaction<Unit, Exception> { handle ->
        PayoutRepository.createPayoutStatus(
            status = PayoutStatus.DISPUTED,
            payoutId = id,
            handle = handle
        )
    }
}

fun LessonResource.isLessonValid(userId: Int): Boolean {

    val userIsStudent = (studentId == userId).also { if (!it) {
        logger.info { "Payment check failed because student id $studentId does not match user $userId" } }
    }
    val lessonHasNeverBeenConfirmed = (!lessonStates.any { it.status == LessonStates.CONFIRMED}).also { if (!it) {
        logger.info { "Payment check failed because lesson ${this.id} has been confirmed" } }
    }
    val lessonIsInFuture = lessonDate.isAfter(ZonedDateTime.now()).also { if (!it) {
        logger.info { "Payment check failed because lesson start date is in the past" } }
    }
    val lessonIsCurrentlyPending = (lessonStates.first().status == LessonStates.PENDING || lessonStates.first().status == LessonStates.RESCHEDULED).also { if (!it) {
        logger.info { "Payment check failed lesson is currently ${lessonStates.first().status}" } }
    }

    return userIsStudent &&
            lessonHasNeverBeenConfirmed &&
            lessonIsInFuture &&
            lessonIsCurrentlyPending
}

fun sendOrderConfirmationEmail(userId: Int, orderResource: OrderResource, last4: String?, cardType: String?) {
    val user = UserRepository.getUser(userId) ?: throw UserNotFoundException()
            .also { logger.error { "Could not find $userId user when sending payment confirmation email" } }

    EmailTemplate(
            message = """
    <h1>Thanks for your order</h1>
        <p>Order details:</p>
        <ul>
            <li>Order Date: ${DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").format(orderResource.dateCreated)}</li>
            <li>Order Id: ${orderResource.externalId}</li>
            <li>Credit Card Type: ${cardType ?: "n/a"}</li>
            <li>Credit Card Number: xxxx-${last4 ?: "n/a"}</li>
            <li>Tutor fee: £${((orderResource.amount - orderResource.processingFee)).formatPenceToPoundCurrency()}</li>
            <li>Processing fee: £${orderResource.processingFee.formatPenceToPoundCurrency()}</li>
            <li>Grand Total: £${orderResource.amount.formatPenceToPoundCurrency()}</li>
            <li>Topic: ${orderResource.lesson.topic.name}</li>
            <li>Tutor: ${orderResource.lesson.tutorDisplayName}</li>
            <li>Student: ${orderResource.lesson.studentDisplayName}</li>
            <li>Start Time: ${
                DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").format(orderResource.lesson.lessonDate)
            }</li>
            <li>End Time: ${
                DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").format(orderResource.lesson.lessonEndDate)
            }</li>
        </ul>
    """,
            subject = "Order confirmation #${orderResource.externalId}"
    ).send(user.email)
}

fun sendOrderRefundEmail(userId: Int, orderResource: OrderResource) {
    val user = UserRepository.getUser(userId) ?: throw UserNotFoundException()
            .also { logger.error { "Could not find $userId user when sending payment refund email" } }

    EmailTemplate(
            message = """
    <h1>We have refunded you.</h1>
        <p>Order details:</p>
        <ul>
            <li>Order Date: ${DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").format(orderResource.dateCreated)}</li>
            <li>Order Id: ${orderResource.externalId}</li>
            <li>Refund Total: £${orderResource.amount.formatPenceToPoundCurrency()}</li>
            <li>Topic: ${orderResource.lesson.topic.name}</li>
            <li>Tutor: ${orderResource.lesson.tutorDisplayName}</li>
            <li>Student: ${orderResource.lesson.studentDisplayName}</li>
        </ul>
    """,
            subject = "Order Refund #${orderResource.externalId}"
    ).send(user.email)
}