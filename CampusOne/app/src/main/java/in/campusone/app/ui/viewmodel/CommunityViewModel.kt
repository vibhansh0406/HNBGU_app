package `in`.campusone.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.campusone.app.data.preferences.UserPreferencesRepository
import `in`.campusone.app.data.repository.CommunityRepository
import `in`.campusone.app.domain.model.AppNotification
import `in`.campusone.app.domain.model.CommunityCategory
import `in`.campusone.app.domain.model.CommunityComment
import `in`.campusone.app.domain.model.CommunityPost
import `in`.campusone.app.domain.model.CommunitySort
import `in`.campusone.app.domain.model.UserProfile
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class CommunityMode {
    FEED,
    DISCUSSIONS
}

data class CommunityUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isSubmitting: Boolean = false,
    val profile: UserProfile = UserProfile(),
    val mode: CommunityMode = CommunityMode.FEED,
    val query: String = "",
    val category: CommunityCategory? = null,
    val sort: CommunitySort = CommunitySort.LATEST,
    val posts: List<CommunityPost> = emptyList(),
    val visiblePosts: List<CommunityPost> = emptyList(),
    val selectedPost: CommunityPost? = null,
    val comments: List<CommunityComment> = emptyList(),
    val notifications: List<AppNotification> = emptyList(),
    val unreadNotificationCount: Int = 0,
    val hasMore: Boolean = true,
    val isSyncing: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CommunityViewModel @Inject constructor(
    private val repository: CommunityRepository,
    prefs: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CommunityUiState())
    val uiState: StateFlow<CommunityUiState> = _uiState.asStateFlow()

    private val _messages = MutableSharedFlow<String>()
    val messages = _messages.asSharedFlow()

    private val selectedPostId = MutableStateFlow<String?>(null)

    val unreadNotificationCount: StateFlow<Int> = prefs.userProfile
        .distinctUntilChangedBy { it.uid }
        .flatMapLatest { profile ->
            if (profile.uid.isBlank()) {
                MutableStateFlow(0)
            } else {
                repository.observeUnreadNotificationCount(profile.uid)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    init {
        viewModelScope.launch {
            prefs.userProfile.distinctUntilChangedBy { it.uid }.collect { profile ->
                _uiState.update { it.copy(profile = profile) }
                if (profile.uid.isNotBlank()) {
                    refreshFeed(showLoading = true)
                    refreshNotifications()
                }
            }
        }
        viewModelScope.launch {
            repository.observePosts().collect { posts ->
                _uiState.update {
                    it.copy(posts = posts, isLoading = false).withVisiblePosts()
                }
            }
        }
        viewModelScope.launch {
            selectedPostId.flatMapLatest { postId ->
                if (postId == null) MutableStateFlow(emptyList())
                else repository.observeComments(postId)
            }.collect { comments ->
                _uiState.update { it.copy(comments = comments) }
            }
        }
        viewModelScope.launch {
            prefs.userProfile.distinctUntilChangedBy { it.uid }.collectLatest { profile ->
                if (profile.uid.isBlank()) return@collectLatest
                repository.observeNotifications(profile.uid).collect { notifications ->
                    _uiState.update { it.copy(notifications = notifications) }
                }
            }
        }
    }

    fun selectMode(mode: CommunityMode) {
        _uiState.update { it.copy(mode = mode).withVisiblePosts() }
    }

    fun updateQuery(query: String) {
        _uiState.update { it.copy(query = query).withVisiblePosts() }
    }

    fun selectCategory(category: CommunityCategory?) {
        _uiState.update { it.copy(category = category).withVisiblePosts() }
    }

    fun selectSort(sort: CommunitySort) {
        _uiState.update { it.copy(sort = sort).withVisiblePosts() }
    }

    fun refreshFeed(showLoading: Boolean = false) {
        val profile = _uiState.value.profile
        if (profile.uid.isBlank()) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isRefreshing = true,
                    isLoading = if (showLoading) true else it.isLoading,
                    isSyncing = false
                )
            }
            repository.refreshFeed(profile)
                .onFailure { _messages.emit(it.message ?: "Could not refresh the community feed.") }
            _uiState.update { it.copy(isRefreshing = false, isLoading = false, isSyncing = false) }
        }
    }

    fun loadMore() {
        val state = _uiState.value
        if (state.isRefreshing || !state.hasMore) return
        val oldest = state.posts.minOfOrNull { it.createdAtMillis } ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            repository.loadMore(state.profile, oldest)
                .onSuccess { more -> _uiState.update { it.copy(hasMore = more, isRefreshing = false) } }
                .onFailure {
                    _uiState.update { it.copy(isRefreshing = false) }
                    _messages.emit(it.message ?: "Could not load older posts.")
                }
        }
    }

    fun openPost(postId: String) {
        selectedPostId.value = postId
        _uiState.update { state ->
            state.copy(selectedPost = state.posts.firstOrNull { it.id == postId })
        }
        viewModelScope.launch {
            repository.refreshPost(postId)
                .onSuccess { _uiState.update { it.withSelectedPost() } }
        }
    }

    fun closePost() {
        selectedPostId.value = null
        _uiState.update { it.copy(selectedPost = null, comments = emptyList()) }
    }

    fun createPost(title: String, body: String, category: CommunityCategory) {
        if (title.isBlank() || body.isBlank()) {
            viewModelScope.launch { _messages.emit("Add a title and details before posting.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true) }
            repository.createPost(title, body, category)
                .onSuccess { _messages.emit("Post published.") }
                .onFailure { _messages.emit(it.message ?: "Could not publish the post.") }
            _uiState.update { it.copy(isSubmitting = false) }
        }
    }

    fun editPost(postId: String, title: String, body: String) {
        viewModelScope.launch {
            repository.editPost(postId, title, body)
                .onSuccess {
                    _uiState.update { it.withSelectedPost() }
                    _messages.emit("Post updated.")
                }
                .onFailure { _messages.emit(it.message ?: "Could not update the post.") }
        }
    }

    fun deletePost(postId: String) {
        viewModelScope.launch {
            repository.deletePost(postId)
                .onSuccess {
                    closePost()
                    _messages.emit("Post deleted.")
                }
                .onFailure { _messages.emit(it.message ?: "Could not delete the post.") }
        }
    }

    fun addComment(postId: String, body: String) {
        if (body.isBlank()) return
        viewModelScope.launch {
            repository.addComment(postId, body)
                .onSuccess { _uiState.update { it.withSelectedPost() } }
                .onFailure { _messages.emit(it.message ?: "Could not add the comment.") }
        }
    }

    fun deleteComment(postId: String, commentId: String) {
        viewModelScope.launch {
            repository.deleteComment(postId, commentId)
                .onFailure { _messages.emit(it.message ?: "Could not delete the comment.") }
        }
    }

    fun toggleLike(post: CommunityPost) {
        viewModelScope.launch {
            repository.toggleLike(post.id, post.isLiked, post.likeCount)
                .onFailure { _messages.emit(it.message ?: "Could not update the like.") }
        }
    }

    fun reportPost(postId: String, reason: String = "Reported from community feed") {
        viewModelScope.launch {
            repository.reportPost(postId, reason)
                .onSuccess { _messages.emit("Report sent to moderators.") }
                .onFailure { _messages.emit(it.message ?: "Could not send the report.") }
        }
    }

    fun reportComment(commentId: String, reason: String = "Reported from discussion") {
        viewModelScope.launch {
            repository.reportComment(commentId, reason)
                .onSuccess { _messages.emit("Report sent to moderators.") }
                .onFailure { _messages.emit(it.message ?: "Could not send the report.") }
        }
    }

    fun refreshNotifications() {
        val uid = _uiState.value.profile.uid
        if (uid.isBlank()) return
        viewModelScope.launch {
            repository.refreshNotifications(uid)
                .onFailure { _messages.emit(it.message ?: "Could not refresh notifications.") }
        }
    }

    fun markNotificationRead(notificationId: String) {
        val uid = _uiState.value.profile.uid
        if (uid.isBlank()) return
        viewModelScope.launch { repository.markNotificationRead(notificationId, uid) }
    }

    fun markAllNotificationsRead() {
        val uid = _uiState.value.profile.uid
        if (uid.isBlank()) return
        viewModelScope.launch {
            repository.markAllNotificationsRead(uid)
            _messages.emit("All notifications marked as read.")
        }
    }

    private fun CommunityUiState.withVisiblePosts(): CommunityUiState {
        val filtered = posts.filter { post ->
            val categoryMatch = when {
                category != null -> post.category == category
                mode == CommunityMode.DISCUSSIONS -> post.category != CommunityCategory.ANNOUNCEMENT
                else -> true
            }
            val queryMatch = query.isBlank() ||
                post.title.contains(query, ignoreCase = true) ||
                post.body.contains(query, ignoreCase = true) ||
                post.authorName.contains(query, ignoreCase = true)
            categoryMatch && queryMatch
        }
        val sorted = when (sort) {
            CommunitySort.RELEVANT -> filtered.sortedWith(
                compareByDescending<CommunityPost> { it.likeCount }
                    .thenByDescending { it.createdAtMillis }
            )
            CommunitySort.LATEST -> filtered.sortedByDescending { it.createdAtMillis }
        }
        return copy(visiblePosts = sorted)
    }

    private fun CommunityUiState.withSelectedPost(): CommunityUiState {
        val selected = selectedPostId.value ?: return this
        return copy(selectedPost = posts.firstOrNull { it.id == selected })
    }
}
