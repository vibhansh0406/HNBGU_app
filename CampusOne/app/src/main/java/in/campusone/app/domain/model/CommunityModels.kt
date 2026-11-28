package `in`.campusone.app.domain.model

enum class CommunityCategory {
    GENERAL,
    ACADEMIC,
    CAMPUS_LIFE,
    QUESTION,
    ANNOUNCEMENT
}

enum class CommunitySort {
    LATEST,
    RELEVANT
}

data class CommunityPost(
    val id: String,
    val collegeId: String,
    val authorUid: String,
    val authorName: String,
    val authorRole: UserRole,
    val category: CommunityCategory,
    val title: String,
    val body: String,
    val likeCount: Int = 0,
    val commentCount: Int = 0,
    val isLiked: Boolean = false,
    val createdAtMillis: Long,
    val updatedAtMillis: Long = createdAtMillis
)

data class CommunityComment(
    val id: String,
    val postId: String,
    val authorUid: String,
    val authorName: String,
    val authorRole: UserRole,
    val body: String,
    val createdAtMillis: Long
)

data class AppNotification(
    val id: String,
    val type: String,
    val title: String,
    val body: String,
    val targetType: String,
    val targetId: String,
    val isRead: Boolean,
    val createdAtMillis: Long,
    val attachmentUrl: String? = null,
    val attachmentType: String? = null
)
