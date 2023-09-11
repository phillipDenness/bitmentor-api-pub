package bitmentor.api.repository

import bitmentor.api.config.SharedJdbi
import bitmentor.api.model.student.StudentPreferenceInsert
import bitmentor.api.repository.entity.StudentPreferenceDao
import io.ktor.util.*
import mu.KotlinLogging
import org.jdbi.v3.core.generic.GenericType


@KtorExperimentalAPI
object StudentPreferenceRepository {
    private val logger = KotlinLogging.logger {}

    private const val STUDENT_PREFERENCE_DAO_QUERY: String = """
                        SELECT 
                            id,
                            interests,
                            other,
                            user_id,
                            topic_ids,
                            date_created
                        FROM 
                            bitmentor.student_preference
                     """

    fun getTutorShortList(userId: Int): StudentPreferenceDao? {
        return SharedJdbi.jdbi().inTransaction<StudentPreferenceDao, Exception> { handle ->
            handle.createQuery(
                """$STUDENT_PREFERENCE_DAO_QUERY 
                    WHERE user_id = :user_id
                    """
            )
                .bind("user_id", userId)
                .mapTo(StudentPreferenceDao::class.java)
                .firstOrNull()
        }
    }

    fun save(studentPreferenceInsert: StudentPreferenceInsert, userId: Int): Int {
        return SharedJdbi.jdbi().inTransaction<Int, Exception> { handle ->
            handle.createUpdate(
                """INSERT INTO bitmentor.student_preference(
                                interests, 
                                topic_ids, 
                                other,
                                user_id,
                                date_created
                        )VALUES(
                                :interests, 
                                :topic_ids, 
                                :other, 
                                :user_id,
                                now()
                        )
                     """
            )
                .bindByType("interests", studentPreferenceInsert.interests, object : GenericType<List<String>>() {})
                .bindByType("topic_ids", studentPreferenceInsert.topicIds, object : GenericType<List<Int>>() {})
                .bind("other", studentPreferenceInsert.other)
                .bind("user_id", userId)
                .executeAndReturnGeneratedKeys()
                .mapTo(Int::class.java)
                .first()
        }
    }
}
