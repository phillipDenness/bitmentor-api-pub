package bitmentor.api.model.lesson

data class LessonsResource(
        val lessons: List<LessonResource>,
        val totalLessons: Int
)
