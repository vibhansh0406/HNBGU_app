package `in`.campusone.app.domain.usecase

import `in`.campusone.app.domain.model.CommunityCategory
import `in`.campusone.app.domain.model.CommunityPost
import `in`.campusone.app.domain.model.CommunitySort

/**
 * Pure, deterministic filtering and ordering rules for the community feed.
 * Kept out of the ViewModel so the behaviour is unit-testable and identical
 * for every entry point (feed, discussions, deep links).
 */
object CommunityFilter {

    fun apply(
        posts: List<CommunityPost>,
        query: String = "",
        category: CommunityCategory? = null,
        discussionsOnly: Boolean = false,
        sort: CommunitySort = CommunitySort.LATEST
    ): List<CommunityPost> {
        val filtered = posts.filter { post ->
            val categoryMatch = when {
                category != null -> post.category == category
                discussionsOnly -> post.category != CommunityCategory.ANNOUNCEMENT
                else -> true
            }
            val trimmedQuery = query.trim()
            val queryMatch = trimmedQuery.isEmpty() ||
                post.title.contains(trimmedQuery, ignoreCase = true) ||
                post.body.contains(trimmedQuery, ignoreCase = true) ||
                post.authorName.contains(trimmedQuery, ignoreCase = true)
            categoryMatch && queryMatch
        }
        return when (sort) {
            CommunitySort.LATEST -> filtered.sortedByDescending { it.createdAtMillis }
            CommunitySort.RELEVANT -> filtered.sortedWith(
                compareByDescending<CommunityPost> { it.likeCount }
                    .thenByDescending { it.createdAtMillis }
            )
        }
    }
}
