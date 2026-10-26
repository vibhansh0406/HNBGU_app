package `in`.campusone.app.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Test

class ReminderSchedulerLogicTest {
    @Test
    fun computesFutureRemindersAndSkipsCompletedOrPastOffsets() {
        val now = 1_000_000L
        val specs = ReminderSchedulerLogic.computeReminderSpecs(
            targetId = "assignment-1",
            title = "Lab submission",
            dueEpochMillis = now + 2 * 60 * 60_000L,
            nowEpochMillis = now,
            isCompleted = false,
            reminderEnabled = true,
            leadOffsetsMinutes = listOf(120, 60, 60, 0, -5)
        )

        assertEquals(1, specs.size)
        assertEquals(60, specs.single().leadOffsetMinutes)
        assertEquals(60 * 60_000L, specs.single().initialDelayMillis)
    }

    @Test
    fun completedItemsHaveNoReminders() {
        val specs = ReminderSchedulerLogic.computeReminderSpecs(
            targetId = "exam-1",
            title = "Exam",
            dueEpochMillis = 100_000L,
            nowEpochMillis = 0L,
            isCompleted = true,
            reminderEnabled = true,
            leadOffsetsMinutes = ReminderSchedulerLogic.DEFAULT_EXAM_OFFSETS_MINS
        )
        assertEquals(emptyList<ReminderSpec>(), specs)
    }

    @Test
    fun disabledReminderHasNoWork() {
        val specs = ReminderSchedulerLogic.computeReminderSpecs(
            targetId = "assignment-2",
            title = "Lab report",
            dueEpochMillis = 100_000L,
            nowEpochMillis = 0L,
            isCompleted = false,
            reminderEnabled = false,
            leadOffsetsMinutes = listOf(30)
        )
        assertEquals(emptyList<ReminderSpec>(), specs)
    }
}
