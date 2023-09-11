package bitmentor.api.service

import bitmentor.api.config.Properties
import bitmentor.api.email.EmailTemplate
import bitmentor.api.exceptions.LessonNotFoundException
import bitmentor.api.exceptions.TutorNotFoundException
import bitmentor.api.exceptions.UserNotFoundException
import bitmentor.api.model.lesson.*
import bitmentor.api.model.reminder.ReminderInsert
import bitmentor.api.model.reminder.ReminderTypes
import bitmentor.api.model.reminder.payloads.RemindAvailablePayout
import bitmentor.api.model.reminder.payloads.RemindLesson
import bitmentor.api.model.reminder.payloads.RemindOpenMeeting
import bitmentor.api.model.reminder.payloads.RemindReview
import bitmentor.api.repository.LessonRepository
import bitmentor.api.repository.LessonRepository.deleteLessonQuery
import bitmentor.api.repository.TutorRepository
import bitmentor.api.repository.UserRepository
import io.ktor.features.*
import io.ktor.util.*
import mu.KotlinLogging
import org.jdbi.v3.core.Handle
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

private val logger = KotlinLogging.logger {}

@KtorExperimentalAPI
fun LessonInsert.create(userId: Int): Int {
    validate(
        lessonDate = lessonDate,
        endDate = lessonEndDate,
        cost = cost
    )

    TutorRepository.findTutorByUserId(userId) ?: throw TutorNotFoundException()
    return LessonRepository.createLesson(this, userId)
}

@KtorExperimentalAPI
fun LessonUpdate.update(userId: Int, lessonId: Int): LessonResource {
    validate(
        lessonDate = lessonDate,
        endDate = lessonEndDate,
        cost = cost
    )

    logger.info { "Calling repo with lesson id: $lessonId $this" }
    LessonRepository.updateLesson(lessonUpdate = this, userId = userId, lessonId = lessonId)
    return LessonRepository.getLessonById(lessonId)!!
}

@KtorExperimentalAPI
fun LessonStatusInsert.create(userId: Int, lessonId: Int): Int {
    if (this.status == LessonStates.PENDING) {
        TutorRepository.findTutorByUserId(userId)
                ?: throw TutorNotFoundException().also { logger.warn { "$userId attempted to create a lesson but they are not a tutor." } }
    }
    if (this.status == LessonStates.CONFIRMED) {
        throw BadRequestException("The lesson can only be confirmed after payment")
    }
    return LessonRepository.createLessonStatus(
            lessonStatusInsert = this,
            userId = userId,
            lessonId = lessonId
    )
}

@KtorExperimentalAPI
fun deleteLesson(lessonId: Int, userId: Int) {
    deleteLessonQuery(lessonId = lessonId, userId = userId)
}

@KtorExperimentalAPI
fun getLessonsByDate(userId: Int, start: ZonedDateTime, end: ZonedDateTime): LessonsResource {
    val totalLessons = LessonRepository.countLessonsByDate(userId = userId, start = start, end = end)
    val lessons = LessonRepository.getLessonsByDate(
            userId = userId,
            start = start,
            end = end
    )

    return LessonsResource(
            lessons = lessons,
            totalLessons = totalLessons
    )
}

@KtorExperimentalAPI
fun getLessonsByEnquiryId(userId: Int, enquiryId: Int, start: ZonedDateTime, end: ZonedDateTime): LessonsResource {
    val totalLessons = LessonRepository.countLessonsByEnquiryId(userId = userId, enquiryId = enquiryId, start = start, end = end)
    val lessons = LessonRepository.getLessonsByEnquiryId(
            userId = userId,
            enquiryId = enquiryId,
            start = start,
            end = end
    )

    return LessonsResource(
            lessons = lessons,
            totalLessons = totalLessons
    )
}

@KtorExperimentalAPI
fun getLessonsById(lessonId: Int): LessonResource? {
    return LessonRepository.getLessonById(lessonId)
}

@KtorExperimentalAPI
fun generateReminder(lessonState: LessonStates, lessonId: Int, handle: Handle): List<ReminderInsert?> {
    if (lessonState == LessonStates.CONFIRMED) {
        logger.info { "Generating reminder to OPEN_MEETING for lessonId: $lessonId" }
        val lesson = LessonRepository.getLessonById(lessonId, handle = handle) ?: throw LessonNotFoundException()
        val meetingCreate = ReminderInsert(
                reminderPayload = RemindOpenMeeting(
                        lessonResource = lesson,
                        duration = zonedDateTimeDifference(lesson.lessonDate, lesson.lessonEndDate, ChronoUnit.MINUTES) + 30
                ),
                reminderType = ReminderTypes.OPEN_MEETING,
                triggerDate = lesson.lessonDate.minusMinutes(15)
        )
        val reminderLesson = ReminderInsert(
                reminderPayload = RemindLesson(
                        studentId = lesson.studentId,
                        tutorUserId = lesson.tutorUserId,
                        lessonResource = lesson
                ),
                reminderType = ReminderTypes.LESSON_REMINDER,
                triggerDate = lesson.lessonDate.minusMinutes(60)
        )
        val reviewLesson = ReminderInsert(
                reminderPayload = RemindReview(
                        studentId = lesson.studentId,
                        tutorUserId = lesson.tutorUserId,
                        lessonResource = lesson
                ),
                reminderType = ReminderTypes.REVIEW_REMINDER,
                triggerDate = lesson.lessonEndDate.plusMinutes(60)
        )

        val payoutReminder = getOrderByLessonId(lessonId)?.let {order ->
            order.paymentId?.let {
                ReminderInsert(
                        reminderPayload = RemindAvailablePayout(
                            orderResource = order
                        ),
                        reminderType = ReminderTypes.AVAILABLE_PAYOUT,
                        triggerDate = lesson.lessonEndDate.plusDays(1)
                )
            }
        }

        return listOf(meetingCreate, reminderLesson, reviewLesson, payoutReminder)
    }
    return emptyList()
}

fun zonedDateTimeDifference(d1: ZonedDateTime?, d2: ZonedDateTime?, unit: ChronoUnit): Int {
    return unit.between(d1, d2).toInt()
}

fun LessonStates.isEditable(): Boolean {
    return this == LessonStates.RESCHEDULED || this == LessonStates.PENDING
}

fun invalidStatusChange(lessonResource: LessonResource, targetState: LessonStates): Boolean {
    val currentState = lessonResource.lessonStates.first().status

    return when (targetState) {
        LessonStates.REJECTED -> {
            currentState == LessonStates.CANCELLED ||
            currentState == LessonStates.REJECTED ||
            ZonedDateTime.now().isAfter(lessonResource.lessonDate)
        }
        LessonStates.CANCELLED -> {
            currentState == LessonStates.CANCELLED ||
            currentState == LessonStates.REJECTED ||
            ZonedDateTime.now().isAfter(lessonResource.lessonDate)
        }
        LessonStates.CONFIRMED -> {
            currentState == LessonStates.CANCELLED ||
            currentState == LessonStates.CONFIRMED ||
            currentState == LessonStates.REJECTED
        }
        LessonStates.REVIEWED -> {
            currentState == LessonStates.CANCELLED ||
            currentState == LessonStates.REJECTED ||
            currentState == LessonStates.PENDING ||
            currentState == LessonStates.REVIEWED
        }
        else -> {
            true
        }
    }
}


fun sendLessonCreatedEmail(recipientId: Int, senderId: Int, enquiryId: Int, lessonId: Int) {
    val senderDetails = UserRepository.getUser(senderId) ?: throw UserNotFoundException()
            .also { logger.error { "Could not find user id $senderId when sending lesson alert email" } }

    UserRepository.getUser(recipientId)?.let { userAccount ->
        EmailTemplate(
                message = """
    <p>Hi ${userAccount.firstName},</p>
    <p>We are contacting you because ${senderDetails.displayName} has scheduled a lesson.</p>
    <b>To confirm the lesson please go to <a href="${Properties.clientUrl}/lessons?enquiryId=${enquiryId}&lessonId=${lessonId}">lessons</a></b>
    """,
                subject = "${senderDetails.displayName} has scheduled a lesson on Bitmentor"
        ).send(userAccount.email)
    }
}

fun sendLessonUpdatedEmail(recipientId: Int, senderId: Int, lesson: LessonResource) {
    val senderDetails = UserRepository.getUser(senderId) ?: throw UserNotFoundException()
            .also { logger.error { "Could not find user id $senderId when sending lesson update email" } }

    UserRepository.getUser(recipientId)?.let { userAccount ->
        EmailTemplate(
                message = """
    <p>Hi ${userAccount.firstName},</p>
    <p>We are contacting you because there has been an update with your lesson with ${senderDetails.displayName}.</p>
    <p>To view the lesson please go to <a href="${Properties.clientUrl}/lessons?enquiryId=${lesson.enquiryId}&lessonId=${lesson.id}">View lesson</a></p>
    """,
                subject = "Your lesson with ${senderDetails.displayName}"
        ).send(userAccount.email)
    }
}

@KtorExperimentalAPI
private fun validate(lessonDate: ZonedDateTime, endDate: ZonedDateTime, cost: Double) {
    if (lessonDate.isBefore(ZonedDateTime.now()) || lessonDate.isEqual(ZonedDateTime.now())) {
        throw BadRequestException("Lesson date must be in the future")
    }

    if (lessonDate.isAfter(endDate)) {
        throw BadRequestException("Lesson start date must be before lesson end date")
    }
    if (cost < Properties.squareUpMinPayment) {
        throw BadRequestException("Lesson cost must be greater than £${Properties.squareUpMinPayment}")
    }
}