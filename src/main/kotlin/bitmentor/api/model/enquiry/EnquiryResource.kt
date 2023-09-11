package bitmentor.api.model.enquiry

import bitmentor.api.model.lesson.LessonResource
import bitmentor.api.model.message.MessageResource
import bitmentor.api.model.topic.TopicDao
import bitmentor.api.model.user.UserPublicResource


data class EnquiryResource(
    val id: Int,
    val tutorId: Int,
    val tutorUser: UserPublicResource,
    val studentUser: UserPublicResource,
    val tutorDisplayName: String,
    val studentDisplayName: String,
    val lastMessage: MessageResource,
    val recipientDisplayName: String,
    val recipientProfileImage: String?,
    val topic: TopicDao,
    val upcomingLessons: List<LessonResource>
)
