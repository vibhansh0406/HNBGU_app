package `in`.campusone.app.domain.usecase

import kotlin.math.max

/**
 * Pure Kotlin helper for calculating WorkManager initial delays for class, assignment, and exam reminders.
 */
object ReminderSchedulerLogic {

    fun computeDelayMillis(
        eventEpochMillis: Long,
        nowEpochMillis: Long,
        leadMinutes: Int
    ): Long {
        val triggerMillis = eventEpochMillis - (leadMinutes.toLong() * 60_000L)
        return max(0L, triggerMillis - nowEpochMillis)
    }

    fun shouldScheduleReminder(
        eventEpochMillis: Long,
        nowEpochMillis: Long,
        leadMinutes: Int
    ): Boolean {
        val triggerMillis = eventEpochMillis - (leadMinutes.toLong() * 60_000L)
        return triggerMillis > nowEpochMillis
    }
}
