package bitmentor.api.model.file

data class FileInsert(
        val blobName: String,
        val fileType: FileTypes,
        val fileLocation: String,
        val bucket: String
)
