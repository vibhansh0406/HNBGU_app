package `in`.campusone.app.domain.usecase

import `in`.campusone.app.domain.model.EventRsvpStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class CampusLifeEngineTest {
    @Test
    fun rsvpTransitionMovesOneCountAndAllowsClearingStatus() {
        val changed = CampusLifeEngine.transitionRsvp(
            currentGoing = 4,
            currentInterested = 2,
            currentNotGoing = 1,
            previousStatus = EventRsvpStatus.GOING,
            newStatus = EventRsvpStatus.INTERESTED
        )
        assertEquals(3, changed.goingCount)
        assertEquals(3, changed.interestedCount)
        assertEquals(1, changed.notGoingCount)

        val cleared = CampusLifeEngine.transitionRsvp(
            currentGoing = changed.goingCount,
            currentInterested = changed.interestedCount,
            currentNotGoing = changed.notGoingCount,
            previousStatus = EventRsvpStatus.INTERESTED,
            newStatus = EventRsvpStatus.NONE
        )
        assertEquals(3, cleared.goingCount)
        assertEquals(2, cleared.interestedCount)
        assertEquals(EventRsvpStatus.NONE, cleared.userStatus)
    }

    @Test
    fun autoArchiveHasThirtyDayBoundaryAndSkipsResolvedItems() {
        val now = CampusLifeEngine.AUTO_ARCHIVE_WINDOW_MILLIS
        assertEquals(
            true,
            CampusLifeEngine.shouldAutoArchiveLostFound(0L, now, isResolved = false)
        )
        assertEquals(
            false,
            CampusLifeEngine.shouldAutoArchiveLostFound(0L, now - 1L, isResolved = false)
        )
        assertEquals(
            false,
            CampusLifeEngine.shouldAutoArchiveLostFound(0L, now, isResolved = true)
        )
    }
}
