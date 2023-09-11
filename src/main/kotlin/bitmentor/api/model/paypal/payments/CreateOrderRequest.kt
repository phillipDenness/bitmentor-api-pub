package bitmentor.api.model.paypal.payments


data class CreateOrderRequest(
    val intent: String,
    val purchase_units: List<PurchaseUnitRequest2>,
    val application_context: PaypalApplicationContext
)

data class PurchaseUnitRequest2(
    val amount: PurchaseUnitAmount
)

data class PaypalApplicationContext(
    val shipping_preference: String
)