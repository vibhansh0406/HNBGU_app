package `in`.campusone.app.domain.usecase

data class ReminderSpec(
    val uniqueWorkName: String,
    val title: String,
    val leadOffsetMinutes: Int,
    val initialDelayMillis: Long
)

object ReminderSchedulerLogic {
    val DEFAULT_EXAM_OFFSETS_MINS = listOf(24 * 60, 3 * 60)

    fun computeReminderSpecs(
        targetId: String,
        title: String,
        dueEpochMillis: Long,
        nowEpochMillis: Long,
        isCompleted: Boolean,
        reminderEnabled: Boolean,
        leadOffsetsMinutes: List<Int>
    ): List<ReminderSpec> {
        if (isCompleted || !reminderEnabled) return emptyList()

        return leadOffsetsMinutes
            .asSequence()
            .filter { it > 0 }
            .distinct()
            .mapNotNull { leadMinutes ->
                val delayMillis = dueEpochMillis - nowEpochMillis - leadMinutes * 60_000L
                if (delayMillis <= 0L) return@mapNotNull null
                ReminderSpec(
                    uniqueWorkName = "campusone_${targetId}_${leadMinutes}m",
                    title = title,
                    leadOffsetMinutes = leadMinutes,
                    initialDelayMillis = delayMillis
                )
            }
            .toList()
    }
}
