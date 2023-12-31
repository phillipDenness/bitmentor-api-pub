package bitmentor.api.model.paypal.webhook

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.ZonedDateTime

@JsonIgnoreProperties(ignoreUnknown = true)
data class PaypalDispute(
    val id: String,
    val create_time: ZonedDateTime,
    val event_type: String,
    val resource: WebhookResource
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class WebhookResource(
    val disputed_transactions: List<DisputedTransaction>
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class DisputedTransaction(
    val seller_transaction_id: String
)

//{
//    "id": "WH-4M0448861G563140B-9EX36365822141321",
//    "create_time": "2018-06-21T13:36:33.000Z",
//    "resource_type": "dispute",
//    "event_type": "CUSTOMER.DISPUTE.CREATED",
//    "summary": "A new dispute opened with Case # PP-000-042-663-135",
//    "resource": {
//    "disputed_transactions": [
//    {
//        "seller_transaction_id": "00D10444LD479031K",
//        "seller": {
//        "merchant_id": "RD465XN5VS364",
//        "name": "Test Store"
//    },
//        "items": [],
//        "seller_protection_eligible": true
//    }
//    ],
//    "reason": "MERCHANDISE_OR_SERVICE_NOT_RECEIVED",
//    "dispute_channel": "INTERNAL",
//    "update_time": "2018-06-21T13:35:44.000Z",
//    "create_time": "2018-06-21T13:35:44.000Z",
//    "messages": [
//    {
//        "posted_by": "BUYER",
//        "time_posted": "2018-06-21T13:35:52.000Z",
//        "content": "qwqwqwq"
//    }
//    ],
//    "links": [
//    {
//        "href": "https://api.paypal.com/v1/customer/disputes/PP-000-042-663-135",
//        "rel": "self",
//        "method": "GET"
//    },
//    {
//        "href": "https://api.paypal.com/v1/customer/disputes/PP-000-042-663-135/send-message",
//        "rel": "send_message",
//        "method": "POST"
//    }
//    ],
//    "dispute_amount": {
//        "currency_code": "USD",
//        "value": "3.00"
//    },
//    "dispute_id": "PP-000-042-663-135",
//    "dispute_life_cycle_stage": "INQUIRY",
//    "status": "OPEN"
//},
//    "links": [
//    {
//        "href": "https://api.paypal.com/v1/notifications/webhooks-events/WH-4M0448861G563140B-9EX36365822141321",
//        "rel": "self",
//        "method": "GET",
//        "encType": "application/json"
//    },
//    {
//        "href": "https://api.paypal.com/v1/notifications/webhooks-events/WH-4M0448861G563140B-9EX36365822141321/resend",
//        "rel": "resend",
//        "method": "POST",
//        "encType": "application/json"
//    }
//    ],
//    "event_version": "1.0"
//}
