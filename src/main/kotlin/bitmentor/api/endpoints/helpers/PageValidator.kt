package bitmentor.api.endpoints.helpers

import io.ktor.features.BadRequestException
import io.ktor.http.Parameters
import io.ktor.util.KtorExperimentalAPI

@KtorExperimentalAPI
fun validatePaging(queryParameters: Parameters): Pair<Int, Int> {
    try {
        val page: Int = queryParameters["page"]?.toInt()?.takeIf { it >= 0 }
                ?: throw BadRequestException("Page must be supplied")
        val size: Int = queryParameters["size"]?.toInt()?.takeIf { it >= 0 }
                ?: throw BadRequestException("Size must be supplied")

        return Pair(page, size)
    } catch (e: NumberFormatException) {
        throw BadRequestException("Page & Size must be an integer : $e")
    }
}
