package `in`.campusone.app.domain.usecase

import `in`.campusone.app.domain.model.EventRsvpStatus

data class EventRsvpSnapshot(
    val goingCount: Int,
    val interestedCount: Int,
    val notGoingCount: Int,
    val userStatus: EventRsvpStatus
)

object CampusLifeEngine {
    const val AUTO_ARCHIVE_WINDOW_MILLIS = 30L * 24L * 60L * 60L * 1000L

    fun transitionRsvp(
        currentGoing: Int,
        currentInterested: Int,
        currentNotGoing: Int,
        previousStatus: EventRsvpStatus,
        newStatus: EventRsvpStatus
    ): EventRsvpSnapshot {
        val counts = mutableMapOf(
            EventRsvpStatus.GOING to currentGoing.coerceAtLeast(0),
            EventRsvpStatus.INTERESTED to currentInterested.coerceAtLeast(0),
            EventRsvpStatus.NOT_GOING to currentNotGoing.coerceAtLeast(0)
        )
        if (previousStatus != newStatus) {
            if (previousStatus in counts) {
                counts[previousStatus] = (counts.getValue(previousStatus) - 1).coerceAtLeast(0)
            }
            if (newStatus in counts) {
                counts[newStatus] = counts.getValue(newStatus) + 1
            }
        }
        return EventRsvpSnapshot(
            goingCount = counts.getValue(EventRsvpStatus.GOING),
            interestedCount = counts.getValue(EventRsvpStatus.INTERESTED),
            notGoingCount = counts.getValue(EventRsvpStatus.NOT_GOING),
            userStatus = newStatus
        )
    }

    fun shouldAutoArchiveLostFound(
        createdAtMillis: Long,
        nowMillis: Long,
        isResolved: Boolean
    ): Boolean = !isResolved && nowMillis - createdAtMillis >= AUTO_ARCHIVE_WINDOW_MILLIS
}
