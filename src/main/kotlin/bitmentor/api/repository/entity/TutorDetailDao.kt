package bitmentor.api.repository.entity

import bitmentor.api.model.tutor_detail.PreferredPayoutOption
import org.jdbi.v3.core.mapper.reflect.ColumnName
import java.time.LocalDate

data class TutorDetailDao(
        val tutorId: Int,
        val dateOfBirth: LocalDate?,
        val businessName: String?,
        val phoneNumber: String?,
        val payeeUuid: String?,
        @ColumnName("location")
        val location: String?,
        val paypalEmailAddress: String?,
        val paypalPaymentsReceivable: Boolean?,
        val preferredPayoutOption: PreferredPayoutOption?
)