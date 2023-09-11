package bitmentor.api.model.paypal.webhook

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class PaypalWebhookRequest(
    val id: String,
    val resource_type: String,
    val event_type: String,
    val resource: MerchantOnboardComplete
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class MerchantOnboardComplete(
    val merchant_id: String
)


//{
//    "id": "WH-2WR32451HC0233532-67976317FL4543714",
//    "create_time": "2016-08-02T21:41:28Z",
//    "resource_type": "partner-consent",
//    "event_type": "MERCHANT.PARTNER-CONSENT.REVOKED",
//    "resource": {
//    "merchant_id": "ELAMYJUN78D6G"
//}
//}
//{
//    "id": "WH-58T81104MW861572C-9L9645832V567963S",
//    "event_version": "1.0",
//    "create_time": "2021-02-22T13:54:05.338Z",
//    "resource_type": "merchant-onboarding",
//    "event_type": "MERCHANT.ONBOARDING.COMPLETED",
//    "summary": "The merchant account setup is completed",
//    "resource": {
//    "partner_client_id": "ATry7qhxfVnUeukJOWciiVgpzeuc2uT9goWkWALmm4c3ZknMKGuwePBerYLNw3axzVal35QpAuWSm7uj",
//    "links": [
//    {
//        "method": "GET",
//        "rel": "self",
//        "description": "Get the merchant status information of merchants onboarded by this partner",
//        "href": "https://api.sandbox.paypal.com/v1/customer/partners/MNM67HV7V3H8Q/merchant-integrations/FB3H9M68WXE64"
//    }
//    ],
//    "merchant_id": "FB3H9M68WXE64",
//    "tracking_id": "100046"
//},
//    "links": [
//    {
//        "href": "https://api.sandbox.paypal.com/v1/notifications/webhooks-events/WH-72H67363BU8487253-1J108824Y6617451S",
//        "rel": "self",
//        "method": "GET"
//    },
//    {
//        "href": "https://api.sandbox.paypal.com/v1/notifications/webhooks-events/WH-72H67363BU8487253-1J108824Y6617451S/resend",
//        "rel": "resend",
//        "method": "POST"
//    }
//    ]
//}