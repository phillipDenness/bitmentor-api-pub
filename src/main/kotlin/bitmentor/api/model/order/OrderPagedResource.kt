package bitmentor.api.model.order

data class OrderPagedResource(
        val orders: List<OrderResource>,
        val total: Int,
        val page: Int,
        val size: Int
)