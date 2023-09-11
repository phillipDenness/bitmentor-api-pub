package bitmentor.api.repository

import bitmentor.api.config.SharedJdbi
import bitmentor.api.model.payout.PayoutResource
import bitmentor.api.model.payout.PayoutStatus
import bitmentor.api.model.payout.PayoutStatusResource
import bitmentor.api.repository.entity.PayoutDao
import bitmentor.api.repository.entity.PayoutSummaryDao
import io.ktor.util.*
import mu.KotlinLogging
import org.jdbi.v3.core.Handle


@KtorExperimentalAPI
object PayoutRepository {
    private val logger = KotlinLogging.logger {}


    private const val PAYOUT_QUERY_DAO = """
        SELECT
            out.id,
            pay.tutor_user_id,
            pay.amount,
            pay.processing_fee,
            pay.lesson_id,
            out.payment_id,
            out.date_created
        FROM
            bitmentor.payout out
        INNER JOIN bitmentor.payment pay ON out.payment_id = pay.id
    """

    fun getCountByUserId(userId: Int): Int {
        return SharedJdbi.jdbi().inTransaction<Int, Exception> { handle ->
            handle.createQuery("""
                SELECT
                    COUNT(*)
                FROM
                    bitmentor.payout out
                INNER JOIN bitmentor.payment pay ON out.payment_id = pay.id
                INNER JOIN bitmentor.lesson l ON pay.lesson_id = l.id
                 WHERE out.id IN (
                    SELECT payout_id FROM (
                          SELECT payout_id, status, date_created,
                                 RANK() OVER (PARTITION BY payout_id ORDER BY date_created DESC) latest_status
                          FROM bitmentor.payout_state
                      ) as sub WHERE latest_status = 1
                    AND status = 'PENDING' OR status = 'AVAILABLE') AND pay.tutor_user_id = :user_id
            """)
                    .bind("user_id", userId)
                    .mapTo(Int::class.java)
                    .first()
        }
    }

    fun getByUserId(size: Int, page: Int, userId: Int): List<PayoutResource> {
        return SharedJdbi.jdbi().inTransaction<List<PayoutResource>, Exception> { handle ->
            handle.createQuery("""
                $PAYOUT_QUERY_DAO
                INNER JOIN bitmentor.lesson l ON pay.lesson_id = l.id
                WHERE out.id IN (
                    SELECT payout_id FROM (
                          SELECT payout_id, status, date_created,
                                 RANK() OVER (PARTITION BY payout_id ORDER BY date_created DESC) latest_status
                          FROM bitmentor.payout_state
                      ) as sub WHERE latest_status = 1 
                        AND status = 'PENDING' OR status = 'AVAILABLE' OR status = 'DISPUTED' OR status = 'COMPLETE'
                    ) 
                AND pay.tutor_user_id = :user_id
                ORDER BY date_created DESC LIMIT $size OFFSET $page;
            """)
                    .bind("user_id", userId)
                    .mapTo(PayoutDao::class.java)
                    .list()
                    .map { it.toResource(handle) }
        }
    }

    fun getAllAvailablePayoutsByUserId(userId: Int, handle: Handle): List<PayoutDao> {
        return handle.createQuery("""
            $PAYOUT_QUERY_DAO
            WHERE out.id IN (
                    SELECT payout_id FROM (
                          SELECT payout_id, status, date_created,
                                 RANK() OVER (PARTITION BY payout_id ORDER BY date_created DESC) latest_status
                          FROM bitmentor.payout_state
                      ) as sub WHERE latest_status = 1 AND status = 'AVAILABLE')
            AND pay.tutor_user_id = :user_id 
            ORDER BY date_created DESC
        """)
                .bind("user_id", userId)
                .mapTo(PayoutDao::class.java)
                .list()
    }

    fun PayoutDao.toResource(handle: Handle): PayoutResource {
        val statuses = handle.createQuery("""
            SELECT status, date_created, payout_id
            FROM bitmentor.payout_state
            WHERE payout_id = :payout_id
            ORDER BY date_created DESC
        """)
                .bind("payout_id", id)
                .mapTo(PayoutStatusResource::class.java)
                .list()

        return PayoutResource(
                id = id,
                tutorUserId = tutorUserId,
                amount = amount,
                processingFee = processingFee,
                lesson = LessonRepository.getLessonById(lessonId, handle)
                        ?: throw Exception("Could not find lesson $lessonId"),
                dateCreated = dateCreated,
                paymentId = paymentId,
                statuses = statuses
        )
    }

    fun create(
            paymentId: Int,
            handle: Handle
    ): Int {
        try {
            val id = handle.createUpdate(
                    """
                        INSERT INTO bitmentor.payout(
                                payment_id,
                                date_created
                        )VALUES(
                                :payment_id,
                                now()
                        )"""
            )
                    .bind("payment_id", paymentId)
                    .executeAndReturnGeneratedKeys("id")
                    .mapTo(Int::class.java)
                    .first()

            createPayoutStatus(
                    status = PayoutStatus.PENDING,
                    payoutId = id,
                    handle = handle
            )
            return id
        } catch (e: Exception) {
            logger.error { "An error has occurred saving the payout ${e.message}" }
            throw Exception("An error has occurred saving the payout")
        }
    }

    fun getPayoutByOrderId(paymentId: Int, handle: Handle): PayoutResource? {
        return handle.createQuery("""
                $PAYOUT_QUERY_DAO
                WHERE pay.id = :payment_id
            """)
                .bind("payment_id", paymentId)
                .mapTo(PayoutDao::class.java)
                .firstOrNull()?.toResource(handle)
    }

    fun createPayoutStatus(status: PayoutStatus, payoutId: Int, handle: Handle) {
        try {
            handle.createUpdate(
                    """
                    INSERT INTO bitmentor.payout_state(
                        status,
                        payout_id,
                        date_created
                    )VALUES(
                        :status,
                        :payout_id,
                        now()
                    )"""
            )
                    .bind("status", status)
                    .bind("payout_id", payoutId)
                    .execute()
        } catch (e: Exception) {
            logger.error { "Exception saving payout status ${e.message}" }
            throw e
        }
    }

    fun getAmountAndFees(status: PayoutStatus, userId: Int, handle: Handle): PayoutSummaryDao? {
        return handle.createQuery("""
                SELECT sum(amount) as gross, sum(processing_fee) as fees FROM bitmentor.payout out
                JOIN bitmentor.payment p on p.id = out.payment_id
                JOIN bitmentor.lesson l on l.id = p.lesson_id
                WHERE out.id IN (
                SELECT payout_id FROM (
                      SELECT payout_id, status, date_created,
                             RANK() OVER (PARTITION BY payout_id ORDER BY date_created DESC) latest_status
                      FROM bitmentor.payout_state
                  ) as sub WHERE latest_status = 1
                AND status = :status) AND p.tutor_user_id = :user_id
        """)
                    .bind("status", status)
                    .bind("user_id", userId)
                    .mapTo(PayoutSummaryDao::class.java)
                    .firstOrNull()
    }

    fun getAmountAndFees(status: PayoutStatus, userId: Int): PayoutSummaryDao? {
        return SharedJdbi.jdbi().inTransaction<PayoutSummaryDao?, Exception> { handle ->
            getAmountAndFees(status, userId, handle)
        }
    }

    fun get(payoutId: Int, handle: Handle): PayoutResource? {
        return handle.createQuery("""
                $PAYOUT_QUERY_DAO
                WHERE out.id = :id
            """)
            .bind("payout_id", payoutId)
            .mapTo(PayoutDao::class.java)
            .firstOrNull()?.toResource(handle)
    }
}
