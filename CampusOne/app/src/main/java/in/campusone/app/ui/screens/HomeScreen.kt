package `in`.campusone.app.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import `in`.campusone.app.R
import `in`.campusone.app.data.local.entity.AssignmentEntity
import `in`.campusone.app.data.local.entity.ExamEntity
import `in`.campusone.app.domain.model.NoticeItem
import `in`.campusone.app.domain.model.TimetableSlot
import `in`.campusone.app.domain.usecase.AttendanceInsight
import `in`.campusone.app.domain.usecase.SlotLiveState
import `in`.campusone.app.ui.components.AnimatedAttendanceRing
import `in`.campusone.app.ui.components.C1Banner
import `in`.campusone.app.ui.components.C1EmptyState
import `in`.campusone.app.ui.components.C1ErrorState
import `in`.campusone.app.ui.components.C1LoadingState
import `in`.campusone.app.ui.components.C1SectionHeader
import `in`.campusone.app.ui.components.C1StatusChip
import `in`.campusone.app.ui.components.C1Tone
import `in`.campusone.app.ui.theme.C1Motion
import `in`.campusone.app.ui.theme.C1Radius
import `in`.campusone.app.ui.theme.C1Spacing
import `in`.campusone.app.ui.theme.C1TouchTarget
import `in`.campusone.app.ui.theme.extendedColors
import `in`.campusone.app.ui.viewmodel.CoreUiState
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    uiState: CoreUiState,
    nextThreeDeadlines: List<AssignmentEntity> = emptyList(),
    nextUpcomingExam: ExamEntity? = null,
    dataRefreshError: Boolean = false,
    onToggleLanguage: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenAttendance: () -> Unit,
    onOpenTimetable: () -> Unit,
    onOpenNotices: () -> Unit,
    onOpenNotice: (String) -> Unit,
    onOpenMore: (MoreSubScreen) -> Unit,
    onRetryRefresh: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = C1Spacing.lg, vertical = C1Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(C1Spacing.xl)
    ) {
        item {
            HomeHeader(
                uiState = uiState,
                onToggleLanguage = onToggleLanguage,
                onOpenProfile = onOpenProfile,
                onOpenNotices = onOpenNotices
            )
        }
        item { NextClassCard(uiState = uiState, onOpenTimetable = onOpenTimetable) }
        item { AttendanceSummaryCard(insight = uiState.overallInsight, onClick = onOpenAttendance) }
        item { QuickActionsSection(onOpenMore = onOpenMore) }
        item {
            AnnouncementsSection(
                uiState = uiState,
                dataRefreshError = dataRefreshError,
                onOpenNotice = onOpenNotice,
                onOpenNotices = onOpenNotices,
                onRetryRefresh = onRetryRefresh
            )
        }
        if (nextThreeDeadlines.isNotEmpty() || nextUpcomingExam != null) {
            item {
                AtAGlanceSection(
                    deadlines = nextThreeDeadlines,
                    exam = nextUpcomingExam,
                    onOpenMore = onOpenMore
                )
            }
        }
    }
}

@Composable
private fun HomeHeader(
    uiState: CoreUiState,
    onToggleLanguage: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenNotices: () -> Unit
) {
    val firstName = uiState.profile.name.trim().substringBefore(" ").ifBlank { "Student" }
    val initial = firstName.first().uppercase()
    val courseLine = "${uiState.profile.courseName} • Sem ${uiState.profile.semester} (${uiState.profile.section})"
    val languageCd = stringResource(R.string.language_toggle)
    val profileCd = stringResource(R.string.home_open_profile)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            onClick = onOpenProfile,
            modifier = Modifier
                .size(C1TouchTarget)
                .semantics { contentDescription = profileCd },
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = initial,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        Spacer(modifier = Modifier.width(C1Spacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Namaste, $firstName",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = courseLine,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Surface(
            onClick = onToggleLanguage,
            modifier = Modifier
                .heightIn(min = C1TouchTarget)
                .semantics { contentDescription = languageCd },
            shape = C1Radius.pill,
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Box(
                modifier = Modifier.padding(horizontal = C1Spacing.md),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (uiState.languageTag == "hi") "EN" else "हिन्दी",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        IconButton(onClick = onOpenNotices) {
            BadgedBox(
                badge = {
                    if (uiState.unreadNoticeCount > 0) {
                        Badge {
                            Text(
                                if (uiState.unreadNoticeCount > 9) "9+" else uiState.unreadNoticeCount.toString()
                            )
                        }
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Default.NotificationsNone,
                    contentDescription = stringResource(R.string.nav_notices)
                )
            }
        }
    }
}

@Composable
private fun NextClassCard(
    uiState: CoreUiState,
    onOpenTimetable: () -> Unit
) {
    val next = uiState.nextClass
    val heroSlotId = next?.slot?.id
        ?: uiState.todaySchedule.firstOrNull { it.state == SlotLiveState.ONGOING }?.slot?.id
    val laterToday = remember(uiState.todaySchedule, heroSlotId) {
        uiState.todaySchedule
            .filter { it.state != SlotLiveState.COMPLETED && it.slot.id != heroSlotId }
            .take(3)
    }

    Surface(
        onClick = onOpenTimetable,
        modifier = Modifier.fillMaxWidth(),
        shape = C1Radius.lg,
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Column(
            modifier = Modifier
                .animateContentSize(
                    animationSpec = tween(
                        durationMillis = C1Motion.normalMillis,
                        easing = C1Motion.emphasizedEasing
                    )
                )
                .padding(C1Spacing.xl)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(C1Spacing.sm)
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = stringResource(
                            if (next?.isOngoing == true) R.string.ongoing_class_badge
                            else R.string.next_class_badge
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                if (next != null) {
                    Text(
                        text = when {
                            !next.isToday -> "${dayShort(next.slot)} ${next.slot.startTime}"
                            next.isOngoing -> stringResource(R.string.now_label)
                            else -> stringResource(R.string.starts_in_min, next.minutesUntilStart)
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(C1Spacing.md))
            if (next != null) {
                Text(
                    text = next.slot.subjectName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(C1Spacing.xs))
                Text(
                    text = "${next.slot.startTime} – ${next.slot.endTime} • ${next.slot.roomNumber}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = next.slot.teacherName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                Text(
                    text = stringResource(R.string.no_more_classes_today),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(C1Spacing.xs))
                Text(
                    text = stringResource(R.string.home_view_timetable_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }

            if (laterToday.isNotEmpty()) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = C1Spacing.md),
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.14f)
                )
                Text(
                    text = stringResource(R.string.home_later_today),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                )
                laterToday.forEach { evaluated ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = C1Spacing.sm),
                        horizontalArrangement = Arrangement.spacedBy(C1Spacing.md),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = evaluated.slot.startTime,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                        Text(
                            text = evaluated.slot.subjectName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = evaluated.slot.roomNumber,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AttendanceSummaryCard(
    insight: AttendanceInsight,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = C1Radius.lg,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(C1Spacing.xl),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.attendance_overall),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(C1Spacing.xs))
                SmartCalculatorBanner(insight = insight)
            }
            Spacer(modifier = Modifier.width(C1Spacing.lg))
            AnimatedAttendanceRing(
                percentage = insight.currentPercentage,
                health = insight.health,
                size = 72.dp,
                strokeWidth = 8.dp
            )
        }
    }
}

private data class HomeQuickAction(
    val icon: ImageVector,
    val labelRes: Int,
    val screen: MoreSubScreen
)

@Composable
private fun QuickActionsSection(onOpenMore: (MoreSubScreen) -> Unit) {
    val actions = remember {
        listOf(
            HomeQuickAction(Icons.Default.Assignment, R.string.qa_assignments, MoreSubScreen.ASSIGNMENTS),
            HomeQuickAction(Icons.Default.EventNote, R.string.qa_exams, MoreSubScreen.EXAMS_SYLLABUS),
            HomeQuickAction(Icons.Default.MenuBook, R.string.qa_notes, MoreSubScreen.NOTES_PYQ),
            HomeQuickAction(Icons.Default.Calculate, R.string.qa_gpa, MoreSubScreen.GPA_CALCULATOR),
            HomeQuickAction(Icons.Default.Search, R.string.qa_lost_found, MoreSubScreen.LOST_FOUND),
            HomeQuickAction(Icons.Default.ContactPhone, R.string.qa_directory, MoreSubScreen.DIRECTORY)
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(C1Spacing.md)) {
        C1SectionHeader(title = stringResource(R.string.home_quick_actions))
        actions.chunked(3).forEach { rowActions ->
            Row(
                modifier = Modifier.height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(C1Spacing.sm)
            ) {
                rowActions.forEach { action ->
                    QuickActionTile(
                        action = action,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        onOpenMore = onOpenMore
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickActionTile(
    action: HomeQuickAction,
    modifier: Modifier = Modifier,
    onOpenMore: (MoreSubScreen) -> Unit
) {
    Surface(
        onClick = { onOpenMore(action.screen) },
        modifier = modifier.heightIn(min = 92.dp),
        shape = C1Radius.md,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = C1Spacing.md, horizontal = C1Spacing.sm),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(C1Spacing.sm, Alignment.CenterVertically)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = action.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = stringResource(action.labelRes),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun AnnouncementsSection(
    uiState: CoreUiState,
    dataRefreshError: Boolean,
    onOpenNotice: (String) -> Unit,
    onOpenNotices: () -> Unit,
    onRetryRefresh: () -> Unit
) {
    val hairline = extendedColors.hairline
    val visibleNotices = remember(uiState.notices) {
        uiState.notices
            .sortedWith(
                compareByDescending<NoticeItem> { it.isPinned }
                    .thenByDescending { it.createdAtMillis }
            )
            .take(3)
    }

    Column(verticalArrangement = Arrangement.spacedBy(C1Spacing.md)) {
        C1SectionHeader(
            title = stringResource(R.string.home_announcements),
            actionLabel = stringResource(R.string.home_view_all),
            onAction = onOpenNotices
        )
        when {
            uiState.isLoading && visibleNotices.isEmpty() -> Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = C1Radius.md,
                color = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                C1LoadingState(label = stringResource(R.string.home_loading_announcements))
            }

            dataRefreshError && visibleNotices.isEmpty() -> Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = C1Radius.md,
                color = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                C1ErrorState(
                    title = stringResource(R.string.home_announcements_error),
                    message = stringResource(R.string.home_announcements_error_hint),
                    retryLabel = stringResource(R.string.home_retry),
                    onRetry = onRetryRefresh
                )
            }

            visibleNotices.isEmpty() -> Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = C1Radius.md,
                color = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                C1EmptyState(
                    icon = Icons.Default.Campaign,
                    title = stringResource(R.string.empty_notices),
                    message = stringResource(R.string.empty_notices_hint)
                )
            }

            else -> Column(verticalArrangement = Arrangement.spacedBy(C1Spacing.sm)) {
                if (dataRefreshError) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        C1Banner(
                            text = stringResource(R.string.home_showing_saved),
                            tone = C1Tone.WARNING,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = onRetryRefresh) {
                            Text(
                                text = stringResource(R.string.home_retry),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = C1Radius.md,
                    color = MaterialTheme.colorScheme.surfaceContainerLow
                ) {
                    Column {
                        visibleNotices.forEachIndexed { index, notice ->
                            NoticePreview(notice = notice, onOpenNotice = onOpenNotice)
                            if (index != visibleNotices.lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = C1Spacing.lg),
                                    color = hairline
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NoticePreview(
    notice: NoticeItem,
    onOpenNotice: (String) -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("d MMM", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenNotice(notice.id) }
            .padding(C1Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(C1Spacing.xs)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(C1Spacing.sm)
        ) {
            if (notice.isPinned) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(C1Spacing.xs)
                ) {
                    Icon(
                        imageVector = Icons.Default.PushPin,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = stringResource(R.string.home_pinned),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            if (!notice.isRead) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                )
            }
            Text(
                text = dateFormat.format(Date(notice.createdAtMillis)),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = notice.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = notice.body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "${notice.authorName} • ${notice.authorRole.name}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun AtAGlanceSection(
    deadlines: List<AssignmentEntity>,
    exam: ExamEntity?,
    onOpenMore: (MoreSubScreen) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(C1Spacing.md)) {
        C1SectionHeader(title = stringResource(R.string.home_at_a_glance))

        if (deadlines.isNotEmpty()) {
            Surface(
                onClick = { onOpenMore(MoreSubScreen.ASSIGNMENTS) },
                modifier = Modifier.fillMaxWidth(),
                shape = C1Radius.md,
                color = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                Column(
                    modifier = Modifier.padding(C1Spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(C1Spacing.sm)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(C1Spacing.sm)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Assignment,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = stringResource(R.string.home_upcoming_deadlines),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    deadlines.forEach { task ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = task.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${task.subjectTag} • ${task.dueDateFormatted}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.width(C1Spacing.sm))
                            C1StatusChip(
                                text = task.priority.lowercase().replaceFirstChar { it.uppercase() },
                                tone = priorityTone(task.priority)
                            )
                        }
                    }
                }
            }
        }

        if (exam != null) {
            val daysLeft = ((exam.examDateEpochMillis - System.currentTimeMillis()) / 86_400_000L)
                .coerceAtLeast(0L)
            val countdown = when {
                daysLeft == 0L -> stringResource(R.string.due_today)
                daysLeft == 1L -> stringResource(R.string.due_tomorrow)
                else -> stringResource(R.string.in_days, daysLeft.toInt())
            }
            Surface(
                onClick = { onOpenMore(MoreSubScreen.EXAMS_SYLLABUS) },
                modifier = Modifier.fillMaxWidth(),
                shape = C1Radius.md,
                color = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(C1Spacing.lg),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(C1Spacing.sm)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = stringResource(R.string.home_exam_countdown),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(C1Spacing.xs))
                        Text(
                            text = "${exam.subjectCode}: ${exam.subjectName}",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${exam.examDateFormatted} • ${exam.roomOrCenter}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.width(C1Spacing.md))
                    C1StatusChip(
                        text = countdown,
                        tone = if (daysLeft <= 3L) C1Tone.WARNING else C1Tone.INFO
                    )
                }
            }
        }
    }
}

private fun priorityTone(priority: String): C1Tone = when (priority.uppercase(Locale.getDefault())) {
    "HIGH" -> C1Tone.DANGER
    "MEDIUM" -> C1Tone.WARNING
    else -> C1Tone.NEUTRAL
}

private fun dayShort(slot: TimetableSlot): String =
    DayOfWeek.of(slot.dayOfWeek.coerceIn(1, 7))
        .getDisplayName(TextStyle.SHORT, Locale.getDefault())
