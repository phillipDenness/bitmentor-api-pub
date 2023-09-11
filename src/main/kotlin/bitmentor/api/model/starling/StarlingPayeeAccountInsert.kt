package bitmentor.api.model.starling

data class StarlingPayeeAccountInsert(
        val description: String,
        val defaultAccount: Boolean,
        val countryCode: String,
        val accountIdentifier: String,
        val bankIdentifier: String,
        val bankIdentifierType: String
)