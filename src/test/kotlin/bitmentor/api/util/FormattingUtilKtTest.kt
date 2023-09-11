package bitmentor.api.util

import junit.framework.Assert.assertEquals
import org.junit.Test


open class FormattingUtilKtTest: SafeMockingTestClass() {

    @Test
    fun shouldFormatLessThanAPound() {
        val formatted = 10L.formatPenceToPoundCurrency()
        assertEquals("0.10", formatted)
    }

    @Test
    fun shouldFormatPenceToSterling() {
        val formatted = 100L.formatPenceToPoundCurrency()
        assertEquals("1.00", formatted)
    }

    @Test
    fun shouldFormatPenceToSterlingWithDoubleAndDecimal() {
        val formatted = 1050L.formatPenceToPoundCurrency()
        assertEquals("10.50", formatted)
    }
}