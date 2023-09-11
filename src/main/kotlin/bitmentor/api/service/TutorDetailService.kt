package bitmentor.api.service

import bitmentor.api.exceptions.TutorNotFoundException
import bitmentor.api.model.tutor_detail.PreferredPayoutOption
import bitmentor.api.model.tutor_detail.TutorDetail
import bitmentor.api.model.tutor_detail.UpdateTutorDetailRequest
import bitmentor.api.model.user.Location
import bitmentor.api.repository.TutorDetailRepository
import bitmentor.api.repository.TutorRepository
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.features.*
import io.ktor.util.*
import mu.KotlinLogging
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val logger = KotlinLogging.logger {}

@KtorExperimentalAPI
suspend fun getTutorDetail(userId: Int): TutorDetail? {
    return TutorDetailRepository.getByUserId(userId)?.let { dao ->
            TutorDetail(
                tutorId = dao.tutorId,
                dateOfBirth = dao.dateOfBirth?.maskDob(),
                businessName = dao.businessName,
                phoneNumber = dao.phoneNumber?.maskNumber(),
                location = dao.location?.let { jacksonObjectMapper().readValue(it, Location::class.java) },
                account = dao.payeeUuid?.getPayeeDetail(),
                paypalEmailAddress = dao.paypalEmailAddress,
                paypalPaymentsReceivable = dao.paypalPaymentsReceivable,
                preferredPayoutOption = dao.preferredPayoutOption
            )
    }
}

@KtorExperimentalAPI
suspend fun UpdateTutorDetailRequest.update(userId: Int): TutorDetail {
    phoneNumber?.let {
        if (it.length < 9) {
            throw BadRequestException("Phone number must be at least 9 characters")
        }
    }

    businessName?.let {
        if (it.isEmpty()) {
            throw BadRequestException("Business name must be at least 1 characters")
        }
    }

    dateOfBirth?.let {
        if (!it.isValidAge()) {
            throw BadRequestException("Tutor must be at least 18 years old")
        }
    }

    TutorRepository.findTutorByUserId(userId)?.let {
        TutorDetailRepository.update(updateTutorDetailRequest = this, tutorId = it.id)
    }
    return getTutorDetail(userId) ?: throw TutorNotFoundException()
}

@KtorExperimentalAPI
fun PreferredPayoutOption.update(userId: Int) {
    TutorRepository.findTutorByUserId(userId)?.let {
        TutorDetailRepository.updatePreferredPayoutOption(tutorId = it.id, preferredPayoutOption = this)
    }
}

private fun LocalDate.isValidAge(): Boolean {
    return this.isBefore(LocalDate.now().minusYears(18))
}

private fun LocalDate.maskDob(): String {
    return DateTimeFormatter.ofPattern("dd/MM/yyyy").format(this).replaceRange(0, 6, "**/**/")
}

private fun String.maskNumber(): String {
    return this.replaceRange(0, 11, "*********")
}
