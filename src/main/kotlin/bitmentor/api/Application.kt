package bitmentor.api

import bitmentor.api.config.Properties.clientHostname
import bitmentor.api.config.Properties.clientProtocol
import bitmentor.api.config.Properties.server_port
import bitmentor.api.config.SharedJdbi
import bitmentor.api.endpoints.*
import bitmentor.api.exceptions.UserAccountIncompleteException
import bitmentor.api.exceptions.UserAuthenticationException
import bitmentor.api.exceptions.UserAuthorizationException
import bitmentor.api.service.authenticate
import bitmentor.api.service.doMigrate
import bitmentor.api.service.initReminderHandler
import bitmentor.api.service.initTopicMetaHandler
import bitmentor.api.storage.createPublicImageBucket
import bitmentor.api.storage.createTutorVerificationBucket
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.datatype.jsr310.ser.ZonedDateTimeSerializer
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.jackson.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.jetty.*
import io.ktor.util.*
import kotlinx.coroutines.launch
import org.springframework.hateoas.mediatype.hal.Jackson2HalModule
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter.ISO_INSTANT

@KtorExperimentalAPI
fun Application.module() {
//    val appMicrometerRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
//    install(MicrometerMetrics) {
//        registry = appMicrometerRegistry
//    }

    install(CORS) {
        method(HttpMethod.Options)
        method(HttpMethod.Post)
        method(HttpMethod.Put)
        method(HttpMethod.Delete)
        host(clientHostname, listOf(clientProtocol), listOf("www"))
        header("X-Application-ID")
        header("Accept")
        header("Authorization")
        header("content-type")
        header("Referer")
        header("Sec-Fetch-Dest")
        header("User-Agent")
    }

    install(StatusPages) {
        exception<UserAuthenticationException> { cause ->
            call.respond(HttpStatusCode.Unauthorized, mapOf("errors" to listOf(cause.message)))
        }
        exception<UserAuthorizationException> {
            call.respond(HttpStatusCode.Unauthorized)
        }
        exception<BadRequestException> { cause ->
            cause.message?.let { call.respond(HttpStatusCode.BadRequest, mapOf("errors" to listOf(it))) }
                ?: call.respond(HttpStatusCode.BadRequest)
        }
        exception<UserAccountIncompleteException> {
            call.respond(HttpStatusCode.BadRequest, mapOf("errors" to listOf("You must complete your registration.")))
        }
        exception<NotFoundException> { cause ->
            call.respond(HttpStatusCode.NotFound, mapOf("errors" to listOf(cause.message)))
        }
         exception<JsonMappingException> {
            call.respond(HttpStatusCode.BadRequest, mapOf("errors" to listOf("Malformed payload")))
        }

    }
    install(ContentNegotiation) {

        val javaTimeModule = JavaTimeModule().addSerializer(
            ZonedDateTime::class.java,
            ZonedDateTimeSerializer(ISO_INSTANT)
        )

        jackson {
            registerModule(Jdk8Module())
            registerModule(javaTimeModule)
            registerModule(ParameterNamesModule())
            registerModule(Jackson2HalModule())
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        }
    }

    install(Routing) {
        routing {
            authenticate(this)
            initialiseInfraEndpoints()
            initialiseAuthEndpoints()
            initialiseUserEndpoints()
            initialiseTutorEndpoints()
            initialiseEnquiriesEndpoints()
            initialiseMessageEndpoints()
            initialiseNotificationEndpoints()
            initialiseLessonEndpoints()
            initialiseReviewEndpoints()
            initialiseFileEndpoints()
            initialiseTopicEndpoints()
//            initialiseBbbEndpoints()
            initialisePaymentEndpoints()
            initialiseOrderEndpoints()
            initialisePayoutEndpoints()
            initialiseHelpEndpoints()
            initialisePayeeEndpoints()
            initialiseAdminEndpoints()
            initialiseTutorDetailEndpoints()
            initialisePromotionEndpoints()
            initialiseStarlingEndpoints()
            initialiseTutorShortListEndpoints()
            initialisePaypalEndpoints()
            initialiseStudentEndpoints()
        }
    }

    launch {
        doMigrate()
        createPublicImageBucket()
        createTutorVerificationBucket()
    }

    launch {
        initReminderHandler()
    }

    launch {
        initTopicMetaHandler()
    }
}

@KtorExperimentalAPI
fun main() {
    try {
        embeddedServer(Jetty, server_port, module = Application::module).start(wait = true)
    } finally {
        SharedJdbi.hikariDs().close()
    }
}
