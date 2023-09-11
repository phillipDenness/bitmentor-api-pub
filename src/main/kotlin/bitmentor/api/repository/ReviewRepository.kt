package bitmentor.api.repository

import bitmentor.api.config.SharedJdbi
import bitmentor.api.exceptions.UserAuthorizationException
import bitmentor.api.model.lesson.LessonStates
import bitmentor.api.model.lesson.LessonStatusInsert
import bitmentor.api.model.notification.NotificationInsert
import bitmentor.api.model.notification.NotificationType
import bitmentor.api.model.review.ReviewInsert
import bitmentor.api.model.review.ReviewResource
import bitmentor.api.model.review.ReviewUpdate
import bitmentor.api.repository.entity.ReviewDao
import bitmentor.api.service.toResource
import io.ktor.features.*
import io.ktor.util.*
import mu.KotlinLogging
import org.jdbi.v3.core.Handle
import java.time.ZonedDateTime


@KtorExperimentalAPI
object ReviewRepository {
    private val logger = KotlinLogging.logger {}

    fun findReviewsByUserId(userId: Int, page: Int, size: Int): List<ReviewResource> {
        return SharedJdbi.jdbi().inTransaction<List<ReviewResource>, Exception> { handle ->
            handle.createQuery(
                    """SELECT * 
                FROM bitmentor.review 
                WHERE student_id = :student_id
                ORDER BY date_created DESC LIMIT $size OFFSET $page;"""
            )
                    .bind("student_id", userId)
                    .mapTo(ReviewDao::class.java)
                    .list()
                    .map { it.toResource() }
        }
    }

    fun findReviewsByTutorId(tutorId: Int, page: Int, size: Int): List<ReviewResource> {
        return SharedJdbi.jdbi().inTransaction<List<ReviewResource>, Exception> { handle ->
            findReviewsByTutorId(tutorId, page, size, handle).map { it.toResource() }
        }
    }

    fun findReviewsByTutorId(tutorId: Int, page: Int, size: Int, handle: Handle): List<ReviewDao> {
        return handle.createQuery(
            """SELECT * 
                FROM bitmentor.review 
                WHERE tutor_id = :tutor_id
                ORDER BY date_created DESC LIMIT $size OFFSET $page;"""
        )
            .bind("tutor_id", tutorId)
            .mapTo(ReviewDao::class.java)
            .list()
    }

    fun createReview(
            reviewInsert: ReviewInsert,
            studentId: Int
    ): Int {
        return SharedJdbi.jdbi().inTransaction<Int, Exception> { handle ->
            LessonRepository.getLessonById(reviewInsert.lessonId, handle)
                    ?.let { lessonResource ->
                        if (lessonResource.studentId != studentId) {
                            logger.warn { "user $studentId attempted to provide a review for ${reviewInsert.lessonId} and they are not authorized" }
                            throw UserAuthorizationException("User may not provide a review for lesson ${reviewInsert.lessonId}")
                        }

                        if (lessonResource.lessonStates.first().status != LessonStates.CONFIRMED) {
                            logger.warn { "user $studentId attempted to provide a review for ${reviewInsert.lessonId} before the tutor marked the lesson as complete" }
                            throw BadRequestException("You may not provide a review until the lesson is confirmed")
                        }

                        if (ZonedDateTime.now().isBefore(lessonResource.lessonEndDate)) {
                            logger.warn { "user $studentId attempted to provide a review for ${reviewInsert.lessonId} before the lesson finished" }
                            throw BadRequestException("You may not provide a review until the lesson has finished")
                        }
                        try {
                            handle.createUpdate(
                                    """INSERT INTO bitmentor.review(
                                    tutor_id, 
                                    topic_name, 
                                    overall_rating, 
                                    reason,
                                    student_id,
                                    student_display_name,
                                    date_created
                            )VALUES(
                                    :tutor_id, 
                                    :topic_name, 
                                    :overall_rating, 
                                    :reason,
                                    :student_id,
                                    :student_display_name,
                                    now())""")
                                    .bind("tutor_id", reviewInsert.tutorId)
                                    .bind("topic_name", lessonResource.topic.name)
                                    .bind("overall_rating", reviewInsert.overallRating)
                                    .bind("reason", reviewInsert.reason)
                                    .bind("student_id", studentId)
                                    .bind("student_display_name", lessonResource.studentDisplayName)
                                    .executeAndReturnGeneratedKeys("id")
                                    .mapTo(Int::class.java).first()
                                    .also {
                                        val rating = averageRatingByTutorId(reviewInsert.tutorId, handle)
                                        val count = countReviewsByTutorId(reviewInsert.tutorId, handle)
                                        TutorRepository.updateReview(
                                                tutorId = reviewInsert.tutorId,
                                                rating = rating,
                                                ratings = count,
                                                handle = handle
                                        )
                                        LessonRepository.createLessonStatus(
                                            lessonStatusInsert = LessonStatusInsert(
                                                status = LessonStates.REVIEWED
                                            ),
                                            userId = studentId,
                                            lessonId = lessonResource.id,
                                            handle = handle
                                        )
                                        NotificationRepository.createNotification(
                                                notificationInsert = NotificationInsert(
                                                        type = NotificationType.REVIEW_RECEIVED,
                                                        userId = lessonResource.tutorUserId
                                                ),
                                                handle = handle
                                        )
                                    }
                        } catch (e: Exception) {
                            logger.error { "An error has occurred saving the review $e" }
                            throw Exception("An error has occurred saving the review")
                        }
                    } ?: throw NotFoundException("Lesson not found")
        }
    }

    fun updateReview(reviewUpdate: ReviewUpdate, reviewId: Int, userId: Int) {
        try {
            return SharedJdbi.jdbi().inTransaction<Unit, Exception> { handle ->
                handle.createUpdate(
                        """UPDATE 
                                bitmentor.review
                            SET reason = :reason,
                                overall_rating = :overall_rating
                            WHERE id = :id AND student_id = :student_id;"""
                )
                        .bind("id", reviewId)
                        .bind("reason", reviewUpdate.reason)
                        .bind("overall_rating", reviewUpdate.overallRating)
                        .bind("student_id", userId)
                        .execute()
            }
        } catch (e: Exception) {
            logger.error { "An error has occurred updating the review ${e.message}" }
            throw Exception("An error has occurred updating the review")
        }
    }

    fun deleteReview(reviewId: Int, userId: Int) {
        try {
            return SharedJdbi.jdbi().inTransaction<Unit, Exception> { handle ->
                handle.createUpdate(
                        """DELETE FROM
                            bitmentor.review
                        WHERE id = :id AND student_id = :student_id;"""
                )
                        .bind("id", reviewId)
                        .bind("student_id", userId)
                        .execute()
            }
        } catch (e: Exception) {
            logger.error { "An error has occurred deleting the review ${e.message}" }
            throw Exception("An error has occurred deleting the review")
        }
    }

    fun countReviewsByTutorId(tutorId: Int): Int {
        return SharedJdbi.jdbi().inTransaction<Int, Exception> { handle ->
            countReviewsByTutorId(tutorId, handle)
        }
    }

    fun countReviewsByTutorId(tutorId: Int, handle: Handle): Int {
        return handle.createQuery(
                """SELECT COUNT(*) 
                    FROM bitmentor.review 
                    WHERE tutor_id = :tutor_id"""
        )
                .bind("tutor_id", tutorId)
                .mapTo(Int::class.java)
                .first()
    }

    fun countReviewsByUserId(userId: Int): Int {
        return SharedJdbi.jdbi().inTransaction<Int, Exception> { handle ->
            handle.createQuery(
                    """SELECT COUNT(*) 
                        FROM bitmentor.review 
                        WHERE student_id = :student_id"""
            )
                    .bind("student_id", userId)
                    .mapTo(Int::class.java)
                    .first()
        }
    }

    private fun averageRatingByTutorId(tutorId: Int, handle: Handle): Double {
        return handle.createQuery(
                """SELECT AVG(overall_rating) 
                    FROM bitmentor.review 
                    WHERE tutor_id = :tutor_id"""
        )
                .bind("tutor_id", tutorId)
                .mapTo(Double::class.java)
                .first()
    }
}
