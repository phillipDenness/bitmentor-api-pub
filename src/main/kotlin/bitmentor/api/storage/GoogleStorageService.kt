package bitmentor.api.storage

import bitmentor.api.config.Properties
import bitmentor.api.model.file.Buckets
import bitmentor.api.model.file.FileResource
import bitmentor.api.repository.FileRepository
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.Identity
import com.google.cloud.Policy
import com.google.cloud.storage.Blob
import com.google.cloud.storage.BlobId
import com.google.cloud.storage.BlobInfo
import com.google.cloud.storage.BucketInfo
import com.google.cloud.storage.Storage
import com.google.cloud.storage.StorageClass
import com.google.cloud.storage.StorageException
import com.google.cloud.storage.StorageOptions
import com.google.cloud.storage.StorageRoles
import io.ktor.util.*
import mu.KotlinLogging
import org.springframework.core.io.ClassPathResource
import java.io.InputStream

const val BUCKET_LOCATION = "EU"

private val logger = KotlinLogging.logger {}

fun createPublicImageBucket() {
    logger.info { "Initialising google storage bucket ${Buckets.IMAGE_BUCKET_NAME.label} with credentials ${Properties.googleCredentialFile}" }
    val credential: GoogleCredentials = GoogleCredentials.fromStream(getResourceFileAsString(Properties.googleCredentialFile))
    val storage: Storage = StorageOptions.newBuilder().setCredentials(credential).build().service
    val storageClass = StorageClass.STANDARD

    try {
        val bucket = storage.create(
            BucketInfo.newBuilder(Buckets.IMAGE_BUCKET_NAME.label)
                .setStorageClass(storageClass)
                .setLocation(BUCKET_LOCATION)
                .build()
        )

        val originalPolicy: Policy = storage.getIamPolicy(Buckets.IMAGE_BUCKET_NAME.label)
        storage.setIamPolicy(
                Buckets.IMAGE_BUCKET_NAME.label,
            originalPolicy
                .toBuilder()
                .addIdentity(StorageRoles.objectViewer(), Identity.allUsers()) // All users can view
                .build()
        )
        logger.info { "Created bucket ${bucket.name} in ${bucket.location} with storage class ${bucket.storageClass}" }
    } catch (e: StorageException) {
        if (e.code != 409) {
            logger.error { e }
            throw e
        }
    }
}

fun createTutorVerificationBucket() {
    logger.info { "Initialising google storage bucket ${Buckets.TUTOR_VERIFICATION_BUCKET_NAME.label} with credentials ${Properties.googleCredentialFile}" }
    val credential: GoogleCredentials = GoogleCredentials.fromStream(getResourceFileAsString(Properties.googleCredentialFile))
    val storage: Storage = StorageOptions.newBuilder().setCredentials(credential).build().service
    val storageClass = StorageClass.NEARLINE

    try {
        val bucket = storage.create(
                BucketInfo.newBuilder(Buckets.TUTOR_VERIFICATION_BUCKET_NAME.label)
                        .setStorageClass(storageClass)
                        .setLocation(BUCKET_LOCATION)
                        .build()
        )

        val originalPolicy: Policy = storage.getIamPolicy(Buckets.TUTOR_VERIFICATION_BUCKET_NAME.label)
        storage.setIamPolicy(
                Buckets.TUTOR_VERIFICATION_BUCKET_NAME.label,
                originalPolicy
                        .toBuilder()
                        .build()
        )
        logger.info { "Created bucket ${bucket.name} in ${bucket.location} with storage class ${bucket.storageClass}" }
    } catch (e: StorageException) {
        if (e.code != 409) {
            logger.error { e }
            throw e
        }
    }
}

fun createStoredFile(objectName: String, bytes: ByteArray, mimeType: String, bucket: Buckets): Blob {
    val credential: GoogleCredentials = GoogleCredentials.fromStream(getResourceFileAsString(Properties.googleCredentialFile))
    val storage: Storage = StorageOptions.newBuilder().setCredentials(credential).build().service

    val blobId = BlobId.of(bucket.label, objectName)
    val blobInfo = BlobInfo.newBuilder(blobId)
        .setContentType(mimeType)
        .build()

    val blob = storage.create(blobInfo, bytes)

    logger.info { "Upload succeeded. Media Link: ${blob.mediaLink}" }
    return blob
}

@KtorExperimentalAPI
fun deleteStoredFile(file: FileResource) {
    val credential: GoogleCredentials = GoogleCredentials.fromStream(getResourceFileAsString(Properties.googleCredentialFile))
    val storage: Storage = StorageOptions.newBuilder().setCredentials(credential).build().service

    val blob = BlobId.of(file.bucket, file.blobName)
    logger.info { "Calling storage delete for blob: $blob" }
    storage.delete(blob)
    FileRepository.delete(file.id)
}

private fun getResourceFileAsString(fileName: String): InputStream {
    return ClassPathResource(fileName).inputStream
}
