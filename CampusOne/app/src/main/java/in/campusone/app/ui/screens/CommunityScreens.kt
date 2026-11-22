package `in`.campusone.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import `in`.campusone.app.R
import `in`.campusone.app.domain.model.AppNotification
import `in`.campusone.app.domain.model.CommunityCategory
import `in`.campusone.app.domain.model.CommunityComment
import `in`.campusone.app.domain.model.CommunityPost
import `in`.campusone.app.domain.model.CommunitySort
import `in`.campusone.app.domain.model.UserProfile
import `in`.campusone.app.domain.model.UserRole
import `in`.campusone.app.ui.components.C1Avatar
import `in`.campusone.app.ui.components.C1Button
import `in`.campusone.app.ui.components.C1ButtonIntent
import `in`.campusone.app.ui.components.C1EmptyState
import `in`.campusone.app.ui.components.C1SurfaceCard
import `in`.campusone.app.ui.components.C1LoadingState
import `in`.campusone.app.ui.components.C1StatusChip
import `in`.campusone.app.ui.components.C1TextField
import `in`.campusone.app.ui.components.C1Tone
import androidx.compose.foundation.layout.PaddingValues
import `in`.campusone.app.ui.theme.C1Spacing
import `in`.campusone.app.ui.viewmodel.CommunityMode
import `in`.campusone.app.ui.viewmodel.CommunityUiState
import `in`.campusone.app.ui.viewmodel.CommunityViewModel
import `in`.campusone.app.ui.viewmodel.CoreUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CommunityHubScreen(
    viewModel: CommunityViewModel,
    onBack: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenProfile: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showComposer by remember { mutableStateOf(false) }
    var editingPost by remember { mutableStateOf<CommunityPost?>(null) }
    var reportTarget by remember { mutableStateOf<Pair<String, String>?>(null) }
    var deletePostTarget by remember { mutableStateOf<CommunityPost?>(null) }
    var deleteCommentTarget by remember { mutableStateOf<CommunityComment?>(null) }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(it) }
    }
    LaunchedEffect(Unit) {
        viewModel.refreshFeed()
        viewModel.refreshNotifications()
    }

    BackHandler(enabled = uiState.selectedPost != null) {
        viewModel.closePost()
    }

    val selectedPost = uiState.selectedPost

    Box(modifier = Modifier.fillMaxSize()) {
        if (selectedPost != null) {
            CommunityPostDetail(
                post = selectedPost,
                comments = uiState.comments,
                currentUid = uiState.profile.uid,
                onBack = viewModel::closePost,
                onToggleLike = viewModel::toggleLike,
                onAddComment = viewModel::addComment,
                onDeleteComment = { comment -> deleteCommentTarget = comment },
                onEditPost = { editingPost = it },
                onDeletePost = { deletePostTarget = it },
                onReportPost = { reportTarget = "POST" to it.id },
                onReportComment = { comment -> reportTarget = "COMMENT" to comment.id }
            )
        } else {
            CommunityFeed(
                uiState = uiState,
                onBack = onBack,
                onOpenNotifications = onOpenNotifications,
                onOpenProfile = onOpenProfile,
                onLoadMore = viewModel::loadMore,
                onRefresh = { viewModel.refreshFeed() },
                onModeChange = viewModel::selectMode,
                onQueryChange = viewModel::updateQuery,
                onCategoryChange = viewModel::selectCategory,
                onSortChange = viewModel::selectSort,
                onOpenPost = viewModel::openPost,
                onToggleLike = viewModel::toggleLike,
                onReportPost = { reportTarget = "POST" to it.id },
                onCreatePost = { showComposer = true }
            )
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(C1Spacing.lg)
        )
    }

    if (showComposer || editingPost != null) {
        PostComposerDialog(
            existing = editingPost,
            isSubmitting = uiState.isSubmitting,
            onDismiss = {
                showComposer = false
                editingPost = null
            },
            onSubmit = { title, body, category ->
                val editing = editingPost
                if (editing == null) {
                    viewModel.createPost(title, body, category)
                } else {
                    viewModel.editPost(editing.id, title, body)
                }
                showComposer = false
                editingPost = null
            }
        )
    }

    reportTarget?.let { (type, id) ->
        ReportDialog(
            onDismiss = { reportTarget = null },
            onSubmit = { reason ->
                if (type == "POST") viewModel.reportPost(id, reason) else viewModel.reportComment(id, reason)
                reportTarget = null
            }
        )
    }

    deletePostTarget?.let { post ->
        ConfirmDialog(
            title = stringResource(R.string.community_delete_post_title),
            message = stringResource(R.string.community_delete_post_confirm),
            confirmLabel = stringResource(R.string.community_delete),
            onDismiss = { deletePostTarget = null },
            onConfirm = {
                viewModel.deletePost(post.id)
                deletePostTarget = null
            }
        )
    }

    deleteCommentTarget?.let { comment ->
        ConfirmDialog(
            title = stringResource(R.string.community_delete_comment_title),
            message = stringResource(R.string.community_delete_comment_confirm),
            confirmLabel = stringResource(R.string.community_delete),
            onDismiss = { deleteCommentTarget = null },
            onConfirm = {
                viewModel.deleteComment(comment.postId, comment.id)
                deleteCommentTarget = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CommunityFeed(
    uiState: CommunityUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenProfile: () -> Unit,
    onLoadMore: () -> Unit,
    onModeChange: (CommunityMode) -> Unit,
    onQueryChange: (String) -> Unit,
    onCategoryChange: (CommunityCategory?) -> Unit,
    onSortChange: (CommunitySort) -> Unit,
    onOpenPost: (String) -> Unit,
    onToggleLike: (CommunityPost) -> Unit,
    onReportPost: (CommunityPost) -> Unit,
    onCreatePost: () -> Unit
) {
    Column(modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = C1Spacing.lg, vertical = C1Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.action_back))
                }
                Column {
                    Text(
                        text = stringResource(R.string.community_title),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.semantics { heading() }
                    )
                    Text(
                        text = stringResource(R.string.community_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Row {
                IconButton(onClick = onOpenNotifications) {
                    Icon(
                        Icons.Default.Notifications,
                        contentDescription = stringResource(R.string.notifications_title)
                    )
                }
                IconButton(onClick = onOpenProfile) {
                    Icon(Icons.Default.Person, contentDescription = stringResource(R.string.profile_title))
                }
            }
        }

        // Mode tabs
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = C1Spacing.lg),
            horizontalArrangement = Arrangement.spacedBy(C1Spacing.sm)
        ) {
            item {
                FilterChip(
                    selected = uiState.mode == CommunityMode.FEED,
                    onClick = { onModeChange(CommunityMode.FEED) },
                    label = { Text(stringResource(R.string.community_tab_feed)) }
                )
            }
            item {
                FilterChip(
                    selected = uiState.mode == CommunityMode.DISCUSSIONS,
                    onClick = { onModeChange(CommunityMode.DISCUSSIONS) },
                    label = { Text(stringResource(R.string.community_tab_discussions)) }
                )
            }
            item {
                FilterChip(
                    selected = uiState.sort == CommunitySort.LATEST,
                    onClick = { onSortChange(CommunitySort.LATEST) },
                    label = { Text(stringResource(R.string.community_sort_latest)) }
                )
            }
            item {
                FilterChip(
                    selected = uiState.sort == CommunitySort.RELEVANT,
                    onClick = { onSortChange(CommunitySort.RELEVANT) },
                    label = { Text(stringResource(R.string.community_sort_relevant)) }
                )
            }
        }

        // Search
        C1TextField(
            value = uiState.query,
            onValueChange = onQueryChange,
            label = stringResource(R.string.community_search_hint),
            modifier = Modifier.padding(horizontal = C1Spacing.lg, vertical = C1Spacing.sm),
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp)) }
        )

        // Category filter
        LazyRow(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = C1Spacing.lg),
            horizontalArrangement = Arrangement.spacedBy(C1Spacing.sm)
        ) {
            item {
                FilterChip(
                    selected = uiState.category == null,
                    onClick = { onCategoryChange(null) },
                    label = { Text(stringResource(R.string.community_filter_all)) }
                )
            }
            items(CommunityCategory.entries) { category ->
                FilterChip(
                    selected = uiState.category == category,
                    onClick = { onCategoryChange(category) },
                    label = { Text(categoryLabel(category)) }
                )
            }
        }

        // Section header with new post button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = C1Spacing.lg, vertical = C1Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (uiState.mode == CommunityMode.FEED) {
                    stringResource(R.string.community_tab_feed)
                } else {
                    stringResource(R.string.community_tab_discussions)
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            C1Button(
                text = stringResource(R.string.community_new_post),
                onClick = onCreatePost,
                intent = C1ButtonIntent.TONAL
            )
        }

        // Content
        when {
            uiState.isLoading -> C1LoadingState(label = stringResource(R.string.community_loading))
            uiState.visiblePosts.isEmpty() -> C1EmptyState(
                icon = Icons.Default.Forum,
                title = if (uiState.query.isBlank() && uiState.category == null) {
                    stringResource(R.string.community_empty_title)
                } else {
                    stringResource(R.string.community_no_match)
                },
                message = if (uiState.query.isBlank() && uiState.category == null) {
                    stringResource(R.string.community_empty_message)
                } else {
                    stringResource(R.string.community_no_match_hint)
                }
            )
            else -> PullToRefreshBox(
                isRefreshing = uiState.isRefreshingFeed,
                onRefresh = onRefresh,
                modifier = Modifier.fillMaxSize()
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        start = C1Spacing.lg,
                        end = C1Spacing.lg,
                        bottom = C1Spacing.xxxl
                    ),
                    verticalArrangement = Arrangement.spacedBy(C1Spacing.md)
                ) {
                    items(uiState.visiblePosts, key = { it.id }) { post ->
                        CommunityPostCard(
                            post = post,
                            isOwn = post.authorUid == uiState.profile.uid,
                            onClick = { onOpenPost(post.id) },
                            onToggleLike = { onToggleLike(post) },
                            onReport = { onReportPost(post) }
                        )
                    }
                    if (uiState.hasMore) {
                        item {
                            C1Button(
                                text = stringResource(R.string.community_more),
                                onClick = onLoadMore,
                                intent = C1ButtonIntent.OUTLINED,
                                enabled = !uiState.isRefreshing,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CommunityPostCard(
    post: CommunityPost,
    isOwn: Boolean,
    onClick: () -> Unit,
    onToggleLike: () -> Unit,
    onReport: () -> Unit
) {
    C1SurfaceCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(C1Spacing.lg),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier,
            verticalArrangement = Arrangement.spacedBy(C1Spacing.sm)
        ) {
            AuthorRow(post.authorName, post.authorRole, post.createdAtMillis, isOwn)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(C1Spacing.sm)
            ) {
                CategoryChip(post.category)
                Text(
                    text = post.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = post.body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(C1Spacing.lg)
            ) {
                TextButton(onClick = onToggleLike) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(C1Spacing.xs)
                    ) {
                        Icon(
                            imageVector = if (post.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = if (post.isLiked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(post.likeCount.toString())
                    }
                }
                TextButton(onClick = onClick) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(C1Spacing.xs)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChatBubbleOutline,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(post.commentCount.toString())
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                if (!isOwn) {
                    IconButton(onClick = onReport) {
                        Icon(
                            Icons.Default.Flag,
                            contentDescription = stringResource(R.string.community_report),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CommunityPostDetail(
    post: CommunityPost,
    comments: List<CommunityComment>,
    currentUid: String,
    onBack: () -> Unit,
    onToggleLike: (CommunityPost) -> Unit,
    onAddComment: (String, String) -> Unit,
    onDeleteComment: (CommunityComment) -> Unit,
    onEditPost: (CommunityPost) -> Unit,
    onDeletePost: (CommunityPost) -> Unit,
    onReportPost: (CommunityPost) -> Unit,
    onReportComment: (CommunityComment) -> Unit
) {
    var commentText by remember { mutableStateOf("") }
    Column(modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = C1Spacing.lg, vertical = C1Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.action_back))
            }
                    Text(
                        text = stringResource(R.string.community_tab_discussions),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = C1Spacing.lg,
                end = C1Spacing.lg,
                bottom = C1Spacing.md
            ),
            verticalArrangement = Arrangement.spacedBy(C1Spacing.md)
        ) {
            item {
                C1SurfaceCard(
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    Column(
                        modifier = Modifier.padding(C1Spacing.lg),
                        verticalArrangement = Arrangement.spacedBy(C1Spacing.sm)
                    ) {
                        AuthorRow(
                            post.authorName,
                            post.authorRole,
                            post.createdAtMillis,
                            post.authorUid == currentUid
                        )
                        CategoryChip(post.category)
                        Text(
                            text = post.title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(text = post.body, style = MaterialTheme.typography.bodyLarge)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(C1Spacing.sm)
                        ) {
                            C1Button(
                                text = post.likeCount.toString(),
                                onClick = { onToggleLike(post) },
                                intent = if (post.isLiked) C1ButtonIntent.TONAL else C1ButtonIntent.OUTLINED,
                                icon = { Icon(if (post.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            )
                            if (post.authorUid == currentUid) {
                                IconButton(onClick = { onEditPost(post) }) {
                                    Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.community_edit))
                                }
                                IconButton(onClick = { onDeletePost(post) }) {
                                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.community_delete))
                                }
                            } else {
                                IconButton(onClick = { onReportPost(post) }) {
                                    Icon(
                                        Icons.Default.Flag,
                                        contentDescription = stringResource(R.string.community_report)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            item {
                Text(
                    text = stringResource(R.string.community_comments),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            if (comments.isEmpty()) {
                item {
                    C1EmptyState(
                        icon = Icons.Default.ChatBubbleOutline,
                        title = stringResource(R.string.community_comments_empty),
                        message = stringResource(R.string.community_comments_empty_hint)
                    )
                }
            }
            items(comments, key = { it.id }) { comment ->
                CommentRow(
                    comment = comment,
                    isOwn = comment.authorUid == currentUid,
                    onDelete = { onDeleteComment(comment) },
                    onReport = { onReportComment(comment) }
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(C1Spacing.lg),
            verticalAlignment = Alignment.CenterVertically
        ) {
            C1TextField(
                value = commentText,
                onValueChange = { commentText = it },
                label = stringResource(R.string.community_comment_hint),
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = {
                    if (commentText.isNotBlank()) {
                        onAddComment(post.id, commentText)
                        commentText = ""
                    }
                }
            ) {
                Icon(Icons.Default.Send, contentDescription = stringResource(R.string.community_send))
            }
        }
    }
}

@Composable
private fun CommentRow(
    comment: CommunityComment,
    isOwn: Boolean,
    onDelete: () -> Unit,
    onReport: () -> Unit
) {
    C1SurfaceCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(C1Spacing.md),
            verticalArrangement = Arrangement.spacedBy(C1Spacing.xs)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AuthorRow(comment.authorName, comment.authorRole, comment.createdAtMillis, isOwn)
                Spacer(modifier = Modifier.weight(1f))
                if (isOwn) {
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = stringResource(R.string.community_delete),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else {
                    IconButton(onClick = onReport) {
                        Icon(
                            Icons.Default.Flag,
                            contentDescription = stringResource(R.string.community_report),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            Text(text = comment.body, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun AuthorRow(name: String, role: UserRole, createdAt: Long, isOwn: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(C1Spacing.sm)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = name.take(1).uppercase(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Column {
            Text(
                text = if (isOwn) stringResource(R.string.community_author_you) else name,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(C1Spacing.xs)) {
                if (role != UserRole.STUDENT) {
                    Text(
                        text = role.name.lowercase().replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text("•", style = MaterialTheme.typography.labelSmall)
                }
                Text(
                    text = formatTimestamp(createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CategoryChip(category: CommunityCategory) {
    val tone = when (category) {
        CommunityCategory.ANNOUNCEMENT -> C1Tone.INFO
        CommunityCategory.QUESTION -> C1Tone.WARNING
        CommunityCategory.ACADEMIC -> C1Tone.SUCCESS
        CommunityCategory.CAMPUS_LIFE -> C1Tone.NEUTRAL
        CommunityCategory.GENERAL -> C1Tone.NEUTRAL
    }
    C1StatusChip(text = categoryLabel(category), tone = tone)
}

@Composable
private fun categoryLabel(category: CommunityCategory): String = when (category) {
    CommunityCategory.GENERAL -> stringResource(R.string.community_filter_general)
    CommunityCategory.ACADEMIC -> stringResource(R.string.community_filter_academic)
    CommunityCategory.CAMPUS_LIFE -> stringResource(R.string.community_filter_campus_life)
    CommunityCategory.QUESTION -> stringResource(R.string.community_filter_question)
    CommunityCategory.ANNOUNCEMENT -> stringResource(R.string.community_filter_announcement)
}

@Composable
private fun PostComposerDialog(
    existing: CommunityPost?,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (String, String, CommunityCategory) -> Unit
) {
    var title by remember { mutableStateOf(existing?.title ?: "") }
    var body by remember { mutableStateOf(existing?.body ?: "") }
    var category by remember { mutableStateOf(existing?.category ?: CommunityCategory.GENERAL) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(
                    if (existing == null) R.string.community_composer_title else R.string.community_edit_title
                )
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(C1Spacing.md)) {
                C1TextField(
                    value = title,
                    onValueChange = { title = it },
                    label = stringResource(R.string.community_post_title)
                )
                C1TextField(
                    value = body,
                    onValueChange = { body = it },
                    label = stringResource(R.string.community_post_body),
                    singleLine = false
                )
                if (existing == null) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(C1Spacing.sm)) {
                        items(CommunityCategory.entries) { option ->
                            FilterChip(
                                selected = category == option,
                                onClick = { category = option },
                                label = { Text(categoryLabel(option)) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            C1Button(
                text = stringResource(if (existing == null) R.string.community_publish else R.string.community_save),
                onClick = { onSubmit(title, body, category) },
                enabled = title.isNotBlank() && body.isNotBlank() && !isSubmitting
            )
        },
        dismissButton = {
            C1Button(
                text = stringResource(R.string.community_cancel),
                onClick = onDismiss,
                intent = C1ButtonIntent.GHOST
            )
        }
    )
}

@Composable
private fun ReportDialog(onDismiss: () -> Unit, onSubmit: (String) -> Unit) {
    var reason by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.community_report_title)) },
        text = {
            C1TextField(
                value = reason,
                onValueChange = { reason = it },
                label = stringResource(R.string.community_report_reason),
                singleLine = false
            )
        },
        confirmButton = {
            C1Button(
                text = stringResource(R.string.community_report),
                onClick = { onSubmit(reason.ifBlank { "Reported from community" }) }
            )
        },
        dismissButton = {
            C1Button(
                text = stringResource(R.string.community_cancel),
                onClick = onDismiss,
                intent = C1ButtonIntent.GHOST
            )
        }
    )
}

@Composable
private fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            C1Button(
                text = confirmLabel,
                onClick = onConfirm,
                intent = C1ButtonIntent.DANGER
            )
        },
        dismissButton = {
            C1Button(
                text = stringResource(R.string.community_cancel),
                onClick = onDismiss,
                intent = C1ButtonIntent.GHOST
            )
        }
    )
}

@Composable
fun NotificationsScreen(
    viewModel: CommunityViewModel,
    onBack: () -> Unit,
    onNavigate: (targetType: String, targetId: String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(it) }
    }
    LaunchedEffect(Unit) { viewModel.refreshNotifications() }

    Box(modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = C1Spacing.lg, vertical = C1Spacing.md),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                    Column {
                    Text(
                        text = stringResource(R.string.notifications_title),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                        Text(
                            text = stringResource(R.string.notifications_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (uiState.notifications.any { !it.isRead }) {
                    TextButton(onClick = viewModel::markAllNotificationsRead) {
                        Text(stringResource(R.string.notifications_mark_all))
                    }
                }
            }
            if (uiState.notifications.isEmpty()) {
                C1EmptyState(
                    icon = Icons.Default.Notifications,
                    title = stringResource(R.string.notifications_empty_title),
                    message = stringResource(R.string.notifications_empty_message)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        start = C1Spacing.lg,
                        end = C1Spacing.lg,
                        bottom = C1Spacing.xxxl
                    ),
                    verticalArrangement = Arrangement.spacedBy(C1Spacing.sm)
                ) {
                    items(uiState.notifications, key = { it.id }) { notification ->
                        NotificationRow(
                            notification = notification,
                            onClick = {
                                viewModel.markNotificationRead(notification.id)
                                onNavigate(notification.targetType, notification.targetId)
                            }
                        )
                    }
                }
            }
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(C1Spacing.lg)
        )
    }
}

@Composable
private fun NotificationRow(notification: AppNotification, onClick: () -> Unit) {
    C1SurfaceCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        containerColor = if (notification.isRead) {
            MaterialTheme.colorScheme.surface
        } else {
            MaterialTheme.colorScheme.secondaryContainer
        }
    ) {
        Column(
            modifier = Modifier.padding(C1Spacing.md),
            verticalArrangement = Arrangement.spacedBy(C1Spacing.xs)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = notification.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                if (!notification.isRead) {
                    C1StatusChip(text = stringResource(R.string.notifications_unread), tone = C1Tone.INFO)
                }
            }
            if (notification.body.isNotBlank()) {
                Text(
                    text = notification.body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = formatTimestamp(notification.createdAtMillis),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun StudentProfileScreen(
    uiState: CoreUiState,
    avatarUrl: String? = null,
    onBack: () -> Unit,
    onSaveName: (String) -> Unit
) {
    val profile = uiState.profile
    var name by remember(profile.uid) { mutableStateOf(profile.name) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(C1Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(C1Spacing.md)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.action_back))
            }
            Column {
                Text(
                    text = stringResource(R.string.profile_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(R.string.profile_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        C1SurfaceCard(
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(C1Spacing.lg),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(C1Spacing.md)
            ) {
                C1Avatar(
                    name = profile.name,
                    size = 56.dp,
                    imageUrl = avatarUrl,
                    fontSize = 24
                )
                Column {
                    Text(
                        text = profile.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = profile.courseName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    C1StatusChip(
                        text = profile.role.name.lowercase().replaceFirstChar { it.uppercase() },
                        tone = if (profile.role == UserRole.STUDENT) C1Tone.NEUTRAL else C1Tone.SUCCESS
                    )
                }
            }
        }
        C1TextField(
            value = name,
            onValueChange = { name = it },
            label = stringResource(R.string.profile_name)
        )
        C1Button(
            text = stringResource(R.string.profile_save),
            onClick = { onSaveName(name) },
            enabled = name.isNotBlank() && name.trim() != profile.name,
            modifier = Modifier.fillMaxWidth()
        )
        ProfileDetailRow(stringResource(R.string.profile_college), profile.collegeName)
        ProfileDetailRow(stringResource(R.string.profile_course), profile.courseName)
        ProfileDetailRow(
            stringResource(R.string.profile_semester),
            profile.semester.toString()
        )
        ProfileDetailRow(stringResource(R.string.profile_section), profile.section)
        C1SurfaceCard(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ) {
            Text(
                text = stringResource(R.string.profile_private_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(C1Spacing.md)
            )
        }
    }
}

@Composable
private fun ProfileDetailRow(label: String, value: String) {
    C1SurfaceCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = C1Spacing.lg, vertical = C1Spacing.md),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

private fun formatTimestamp(millis: Long): String {
    if (millis <= 0L) return ""
    return SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(Date(millis))
}