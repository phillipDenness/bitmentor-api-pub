package bitmentor.api.model.tutor

import bitmentor.api.model.topic_meta.TopicMeta

data class TutorPagedResource(
        val tutors: List<TutorResource>,
        val topicMeta: TopicMeta? = null,
        val totalTutors: Int,
        val page: Int,
        val size: Int
)