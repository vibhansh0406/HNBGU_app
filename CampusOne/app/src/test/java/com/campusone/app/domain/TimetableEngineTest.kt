package com.campusone.app.domain

import com.campusone.app.domain.model.TimetableSlot
import com.campusone.app.domain.usecase.TimetableEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TimetableEngineTest {

    private val sampleSlots = listOf(
        TimetableSlot("1", "s1", "OS", "CSE-403", 1, "11:00", "12:00", "R1", "Prof A", 4, "A"),
        TimetableSlot("2", "s2", "DAA", "CSE-401", 1, "09:30", "10:30", "R2", "Prof B", 4, "A")
    )

    @Test
    fun slotsForDay_sortsChronologically() {
        val sorted = TimetableEngine.slotsForDay(sampleSlots, dayOfWeek = 1)
        assertEquals(2, sorted.size)
        assertEquals("CSE-401", sorted[0].subjectCode)
        assertEquals("CSE-403", sorted[1].subjectCode)
    }

    @Test
    fun evaluateTodaySlots_computesMinutesRemainingAndOngoing() {
        val evaluated = TimetableEngine.evaluateTodaySlots(sampleSlots, currentDayOfWeek = 1, currentMinutesOfDay = 10 * 60)
        assertEquals(2, evaluated.size)
        assertTrue(evaluated[0].isOngoing)
        assertEquals(30, evaluated[0].minutesRemaining)
        assertEquals(60, evaluated[1].minutesUntilStart)
    }

    @Test
    fun findNextClassToday_returnsUpcomingOrOngoingSlot() {
        val nextAt9am = TimetableEngine.findNextClassToday(sampleSlots, currentDayOfWeek = 1, currentMinutesOfDay = 9 * 60)
        assertEquals("CSE-401", nextAt9am?.subjectCode)

        val nextAt1pm = TimetableEngine.findNextClassToday(sampleSlots, currentDayOfWeek = 1, currentMinutesOfDay = 13 * 60)
        assertNull(nextAt1pm)
    }

    @Test
    fun detectClashes_identifiesOverlappingSlotsAndIgnoresSelf() {
        val clashes = TimetableEngine.detectClashes(
            allSlots = sampleSlots,
            dayOfWeek = 1,
            startTime = "10:00",
            endTime = "11:30",
            ignoreSlotId = null
        )
        assertEquals(2, clashes.size)
        assertEquals(30, clashes[0].overlapMinutes)
        assertEquals(30, clashes[1].overlapMinutes)

        val selfIgnored = TimetableEngine.detectClashes(
            allSlots = sampleSlots,
            dayOfWeek = 1,
            startTime = "09:30",
            endTime = "10:30",
            ignoreSlotId = "2"
        )
        assertTrue(selfIgnored.isEmpty())
    }
}
