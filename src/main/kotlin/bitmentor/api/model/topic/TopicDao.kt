package bitmentor.api.model.topic

import java.time.ZonedDateTime

data class TopicDao(
        val id: Int,
        val name: String,
        val searches: Int,
        val languageId: Int?,
        val disciplineId: Int,
        val discipline: String,
        val dateUpdated: ZonedDateTime,
        val latestTotalJobs: Int,
        val latestGradJobs: Int
)