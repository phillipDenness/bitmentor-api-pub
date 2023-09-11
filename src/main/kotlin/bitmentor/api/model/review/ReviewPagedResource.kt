package bitmentor.api.model.review

data class ReviewPagedResource(
        val reviews: List<ReviewResource>,
        val totalReviews: Int
)
