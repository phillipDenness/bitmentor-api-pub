package bitmentor.api.model.topic_meta

data class TopicMeta(
    val totalJobs: TopicMetaAggregation,
    val gradJobs: TopicMetaAggregation
)


data class TopicMetaAggregation(
    val jobs: Int,
    val change7Days: Int,
    val change30Days: Int
)
