package bitmentor.api.model.enquiry


data class EnquiryPagedResource(
        val enquiries: List<EnquiryResource>,
        val page: Int,
        val size: Int,
        val total: Int
)
