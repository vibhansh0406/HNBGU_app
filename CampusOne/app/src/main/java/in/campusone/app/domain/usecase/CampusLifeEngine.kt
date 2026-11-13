package `in`.campusone.app.domain.usecase

import `in`.campusone.app.domain.model.EventRsvpStatus

object CampusLifeEngine {

    /** 30-day auto-archive window for Lost & Found items. */
    val AUTO_ARCHIVE_WINDOW_MILLIS: Long = 30L * 24 * 60 * 60 * 1000

    /**
     * Computes the new RSVP counts and the user's resulting status when transitioning
     * from `previousStatus` to `newStatus` for a 3-state RSVP (Going / Interested / Not Going).
     */
    fun transitionRsvp(
        currentGoing: Int,
        currentInterested: Int,
        currentNotGoing: Int,
        previousStatus: EventRsvpStatus,
        newStatus: EventRsvpStatus
    ): RsvpSnapshot {
        var going = currentGoing.coerceAtLeast(0)
        var interested = currentInterested.coerceAtLeast(0)
        var notGoing = currentNotGoing.coerceAtLeast(0)

        // Remove the user's previous vote if they had one
        when (previousStatus) {
            EventRsvpStatus.GOING -> going = (going - 1).coerceAtLeast(0)
            EventRsvpStatus.INTERESTED -> interested = (interested - 1).coerceAtLeast(0)
            EventRsvpStatus.NOT_GOING -> notGoing = (notGoing - 1).coerceAtLeast(0)
            EventRsvpStatus.NONE -> Unit
        }

        // Apply the new vote
        when (newStatus) {
            EventRsvpStatus.GOING -> going++
            EventRsvpStatus.INTERESTED -> interested++
            EventRsvpStatus.NOT_GOING -> notGoing++
            EventRsvpStatus.NONE -> Unit
        }

        return RsvpSnapshot(going, interested, notGoing, newStatus)
    }

    data class RsvpSnapshot(
        val goingCount: Int,
        val interestedCount: Int,
        val notGoingCount: Int,
        val userStatus: EventRsvpStatus
    )
}
