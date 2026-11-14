package `in`.campusone.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import `in`.campusone.app.domain.model.UserRole

@Entity(
    tableName = "community_posts",
    indices = [
        Index(value = ["createdAtMillis"]),
        Index(value = ["category", "createdAtMillis"])
    ]
)
data class CommunityPostEntity(
    @PrimaryKey val id: String,
    val collegeId: String,
    val authorUid: String,
    val authorName: String,
    val authorRole: UserRole,
    val category: String,
    val title: String,
    val body: String,
    val likeCount: Int = 0,
    val commentCount: Int = 0,
    val isLiked: Boolean = false,
    val createdAtMillis: Long,
    val updatedAtMillis: Long = createdAtMillis
)

@Entity(
    tableName = "community_comments",
    indices = [Index(value = ["postId", "createdAtMillis"])]
)
data class CommunityCommentEntity(
    @PrimaryKey val id: String,
    val postId: String,
    val authorUid: String,
    val authorName: String,
    val authorRole: UserRole,
    val body: String,
    val createdAtMillis: Long
)

@Entity(
    tableName = "app_notifications",
    indices = [Index(value = ["userId", "isRead", "createdAtMillis"])]
)
data class AppNotificationEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val type: String,
    val title: String,
    val body: String,
    val targetType: String,
    val targetId: String,
    val isRead: Boolean = false,
    val createdAtMillis: Long
)
