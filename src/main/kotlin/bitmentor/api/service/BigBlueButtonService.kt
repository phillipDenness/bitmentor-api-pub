package bitmentor.api.service

import bitmentor.api.config.Properties
import bitmentor.api.config.bbbClient
import bitmentor.api.model.bigbluebutton.BbbCreate
import bitmentor.api.model.bigbluebutton.BbbMeetingInsert
import bitmentor.api.model.bigbluebutton.BbbReturnCodes
import bitmentor.api.model.reminder.payloads.RemindOpenMeeting
import bitmentor.api.repository.BbbRepository
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.util.*
import mu.KotlinLogging
import org.apache.commons.codec.digest.DigestUtils

private val logger = KotlinLogging.logger {}

internal val kotlinXmlMapper = XmlMapper(JacksonXmlModule().apply {
    setDefaultUseWrapper(false)
}).registerKotlinModule()
        .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

internal inline fun <reified T : Any> parseAs(resource: String): T {
    return kotlinXmlMapper.readValue(resource)
}

fun getChecksum(queryString: String): String {
    return DigestUtils.shaHex(queryString + Properties.bbbSecret)
}

@KtorExperimentalAPI
suspend fun RemindOpenMeeting.createMeeting(): BbbCreate {
    val meetingName = java.net.URLEncoder.encode("${lessonResource.topic.name} lesson. Tutor: ${lessonResource.tutorDisplayName}", "utf-8")
    val bannerText = java.net.URLEncoder.encode("bitmentor.co.uk", "utf-8")
    val bannerColor = java.net.URLEncoder.encode("#214659", "utf-8")
    val meetingId = "meet${lessonResource.id}"

    val queryString = "name=$meetingName&meetingID=$meetingId&duration=$duration&bannerColor=$bannerColor&bannerText=$bannerText"
    val checksum = getChecksum("create$queryString")

    logger.info { "Sending create request ${Properties.bbbUrl}create?$queryString&checksum=$checksum" }
    var response: String? = null
    try {
        response = bbbClient().use { client ->
            val res = client.get<String>("${Properties.bbbUrl}create?$queryString&checksum=$checksum")
            client.close()
            res
        }
    } catch (e: ServerResponseException) {}

    val meetingResponse = parseAs<BbbCreate>(response!!)
    if (meetingResponse.returncode == BbbReturnCodes.SUCCESS) {
        logger.info { "Successfully created meeting with moderator pw: ${meetingResponse.moderatorPW}" }
        BbbRepository.createMeeting(BbbMeetingInsert(
                meetingId = meetingId,
                moderatorPw = meetingResponse.moderatorPW!!,
                duration = duration,
                lessonId = lessonResource.id
        ))
    } else {
        if (meetingResponse.messageKey != "idNotUnique") {
            logger.error { "Error creating meeting: $this. Message key: ${meetingResponse.messageKey}" }
            throw RuntimeException()
        }
    }

    return meetingResponse
}

fun RemindOpenMeeting.createMeetingJoinLinks(userName: String, password: String): String {
    val fullName = java.net.URLEncoder.encode(userName, "utf-8")
    val meetingId = "meet${lessonResource.id}"
    val queryString = "fullName=$fullName&meetingID=$meetingId&password=$password"
    val checksum = getChecksum("join$queryString")

    val url = "${Properties.bbbUrl}join?$queryString&checksum=$checksum"
    logger.info { "Returning join request $url" }
    return url
}