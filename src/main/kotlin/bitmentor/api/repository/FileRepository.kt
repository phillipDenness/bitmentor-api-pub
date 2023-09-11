package bitmentor.api.repository

import bitmentor.api.config.SharedJdbi
import bitmentor.api.model.file.FileInsert
import bitmentor.api.model.file.FileResource
import io.ktor.util.*
import mu.KotlinLogging


@KtorExperimentalAPI
object FileRepository {
    private val logger = KotlinLogging.logger {}

    fun createFile(
            fileInsert: FileInsert,
            userId: Int
    ): Int {
        logger.info { "Storing file $fileInsert for userId $userId" }
        return SharedJdbi.jdbi().inTransaction<Int, Exception> { handle ->
            handle.createUpdate("""
                INSERT INTO bitmentor.user_file(
                    user_id,
                    file_type,
                    file_location,
                    blob_name,
                    bucket,
                    date_created
                )VALUES(
                    :user_id,
                    :file_type,
                    :file_location,
                    :blob_name,
                    :bucket,
                    now()
                )"""
            )
                    .bind("user_id", userId)
                    .bind("file_type", fileInsert.fileType)
                    .bind("file_location", fileInsert.fileLocation)
                    .bind("blob_name", fileInsert.blobName)
                    .bind("bucket", fileInsert.bucket)
                    .execute()
        }
    }

    fun getByUserId(userId: Int): List<FileResource> {
        return SharedJdbi.jdbi().inTransaction<List<FileResource>, Exception> { handle ->
            handle.createQuery("""
                SELECT id, user_id, file_type, file_location, blob_name, bucket, date_created
                FROM bitmentor.user_file
                WHERE user_id = :user_id
            """)
                    .bind("user_id", userId)
                    .mapTo(FileResource::class.java)
                    .list()
        }
    }

    fun delete(id: Int) {
        logger.info { "Deleting file from database id: $id" }
        return SharedJdbi.jdbi().inTransaction<Unit, Exception> { handle ->
            handle.createUpdate("""
                DELETE FROM bitmentor.user_file
                WHERE id = :id
                """
            )
                    .bind("id", id)
                    .execute()
        }
    }
}
