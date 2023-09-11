package bitmentor.api.model.bigbluebutton

data class BbbMeetingInsert(
        val meetingId: String,
        val moderatorPw: String,
        val lessonId: Int,
        val duration: Int
)