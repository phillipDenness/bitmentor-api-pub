package bitmentor.api.repository.entity

data class TutorTopicDao(
        val id: Int,
        val tutorId: Int,
        val topicId: Int,
        val name: String,
        val cost: Double,
        val years: Int,
        val disciplineId: Int,
        val discipline: String
)