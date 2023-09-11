package bitmentor.api.service.oauth

import bitmentor.api.config.Properties
import bitmentor.api.config.client
import bitmentor.api.exceptions.UserAuthenticationException
import bitmentor.api.model.auth.github.GithubAccessResponse
import bitmentor.api.model.auth.github.GithubEmailResponse
import io.ktor.client.request.*
import io.ktor.http.*
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

suspend fun verifyGithubToken(code: String): String {
    val url = "https://github.com/login/oauth/access_token?client_id=${Properties.githubClientId}&redirect_uri=&client_secret=${Properties.githubClientSecret}&code=$code"
    try {
        val response = client().use { client ->
            client.post<GithubAccessResponse> {
                url(url)
                contentType(ContentType.Application.Json)
            }
        }
        return response.access_token
    } catch (e: Exception) {
        logger.error { "Invalid github token $e" }
        throw UserAuthenticationException("Invalid github token")
    }
}

suspend fun getGithubUser(token: String): GithubEmailResponse {
    return client().use { client ->
        client.get<List<GithubEmailResponse>> {
            url("https://api.github.com/user/emails")
            header("Authorization", "token $token")
        }
    }.find { it.primary }!!
}