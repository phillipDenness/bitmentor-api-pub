package bitmentor.api.model.tutor_detail

import bitmentor.api.model.user.Location
import java.time.LocalDate

data class TutorDetailInsert(
        val tutorId: Int,
        val dateOfBirth: LocalDate,
        val businessName: String?,
        val phoneNumber: String,
        val location: Location
)