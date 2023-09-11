package bitmentor.api.model.starling

data class StarlingPayeeRequest(
        val payeeName: String,
        val phoneNumber: String,
        val payeeType: String,
        val firstName: String,
        val middleName: String?,
        val lastName: String,
        val businessName: String?,
        val dateOfBirth: String,
        val accounts: List<StarlingPayeeAccountInsert>
)