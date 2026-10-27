package `in`.campusone.app.domain.usecase

import `in`.campusone.app.domain.model.DirectoryContactItem
import `in`.campusone.app.domain.model.RsvpStatus

/**
 * Pure Kotlin helper for searching the offline Campus Directory and computing RSVP/Lost-and-Found logic.
 */
object CampusLifeEngine {
    const val AUTO_ARCHIVE_DAYS = 30L
    private const val MILLIS_PER_DAY = 86_400_000L

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
        return (nowMillis - postedAtMillis) >= (AUTO_ARCHIVE_DAYS * MILLIS_PER_DAY)
    }

    fun updateRsvpCount(
        currentGoing: Int,
        previousStatus: RsvpStatus?,
        newStatus: RsvpStatus
    ): Int {
        var count = currentGoing
        if (previousStatus == RsvpStatus.GOING && newStatus != RsvpStatus.GOING) {
            count = (count - 1).coerceAtLeast(0)
        } else if (previousStatus != RsvpStatus.GOING && newStatus == RsvpStatus.GOING) {
            count += 1
        }
        return count
    }
}
