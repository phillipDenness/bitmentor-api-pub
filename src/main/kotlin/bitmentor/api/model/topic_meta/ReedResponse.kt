package bitmentor.api.model.topic_meta

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class ReedResponse(
    val totalResults: Int
)