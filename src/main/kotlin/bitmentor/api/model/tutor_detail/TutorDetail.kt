package bitmentor.api.model.tutor_detail

import bitmentor.api.model.payee.PayeeAccountResource
import bitmentor.api.model.user.Location

data class TutorDetail (
        val tutorId: Int,
        val dateOfBirth: String?,
        val businessName: String?,
        val phoneNumber: String?,
        val account: PayeeAccountResource?,
        val location: Location?,
        val paypalEmailAddress: String?,
        val paypalPaymentsReceivable: Boolean?,
        val preferredPayoutOption: PreferredPayoutOption?
)