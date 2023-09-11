package bitmentor.api.model.enquiry

import bitmentor.api.model.message.MessageInsert

data class EnquiryInsert(
    val tutorUserId: Int,
    val topicId: Int,
    val tutorId: Int,
    val message: MessageInsert
)
