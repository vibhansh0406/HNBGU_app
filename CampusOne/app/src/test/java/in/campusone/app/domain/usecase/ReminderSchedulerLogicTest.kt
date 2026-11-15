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
        // due in 1 hour, reminder 1 hour before -> fires now
        val specs = spec(due = now + 60 * 60_000L, offsets = listOf(60))
        assertEquals(1, specs.size)
        assertEquals(60, specs.first().leadOffsetMinutes)
        assertEquals(0L, specs.first().initialDelayMillis)
    }

    @Test
    fun `reminder due outside the window is not scheduled`() {
        // due tomorrow, reminder 1 hour before -> fires tomorrow, outside window
        val specs = spec(due = now + 24 * 60 * 60_000L, offsets = listOf(60))
        assertTrue(specs.isEmpty())
    }

    @Test
    fun `multiple offsets each produce their own spec`() {
        val specs = spec(due = now + 2 * 60 * 60_000L, offsets = listOf(120, 60))
        assertEquals(2, specs.size)
        assertEquals(listOf(120, 60), specs.map { it.leadOffsetMinutes })
    }

    @Test
    fun `completed reminders are never scheduled`() {
        val specs = spec(due = now + 60 * 60_000L, offsets = listOf(60), completed = true)
        assertTrue(specs.isEmpty())
    }

    @Test
    fun `disabled reminders are never scheduled`() {
        val specs = spec(due = now + 60 * 60_000L, offsets = listOf(60), enabled = false)
        assertTrue(specs.isEmpty())
    }

    @Test
    fun `unique work name encodes target and offset`() {
        val specs = spec(due = now + 60 * 60_000L, offsets = listOf(60))
        assertEquals("reminder_t1_60m", specs.first().uniqueWorkName)
    }

    @Test
    fun `initial delay is non-negative when due inside window`() {
        val specs = spec(due = now + 30 * 60_000L, offsets = listOf(30))
        assertEquals(1, specs.size)
        assertTrue(specs.first().initialDelayMillis >= 0L)
    }
}
