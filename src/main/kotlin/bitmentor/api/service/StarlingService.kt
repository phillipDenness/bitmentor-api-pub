package bitmentor.api.service

import bitmentor.api.config.Properties.starlingWebhookSecret

import java.security.MessageDigest
import java.util.*

fun isStarlingPayloadValid(signature: String, payload: String): Boolean {
    val messageDigest = MessageDigest.getInstance("SHA-512")
    messageDigest.update(starlingWebhookSecret.toByteArray())
    messageDigest.update(payload.toByteArray())
    val calculatedSignature: String = Base64.getEncoder().encodeToString(messageDigest.digest())
    return calculatedSignature == signature
}