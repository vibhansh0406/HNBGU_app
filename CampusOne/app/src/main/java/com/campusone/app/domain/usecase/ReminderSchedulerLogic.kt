package com.campusone.app.domain.usecase

import kotlin.math.max

data class ReminderSpec(
    val uniqueWorkName: String,
    val offsetMinutes: Int,
    val initialDelayMillis: Long
)

object ReminderSchedulerLogic {

    val DEFAULT_EXAM_OFFSETS_MINS: List<Int> = listOf(1440, 120)
    val DEFAULT_ASSIGNMENT_OFFSETS_MINS: List<Int> = listOf(1440, 120)

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

    fun computeReminderSpecs(
        itemId: String,
        targetEpochMillis: Long,
        nowEpochMillis: Long,
        offsetsMinutes: List<Int> = DEFAULT_EXAM_OFFSETS_MINS
    ): List<ReminderSpec> {
        return offsetsMinutes.mapNotNull { offsetMins ->
            if (shouldScheduleReminder(targetEpochMillis, nowEpochMillis, offsetMins)) {
                ReminderSpec(
                    uniqueWorkName = "rem_${itemId}_$offsetMins",
                    offsetMinutes = offsetMins,
                    initialDelayMillis = computeDelayMillis(targetEpochMillis, nowEpochMillis, offsetMins)
                )
            } else {
                null
            }
        }
    }
}

