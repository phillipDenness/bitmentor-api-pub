package bitmentor.api.model.project

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class TutorProjectInsert(
    val title: String,
    val description: String,
    val link: String
)
