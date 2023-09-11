package bitmentor.api.model.bigbluebutton

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonRootName

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonRootName("response")
data class BbbCreate(
    val returncode: BbbReturnCodes,
    val moderatorPW: String? = null,
    val messageKey: String?
)