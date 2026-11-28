package `in`.campusone.app.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Attachment
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import `in`.campusone.app.R
import `in`.campusone.app.domain.model.AttachmentType
import `in`.campusone.app.domain.model.NoticeItem
import `in`.campusone.app.domain.model.UserRole
import `in`.campusone.app.ui.components.C1Banner
import `in`.campusone.app.ui.components.C1Button
import `in`.campusone.app.ui.components.C1ButtonIntent
import `in`.campusone.app.ui.components.C1EmptyState
import `in`.campusone.app.ui.components.C1ErrorState
import `in`.campusone.app.ui.components.C1SectionHeader
import `in`.campusone.app.ui.components.C1SurfaceCard
import `in`.campusone.app.ui.components.C1LoadingState
import `in`.campusone.app.ui.components.C1StatusChip
import `in`.campusone.app.ui.components.C1SegmentedButton
import `in`.campusone.app.ui.components.C1SegmentedButtonColors
import `in`.campusone.app.ui.components.C1TextField
import `in`.campusone.app.ui.components.C1Tone
import `in`.campusone.app.ui.theme.C1Radius
import `in`.campusone.app.ui.theme.C1Spacing
import `in`.campusone.app.ui.theme.extendedColors
import `in`.campusone.app.ui.viewmodel.CoreUiState
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class NoticeFilter { ALL, PINNED, UNREAD, OFFICIAL }

private val noticeCategories = listOf("GENERAL", "ACADEMIC", "EXAM", "EVENT", "URGENT", "PLACEMENT")
private val noticePriorities = listOf("LOW", "NORMAL", "HIGH", "URGENT")

@Composable
fun NoticeBoardScreen(
    uiState: CoreUiState,
    onMarkNoticeRead: (String) -> Unit,
    onMarkAllRead: () -> Unit,
    onPostNotice: (
        title: String,
        body: String,
        attachmentUrl: String?,
        attachmentType: AttachmentType,
        isPinned: Boolean,
        category: String,
        priority: String,
        isOfficial: Boolean
    ) -> Unit,
    onUploadNoticeAttachment: suspend (contentUri: String, mimeType: String) -> Result<String>,
    dataRefreshError: Boolean = false,
    onRetryRefresh: () -> Unit = {},
    focusNoticeId: String? = null,
    onFocusConsumed: () -> Unit = {},
    onOpenNotifications: () -> Unit = {},
    onOpenProfile: () -> Unit = {}
) {
    var query by rememberSaveable { mutableStateOf("") }
    var filterIndex by rememberSaveable { mutableIntStateOf(0) }
    var showComposer by rememberSaveable { mutableStateOf(false) }
    var selectedNoticeId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedCategory by rememberSaveable { mutableStateOf<String?>(null) }
    val filter = NoticeFilter.entries[filterIndex]

    val selectedNotice = uiState.notices.firstOrNull { it.id == selectedNoticeId }

    LaunchedEffect(selectedNoticeId, uiState.notices) {
        if (selectedNoticeId != null && selectedNotice == null) {
            selectedNoticeId = null
        }
    }

    LaunchedEffect(focusNoticeId, uiState.notices) {
        if (focusNoticeId != null && uiState.notices.any { it.id == focusNoticeId }) {
            selectedNoticeId = focusNoticeId
            onFocusConsumed()
        }
    }

    if (selectedNotice != null) {
        NoticeDetail(
            notice = selectedNotice,
            onBack = { selectedNoticeId = null },
            onMarkRead = onMarkNoticeRead
        )
        return
    }

    val canPost = uiState.profile.role == UserRole.CR ||
        uiState.profile.role == UserRole.ADMIN ||
        uiState.profile.role == UserRole.MODERATOR

    val searched = uiState.notices.filter { notice ->
        query.isBlank() ||
            notice.title.contains(query, ignoreCase = true) ||
            notice.body.contains(query, ignoreCase = true) ||
            notice.authorName.contains(query, ignoreCase = true)
    }.filter { selectedCategory == null || it.category == selectedCategory }
    val displayedNotices = when (filter) {
        NoticeFilter.ALL -> searched
        NoticeFilter.PINNED -> searched.filter { it.isPinned }
        NoticeFilter.UNREAD -> searched.filter { !it.isRead }
        NoticeFilter.OFFICIAL -> searched.filter { it.isOfficial }
    }

    Scaffold(
        floatingActionButton = {
            if (canPost) {
                FloatingActionButton(
                    onClick = { showComposer = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.post_notice))
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            FlagshipAppHeader(
                campusName = uiState.profile.collegeName,
                userName = uiState.profile.name,
                onNotifications = onOpenNotifications,
                onProfile = onOpenProfile
            )
            LazyColumn(
                modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(
                start = C1Spacing.lg,
                end = C1Spacing.lg,
                top = C1Spacing.lg,
                bottom = 96.dp
            ),
            verticalArrangement = Arrangement.spacedBy(C1Spacing.md)
        ) {
            // Header item: title, search, filters
            item {
                Column(verticalArrangement = Arrangement.spacedBy(C1Spacing.md)) {
                    FlagshipPageTitle(
                        title = stringResource(R.string.notices_title),
                        subtitle = "${uiState.profile.courseName} · Sem ${uiState.profile.semester} · Section ${uiState.profile.section}",
                        trailing = if (uiState.unreadNoticeCount > 0) {
                            {
                                TextButton(onClick = onMarkAllRead) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(C1Spacing.xs)) {
                                        Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(17.dp))
                                        Text(stringResource(R.string.mark_all_read))
                                    }
                                }
                            }
                        } else null
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(C1Spacing.sm)) {
                        FlagshipPill(
                            text = "${uiState.unreadNoticeCount} unread",
                            background = if (uiState.unreadNoticeCount > 0) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            foreground = if (uiState.unreadNoticeCount > 0) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FlagshipPill(
                            text = "${uiState.notices.size} updates",
                            background = MaterialTheme.colorScheme.surfaceVariant,
                            foreground = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    C1TextField(
                        value = query,
                        onValueChange = { query = it },
                        label = stringResource(R.string.notices_search_hint),
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp)) }
                    )

                    // Compact dropdown filter (saves vertical space)
                    var filterExpanded by remember { mutableStateOf(false) }
                    var categoryExpanded by remember { mutableStateOf(false) }
                    Row(horizontalArrangement = Arrangement.spacedBy(C1Spacing.sm)) {
                        Box {
                            C1Button(
                                text = stringResource(R.string.notices_filter_all) + " ▾",
                                onClick = { filterExpanded = true },
                                intent = C1ButtonIntent.TONAL
                            )
                            DropdownMenu(expanded = filterExpanded, onDismissRequest = { filterExpanded = false }) {
                                listOf(
                                    stringResource(R.string.notices_filter_all),
                                    stringResource(R.string.home_pinned),
                                    if (uiState.unreadNoticeCount > 0) "${stringResource(R.string.notices_filter_unread)} (${uiState.unreadNoticeCount})" else stringResource(R.string.notices_filter_unread),
                                    stringResource(R.string.notice_official)
                                ).forEachIndexed { i, label ->
                                    DropdownMenuItem(text = { Text(label) }, onClick = { filterIndex = i; filterExpanded = false })
                                }
                            }
                        }
                        Box {
                            C1Button(
                                text = (selectedCategory ?: stringResource(R.string.notices_filter_all)) + " ▾",
                                onClick = { categoryExpanded = true },
                                intent = C1ButtonIntent.TONAL
                            )
                            DropdownMenu(expanded = categoryExpanded, onDismissRequest = { categoryExpanded = false }) {
                                DropdownMenuItem(text = { Text(stringResource(R.string.notices_filter_all)) }, onClick = { selectedCategory = null; categoryExpanded = false })
                                noticeCategories.forEach { category ->
                                    DropdownMenuItem(text = { Text(noticeCategoryLabel(category)) }, onClick = { selectedCategory = category; categoryExpanded = false })
                                }
                            }
                        }
                    }


                }
            }

            // State-dependent content — error and empty states are mutually exclusive
            if (uiState.isLoading && uiState.notices.isEmpty()) {
                // State 1: Loading, no cached data yet
                item {
                    C1LoadingState(label = stringResource(R.string.home_loading_announcements))
                }
            } else if (dataRefreshError && uiState.notices.isEmpty()) {
                // State 2: Request failed, no cached data — error state with retry
                item {
                    C1ErrorState(
                        title = stringResource(R.string.home_announcements_error),
                        message = stringResource(R.string.home_announcements_error_hint),
                        retryLabel = stringResource(R.string.home_retry),
                        onRetry = onRetryRefresh
                    )
                }
            } else if (dataRefreshError && uiState.notices.isNotEmpty()) {
                // State 3: Request failed but cached data exists — warning banner + cached notices
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(C1Spacing.xs)) {
                        C1Banner(
                            text = stringResource(R.string.notices_sync_failed),
                            tone = C1Tone.WARNING,
                            icon = Icons.Default.Info,
                            actionLabel = stringResource(R.string.home_retry),
                            onAction = onRetryRefresh
                        )
                    }
                }
                items(displayedNotices, key = { it.id }) { notice ->
                    NoticeListCard(
                        notice = notice,
                        onClick = {
                            if (!notice.isRead) onMarkNoticeRead(notice.id)
                            selectedNoticeId = notice.id
                        }
                    )
                }
            } else if (uiState.notices.isEmpty()) {
                // State 4: Successfully loaded, genuinely empty
                item {
                    NoticeEmptyState(
                        icon = Icons.Default.Campaign,
                        title = stringResource(R.string.empty_notices),
                        message = stringResource(R.string.empty_notices_hint)
                    )
                }
            } else if (displayedNotices.isEmpty()) {
                // State 5: Filter applied, no matches
                item {
                    NoticeEmptyState(
                        icon = Icons.Default.Search,
                        title = stringResource(R.string.notices_none_match),
                        message = stringResource(R.string.notices_none_match_hint)
                    )
                }
            } else {
                // State 6: Successfully loaded with announcements
                items(displayedNotices, key = { it.id }) { notice ->
                    NoticeListCard(
                        notice = notice,
                        onClick = {
                            if (!notice.isRead) onMarkNoticeRead(notice.id)
                            selectedNoticeId = notice.id
                        }
                    )
                }
            }
        }
        }
    }

    if (showComposer) {
        PostNoticeDialog(
            isAdmin = uiState.profile.role == UserRole.ADMIN,
            onDismiss = { showComposer = false },
            onUploadAttachment = onUploadNoticeAttachment,
            onSubmit = { title, body, url, type, pinned, category, priority, official ->
                onPostNotice(title, body, url, type, pinned, category, priority, official)
                showComposer = false
            }
        )
    }
}

@Composable
private fun NoticeEmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    message: String
) {
    C1SurfaceCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        C1EmptyState(icon = icon, title = title, message = message)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NoticeListCard(notice: NoticeItem, onClick: () -> Unit) {
    C1SurfaceCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surface,
        borderColor = if (!notice.isRead) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        } else {
            MaterialTheme.colorScheme.outlineVariant
        }
    ) {
        Column(modifier = Modifier.padding(C1Spacing.lg)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (notice.isPinned) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = stringResource(R.string.home_pinned),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(C1Spacing.xs))
                    }
                    Text(
                        text = "${notice.authorName} · ${notice.authorRole.name}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (!notice.isRead) {
                    Spacer(modifier = Modifier.width(C1Spacing.sm))
                    C1StatusChip(
                        text = stringResource(R.string.new_badge),
                        tone = C1Tone.INFO
                    )
                }
            }

            Spacer(modifier = Modifier.height(C1Spacing.xs))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(C1Spacing.xs)) {
                if (notice.isOfficial) {
                    C1StatusChip(
                        text = stringResource(R.string.notice_official),
                        tone = C1Tone.SUCCESS
                    )
                }
                C1StatusChip(text = noticeCategoryLabel(notice.category), tone = C1Tone.NEUTRAL)
                if (notice.priority == "HIGH" || notice.priority == "URGENT") {
                    C1StatusChip(
                        text = noticePriorityLabel(notice.priority),
                        tone = if (notice.priority == "URGENT") C1Tone.DANGER else C1Tone.WARNING
                    )
                }
            }
            Spacer(modifier = Modifier.height(C1Spacing.xs))
            Text(
                text = notice.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(C1Spacing.xs))
            Text(
                text = notice.body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(C1Spacing.sm))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatNoticeDate(notice.createdAtMillis),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!notice.attachmentUrl.isNullOrBlank()) {
                    Icon(
                        imageVector = Icons.Default.Attachment,
                        contentDescription = stringResource(R.string.notice_attachment),
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NoticeDetail(
    notice: NoticeItem,
    onBack: () -> Unit,
    onMarkRead: (String) -> Unit
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val unavailableMessage = stringResource(R.string.attachment_unavailable)

    BackHandler(onBack = onBack)

    LaunchedEffect(notice.id) {
        if (!notice.isRead) onMarkRead(notice.id)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding),
            contentPadding = PaddingValues(
                start = C1Spacing.lg,
                end = C1Spacing.lg,
                top = C1Spacing.sm,
                bottom = C1Spacing.xxxl
            ),
            verticalArrangement = Arrangement.spacedBy(C1Spacing.lg)
        ) {
            item {
                TextButton(onClick = onBack) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(C1Spacing.xs)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(stringResource(R.string.action_back))
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(C1Spacing.sm)) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(C1Spacing.sm)) {
                        if (notice.isPinned) {
                            C1StatusChip(
                                text = stringResource(R.string.home_pinned),
                                tone = C1Tone.INFO
                            )
                        }
                        if (!notice.isRead) {
                            C1StatusChip(
                                text = stringResource(R.string.new_badge),
                                tone = C1Tone.INFO
                            )
                        }
                        if (notice.isOfficial) {
                            C1StatusChip(
                                text = stringResource(R.string.notice_official),
                                tone = C1Tone.SUCCESS
                            )
                        }
                        C1StatusChip(
                            text = noticeCategoryLabel(notice.category),
                            tone = C1Tone.NEUTRAL
                        )
                        if (notice.priority == "HIGH" || notice.priority == "URGENT") {
                            C1StatusChip(
                                text = noticePriorityLabel(notice.priority),
                                tone = if (notice.priority == "URGENT") C1Tone.DANGER else C1Tone.WARNING
                            )
                        }
                    }
                    Text(
                        text = notice.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${notice.authorName} · ${notice.authorRole.name}",
                        style = MaterialTheme.typography.labelLarge,
                        color = extendedColors.brandForeground
                    )
                    Text(
                        text = formatNoticeDate(notice.createdAtMillis),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                Text(
                    text = notice.body,
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            if (!notice.attachmentUrl.isNullOrBlank()) {
                item {
                    C1SurfaceCard(
                        onClick = {
                            if (!openUrl(context, notice.attachmentUrl)) {
                                scope.launch { snackbarHostState.showSnackbar(unavailableMessage) }
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        Row(
                            modifier = Modifier.padding(C1Spacing.lg),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Attachment,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.width(C1Spacing.sm))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.notice_attachment),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = notice.attachmentType.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = stringResource(R.string.open_attachment),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PostNoticeDialog(
    isAdmin: Boolean,
    onDismiss: () -> Unit,
    onUploadAttachment: suspend (contentUri: String, mimeType: String) -> Result<String>,
    onSubmit: (
        title: String,
        body: String,
        attachmentUrl: String?,
        attachmentType: AttachmentType,
        isPinned: Boolean,
        category: String,
        priority: String,
        isOfficial: Boolean
    ) -> Unit
) {
    var title by rememberSaveable { mutableStateOf("") }
    var body by rememberSaveable { mutableStateOf("") }
    var attachmentUrl by rememberSaveable { mutableStateOf("") }
    var isPinned by rememberSaveable { mutableStateOf(false) }
    var category by rememberSaveable { mutableStateOf("GENERAL") }
    var priority by rememberSaveable { mutableStateOf("NORMAL") }
    var isOfficial by rememberSaveable { mutableStateOf(false) }
    var uploadedAttachmentType by rememberSaveable { mutableStateOf(AttachmentType.NONE.name) }
    var isUploadingAttachment by rememberSaveable { mutableStateOf(false) }
    var attachmentUploadError by rememberSaveable { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val attachmentPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { selectedUri ->
        if (selectedUri != null) {
            val mimeType = context.contentResolver.getType(selectedUri).orEmpty()
            scope.launch {
                isUploadingAttachment = true
                attachmentUploadError = null
                val result = onUploadAttachment(selectedUri.toString(), mimeType)
                if (result.isSuccess) {
                    attachmentUrl = result.getOrNull().orEmpty()
                    uploadedAttachmentType = if (
                        mimeType.equals("application/pdf", ignoreCase = true) ||
                        selectedUri.lastPathSegment?.endsWith(".pdf", ignoreCase = true) == true
                    ) AttachmentType.PDF.name else AttachmentType.IMAGE.name
                } else {
                    attachmentUploadError = result.exceptionOrNull()?.message
                        ?: "Attachment upload failed"
                }
                isUploadingAttachment = false
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.post_notice)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(C1Spacing.sm)
            ) {
                C1TextField(
                    value = title,
                    onValueChange = { title = it },
                    label = stringResource(R.string.notice_composer_title)
                )
                C1TextField(
                    value = body,
                    onValueChange = { body = it },
                    label = stringResource(R.string.notice_composer_body),
                    singleLine = false
                )
                Text(
                    text = stringResource(R.string.notice_composer_category),
                    style = MaterialTheme.typography.labelLarge
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(C1Spacing.xs)) {
                    items(noticeCategories) { option ->
                        FilterChip(
                            selected = category == option,
                            onClick = { category = option },
                            label = { Text(noticeCategoryLabel(option)) }
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.notice_composer_priority),
                    style = MaterialTheme.typography.labelLarge
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(C1Spacing.xs)) {
                    items(noticePriorities) { option ->
                        FilterChip(
                            selected = priority == option,
                            onClick = { priority = option },
                            label = { Text(noticePriorityLabel(option)) }
                        )
                    }
                }
                C1TextField(
                    value = attachmentUrl,
                    onValueChange = {
                        attachmentUrl = it
                        uploadedAttachmentType = AttachmentType.NONE.name
                    },
                    label = stringResource(R.string.notice_composer_attachment)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(C1Spacing.sm)
                ) {
                    C1Button(
                        text = if (isUploadingAttachment) "Uploading..." else "Upload PDF",
                        onClick = { attachmentPicker.launch(arrayOf("application/pdf")) },
                        modifier = Modifier.weight(1f),
                        intent = C1ButtonIntent.OUTLINED,
                        enabled = !isUploadingAttachment,
                        loading = isUploadingAttachment,
                        icon = { Icon(Icons.Default.Attachment, contentDescription = null) }
                    )
                    C1Button(
                        text = "Upload Image",
                        onClick = { attachmentPicker.launch(arrayOf("image/*")) },
                        modifier = Modifier.weight(1f),
                        intent = C1ButtonIntent.OUTLINED,
                        enabled = !isUploadingAttachment
                    )
                }
                if (attachmentUrl.isNotBlank() && uploadedAttachmentType != AttachmentType.NONE.name) {
                    Text(
                        text = "Attachment uploaded (${uploadedAttachmentType})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                if (!attachmentUploadError.isNullOrBlank()) {
                    Text(
                        text = attachmentUploadError.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isPinned, onCheckedChange = { isPinned = it })
                    Spacer(modifier = Modifier.width(C1Spacing.xs))
                    Text(stringResource(R.string.notice_composer_pin))
                }
                if (isAdmin) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isOfficial, onCheckedChange = { isOfficial = it })
                        Spacer(modifier = Modifier.width(C1Spacing.xs))
                        Text(stringResource(R.string.notice_composer_official))
                    }
                }
            }
        },
        confirmButton = {
            C1Button(
                text = stringResource(R.string.notice_publish),
                onClick = {
                    val attType = when {
                        attachmentUrl.isBlank() -> AttachmentType.NONE
                        uploadedAttachmentType != AttachmentType.NONE.name ->
                            AttachmentType.valueOf(uploadedAttachmentType)
                        attachmentUrl.endsWith(".pdf", ignoreCase = true) -> AttachmentType.PDF
                        attachmentUrl.endsWith(".png", true) || attachmentUrl.endsWith(".jpg", true) -> AttachmentType.IMAGE
                        else -> AttachmentType.LINK
                    }
                    onSubmit(
                        title,
                        body,
                        attachmentUrl.ifBlank { null },
                        attType,
                        isPinned,
                        category,
                        priority,
                        isOfficial
                    )
                },
                enabled = title.isNotBlank() && body.isNotBlank() && !isUploadingAttachment
            )
        },
        dismissButton = {
            C1Button(
                text = stringResource(R.string.action_cancel),
                onClick = onDismiss,
                intent = C1ButtonIntent.GHOST
            )
        }
    )
}

private fun openUrl(context: Context, url: String): Boolean = runCatching {
    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    true
}.getOrDefault(false)

private fun formatNoticeDate(millis: Long): String =
    SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(millis))

@Composable
private fun noticeCategoryLabel(category: String): String = when (category) {
    "ACADEMIC" -> stringResource(R.string.notice_category_academic)
    "EXAM" -> stringResource(R.string.notice_category_exam)
    "EVENT" -> stringResource(R.string.notice_category_event)
    "URGENT" -> stringResource(R.string.notice_category_urgent)
    "PLACEMENT" -> stringResource(R.string.notice_category_placement)
    else -> stringResource(R.string.notice_category_general)
}

@Composable
private fun noticePriorityLabel(priority: String): String = when (priority) {
    "LOW" -> stringResource(R.string.notice_priority_low)
    "HIGH" -> stringResource(R.string.notice_priority_high)
    "URGENT" -> stringResource(R.string.notice_priority_urgent)
    else -> stringResource(R.string.notice_priority_normal)
}