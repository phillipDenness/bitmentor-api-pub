package bitmentor.api.service

import bitmentor.api.config.GenericObjectMapper
import bitmentor.api.config.Properties
import bitmentor.api.config.paypalClient
import bitmentor.api.exceptions.InvalidLessonStatusChange
import bitmentor.api.exceptions.LessonNotFoundException
import bitmentor.api.model.help.HelpTelegram
import bitmentor.api.model.lesson.LessonStates
import bitmentor.api.model.lesson.LessonStatusInsert
import bitmentor.api.model.payment.PaymentInsert
import bitmentor.api.model.payment.PaymentStatus
import bitmentor.api.model.payment.PaymentStatusInsert
import bitmentor.api.model.payout.PayoutStatus
import bitmentor.api.model.paypal.RefundResponse
import bitmentor.api.model.paypal.payments.*
import bitmentor.api.model.paypal.webhook.PaypalDispute
import bitmentor.api.repository.*
import bitmentor.api.repository.entity.PaypalDao
import com.paypal.core.PayPalEnvironment
import com.paypal.core.PayPalHttpClient
import com.paypal.http.exceptions.HttpException
import io.ktor.client.request.*
import io.ktor.content.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.util.*
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.jdbi.v3.core.Handle
import java.io.IOException
import java.math.RoundingMode
import java.text.DecimalFormat
import kotlin.math.roundToLong


private val logger = KotlinLogging.logger {}

private val environment: PayPalEnvironment = if(Properties.paypalSandbox) {
    PayPalEnvironment.Sandbox(Properties.paypalClientId, Properties.paypalSecret)
} else {
    PayPalEnvironment.Live(Properties.paypalClientId, Properties.paypalSecret)
}

var client = PayPalHttpClient(environment)

fun getAccessCode(): String {
    return environment.authorizationString()
}

@KtorExperimentalAPI
suspend fun processPaypalOrder(lessonId: Int, userId: Int, code: String?): String? {
        val lesson = getLessonsById(lessonId) ?: throw LessonNotFoundException()

        if (lesson.isLessonValid(userId)) {
            val df = DecimalFormat("#.##")
            df.roundingMode = RoundingMode.CEILING
            val amount = code?.let { (applyPromo(code = code, lessonResource = lesson, userId = userId)) }
                ?: lesson.cost

            if (amount > 0) {
                val fee = lesson.cost / 10

                logger.info { "Creating Paypal order and returning string link. Amount $amount, fee $fee" }
                val order = createOrder(
                    amount
                )

                val paypalDao = PaypalDao(
                    orderId = order.id,
                    status = order.status,
                    lessonId = lessonId,
                    grossAmount = df.format(amount)
                )
                logger.info { "Order created, persisting dao $paypalDao" }
                PaypalPaymentRepository.create(paypalDao)
                return order.id
            } else {
                throw BadRequestException("Free orders must be processed using free endpoint")
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
suspend fun createOrder(lessonCost: Double): CreateOrderResponse {
    val df = DecimalFormat("#.##")
    df.roundingMode = RoundingMode.CEILING

//    val orderRequest = OrderRequest()
//    orderRequest.checkoutPaymentIntent("CAPTURE")
//    val purchaseUnits: MutableList<PurchaseUnitRequest> = ArrayList()
//    purchaseUnits.add(PurchaseUnitRequest().amountWithBreakdown(
//        AmountWithBreakdown().currencyCode("GBP").value(df.format(lessonCost)))
//    )
//    orderRequest.purchaseUnits(purchaseUnits)
//    orderRequest.applicationContext(ApplicationContext().shippingPreference("NO_SHIPPING"))
//    val request = OrdersCreateRequest().requestBody(orderRequest)

    try {
        val orderRequest = CreateOrderRequest(
            intent = "CAPTURE",
            purchase_units = listOf(
                PurchaseUnitRequest2(
                    amount = PurchaseUnitAmount(currency_code = "GBP", value = df.format(lessonCost))
                )
            ),
            application_context = PaypalApplicationContext(
                shipping_preference = "NO_SHIPPING"
            )
        )

        val requestBody = GenericObjectMapper.getMapper().writeValueAsString(orderRequest)
        return paypalClient().use { client ->
            client.post<CreateOrderResponse> {
                url("${environment.webUrl()}/v2/checkout/orders")
                header(HttpHeaders.Authorization, getAccessCode())
                body = TextContent(requestBody, ContentType.Application.Json)
            }
        }
    } catch (e: IOException) {
        if (e is HttpException) {
            logger.error { "Paypal order creation failed $e" }
            throw IllegalArgumentException("Paypal order creation failed because ${e.message}. Please try again later.")
        } else {
            logger.error { "Paypal order creation failed $e" }
            throw BadRequestException("Paypal order creation failed because ${e.message}. Please try again later.")
        }
    }
}

@KtorExperimentalAPI
suspend fun handleOrderApproval(orderId: String, userId: Int) {
    val order = PaypalPaymentRepository.get(orderId)
        ?: throw BadRequestException("No order found for order id $orderId")
    val lesson = LessonRepository.getLessonById(order.lessonId)
        ?: throw IllegalArgumentException().also { logger.error { "Error pulling lesson ${order.lessonId}" } }

    if (lesson.studentId != userId) {
        logger.warn { "User $userId attempted to pay for lesson $lesson and they are not the student" }
        throw BadRequestException("Only the student may pay for the lesson.")
    }

    val amountPennies = (order.grossAmount.toDouble() * 100).roundToLong()

    try {
        val orderConfirmation = paypalClient().use { client ->
            client.post<CaptureOrderResponse>{
                url("${environment.webUrl()}/v2/checkout/orders/$orderId/capture")
                header(HttpHeaders.Authorization, getAccessCode())
                body = TextContent("{}", ContentType.Application.Json)
            }
        }

        if (orderConfirmation.status == "COMPLETED") {
            try {
                if (orderConfirmation.purchase_units.isEmpty()) {
                    logger.error { "Order confirmation is missing purchase units $orderConfirmation" }
                }
                logger.info { "Order confirmation received: ${GenericObjectMapper.getMapper().writeValueAsString(orderConfirmation)}" }
                val payment = orderConfirmation.purchase_units.first().payments.captures.first()
                PaypalPaymentRepository.update(
                    status = orderConfirmation.status,
                    netAmount = payment.seller_receivable_breakdown?.net_amount?.value,
                    paypalFee = payment.seller_receivable_breakdown?.paypal_fee?.value,
                    orderId = orderId,
                    captureId = payment.id
                )
                val orderResource = PaymentRepository.create(
                    paymentInsert = PaymentInsert(
                        lessonId = lesson.id,
                        externalId = order.orderId,
                        paymentId = order.orderId,
                        amount = amountPennies,
                        processingFee = (amountPennies * Properties.paymentFeePercent).roundToLong(),
                        tutorUserId = lesson.tutorUserId,
                        userId = lesson.studentId
                    ),
                    userId = userId
                )
                try {
                    sendOrderConfirmationEmail(userId = userId, orderResource = orderResource, last4 = null, cardType = "Paypal")
                } catch (e: Exception) {
                    logger.error { "Attempted to send email confirmation for order ${order.orderId} but failed." }
                }
            } catch (e: Exception) {
                logger.error { "Paypal order captured $orderId. However saving failed due to $e. Sending telegram alert" }
                sendBotMessage("Paypal capture succeeded but saving failed for orderId: $orderId")
            }
        } else {
            logger.error { "Paypal capture status is not COMPLETED but captured status ${orderConfirmation.status}" }
            throw BadRequestException("Paypal capture failed with status ${orderConfirmation.status}. Please try again.")
        }
    } catch (ioe: IOException) {
        if (ioe is HttpException) {
            val he: HttpException = ioe
            logger.error { "Paypal capture failed due to  ${he}. No capture was taken" }
            throw IllegalArgumentException("Paypal payment capture failed due to ${he.message}. Please try again")
        } else {
            logger.error { "Paypal capture failed due to ${ioe}. No capture was taken" }
            throw BadRequestException("Paypal payment capture failed due to ${ioe.message}. Please try again")
        }
    }
}

@KtorExperimentalAPI
fun refundOrder(handle: Handle, lessonId: Int, userId: Int) {
    logger.info { "initiating refund for lesson id $lessonId" }
    val orderResource = PaymentRepository.getByLessonId(lessonId)
        ?: throw Exception("Could not find order for lesson id $lessonId")
            .also { logger.error { "Could not find order for lesson id $lessonId" } }

    // TODO if userId is student then do not return platform fee
    if (orderResource.paymentId != null) {
        val paypalPayment = PaypalPaymentRepository.get(orderResource.paymentId, handle)
            ?: throw Exception("Could not find paypal order ${orderResource.paymentId}")
                .also { logger.error { "Could not find paypal order ${orderResource.paymentId}" } }

        logger.info { "Fetched paypal order $paypalPayment" }
        try {
            runBlocking {
                val response = paypalClient().use { client ->
                    client.post<RefundResponse> {
                        url("${environment.webUrl()}/v2/payments/captures/${paypalPayment.captureId}/refund")
                        header(HttpHeaders.Authorization, getAccessCode())
                        body = TextContent("{}", ContentType.Application.Json)
                    }
                }

                if (response.status == "COMPLETED") {
                    val payout = PayoutRepository.getPayoutByOrderId(paymentId = orderResource.id, handle = handle)
                        ?: throw Exception("Could not find payout for paymentId ${orderResource.id}")

                    PayoutRepository.createPayoutStatus(
                        status = PayoutStatus.CANCELLED,
                        payoutId = payout.id,
                        handle = handle
                    )
                    logger.info { "Created refund payment request ${response.id}" }
                    PaymentRepository.updateRefundId(
                        orderId = orderResource.id,
                        refundId = response.id,
                        handle = handle
                    )
                    PaymentRepository.createPaymentStatus(
                        status = PaymentStatusInsert(
                            status = PaymentStatus.REFUNDED,
                            paymentId = orderResource.id
                        ),
                        handle = handle
                    )

                    PaypalPaymentRepository.updateState(
                        status = response.status,
                        orderId = paypalPayment.orderId
                    )
                } else {
                    throw IllegalArgumentException("Paypal refund response was ${response.status}")
                }

                PaymentRepository.createPaymentStatus(
                    status = PaymentStatusInsert(
                        status = PaymentStatus.REFUNDED,
                        paymentId = orderResource.id
                    ), handle = handle
                )
                sendOrderRefundEmail(userId = orderResource.userId, orderResource = orderResource)
            }
        } catch (ex: Exception) {
            logger.info { "Exception during refund $ex" }
            throw ex
        }
    } else {
        logger.info { "No payment was taken for this lesson" }
    }
}

@KtorExperimentalAPI
fun getPaypalOrder(orderId: String, userId: Int): PaypalDao? {
    PaypalPaymentRepository.get(orderId)?.let {
        val lesson = LessonRepository.getLessonById(it.lessonId)
        if (lesson?.studentId == userId || lesson?.tutorId == userId) {
            return it
        }
    }
    return null
}

@KtorExperimentalAPI
fun createPaypalEmail(email: String, userId: Int) {
    val tutorDetail = TutorDetailRepository.getByUserId(userId = userId)
        ?: throw BadRequestException("User must be a tutor to add paypal email")
    TutorDetailRepository.updatePaypalEmail(email = email, tutorId = tutorDetail.tutorId, isReceivable = true)
}


@KtorExperimentalAPI
fun deletePaypalEmail(userId: Int) {
    val tutorDetail = TutorDetailRepository.getByUserId(userId = userId)
        ?: throw BadRequestException("User must be a tutor to add paypal email")
    TutorDetailRepository.updatePaypalEmail(email = null, tutorId = tutorDetail.tutorId, isReceivable = false)
}

@KtorExperimentalAPI
suspend fun handlePaypalDispute(disputeEvent: PaypalDispute) {
    val paypalOrder =
        PaypalPaymentRepository.getByCaptureId(disputeEvent.resource.disputed_transactions.first().seller_transaction_id)
    if (paypalOrder == null) {
        logger.warn { "Received webhook dispute for order ${disputeEvent.resource.disputed_transactions.first().seller_transaction_id} but could not find it" }
        throw BadRequestException("Unable to find paypal order with ${disputeEvent.resource.disputed_transactions.first().seller_transaction_id}")
    }

    logger.info { "Retrieved paypal event for disputeEvent: $paypalOrder" }
    getOrderByLessonId(paypalOrder.lessonId)?.let { order ->
        try {
            LessonStatusInsert(
                status = LessonStates.REJECTED
            ).create(userId = order.userId, lessonId = order.lesson.id)
            logger.info { "Lesson updated to state: rejected. Refund complete" }
        } catch (e: InvalidLessonStatusChange) {

            PaypalPaymentRepository.updateState(status = "DISPUTED", orderId = paypalOrder.orderId)
            order.disputeOrder()
            getUser(order.userId).let { user ->
                val payload = HelpTelegram(
                    message = "User has created paypal dispute id: ${disputeEvent.id}",
                    category = PaymentStatus.REFUND_REQUESTED.toString(),
                    userId = order.userId,
                    email = user.email,
                    displayName = user.displayName ?: ""
                )
                sendBotMessage(payload.format())
            }
        }
    }?: throw IllegalArgumentException("Could not find order by lesson id ${paypalOrder.lessonId}")
        .also { logger.error {"Could not find order by lesson id ${paypalOrder.lessonId}"} }
}