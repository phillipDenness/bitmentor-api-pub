package bitmentor.api.model.promotion

import bitmentor.api.model.promotion.promotions.Promotion

data class ApplyPromoResponse(
    val promotion: Promotion,
    val oldTotal: Double,
    val discount: Double,
    val total: Double
)