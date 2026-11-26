package `in`.campusone.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import `in`.campusone.app.data.local.entity.AppNotificationEntity
import `in`.campusone.app.data.local.entity.CommunityCommentEntity
import `in`.campusone.app.data.local.entity.CommunityPostEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CommunityDao {
    @Query("SELECT * FROM community_posts ORDER BY createdAtMillis DESC")
    fun observePosts(): Flow<List<CommunityPostEntity>>

    @Query("SELECT * FROM community_posts WHERE id = :postId")
    fun observePost(postId: String): Flow<CommunityPostEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPosts(posts: List<CommunityPostEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPost(post: CommunityPostEntity)

    @Query("UPDATE community_posts SET likeCount = :likeCount, isLiked = :isLiked WHERE id = :postId")
    suspend fun setPostLike(postId: String, likeCount: Int, isLiked: Boolean)

    @Query("UPDATE community_posts SET commentCount = :count WHERE id = :postId")
    suspend fun setPostCommentCount(postId: String, count: Int)

    @Query("UPDATE community_posts SET title = :title, body = :body, updatedAtMillis = :updatedAtMillis WHERE id = :postId")
    suspend fun updatePostContent(postId: String, title: String, body: String, updatedAtMillis: Long)

    @Query("DELETE FROM community_posts WHERE id = :postId")
    suspend fun deletePost(postId: String)

    @Query("SELECT * FROM community_comments WHERE postId = :postId ORDER BY createdAtMillis ASC")
    fun observeComments(postId: String): Flow<List<CommunityCommentEntity>>

    @Query("SELECT * FROM community_comments WHERE postId = :postId ORDER BY createdAtMillis ASC")
    suspend fun getComments(postId: String): List<CommunityCommentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertComments(comments: List<CommunityCommentEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertComment(comment: CommunityCommentEntity)

    @Query("DELETE FROM community_comments WHERE id = :commentId")
    suspend fun deleteComment(commentId: String)

    @Transaction
    suspend fun replaceComments(postId: String, comments: List<CommunityCommentEntity>) {
        comments.forEach { upsertComment(it) }
        setPostCommentCount(postId, comments.size)
    }

    @Query("SELECT * FROM app_notifications WHERE userId = :userId ORDER BY createdAtMillis DESC LIMIT 200")
    fun observeNotifications(userId: String): Flow<List<AppNotificationEntity>>

    @Query("SELECT COUNT(*) FROM app_notifications WHERE userId = :userId AND isRead = 0")
    fun observeUnreadNotificationCount(userId: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNotifications(notifications: List<AppNotificationEntity>)

    @Query("UPDATE app_notifications SET isRead = 1 WHERE id = :notificationId AND userId = :userId")
    suspend fun markNotificationRead(notificationId: String, userId: String)

    @Query("UPDATE app_notifications SET isRead = 1 WHERE userId = :userId")
    suspend fun markAllNotificationsRead(userId: String)

    @Query("DELETE FROM app_notifications WHERE id = :notificationId AND userId = :userId")
    suspend fun deleteNotification(notificationId: String, userId: String)
}
