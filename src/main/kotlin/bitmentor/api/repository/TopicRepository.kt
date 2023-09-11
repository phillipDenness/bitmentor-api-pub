package bitmentor.api.repository

import bitmentor.api.config.SharedJdbi
import bitmentor.api.model.topic.TopicDao
import io.ktor.util.*
import mu.KotlinLogging
import org.jdbi.v3.core.Handle


@KtorExperimentalAPI
object TopicRepository {
    private val logger = KotlinLogging.logger {}

    private const val TOPIC_STATS_DAO_QUERY: String = """
                        SELECT
                            t.id as id,
                            t.name as name,
                            searches,
                            t.date_updated,
                            discipline_id,
                            language_id,
                            d.name as discipline,
                           latest_grad_jobs,1
                           latest_total_jobs
                        FROM
                            bitmentor.topic t
                                JOIN bitmentor.discipline d ON t.discipline_id = d.id
                                full outer join bitmentor.topic_meta tm on t.id = tm.topic_id
                        """

    fun getTopicStatsByTopic(topic: String, handle: Handle): TopicDao? {
        return handle.createQuery(
                """$TOPIC_STATS_DAO_QUERY WHERE t.name = :name;"""
        )
            .bind("name", topic)
            .mapTo(TopicDao::class.java)
            .firstOrNull()
    }


    fun getTopTopicByGrad(size: Int): List<TopicDao> {
        return SharedJdbi.jdbi().inTransaction<List<TopicDao>, Exception> { handle ->
            handle.createQuery("""$TOPIC_STATS_DAO_QUERY
                WHERE NOT (t.id = ANY ('{5,6,10}'::int[]))
                ORDER BY latest_grad_jobs DESC LIMIT $size;""")
                .mapTo(TopicDao::class.java)
                .list()
        }
    }

    fun getTopicById(topicId: Int): TopicDao? {
        return SharedJdbi.jdbi().inTransaction<TopicDao, Exception> { handle ->
            getTopicById(topicId, handle)
        }
    }

    fun getTopicById(topicId: Int, handle: Handle): TopicDao? {
        return handle.createQuery(
                """$TOPIC_STATS_DAO_QUERY WHERE t.id = :id;"""
        )
                .bind("id", topicId)
                .mapTo(TopicDao::class.java)
                .firstOrNull()
    }

    fun getTopicByDiscipline(discipline: String): List<TopicDao> {
        return SharedJdbi.jdbi().inTransaction<List<TopicDao>, Exception> { handle ->
            handle.createQuery(
                """$TOPIC_STATS_DAO_QUERY WHERE d.name = :discipline_name;"""
            )
                .bind("discipline_name", discipline)
                .mapTo(TopicDao::class.java)
                .list()
        }
    }

    fun getTopTopicBySearches(size: Int): List<TopicDao> {
        return SharedJdbi.jdbi().inTransaction<List<TopicDao>, Exception> { handle ->
            handle.createQuery("""$TOPIC_STATS_DAO_QUERY 
                ORDER BY searches DESC LIMIT $size;""")
                    .mapTo(TopicDao::class.java)
                    .list()
        }
    }

    fun getAllTopics(): List<TopicDao> {
        return SharedJdbi.jdbi().inTransaction<List<TopicDao>, Exception> { handle ->
            handle.createQuery("""$TOPIC_STATS_DAO_QUERY 
                ORDER BY searches DESC""")
                    .mapTo(TopicDao::class.java)
                    .list()
        }
    }

    fun incrementTopicSearchStat(topicId: Int, handle: Handle) {
        logger.info { "Incrementing topicId $topicId by 1" }
        handle.createUpdate("""UPDATE bitmentor.topic
            SET searches = searches + 1
            WHERE id = :topicId
        """)
                .bind("topicId", topicId)
                .execute()
    }
}
