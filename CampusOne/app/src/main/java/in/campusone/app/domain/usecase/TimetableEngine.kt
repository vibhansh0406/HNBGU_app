package `in`.campusone.app.domain.usecase

import `in`.campusone.app.domain.model.TimetableSlot
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Pure Kotlin Timetable Engine for sorting weekly slots and identifying the next upcoming class.
 */
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
