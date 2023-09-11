package bitmentor.api.repository.entity

import java.time.ZonedDateTime

data class TopicMetaDao(
        val topicId: Int,
        val totalJobs: String,
        val gradJobs: String,
        val dateUpdated: ZonedDateTime,
        val latestTotalJobs: Int,
        val latestGradJobs: Int
)