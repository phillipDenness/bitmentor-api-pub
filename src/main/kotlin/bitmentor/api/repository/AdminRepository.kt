package bitmentor.api.repository

import bitmentor.api.config.SharedJdbi
import bitmentor.api.model.file.FileInsert
import io.ktor.util.*
import mu.KotlinLogging


@KtorExperimentalAPI
object AdminRepository {
    private val logger = KotlinLogging.logger {}

    fun isAdmin(
            userId: Int
    ): Boolean {
        return SharedJdbi.jdbi().inTransaction<Boolean, Exception> { handle ->
            handle.createQuery("""
                SELECT count(*) FROM bitmentor.admin WHERE user_id = :user_id"""
            )
                    .bind("user_id", userId)
                    .mapTo(Int::class.java)
                    .first() > 0
        }
    }
}
