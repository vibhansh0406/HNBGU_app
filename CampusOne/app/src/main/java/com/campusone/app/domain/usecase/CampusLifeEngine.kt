package com.campusone.app.domain.usecase

import com.campusone.app.domain.model.DirectoryContactItem
import com.campusone.app.domain.model.RsvpStatus

data class RsvpTransitionResult(
    val goingCount: Int,
    val interestedCount: Int,
    val currentStatus: RsvpStatus
)

object CampusLifeEngine {
    const val AUTO_ARCHIVE_DAYS = 30L
    private const val MILLIS_PER_DAY = 86_400_000L
    const val AUTO_ARCHIVE_WINDOW_MILLIS: Long = AUTO_ARCHIVE_DAYS * MILLIS_PER_DAY

    fun filterDirectory(
        items: List<DirectoryContactItem>,
        query: String,
        categoryFilter: String? = null
    ): List<DirectoryContactItem> {
        val trimmed = query.trim().lowercase()
        return items.filter { item ->
            val matchesCategory = categoryFilter.isNullOrBlank() ||
                categoryFilter == "ALL" ||
                item.category.equals(categoryFilter, ignoreCase = true)
            val matchesQuery = trimmed.isEmpty() ||
                item.name.lowercase().contains(trimmed) ||
                item.department.lowercase().contains(trimmed) ||
                item.designation.lowercase().contains(trimmed) ||
                item.officeLocation.lowercase().contains(trimmed) ||
                item.category.lowercase().contains(trimmed)
            matchesCategory && matchesQuery
        }
    }

    fun isLostFoundExpired(postedAtMillis: Long, nowMillis: Long): Boolean {
        return (nowMillis - postedAtMillis) >= AUTO_ARCHIVE_WINDOW_MILLIS
    }

    fun transitionRsvp(
        currentGoing: Int,
        currentInterested: Int,
        previousStatus: RsvpStatus?,
        newStatus: RsvpStatus
    ): RsvpTransitionResult {
        var going = currentGoing
        var interested = currentInterested

        if (previousStatus == RsvpStatus.GOING) {
            going = (going - 1).coerceAtLeast(0)
        } else if (previousStatus == RsvpStatus.INTERESTED) {
            interested = (interested - 1).coerceAtLeast(0)
        }

        if (newStatus == RsvpStatus.GOING) {
            going += 1
        } else if (newStatus == RsvpStatus.INTERESTED) {
            interested += 1
        }

        return RsvpTransitionResult(
            goingCount = going,
            interestedCount = interested,
            currentStatus = newStatus
        )
    }

    fun updateRsvpCount(
        currentGoing: Int,
        previousStatus: RsvpStatus?,
        newStatus: RsvpStatus
    ): Int {
        return transitionRsvp(
            currentGoing = currentGoing,
            currentInterested = 0,
            previousStatus = previousStatus,
            newStatus = newStatus
        ).goingCount
    }
}
