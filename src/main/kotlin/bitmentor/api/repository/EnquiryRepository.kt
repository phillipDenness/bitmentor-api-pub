package bitmentor.api.repository

import bitmentor.api.config.SharedJdbi
import bitmentor.api.exceptions.TutorNotFoundException
import bitmentor.api.exceptions.UserAccountIncompleteException
import bitmentor.api.exceptions.UserNotFoundException
import bitmentor.api.model.enquiry.EnquiryResource
import bitmentor.api.model.message.MessageInsert
import bitmentor.api.repository.entity.EnquiryDao
import bitmentor.api.service.getUserPublic
import io.ktor.features.*
import io.ktor.util.*
import mu.KotlinLogging
import org.jdbi.v3.core.Handle
import java.time.ZonedDateTime

object EnquiryRepository {
    private val logger = KotlinLogging.logger {}

    fun countConfirmedLessons(id: Int): Int {
        return SharedJdbi.jdbi().inTransaction<Int, Exception> { handle ->
            handle.createQuery("""
                SELECT COUNT(*)
                FROM bitmentor.enquiry
                         JOIN bitmentor.lesson ON enquiry.id = lesson.enquiry_id
                         JOIN bitmentor.lesson_state ls ON lesson.id = ls.lesson_id
                WHERE enquiry.id = :id AND ls.status = 'CONFIRMED'
            """)
                    .bind("id", id)
                    .mapTo(Int::class.java)
                    .first()
        }
    }

    fun countByUserId(userId: Int, isTutor: Boolean): Int {
        return SharedJdbi.jdbi().inTransaction<Int, Exception> { handle ->
            var query = """SELECT COUNT(*)
                    FROM bitmentor.enquiry"""

            query += if (isTutor) {
                " WHERE tutor_user_id = :user_id"
            } else {
                " WHERE tutor_user_id = :user_id OR student_user_id = :user_id"
            }

            handle.createQuery(query)
                .bind("user_id", userId)
                .mapTo(Int::class.java)
                .first()
        }
    }

    @KtorExperimentalAPI
    fun create(
            tutorUserId: Int,
            studentUserId: Int,
            tutorId: Int,
            messageInsert: MessageInsert,
            senderId: Int,
            topicId: Int
    ): Int {
            return SharedJdbi.jdbi().inTransaction<Int, Exception> { handle ->
                TutorRepository.getTutor(tutorId, handle)?.takeIf { it.userId == tutorUserId }
                    ?: throw TutorNotFoundException()

                UserRepository.getUser(senderId, handle)?.displayName
                    ?: throw UserAccountIncompleteException()

                var enquiryId = getEnquiry(tutorUserId = tutorUserId, studentUserId = studentUserId, handle = handle)?.id
                try {
                    if (enquiryId == null) {
                        enquiryId = handle.createUpdate(
                            """INSERT INTO bitmentor.enquiry(
                                        tutor_user_id,
                                        student_user_id,
                                        tutor_id,
                                        topic_id,
                                        date_created
                                )VALUES(
                                        :tutor_user_id,
                                        :student_user_id,
                                        :tutor_id,
                                        :topic_id,
                                        now()
                                )"""
                        )
                            .bind("tutor_user_id", tutorUserId)
                            .bind("student_user_id", studentUserId)
                            .bind("tutor_id", tutorId)
                            .bind("topic_id", topicId)
                            .executeAndReturnGeneratedKeys("id")
                            .mapTo(Int::class.java).first()
                    }

                    MessageRepository.createMessage(
                            messageInsert = messageInsert.copy(enquiryId = enquiryId),
                            senderId = senderId,
                            handle = handle
                    )
                    enquiryId
                } catch (e: Exception) {
                    logger.error { "An error occurred saving the enquiry for $messageInsert ${e.message}" }
                    throw Exception("An error has occurred saving the enquiry")
                }
            }
    }

    @KtorExperimentalAPI
    fun getPagedByUserId(page: Int, size: Int, userId: Int, isTutor: Boolean): List<EnquiryResource> {
        return SharedJdbi.jdbi().inTransaction<List<EnquiryResource>, Exception> { handle ->

            var query = """SELECT 
                        id,
                        tutor_user_id,
                        tutor_id,
                        student_user_id,
                        topic_id,
                        date_created
                        FROM bitmentor.enquiry
                        """

            query += if (isTutor) {
                " WHERE tutor_user_id = :user_id"
            } else {
                " WHERE tutor_user_id = :user_id OR student_user_id = :user_id"
            }

            handle.createQuery(
                    "$query ORDER BY date_created DESC LIMIT $size OFFSET ${page * size};"
            )
                .bind("user_id", userId)
                .mapTo(EnquiryDao::class.java)
                .list()
                .map { it.enrichEnquiry(userId) }
        }
    }


    @KtorExperimentalAPI
    fun getById(id: Int, userId: Int): EnquiryResource? {
        return SharedJdbi.jdbi().inTransaction<EnquiryResource, Exception> { handle ->

            val query = """SELECT 
                        id,
                        tutor_user_id,
                        tutor_id,
                        student_user_id,
                        topic_id,
                        date_created
                        FROM bitmentor.enquiry
                        WHERE id = :id;"""

            handle.createQuery(
                    query
            )
                    .bind("id", id)
                    .mapTo(EnquiryDao::class.java)
                    .firstOrNull()?.enrichEnquiry(userId)
        }
    }

    private fun getEnquiry(
        tutorUserId: Int,
        studentUserId: Int,
        handle: Handle
    ): EnquiryDao? {
        return handle.createQuery(
            """SELECT *
                FROM bitmentor.enquiry
                WHERE tutor_user_id = :tutor_user_id AND student_user_id = :student_user_id"""
        )
            .bind("tutor_user_id", tutorUserId)
            .bind("student_user_id", studentUserId)
            .mapTo(EnquiryDao::class.java)
            .firstOrNull()
    }

    @KtorExperimentalAPI
    private fun EnquiryDao.enrichEnquiry(userId: Int): EnquiryResource {
        val tutorUser = getUserPublic(tutorUserId)
            ?: throw UserNotFoundException()
        val studentUser = getUserPublic(studentUserId)
            ?: throw UserNotFoundException()


        val messageResource = MessageRepository.getMessagesByEnquiryId(page = 0, size = 1, enquiryId = id, userId = userId)
        if (messageResource.isEmpty()) {
            throw NotFoundException("No messages were found relating to enquiry id $id.")
        }

        return EnquiryResource(
            id = id,
            tutorDisplayName = tutorUser.displayName!!,
            studentDisplayName = studentUser.displayName!!,
            studentUser = studentUser,
            tutorUser = tutorUser,
            lastMessage = messageResource[0],
            recipientDisplayName = tutorUser.displayName.takeIf { userId == studentUserId } ?: studentUser.displayName,
            recipientProfileImage = tutorUser.profileImageUrl.takeIf { userId == studentUserId } ?: studentUser.profileImageUrl,
            upcomingLessons = LessonRepository.getLessonsByEnquiryId(
                    userId = userId,
                    enquiryId = id,
                    start = ZonedDateTime.now(),
                    end = ZonedDateTime.now().plusDays(90)
            ),
            topic = TopicRepository.getTopicById(topicId) ?: throw Exception("Topic not found id: $topicId"),
            tutorId = this.tutorId
        )
    }
}
