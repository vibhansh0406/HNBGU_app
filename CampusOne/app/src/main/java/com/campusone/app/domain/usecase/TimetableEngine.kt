package com.campusone.app.domain.usecase

import com.campusone.app.domain.model.EvaluatedSlot
import com.campusone.app.domain.model.TimetableClash
import com.campusone.app.domain.model.TimetableSlot
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.max
import kotlin.math.min

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

    fun isValidTimeFormat(hhMm: String): Boolean {
        val trimmed = hhMm.trim()
        val parts = trimmed.split(":")
        if (parts.size != 2) return false
        val h = parts[0].toIntOrNull() ?: return false
        val m = parts[1].toIntOrNull() ?: return false
        return h in 0..23 && m in 0..59
    }

    fun isValidSlotRange(startTime: String, endTime: String): Boolean {
        if (!isValidTimeFormat(startTime) || !isValidTimeFormat(endTime)) return false
        return parseMinutes(endTime) > parseMinutes(startTime)
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

    fun detectClashes(
        allSlots: List<TimetableSlot>,
        dayOfWeek: Int,
        startTime: String,
        endTime: String,
        ignoreSlotId: String? = null
    ): List<TimetableClash> {
        val candidateStart = parseMinutes(startTime)
        val candidateEnd = parseMinutes(endTime)
        if (candidateEnd <= candidateStart) return emptyList()

        return slotsForDay(allSlots, dayOfWeek)
            .filter { it.id != ignoreSlotId }
            .mapNotNull { existing ->
                val existStart = parseMinutes(existing.startTime)
                val existEnd = parseMinutes(existing.endTime)
                val overlapStart = max(candidateStart, existStart)
                val overlapEnd = min(candidateEnd, existEnd)
                if (overlapEnd > overlapStart) {
                    TimetableClash(
                        conflictingSlot = existing,
                        overlapMinutes = overlapEnd - overlapStart
                    )
                } else {
                    null
                }
            }
    }

    fun countRemainingOccurrencesUntil(
        allSlots: List<TimetableSlot>,
        subjectId: String,
        subjectCode: String,
        subjectName: String,
        fromDate: LocalDate,
        toDate: LocalDate
    ): Int {
        if (toDate.isBefore(fromDate)) return 0
        val matchingSlots = allSlots.filter { slot ->
            slot.subjectId == subjectId ||
                slot.subjectCode.equals(subjectCode, ignoreCase = true) ||
                slot.subjectName.equals(subjectName, ignoreCase = true)
        }
        if (matchingSlots.isEmpty()) return 0

        val slotsPerDayOfWeek = matchingSlots.groupBy { it.dayOfWeek }.mapValues { it.value.size }
        var total = 0
        var cursor = fromDate.plusDays(1)
        var guardDays = 0
        while (!cursor.isAfter(toDate) && guardDays < 366) {
            val dow = cursor.dayOfWeek.value
            total += slotsPerDayOfWeek[dow] ?: 0
            cursor = cursor.plusDays(1)
            guardDays += 1
        }
        return total
    }

    fun computeNextOccurrenceDelayMillis(
        dayOfWeek: Int,
        startTime: String,
        leadMinutes: Int,
        currentDayOfWeek: Int,
        currentMinutesOfDay: Int
    ): Long {
        val targetStartMins = parseMinutes(startTime)
        val triggerMinsOfDay = (targetStartMins - leadMinutes.coerceIn(0, 180)).coerceAtLeast(0)
        var daysAhead = (dayOfWeek.coerceIn(1, 7) - currentDayOfWeek.coerceIn(1, 7) + 7) % 7
        if (daysAhead == 0 && triggerMinsOfDay <= currentMinutesOfDay) {
            daysAhead = 7
        }
        val totalMinutesUntilTrigger = (daysAhead * 24 * 60) + (triggerMinsOfDay - currentMinutesOfDay)
        return max(60_000L, totalMinutesUntilTrigger.toLong() * 60_000L)
    }
}
