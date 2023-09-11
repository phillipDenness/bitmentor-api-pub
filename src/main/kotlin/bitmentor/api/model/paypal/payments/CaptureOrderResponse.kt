package bitmentor.api.model.paypal.payments

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class CaptureOrderResponse(
    val id: String,
    val status: String,
    val purchase_units: List<CapturePurchaseUnit>
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class CapturePurchaseUnit(
    val payments: PaypalCaptures
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class PaypalCaptures(
    val captures: List<CapturePayment>
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class CapturePayment(
    val id: String,
    val amount: PurchaseUnitAmount,
    val seller_receivable_breakdown: SellerBreakdown?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class SellerBreakdown(
    val gross_amount: PurchaseUnitAmount,
    val paypal_fee: PurchaseUnitAmount,
    val net_amount: PurchaseUnitAmount
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class PurchaseUnitAmount(
    val currency_code: String,
    val value: String
)