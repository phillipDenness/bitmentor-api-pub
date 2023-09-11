package bitmentor.api.service

import bitmentor.api.model.short_list.TutorShortList
import bitmentor.api.repository.TutorShortListRepository
import bitmentor.api.repository.UserRepository
import io.ktor.util.*

@KtorExperimentalAPI
fun getTutorShortList(userId: Int): List<TutorShortList> {
    return UserRepository.getUser(userId)?.shortListTutors?.let { shortListTutors ->
        TutorShortListRepository.getTutorShortList(tutorIds = shortListTutors).map { dao ->
            TutorShortList(
                tutorId = dao.tutorId,
                displayName = dao.displayName,
                topics = getTutorTopics(id = dao.tutorId),
                tagline = dao.tagline,
                profileImageUrl = dao.profileImageUrl
            )
        }
    } ?: emptyList()
}

fun updateTutorShortList(userId: Int, tutorIds: Set<Int>) {
    UserRepository.updateShortList(tutorIds = tutorIds, userId = userId)
}