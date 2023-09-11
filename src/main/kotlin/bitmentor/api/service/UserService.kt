package bitmentor.api.service

import bitmentor.api.exceptions.UserAccountIncompleteException
import bitmentor.api.exceptions.UserNotFoundException
import bitmentor.api.model.auth.UserSession
import bitmentor.api.model.user.UpdateUserRequest
import bitmentor.api.model.user.UserPublicResource
import bitmentor.api.model.user.UserResource
import bitmentor.api.repository.FileRepository
import bitmentor.api.repository.TutorRepository
import bitmentor.api.repository.UserRepository
import bitmentor.api.storage.deleteStoredFile
import io.ktor.util.*
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}
fun getUser(userId: Int): UserResource {
    try {
        return UserRepository.getUser(userId)?.let {
            UserResource(
                title = it.title,
                id = it.id,
                joinDate = it.joinDate,
                displayName = it.displayName,
                firstName = it.firstName,
                middleName = it.middleName,
                lastName = it.lastName,
                lastOnline = it.lastOnline,
                profileImageUrl = it.profileImageUrl,
                email = it.email,
                lastModified = it.lastModified
            )
        } ?: throw UserNotFoundException()
    } catch (e: IllegalArgumentException) {
        throw UserAccountIncompleteException()
    }
}

fun getUserPublic(userId: Int): UserPublicResource? {
    try {
        return UserRepository.getUser(userId)?.let {
            UserPublicResource(
                id = it.id,
                joinDate = it.joinDate,
                title = it.title,
                displayName = it.displayName,
                firstName = it.firstName,
                lastOnline = it.lastOnline,
                profileImageUrl = it.profileImageUrl,
                lastModified = it.lastModified
            )
        } ?: throw UserNotFoundException()
    } catch (e: IllegalArgumentException) {
        throw UserAccountIncompleteException()
    }
}

fun isUserComplete(userId: Int): Boolean {
    return UserRepository.getUser(userId)?.displayName != null
}

@KtorExperimentalAPI
fun deleteUser(userId: Int) {
    FileRepository.getByUserId(userId).forEach {
        logger.info { "Calling google to delete user file $it" }
        deleteStoredFile(it)
    }
    UserRepository.delete(userId)
}

@KtorExperimentalAPI
fun UpdateUserRequest.update(userId: Int): UserSession {
    UserRepository.updateUser(this, userId)

    return UserSession(
        id = userId,
        completedRegistration = true,
        tutorId = TutorRepository.findTutorByUserId(userId)?.id,
            loginType = null
    )
}
