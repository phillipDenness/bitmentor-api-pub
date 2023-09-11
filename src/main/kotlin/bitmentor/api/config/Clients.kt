package bitmentor.api.config

import bitmentor.api.service.getAccessCode
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.client.*
import io.ktor.client.features.auth.*
import io.ktor.client.features.auth.providers.*
import io.ktor.client.features.json.*
import io.ktor.http.*
import io.ktor.util.*
import org.springframework.hateoas.mediatype.hal.Jackson2HalModule


fun client() = HttpClient {
    install(JsonFeature) {
        serializer = JacksonSerializer {
            this.registerModule(JavaTimeModule()).registerModule(Jackson2HalModule())
        }
    }
}

@KtorExperimentalAPI
fun bbbClient() = HttpClient {
    Json {
        serializer = XMLSerializer()
        acceptContentTypes = listOf(ContentType.Application.Xml)
    }
}

fun reedClient() = HttpClient {
    install(JsonFeature) {
        serializer = JacksonSerializer {
            this.registerModule(JavaTimeModule()).registerModule(Jackson2HalModule())
        }
    }
    install(Auth) {
        basic {
            sendWithoutRequest = true
            username = Properties.reedApiToken
            password = ""
        }
    }
}


fun paypalClient() = HttpClient {
    install(JsonFeature) {
        serializer = JacksonSerializer {
            this.registerModule(JavaTimeModule()).registerModule(Jackson2HalModule())
        }
    }
    headersOf(HttpHeaders.Authorization, getAccessCode())
}