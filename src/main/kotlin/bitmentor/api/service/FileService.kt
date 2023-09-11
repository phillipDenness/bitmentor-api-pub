package bitmentor.api.service

import bitmentor.api.config.Properties
import bitmentor.api.email.EmailTemplate
import bitmentor.api.exceptions.BadFileTypeException
import bitmentor.api.exceptions.FileTooLargeException
import bitmentor.api.exceptions.TutorNotFoundException
import bitmentor.api.model.file.Buckets
import bitmentor.api.model.file.FileInsert
import bitmentor.api.model.file.FileTypes
import bitmentor.api.model.tutor.VerificationState
import bitmentor.api.repository.FileRepository
import bitmentor.api.repository.TutorRepository
import bitmentor.api.repository.UserRepository
import bitmentor.api.storage.createStoredFile
import bitmentor.api.storage.deleteStoredFile
import io.ktor.http.content.*
import io.ktor.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import org.apache.tika.Tika
import java.io.File

private val logger = KotlinLogging.logger {}

const val MEGABYTE_IN_BYTES = 1048576

@KtorExperimentalAPI
suspend fun MultiPartData.uploadUserImage(userId: Int) {
    this.forEachPart { part ->
        if (part is PartData.FileItem) {
            val ext = File(part.originalFileName).extension
            withContext(Dispatchers.IO) {
                val byteArray = convertToByteArray(part)
                val mimeType: String = Tika().detect(byteArray)
                if (!isImage(mimeType)) {
                    throw BadFileTypeException("File is not an image $mimeType")
                }
                if (byteArray.size > (MEGABYTE_IN_BYTES * 3)) {
                    throw FileTooLargeException("File size exceeds tolerance of 3 mb")
                }
                logger.info { "Creating stored image $ext of type: $mimeType for user $userId" }
                createStoredFile("user$userId-image.${ext}", byteArray, mimeType, Buckets.IMAGE_BUCKET_NAME)
                        .let { blob ->
                            UserRepository.updateUserProfileImage(
                                    userId = userId,
                                    profileImageUrl = blob.mediaLink
                            )
                            FileRepository.createFile(
                                    userId = userId,
                                    fileInsert = FileInsert(
                                            fileType = FileTypes.USER_DISPLAY_IMAGE,
                                            fileLocation = blob.selfLink,
                                            blobName = blob.name,
                                            bucket = blob.bucket
                                    )
                            )
                        }
            }
        }
        part.dispose()
    }
}

@KtorExperimentalAPI
suspend fun MultiPartData.uploadFile(userId: Int, fileType: FileTypes) {
    this.forEachPart { part ->
        if (part is PartData.FileItem) {
            val ext = File(part.originalFileName).extension
            withContext(Dispatchers.IO) {
                val byteArray = convertToByteArray(part)

                if (fileType == FileTypes.TUTOR_DBS_VERIFICATION) {
                    TutorRepository.findTutorByUserId(userId)?.let {
                        val mimeType: String = Tika().detect(byteArray)
                        if (!isImage(mimeType) && !isPdf(mimeType)) {
                            throw BadFileTypeException("File is not supported $mimeType")
                        }
                        if (byteArray.size > (MEGABYTE_IN_BYTES * 3)) {
                            throw FileTooLargeException("File size exceeds tolerance of 3 mb")
                        }
                        logger.info { "Creating stored file $ext tutor dbs verification of type: $mimeType for user $userId" }
                        createStoredFile("tutor-dbs-verification-id-$userId.${ext}", byteArray, mimeType, Buckets.TUTOR_VERIFICATION_BUCKET_NAME)
                                .let { blob ->
                                    FileRepository.createFile(
                                            userId = userId,
                                            fileInsert = FileInsert(
                                                    fileType = fileType,
                                                    fileLocation = blob.selfLink,
                                                    blobName = blob.generatedId,
                                                    bucket = blob.bucket
                                            )
                                    )
                                    TutorRepository.updateDbsState(
                                            tutorState = VerificationState.PENDING_VERIFICATION,
                                            tutorId = it.id
                                    )

                                    sendBotMessage(formatMessage(selfLink = blob.selfLink, userId = userId, type = fileType, tutorId = it.id))
                                    sendDbsVerificationReceivedEmail(userId)
                                }
                    }?: throw TutorNotFoundException()
                } else if (fileType == FileTypes.TUTOR_ID_VERIFICATION) {
                    TutorRepository.findTutorByUserId(userId)?.let {
                        val mimeType: String = Tika().detect(byteArray)
                        if (!isImage(mimeType) && !isPdf(mimeType)) {
                            throw BadFileTypeException("File is not supported $mimeType")
                        }
                        if (byteArray.size > (MEGABYTE_IN_BYTES * 3)) {
                            throw FileTooLargeException("File size exceeds tolerance of 3 mb")
                        }
                        logger.info { "Creating stored file $ext tutor id verification of type: $mimeType for user $userId" }
                        createStoredFile("tutor-id-verification-id-$userId.${ext}", byteArray, mimeType, Buckets.TUTOR_VERIFICATION_BUCKET_NAME)
                                .let { blob ->
                                    FileRepository.createFile(
                                            userId = userId,
                                            fileInsert = FileInsert(
                                                    fileType = fileType,
                                                    fileLocation = blob.selfLink,
                                                    blobName = blob.name,
                                                    bucket = blob.bucket
                                            )
                                    )
                                    TutorRepository.updateIdState(
                                            tutorState = VerificationState.PENDING_VERIFICATION,
                                            tutorId = it.id
                                    )

                                    sendBotMessage(formatMessage(selfLink = blob.selfLink, tutorId = it.id, type = fileType, userId = userId))
                                    sendIDVerifiedEmail(userId)
                                }
                    }?: throw TutorNotFoundException()
                }
            }
        }
        part.dispose()
    }
}

@KtorExperimentalAPI
fun deleteProfileImage(userId: Int) {
    FileRepository.getByUserId(userId)
        .find { it.fileType === FileTypes.USER_DISPLAY_IMAGE }
        ?.let {
            deleteStoredFile(it)
            UserRepository.updateUserProfileImage(userId = userId, profileImageUrl = null)
        }
}

@KtorExperimentalAPI
private suspend fun formatMessage(selfLink: String, tutorId: Int, type: FileTypes, userId: Int): String {
    val user = getUser(userId)
    val tutorDetails = getTutorDetail(userId)

    return """
        <b>Category: Tutor verification document $type</b>
        <i>Email: ${user.email}</i>
        <i>Display Name: ${user.displayName}</i>
        <i>First name: ${user.firstName}</i>
        <i>Last name: ${user.lastName}</i>
        <i>DOB: ${tutorDetails?.dateOfBirth}</i>
        <i>TutorId: $tutorId</i>
        <pre>$selfLink</pre>
        <pre>${Properties.clientUrl}/admin</pre>
    """
}

private fun isImage(mimeType: String): Boolean {
    return mimeType.split("/")[0] == "image"
}

private fun isPdf(mimeType: String): Boolean {
    return mimeType.split("/")[1] == "pdf"
}

private fun convertToByteArray(part: PartData.FileItem): ByteArray {
    return part.streamProvider().readAllBytes()
}

private fun sendDbsVerificationReceivedEmail(recipientId: Int) {
    UserRepository.getUser(recipientId)?.let { userAccount ->
        EmailTemplate(
                message = """
    <p>Hi ${userAccount.firstName},</p>
    <b>Thank you for submitting your CRB / DBS check.</b>
    <p>Your document will be reviewed as soon as possible. 
        Once verified we will ensure your profile will appear more prominently in students` search results.</p>
    """,
                subject = "Tutor DBS/CRB Verification"
        ).send(userAccount.email)
    }
}

private fun sendIDVerifiedEmail(recipientId: Int) {
    UserRepository.getUser(recipientId)?.let { userAccount ->
        EmailTemplate(
                message = """
    <p>Hi ${userAccount.firstName},</p>
    <b>Thank you for submitting your ID information.</b>
    <p>Your document will be reviewed as soon as possible. 
        Once verified you will unlock more features in the tutor portal.</p>
    """,
                subject = "Tutor Verification"
        )
    }
}