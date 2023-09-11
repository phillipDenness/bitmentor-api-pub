package bitmentor.api.model.paypal.integration

data class PartnerReferralPost(
    val tracking_id: String,
    val operations: List<PaypalOperations>,
    val products: List<PaypalProducts>,
    val legal_consents: List<LegalConsent>,
    val preferred_language_code: String,
    val partner_config_override: PartnerConfigOverride,
    val email: String
)

data class PartnerConfigOverride(
    val partner_logo_url: String,
    val return_url: String,
    val return_url_description: String
)

data class LegalConsent(
    val type: String,
    val granted: Boolean
)

data class PaypalOperations(
    val operation: String,
    val api_integration_preference: ApiIntegrationPreference
)

data class ApiIntegrationPreference(
    val rest_api_integration: RestApiIntegration
)

data class RestApiIntegration(
    val integration_method: String,
    val integration_type: String,
    val third_party_details: ThirdPartyDetails
)

data class ThirdPartyDetails(
    val features: List<PaypalThirdPartyFeatures>
)