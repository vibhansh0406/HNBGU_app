package `in`.campusone.app.domain.usecase

object ReminderSchedulerLogic {

    /** Default exam reminder offsets: 24h before and 3h before. */
    val DEFAULT_EXAM_OFFSETS_MINS: List<Int> = listOf(1440, 180)

    /**
     * Computes which reminder offsets are due "now" (within the worker's 15-minute polling
     * window) for a given deadline. Returns one spec per offset whose fire time falls in
     * `[now + 1ms, now + 15min]`.
     */
    fun computeReminderSpecs(
        targetId: String,
        title: String,
        dueEpochMillis: Long,
        nowEpochMillis: Long,
        isCompleted: Boolean,
        reminderEnabled: Boolean,
        leadOffsetsMinutes: List<Int>
    ): List<ReminderSpec> {
        if (!reminderEnabled || isCompleted) return emptyList()

        val windowStart = nowEpochMillis + 1L
        val windowEnd = nowEpochMillis + 15L * 60_000L

        return leadOffsetsMinutes
            .mapNotNull { offsetMinutes ->
                val fireTime = dueEpochMillis - offsetMinutes.toLong() * 60_000L
                if (fireTime in windowStart..windowEnd) {
                    ReminderSpec(
                        targetId = targetId,
                        title = title,
                        leadOffsetMinutes = offsetMinutes,
                        initialDelayMillis = fireTime - nowEpochMillis,
                        uniqueWorkName = "reminder_${targetId}_${offsetMinutes}m"
                    )
                } else {
                    null
                }
            }
    }

    data class ReminderSpec(
        val targetId: String,
        val title: String,
        val leadOffsetMinutes: Int,
        val initialDelayMillis: Long,
        val uniqueWorkName: String
    )
}
