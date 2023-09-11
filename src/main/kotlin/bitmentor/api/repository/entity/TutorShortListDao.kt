package bitmentor.api.repository.entity

data class TutorShortListDao(
    val tutorId: Int,
    val displayName: String,
    val tagline: String,
    val profileImageUrl: String?
)