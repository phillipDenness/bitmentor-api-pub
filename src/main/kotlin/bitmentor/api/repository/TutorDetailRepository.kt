package bitmentor.api.repository

import bitmentor.api.config.SharedJdbi
import bitmentor.api.exceptions.TutorAlreadyRegisteredException
import bitmentor.api.model.tutor.VerificationState
import bitmentor.api.model.tutor_detail.PreferredPayoutOption
import bitmentor.api.model.tutor_detail.UpdateTutorDetailRequest
import bitmentor.api.repository.entity.TutorDetailDao
import io.ktor.util.*
import mu.KotlinLogging
import org.jdbi.v3.core.Handle


@KtorExperimentalAPI
object TutorDetailRepository {
    private val logger = KotlinLogging.logger {}

    private const val TUTOR_DETAIL_DAO_QUERY: String = """
                        SELECT 
                            tutor_id,
                            date_of_birth,
                            phone_number,
                            business_name,
                            payee_uuid,
                            location,
                            paypal_email_address,
                            paypal_payments_receivable,
                            preferred_payout_option,
                            last_modified
                        FROM 
                            bitmentor.tutor_detail td"""

    fun create(tutorId: Int, handle: Handle): Int {
        try {
                return handle.createUpdate(
                        """INSERT INTO bitmentor.tutor_detail(
                                    tutor_id,
                                    last_modified
                            )VALUES(
                                    :tutor_id,
                                    now()
                            )"""
                )
                        .bind("tutor_id", tutorId)
                        .executeAndReturnGeneratedKeys("id").mapTo(Int::class.java).first()
        } catch (e: Exception) {
            logger.error { "Error saving tutor details $e" }
            throw TutorAlreadyRegisteredException()
        }
    }

    fun updatePayeeUuid(payeeUid: String?, tutorId: Int) {
        return SharedJdbi.jdbi().inTransaction<Unit, Exception> { handle ->
            handle.createUpdate("""UPDATE 
                                bitmentor.tutor_detail
                            SET payee_uuid = :payee_uuid,
                            last_modified = NOW()
                            WHERE tutor_id = :tutor_id""")
                    .bind("payee_uuid", payeeUid)
                    .bind("tutor_id", tutorId)
                    .execute()
        }
    }

    fun update(updateTutorDetailRequest: UpdateTutorDetailRequest, tutorId: Int) {
        return SharedJdbi.jdbi().inTransaction<Unit, Exception> { handle ->

            updateTutorDetailRequest.businessName?.let { businessName ->
                handle.createUpdate("""UPDATE 
                                bitmentor.tutor_detail
                            SET business_name = :business_name,
                            last_modified = NOW()
                            WHERE tutor_id = :tutor_id""")
                        .bind("business_name", businessName)
                        .bind("tutor_id", tutorId)
                        .execute()
            }

            updateTutorDetailRequest.dateOfBirth?.let { dateOfBirth ->
                handle.createUpdate("""UPDATE 
                                bitmentor.tutor_detail
                            SET date_of_birth = :date_of_birth,
                            last_modified = NOW()
                            WHERE tutor_id = :tutor_id""")
                        .bind("date_of_birth", dateOfBirth)
                        .bind("tutor_id", tutorId)
                        .execute().also {
                            TutorRepository.updateVerificationStateByUserId(VerificationState.NOT_VERIFIED, tutorId, handle)
//                            TutorRepository.updateActive(tutorId = tutorId, activeState = false, handle = handle) // TODO reinstate when requiring id before activation
                        }
            }

            updateTutorDetailRequest.phoneNumber?.let { phoneNumber ->
                handle.createUpdate("""UPDATE 
                                bitmentor.tutor_detail
                            SET phone_number = :phone_number,
                            last_modified = NOW()
                            WHERE tutor_id = :tutor_id""")
                        .bind("phone_number", phoneNumber)
                        .bind("tutor_id", tutorId)
                        .execute()
            }
        }
    }

    fun getByUserId(userId: Int): TutorDetailDao? {
        return SharedJdbi.jdbi().inTransaction<TutorDetailDao, Exception> { handle ->
            handle.createQuery("""
                $TUTOR_DETAIL_DAO_QUERY
                JOIN bitmentor.tutor ON td.tutor_id = tutor.id
                WHERE user_id = :user_id
            """)
                    .bind("user_id", userId)
                    .mapTo(TutorDetailDao::class.java)
                    .firstOrNull()
        }
    }

    fun get(tutorId: Int): TutorDetailDao? {
        return SharedJdbi.jdbi().inTransaction<TutorDetailDao, Exception> { handle ->
            handle.createQuery("""
                $TUTOR_DETAIL_DAO_QUERY
                JOIN bitmentor.tutor ON td.tutor_id = tutor.id
                WHERE tutor_id = :tutor_id
            """)
                .bind("tutor_id", tutorId)
                .mapTo(TutorDetailDao::class.java)
                .firstOrNull()
        }
    }

    fun updatePaypalEmail(email: String?, isReceivable: Boolean, tutorId: Int) {
        return SharedJdbi.jdbi().inTransaction<Unit, Exception> { handle ->
            handle.createUpdate("""UPDATE bitmentor.tutor_detail
                            SET paypal_email_address = :paypal_email_address,
                            paypal_payments_receivable = :paypal_payments_receivable,
                            last_modified = NOW()
                            WHERE tutor_id = :tutor_id""")
                .bind("paypal_email_address", email)
                .bind("paypal_payments_receivable", isReceivable)
                .bind("tutor_id", tutorId)
                .execute()
        }
    }

    fun updatePreferredPayoutOption(tutorId: Int, preferredPayoutOption: PreferredPayoutOption) {
        return SharedJdbi.jdbi().inTransaction<Unit, Exception> { handle ->
            handle.createUpdate("""UPDATE bitmentor.tutor_detail
                            SET preferred_payout_option = :preferred_payout_option,
                            last_modified = NOW()
                            WHERE tutor_id = :tutor_id""")
                .bind("preferred_payout_option", preferredPayoutOption)
                .bind("tutor_id", tutorId)
                .execute()
        }
    }
}
