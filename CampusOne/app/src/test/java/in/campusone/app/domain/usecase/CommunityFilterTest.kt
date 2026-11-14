package `in`.campusone.app.domain.usecase

import `in`.campusone.app.domain.model.CommunityCategory
import `in`.campusone.app.domain.model.CommunityPost
import `in`.campusone.app.domain.model.CommunitySort
import `in`.campusone.app.domain.model.UserRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CommunityFilterTest {

    private fun post(
        id: String,
        title: String = "Title $id",
        body: String = "Body $id",
        author: String = "Student",
        category: CommunityCategory = CommunityCategory.GENERAL,
        likes: Int = 0,
        createdAt: Long = 0L
    ) = CommunityPost(
        id = id,
        collegeId = "hnbgu",
        authorUid = "uid-$id",
        authorName = author,
        authorRole = UserRole.STUDENT,
        category = category,
        title = title,
        body = body,
        likeCount = likes,
        commentCount = 0,
        isLiked = false,
        createdAtMillis = createdAt
    )

    @Test
    fun `latest sort orders by newest first`() {
        val posts = listOf(
            post("a", createdAt = 100L),
            post("c", createdAt = 300L),
            post("b", createdAt = 200L)
        )
        val result = CommunityFilter.apply(posts, sort = CommunitySort.LATEST)
        assertEquals(listOf("c", "b", "a"), result.map { it.id })
    }

    @Test
    fun `relevant sort prefers more liked posts and falls back to recency`() {
        val posts = listOf(
            post("low", likes = 1, createdAt = 300L),
            post("high", likes = 9, createdAt = 100L),
            post("tie", likes = 9, createdAt = 200L)
        )
        val result = CommunityFilter.apply(posts, sort = CommunitySort.RELEVANT)
        assertEquals(listOf("tie", "high", "low"), result.map { it.id })
    }

    @Test
    fun `search matches title body and author case insensitively`() {
        val posts = listOf(
            post("t", title = "DBMS Lab"),
            post("b", body = "library timing"),
            post("a", author = "Aditi Rawat"),
            post("x")
        )
        assertEquals(listOf("t"), CommunityFilter.apply(posts, query = "dbms").map { it.id })
        assertEquals(listOf("b"), CommunityFilter.apply(posts, query = "LIBRARY").map { it.id })
        assertEquals(listOf("a"), CommunityFilter.apply(posts, query = "aditi").map { it.id })
    }

    @Test
    fun `blank or whitespace query does not filter anything`() {
        val posts = listOf(post("a"), post("b"))
        assertEquals(2, CommunityFilter.apply(posts, query = "   ").size)
    }

    @Test
    fun `category selection narrows the feed and wins over discussions mode`() {
        val posts = listOf(
            post("g", category = CommunityCategory.GENERAL),
            post("q", category = CommunityCategory.QUESTION),
            post("n", category = CommunityCategory.ANNOUNCEMENT)
        )
        val byCategory = CommunityFilter.apply(posts, category = CommunityCategory.QUESTION)
        assertEquals(listOf("q"), byCategory.map { it.id })

        val byCategoryInDiscussions = CommunityFilter.apply(
            posts,
            category = CommunityCategory.ANNOUNCEMENT,
            discussionsOnly = true
        )
        assertEquals(listOf("n"), byCategoryInDiscussions.map { it.id })
    }

    @Test
    fun `discussions mode hides announcement posts`() {
        val posts = listOf(
            post("g", category = CommunityCategory.GENERAL),
            post("n", category = CommunityCategory.ANNOUNCEMENT),
            post("c", category = CommunityCategory.CAMPUS_LIFE)
        )
        val result = CommunityFilter.apply(posts, discussionsOnly = true)
        assertEquals(setOf("g", "c"), result.map { it.id }.toSet())
    }

    @Test
    fun `empty input returns an empty list`() {
        assertTrue(CommunityFilter.apply(emptyList()).isEmpty())
    }
}
