package bitmentor.api.model.topic_meta

import java.time.LocalDate

data class TopicMetaData(
    val topicId: Int,
    val totalJobs: Map<LocalDate, Int>,
    val gradJobs: Map<LocalDate, Int>,
    val latestTotalJobs: Int,
    val latestGradJobs: Int
)
