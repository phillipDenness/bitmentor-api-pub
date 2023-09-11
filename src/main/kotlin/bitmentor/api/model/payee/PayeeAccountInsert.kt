package bitmentor.api.model.payee

data class PayeeAccountInsert(
        val accountIdentifier: String,
        val bankIdentifier: String
)