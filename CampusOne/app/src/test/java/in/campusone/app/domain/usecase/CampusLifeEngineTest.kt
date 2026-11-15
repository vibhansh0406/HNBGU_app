package `in`.campusone.app.domain.usecase

import `in`.campusone.app.domain.model.EventRsvpStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class CampusLifeEngineTest {

    @Test
    fun `transitioning from NONE to GOING increments going count`() {
        val result = CampusLifeEngine.transitionRsvp(
            currentGoing = 5, currentInterested = 3, currentNotGoing = 2,
            previousStatus = EventRsvpStatus.NONE,
            newStatus = EventRsvpStatus.GOING
        )
        assertEquals(6, result.goingCount)
        assertEquals(3, result.interestedCount)
        assertEquals(2, result.notGoingCount)
        assertEquals(EventRsvpStatus.GOING, result.userStatus)
    }

    @Test
    fun `transitioning from GOING to INTERESTED decrements going and increments interested`() {
        val result = CampusLifeEngine.transitionRsvp(
            currentGoing = 5, currentInterested = 3, currentNotGoing = 2,
            previousStatus = EventRsvpStatus.GOING,
            newStatus = EventRsvpStatus.INTERESTED
        )
        assertEquals(4, result.goingCount)
        assertEquals(4, result.interestedCount)
        assertEquals(2, result.notGoingCount)
        assertEquals(EventRsvpStatus.INTERESTED, result.userStatus)
    }

    @Test
    fun `counts never go below zero when removing a previous vote`() {
        val result = CampusLifeEngine.transitionRsvp(
            currentGoing = 0, currentInterested = 0, currentNotGoing = 0,
            previousStatus = EventRsvpStatus.GOING,
            newStatus = EventRsvpStatus.NOT_GOING
        )
        assertEquals(0, result.goingCount)
        assertEquals(0, result.interestedCount)
        assertEquals(1, result.notGoingCount)
    }

    @Test
    fun `selecting NONE removes the user's previous vote without adding a new one`() {
        val result = CampusLifeEngine.transitionRsvp(
            currentGoing = 5, currentInterested = 3, currentNotGoing = 2,
            previousStatus = EventRsvpStatus.INTERESTED,
            newStatus = EventRsvpStatus.NONE
        )
        assertEquals(5, result.goingCount)
        assertEquals(2, result.interestedCount)
        assertEquals(2, result.notGoingCount)
        assertEquals(EventRsvpStatus.NONE, result.userStatus)
    }
}
