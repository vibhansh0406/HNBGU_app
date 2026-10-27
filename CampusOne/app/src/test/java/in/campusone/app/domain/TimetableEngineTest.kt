package `in`.campusone.app.domain

import `in`.campusone.app.domain.model.TimetableSlot
import `in`.campusone.app.domain.usecase.TimetableEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
    fun findNextClassToday_returnsUpcomingOrOngoingSlot() {
        val nextAt9am = TimetableEngine.findNextClassToday(sampleSlots, currentDayOfWeek = 1, currentMinutesOfDay = 9 * 60)
        assertEquals("CSE-401", nextAt9am?.subjectCode)

        val nextAt1pm = TimetableEngine.findNextClassToday(sampleSlots, currentDayOfWeek = 1, currentMinutesOfDay = 13 * 60)
        assertNull(nextAt1pm)
    }
}
