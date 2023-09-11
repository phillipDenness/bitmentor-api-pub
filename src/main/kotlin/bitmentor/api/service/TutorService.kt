package bitmentor.api.service

import bitmentor.api.config.Properties
import bitmentor.api.email.EmailTemplate
import bitmentor.api.exceptions.TutorNotFoundException
import bitmentor.api.exceptions.UserAccountIncompleteException
import bitmentor.api.model.auth.UserSession
import bitmentor.api.model.availability.AvailabilityTutor
import bitmentor.api.model.tutor.*
import bitmentor.api.repository.TutorProjectRepository
import bitmentor.api.repository.TutorRepository
import bitmentor.api.repository.TutorTopicRepository
import bitmentor.api.repository.UserRepository
import bitmentor.api.repository.entity.TutorDao
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.features.*
import io.ktor.util.*
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

@KtorExperimentalAPI
fun getTutorsByDisciplineId(disciplineId: Int, page: Int, size: Int, sort: TutorSort, dbsVerified: Boolean?): TutorPagedResource {
    val totalTutors = TutorTopicRepository.countTutorsByDiscipline(disciplineId, dbsVerified)
    val tutors = TutorRepository.getPagedTutorsByDisciplineId(
        page = page,
        size = size,
        disciplineId = disciplineId,
        sort = sort,
        dbsVerified = dbsVerified
    ).map {
        it.buildResource() ?: throw TutorNotFoundException()
    }
    return TutorPagedResource(
        tutors = tutors,
        page = page,
        size = size,
        totalTutors = totalTutors
    )
}

@KtorExperimentalAPI
fun getTutorsByTopicId(topicId: Int, page: Int, size: Int, sort: TutorSort, dbsVerified: Boolean?): TutorPagedResource {
    val totalTutors = TutorTopicRepository.countTutorsByTopic(topicId, dbsVerified)
    val tutors = TutorRepository.getPagedTutorsByTopicId(
        page = page,
        size = size,
        topicId = topicId,
        dbsVerified = dbsVerified,
        sort = sort
    ).map {
        it.buildResource() ?: throw TutorNotFoundException()
    }
    return TutorPagedResource(
        tutors = tutors,
        page = page,
        size = size,
        totalTutors = totalTutors,
        topicMeta = getTopicMeta(topicId)
    )
}

@KtorExperimentalAPI
fun getTutors(page: Int, size: Int, sort: TutorSort, dbsVerified: Boolean?): TutorPagedResource {
    val totalTutors = TutorRepository.countTutors(dbsVerified)
    val tutors = TutorRepository.getPagedTutors(
        page = page,
        size = size,
        sort = sort,
        dbsVerified = dbsVerified
    ).map {
        it.buildResource() ?: throw TutorNotFoundException()
    }
    return TutorPagedResource(
            tutors = tutors,
            page = page,
            size = size,
            totalTutors = totalTutors
    )
}

@KtorExperimentalAPI
fun getTutor(id: Int): TutorResource? {
    return TutorRepository.getTutor(id)?.buildResource()
}

@KtorExperimentalAPI
fun TutorInsert.createTutor(userId: Int): UserSession {
    UserRepository.findUserById(userId)?.displayName
        ?: throw UserAccountIncompleteException()

    if (this.tutorTopics.any { it.cost < Properties.squareUpMinPayment }){
        throw BadRequestException("Cost must be minimum £${Properties.squareUpMinPayment}");
    }

    val formatted = this.copy(
            about = this.about.replace("(\r\n|\n)", "<br />"),
            experience = this.experience?.replace("(\r\n|\n)", "<br />")
    )

    github?.isValidGitHubUrl()

    return UserSession(
        id = userId,
        tutorId = TutorRepository.createTutor(formatted, userId),
        completedRegistration = true,
        loginType = null
    ).also {
        sendTutorJoinEmail(userId)
    }
}

@KtorExperimentalAPI
fun getTutorId(userId: Int): Int? {
    return TutorRepository.findTutorByUserId(userId)?.id
}

@KtorExperimentalAPI
fun UpdateTutorRequest.updateTutor(
    userId: Int
): TutorResource {
    val tutorDao = TutorRepository.findTutorByUserId(userId)
        ?: throw TutorNotFoundException()

    // TODO reactivate verification before publication
//    if (isActive && tutorDao.idVerificationState != VerificationState.VERIFIED) {
//        throw BadRequestException("Must be ID verified before publishing")
//    }

    if (this.tutorTopics.isEmpty()) {
        throw BadRequestException("Must have at least 1 topic");
    }
    if (this.tutorTopics.any { it.cost < Properties.squareUpMinPayment }){
        throw BadRequestException("Cost must be minimum £${Properties.squareUpMinPayment}");
    }

    val formatted = this.copy(
            about = this.about.replace("(\r\n|\n)", "<br />"),
            experience = this.experience?.replace("(\r\n|\n)", "<br />")
    )

    github?.isValidGitHubUrl()

    TutorRepository.updateTutor(
            tutorInsert = formatted,
            tutorId = tutorDao.id
    )

    return getTutor(tutorDao.id) ?: throw TutorNotFoundException()
}

fun getTutorTopics(id: Int): List<TutorTopic> {
    return TutorTopicRepository.getTutorTopicsByTutorId(tutorId = id).let { topicDaos ->
        topicDaos.map {
            TutorTopic(
                id = it.id,
                name = it.name,
                cost = it.cost,
                topicId = it.topicId,
                discipline = it.discipline,
                disciplineId = it.disciplineId,
                years = it.years
            )
        }
    }
}

@KtorExperimentalAPI
private fun String.isValidGitHubUrl() {
    if (!this.contains("github.com", ignoreCase = true)) {
        throw BadRequestException("Github link must point to github.com")
    }
}

private fun TutorDao.buildResource(): TutorResource? {
    return UserRepository.getUser(this.userId)
            ?.let {
                TutorResource(
                    id = this.id,
                    tutorUserId = it.id,
                    displayName = it.displayName ?: throw UserAccountIncompleteException(),
                    lastOnline = it.lastOnline,
                    profileImageUrl = it.profileImageUrl,
                    idVerificationState = this.idVerificationState,
                    dbsVerificationState = this.dbsVerificationState,
                    isActive = this.isActive,
                    tutorJoinDate = this.tutorJoinDate,
                    tagline = this.tagline,
                    about = this.about,
                    experience = this.experience,
                    github = this.github,
                    availability = jacksonObjectMapper().readValue(this.availability, AvailabilityTutor::class.java),
                    tutorTopics = getTutorTopics(id = id),
                    rating = rating,
                    ratings = ratings,
                    promotions = getAllPromotions().filter { promo -> promotions?.contains(promo.code) ?: false },
                    areasOfInterest = this.areasOfInterest ?: emptyList(),
                    otherInterest = this.otherInterest,
                    projects = TutorProjectRepository.findByTutorId(this.id)
                )
            }
}

private fun sendTutorJoinEmail(recipientId: Int) {
    UserRepository.getUser(recipientId)?.let { userAccount ->
        EmailTemplate(
                message = """
    <p>Welcome ${userAccount.firstName}!</p>
    <p>Thank you for registering as a tutor. We hope you have had a chance to start 
    building your tutor profile with us. The next step of the registration process is to verify your account. 
    You can do this by logging into your account and visiting the <a href="${Properties.clientUrl}/account?view=tutor">Tutor Account</a>.</p>

    <b>We recommend doing the following to attract more students</b>
    <ol>
        <li>Upload a profile picture</li>
        <li>Link your GitHub account</li>
        <li>Activate promotions</li>
        <li>Introduce yourself</li>
        <li>Describe your experience and qualifications</li>
    </ol>
    """,
                subject = "Tutor profile activation"
        ).send(userAccount.email)
    }
}

fun dbsVerifiedCondition(dbsVerified: Boolean?): String {
    return dbsVerified?.let {
        if (it) {
            "AND dbs_verification_state = 'VERIFIED'"
        } else {
            "AND dbs_verification_state = 'NOT_VERIFIED'"
        }
    } ?: ""
}