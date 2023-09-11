package bitmentor.api.model.lesson

import bitmentor.api.model.promotion.promotions.Promotion
import bitmentor.api.model.topic.TopicDao
import bitmentor.api.repository.entity.LessonStateDao
import java.time.ZonedDateTime

data class LessonResource(
    val id: Int,
    val tutorUserId: Int,
    val tutorId: Int,
    val tutorDisplayName: String,
    val studentId: Int,
    val studentDisplayName: String,
    val lessonDate: ZonedDateTime,
    val lessonEndDate: ZonedDateTime,
    val cost: Double,
    val topic: TopicDao,
    val lessonStates: List<LessonStateDao>,
    val enquiryId: Int,
    val promotion: Promotion? = null
)
