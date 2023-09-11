package bitmentor.api.service

import bitmentor.api.config.Properties
import bitmentor.api.email.EmailTemplate
import bitmentor.api.exceptions.UserNotFoundException
import bitmentor.api.model.review.ReviewInsert
import bitmentor.api.model.review.ReviewPagedResource
import bitmentor.api.model.review.ReviewResource
import bitmentor.api.model.review.ReviewUpdate
import bitmentor.api.repository.ReviewRepository
import bitmentor.api.repository.TutorRepository
import bitmentor.api.repository.UserRepository
import bitmentor.api.repository.entity.ReviewDao
import io.ktor.util.*
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

@KtorExperimentalAPI
fun ReviewInsert.create(userId: Int): Int {
    return ReviewRepository.createReview(
            reviewInsert = this,
            studentId = userId
    ).also {
        sendReviewReceivedEmail(this.tutorId, userId)
    }
}

@KtorExperimentalAPI
fun ReviewUpdate.update(userId: Int, reviewId: Int) {
    ReviewRepository.updateReview(
            reviewUpdate = this,
            userId = userId,
            reviewId = reviewId
    )
}

@KtorExperimentalAPI
fun deleteReview(userId: Int, reviewId: Int) {
    ReviewRepository.deleteReview(
            reviewId = reviewId,
            userId = userId
    )
}

@KtorExperimentalAPI
fun getReviewsByTutorId(tutorId: Int, page: Int, size: Int): ReviewPagedResource {
    val totalReviews = ReviewRepository.countReviewsByTutorId(tutorId = tutorId)
    val reviews = ReviewRepository.findReviewsByTutorId(tutorId = tutorId, page = page, size = size)

    return ReviewPagedResource(
            reviews = reviews,
            totalReviews = totalReviews
    )
}

@KtorExperimentalAPI
fun getReviewsByUserId(userId: Int, page: Int, size: Int): ReviewPagedResource {
    val totalReviews = ReviewRepository.countReviewsByUserId(userId = userId)
    val reviews = ReviewRepository.findReviewsByUserId(userId = userId, page = page, size = size)

    return ReviewPagedResource(
            reviews = reviews,
            totalReviews = totalReviews
    )
}

@KtorExperimentalAPI
private fun sendReviewReceivedEmail(recipientId: Int, senderId: Int) {
    val senderDetails = UserRepository.getUser(senderId) ?: throw UserNotFoundException()
            .also { logger.error { "Could not find $senderId user when sending review received email" } }

    val tutor = TutorRepository.findTutorByUserId(recipientId)
    UserRepository.getUser(recipientId)?.let { userAccount ->
        EmailTemplate(
                message = """
    <p>Hi ${userAccount.firstName}!</p>
    <p>We are contacting you because ${senderDetails.displayName} has left you feedback.</p>
    <p>You can view your feedback on your <a href=${Properties.clientUrl}/tutors/${tutor?.id}$>profile</a></p>
    """,
                subject = "Feedback from ${senderDetails.displayName}"
        ).send(userAccount.email)
    }
}


fun ReviewDao.toResource(): ReviewResource {
    val studentImage = UserRepository.getUser(studentId)?.profileImageUrl
    return ReviewResource(
        id = id,
        reason = reason,
        dateCreated = dateCreated,
        tutorId = tutorId,
        studentId = studentId,
        overallRating = overallRating,
        studentDisplayName = studentDisplayName,
        studentProfileImageUrl = studentImage,
        topic = topic_name
    )
}