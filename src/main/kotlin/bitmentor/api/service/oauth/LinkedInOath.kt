package bitmentor.api.service.oauth

import bitmentor.api.config.Properties
import bitmentor.api.config.client
import bitmentor.api.exceptions.UserAuthenticationException
import bitmentor.api.model.auth.linkedin.LinkedinAccessResponse
import bitmentor.api.model.auth.linkedin.LinkedinEmailResponse
import io.ktor.client.request.*
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

suspend fun verifyLinkedInToken(code: String): String {
    val url = "https://www.linkedin.com/oauth/v2/accessToken?grant_type=authorization_code&client_id=${Properties.linkedinClientId}&client_secret=${Properties.linkedinClientSecret}&code=$code&redirect_uri=${Properties.clientUrl}/auth/linkedin"
    try {
        val response = client().use { client ->
            client.get<LinkedinAccessResponse> {
                url(url)
            }
        }
        return response.access_token
    } catch (e: Exception) {
        logger.error { "Invalid linkedin token $e" }
        throw UserAuthenticationException("Invalid linkedin token")
    }
}

suspend fun getLinkedInUser(token: String): String {
    logger.info { token }
    return client().use { client ->
        client.get<LinkedinEmailResponse> {
            url("https://api.linkedin.com/v2/emailAddress?q=members&projection=(elements*(handle~))")
            header("Authorization", "Bearer $token")
        }
    }.elements.first().handle.emailAddress
}