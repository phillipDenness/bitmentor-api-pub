package bitmentor.api.repository

import bitmentor.api.config.SharedJdbi
import bitmentor.api.repository.entity.TutorShortListDao
import io.ktor.util.*
import mu.KotlinLogging
import org.jdbi.v3.core.generic.GenericType


@KtorExperimentalAPI
object TutorShortListRepository {
    private val logger = KotlinLogging.logger {}

    private const val TUTOR_DAO_QUERY: String = """
                        SELECT 
                            t.id as tutorId,
                            u.profile_image_url,
                            u.display_name,
                            t.tagline
                        FROM 
                            bitmentor.tutor t
                        INNER JOIN bitmentor.user_account u ON t.user_id = u.id
                     """

    fun getTutorShortList(tutorIds: Set<Int>): Set<TutorShortListDao> {
        return SharedJdbi.jdbi().inTransaction<Set<TutorShortListDao>, Exception> { handle ->
            handle.createQuery(
                """$TUTOR_DAO_QUERY 
                    WHERE t.id = ANY (:short_list_tutors)
                    """
            )
                .bindByType("short_list_tutors", tutorIds, object : GenericType<List<Int>>() {})
                .mapTo(TutorShortListDao::class.java)
                .toSet()
        }
    }
}
