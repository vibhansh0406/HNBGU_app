package `in`.campusone.app.data.repository

import `in`.campusone.app.data.local.dao.CommunityDao
import `in`.campusone.app.data.local.entity.AppNotificationEntity
import `in`.campusone.app.data.local.entity.CommunityCommentEntity
import `in`.campusone.app.data.local.entity.CommunityPostEntity
import `in`.campusone.app.data.preferences.UserPreferencesRepository
import `in`.campusone.app.data.remote.SupabaseSyncRepository
import `in`.campusone.app.domain.model.AppNotification
import `in`.campusone.app.domain.model.CommunityCategory
import `in`.campusone.app.domain.model.CommunityComment
import `in`.campusone.app.domain.model.CommunityPost
import `in`.campusone.app.domain.model.UserProfile
import `in`.campusone.app.domain.model.UserRole
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

@Singleton
class CommunityRepository @Inject constructor(
    private val dao: CommunityDao,
    private val supabaseSync: SupabaseSyncRepository,
    private val prefs: UserPreferencesRepository
) {

    fun observePosts(): Flow<List<CommunityPost>> =
        dao.observePosts().map { list -> list.map { it.toModel() } }

    fun observeComments(postId: String): Flow<List<CommunityComment>> =
        dao.observeComments(postId).map { list -> list.map { it.toModel() } }

    fun observeNotifications(userId: String): Flow<List<AppNotification>> =
        dao.observeNotifications(userId).map { list -> list.map { it.toModel() } }

    fun observeUnreadNotificationCount(userId: String): Flow<Int> =
        dao.observeUnreadNotificationCount(userId)

    suspend fun refreshFeed(profile: UserProfile): Result<Unit> = runCatching {
        if (!supabaseSync.isSyncedEnabled()) return@runCatching
        val remote = supabaseSync.downloadCommunityPosts(profile.collegeId, limit = PAGE_SIZE)
        val liked = supabaseSync.downloadMyCommunityLikes(profile.uid, remote.mapNotNull { it["id"] as? String })
        dao.upsertPosts(remote.map { it.toPostEntity(liked) })
    }

    suspend fun loadMore(profile: UserProfile, beforeCreatedAt: Long): Result<Boolean> = runCatching {
        if (!supabaseSync.isSyncedEnabled()) return@runCatching false
        val remote = supabaseSync.downloadCommunityPosts(
            profile.collegeId,
            beforeCreatedAt = beforeCreatedAt,
            limit = PAGE_SIZE
        )
        val liked = supabaseSync.downloadMyCommunityLikes(profile.uid, remote.mapNotNull { it["id"] as? String })
        dao.upsertPosts(remote.map { it.toPostEntity(liked) })
        remote.size >= PAGE_SIZE
    }

    suspend fun refreshPost(postId: String): Result<Unit> = runCatching {
        if (!supabaseSync.isSyncedEnabled()) return@runCatching
        val remote = supabaseSync.downloadCommunityPost(postId) ?: return@runCatching
        val uid = prefs.userProfile.first().uid
        val liked = supabaseSync.downloadMyCommunityLikes(uid, listOf(postId))
        dao.upsertPost(remote.toPostEntity(liked))
        refreshComments(postId)
    }

    suspend fun refreshComments(postId: String): Result<Unit> = runCatching {
        if (!supabaseSync.isSyncedEnabled()) return@runCatching
        val remote = supabaseSync.downloadCommunityComments(postId)
        dao.replaceComments(postId, remote.map { it.toCommentEntity() })
    }

    suspend fun createPost(
        title: String,
        body: String,
        category: CommunityCategory
    ): Result<String> = runCatching {
        val profile = prefs.userProfile.first()
        val now = System.currentTimeMillis()
        val id = "post_${UUID.randomUUID()}"
        val entity = CommunityPostEntity(
            id = id,
            collegeId = profile.collegeId,
            authorUid = profile.uid,
            authorName = profile.name.ifBlank { "Student" },
            authorRole = profile.role,
            category = category.name,
            title = title.trim(),
            body = body.trim(),
            createdAtMillis = now,
            updatedAtMillis = now
        )
        dao.upsertPost(entity)
        val synced = supabaseSync.uploadCommunityPost(mapOf(
            "id" to entity.id,
            "college_id" to entity.collegeId,
            "author_uid" to entity.authorUid,
            "author_name" to entity.authorName,
            "author_role" to entity.authorRole.name,
            "category" to entity.category,
            "title" to entity.title,
            "body" to entity.body,
            "like_count" to 0,
            "comment_count" to 0,
            "report_count" to 0,
            "is_hidden" to false,
            "created_at" to entity.createdAtMillis,
            "updated_at" to entity.updatedAtMillis
        ))
        if (!synced) throw IllegalStateException("Post saved on device. It will sync when the connection is back.")
        id
    }

    suspend fun editPost(postId: String, title: String, body: String): Result<Unit> = runCatching {
        val now = System.currentTimeMillis()
        dao.updatePostContent(postId, title.trim(), body.trim(), now)
        if (!supabaseSync.updateCommunityPost(postId, title.trim(), body.trim(), now)) {
            throw IllegalStateException("Edit saved on device. Sync failed.")
        }
    }

    suspend fun deletePost(postId: String): Result<Unit> = runCatching {
        dao.deletePost(postId)
        if (!supabaseSync.deleteCommunityPost(postId)) {
            throw IllegalStateException("Removed on device. Server delete failed.")
        }
    }

    suspend fun addComment(postId: String, body: String): Result<Unit> = runCatching {
        val profile = prefs.userProfile.first()
        val now = System.currentTimeMillis()
        val entity = CommunityCommentEntity(
            id = "comment_${UUID.randomUUID()}",
            postId = postId,
            authorUid = profile.uid,
            authorName = profile.name.ifBlank { "Student" },
            authorRole = profile.role,
            body = body.trim(),
            createdAtMillis = now
        )
        dao.upsertComment(entity)
        val comments = dao.getComments(postId)
        dao.setPostCommentCount(postId, comments.size)
        val synced = supabaseSync.uploadCommunityComment(mapOf(
            "id" to entity.id,
            "post_id" to postId,
            "author_uid" to entity.authorUid,
            "author_name" to entity.authorName,
            "author_role" to entity.authorRole.name,
            "body" to entity.body,
            "is_hidden" to false,
            "created_at" to entity.createdAtMillis
        ))
        if (!synced) throw IllegalStateException("Comment saved on device. Sync failed.")
    }

    suspend fun deleteComment(postId: String, commentId: String): Result<Unit> = runCatching {
        dao.deleteComment(commentId)
        dao.setPostCommentCount(postId, dao.getComments(postId).size)
        if (!supabaseSync.deleteCommunityComment(commentId)) {
            throw IllegalStateException("Comment removed on device. Server delete failed.")
        }
    }

    suspend fun toggleLike(postId: String, currentlyLiked: Boolean, currentCount: Int): Result<Unit> =
        runCatching {
            val uid = prefs.userProfile.first().uid
            val nextLiked = !currentlyLiked
            dao.setPostLike(postId, (currentCount + if (nextLiked) 1 else -1).coerceAtLeast(0), nextLiked)
            if (!supabaseSync.setCommunityLike(postId, uid, nextLiked)) {
                dao.setPostLike(postId, currentCount, currentlyLiked)
                throw IllegalStateException("Could not update like. Try again.")
            }
        }

    suspend fun reportPost(postId: String, reason: String): Result<Unit> = runCatching {
        if (!supabaseSync.submitContentReport("POST", postId, reason)) {
            throw IllegalStateException("Report could not be submitted. Try again.")
        }
    }

    suspend fun reportComment(commentId: String, reason: String): Result<Unit> = runCatching {
        if (!supabaseSync.submitContentReport("COMMENT", commentId, reason)) {
            throw IllegalStateException("Report could not be submitted. Try again.")
        }
    }

    suspend fun refreshNotifications(userId: String): Result<Unit> = runCatching {
        if (!supabaseSync.isSyncedEnabled()) return@runCatching
        val remote = supabaseSync.downloadNotifications(userId)
        dao.upsertNotifications(remote.map { it.toNotificationEntity(userId) })
    }

    suspend fun markNotificationRead(notificationId: String, userId: String) {
        dao.markNotificationRead(notificationId, userId)
        supabaseSync.markNotificationRead(notificationId)
    }

    suspend fun markAllNotificationsRead(userId: String) {
        dao.markAllNotificationsRead(userId)
        supabaseSync.markAllNotificationsRead(userId)
    }

    suspend fun deleteNotification(notificationId: String, userId: String) {
        dao.deleteNotification(notificationId, userId)
        supabaseSync.deleteNotification(notificationId)
    }

    private fun Map<String, Any>.toPostEntity(liked: Set<String>): CommunityPostEntity {
        val id = this["id"] as? String ?: ""
        return CommunityPostEntity(
            id = id,
            collegeId = this["college_id"] as? String ?: "hnbgu",
            authorUid = this["author_uid"] as? String ?: "",
            authorName = this["author_name"] as? String ?: "Student",
            authorRole = role(this["author_role"] as? String),
            category = this["category"] as? String ?: CommunityCategory.GENERAL.name,
            title = this["title"] as? String ?: "",
            body = this["body"] as? String ?: "",
            likeCount = number(this["like_count"]),
            commentCount = number(this["comment_count"]),
            isLiked = id in liked,
            createdAtMillis = long(this["created_at"]),
            updatedAtMillis = long(this["updated_at"])
        )
    }

    private fun Map<String, Any>.toCommentEntity(): CommunityCommentEntity = CommunityCommentEntity(
        id = this["id"] as? String ?: "",
        postId = this["post_id"] as? String ?: "",
        authorUid = this["author_uid"] as? String ?: "",
        authorName = this["author_name"] as? String ?: "Student",
        authorRole = role(this["author_role"] as? String),
        body = this["body"] as? String ?: "",
        createdAtMillis = long(this["created_at"])
    )

    private fun Map<String, Any>.toNotificationEntity(userId: String): AppNotificationEntity =
        AppNotificationEntity(
            id = this["id"] as? String ?: "",
            userId = userId,
            type = this["type"] as? String ?: "SYSTEM",
            title = this["title"] as? String ?: "",
            body = this["body"] as? String ?: "",
            targetType = this["target_type"] as? String ?: "",
            targetId = this["target_id"] as? String ?: "",
            isRead = this["is_read"] == true,
            createdAtMillis = long(this["created_at"]),
            attachmentUrl = this["attachment_url"] as? String,
            attachmentType = this["attachment_type"] as? String
        )

    private fun CommunityPostEntity.toModel() = CommunityPost(
        id = id,
        collegeId = collegeId,
        authorUid = authorUid,
        authorName = authorName,
        authorRole = authorRole,
        category = runCatching { CommunityCategory.valueOf(category) }
            .getOrDefault(CommunityCategory.GENERAL),
        title = title,
        body = body,
        likeCount = likeCount,
        commentCount = commentCount,
        isLiked = isLiked,
        createdAtMillis = createdAtMillis,
        updatedAtMillis = updatedAtMillis
    )

    private fun CommunityCommentEntity.toModel() = CommunityComment(
        id = id,
        postId = postId,
        authorUid = authorUid,
        authorName = authorName,
        authorRole = authorRole,
        body = body,
        createdAtMillis = createdAtMillis
    )

    private fun AppNotificationEntity.toModel() = AppNotification(
        id = id,
        type = type,
        title = title,
        body = body,
        targetType = targetType,
        targetId = targetId,
        isRead = isRead,
        createdAtMillis = createdAtMillis,
        attachmentUrl = attachmentUrl,
        attachmentType = attachmentType
    )

    private fun role(value: String?): UserRole = when (value?.uppercase()) {
        "TEACHER", "CLUB_ADMIN" -> UserRole.MODERATOR
        "SUPER_ADMIN" -> UserRole.ADMIN
        else -> runCatching { UserRole.valueOf(value ?: "") }.getOrDefault(UserRole.STUDENT)
    }

    private fun number(value: Any?): Int = when (value) {
        is Number -> value.toInt()
        else -> 0
    }

    private fun long(value: Any?): Long = when (value) {
        is Number -> value.toLong()
        else -> 0L
    }

    companion object {
        const val PAGE_SIZE = 20
    }
}
