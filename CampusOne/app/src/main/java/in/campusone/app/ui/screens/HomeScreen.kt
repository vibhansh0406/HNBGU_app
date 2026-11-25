package `in`.campusone.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.material.icons.filled.ChevronRight
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import `in`.campusone.app.R
import `in`.campusone.app.data.local.entity.AssignmentEntity
import `in`.campusone.app.data.local.entity.CampusEventEntity
import `in`.campusone.app.data.local.entity.ExamEntity
import `in`.campusone.app.domain.model.NoticeItem
import `in`.campusone.app.domain.model.TimetableSlot
import `in`.campusone.app.domain.usecase.AttendanceHealth
import `in`.campusone.app.domain.usecase.AttendanceInsight
import `in`.campusone.app.domain.usecase.SlotLiveState
import `in`.campusone.app.ui.components.AnimatedAttendanceRing
import `in`.campusone.app.ui.components.C1Avatar
import `in`.campusone.app.ui.components.C1Banner
import `in`.campusone.app.ui.components.C1Button
import `in`.campusone.app.ui.components.C1ButtonIntent
import `in`.campusone.app.ui.components.C1CompactStatCard
import `in`.campusone.app.ui.components.C1EmptyState
import `in`.campusone.app.ui.components.C1ErrorState
import `in`.campusone.app.ui.components.C1FilterChip
import `in`.campusone.app.ui.components.C1SkeletonRows
import `in`.campusone.app.ui.components.C1SectionHeader
import `in`.campusone.app.ui.components.C1StatusChip
import `in`.campusone.app.ui.components.C1SurfaceCard
import `in`.campusone.app.ui.components.C1Tone
import `in`.campusone.app.ui.theme.C1Motion
import `in`.campusone.app.ui.theme.C1Radius
import `in`.campusone.app.ui.theme.C1Spacing
import `in`.campusone.app.ui.theme.C1TouchTarget
import `in`.campusone.app.ui.theme.C1WindowSize
import `in`.campusone.app.ui.theme.extendedColors
import `in`.campusone.app.ui.theme.windowSize
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
    onRetryRefresh: () -> Unit,
    campusEvents: List<CampusEventEntity> = emptyList(),
    onOpenNotifications: () -> Unit = onOpenNotices
) {
    val ws = windowSize
    val isCompact = ws == C1WindowSize.COMPACT

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
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
                horizontal = C1Spacing.pagePadding(ws),
                vertical = C1Spacing.lg
            ),
            verticalArrangement = Arrangement.spacedBy(C1Spacing.sectionGap(ws))
        ) {
        item {
            HomeHeader(
                uiState = uiState,
                onToggleLanguage = onToggleLanguage
            )
        }
        item {
            NextClassCard(
                uiState = uiState,
                onOpenTimetable = onOpenTimetable
            )
        }
        item {
            QuickActionsSection(onOpenMore = onOpenMore)
        }
        item {
            AttendanceSummaryCard(
                insight = uiState.overallInsight,
                onClick = onOpenAttendance
            )
        }
        item {
            TodaySchedulePreview(uiState = uiState, onOpenTimetable = onOpenTimetable)
        }
        item {
            AnnouncementsSection(
                uiState = uiState,
                dataRefreshError = dataRefreshError,
                onOpenNotice = onOpenNotice,
                onOpenNotices = onOpenNotices,
                onRetryRefresh = onRetryRefresh
            )
        }
        if (campusEvents.any { it.isApproved && it.eventEpochMillis >= System.currentTimeMillis() }) {
            item {
                CampusBuzzSection(
                    events = campusEvents,
                    onOpenEvents = { onOpenMore(MoreSubScreen.EVENTS_CLUBS) }
                )
            }
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
}

@Composable
private fun HomeHeader(
    uiState: CoreUiState,
    onToggleLanguage: () -> Unit
) {
    val firstName = uiState.profile.name.trim().substringBefore(" ").ifBlank { "Student" }
    val courseLine = "${uiState.profile.courseName} • Sem ${uiState.profile.semester} (${uiState.profile.section})"

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = stringResource(R.string.home_greeting, firstName),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
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
        C1FilterChip(
            label = if (uiState.languageTag == "hi") "EN" else "हिन्दी",
            selected = false,
            onClick = onToggleLanguage
        )
    }
}

@Composable
private fun NextClassCard(
    uiState: CoreUiState,
    onOpenTimetable: () -> Unit
) {
    val ws = windowSize
    val next = uiState.nextClass
    val heroSlotId = next?.slot?.id
        ?: uiState.todaySchedule.firstOrNull { it.state == SlotLiveState.ONGOING }?.slot?.id
    val laterToday = remember(uiState.todaySchedule, heroSlotId) {
        uiState.todaySchedule
            .filter { it.state != SlotLiveState.COMPLETED && it.slot.id != heroSlotId }
            .take(3)
    }

    C1SurfaceCard(
        onClick = onOpenTimetable,
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentPadding = PaddingValues(C1Spacing.xxl)
    ) {
        Column(
            modifier = Modifier
                .animateContentSize(
                    animationSpec = tween(
                        durationMillis = C1Motion.normalMillis,
                        easing = C1Motion.emphasizedEasing
                    )
                )
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
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(C1Spacing.xs))
                Text(
                    text = "${next.slot.startTime} – ${next.slot.endTime} • ${next.slot.roomNumber}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f),
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
                // Adaptive grid: 1 column on compact, 2+ on larger
                val cols = if (ws == C1WindowSize.COMPACT) 1 else 2
                if (cols == 1) {
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
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(C1Spacing.sm)
                    ) {
                        laterToday.forEach { evaluated ->
                            Column(
                                modifier = Modifier.weight(1f)
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
                                    overflow = TextOverflow.Ellipsis
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
    }
}

@Composable
private fun TodaySchedulePreview(
    uiState: CoreUiState,
    onOpenTimetable: () -> Unit
) {
    val entries = uiState.todaySchedule.take(3)
    Column(verticalArrangement = Arrangement.spacedBy(C1Spacing.md)) {
        FlagshipSectionTitle(
            title = "Today's schedule",
            subtitle = if (uiState.todaySchedule.isEmpty()) "Your day at a glance" else "${uiState.todaySchedule.size} scheduled sessions",
            action = "Full timetable",
            onAction = onOpenTimetable
        )
        C1SurfaceCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(C1Spacing.md),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            if (entries.isEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = C1Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(C1Spacing.md)
                ) {
                    Box(
                        modifier = Modifier.size(38.dp).clip(C1Radius.md).background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Default.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.secondary) }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("No classes scheduled today", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                        Text("Your published weekly timetable is still available.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(C1Spacing.sm)) {
                    entries.forEachIndexed { index, evaluated ->
                        val slot = evaluated.slot
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(C1Spacing.md)
                        ) {
                            Column(modifier = Modifier.width(76.dp)) {
                                Text(slot.startTime, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Text(slot.endTime, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Box(Modifier.width(3.dp).height(42.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.32f)))
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(slot.subjectName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${slot.subjectCode} · ${slot.roomNumber}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            when (evaluated.state) {
                                SlotLiveState.ONGOING -> FlagshipPill("LIVE")
                                SlotLiveState.UP_NEXT -> FlagshipPill("UP NEXT", background = MaterialTheme.colorScheme.tertiaryContainer, foreground = MaterialTheme.colorScheme.onTertiaryContainer)
                                SlotLiveState.COMPLETED -> FlagshipPill("DONE", background = MaterialTheme.colorScheme.surfaceVariant, foreground = MaterialTheme.colorScheme.onSurfaceVariant)
                                SlotLiveState.UPCOMING_LATER -> FlagshipPill("LATER", background = MaterialTheme.colorScheme.surfaceVariant, foreground = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        if (index != entries.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun CampusBuzzSection(
    events: List<CampusEventEntity>,
    onOpenEvents: () -> Unit
) {
    val upcoming = remember(events) {
        events.filter { it.isApproved && it.eventEpochMillis >= System.currentTimeMillis() }
            .sortedBy { it.eventEpochMillis }
            .take(2)
    }
    if (upcoming.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(C1Spacing.md)) {
        FlagshipSectionTitle(
            title = "Campus buzz",
            subtitle = "Verified events from your campus",
            action = "Explore all",
            onAction = onOpenEvents
        )
        upcoming.forEach { event ->
            C1SurfaceCard(
                modifier = Modifier.fillMaxWidth(),
                containerColor = MaterialTheme.colorScheme.surface,
                contentPadding = PaddingValues(C1Spacing.lg),
                onClick = onOpenEvents
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(C1Spacing.md)) {
                    Box(
                        modifier = Modifier.size(44.dp).clip(C1Radius.md).background(MaterialTheme.colorScheme.tertiaryContainer),
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer) }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(event.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text("${event.clubName} · ${event.eventDateFormatted}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(event.venue, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    FlagshipPill(event.category)
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
    C1SurfaceCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(C1Spacing.xl),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.attendance_overall),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(C1Spacing.xs))
                SmartCalculatorBanner(insight = insight)
            }
            Spacer(modifier = Modifier.width(C1Spacing.lg))
            AnimatedAttendanceRing(
                percentage = insight.currentPercentage,
                health = insight.health,
                size = 90.dp,
                strokeWidth = 10.dp,
                label = "Overall"
            )
        }
    }
}

private data class HomeQuickAction(
    val icon: ImageVector,
    val labelRes: Int,
    val descriptionRes: Int,
    val screen: MoreSubScreen
)

@Composable
private fun QuickActionsSection(onOpenMore: (MoreSubScreen) -> Unit) {
    val ws = windowSize
    // Keep the phone dashboard compact; widen the grid only when space is available.
    val cols = when (ws) {
        C1WindowSize.COMPACT -> 2
        C1WindowSize.MEDIUM -> 3
        else -> 4
    }

    val actions = listOf(
        HomeQuickAction(Icons.Default.Assignment, R.string.qa_assignments, R.string.qa_assignments_desc, MoreSubScreen.ASSIGNMENTS),
        HomeQuickAction(Icons.Default.EventNote, R.string.qa_exams, R.string.qa_exams_desc, MoreSubScreen.EXAMS_SYLLABUS),
        HomeQuickAction(Icons.Default.MenuBook, R.string.qa_notes, R.string.qa_notes_desc, MoreSubScreen.NOTES_PYQ),
        HomeQuickAction(Icons.Default.Calculate, R.string.qa_gpa, R.string.qa_gpa_desc, MoreSubScreen.GPA_CALCULATOR),
        HomeQuickAction(Icons.Default.Search, R.string.qa_lost_found, R.string.qa_lost_found_desc, MoreSubScreen.LOST_FOUND),
        HomeQuickAction(Icons.Default.ContactPhone, R.string.qa_directory, R.string.qa_directory_desc, MoreSubScreen.DIRECTORY)
    )

    Column(verticalArrangement = Arrangement.spacedBy(C1Spacing.md)) {
        C1SectionHeader(
            title = stringResource(R.string.home_quick_actions),
            subtitle = stringResource(R.string.home_quick_actions_subtitle)
        )
        actions.chunked(cols).forEach { rowActions ->
            Row(horizontalArrangement = Arrangement.spacedBy(C1Spacing.sm)) {
                rowActions.forEach { action ->
                    QuickActionTile(
                        action = action,
                        modifier = Modifier.weight(1f),
                        onOpenMore = onOpenMore
                    )
                }
                repeat((cols - rowActions.size).coerceAtLeast(0)) {
                    Spacer(modifier = Modifier.weight(1f))
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
    val iconColor = MaterialTheme.colorScheme.primary
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }

    AnimatedVisibility(
        visible = entered,
        enter = fadeIn(animationSpec = tween(C1Motion.normalMillis)) +
            slideInVertically(animationSpec = tween(C1Motion.normalMillis)) { it / 10 },
        modifier = modifier
    ) {
        C1SurfaceCard(
            modifier = Modifier.heightIn(min = 92.dp),
            onClick = { onOpenMore(action.screen) },
            contentPadding = PaddingValues(horizontal = C1Spacing.md, vertical = C1Spacing.md),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(C1Spacing.sm),
                horizontalAlignment = Alignment.Start
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(C1Radius.tile)
                        .background(iconColor.copy(alpha = 0.10f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = action.icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = stringResource(action.labelRes),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    minLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
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
    val ws = windowSize
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
            uiState.isLoading && visibleNotices.isEmpty() -> C1SurfaceCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(0.dp),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                C1SkeletonRows(
                    modifier = Modifier.padding(C1Spacing.lg),
                    rows = 3
                )
            }

            dataRefreshError && visibleNotices.isEmpty() -> C1SurfaceCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(0.dp),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                C1ErrorState(
                    title = stringResource(R.string.home_announcements_error),
                    message = stringResource(R.string.home_announcements_error_hint),
                    retryLabel = stringResource(R.string.home_retry),
                    onRetry = onRetryRefresh
                )
            }

            visibleNotices.isEmpty() -> C1SurfaceCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(0.dp),
                containerColor = MaterialTheme.colorScheme.surface
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
                C1SurfaceCard(
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = MaterialTheme.colorScheme.surface
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
            .padding(C1Spacing.lg)
            .semantics(mergeDescendants = true) {},
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
                        style = MaterialTheme.typography.labelSmall,
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
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
        Text(
            text = notice.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = notice.body,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "${notice.authorName} • ${notice.authorRole.name}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
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
            C1SurfaceCard(
                onClick = { onOpenMore(MoreSubScreen.ASSIGNMENTS) },
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(0.dp),
                containerColor = MaterialTheme.colorScheme.surface
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
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
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
            C1SurfaceCard(
                onClick = { onOpenMore(MoreSubScreen.EXAMS_SYLLABUS) },
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(0.dp),
                containerColor = MaterialTheme.colorScheme.surface
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
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(C1Spacing.xs))
                        Text(
                            text = "${exam.subjectCode}: ${exam.subjectName}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
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

@Composable
fun SmartCalculatorBanner(insight: AttendanceInsight) {
    val extended = extendedColors
    val (text, color) = when (insight.health) {
        AttendanceHealth.NO_DATA ->
            stringResource(R.string.no_classes_recorded) to MaterialTheme.colorScheme.onSurfaceVariant
        AttendanceHealth.SAFE ->
            stringResource(
                R.string.bunk_safe,
                insight.classesCanMissSafely,
                insight.targetPercentage
            ) to extended.success
        AttendanceHealth.ON_EDGE ->
            stringResource(
                R.string.bunk_on_edge,
                insight.targetPercentage
            ) to extended.warning
        AttendanceHealth.WARNING, AttendanceHealth.CRITICAL ->
            stringResource(
                R.string.bunk_recover,
                insight.classesMustAttendToRecover,
                insight.targetPercentage
            ) to MaterialTheme.colorScheme.error
    }
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.SemiBold,
        color = color
    )
}
