package com.campusone.app.domain

import com.campusone.app.domain.usecase.ReminderSchedulerLogic
import com.campusone.app.domain.usecase.SyncConflictResolver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderAndSyncTest {

    @Test
    fun reminderDelayAndSpecs_computeAccurately() {
        val now = 1_000_000L
        val event = now + 48 * 60 * 60_000L
        val delay = ReminderSchedulerLogic.computeDelayMillis(event, now, leadMinutes = 15)
        assertTrue(delay > 0L)
        val specs = ReminderSchedulerLogic.computeReminderSpecs("exam_1", event, now)
        assertEquals(2, specs.size)
    }

    @Test
    fun syncConflictResolver_usesLastWriteWins() {
        assertTrue(SyncConflictResolver.shouldOverwriteLocal(100L, 200L))
        assertFalse(SyncConflictResolver.shouldOverwriteLocal(200L, 100L))
    }
}
