package bitmentor.api.service

import bitmentor.api.config.Properties
import bitmentor.api.config.SharedJdbi
import bitmentor.api.email.EmailTemplate
import bitmentor.api.model.lesson.LessonResource
import bitmentor.api.model.payment.PaymentStatus
import bitmentor.api.model.payout.PayoutPagedResource
import bitmentor.api.model.payout.PayoutResource
import bitmentor.api.model.payout.PayoutStatus
import bitmentor.api.model.payout.PayoutSummary
import bitmentor.api.repository.PayoutRepository
import bitmentor.api.repository.PaypalPaymentRepository
import bitmentor.api.repository.TutorDetailRepository
import bitmentor.api.repository.UserRepository
import bitmentor.api.repository.entity.PayoutSummaryDao
import io.ktor.features.*
import io.ktor.util.*
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging


private val logger = KotlinLogging.logger {}

@KtorExperimentalAPI
fun markPayoutsComplete(payoutIds: List<String>) {
    SharedJdbi.jdbi().inTransaction<Unit, Exception> { handle ->
        try {
            payoutIds.forEach { payoutId ->
                PayoutRepository.createPayoutStatus(
                    status = PayoutStatus.COMPLETE,
                    payoutId = payoutId.toInt(),
                    handle = handle
                )
            }

            PayoutRepository.get(payoutIds.first().toInt(), handle)?.let { sendPayoutCompletedEmail(it) }

        } catch (e: Exception) {
            logger.error { "error while saving payout status: $e" }
            throw BadRequestException("Error while saving payout status")
        }
    }
}

@KtorExperimentalAPI
fun getPayouts(userId: Int, page: Int, size: Int): PayoutPagedResource {
    val total = PayoutRepository.getCountByUserId(userId)

    val payouts = PayoutRepository.getByUserId(
            userId = userId,
            page = page,
            size = size
    )

    return PayoutPagedResource(
            payouts = payouts,
            summary = generatePayoutSummary(userId),
            total = total,
            size = size,
            page = page
    )
}

@KtorExperimentalAPI
fun LessonResource.markPayoutAvailable() {
    getOrderByLessonId(id)?.let { order ->
        if(order.statusHistory.map { it.status }.contains(PaymentStatus.REFUND_REQUESTED)) {
            throw Exception("Refund requested deferring to manual payout")
        }
        val paypalPayment = PaypalPaymentRepository.get(orderId = order.paymentId!!)
            ?: throw Exception("Could not find paypal order for order id: ${order.id}")
        if (paypalPayment.status == "COMPLETED") {
            SharedJdbi.jdbi().inTransaction<Unit, Exception> { handle ->
                val payout = PayoutRepository.getPayoutByOrderId(paymentId = order.id, handle = handle)
                        ?: throw Exception("payout not found for order id ${order.id}")
                if(payout.statuses.first().status == PayoutStatus.DISPUTED) {
                    throw Exception("Payout disputed when attempting to make payout available. Deferring to manual payout")
                } else {
                    logger.info { "Successfully marking payout as available" }
                    PayoutRepository.createPayoutStatus(
                            status = PayoutStatus.AVAILABLE,
                            payoutId = payout.id,
                            handle = handle
                    )
                    sendPayoutAvailableEmail(payout)
                }
            }
        } else {
            throw Exception("Payment not complete: ${order.statusHistory.first().status}")
        }
    } ?: throw Exception("Order not found for lesson id $id")
}

@KtorExperimentalAPI
fun generatePayoutSummary(userId: Int): PayoutSummary {
    val disputed = PayoutRepository.getAmountAndFees(
            status = PayoutStatus.DISPUTED,
            userId = userId
    )
    val pending = PayoutRepository.getAmountAndFees(
            status = PayoutStatus.PENDING,
            userId = userId
    )
    val available = PayoutRepository.getAmountAndFees(
            status = PayoutStatus.AVAILABLE,
            userId = userId
    )

    logger.info { "User $userId pending payout: $pending, available payout: $available" }
    return PayoutSummary(
            availablePayout = available?.calculate() ?: 0,
            pendingPayout = pending?.calculate() ?: 0,
            disputedPayout = disputed?.calculate()
    )
}

fun PayoutSummaryDao.calculate(): Long {
    return gross - fees
}

@KtorExperimentalAPI
fun payoutRequested(userId: Int) {
        SharedJdbi.jdbi().inTransaction<Unit, Exception> { handle ->
            val payouts = PayoutRepository.getAllAvailablePayoutsByUserId(userId = userId, handle = handle)
            payouts.forEach { payout ->
                PayoutRepository.createPayoutStatus(
                        status = PayoutStatus.REQUESTED,
                        payoutId = payout.id,
                        handle = handle
                )
            }

            val payoutSummary = PayoutRepository.getAmountAndFees(
                    status = PayoutStatus.REQUESTED,
                    userId = userId,
                    handle = handle
            ) ?: throw Exception("No payout summary found for requested")

            val details = TutorDetailRepository.getByUserId(userId)
            val user = getUser(userId)

            val payoutInfo = if (details?.paypalPaymentsReceivable == true) {
                details.paypalEmailAddress
            } else {
                user.email
            }
            runBlocking {
                sendBotMessage(
                        """
                            <b>Category: PAYOUT_REQUESTED</b>
                            <i>UserId: $userId</i>
                            <i>Preferred payout option: ${details?.preferredPayoutOption}</>
                            <i>Email: $payoutInfo</i>
                            <i>Tutor Fee: ${payoutSummary.calculate()}p</i>
                            <i>Processing Fee: ${payoutSummary.fees}p</i>
                            <i>PayoutIds: ${payouts.map { it.id }}</i>
                        """
                )
            }
        }
}

fun sendPayoutAvailableEmail(payout: PayoutResource) {
    UserRepository.getUser(payout.tutorUserId)?.let { userAccount ->
        EmailTemplate(
                message = """
        <p>Hi ${userAccount.firstName},</p>
        <b>Great news! Your payment is ready.</b>
        <p>Please visit tutor portal to manage your payouts.</p>
        <p>Visit <a href="${Properties.clientUrl}/portal?view=payout">portal</a></p>
        """,
                subject = "Tutor payout available"
        ).send(userAccount.email)
    }
}


fun sendPayoutCompletedEmail(payout: PayoutResource) {
    UserRepository.getUser(payout.tutorUserId)?.let { userAccount ->
        EmailTemplate(
            message = """
        <p>Hi ${userAccount.firstName},</p>
        <b>Great news! Your payment is completed.</b>
        <p>Please visit tutor portal to manage your payouts.</p>
        <p>Visit <a href="${Properties.clientUrl}/portal?view=payout">portal</a></p>
        """,
            subject = "Tutor payout complete"
        ).send(userAccount.email)
    }
}