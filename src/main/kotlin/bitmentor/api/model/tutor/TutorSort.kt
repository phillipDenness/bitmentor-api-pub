package bitmentor.api.model.tutor

enum class TutorSort(val column: String) {
    JOIN_DATE_ASC("tutor.tutor_join_date ASC"),
    RATING_DESC("tutor.rating DESC, tutor.ratings DESC"),
    JOIN_DATE_DESC("tutor.tutor_join_date DESC"),
    RATING_ASC("tutor.rating ASC, tutor.ratings ASC")
}