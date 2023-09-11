package bitmentor.api.model.promotion

import bitmentor.api.model.promotion.promotions.Promotion

data class PromotionsResource(
        val promotions: List<Promotion>
)