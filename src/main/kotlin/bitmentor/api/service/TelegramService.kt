package bitmentor.api.service

import bitmentor.api.config.Properties
import bitmentor.api.config.client
import bitmentor.api.model.TelegramResponse
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

suspend fun sendBotMessage(message: String) {
    try {
        client().use { client ->
            client.get<TelegramResponse>("https://api.telegram.org/bot${Properties.telegramApiToken}/sendMessage?chat_id=${Properties.telegramChatId}&text=$message&parse_mode=html")
            client.close()
        }
    } catch (ex: ClientRequestException) {
        val response = String(ex.response.readBytes())
        logger.error { "ClientRequestException while sending telegram message $response" }
    }
}