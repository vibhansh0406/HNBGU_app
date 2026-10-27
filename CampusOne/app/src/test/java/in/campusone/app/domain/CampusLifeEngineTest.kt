package `in`.campusone.app.domain

import `in`.campusone.app.domain.model.DirectoryContactItem
import `in`.campusone.app.domain.model.RsvpStatus
import `in`.campusone.app.domain.usecase.CampusLifeEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CampusLifeEngineTest {

    @Test
    fun filterDirectory_matchesQueryAndCategory() {
        val sample = listOf(
            DirectoryContactItem("1", "Prof. Rauthan", "HOD", "CSE", "FACULTY", "123", "a@b.in", "Chauras", "10-1"),
            DirectoryContactItem("2", "Health Centre", "Doctor", "Medical", "EMERGENCY", "999", "m@b.in", "Birla", "24x7")
        )
        val res = CampusLifeEngine.filterDirectory(sample, "chauras", "FACULTY")
        assertEquals(1, res.size)
        assertEquals("Prof. Rauthan", res.first().name)
    }

    @Test
    fun lostFoundExpiryAndRsvpTransitions_areConsistent() {
        val now = 100L * 86_400_000L
        val posted31DaysAgo = now - (31L * 86_400_000L)
        assertTrue(CampusLifeEngine.isLostFoundExpired(posted31DaysAgo, now))

        val countAfterGoing = CampusLifeEngine.updateRsvpCount(10, null, RsvpStatus.GOING)
        assertEquals(11, countAfterGoing)
        val countAfterLeave = CampusLifeEngine.updateRsvpCount(countAfterGoing, RsvpStatus.GOING, RsvpStatus.NOT_GOING)
        assertEquals(10, countAfterLeave)
    }
}
