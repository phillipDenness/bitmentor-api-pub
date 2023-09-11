package bitmentor.api.util

import java.math.BigDecimal
import java.math.RoundingMode

fun Long.formatPenceToPoundCurrency(): String {
        return (BigDecimal(this).divide(BigDecimal(100))).setScale(2, RoundingMode.HALF_EVEN).toPlainString()
}