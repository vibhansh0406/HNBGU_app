package `in`.campusone.app.domain.usecase

import `in`.campusone.app.domain.model.TimetableSlot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class TimetableEngineTest {

    private val mondaySlots = listOf(
        TimetableSlot(
            id = "mon_1",
            subjectId = "CS301",
            subjectName = "Data Structures",
            subjectCode = "CS301",
            teacherName = "Dr. Rauthan",
            roomNumber = "104",
            dayOfWeek = 1, // Monday
            startTime = "09:00",
            endTime = "10:00"
        ),
        TimetableSlot(
            id = "mon_2",
            subjectId = "CS302",
            subjectName = "Operating Systems",
            subjectCode = "CS302",
            teacherName = "Dr. Barthwal",
            roomNumber = "104",
            dayOfWeek = 1, // Monday
            startTime = "10:05",
            endTime = "11:05"
        ),
        TimetableSlot(
            id = "mon_3",
            subjectId = "CS303",
            subjectName = "DBMS",
            subjectCode = "CS303",
            teacherName = "Dr. Rana",
            roomNumber = "Lab 2",
            dayOfWeek = 1, // Monday
            startTime = "11:15",
            endTime = "12:15"
        )
    )

    @Test
    fun `evaluateTodaySchedule marks completed, ongoing, and single up_next accurately`() {
        // Monday 2026-10-26 at 09:30 AM (during first class)
        val now = LocalDateTime.of(2026, 10, 26, 9, 30)
        val evaluated = TimetableEngine.evaluateTodaySchedule(mondaySlots, now)

        assertEquals(3, evaluated.size)
        assertEquals(SlotLiveState.ONGOING, evaluated[0].state)
        assertEquals(30L, evaluated[0].minutesRemaining)

        assertEquals(SlotLiveState.UP_NEXT, evaluated[1].state)
        assertEquals(35L, evaluated[1].minutesUntilStart)

        assertEquals(SlotLiveState.UPCOMING_LATER, evaluated[2].state)
    }

    @Test
    fun `findNextOrOngoingClass rolls over to next available day after today classes finish`() {
        // Monday 2026-10-26 at 16:00 PM (all Monday classes over; next is next Monday in this list)
        val now = LocalDateTime.of(2026, 10, 26, 16, 0)
        val next = TimetableEngine.findNextOrOngoingClass(mondaySlots, now)

        assertNotNull(next)
        assertFalse(next!!.isToday)
        assertFalse(next.isOngoing)
        assertEquals("CS301", next.slot.subjectId)
    }

    @Test
    fun `findNextOrOngoingClass identifies ongoing class immediately`() {
        val now = LocalDateTime.of(2026, 10, 26, 10, 20)
        val next = TimetableEngine.findNextOrOngoingClass(mondaySlots, now)

        assertNotNull(next)
        assertTrue(next!!.isOngoing)
        assertTrue(next.isToday)
        assertEquals("CS302", next.slot.subjectId)
    }
}
