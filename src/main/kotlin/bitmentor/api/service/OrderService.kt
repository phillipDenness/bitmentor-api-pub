package bitmentor.api.service

import bitmentor.api.model.order.OrderPagedResource
import bitmentor.api.model.order.OrderResource
import bitmentor.api.repository.LessonRepository
import bitmentor.api.repository.PaymentRepository
import bitmentor.api.repository.entity.OrderDao
import io.ktor.util.*
import mu.KotlinLogging
import org.jdbi.v3.core.Handle


private val logger = KotlinLogging.logger {}

@KtorExperimentalAPI
fun getOrderByLessonId(lessonId: Int): OrderResource? {
    return PaymentRepository.getByLessonId(lessonId)
}

@KtorExperimentalAPI
fun getOrders(userId: Int, page: Int, size: Int): OrderPagedResource {
    val totalOrders = PaymentRepository.countByUserId(userId)

    val orders = PaymentRepository.getByUserId(userId, page, size)

    return OrderPagedResource(
        orders = orders,
        total = totalOrders,
        size = size,
        page = page
    )
}

//fun OrderResource.getPaymentStatus(): String {
//    if (refundId == null) {
//        if (paymentId == null) {
//            return "COMPLETE"
//        }
//        val paymentsApi = squareClient.paymentsApi
//        return paymentsApi.getPayment(paymentId).payment.status
//    }
//    val refundsApi = squareClient.refundsApi
//    return "REFUND_" + refundsApi.getPaymentRefund(refundId).refund.status
//}

@KtorExperimentalAPI
fun OrderDao.toResource(handle: Handle): OrderResource {
    val lessonResource = LessonRepository.getLessonById(lessonId, handle)
    val statusHistory = PaymentRepository.getStatus(id, handle)
    return OrderResource(
            id = id,
            lesson = lessonResource!!,
            userId = userId,
            tutorUserId = tutorUserId,
            externalId = externalId,
            paymentId = paymentId,
            amount = amount,
            processingFee = processingFee,
            dateUpdated = dateUpdated,
            refundId = refundId,
            dateCreated = dateCreated,
            statusHistory = statusHistory
    )
}
