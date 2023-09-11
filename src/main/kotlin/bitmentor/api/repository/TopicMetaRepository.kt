package bitmentor.api.repository

import bitmentor.api.config.GenericObjectMapper
import bitmentor.api.config.Properties
import bitmentor.api.config.SharedJdbi
import bitmentor.api.model.topic_meta.TopicMetaData
import bitmentor.api.repository.entity.TopicMetaDao
import com.fasterxml.jackson.core.type.TypeReference
import io.ktor.util.*
import mu.KotlinLogging
import org.postgresql.util.PGobject
import java.time.LocalDate
import java.util.*

@KtorExperimentalAPI
object TopicMetaRepository {
    private val logger = KotlinLogging.logger {}

    private val typeRef = object : TypeReference<HashMap<LocalDate, Int>>() {}

    fun get(topicId: Int): TopicMetaData? {
        return SharedJdbi.jdbi().inTransaction<TopicMetaData?, Exception> { handle ->
            handle.createQuery("""
                        SELECT * FROM bitmentor.topic_meta WHERE topic_id = :topic_id"""
                    )
                            .bind("topic_id", topicId)
                            .mapTo(TopicMetaDao::class.java)
                            .firstOrNull()?.toTopicMetaData()
        }
    }

    fun update(topicMetaData: TopicMetaData) {
        return SharedJdbi.jdbi().inTransaction<Unit, Exception> { handle ->
            handle.createUpdate("""
                UPDATE bitmentor.topic_meta
                SET total_jobs = :total_jobs,
                grad_jobs = :grad_jobs,
                date_updated = now(), 
                latest_total_jobs = :latest_total_jobs, 
                latest_grad_jobs = :latest_grad_jobs
                WHERE topic_id = :topic_id"""
            )
                .bind("topic_id", topicMetaData.topicId)
                .bind("total_jobs", insertJson(topicMetaData.totalJobs))
                .bind("grad_jobs", insertJson(topicMetaData.gradJobs))
                .bind("latest_total_jobs", topicMetaData.latestTotalJobs)
                .bind("latest_grad_jobs", topicMetaData.latestGradJobs)
                .execute()
        }
    }

    fun save(topicMetaData: TopicMetaData) {
        return SharedJdbi.jdbi().inTransaction<Unit, Exception> { handle ->
            handle.createUpdate("""
                INSERT INTO bitmentor.topic_meta(topic_id, total_jobs, grad_jobs, date_updated, latest_total_jobs, latest_grad_jobs)
                VALUES (:topic_id, :total_jobs, :grad_jobs, now(), :latest_total_jobs, :latest_grad_jobs)"""
            )
                .bind("topic_id", topicMetaData.topicId)
                .bind("total_jobs", insertJson(topicMetaData.totalJobs))
                .bind("grad_jobs", insertJson(topicMetaData.gradJobs))
                .bind("latest_total_jobs", topicMetaData.latestTotalJobs)
                .bind("latest_grad_jobs", topicMetaData.latestGradJobs)
                .execute()
        }
    }

    private fun TopicMetaDao.toTopicMetaData():TopicMetaData {
        return TopicMetaData(
            topicId = this.topicId,
            totalJobs = GenericObjectMapper.getMapper().readValue(this.totalJobs, typeRef),
            gradJobs = GenericObjectMapper.getMapper().readValue(this.gradJobs, typeRef),
            latestGradJobs = this.latestGradJobs,
            latestTotalJobs = this.latestTotalJobs
        )
    }

    private fun insertJson(map: Map<LocalDate, Int>): PGobject {
        val insert = Properties.jsonObject()
        insert.type = Properties.JSON
        insert.value = GenericObjectMapper.getMapper().writeValueAsString(map)
        return insert
    }
}
