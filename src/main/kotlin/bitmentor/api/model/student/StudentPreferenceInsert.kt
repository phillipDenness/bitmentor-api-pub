package bitmentor.api.model.student


data class StudentPreferenceInsert(
    val interests: List<AreasOfInterest>,
    val other: String?,
    val topicIds: List<Int>
)