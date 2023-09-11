package bitmentor.api.model.lesson

import bitmentor.api.model.notification.NotificationType

enum class LessonStates(val isTutor: Boolean) {
    PENDING(true),
    RESCHEDULED(true),
    CONFIRMED(false),
    REJECTED(false),
    CANCELLED(true),
    REVIEWED(false)
}

fun LessonStates.toNotification(): NotificationType? {
    return when (this) {
        LessonStates.PENDING -> {
            NotificationType.LESSON_CREATED
        }
        LessonStates.CONFIRMED -> {
            NotificationType.LESSON_CONFIRMED
        }
        LessonStates.REJECTED -> {
            NotificationType.LESSON_REJECTED
        }
        LessonStates.CANCELLED -> {
            NotificationType.LESSON_CANCELLED
        }
        else -> null
    }
}