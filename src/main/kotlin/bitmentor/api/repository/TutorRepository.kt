package bitmentor.api.repository

import bitmentor.api.config.GenericObjectMapper
import bitmentor.api.config.Properties
import bitmentor.api.config.Properties.jsonObject
import bitmentor.api.config.SharedJdbi
import bitmentor.api.exceptions.TutorAlreadyRegisteredException
import bitmentor.api.model.notification.NotificationInsert
import bitmentor.api.model.notification.NotificationType
import bitmentor.api.model.tutor.TutorInsert
import bitmentor.api.model.tutor.TutorSort
import bitmentor.api.model.tutor.UpdateTutorRequest
import bitmentor.api.model.tutor.VerificationState
import bitmentor.api.repository.entity.TutorDao
import bitmentor.api.service.dbsVerifiedCondition
import io.ktor.features.*
import io.ktor.util.*
import mu.KotlinLogging
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.generic.GenericType
import org.jdbi.v3.core.statement.UnableToExecuteStatementException


@KtorExperimentalAPI
object TutorRepository {
    private val logger = KotlinLogging.logger {}

    private const val TUTOR_DAO_QUERY: String = """
                        SELECT 
                            id,
                            user_id, 
                            is_active, 
                            id_verification_state, 
                            dbs_verification_state,
                            tutor_join_date,
                            tagline, 
                            about, 
                            experience, 
                            github, 
                            availability,
                            rating,
                            ratings,
                            promotions,
                            areas_of_interest,
                            other_interest
                        FROM 
                            bitmentor.tutor"""

    fun getTutor(tutorId: Int): TutorDao? {
        return SharedJdbi.jdbi().inTransaction<TutorDao?, Exception> { handle ->
            getTutor(tutorId, handle)
        }
    }

    fun getTutor(tutorId: Int, handle: Handle): TutorDao? {
        return handle.createQuery(
            """$TUTOR_DAO_QUERY WHERE id = :id"""
        )
            .bind("id", tutorId)
            .mapTo(TutorDao::class.java)
            .firstOrNull()
    }

    fun countTutors(dbsVerified: Boolean?): Int {
        return SharedJdbi.jdbi().inTransaction<Int, Exception> { handle ->
            handle.createQuery(
                    """SELECT COUNT(*) 
                        FROM bitmentor.tutor t
                        WHERE t.is_active = true ${dbsVerifiedCondition(dbsVerified)}"""
            )
                    .mapTo(Int::class.java)
                    .first()
        }
    }

    fun findTutorByUserId(userId: Int, handle: Handle): TutorDao? {
            return handle.createQuery(
                """$TUTOR_DAO_QUERY WHERE user_id = :user_id"""
            )
                .bind("user_id", userId)
                .mapTo(TutorDao::class.java)
                .firstOrNull()
    }

    fun findTutorByUserId(userId: Int): TutorDao? {
        return SharedJdbi.jdbi().inTransaction<TutorDao?, Exception> { handle ->
            findTutorByUserId(userId, handle)
        }
    }

    fun createTutor(
            tutorInsert: TutorInsert,
            userId: Int
    ): Int {
        try {
            val insertAvailability = jsonObject()
            insertAvailability.type = Properties.JSON
            insertAvailability.value = GenericObjectMapper.getMapper().writeValueAsString(tutorInsert.availability)

            return SharedJdbi.jdbi().inTransaction<Int, Exception> { handle ->
                handle.createUpdate(
                        """INSERT INTO bitmentor.tutor(
                                    user_id, 
                                    is_active, 
                                    id_verification_state, 
                                    dbs_verification_state,
                                    tutor_join_date, 
                                    tagline,
                                    about,
                                    experience,
                                    github,
                                    availability,
                                    rating,
                                    ratings,
                                    areas_of_interest,
                                    other_interest
                            )VALUES(
                                    :user_id, 
                                    :is_active, 
                                    :id_verification_state, 
                                    :dbs_verification_state,
                                    now(), 
                                    :tagline,
                                    :about,
                                    :experience,
                                    :github,
                                    :availability,
                                    :rating,
                                    :ratings,
                                    :areas_of_interest,
                                    :other_interest
                            )"""
                )
                    .bind("user_id", userId)
                    .bind("is_active", tutorInsert.active)
                    .bind("id_verification_state", VerificationState.NOT_VERIFIED)
                    .bind("dbs_verification_state", VerificationState.NOT_VERIFIED)
                    .bind("tagline", tutorInsert.tagline)
                    .bind("about", tutorInsert.about)
                    .bind("experience", tutorInsert.experience)
                    .bind("github", tutorInsert.github)
                    .bind("availability", insertAvailability)
                    .bind("rating", 0)
                    .bind("ratings", 0)
                    .bindByType("areas_of_interest", tutorInsert.areasOfInterest, object : GenericType<List<String>>() {})
                    .bind("other_interest", tutorInsert.other)
                    .executeAndReturnGeneratedKeys("id").mapTo(Int::class.java).first()
                    .also {
                        tutorInsert.tutorTopics.forEach { topic ->
                            TutorTopicRepository.createTopic(
                                    tutorTopic = topic,
                                    tutorId = it
                            )
                        }
                        TutorDetailRepository.create(tutorId = it, handle = handle)
                        tutorInsert.projects.forEach { project ->
                            TutorProjectRepository.create(
                                projectInsert = project,
                                handle = handle,
                                tutorId = it
                            )
                        }

                        if (!tutorInsert.active) {
                            // Todo reinstate activation after sign up
                            NotificationRepository.createNotification(
                                notificationInsert = NotificationInsert(
                                    userId = userId,
                                    type = NotificationType.COMPLETE_TUTOR
                                ),
                                handle = handle
                            )
                        }
                    }
            }
        } catch (e: UnableToExecuteStatementException) {
            logger.error { "Error saving tutor $e" }
            throw TutorAlreadyRegisteredException()
        }
    }

    fun updateTutor(tutorInsert: UpdateTutorRequest, tutorId: Int) {
        try {
            val insertAvailability = jsonObject()
            insertAvailability.type = Properties.JSON
            insertAvailability.value = GenericObjectMapper.getMapper().writeValueAsString(tutorInsert.availability)

            return SharedJdbi.jdbi().inTransaction<Unit, Exception> { handle ->

                val lastUpdate = getTutor(tutorId)?.let {
                    if (tutorInsert.tutorTopics.size < 0 && tutorInsert.isActive ) {
                        logger.warn { "User tried to remove all topics and become active" }
                        throw BadRequestException("Must have at least 1 topic to be active")
                    }
                    it
                }
                handle.createUpdate(
                        """UPDATE 
                                bitmentor.tutor
                            SET tagline = :tagline,
                             is_active = :is_active,
                            about = :about,
                            experience = :experience,
                            github = :github,
                            availability = :availability,
                            areas_of_interest = :areas_of_interest,
                            other_interest = :other_interest
                            WHERE id = :tutor_id;"""
                )
                    .bind("tutor_id", tutorId)
                    .bind("is_active", tutorInsert.isActive)
                    .bind("tagline", tutorInsert.tagline)
                    .bind("about", tutorInsert.about)
                    .bind("experience", tutorInsert.experience)
                    .bind("github", tutorInsert.github)
                    .bind("availability", insertAvailability)
                    .bindByType("areas_of_interest", tutorInsert.areasOfInterest, object : GenericType<List<String>>() {})
                    .bind("other_interest", tutorInsert.otherInterest)
                    .execute().also {

                    val existingSkills = TutorTopicRepository.findTopicsByTutorId(
                        tutorId = tutorId,
                        handle = handle
                    )

                    existingSkills.forEach {
                        TutorTopicRepository.deleteTutorTopic(
                            tutorTopicId = it.id,
                            handle = handle
                        )
                    }

                    tutorInsert.tutorTopics.forEach { toInsert ->
                        TutorTopicRepository.createTopic(
                            tutorTopic = toInsert,
                            tutorId = tutorId
                        )
                    }
                    TutorProjectRepository.update(
                        tutorId = tutorId,
                        projects = tutorInsert.projects,
                        handle = handle
                    )
                    if (tutorInsert.isActive && !lastUpdate!!.isActive) {
                        NotificationRepository.createNotification(
                            notificationInsert =  NotificationInsert(
                                userId = lastUpdate.userId,
                                type = NotificationType.PUBLIC_TUTOR
                            ),
                            handle = handle
                        )
                        NotificationRepository.deleteNotification(
                            userId = lastUpdate.userId,
                            type = NotificationType.COMPLETE_TUTOR,
                            handle = handle
                        )
                    }
                }
            }
        } catch (e: Exception) {
            logger.error { "An error has occurred updating the tutor ${e.message}" }
            throw Exception("An error has occurred updating the tutor account")
        }
    }

    fun getPagedTutors(page: Int, size: Int, sort: TutorSort, dbsVerified: Boolean?): List<TutorDao> {
        return SharedJdbi.jdbi().inTransaction<List<TutorDao>, Exception> { handle ->
            handle.createQuery("""$TUTOR_DAO_QUERY 
                WHERE is_active = true ${dbsVerifiedCondition(dbsVerified)}
                ORDER BY ${sort.column} LIMIT :size OFFSET :offset""")
                .bind("offset", page * size)
                .bind("size", size)
                .mapTo(TutorDao::class.java)
                .toList()
        }
    }

    fun getPagedTutorsByDisciplineId(page: Int, size: Int, disciplineId: Int, sort: TutorSort, dbsVerified: Boolean?): List<TutorDao> {
        return SharedJdbi.jdbi().inTransaction<List<TutorDao>, Exception> { handle ->
            TutorTopicRepository.getTutorTopicsByDiscipline(
                disciplineId = disciplineId,
                handle = handle,
                page = page,
                size = size,
                sort = sort,
                dbsVerified = dbsVerified
            ).map {
                getTutor(it.tutorId, handle)!!
            }
        }
    }

    fun getPagedTutorsByTopicId(page: Int, size: Int, topicId: Int, sort: TutorSort, dbsVerified: Boolean?): List<TutorDao> {
        return SharedJdbi.jdbi().inTransaction<List<TutorDao>, Exception> { handle ->
            TutorTopicRepository.getTutorByTopicId(
                topicId = topicId,
                handle = handle,
                page = page,
                size = size,
                sort = sort,
                dbsVerified = dbsVerified
            ).map {
                getTutor(it.tutorId, handle)!!
            }
        }
    }

    fun updateReview(tutorId: Int, rating: Double, ratings: Int, handle: Handle) {
        handle.createUpdate("""UPDATE 
                                bitmentor.tutor
                            SET rating = :rating,
                            ratings = :ratings
                            WHERE id = :tutor_id""")
                .bind("rating", rating)
                .bind("ratings", ratings)
                .bind("tutor_id", tutorId)
                .execute()
    }

    fun updateActive(tutorId: Int, activeState: Boolean, handle: Handle) {
        handle.createUpdate("""UPDATE 
                                bitmentor.tutor
                            SET is_active = :active_state
                            WHERE id = :tutor_id""")
                .bind("active_state", activeState)
                .bind("tutor_id", tutorId)
                .execute()
    }

    fun updateVerificationStateByUserId(tutorState: VerificationState, tutorId: Int, handle: Handle) {
        handle.createUpdate("""UPDATE 
                            bitmentor.tutor
                        SET id_verification_state = :id_verification_state
                        WHERE id = :tutor_id""")
                .bind("id_verification_state", tutorState)
                .bind("tutor_id", tutorId)
                .execute()
    }

    fun updateIdState(tutorState: VerificationState, tutorId: Int) {
        return SharedJdbi.jdbi().inTransaction<Unit, Exception> { handle ->
            handle.createUpdate("""UPDATE 
                                bitmentor.tutor
                            SET id_verification_state = :id_verification_state
                            WHERE id = :tutor_id""")
                    .bind("id_verification_state", tutorState)
                    .bind("tutor_id", tutorId)
                    .execute()
        }
    }

    fun updateDbsState(tutorState: VerificationState, tutorId: Int) {
        return SharedJdbi.jdbi().inTransaction<Unit, Exception> { handle ->
            handle.createUpdate("""UPDATE 
                                bitmentor.tutor
                            SET dbs_verification_state = :dbs_verification_state
                            WHERE id = :tutor_id""")
                    .bind("dbs_verification_state", tutorState)
                    .bind("tutor_id", tutorId)
                    .execute()
        }
    }

    fun updatePromotions(tutorId: Int, promotions: List<String>) {
        return SharedJdbi.jdbi().inTransaction<Unit, Exception> { handle ->
            handle.createUpdate("""UPDATE 
                                bitmentor.tutor
                            SET promotions = :promotions
                            WHERE id = :tutor_id""")
                    .bindByType("promotions", promotions, object : GenericType<List<String>>() {})
                    .bind("tutor_id", tutorId)
                    .execute()
        }
    }
}
