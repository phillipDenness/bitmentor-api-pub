package bitmentor.api.repository

import bitmentor.api.config.SharedJdbi
import bitmentor.api.exceptions.InvalidLessonStatusChange
import bitmentor.api.exceptions.TopicNotFoundException
import bitmentor.api.exceptions.UserAuthorizationException
import bitmentor.api.exceptions.UserNotFoundException
import bitmentor.api.model.lesson.*
import bitmentor.api.model.notification.NotificationInsert
import bitmentor.api.model.reminder.ReminderTypes
import bitmentor.api.repository.entity.LessonDao
import bitmentor.api.repository.entity.LessonStateDao
import bitmentor.api.service.*
import io.ktor.features.*
import io.ktor.util.*
import mu.KotlinLogging
import org.jdbi.v3.core.Handle
import java.time.ZonedDateTime


@KtorExperimentalAPI
object LessonRepository {
    private val logger = KotlinLogging.logger {}

    private const val LESSON_DAO_QUERY: String = """
                        SELECT 
                            id,
                            tutor_user_id, 
                            student_id, 
                            date_lesson,
                            end_date_lesson,
                            date_created,
                            cost,
                            topic_id,
                            enquiry_id,
                            promo_used
                        FROM 
                            bitmentor.lesson"""

    fun getLessonById(lessonId: Int, handle: Handle): LessonResource? {
        val lessonDao = handle.createQuery(
                """$LESSON_DAO_QUERY
            WHERE id = :id;"""
        )
                .bind("id", lessonId)
                .mapTo(LessonDao::class.java)
                .firstOrNull()

        return lessonDao?.toResource(handle)
    }

    fun getLessonById(lessonId: Int): LessonResource? {
        return SharedJdbi.jdbi().inTransaction<LessonResource?, Exception> { handle ->
            getLessonById(lessonId, handle)
        }
    }

    fun countLessonsByDate(userId: Int, start: ZonedDateTime, end: ZonedDateTime): Int {
        return SharedJdbi.jdbi().inTransaction<Int, Exception> { handle ->
            handle.createQuery(
                    """SELECT COUNT(*) 
                        FROM bitmentor.lesson 
                        WHERE (tutor_user_id = :user_id OR student_id = :user_id)
                        AND (date_lesson > :start AND end_date_lesson < :end)"""
            )
                    .bind("user_id", userId)
                    .bind("start", start)
                    .bind("end", end)
                    .mapTo(Int::class.java)
                    .first()
        }
    }


    fun getLessonsByDate(userId: Int, start: ZonedDateTime, end: ZonedDateTime): List<LessonResource> {
        return SharedJdbi.jdbi().inTransaction<List<LessonResource>, Exception> { handle ->
            getLessonsByDate(userId = userId, handle = handle, start = start, end = end)
        }
    }

    private fun getLessonsByDate(userId: Int, handle: Handle, start: ZonedDateTime, end: ZonedDateTime): List<LessonResource> {
        val lessonDaos = handle.createQuery(
                """$LESSON_DAO_QUERY
                WHERE (tutor_user_id = :user_id OR student_id = :user_id) 
                AND (date_lesson > :start AND end_date_lesson < :end)
                ORDER BY date_lesson"""
        )
                .bind("user_id", userId)
                .bind("start", start)
                .bind("end", end)
                .mapTo(LessonDao::class.java)
                .list()

        return lessonDaos.map {
            it.toResource(handle)
        }
    }

    fun getLessonsByEnquiryId(userId: Int, enquiryId: Int, start: ZonedDateTime, end: ZonedDateTime): List<LessonResource> {
        return SharedJdbi.jdbi().inTransaction<List<LessonResource>, Exception> { handle ->
            getLessonsByEnquiryId(userId = userId, handle = handle, enquiryId = enquiryId, start = start, end = end)
        }
    }

    private fun getLessonsByEnquiryId(userId: Int, handle: Handle, enquiryId: Int, start: ZonedDateTime, end: ZonedDateTime): List<LessonResource> {
        logger.info { "Fetching lessons for userId: $userId and enquiryId: $enquiryId between $start and $end" }
        val lessonDaos = handle.createQuery(
                """$LESSON_DAO_QUERY
                WHERE (tutor_user_id = :user_id OR student_id = :user_id) AND enquiry_id = :enquiry_id
                AND (date_lesson > :start AND end_date_lesson < :end)
                ORDER BY date_lesson"""
        )
            .bind("user_id", userId)
            .bind("enquiry_id", enquiryId)
            .bind("start", start)
            .bind("end", end)
            .mapTo(LessonDao::class.java)
            .list()

        return lessonDaos.map {
            it.toResource(handle)
        }
    }

    fun countLessonsByEnquiryId(userId: Int, enquiryId: Int, start: ZonedDateTime, end: ZonedDateTime): Int {
        return SharedJdbi.jdbi().inTransaction<Int, Exception> { handle ->
            handle.createQuery(
                    """SELECT COUNT(*) 
                        FROM bitmentor.lesson 
                        WHERE (tutor_user_id = :user_id OR student_id = :user_id) 
                        AND enquiry_id = :enquiry_id
                        AND (date_lesson > :start AND date_lesson < :end)
                        """
            )
                    .bind("user_id", userId)
                    .bind("enquiry_id", enquiryId)
                    .bind("start", start)
                    .bind("end", end)
                    .mapTo(Int::class.java)
                    .first()
        }
    }

    private fun getLessonStatesByLessonId(lessonId: Int, handle: Handle): List<LessonStateDao> {
        return handle.createQuery(
                """SELECT * FROM bitmentor.lesson_state 
                    WHERE lesson_id = :lesson_id
                    ORDER BY date_created DESC"""
        )
                .bind("lesson_id", lessonId)
                .mapTo(LessonStateDao::class.java)
                .list()
    }

    fun createLesson(
        lessonInsert: LessonInsert,
        tutorUserId: Int
    ): Int {
        return SharedJdbi.jdbi().inTransaction<Int, Exception> { handle ->
            try {
                handle.createUpdate(
                    """INSERT INTO bitmentor.lesson(
                                    tutor_user_id,
                                    date_lesson,
                                    end_date_lesson,
                                    cost,
                                    student_id,
                                    topic_id,
                                    enquiry_id,
                                    date_created,
                                    reminder_ids
                            )VALUES(
                                    :tutor_user_id,
                                    :date_lesson,
                                    :end_date_lesson,
                                    :cost,
                                    :student_id,
                                    :topic_id,
                                    :enquiry_id,
                                    now(),
                                    ARRAY[]::integer[]
                            )"""
                )
                    .bind("tutor_user_id", tutorUserId)
                    .bind("date_lesson", lessonInsert.lessonDate)
                    .bind("end_date_lesson", lessonInsert.lessonEndDate)
                    .bind("cost", lessonInsert.cost)
                    .bind("student_id", lessonInsert.studentId)
                    .bind("topic_id", lessonInsert.topicId)
                    .bind("enquiry_id", lessonInsert.enquiryId)
                    .executeAndReturnGeneratedKeys("id")
                    .mapTo(Int::class.java)
                    .first()
                    .also {
                        createLessonState(
                            lessonId = it,
                            lessonState = LessonStates.PENDING,
                            handle = handle,
                            notifyUserId = LessonStates.PENDING.notifyUser(studentUserId = lessonInsert.studentId, tutorUserId = tutorUserId),
                            userId = tutorUserId
                        )
                        sendLessonCreatedEmail(
                                recipientId = lessonInsert.studentId,
                                senderId = tutorUserId,
                                enquiryId = lessonInsert.enquiryId,
                                lessonId = it
                        )
                    }
            } catch (e: Exception) {
                logger.error { "An error has occurred saving the lesson ${e.message}" }
                throw Exception("An error has occurred saving the lesson")
            }
        }
    }

    fun updateLesson(lessonUpdate: LessonUpdate, userId: Int, lessonId: Int) {
        return SharedJdbi.jdbi().inTransaction<Unit, Exception> { handle ->
            try {
                logger.info { "Processing lesson update request $lessonUpdate" }
                getLessonById(lessonId, handle)?.let { lessonResource ->
                    if (lessonResource.tutorUserId != userId) {
                        logger.warn { "User $userId attempted to update lesson $lessonResource" }
                        throw UserAuthorizationException("User is not authorised to update lessons")
                    }
                    if (lessonResource.lessonStates.first().status.isEditable()) {
                        handle.createUpdate("""
                            UPDATE bitmentor.lesson
                            SET date_lesson = :date_lesson,
                                end_date_lesson = :end_date_lesson,
                                cost = :cost,
                                topic_id = :topic_id
                            WHERE id = :id;
                        """)
                                .bind("date_lesson", lessonUpdate.lessonDate)
                                .bind("end_date_lesson", lessonUpdate.lessonEndDate)
                                .bind("cost", lessonUpdate.cost)
                                .bind("topic_id", lessonUpdate.topicId)
                                .bind("id", lessonId)
                                .execute()
                                .also {
                                    createLessonState(
                                        lessonState = LessonStates.RESCHEDULED,
                                        lessonId = lessonId,
                                        handle = handle,
                                        notifyUserId = LessonStates.RESCHEDULED.notifyUser(studentUserId = lessonResource.studentId, tutorUserId = lessonResource.tutorUserId),
                                        userId = userId
                                    )
                                    getLessonById(lessonId)?.let { lesson ->
                                        sendLessonUpdatedEmail(
                                                recipientId = lessonResource.studentId,
                                                senderId = userId,
                                                lesson = lesson
                                        )
                                    }
                                }
                    } else {
                        throw BadRequestException("You cannot edit a lesson not in pending state")
                                .also { logger.warn { "User $userId attempted to edit a lesson in non-pending state" } }
                    }
                } ?: throw NotFoundException()
                        .also { logger.warn {"$lessonId was not found during $lessonUpdate"} }
            } catch (e: BadRequestException) {
                throw e
            } catch (e: NotFoundException) {
                throw e
            } catch (e: Exception) {
                logger.error { "An exception occurred updating the lesson ${e.message}" }
                throw Exception("An exception occurred updating the lesson")
            }
        }
    }

    fun deleteLessonQuery(lessonId: Int, userId: Int) {
        return SharedJdbi.jdbi().inTransaction<Unit, Exception> { handle ->

            getLessonById(
                    lessonId = lessonId,
                    handle = handle
            )?.let {
                if (it.tutorUserId != userId) {
                    logger.warn { "User $userId attempted to delete lesson $it" }
                    throw UserAuthorizationException("Only the tutor may delete lessons")
                }
                if (it.lessonStates.first().status == LessonStates.PENDING) {
                    handle.createUpdate("""
                                DELETE FROM bitmentor.lesson
                                WHERE id = :lesson_id
                            """)
                            .bind("lesson_id", lessonId)
                            .execute()
                } else {
                    logger.warn { "user $userId attempted to delete lesson $lessonId which is not in pending state" }
                    throw BadRequestException("Only pending lessons may be deleted")
                }
            }
        }
    }

    fun createLessonStatus(lessonStatusInsert: LessonStatusInsert, lessonId: Int, userId: Int, handle: Handle): Int {
        return getLessonById(lessonId = lessonId, handle = handle)?.let {lesson ->

            if (lessonStatusInsert.status.isTutor && lesson.tutorUserId != userId) {
                logger.warn { "User $userId attempted to create lesson update $lessonStatusInsert for lesson $lessonId" }
                throw UserAuthorizationException("")
            }
            if (!lessonStatusInsert.status.isTutor && lesson.studentId != userId) {
                logger.warn { "User $userId attempted to create lesson update $lessonStatusInsert for lesson $lessonId" }
                throw UserAuthorizationException("")
            }

            if (invalidStatusChange(lesson, lessonStatusInsert.status)) {
                throw InvalidLessonStatusChange().also { logger.warn { "user $userId attempted to make an invalid status change insert: $lessonStatusInsert, lesson: $it" } }
            }

            createLessonState(
                lessonState = lessonStatusInsert.status,
                lessonId = lessonId,
                handle = handle,
                notifyUserId = lessonStatusInsert.status.notifyUser(studentUserId = lesson.studentId, tutorUserId = lesson.tutorUserId),
                userId = userId
            ).also { _ ->
                sendLessonUpdatedEmail(
                        recipientId = lesson.tutorUserId
                                .takeIf { lesson.tutorUserId != userId } ?: lesson.studentId,
                        senderId = userId,
                        lesson = lesson
                )
            }

        } ?: throw NotFoundException()
            .also { logger.warn {"$lessonId was not found during status update $lessonStatusInsert"} }
    }


    fun createLessonStatus(lessonStatusInsert: LessonStatusInsert, lessonId: Int, userId: Int): Int {
        return SharedJdbi.jdbi().inTransaction<Int, Exception> { handle ->
            createLessonStatus(lessonStatusInsert, lessonId, userId, handle)
        }
    }

    fun deleteReminder(reminderId: Int, lessonId: Int, handle: Handle) {
        handle.createUpdate("""
            UPDATE bitmentor.lesson
            SET reminder_ids = (select array_remove(reminder_ids, :reminder_id) from bitmentor.lesson where id = :id)
            WHERE id = :id;
        """)
                .bind("reminder_id", reminderId)
                .bind("id", lessonId)
                .execute()
    }

    fun deleteReminder(reminderId: Int, lessonId: Int) {
        SharedJdbi.jdbi().inTransaction<Unit, Exception> { handle ->
            deleteReminder(reminderId, lessonId, handle)
        }
    }

    fun saveLessonPromo(lessonId: Int, code: String) {
        return SharedJdbi.jdbi().inTransaction<Unit, Exception> { handle ->
            handle.createUpdate("""
                UPDATE bitmentor.lesson
                SET promo_used = :promo_used
                WHERE id = :id;
            """)
                .bind("promo_used", code)
                .bind("id", lessonId)
                .execute()
        }
    }

    private fun createLessonState(
        lessonState: LessonStates,
        lessonId: Int,
        handle: Handle,
        notifyUserId: Int,
        userId: Int
    ): Int {
        if (lessonState == LessonStates.CANCELLED || lessonState == LessonStates.REJECTED) {
            getLessonById(lessonId, handle)?.let {
                logger.info { "Checking if lesson requires a refund. Status: ${it.lessonStates.first().status}" }
                if (it.lessonStates.first().status == LessonStates.CONFIRMED) {
                    logger.info { "User cancelled lesson $lessonId and it is confirmed. Proceeding with refund request" }
                    refundOrder(
                        lessonId = lessonId,
                        handle = handle,
                        userId = userId
                    )
                }
            }
            deleteReminderByType(
                lessonId = lessonId,
                types = listOf(ReminderTypes.OPEN_MEETING, ReminderTypes.LESSON_REMINDER, ReminderTypes.REVIEW_REMINDER, ReminderTypes.AVAILABLE_PAYOUT),
                handle = handle
            )
        }

        return handle.createUpdate(
                """INSERT INTO bitmentor.lesson_state(
                                    status,
                                    lesson_id,
                                    date_created
                            )VALUES(
                                    :status,
                                    :lesson_id,
                                    now()
                            )"""
        )
                .bind("status", lessonState)
                .bind("lesson_id", lessonId)
                .executeAndReturnGeneratedKeys("id")
                .mapTo(Int::class.java)
                .first().also {
                    lessonState.toNotification()?.let { notification ->
                        NotificationRepository.createNotification(
                            notificationInsert = NotificationInsert(
                                userId = notifyUserId,
                                type = notification
                            ),
                            handle = handle
                        )
                    }

                    generateReminder(lessonState, lessonId, handle).map {
                        it?.let {
                            ReminderRepository.createReminder(
                                    reminderInsert = it,
                                    handle = handle
                            ).also { reminderId ->
                                handle.createUpdate("""
                                update bitmentor.lesson
                                set reminder_ids = array_append(reminder_ids, :reminder_id)
                                where id = :lesson_id
                            """)
                                        .bind("lesson_id", lessonId)
                                        .bind("reminder_id", reminderId)
                                        .execute()
                            }
                        }
                    }
                }
    }

    private fun deleteReminderByType(lessonId: Int, types: List<ReminderTypes>, handle: Handle) {
        val reminderIds = handle.createQuery("""
                SELECT unnest(reminder_ids) as reminder_ids FROM bitmentor.lesson
                WHERE id = :id;
            """)
                .bind("id", lessonId)
                .mapTo(Int::class.java)
                .list()

        types.forEach { type ->
            ReminderRepository.deleteReminderByReminderType(reminderIds, type, handle)?.let {
                deleteReminder(reminderId = it, lessonId = lessonId, handle = handle)
            }
        }
    }

    private fun LessonDao.toResource(handle: Handle): LessonResource {
        val tutorUserResource = UserRepository.getUser(userId = tutorUserId, handle = handle)
            ?: throw UserNotFoundException()
        return LessonResource(
                id = id,
                enquiryId = enquiryId,
                lessonStates = getLessonStatesByLessonId(lessonId = id, handle = handle),
                cost = cost,
                lessonDate = dateLesson,
                lessonEndDate = endDateLesson,
                tutorUserId = tutorUserId,
                studentId = studentId,
                tutorDisplayName = tutorUserResource.displayName!!,
                studentDisplayName = UserRepository.getUser(userId = studentId, handle = handle)!!.displayName
                        ?: throw UserNotFoundException(),
            topic = TopicRepository.getTopicById(topicId, handle) ?: throw TopicNotFoundException(),
            tutorId = TutorRepository.findTutorByUserId(userId = tutorUserId, handle = handle)!!.id,
            promotion = getAllPromotions().find { promo -> promo.code == promoUsed }
        )
    }

    private fun LessonStates.notifyUser(studentUserId: Int, tutorUserId: Int): Int {
        return studentUserId.takeIf { this.isTutor } ?: tutorUserId
    }
}
