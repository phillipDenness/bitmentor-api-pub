package bitmentor.api.service

import bitmentor.api.model.topic.TopicDao
import bitmentor.api.repository.TopicRepository
import io.ktor.util.*

@KtorExperimentalAPI
fun getTopTopics(top: Int): List<TopicDao> {
    return TopicRepository.getTopTopicBySearches(top)
}

@KtorExperimentalAPI
fun getTopTopicByGradJobs(top: Int): List<TopicDao> {
    return TopicRepository.getTopTopicByGrad(top)
}

@KtorExperimentalAPI
fun getTopics(): List<TopicDao> {
    return TopicRepository.getAllTopics()
}

@KtorExperimentalAPI
fun getTopicsByDiscipline(discipline: String): List<TopicDao> {
    return TopicRepository.getTopicByDiscipline(discipline)
}
