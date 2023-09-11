package bitmentor.api.endpoints


import bitmentor.api.endpoints.helpers.checkEmail
import bitmentor.api.endpoints.helpers.isValidPassword
import bitmentor.api.exceptions.InvalidPasswordException
import bitmentor.api.exceptions.InvalidRegexException
import bitmentor.api.exceptions.UserAuthenticationException
import bitmentor.api.model.auth.*
import bitmentor.api.model.auth.github.GithubLoginRequest
import bitmentor.api.model.auth.linkedin.LinkedInLoginRequest
import bitmentor.api.service.*
import com.auth0.jwt.exceptions.JWTVerificationException
import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.*
import mu.KotlinLogging
import java.util.concurrent.TimeUnit


private val logger = KotlinLogging.logger {}

@KtorExperimentalAPI
fun Routing.initialiseAuthEndpoints() = apply {
    post("/public/signup") {
        try {
            val userInsert = call.receive<UserInsert>()
            logger.info { "Sign Up ${userInsert.email}" }

            checkEmail(userInsert.email)
            isValidPassword(userInsert.password)

            if (!isUserActive(userInsert.email)) {
                val token = signActivation(userInsert)
                sendActivationEmail(userInsert.email, token)
                call.respond(HttpStatusCode.OK)
            } else {
                call.respond(HttpStatusCode.BadRequest, mapOf("errors" to listOf("Email is already registered. Please try and login")))
            }

        } catch (e: InvalidPasswordException) {
            call.respond(HttpStatusCode.BadRequest, mapOf("errors" to listOf(e.message)))
        } catch (e: InvalidRegexException) {
            call.respond(HttpStatusCode.BadRequest, mapOf("errors" to listOf(e.message)))
        } catch (e: Exception) {
            logger.error { "Exception signing up a user ${e.message}" }
            call.respond(HttpStatusCode.InternalServerError, mapOf("errors" to listOf("${e.message}")))
        }
    }

    post("/public/activate") {
        try {
            val activation = call.receive<UserActivationRequest>()
            logger.info { "Activation request" }

            activation.activateUser()
            call.respond(HttpStatusCode.Created)

        } catch (e: JWTVerificationException) {
            logger.info { "Token could not be verified" }
            call.respond(
                HttpStatusCode.Unauthorized,
                mapOf("errors" to listOf("Token may have expired. Sign up again"))
            )
        } catch (e: Exception) {
            logger.error { "Exception activating a user ${e.message}" }
            call.respond(HttpStatusCode.InternalServerError, mapOf("errors" to listOf("${e.message}")))
        }
    }

    val retryCache: Cache<String, Int> = Caffeine.newBuilder()
        .expireAfterWrite(15, TimeUnit.MINUTES)
        .build()
    post("/public/login") {

        val userlogin = call.receive<UserLoginRequest>()

        try {
            logger.info { "User login ${userlogin.email}" }

            val retries = retryCache.getIfPresent(userlogin.email) ?: 0

            if (retries > 5) {
                logger.warn { "User ${userlogin.email} has exceeded their retry attempts" }
                call.respond(HttpStatusCode.BadRequest, mapOf("errors" to listOf("Retry limit exceeded. Wait 15 minutes and try again.")))
            } else {
                checkEmail(userlogin.email)
                isValidPassword(userlogin.password)

                call.respond(HttpStatusCode.OK, userlogin.authenticate())
            }
        } catch (e: InvalidRegexException) {
            call.respond(HttpStatusCode.BadRequest, mapOf("errors" to listOf(e.message)))
        } catch (e: UserAuthenticationException) {
            val retries = retryCache.getIfPresent(userlogin.email) ?: 0
            retryCache.put(userlogin.email, retries + 1)

            val message = if (retries > 1) {
                "${e.message} you have ${5 - retries} retries remaining"
            } else {
                e.message
            }
            call.respond(HttpStatusCode.Unauthorized, mapOf("errors" to listOf(message)))
        } catch (e: JWTVerificationException) {
            logger.info { "Token could not be verified" }
            call.respond(HttpStatusCode.Unauthorized)
        } catch (e: Exception) {
            logger.error { "Exception activating a user ${e.message}" }
            call.respond(HttpStatusCode.InternalServerError, mapOf("errors" to listOf("${e.message}")))
        }
    }

    post("/public/google-login") {
        try {
            val userToken = call.receive<GoogleLoginRequest>()

            call.respond(HttpStatusCode.OK, loginWithGoogle(userToken.idToken))
        } catch (e: UserAuthenticationException) {
            call.respond(HttpStatusCode.Unauthorized, mapOf("errors" to listOf(e.message)))
        } catch (e: UserAuthenticationException) {
            logger.info { "Token could not be verified" }
            call.respond(HttpStatusCode.Unauthorized)
        } catch (e: Exception) {
            logger.error { "Exception with google login ${e.message}" }
            call.respond(HttpStatusCode.InternalServerError, mapOf("errors" to listOf("Login failed. Please try again.")))
        }
    }

    post("/public/linkedin-login") {
        try {
            val userToken = call.receive<LinkedInLoginRequest>()
            call.respond(HttpStatusCode.OK, loginWithLinkedIn(userToken.code))
        } catch (e: UserAuthenticationException) {
            call.respond(HttpStatusCode.Unauthorized, mapOf("errors" to listOf(e.message)))
        } catch (e: UserAuthenticationException) {
            logger.info { "Token could not be verified" }
            call.respond(HttpStatusCode.Unauthorized)
        } catch (e: Exception) {
            logger.error { "Exception with linkedin login ${e.message}" }
            call.respond(HttpStatusCode.InternalServerError, mapOf("errors" to listOf("${e.message}")))
        }
    }

    post("/public/github-login") {
        try {
            val userToken = call.receive<GithubLoginRequest>()

            logger.info { userToken }
            call.respond(HttpStatusCode.OK, loginWithGithub(userToken.code))
        } catch (e: UserAuthenticationException) {
            call.respond(HttpStatusCode.Unauthorized, mapOf("errors" to listOf(e.message)))
        } catch (e: JWTVerificationException) {
            logger.info { "Token could not be verified" }
            call.respond(HttpStatusCode.Unauthorized)
        } catch (e: Exception) {
            logger.error { "Exception with github login ${e.message}" }
            call.respond(HttpStatusCode.InternalServerError, mapOf("errors" to listOf("${e.message}")))
        }
    }

    put("/public/forgot-password") {
        try {
            val passwordForgot = call.receive<ForgotPasswordRequest>()
            logger.info { "Password forgot" }

            checkEmail(passwordForgot.email)

            signAndEmailReset(passwordForgot.email)

            call.respond(HttpStatusCode.OK)

        } catch (e: UserAuthenticationException) {
            call.respond(HttpStatusCode.Unauthorized, mapOf("errors" to listOf(e.message)))
        } catch (e: JWTVerificationException) {
            logger.info { "Token could not be verified" }
            call.respond(HttpStatusCode.Unauthorized)
        } catch (e: Exception) {
            logger.error { "Exception activating a user ${e.message}" }
            call.respond(HttpStatusCode.InternalServerError, mapOf("errors" to listOf("${e.message}")))
        }
    }

    put("/public/reset-password") {
        try {
            val passwordReset = call.receive<ResetPasswordRequest>()
            logger.info { "Password reset" }
            isValidPassword(passwordReset.password)
            passwordReset.verifyTokenAndReset()

            call.respond(HttpStatusCode.OK)
        } catch (e: InvalidPasswordException) {
            call.respond(HttpStatusCode.BadRequest, mapOf("errors" to listOf(e.message)))
        } catch (e: UserAuthenticationException) {
            call.respond(HttpStatusCode.Unauthorized, mapOf("errors" to listOf(e.message)))
        } catch (e: InvalidRegexException) {
            call.respond(HttpStatusCode.BadRequest, mapOf("errors" to listOf("${e.message}")))
        } catch (e: JWTVerificationException) {
            logger.info { "Token could not be verified" }
            call.respond(HttpStatusCode.Unauthorized)
        } catch (e: Exception) {
            logger.error { "Exception activating a user ${e.message}" }
            call.respond(HttpStatusCode.InternalServerError, mapOf("errors" to listOf("${e.message}")))
        }
    }

    put("/reset-password") {
        try {
            val passwordReset = call.receive<ChangePasswordRequest>()

            val senderId = extractUserId(call)

            logger.info { "Password reset for user $senderId" }
            isValidPassword(passwordReset.password)
            isValidPassword(passwordReset.currentPassword)

            if (passwordReset.currentPassword == passwordReset.password) {
                call.respond(HttpStatusCode.BadRequest, mapOf("errors" to listOf("New password must be different")))
            } else {
                passwordReset.verifyAndReset(senderId)
                call.respond(HttpStatusCode.OK)
            }
        } catch (e: InvalidPasswordException) {
            call.respond(HttpStatusCode.BadRequest, mapOf("errors" to listOf(e.message)))
        } catch (e: UserAuthenticationException) {
            call.respond(HttpStatusCode.Unauthorized, mapOf("errors" to listOf(e.message)))
        } catch (e: InvalidRegexException) {
            call.respond(HttpStatusCode.BadRequest, mapOf("errors" to listOf("${e.message}")))
        } catch (e: JWTVerificationException) {
            logger.info { "Token could not be verified" }
            call.respond(HttpStatusCode.Unauthorized)
        } catch (e: Exception) {
            logger.error { "Exception resetting a user ${e.message}" }
            call.respond(HttpStatusCode.InternalServerError, mapOf("errors" to listOf("${e.message}")))
        }
    }

    get("/auth-info") {
        val senderId = extractUserId(call)

        call.respond(UserInfo(
                userId = senderId,
                tutorId = getTutorId(senderId),
                completedRegistration = isUserComplete(senderId)
        ))
    }
}
