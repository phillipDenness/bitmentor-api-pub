package bitmentor.api.model.short_list

import bitmentor.api.model.tutor.TutorTopic

data class TutorShortList(
    val tutorId: Int,
    val displayName: String,
    val tagline: String,
    val topics: List<TutorTopic>,
    val profileImageUrl: String?
)