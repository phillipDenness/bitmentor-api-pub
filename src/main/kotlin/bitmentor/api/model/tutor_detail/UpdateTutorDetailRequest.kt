package bitmentor.api.model.tutor_detail

import java.time.LocalDate

data class UpdateTutorDetailRequest(
    val dateOfBirth: LocalDate?,
    val businessName: String?,
    val phoneNumber: String?
)
