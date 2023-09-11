package bitmentor.api.model.tutor

data class TutorTopic(
    val id: Int,
    val topicId: Int,
    val name: String,
    val discipline: String,
    val disciplineId: Int,
    val cost: Double,
    val years: Int
)
