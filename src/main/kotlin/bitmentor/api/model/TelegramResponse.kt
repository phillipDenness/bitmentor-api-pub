package bitmentor.api.model
import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class TelegramResponse(
    val ok: String
)