package bitmentor.api.model.file

import java.time.ZonedDateTime

data class FileResource(
        val id: Int,
        val userId: Int,
        val fileType: FileTypes,
        val blobName: String,
        val bucket: String,
        val fileLocation: String,
        val dateCreated: ZonedDateTime
)
