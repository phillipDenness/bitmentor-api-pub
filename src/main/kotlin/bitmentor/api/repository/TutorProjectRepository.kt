package bitmentor.api.repository

import bitmentor.api.config.SharedJdbi
import bitmentor.api.model.project.TutorProjectInsert
import bitmentor.api.repository.entity.TutorProjectDao
import mu.KotlinLogging
import org.jdbi.v3.core.Handle

object TutorProjectRepository {
    private val logger = KotlinLogging.logger {}

    private const val TUTOR_PROJECT_DAO_QUERY = """
                        SELECT
                            id,
                            tutor_id,
                            title,
                            description,
                            link,
                            last_modified
                        FROM bitmentor.tutor_project"""

    fun findByTutorId(tutorId: Int): List<TutorProjectDao> {
        return  SharedJdbi.jdbi().inTransaction<List<TutorProjectDao>, Exception> { handle ->
            handle.createQuery(
                """$TUTOR_PROJECT_DAO_QUERY WHERE tutor_id = :tutor_id"""
            )
                .bind("tutor_id", tutorId)
                .mapTo(TutorProjectDao::class.java)
                .list()
        }
    }

    fun create(
        projectInsert: TutorProjectInsert,
        tutorId: Int,
        handle: Handle
    ): Int {
        return try {
            handle.createUpdate(
                    """INSERT INTO bitmentor.tutor_project(
                                tutor_id, 
                                title, 
                                description,
                                link,
                                last_modified
                        )VALUES(
                                :tutor_id, 
                                :title, 
                                :description,
                                :link,
                                now()
                        )"""
            )
                .bind("tutor_id", tutorId)
                .bind("title", projectInsert.title)
                .bind("description", projectInsert.description)
                .bind("link", projectInsert.link)
                .executeAndReturnGeneratedKeys("id")
                .mapTo(Int::class.java).first()
        } catch (e: Exception) {
            logger.error { "An error has occurred saving the tutor project: ${e.message}" }
            throw Exception("An error has occurred saving the tutor project")
        }
    }

    fun delete(tutorProjectId: Int, handle: Handle) {
        try {
                handle.createUpdate(
                    """DELETE FROM
                                bitmentor.tutor_project
                            WHERE id = :id;"""
                )
                    .bind("id", tutorProjectId)
                    .execute()

        } catch (e: Exception) {
            logger.error { "An error has occurred deleting the tutor project: ${e.message}" }
            throw Exception("An error has occurred deleting the tutor project")
        }
    }

    fun update(tutorId: Int, projects: List<TutorProjectInsert>, handle: Handle) {
        try {
            handle.createUpdate("""
                DELETE FROM bitmentor.tutor_project 
                WHERE tutor_id = :tutor_id
                """)
                .bind("tutor_id", tutorId)
                .execute()

            projects.forEach { project ->
                create(
                    projectInsert = project,
                    handle = handle,
                    tutorId = tutorId
                )
            }
        }catch (e: Exception) {
            logger.error { "An error has occurred updating the tutor: $tutorId projects: ${e.message}" }
            throw Exception("An error has occurred deleting the tutor project")
        }
    }
}
