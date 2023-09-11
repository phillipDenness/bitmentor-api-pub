package bitmentor.api.repository

import bitmentor.api.config.GenericObjectMapper
import bitmentor.api.config.Properties
import bitmentor.api.config.SharedJdbi
import bitmentor.api.model.reminder.ReminderInsert
import bitmentor.api.model.reminder.ReminderTypes
import bitmentor.api.repository.entity.ReminderDao
import mu.KotlinLogging
import org.jdbi.v3.core.Handle

object ReminderRepository {
    private val logger = KotlinLogging.logger {}

    fun createReminder(
            reminderInsert: ReminderInsert,
            handle: Handle
    ): Int {
        try {

            val insertJsonB = Properties.jsonObject()
            insertJsonB.type = Properties.JSON
            insertJsonB.value = GenericObjectMapper.getMapper().writeValueAsString(reminderInsert.reminderPayload)

            return handle.createUpdate(
                    """INSERT INTO bitmentor.reminder(
                    reminder_type, 
                    reminder_payload, 
                    trigger_date,
                    date_created
            )VALUES(
                    :reminder_type, 
                    :reminder_payload, 
                    :trigger_date, 
                    now())""")
                    .bind("reminder_type", reminderInsert.reminderType)
                    .bind("reminder_payload", insertJsonB)
                    .bind("trigger_date", reminderInsert.triggerDate)
                    .executeAndReturnGeneratedKeys("id")
                    .mapTo(Int::class.java)
                    .first()
        } catch (e: Exception) {
            logger.error { "An error has occurred saving the reminder ${e.message}" }
            throw Exception("An error has occurred saving the reminder")
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

    fun deleteReminderByReminderType(reminderIds: List<Int>, reminderType: ReminderTypes, handle: Handle): Int? {
        val reminderId = if (reminderIds.isNotEmpty()) {
                        handle.createQuery("""
                        SELECT id FROM bitmentor.reminder
                        WHERE id IN (<reminder_ids>) AND reminder_type = :reminder_type
                    """)
                    .bindList("reminder_ids", reminderIds)
                    .bind("reminder_type", reminderType)
                    .mapTo(Int::class.java)
                    .firstOrNull()
            } else { null }

        if (reminderId != null) {
            deleteReminder(reminderId)
        }
        return reminderId
    }

    fun updateError(reminderDao: ReminderDao, message: String?) {
        SharedJdbi.jdbi().inTransaction<Unit, Exception> { handle ->
            handle.createUpdate("""UPDATE bitmentor.reminder
                            SET error = :error
                            WHERE id = :id; """)
                .bind("error", message)
                .bind("id", reminderDao.id)
                .execute()
        }
    }
}
