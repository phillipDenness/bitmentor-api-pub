package bitmentor.api.endpoints.helpers

import bitmentor.api.model.user.Location
import bitmentor.api.model.user.UpdateUserRequest
import io.ktor.features.*
import io.ktor.util.*

@KtorExperimentalAPI
fun UpdateUserRequest.validate() {
    if (this.displayName == "" || this.firstName == "" || this.lastName == "")  {
        throw BadRequestException("Malformed payload")
    }
}

@KtorExperimentalAPI
fun Location.validate() {
    if (this.city == "" || this.country == "" || this.postcode == "" || this.firstLineAddress == "") {
        throw BadRequestException("Malformed payload")
    }
}
