package bitmentor.api.model.tutor

import bitmentor.api.model.availability.AvailabilityTutor
import bitmentor.api.model.project.TutorProjectInsert
import bitmentor.api.model.student.AreasOfInterest
import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class TutorInsert(
    val tagline: String,
    val about: String,
    val experience: String?,
    val github: String?,
    val availability: AvailabilityTutor,
    val tutorTopics: List<TutorTopicInsert>,
    val active: Boolean,
    val other: String?,
    val areasOfInterest: List<AreasOfInterest>,
    val projects: List<TutorProjectInsert>
)