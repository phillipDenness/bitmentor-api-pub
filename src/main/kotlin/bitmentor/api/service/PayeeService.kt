package bitmentor.api.service

import bitmentor.api.config.Properties
import bitmentor.api.config.client
import bitmentor.api.model.payee.PayeeAccountInsert
import bitmentor.api.model.payee.PayeeAccountResource
import bitmentor.api.model.payee.PayeeInsert
import bitmentor.api.model.starling.*
import bitmentor.api.model.tutor.VerificationState
import bitmentor.api.repository.TutorDetailRepository
import bitmentor.api.repository.TutorRepository
import io.ktor.client.call.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.util.*
import mu.KotlinLogging
import java.time.format.DateTimeFormatter

private val logger = KotlinLogging.logger {}

@KtorExperimentalAPI
suspend fun PayeeInsert.create(userId: Int) {
    val user = getUser(userId)
    val tutorDao = TutorDetailRepository.getByUserId(userId)
            ?: throw Exception("Could not find tutor details for userId $userId")

    if (tutorDao.dateOfBirth == null || tutorDao.phoneNumber == null) {
        throw BadRequestException("Tutor must have verified date of birth and phone number before creating payee")
    }

    val tutor = TutorRepository.findTutorByUserId(userId)
    if (tutor == null || tutor.idVerificationState == VerificationState.NOT_VERIFIED) {
        throw BadRequestException("Must send ID for verification first")
    }

    tutorDao.payeeUuid?.let { payeeUuid ->
        try {
            logger.info { "Removing existing payee ${tutorDao.payeeUuid} for user $userId" }
            client().use { client ->
                client.delete<StarlingDeleteResponse>() {
                    url("${Properties.starlingUrl}/api/v2/payees/$payeeUuid")
                    header("Authorization", "Bearer ${Properties.starlingToken}")
                    contentType(ContentType.Application.Json)
                }
            }

        } catch (e: ClientRequestException) {
            val response = String(e.response.readBytes())
            logger.error { "Error deleting starling payee id: ${tutorDao.payeeUuid} $response" }
            throw e
        } catch (ignored: NoTransformationFoundException) {}
    }

    try {
        val response = client().use { client ->
            client.put<StarlingResponse> {
                url("${Properties.starlingUrl}/api/v2/payees")
                header("Authorization", "Bearer ${Properties.starlingToken}")
                contentType(ContentType.Application.Json)
                body = StarlingPayeeRequest(
                        payeeName = user.email,
                        phoneNumber = tutorDao.phoneNumber,
                        payeeType = "INDIVIDUAL",
                        firstName = user.firstName ?: throw Exception("User $userId missing firstName"),
                        middleName = user.middleName,
                        lastName = user.lastName ?: throw Exception("User $userId missing lastName"),
                        businessName = tutorDao.businessName,
                        dateOfBirth = DateTimeFormatter.ISO_LOCAL_DATE.format(tutorDao.dateOfBirth),
                        accounts = listOf(account.toStarlingAccount(user.email))
                )
            }
        }

        logger.info { "Created payee ${response.payeeUid} updating tutor" }
        TutorDetailRepository.updatePayeeUuid(payeeUid = response.payeeUid, tutorId = tutorDao.tutorId)
    } catch (e: ClientRequestException) {
        val response = String(e.response.readBytes())
        logger.error { "Error creating starling payee $response" }
        throw e
    }

}

suspend fun String.getPayeeDetail(): PayeeAccountResource? {
    val response = client().use { client ->
        client.get<StarlingPayees> {
            url("${Properties.starlingUrl}/api/v2/payees")
            header("Authorization", "Bearer ${Properties.starlingToken}")
            contentType(ContentType.Application.Json)
        }
    }

    return response.payees.find { it.payeeUid == this }?.accounts?.first()?.let { account ->
        PayeeAccountResource(
                accountIdentifier = account.accountIdentifier.maskAccount(),
                bankIdentifier = account.bankIdentifier.maskBank()
        )
    }
}

@KtorExperimentalAPI
suspend fun deletePayee(userId: Int) {
    val tutorDao = TutorDetailRepository.getByUserId(userId)
            ?: throw Exception("Could not find tutor details for userId $userId")

    if (tutorDao.payeeUuid == null) {
        throw BadRequestException("Could not find tutor payee information. Please try refreshing.")
    }
    try {
        client().use { client ->
            client.delete<HttpResponse> {
                url("${Properties.starlingUrl}/api/v2/payees/${tutorDao.payeeUuid}")
                header("Authorization", "Bearer ${Properties.starlingToken}")
                contentType(ContentType.Application.Json)
            }
        }

        TutorDetailRepository.updatePayeeUuid(payeeUid = null, tutorId = tutorDao.tutorId)
    } catch (e: ClientRequestException) {
        val response = String(e.response.readBytes())
        logger.error { "Error deleting starling payee $response" }
        throw e
    }
}

fun PayeeAccountInsert.toStarlingAccount(email: String): StarlingPayeeAccountInsert {
    return StarlingPayeeAccountInsert(
            description = email,
            defaultAccount = true,
            countryCode = "GB",
            accountIdentifier = accountIdentifier,
            bankIdentifier = bankIdentifier,
            bankIdentifierType = "SORT_CODE"
    )
}

private fun String.maskBank(): String {
    return this.replaceRange(0, 4, "**-**-")
}

private fun String.maskAccount(): String {
    return this.replaceRange(0, 6, "******")
}