package bitmentor.api.endpoints

import bitmentor.api.module
import bitmentor.api.util.SafeMockingTestClass
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.ktor.util.*
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@KtorExperimentalAPI
class PromotionEndpointsKtTest: SafeMockingTestClass() {

    @Test
    fun `get promotions contract endpoint should return 200 when successful request made`() =
            withTestApplication(Application::module) {
                runBlocking {
                    with(
                        handleRequest(HttpMethod.Get, "promotions") {}
                    ) {
                        assertEquals(HttpStatusCode.OK, response.status())
                        val body = response.content.toString()
                        assertTrue(body.contains("\"description\":"))
                        assertTrue(body.contains("\"code\":"))
                    }
                }
            }

}