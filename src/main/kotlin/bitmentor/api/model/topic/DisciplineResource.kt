package bitmentor.api.model.topic


data class DisciplinesResource(
    val disciplines: List<DisciplineResource>,
    val total: Int
)

data class DisciplineResource(
    val disciplineId: Int,
    val discipline: String,
    val topics: List<TopicDao>
)