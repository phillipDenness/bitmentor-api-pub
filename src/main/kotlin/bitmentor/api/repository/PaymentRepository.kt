package bitmentor.api.repository

import bitmentor.api.config.SharedJdbi
import bitmentor.api.model.lesson.LessonStates
import bitmentor.api.model.lesson.LessonStatusInsert
import bitmentor.api.model.order.OrderResource
import bitmentor.api.model.payment.PaymentInsert
import bitmentor.api.model.payment.PaymentStatus
import bitmentor.api.model.payment.PaymentStatusInsert
import bitmentor.api.repository.entity.OrderDao
import bitmentor.api.repository.entity.PaymentStatusDao
import bitmentor.api.service.toResource
import io.ktor.util.*
import mu.KotlinLogging
import org.jdbi.v3.core.Handle


@KtorExperimentalAPI
object PaymentRepository {
    private val logger = KotlinLogging.logger {}

    private const val PAYMENT_DAO_QUERY: String = """
                        SELECT
                            id,
                            lesson_id, 
                            user_id,
                            tutor_user_id,
                            external_id,
                            payment_id,
                            amount,
                            processing_fee,
                            refund_id,
                            date_updated,
                            date_created
                        FROM
                            bitmentor.payment"""

    fun create(
            paymentInsert: PaymentInsert,
            userId: Int
    ): OrderResource {
        return SharedJdbi.jdbi().inTransaction<OrderResource, Exception> { handle ->
            try {
                val id = handle.createUpdate(
                        """
                            INSERT INTO bitmentor.payment(
                                    lesson_id, 
                                    user_id,
                                    tutor_user_id,
                                    external_id,
                                    payment_id,
                                    amount,
                                    processing_fee,
                                    date_updated,
                                    date_created
                            )VALUES(
                                    :lesson_id, 
                                    :user_id,
                                    :tutor_user_id,
                                    :external_id, 
                                    :payment_id,
                                    :amount, 
                                    :processing_fee,
                                    now(),
                                    now())""")
                        .bind("user_id", paymentInsert.userId)
                        .bind("tutor_user_id", paymentInsert.tutorUserId)
                        .bind("lesson_id", paymentInsert.lessonId)
                        .bind("external_id", paymentInsert.externalId)
                        .bind("payment_id", paymentInsert.paymentId)
                        .bind("amount", paymentInsert.amount)
                        .bind("processing_fee", paymentInsert.processingFee)
                        .executeAndReturnGeneratedKeys("id")
                        .mapTo(Int::class.java)
                        .first()

                createPaymentStatus(status = PaymentStatusInsert(status = PaymentStatus.CONFIRMED, paymentId = id), handle = handle)
                if (paymentInsert.paymentId != null) {
                    PayoutRepository.create(paymentId = id, handle = handle)
                }
                LessonRepository.createLessonStatus(
                        lessonStatusInsert = LessonStatusInsert(LessonStates.CONFIRMED),
                        userId = userId,
                        lessonId = paymentInsert.lessonId,
                        handle = handle
                )


                getById(id) ?: throw Exception("Could not find created payment $id")
            } catch (e: Exception) {
                logger.error { "An error has occurred saving the payment ${e}" }
                throw Exception("An error has occurred saving the payment")
            }
        }
    }

    fun updateRefundId(orderId: Int, refundId: String, handle: Handle) {
        handle.createUpdate("""
            UPDATE bitmentor.payment
            SET refund_id = :refund_id,
                date_updated = now()
            WHERE id = :id
        """)
                .bind("id", orderId)
                .bind("refund_id", refundId)
                .execute()
    }

    fun countByUserId(userId: Int): Int {
        return SharedJdbi.jdbi().inTransaction<Int, Exception> { handle ->
            handle.createQuery(
                    """SELECT COUNT(*)
                            FROM bitmentor.payment
                            WHERE user_id = :user_id;
                        """
            )
                .bind("user_id", userId)
                .mapTo(Int::class.java)
                .first()
        }
    }

    fun getByUserId(userId: Int, page: Int, size: Int): List<OrderResource> {
        return SharedJdbi.jdbi().inTransaction<List<OrderResource>, Exception> { handle ->
            handle.createQuery(
                    """$PAYMENT_DAO_QUERY
                            WHERE user_id = :user_id
                            ORDER BY date_created DESC LIMIT $size OFFSET $page;"""
            )
                    .bind("user_id", userId)
                    .mapTo(OrderDao::class.java)
                    .list().map { it.toResource(handle) }
        }
    }

    private fun getById(id: Int): OrderResource? {
        return SharedJdbi.jdbi().inTransaction<OrderResource?, Exception> { handle ->
            handle.createQuery(
                    """$PAYMENT_DAO_QUERY
                            WHERE id = :id"""
            )
                    .bind("id", id)
                    .mapTo(OrderDao::class.java)
                    .firstOrNull()?.toResource(handle)

        }
    }

    fun getByLessonId(lessonId: Int): OrderResource? {
        return SharedJdbi.jdbi().inTransaction<OrderResource?, Exception> { handle ->
            handle.createQuery(
                    """$PAYMENT_DAO_QUERY
                            WHERE lesson_id = :lesson_id"""
            )
                    .bind("lesson_id", lessonId)
                    .mapTo(OrderDao::class.java)
                    .firstOrNull()
                    ?.toResource(handle)
        }
    }

    fun createPaymentStatus(status: PaymentStatusInsert) {
        SharedJdbi.jdbi().inTransaction<Unit, Exception> { handle ->
            createPaymentStatus(status, handle)
        }
    }

    fun createPaymentStatus(status: PaymentStatusInsert, handle: Handle) {
        handle.createUpdate("""
            INSERT INTO bitmentor.payment_status(
                payment_id,
                status,
                date_created
            )VALUES(
                :payment_id,
                :status,
                now()
            )
        """)
                .bind("payment_id", status.paymentId)
                .bind("status", status.status)
                .execute()
    }

    fun getStatus(paymentId: Int, handle: Handle): List<PaymentStatusDao> {
        return handle.createQuery(
                """SELECT * FROM bitmentor.payment_status 
                    WHERE payment_id = :payment_id
                    ORDER BY date_created DESC"""
        )
                .bind("payment_id", paymentId)
                .mapTo(PaymentStatusDao::class.java)
                .list()
    }
}
