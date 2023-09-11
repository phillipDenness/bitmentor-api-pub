package bitmentor.api.repository.entity

import bitmentor.api.model.student.AreasOfInterest
import bitmentor.api.model.tutor.VerificationState
import org.jdbi.v3.core.mapper.reflect.ColumnName
import java.time.LocalDate

data class TutorDao(
        @ColumnName("id")
        val id: Int,

        @ColumnName("user_id")
        val userId: Int,

        @ColumnName("is_active")
        val isActive: Boolean,

        @ColumnName("id_verification_state")
        val idVerificationState: VerificationState,

        val dbsVerificationState: VerificationState,
        @ColumnName("tutor_join_Date")
        val tutorJoinDate: LocalDate,

        @ColumnName("tagline")
        val tagline: String,

        @ColumnName("about")
        val about: String,
        val experience: String?,
        val github: String?,

        @ColumnName("availability")
        val availability: String,

        @ColumnName("rating")
        val rating: Double,

        @ColumnName("ratings")
        val ratings: Int,

        val promotions: List<String>?,
        val areasOfInterest: List<AreasOfInterest>?,
        val otherInterest: String?
)
