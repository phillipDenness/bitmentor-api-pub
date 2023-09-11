package bitmentor.api.model.user

data class Location(
        val firstLineAddress: String,
        val secondLineAddress: String,
        val city: String,
        val postcode: String,
        val country: String
)
