package bitmentor.api.model.tutor

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class TutorTopicInsert(
    val topicId: Int,
    val name: String,
    val cost: Double,
    val years: Int
)
