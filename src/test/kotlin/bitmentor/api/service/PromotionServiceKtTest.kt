package bitmentor.api.service

import bitmentor.api.util.SafeMockingTestClass
import org.junit.Test
import kotlin.test.assertEquals

internal class PromotionServiceKtTest: SafeMockingTestClass() {
    @Test
    fun getAllPromotionsShouldReturnEveryPromotion() {
        val promos = getAllPromotions()

        assertEquals(2, promos.size)
    }

    @Test
    fun calculateDiscountShouldDeductPercentageFromLessonCost() {
        val discount = calculateDiscount(10.0, 25)
        assertEquals(2.5, discount)
    }

}