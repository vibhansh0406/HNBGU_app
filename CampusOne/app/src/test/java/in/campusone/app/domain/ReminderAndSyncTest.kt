package `in`.campusone.app.domain

import `in`.campusone.app.domain.usecase.ReminderSchedulerLogic
import `in`.campusone.app.domain.usecase.SyncConflictResolver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderAndSyncTest {

    @Test
    fun reminderDelay_subtractsLeadMinutes() {
        val now = 1_000_000L
        val event = now + 30 * 60_000L // 30 mins in future
        val delay = ReminderSchedulerLogic.computeDelayMillis(event, now, leadMinutes = 15)
        assertEquals(15 * 60_000L, delay)
        assertTrue(ReminderSchedulerLogic.shouldScheduleReminder(event, now, 15))
    }

    @Test
    fun syncConflictResolver_usesLastWriteWins() {
        assertTrue(SyncConflictResolver.shouldOverwriteLocal(100L, 200L))
        assertFalse(SyncConflictResolver.shouldOverwriteLocal(200L, 100L))
    }
}
