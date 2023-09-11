package bitmentor.api.repository

import bitmentor.api.config.SharedJdbi
import bitmentor.api.model.bigbluebutton.BbbMeetingInsert
import bitmentor.api.repository.entity.ReminderDao
import mu.KotlinLogging

object BbbRepository {
    private val logger = KotlinLogging.logger {}

    fun createMeeting(
            meetingInsert: BbbMeetingInsert
    ): Int {
            return SharedJdbi.jdbi().inTransaction<Int, Exception> { handle ->
                try {
                 handle.createUpdate(
                        """
                            INSERT INTO bitmentor.bbb_meeting(
                                    lesson_id, 
                                    meeting_id, 
                                    moderator_pw,
                                    duration,
                                    date_created
                            )VALUES(
                                    :lesson_id, 
                                    :meeting_id, 
                                    :moderator_pw, 
                                    :duration, 
                                    now())""")
                        .bind("lesson_id", meetingInsert.lessonId)
                        .bind("meeting_id", meetingInsert.meetingId)
                        .bind("moderator_pw", meetingInsert.moderatorPw)
                        .bind("duration", meetingInsert.duration)
                        .executeAndReturnGeneratedKeys("id")
                        .mapTo(Int::class.java)
                        .first()
            } catch (e: Exception) {
                logger.error { "An error has occurred saving the meeting ${e.message}" }
                throw Exception("An error has occurred saving the meeting")
            }
        }
    }

    fun findDueReminders(): List<ReminderDao> {
        return SharedJdbi.jdbi().inTransaction<List<ReminderDao>, Exception> { handle ->
            handle.createQuery(
                    """
                        SELECT * 
                        FROM bitmentor.reminder
                        WHERE trigger_date < now()
                        """)
                    .mapTo(ReminderDao::class.java)
                    .toList()
        }
    }

    fun deleteReminder(id: Int) {
        SharedJdbi.jdbi().inTransaction<Unit, Exception> { handle ->
            handle.createUpdate("""
                DELETE FROM bitmentor.reminder
                WHERE id = :id
                """)
                    .bind("id", id)
                    .execute()
        }
    }
}
