package bitmentor.api.model.starling

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.LocalDate

@JsonIgnoreProperties(ignoreUnknown = true)
data class StarlingPayees(
        val payees: List<StarlingPayeeResource>
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class StarlingPayeeResource(
        val payeeUid: String,
        val payeeName: String,
        val phoneNumber: String,
        val payeeType: String,
        val firstName: String,
        val middleName: String?,
        val lastName: String,
        val businessName: String?,
        val dateOfBirth: LocalDate,
        val accounts: List<StarlingAccountResource>
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class StarlingAccountResource(
        val payeeAccountUid: String,
        val description: String,
        val defaultAccount: Boolean,
        val countryCode: String,
        val accountIdentifier: String,
        val bankIdentifier: String,
        val bankIdentifierType: String,
        val lastReferences: List<String>
)