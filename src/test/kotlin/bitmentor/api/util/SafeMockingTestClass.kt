package bitmentor.api.util

import bitmentor.api.service.authenticate
import bitmentor.api.service.doMigrate
import bitmentor.api.service.extractUserId
import io.mockk.*
import org.junit.Before

open class SafeMockingTestClass {
    @Before
    fun mockFlyway() {
        unmockkAll()
        mockkStatic("bitmentor.api.service.FlywayServiceKt")
        mockkStatic("bitmentor.api.service.AuthServiceKt")
        every { authenticate(any()) } just Runs
        every { extractUserId(any()) } returns 1
        every { doMigrate() } just Runs
    }
}