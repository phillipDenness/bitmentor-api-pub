package bitmentor.api.repository

import bitmentor.api.config.SharedJdbi
import bitmentor.api.repository.entity.PaypalDao
import io.ktor.util.*
import mu.KotlinLogging
import org.jdbi.v3.core.Handle


@KtorExperimentalAPI
object PaypalPaymentRepository {
    private val logger = KotlinLogging.logger {}

    private const val DAO_QUERY: String = """
                        SELECT 
                            order_id,
                            lesson_id,
                            status,
                            gross_amount,
                            net_amount,
                            paypal_fee,
                            capture_id,
                            date_updated
                        FROM 
                            bitmentor.paypal_payment"""

    fun create(paypalDao: PaypalDao) {
        return SharedJdbi.jdbi().inTransaction<Unit, Exception> { handle ->
            handle.createUpdate(
                """INSERT INTO bitmentor.paypal_payment(
                                order_id,
                                lesson_id,
                                status,
                                gross_amount,
                                date_updated
                        )VALUES(
                                :order_id,
                                :lesson_id,
                                :status,
                                :gross_amount,
                                now()
                        )"""
            )
                .bind("order_id", paypalDao.orderId)
                .bind("lesson_id", paypalDao.lessonId)
                .bind("status", paypalDao.status)
                .bind("gross_amount", paypalDao.grossAmount)
                .execute()
        }
    }

    fun update(
        status: String,
        netAmount: String?,
        paypalFee: String?,
        orderId: String,
        captureId: String
    ) {
        return SharedJdbi.jdbi().inTransaction<Unit, Exception> { handle ->
            logger.info { "Updating $orderId with $status" }
            handle.createUpdate(
                """UPDATE bitmentor.paypal_payment
                        SET status = :status,
                        net_amount = :net_amount,
                        paypal_fee = :paypal_fee,
                        capture_id = :capture_id,
                        date_updated = NOW()
                        WHERE order_id = :order_id"""
            )
                .bind("order_id", orderId)
                .bind("status", status)
                .bind("net_amount", netAmount)
                .bind("capture_id", captureId)
                .bind("paypal_fee", paypalFee)
                .execute()
        }
    }

    fun updateState(
        status: String,
        orderId: String
    ) {
        return SharedJdbi.jdbi().inTransaction<Unit, Exception> { handle ->
            logger.info { "Updating $orderId with $status" }
            handle.createUpdate(
                """UPDATE bitmentor.paypal_payment
                        SET status = :status,
                        date_updated = NOW()
                        WHERE order_id = :order_id"""
            )
                .bind("order_id", orderId)
                .bind("status", status)
                .execute()
        }
    }

    fun get(orderId: String): PaypalDao? {
        return SharedJdbi.jdbi().inTransaction<PaypalDao, Exception> { handle ->
            get(orderId, handle)
        }
    }

    fun get(orderId: String, handle: Handle): PaypalDao? {
        return handle.createQuery("""
            $DAO_QUERY WHERE order_id = :order_id
        """)
                .bind("order_id", orderId)
                .mapTo(PaypalDao::class.java)
                .firstOrNull()
    }

    fun getByCaptureId(captureId: String): PaypalDao? {
        return SharedJdbi.jdbi().inTransaction<PaypalDao, Exception> { handle ->
            getByCaptureId(captureId, handle)
        }
    }

    fun getByCaptureId(captureId: String, handle: Handle): PaypalDao? {
        return handle.createQuery("""
            $DAO_QUERY WHERE capture_id = :capture_id
        """)
            .bind("capture_id", captureId)
            .mapTo(PaypalDao::class.java)
            .firstOrNull()
    }
}
