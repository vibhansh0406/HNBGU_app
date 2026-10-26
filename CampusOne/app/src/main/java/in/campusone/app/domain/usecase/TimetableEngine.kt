package `in`.campusone.app.domain.usecase

import `in`.campusone.app.domain.model.TimetableSlot
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

enum class SlotLiveState {
    COMPLETED,
    ONGOING,
    UP_NEXT,
    UPCOMING_LATER
}

data class classEvaluatedSlot(
    val slot: TimetableSlot,
    val state: SlotLiveState,
    val minutesUntilStart: Long? = null,
    val minutesRemaining: Long? = null
)

data class NextClassResult(
    val slot: TimetableSlot,
    val isOngoing: Boolean,
    val isToday: Boolean,
    val minutesUntilStart: Long
)

object TimetableEngine {
    private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    fun parseTime(hhMm: String): LocalTime = LocalTime.parse(hhMm.trim(), timeFormatter)

    /**
     * Evaluates today's slots in chronological order and marks COMPLETED, ONGOING,
     * the single earliest upcoming class as UP_NEXT, and remaining as UPCOMING_LATER.
     */
    fun evaluateTodaySchedule(
        allSlots: List<TimetableSlot>,
        now: LocalDateTime,
        selectedSubjectIds: Set<String> = emptySet()
    ): List<classEvaluatedSlot> {
        val todayDayValue = now.dayOfWeek.value
        val currentTime = now.toLocalTime()

        val filtered = allSlots
            .filter { it.dayOfWeek == todayDayValue }
            .filter { !it.isElective || selectedSubjectIds.isEmpty() || it.subjectId in selectedSubjectIds }
            .sortedBy { parseTime(it.startTime) }

        var upNextAssigned = false
        return filtered.map { slot ->
            val start = parseTime(slot.startTime)
            val end = parseTime(slot.endTime)

            when {
                !currentTime.isBefore(end) -> {
                    classEvaluatedSlot(slot, SlotLiveState.COMPLETED)
                }
                !currentTime.isBefore(start) && currentTime.isBefore(end) -> {
                    val rem = Duration.between(currentTime, end).toMinutes()
                    classEvaluatedSlot(slot, SlotLiveState.ONGOING, minutesRemaining = rem)
                }
                else -> {
                    val until = Duration.between(currentTime, start).toMinutes()
                    val state = if (!upNextAssigned) {
                        upNextAssigned = true
                        SlotLiveState.UP_NEXT
                    } else {
                        SlotLiveState.UPCOMING_LATER
                    }
                    classEvaluatedSlot(slot, state, minutesUntilStart = until)
                }
            }
        }
    }

    /**
     * Finds the currently ongoing class or the next upcoming class across the week
     * (used by Home Dashboard and Glance Home-Screen Widget).
     */
    fun findNextOrOngoingClass(
        allSlots: List<TimetableSlot>,
        now: LocalDateTime,
        selectedSubjectIds: Set<String> = emptySet()
    ): NextClassResult? {
        val eligible = allSlots.filter {
            !it.isElective || selectedSubjectIds.isEmpty() || it.subjectId in selectedSubjectIds
        }
        if (eligible.isEmpty()) return null

        val currentDay = now.dayOfWeek.value
        val currentTime = now.toLocalTime()

        // 1. Check today's remaining or ongoing classes first
        val todaySlots = eligible
            .filter { it.dayOfWeek == currentDay }
            .sortedBy { parseTime(it.startTime) }

        for (slot in todaySlots) {
            val start = parseTime(slot.startTime)
            val end = parseTime(slot.endTime)
            if (!currentTime.isBefore(start) && currentTime.isBefore(end)) {
                return NextClassResult(
                    slot = slot,
                    isOngoing = true,
                    isToday = true,
                    minutesUntilStart = 0L
                )
            }
            if (currentTime.isBefore(start)) {
                return NextClassResult(
                    slot = slot,
                    isOngoing = false,
                    isToday = true,
                    minutesUntilStart = Duration.between(currentTime, start).toMinutes()
                )
            }
        }

        // 2. Look ahead up to 7 days for the next scheduled class
        for (offset in 1..7) {
            val targetDay = ((currentDay - 1 + offset) % 7) + 1
            val daySlots = eligible
                .filter { it.dayOfWeek == targetDay }
                .sortedBy { parseTime(it.startTime) }
            if (daySlots.isNotEmpty()) {
                val firstSlot = daySlots.first()
                val nextDate = now.toLocalDate().plusDays(offset.toLong())
                val nextDateTime = LocalDateTime.of(nextDate, parseTime(firstSlot.startTime))
                val mins = Duration.between(now, nextDateTime).toMinutes()
                return NextClassResult(
                    slot = firstSlot,
                    isOngoing = false,
                    isToday = false,
                    minutesUntilStart = mins
                )
            }
        }
        return null
    }
}
