package com.campusone.app.domain.usecase

import com.campusone.app.domain.model.EvaluatedSlot
import com.campusone.app.domain.model.TimetableSlot
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.max

object TimetableEngine {
    private val formatter = DateTimeFormatter.ofPattern("HH:mm")

    fun parseMinutes(hhMm: String): Int {
        return try {
            val lt = LocalTime.parse(hhMm.trim(), formatter)
            lt.hour * 60 + lt.minute
        } catch (_: Exception) {
            val parts = hhMm.trim().split(":")
            val h = parts.getOrNull(0)?.toIntOrNull() ?: 9
            val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
            (h.coerceIn(0, 23) * 60) + m.coerceIn(0, 59)
        }
    }

    fun slotsForDay(allSlots: List<TimetableSlot>, dayOfWeek: Int): List<TimetableSlot> {
        return allSlots
            .filter { it.dayOfWeek == dayOfWeek }
            .sortedBy { parseMinutes(it.startTime) }
    }

    fun evaluateTodaySlots(
        allSlots: List<TimetableSlot>,
        currentDayOfWeek: Int,
        currentMinutesOfDay: Int
    ): List<EvaluatedSlot> {
        return slotsForDay(allSlots, currentDayOfWeek).map { slot ->
            val startMins = parseMinutes(slot.startTime)
            val endMins = parseMinutes(slot.endTime)
            val isOngoing = currentMinutesOfDay in startMins until endMins
            val minutesUntilStart = max(0, startMins - currentMinutesOfDay)
            val minutesRemaining = max(0, endMins - currentMinutesOfDay)
            EvaluatedSlot(
                slot = slot,
                minutesUntilStart = minutesUntilStart,
                minutesRemaining = minutesRemaining,
                isOngoing = isOngoing
            )
        }
    }

    fun findNextClassToday(
        allSlots: List<TimetableSlot>,
        currentDayOfWeek: Int,
        currentMinutesOfDay: Int
    ): TimetableSlot? {
        return slotsForDay(allSlots, currentDayOfWeek)
            .firstOrNull { slot ->
                parseMinutes(slot.endTime) > currentMinutesOfDay
            }
    }
}
