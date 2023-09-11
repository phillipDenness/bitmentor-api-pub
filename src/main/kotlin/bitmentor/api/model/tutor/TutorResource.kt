package bitmentor.api.model.tutor

import bitmentor.api.model.availability.AvailabilityTutor
import bitmentor.api.model.promotion.promotions.Promotion
import bitmentor.api.model.student.AreasOfInterest
import bitmentor.api.repository.entity.TutorProjectDao
import java.time.LocalDate
import java.time.ZonedDateTime

data class TutorResource(
    val id: Int,
    val tutorUserId: Int,
    val displayName: String,
    val lastOnline: ZonedDateTime,
    val profileImageUrl: String?,
    val idVerificationState: VerificationState,
    val dbsVerificationState: VerificationState,
    val isActive: Boolean,
    val tutorJoinDate: LocalDate,
    val tagline: String,
    val about: String,
    val experience: String?,
    val github: String?,
    val availability: AvailabilityTutor,
    val tutorTopics: List<TutorTopic>,
    val rating: Double,
    val promotions: List<Promotion>,
    val ratings: Int,
    val areasOfInterest: List<AreasOfInterest>,
    val otherInterest: String?,
    val projects: List<TutorProjectDao>
)
