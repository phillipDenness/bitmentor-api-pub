package bitmentor.api.repository

import bitmentor.api.config.SharedJdbi
import bitmentor.api.model.auth.UserInsert
import bitmentor.api.model.notification.NotificationInsert
import bitmentor.api.model.notification.NotificationType
import bitmentor.api.model.user.UpdateUserRequest
import bitmentor.api.repository.entity.UserAccountDao
import bitmentor.api.repository.entity.UserDao
import mu.KotlinLogging
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.generic.GenericType

object UserRepository {
    private val logger = KotlinLogging.logger {}

    private const val USER_DAO_QUERY: String = """
                        SELECT 
                            id,
                            email, 
                            title,
                            join_date, 
                            display_name, 
                            first_name, 
                            middle_name,
                            last_name, 
                            last_online, 
                            last_modified, 
                            profile_image_url,
                            short_list_tutors
                        FROM 
                            bitmentor.user_account"""

    fun getUser(userId: Int): UserAccountDao? {
        return SharedJdbi.jdbi().inTransaction<UserAccountDao?, Exception> { handle ->
            getUser(userId, handle)
        }
    }

    fun getUser(userId: Int, handle: Handle): UserAccountDao? {
        return handle.createQuery(
            """$USER_DAO_QUERY WHERE id = :id"""
        )
            .bind("id", userId)
            .mapTo(UserAccountDao::class.java)
            .firstOrNull()
    }

    fun findUserByEmail(email: String): UserDao? {
        return SharedJdbi.jdbi().inTransaction<UserDao?, Exception> { handle ->
            handle.createQuery(
                    """$USER_DAO_QUERY WHERE email = :email"""
            )
                    .bind("email", email)
                    .mapTo(UserDao::class.java)
                    .firstOrNull()
        }
    }

    fun findUserById(id: Int, handle: Handle): UserDao? {
            return handle.createQuery(
                    """$USER_DAO_QUERY WHERE id = :id"""
            )
                    .bind("id", id)
                    .mapTo(UserDao::class.java)
                    .firstOrNull()
    }

    fun findUserById(id: Int): UserDao? {
        return SharedJdbi.jdbi().inTransaction<UserDao?, Exception> { handle ->
            findUserById(id, handle)
        }
    }

    fun createUser(
            userInsert: UserInsert
    ): Int {
        val salt = "md5"

        try {
            return SharedJdbi.jdbi().inTransaction<Int, Exception> { handle ->
                handle.createUpdate(
                        """INSERT INTO bitmentor.user_account(
                            email, 
                            password, 
                            join_Date, 
                            last_online, 
                            last_modified
                            )VALUES(
                                   :email, 
                                   public.crypt (:password, public.gen_salt(:salt::text)), 
                                   now(), 
                                   now(), 
                                   now()
                                   )"""
                )
                        .bind("email", userInsert.email)
                        .bind("password", userInsert.password)
                        .bind("salt", salt)
                        .executeAndReturnGeneratedKeys("id").mapTo(Int::class.java)
                        .first().also {
                            NotificationRepository.createNotification(
                                    NotificationInsert(
                                            userId = it,
                                            type = NotificationType.COMPLETE_ACCOUNT
                                    ),
                                    handle
                            )
                        }
            }

        } catch (e: Exception) {
            logger.error { "An error has occurred saving the user ${e.message}" }
            throw Exception("An error has occurred saving the user")
        }
    }

    fun updatePassword(userId: Int, password: String, token: String, isVerified: Boolean? = false) {
        val salt = "md5"
        try {
            val query = """UPDATE bitmentor.user_account
                            SET password_reset = '',
                                password = public.crypt (:password, public.gen_salt(:salt::text))
                            WHERE id = :userId AND password_reset = :token""".takeUnless { isVerified == true }
                    ?: """UPDATE bitmentor.user_account
                            SET password_reset = '',
                                password = public.crypt (:password, public.gen_salt(:salt::text))
                            WHERE id = :userId"""

            return SharedJdbi.jdbi().inTransaction<Unit, Exception> { handle ->
                handle.createUpdate(
                        query
                )
                        .bind("userId", userId)
                        .bind("password", password)
                        .bind("token", token)
                        .bind("salt", salt)
                        .execute()
            }
        } catch (e: Exception) {
            throw Exception("An error has occurred checking the users password")
        }
    }

    fun updatePasswordReset(userId: Int, token: String) {
        try {
            return SharedJdbi.jdbi().inTransaction<Unit, Exception> { handle ->
                handle.createUpdate(
                        """UPDATE bitmentor.user_account
                            SET password_reset = :token
                            WHERE id = :userId"""
                )
                        .bind("userId", userId)
                        .bind("token", token)
                        .execute()
            }
        } catch (e: Exception) {
            throw Exception("An error has occurred checking the users password")
        }
    }

    fun authenticateUserPassword(
            email: String,
            password: String,
            userId: Int? = null
    ): Int {
        try {
            return SharedJdbi.jdbi().inTransaction<Int, Exception> { handle ->
                val query = """SELECT count(*) FROM bitmentor.user_account
                        WHERE password is NOT NULL
                          AND email = :email
                          AND password = public.crypt(:password::text,password);""".takeIf { userId == null }
                        ?: """SELECT count(*) FROM bitmentor.user_account
                        WHERE password is NOT NULL
                          AND id = :user_id
                          AND password = public.crypt(:password::text,password);"""

                handle.createQuery(
                        query
                )
                        .bind("email", email)
                        .bind("password", password)
                        .bind("user_id", userId)
                        .mapTo(Int::class.java)
                        .first()
            }
        } catch (e: Exception) {
            throw Exception("An error has occurred checking the users password")
        }
    }

    fun updateUser(updateUserRequest: UpdateUserRequest, userId: Int) {
        try {
            return SharedJdbi.jdbi().inTransaction<Unit, Exception> { handle ->
                handle.createUpdate(
                        """UPDATE bitmentor.user_account
                            SET display_name = :display_name, 
                            title = :title,
                            first_name = :first_name,
                            middle_name = :middle_name,
                            last_name = :last_name,
                            last_modified = NOW()
                            WHERE id = :user_id;"""
                )
                        .bind("user_id", userId)
                        .bind("display_name", updateUserRequest.displayName)
                        .bind("title", updateUserRequest.title)
                        .bind("first_name", updateUserRequest.firstName)
                        .bind("middle_name", updateUserRequest.middleName.takeUnless { it == "" })
                        .bind("last_name", updateUserRequest.lastName)
                        .execute().also {
                        NotificationRepository.deleteNotification(userId, NotificationType.COMPLETE_ACCOUNT)
                    }
            }
        } catch (e: Exception) {
            logger.error { "An error has occurred updating the users account ${e.message}" }
            throw Exception("An error has occurred updating the users account")
        }
    }

    fun updateLastActive(userId: Int) {
        return SharedJdbi.jdbi().inTransaction<Unit, Exception> { handle ->
            updateLastActive(userId, handle)
        }
    }

    fun updateUserProfileImage(userId: Int, profileImageUrl: String?) {
        try {
            logger.info { "Updating users $userId profile image url: $profileImageUrl" }
            return SharedJdbi.jdbi().inTransaction<Unit, Exception> { handle ->
                handle.createUpdate(
                    """UPDATE bitmentor.user_account
                            SET profile_image_url = :profile_image_url
                            WHERE id = :user_id;"""
                )
                    .bind("user_id", userId)
                    .bind("profile_image_url", profileImageUrl)
                    .execute()
            }
        } catch (e: Exception) {
            logger.error { "An error has occurred updating the users profile image url ${e.message}" }
            throw Exception("An error has occurred updating the users active")
        }
    }

    fun updateShortList(tutorIds: Set<Int>, userId: Int) {
        return SharedJdbi.jdbi().inTransaction<Unit, Exception> { handle ->
            handle.createUpdate(
                """UPDATE bitmentor.user_account
                            SET short_list_tutors = :short_list_tutors
                            WHERE id = :user_id;"""
            )
                .bind("user_id", userId)
                .bindByType("short_list_tutors", tutorIds, object : GenericType<List<Int>>() {})
                .execute()
        }
    }

    private fun updateLastActive(userId: Int, handle: Handle) {
        try {
            handle.createUpdate(
                """UPDATE bitmentor.user_account
                            SET last_online = NOW()
                            WHERE id = :user_id;"""
            )
                .bind("user_id", userId)
                .execute()
        } catch (e: Exception) {
            logger.error { "An error has occurred updating the users active ${e.message}" }
            throw Exception("An error has occurred updating the users active")
        }
    }

    fun delete(userId: Int) {
        logger.info { "Deleting user from database userId: $userId" }
        return SharedJdbi.jdbi().inTransaction<Unit, Exception> { handle ->
            handle.createUpdate(
                    """DELETE FROM bitmentor.user_account
                            WHERE id = :user_id;"""
            )
                    .bind("user_id", userId)
                    .execute()
        }
    }
}
