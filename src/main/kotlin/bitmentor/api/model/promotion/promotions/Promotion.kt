package bitmentor.api.model.promotion.promotions

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    property = "code",
    include = JsonTypeInfo.As.EXISTING_PROPERTY
) @JsonSubTypes(
    value = [
        JsonSubTypes.Type(value = Loyalty25Promotion::class, name = "LOYALTY25"),
        JsonSubTypes.Type(value = TrialPromotion::class, name = "TRIAL")
    ]
)
abstract class Promotion(open val code: String) {

    abstract val description: String
    abstract val discount: Int

    abstract fun isValid(id: Int): Boolean
}