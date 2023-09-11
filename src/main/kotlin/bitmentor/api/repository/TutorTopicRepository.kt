package bitmentor.api.repository

import bitmentor.api.config.SharedJdbi
import bitmentor.api.model.tutor.TutorSort
import bitmentor.api.model.tutor.TutorTopicInsert
import bitmentor.api.repository.entity.TutorTopicDao
import bitmentor.api.service.dbsVerifiedCondition
import io.ktor.util.*
import mu.KotlinLogging
import org.jdbi.v3.core.Handle

object TutorTopicRepository {
    private val logger = KotlinLogging.logger {}

    private const val TUTOR_TOPIC_DAO_QUERY = """
                        SELECT
                            tutor_id,
                            tt.id,
                            topic_id,
                            cost,
                            years,
                            t.name as name,
                            d.name as discipline,
                            d.id as discipline_id
                        FROM bitmentor.tutor_topic tt
                        JOIN bitmentor.topic t ON tt.topic_id = t.id
                        JOIN bitmentor.discipline d on d.id = t.discipline_id
                        JOIN bitmentor.tutor on tutor.id = tt.tutor_id"""

    fun getTutorTopicsByTutorId(tutorId: Int): List<TutorTopicDao> {
        return SharedJdbi.jdbi().inTransaction<List<TutorTopicDao>, Exception> { handle ->
            handle.createQuery(
            """$TUTOR_TOPIC_DAO_QUERY WHERE tutor_id = :tutor_id"""
            )
                .bind("tutor_id", tutorId)
                .mapTo(TutorTopicDao::class.java)
                .list()
        }
    }

    fun getTutorByTopicId(topicId: Int, handle: Handle, page: Int, size: Int, sort: TutorSort, dbsVerified: Boolean?): List<TutorTopicDao> {
        return handle.createQuery(
            """$TUTOR_TOPIC_DAO_QUERY
                        WHERE topic_id = :topic_id AND tutor.is_active = true ${dbsVerifiedCondition(dbsVerified)}
                        ORDER BY ${sort.column} LIMIT :size OFFSET :offset"""
        )
            .bind("offset", page * size)
            .bind("size", size)
            .bind("topic_id", topicId)
            .mapTo(TutorTopicDao::class.java)
            .list()
    }

    fun getTutorTopicsByDiscipline(
        disciplineId: Int,
        handle: Handle,
        page: Int,
        size: Int,
        sort: TutorSort,
        dbsVerified: Boolean?
    ): List<TutorTopicDao> {
        return handle.createQuery(
                """SELECT
                            topic_info.id,
                            tutor_id,
                            topic_id,
                            topic_info.name as name,
                            cost,
                            years,
                            d.id as discipline_id,
                            d.name as discipline
                        FROM (SELECT distinct on (tutor_id)
                                        u.id,
                                        u.tutor_id,
                                        t.date_updated,
                                        u.cost,
                                        u.years,
                                        t.discipline_id,
                                        t.name,
                                        u.topic_id
                              FROM bitmentor.tutor_topic u
                                JOIN bitmentor.topic t ON u.topic_id = t.id
                                WHERE t.discipline_id = :discipline_id
                              ORDER BY tutor_id) topic_info
                             JOIN bitmentor.discipline d on d.id = topic_info.discipline_id
                             JOIN bitmentor.tutor on tutor.id = topic_info.tutor_id
                        WHERE tutor.is_active = true ${dbsVerifiedCondition(dbsVerified)}
                        ORDER BY ${sort.column} LIMIT :size OFFSET :offset"""
        )
            .bind("offset", page * size)
            .bind("size", size)
            .bind("discipline_id", disciplineId)
            .mapTo(TutorTopicDao::class.java)
            .list()
    }

    @KtorExperimentalAPI
    fun countTutorsByDiscipline(disciplineId: Int, dbsVerified: Boolean?): Int {
        return SharedJdbi.jdbi().inTransaction<Int, Exception> { handle ->
            handle.createQuery(
                    """SELECT COUNT(*)
                        FROM (SELECT distinct on (tutor_id)
                                        u.tutor_id,
                                        t.discipline_id
                              FROM bitmentor.tutor_topic u
                                JOIN bitmentor.topic t ON u.topic_id = t.id
                                WHERE t.discipline_id = :discipline_id
                              ORDER BY tutor_id) topic_info
                             JOIN bitmentor.discipline d on d.id = topic_info.discipline_id
                             JOIN bitmentor.tutor on tutor.id = topic_info.tutor_id
                        WHERE tutor.is_active = true ${dbsVerifiedCondition(dbsVerified)}"""
            )
                    .bind("discipline_id", disciplineId)
                    .mapTo(Int::class.java)
                    .first()
        }
    }

    @KtorExperimentalAPI
    fun countTutorsByTopic(topicId: Int, dbsVerified: Boolean?): Int {
        return SharedJdbi.jdbi().inTransaction<Int, Exception> { handle ->
            handle.createQuery(
                    """SELECT COUNT(*) 
                        FROM (SELECT distinct on (tutor_id) u.*
                              FROM bitmentor.tutor_topic u
                                ORDER BY tutor_id) tt
                        JOIN bitmentor.tutor t on t.id = tt.tutor_id
                        WHERE t.is_active = true AND topic_id = :topic_id ${dbsVerifiedCondition(dbsVerified)}"""
            )
                    .bind("topic_id", topicId)
                    .mapTo(Int::class.java)
                    .first().also {
                        TopicRepository.incrementTopicSearchStat(topicId, handle)
                    }
        }
    }

    fun findTopicsByTutorId(tutorId: Int, handle: Handle): List<TutorTopicDao> {
        return handle.createQuery(
                """$TUTOR_TOPIC_DAO_QUERY WHERE tutor_id = :tutor_id"""
            )
                .bind("tutor_id", tutorId)
                .mapTo(TutorTopicDao::class.java)
                .list()
    }

    fun createTopic(
            tutorTopic: TutorTopicInsert,
            tutorId: Int
    ): Int {
        try {
            return SharedJdbi.jdbi().inTransaction<Int, Exception> { handle ->
                handle.createUpdate(
                        """INSERT INTO bitmentor.tutor_topic(
                                    tutor_id, 
                                    topic_id, 
                                    cost,
                                    years
                            )VALUES(
                                    :tutor_id, 
                                    :topic_id, 
                                    :cost,
                                    :years
                            )"""
                )
                        .bind("tutor_id", tutorId)
                        .bind("topic_id", tutorTopic.topicId)
                        .bind("cost", tutorTopic.cost)
                        .bind("years", tutorTopic.years)
                        .executeAndReturnGeneratedKeys("id")
                        .mapTo(Int::class.java).first()
            }
        } catch (e: Exception) {
            logger.error { "An error has occurred saving the tutor topic ${e.message}" }
            throw Exception("An error has occurred saving the tutor topic")
        }
    }

    fun deleteTutorTopic(tutorTopicId: Int, handle: Handle) {
        try {
                handle.createUpdate(
                    """DELETE FROM
                                bitmentor.tutor_topic
                            WHERE id = :id;"""
                )
                    .bind("id", tutorTopicId)
                    .execute()

        } catch (e: Exception) {
            logger.error { "An error has occurred deleting the tutor topic ${e.message}" }
            throw Exception("An error has occurred deleting the tutor topic")
        }
    }
}
