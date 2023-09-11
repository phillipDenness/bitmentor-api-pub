package bitmentor.api.model.topic

data class TopicsResource(
        val topics: List<TopicDao>,
        val total: Int
)
