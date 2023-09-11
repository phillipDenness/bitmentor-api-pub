package bitmentor.api.service

import bitmentor.api.config.GenericObjectMapper.getMapper
import bitmentor.api.config.Properties
import bitmentor.api.config.Properties.jwtSecretActivation
import bitmentor.api.config.Properties.jwtSecretForgot
import bitmentor.api.config.Properties.jwtSecretLogin
import bitmentor.api.email.EmailTemplate
import bitmentor.api.exceptions.UserAuthenticationException
import bitmentor.api.model.auth.*
import bitmentor.api.repository.TutorRepository
import bitmentor.api.repository.UserRepository
import bitmentor.api.repository.UserRepository.updatePasswordReset
import bitmentor.api.service.oauth.getGithubUser
import bitmentor.api.service.oauth.getLinkedInUser
import bitmentor.api.service.oauth.verifyGithubToken
import bitmentor.api.service.oauth.verifyLinkedInToken
import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTCreationException
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier
import com.google.api.client.http.apache.ApacheHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.*
import io.ktor.util.pipeline.*
import mu.KotlinLogging
import java.util.*


private val logger = KotlinLogging.logger {}
private val userAttributeKey = AttributeKey<String>("user")

const val ONE_MINUTE_IN_MILLIS: Long = 60000
const val ONE_DAY_IN_MILLIES: Long = 86400000

const val CLAIM = "userId"
const val AUTH_CLAIM = "authenticationType"

val activationVerifier: JWTVerifier = JWT.require(Algorithm.HMAC256(Properties.jwtSecretActivation))
    .withIssuer("auth0")
    .build() //Reusable verifier instance

val signinVerifier: JWTVerifier = JWT.require(Algorithm.HMAC256(Properties.jwtSecretLogin))
        .withIssuer("auth0")
        .build() //Reusable verifier instance

val resetVerifier: JWTVerifier = JWT.require(Algorithm.HMAC256(Properties.jwtSecretForgot))
        .withIssuer("auth0")
        .build() //Reusable verifier instance

val jacksonFactory = JacksonFactory()
val verifier: GoogleIdTokenVerifier = GoogleIdTokenVerifier.Builder(ApacheHttpTransport(), jacksonFactory) // Specify the CLIENT_ID of the app that accesses the backend:
    .setAudience(listOf(Properties.googleClientId))
    .build()

fun authenticate(route: Route) =
    route.intercept(ApplicationCallPipeline.Features) { verifyPermissions() }

suspend fun PipelineContext<Unit, ApplicationCall>.verifyPermissions() {
    if (endpointRequiresAuthorization(call.request.local.method, call.request.local.uri)) {
        call.request.headers["Authorization"]?.let {
            try {
                val decodedJWT = signinVerifier.verify(it)
                val userId = decodedJWT.getClaim(CLAIM).asInt()

                logger.info { "Storing userId in attributes : $userId" }
                if (isUserActive(userId)) {
                    call.attributes.put(userAttributeKey, userId.toString())
                } else {
                    logger.error { "UserId: $userId was not found in the DB" }
                    call.respond(HttpStatusCode.Unauthorized)
                    finish()
                }
            } catch (e: Exception) {
                logger.error { "Could not decode token: $it" }
                call.respond(HttpStatusCode.Unauthorized)
                finish()
            }
        } ?: call.respond(HttpStatusCode.Unauthorized).apply {
            logger.error { "Request did not have authorization header" }
            finish()
        }
    }
    proceed()
}

fun extractUserId(applicationCall: ApplicationCall): Int {
    return applicationCall.attributes[userAttributeKey].toInt()
}


fun signAndEmailReset(email: String): String {
    return UserRepository.findUserByEmail(email)?.let {user ->
        try {
            signResetToken(user.id).also { token ->
                updatePasswordReset(
                        userId = user.id,
                        token = token
                )
                sendResetEmail(
                        email = user.email,
                        token = token
                )
            }
        } catch (e: Exception) {
            logger.error { e }
            throw Exception("Exception while generating and updating user reset.")
        }
    } ?: throw UserAuthenticationException("Email address is not registered")

}

fun ResetPasswordRequest.verifyTokenAndReset() {
    val decodedJWT = resetVerifier.verify(this.token)

    try {
        val userId = decodedJWT.getClaim(CLAIM).asInt()
        UserRepository.updatePassword(
                userId = userId,
                password = password,
                token = token
        )
    } catch (e: Exception) {
        throw Exception("Exception occurred resetting user password")
    }
}

fun signActivation(user: UserInsert): String {
    try {
        val date = Calendar.getInstance()
        val t = date.timeInMillis
        val expiry120Minutes = Date(t + 120 * ONE_MINUTE_IN_MILLIS)
        val algorithm: Algorithm = Algorithm.HMAC256(jwtSecretActivation)

        return JWT.create()
            .withIssuer("auth0")
            .withClaim(CLAIM, jacksonObjectMapper().writeValueAsString(user))
            .withExpiresAt(expiry120Minutes)
            .sign(algorithm)

    } catch (exception: JWTCreationException) {
        //Invalid Signing configuration / Couldn't convert Claims.
        logger.error { "Exception during signing activation ${exception.message}" }
        throw exception
    }
}

fun UserActivationRequest.activateUser() {
    val decodedJWT = activationVerifier.verify(this.token)

    try {
        val userInsert = getMapper()
                .readValue(decodedJWT.getClaim(CLAIM).asString(), UserInsert::class.java)
        UserRepository.createUser(userInsert)

        sendWelcomeEmail(userInsert.email)
    } catch (e: Exception) {
        throw Exception("Exception occurred converting JWT into user")
    }
}

fun ChangePasswordRequest.verifyAndReset(userId: Int) {
    if (UserRepository.authenticateUserPassword(
                    email = "",
                    password = currentPassword,
                    userId = userId
            ) < 1) {
        throw UserAuthenticationException("User email and password does not match")
    }

    UserRepository.updatePassword(
            userId = userId,
            password = password,
            token = "",
            isVerified = true
    )
}

@KtorExperimentalAPI
fun UserLoginRequest.authenticate(): UserLoginResponse {
    if (UserRepository.authenticateUserPassword(
            email = email,
            password = password
    ) < 1) {
        throw UserAuthenticationException("User email and password does not match")
    }

    return UserRepository.findUserByEmail(email)
            ?.let {
                val isCompleted = it.displayName?.let { true } ?: false
                UserLoginResponse(
                        token = signLoginToken(it.id, LoginTypes.STANDARD),
                        user = UserSession(
                                id = it.id,
                                tutorId = TutorRepository.findTutorByUserId(it.id)?.id,
                            completedRegistration = isCompleted,
                                loginType = LoginTypes.STANDARD
                        ))
            } ?: throw UserAuthenticationException("User not found in database")
}

fun isUserActive(email: String): Boolean {
    try {
        return UserRepository.findUserByEmail(email)?.let {
            true
        } ?: false
    } catch (e: Exception) {
        logger.error { e.message }
        throw Exception("Exception occurred finding user by email")
    }
}

fun isUserActive(userId: Int): Boolean {
    try {
        return UserRepository.findUserById(userId)?.let {
            UserRepository.updateLastActive(userId)
            true
        } ?: false
    } catch (e: Exception) {
        logger.error { e.message }
        throw Exception("Exception occurred finding user by email")
    }
}

@KtorExperimentalAPI
fun loginWithGoogle(token: String): UserLoginResponse {
    val googleIdToken = verifyTokenRetry(token)
        ?: throw UserAuthenticationException("Invalid google token.")

    val payload = googleIdToken.payload
    val emailVerified = payload.emailVerified

    if (!emailVerified) {
        throw UserAuthenticationException("Google email is unverified")
    }

    val email = payload.email
    return oauthLoginRegister(email)
}

fun verifyTokenRetry(token: String): GoogleIdToken? {
    return try {
        verifier.verify(token)
    } catch (e: Exception) {
        logger.warn { "Exception caught with google verifier. Retrying $e" }
        verifier.verify(token)
    }
}

@KtorExperimentalAPI
suspend fun loginWithGithub(token: String): UserLoginResponse {
    val accessToken = verifyGithubToken(token)
    val githubAccount = getGithubUser(accessToken)

    if (!githubAccount.verified) {
        throw UserAuthenticationException("Github email is unverified")
    }

    val email = githubAccount.email
    return oauthLoginRegister(email)
}


@KtorExperimentalAPI
suspend fun loginWithLinkedIn(token: String): UserLoginResponse {
    val accessToken = verifyLinkedInToken(token)
    val email = getLinkedInUser(accessToken)
    return oauthLoginRegister(email)
}


fun sendActivationEmail(email: String, token: String) {
    EmailTemplate(
            message = """
                <p>Hi</p>
                <b>Welcome and thanks for setting up an account with Bitmentor</b>
                <p>Please click the link below to activate your account</p>
                <p>${Properties.clientUrl}${Properties.emailActivateRedirectUrl}/$token</p>
                <p>If you can't click the link, just copy and paste this URL into a web browser.</p>
                """,
            subject = "Activate your Bitmentor Account"
    ).send(email)
}

@KtorExperimentalAPI
private fun oauthLoginRegister(email: String): UserLoginResponse {
    val user = UserRepository.findUserByEmail(email)
    if (user != null) {
        logger.info { "Found existing user. Return email and token $email" }
        val isCompleted = user.displayName?.let { true } ?: false
        return UserLoginResponse(
                token = signLoginToken(user.id, LoginTypes.GOOGLE),
                user = UserSession(
                        id = user.id,
                        tutorId = TutorRepository.findTutorByUserId(user.id)?.id,
                        completedRegistration = isCompleted,
                        loginType = LoginTypes.GOOGLE
                ))
    }

    val password = email + jwtSecretLogin
    logger.info { "Generating new user and return email and token $email" }
    return UserRepository.createUser(UserInsert(
            password = password,
            email = email
    )).let {
        sendWelcomeEmail(email)
        UserLoginResponse(
                token = signLoginToken(it, LoginTypes.GOOGLE),
                user = UserSession(
                        id = it,
                        tutorId = 1,
                        completedRegistration = false,
                        loginType = LoginTypes.GOOGLE
                )
        )
    }
}

private fun signResetToken(id: Int): String {
    try {
        val date = Calendar.getInstance()
        val t = date.timeInMillis
        val expiry10Minutes = Date(t + 10 * ONE_MINUTE_IN_MILLIS)
        val algorithm: Algorithm = Algorithm.HMAC256(jwtSecretForgot)

        return JWT.create()
                .withIssuer("auth0")
                .withClaim(CLAIM, id)
                .withExpiresAt(expiry10Minutes)
                .sign(algorithm)
    } catch (exception: JWTCreationException) {
            //Invalid Signing configuration / Couldn't convert Claims.
            logger.error { "Exception during signing reset token ${exception.message}" }
            throw exception
    }
}

private fun signLoginToken(id: Int, loginType: LoginTypes): String {
    val date = Calendar.getInstance()
    val t = date.timeInMillis
    val expiry7Days = Date(t + 7 * ONE_DAY_IN_MILLIES)
    val algorithm: Algorithm = Algorithm.HMAC256(jwtSecretLogin)

    return JWT.create()
            .withIssuer("auth0")
            .withClaim(CLAIM, id)
            .withClaim(AUTH_CLAIM, loginType.toString())
            .withExpiresAt(expiry7Days)
            .sign(algorithm)
}

private fun sendResetEmail(email: String, token: String) {
    EmailTemplate(
            message = """
        <p>Hi</p>
        <p>We got a request to reset your password. Please click on the link below to reset your password using our secure server.</p>
        <p>${Properties.clientUrl}${Properties.emailResetRedirectUrl}/$token</p>
        <b>Need some help?</b>
        <p>If we may be of further help, or if you have any comments or suggestions about our service, please visit ${Properties.clientUrl}/help-center</p>
        """,
            subject = "Password Reset"
    ).send(email)
}


private fun sendWelcomeEmail(email: String) {
    EmailTemplate(
            message = """
        <p>Welcome</p>
        <b>Thank you for registering at Bitmentor</b>
        <p>We hope you enjoy your online tutoring experience with a Bitmentor account and we look forward to hearing from you again soon.</p>
        """,
            subject = "Welcome To Bitmentor"
    ).send(email)
}

private fun endpointRequiresAuthorization(method: HttpMethod, path: String): Boolean {
    return (method == HttpMethod.Get || method == HttpMethod.Post || method == HttpMethod.Put || method == HttpMethod.Delete) &&
            !path.contains("/public/")
}
