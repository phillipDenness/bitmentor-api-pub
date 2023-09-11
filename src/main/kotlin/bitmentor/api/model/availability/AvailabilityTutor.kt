package bitmentor.api.model.availability

data class AvailabilityTutor(
        val monday: AvailableTimes,
        val tuesday: AvailableTimes,
        val wednesday: AvailableTimes,
        val thursday: AvailableTimes,
        val friday: AvailableTimes,
        val saturday: AvailableTimes,
        val sunday: AvailableTimes
)

data class AvailableTimes(
        val morning: Boolean,
        val afternoon: Boolean,
        val evening: Boolean
)