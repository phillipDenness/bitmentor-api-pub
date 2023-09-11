package bitmentor.api.service

import bitmentor.api.util.SafeMockingTestClass
import io.ktor.util.*
import org.junit.Test
import kotlin.test.assertEquals

@KtorExperimentalAPI
class TopicMetaServiceKtTest : SafeMockingTestClass() {

    @Test
    fun `get TopicMetaData should return the newest data first`() {
//        val meta = getTopicMeta(5)
//        assertEquals(9517, meta?.totalJobs?.jobs)
    }

    @Test
    fun `calculateChange returns decrease percentage`() {
        val change = calculateChange(10, 20)
        assertEquals(-50, change)
    }

    @Test
    fun `calculateChange returns increase percentage`() {
        val change = calculateChange(20, 10)
        assertEquals(100, change)
    }
}
