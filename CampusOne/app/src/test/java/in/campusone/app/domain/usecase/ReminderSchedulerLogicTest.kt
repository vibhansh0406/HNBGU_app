package `in`.campusone.app.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderSchedulerLogicTest {

    private val now = 1_700_000_000_000L

    private fun spec(
        due: Long,
        offsets: List<Int> = listOf(1440, 180),
        completed: Boolean = false,
        enabled: Boolean = true
    ) = ReminderSchedulerLogic.computeReminderSpecs(
        targetId = "t1",
        title = "Exam",
        dueEpochMillis = due,
        nowEpochMillis = now,
        isCompleted = completed,
        reminderEnabled = enabled,
        leadOffsetsMinutes = offsets
    )

    @Test
    fun `reminder due inside the 15-minute window is scheduled`() {
        // due in 1 hour 1 min, reminder 1 hour before -> fires in 1 min (inside window)
        val specs = spec(due = now + 61 * 60_000L, offsets = listOf(60))
        assertEquals(1, specs.size)
        assertEquals(60, specs.first().leadOffsetMinutes)
        assertEquals(60_000L, specs.first().initialDelayMillis)
    }

    @Test
    fun `reminder due outside the window is not scheduled`() {
        // due tomorrow, reminder 1 hour before -> fires tomorrow, outside window
        val specs = spec(due = now + 24 * 60 * 60_000L, offsets = listOf(60))
        assertTrue(specs.isEmpty())
    }

    @Test
    fun `multiple offsets each produce their own spec`() {
        // due in 16 min, offsets 15 and 1 min -> fire in 1 min and 15 min (both in window)
        val specs = spec(due = now + 16 * 60_000L, offsets = listOf(15, 1))
        assertEquals(2, specs.size)
        assertEquals(listOf(15, 1), specs.map { it.leadOffsetMinutes })
    }

    @Test
    fun `completed reminders are never scheduled`() {
        val specs = spec(due = now + 61 * 60_000L, offsets = listOf(60), completed = true)
        assertTrue(specs.isEmpty())
    }

    @Test
    fun `disabled reminders are never scheduled`() {
        val specs = spec(due = now + 61 * 60_000L, offsets = listOf(60), enabled = false)
        assertTrue(specs.isEmpty())
    }

    @Test
    fun `unique work name encodes target and offset`() {
        val specs = spec(due = now + 61 * 60_000L, offsets = listOf(60))
        assertEquals("reminder_t1_60m", specs.first().uniqueWorkName)
    }

    @Test
    fun `initial delay is non-negative when due inside window`() {
        val specs = spec(due = now + 31 * 60_000L, offsets = listOf(30))
        assertEquals(1, specs.size)
        assertTrue(specs.first().initialDelayMillis >= 0L)
    }
}
