package bitmentor.api.service

import bitmentor.api.config.GenericObjectMapper
import bitmentor.api.config.Properties
import bitmentor.api.email.EmailTemplate
import bitmentor.api.model.lesson.LessonResource
import bitmentor.api.model.reminder.ReminderResource
import bitmentor.api.model.reminder.ReminderType
import bitmentor.api.model.reminder.ReminderTypes
import bitmentor.api.model.reminder.payloads.RemindAvailablePayout
import bitmentor.api.model.reminder.payloads.RemindLesson
import bitmentor.api.model.reminder.payloads.RemindOpenMeeting
import bitmentor.api.model.reminder.payloads.RemindReview
import bitmentor.api.repository.LessonRepository
import bitmentor.api.repository.ReminderRepository
import bitmentor.api.repository.UserRepository
import bitmentor.api.repository.entity.ReminderDao
import io.ktor.util.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import java.time.format.DateTimeFormatter
import kotlin.concurrent.timer

private val logger = KotlinLogging.logger {}

@KtorExperimentalAPI
fun initReminderHandler() {
    logger.trace { "Starting notification scheduler task with delay ${Properties.notificationScheduleDelay}" }

    timer("scheduleNotificationTask", false, Properties.notificationScheduleDelay, Properties.notificationScheduleDelay) {
        GlobalScope.launch {
            scheduleNotificationTask()
        }
    }
}

@KtorExperimentalAPI
suspend fun scheduleNotificationTask() {
    logger.trace { "Schedule notification task" }
    try {
        ReminderRepository.findDueReminders().forEach { reminderDao ->
            try {
                val reminder = reminderDao.toReminder()
                logger.info { "Processing reminder resource: ${reminder.id}, ${reminder.reminderPayload}" }

                when (reminder.reminderType) {
                    ReminderTypes.OPEN_MEETING -> {
                        val reminderPayload = reminder.reminderPayload as RemindOpenMeeting
                        reminderPayload.handle(reminder.id)
                    }
                    ReminderTypes.LESSON_REMINDER -> {
                        val reminderPayload = reminder.reminderPayload as RemindLesson
                        reminderPayload.handle(reminder.id)
                    }
                    ReminderTypes.REVIEW_REMINDER -> {
                        val reminderPayload = reminder.reminderPayload as RemindReview
                        reminderPayload.handle(reminder.id)
                    }
                    ReminderTypes.AVAILABLE_PAYOUT -> {
                        val reminderPayload = reminder.reminderPayload as RemindAvailablePayout
                        reminderPayload.handle(reminder.id)
                    }
                }
                ReminderRepository.deleteReminder(reminder.id)
            } catch (ex: Exception) {
                logger.error { "Exception during reminder $reminderDao. ex: $ex" }
                ReminderRepository.updateError(reminderDao, ex.message)
            }
        }
    } catch (error: Exception) {
        logger.error { "Exception occurred: $error" }
    }
}

@KtorExperimentalAPI
suspend fun RemindOpenMeeting.handle(id: Int) {
    logger.info { "Preparing to open BigBlueMeeting lesson: $this" }
    val createdMeeting = this.createMeeting()

    val studentUrl = this.createMeetingJoinLinks(userName = this.lessonResource.studentDisplayName, password = createdMeeting.moderatorPW!!)
    val tutorUrl = this.createMeetingJoinLinks(userName = this.lessonResource.tutorDisplayName, password = createdMeeting.moderatorPW)
    sendJoinEmail(lessonResource.tutorUserId, tutorUrl, lessonResource)
    sendJoinEmail(lessonResource.studentId, studentUrl, lessonResource)

    LessonRepository.deleteReminder(reminderId = id, lessonId = lessonResource.id)
}

@KtorExperimentalAPI
fun RemindLesson.handle(id: Int) {
    logger.info { "Sending lesson reminder: $this" }
    sendUpcomingLessonEmail(tutorUserId, lessonResource)
    sendUpcomingLessonEmail(studentId, lessonResource)
    LessonRepository.deleteReminder(reminderId = id, lessonId = lessonResource.id)
}

@KtorExperimentalAPI
fun RemindReview.handle(id: Int) {
    logger.info { "Sending lesson review reminder: $this" }
    sendReviewLessonEmail(lesson = lessonResource)
    LessonRepository.deleteReminder(reminderId = id, lessonId = lessonResource.id)
}

@KtorExperimentalAPI
fun RemindAvailablePayout.handle(id: Int) {
    try {
        logger.info { "Processing payout availability: $this" }
        this.orderResource.lesson.markPayoutAvailable()
    } catch (e: Exception) {
        logger.error { "Exception while processing payout availability reminder: ${e.message} " }
        runBlocking {
            sendBotMessage("""
                <b>Category: payout-reminder-failure</b>
                <b>${this@handle.orderResource}</b>
                <pre>message: ${e.message}</pre>
            """)
        }
    }
    LessonRepository.deleteReminder(reminderId = id, lessonId = orderResource.lesson.id)
}

fun ReminderDao.toReminder(): ReminderResource {
    return ReminderResource(
            id = id,
            reminderType = reminderType,
            reminderPayload = GenericObjectMapper.getMapper().readValue(reminderPayload, ReminderType::class.java),
            triggerDate = triggerDate,
            dateCreated = dateCreated
    )
}

fun sendUpcomingLessonEmail(userId: Int, lesson: LessonResource) {
    val otherPersonName = lesson.studentDisplayName.takeIf { userId != lesson.studentId }
            ?: lesson.tutorDisplayName

    UserRepository.getUser(userId)?.let { userAccount ->
        EmailTemplate(
                message = """
        <p>Hi ${userAccount.firstName},</p>
        <h1>Get ready for your lesson with $otherPersonName</h1>
        <b>This is a reminder that your lesson starts at ${
                    DateTimeFormatter.ofPattern("HH:mm").format(lesson.lessonDate)}.</b>
        <p>Here are the lesson details:</p>
        <ul>
            <li>Topic: ${lesson.topic.name}</li>
            <li>Tutor: ${lesson.tutorDisplayName}</li>
            <li>Student: ${lesson.studentDisplayName}</li>
            <li>Start Time: ${
                    DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").format(lesson.lessonDate)}</li>
            <li>End Time: ${
                    DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").format(lesson.lessonEndDate)}</li>
        </ul>
        
        <p>We will remind you again 15 minutes before the lesson and provide a link to your meeting room.</p>
        <p>Please check your junk mail if you do get the meeting link before the lesson.</p>
        """,
                subject = "Your lesson with $otherPersonName starts soon"
        ).send(userAccount.email)
    }
}

fun sendReviewLessonEmail(lesson: LessonResource) {
    UserRepository.getUser(lesson.studentId)?.let { userAccount ->
        EmailTemplate(
                message = """
        <p>Hi ${userAccount.firstName},</p>
        <b>We hope your lesson with ${lesson.tutorDisplayName} went well.</b>
        <p>Please leave your feedback for ${lesson.tutorDisplayName} and let them know how it went.</p>
        """,
            subject = "How was your lesson with ${lesson.tutorDisplayName}?"
        ).send(userAccount.email)
    }
}

fun sendJoinEmail(userId: Int, url: String, lessonResource: LessonResource) {
    UserRepository.getUser(userId)?.let { userAccount ->
        EmailTemplate(
                message = """
        <p>Hi ${userAccount.firstName},</p>
        <b>We have prepared the meeting room for you. Please click the link below to join:</b>
        <p>$url</p>
       <ul>
            <li>Topic: ${lessonResource.topic.name}</li>
            <li>Tutor: ${lessonResource.tutorDisplayName}</li>
            <li>Student: ${lessonResource.studentDisplayName}</li>
            <li>Start Time: ${
                    DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").format(lessonResource.lessonDate)}</li>
            <li>End Time: ${
                    DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").format(lessonResource.lessonEndDate)}</li>
        </ul>
        """,
                subject = "Your meeting room is ready to join"
        ).send(userAccount.email)
    }
}
