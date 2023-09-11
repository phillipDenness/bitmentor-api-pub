package bitmentor.api.service

import bitmentor.api.config.Properties
import bitmentor.api.config.reedClient
import bitmentor.api.model.topic.TopicDao
import bitmentor.api.model.topic_meta.ReedResponse
import bitmentor.api.model.topic_meta.TopicMeta
import bitmentor.api.model.topic_meta.TopicMetaAggregation
import bitmentor.api.model.topic_meta.TopicMetaData
import bitmentor.api.repository.TopicMetaRepository
import bitmentor.api.repository.TopicRepository
import io.ktor.client.request.*
import io.ktor.util.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import mu.KotlinLogging
import java.time.LocalDate
import kotlin.concurrent.timer

private val logger = KotlinLogging.logger {}

@KtorExperimentalAPI
fun initTopicMetaHandler() {
    logger.info { "Starting topic meta scheduler task with delay 3600000" }

    timer("scheduleNotificationTask", false, 0, 3600000) {
        GlobalScope.launch {
            val date = LocalDate.now()
            TopicRepository.getAllTopics().forEach { topic -> fetchDataFromReed(topic, date) }
        }
    }
}

@KtorExperimentalAPI
suspend fun fetchDataFromReed(topic: TopicDao, date: LocalDate) {
    reedClient().use { client ->
        val total = client.get<ReedResponse> {
            url("${Properties.reedUrl}/api/1.0/search?keywords=${topic.name}")
        }
        val grad = client.get<ReedResponse> {
            url("${Properties.reedUrl}/api/1.0/search?keywords=${topic.name}&graduate=true")
        }
        logger.trace { "Fetching data from reed api for topic ${topic.name} for $date" }

        saveTopicMeta(date = date, totalJobs = total.totalResults, gradJobs = grad.totalResults, topicId = topic.id)
    }
}

@KtorExperimentalAPI
fun saveTopicMeta(date: LocalDate, totalJobs: Int, gradJobs: Int, topicId: Int) {
    val topicData = TopicMetaRepository.get(topicId)
    if (topicData == null) {
        TopicMetaRepository.save(TopicMetaData(
            topicId = topicId,
            totalJobs = mapOf(Pair(date, totalJobs)),
            gradJobs = mapOf(Pair(date, gradJobs)),
            latestTotalJobs = totalJobs,
            latestGradJobs = gradJobs
        ))
    } else {
        logger.trace { "Returned existing data for topicId: $topicId. Data $topicData" }
        val totalMap = topicData.totalJobs.toMutableMap()
        totalMap.putIfAbsent(date, totalJobs)
        val gradMap = topicData.gradJobs.toMutableMap()
        gradMap.putIfAbsent(date, gradJobs)

        val insert = TopicMetaData(
            topicId = topicId,
            totalJobs = totalMap,
            gradJobs = gradMap,
            latestTotalJobs = totalJobs,
            latestGradJobs = gradJobs
        )
        logger.trace { "Inserting new data $insert" }
        TopicMetaRepository.update(insert)
    }
}

@KtorExperimentalAPI
fun getTopicMeta(topicId: Int): TopicMeta? {
    return TopicMetaRepository.get(topicId)?.let { topicData ->
        val totalJobs: List<Int> = topicData.totalJobs.entries.sortedWith(compareBy { it.key }).reversed().map { it.value }
        val gradJobs: List<Int> = topicData.gradJobs.entries.sortedWith(compareBy { it.key }).reversed().map { it.value }

        TopicMeta(
            totalJobs = createTopicMetaAggregation(totalJobs),
            gradJobs = createTopicMetaAggregation(gradJobs)
        )
    }
}

fun createTopicMetaAggregation(jobs: List<Int>): TopicMetaAggregation {
    return TopicMetaAggregation(
        jobs.first(),
        change7Days = if (jobs.size > 6) {calculateChange(jobs.first(), jobs[6])} else {0},
        change30Days = if (jobs.size > 29) {calculateChange(jobs.first(), jobs[29])} else {0}
    )
}

fun calculateChange(a: Int, b: Int): Int {
    return (((a.toDouble() - b.toDouble()) / b.toDouble()) * 100).toInt()
}