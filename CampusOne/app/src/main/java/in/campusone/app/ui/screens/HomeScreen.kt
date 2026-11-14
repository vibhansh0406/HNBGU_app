package `in`.campusone.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AssignmentLate
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import `in`.campusone.app.R
import `in`.campusone.app.data.local.entity.AssignmentEntity
import `in`.campusone.app.data.local.entity.ExamEntity
import `in`.campusone.app.domain.model.NoticeItem
import `in`.campusone.app.ui.components.AnimatedAttendanceRing
import `in`.campusone.app.ui.theme.EdgeAmber
import `in`.campusone.app.ui.viewmodel.CoreUiState

@Composable
fun HomeScreen(
    uiState: CoreUiState,
    nextThreeDeadlines: List<AssignmentEntity> = emptyList(),
    nextUpcomingExam: ExamEntity? = null,
    onToggleLanguage: () -> Unit,
    onOpenAttendance: () -> Unit,
    onOpenTimetable: () -> Unit,
    onOpenNotices: () -> Unit,
    onOpenStudyHub: () -> Unit = {}
) {
    val topNotice = uiState.notices.firstOrNull { it.isPinned } ?: uiState.notices.firstOrNull()
    val hasGlance = nextThreeDeadlines.isNotEmpty() || nextUpcomingExam != null || topNotice != null

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Namaste, ${uiState.profile.name.substringBefore(" ")}",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        text = "${uiState.profile.courseName} • Sem ${uiState.profile.semester} (${uiState.profile.section})",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(onClick = onToggleLanguage) {
                    Icon(
                        Icons.Default.Translate,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        if (uiState.languageTag == "hi") "EN" else "हिन्दी",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }

        item {
            ElevatedCard(
                onClick = onOpenTimetable,
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (uiState.nextClass?.isOngoing == true) {
                                    stringResource(R.string.ongoing_class_badge)
                                } else {
                                    stringResource(R.string.next_class_badge)
                                },
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        uiState.nextClass?.let { next ->
                            Text(
                                text = if (next.isOngoing) {
                                    stringResource(R.string.now_label)
                                } else {
                                    stringResource(R.string.starts_in_min, next.minutesUntilStart)
                                },
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    val nextSlot = uiState.nextClass?.slot
                    if (nextSlot != null) {
                        Text(
                            text = nextSlot.subjectName,
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${nextSlot.startTime} – ${nextSlot.endTime} • ${nextSlot.roomNumber}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = nextSlot.teacherName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.no_more_classes_today),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }

        item {
            ElevatedCard(
                onClick = onOpenAttendance,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.attendance_overall),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        SmartCalculatorBanner(insight = uiState.overallInsight)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    AnimatedAttendanceRing(
                        percentage = uiState.overallInsight.currentPercentage,
                        health = uiState.overallInsight.health,
                        size = 76.dp,
                        strokeWidth = 8.dp
                    )
                }
            }
        }

        if (hasGlance) {
            item {
                AtAGlanceCard(
                    deadlines = nextThreeDeadlines,
                    exam = nextUpcomingExam,
                    notice = topNotice,
                    onOpenStudyHub = onOpenStudyHub,
                    onOpenNotices = onOpenNotices
                )
            }
        }
    }
}

@Composable
private fun AtAGlanceCard(
    deadlines: List<AssignmentEntity>,
    exam: ExamEntity?,
    notice: NoticeItem?,
    onOpenStudyHub: () -> Unit,
    onOpenNotices: () -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Text(
                text = stringResource(R.string.home_at_a_glance),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            if (deadlines.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenStudyHub)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AssignmentLate,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.home_upcoming_deadlines),
                            style = MaterialTheme.typography.titleMedium
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
                                    maxLines = 1
                                )
                                Text(
                                    text = "${task.subjectTag} • ${task.dueDateFormatted}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = task.priority,
                                style = MaterialTheme.typography.labelMedium,
                                color = when (task.priority) {
                                    "HIGH" -> MaterialTheme.colorScheme.error
                                    "LOW" -> MaterialTheme.colorScheme.onSurfaceVariant
                                    else -> EdgeAmber
                                }
                            )
                        }
                    }
                }
                if (exam != null || notice != null) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outline
                    )
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenStudyHub)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.home_exam_countdown),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${exam.subjectCode}: ${exam.subjectName}",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${exam.examDateFormatted} • ${exam.roomOrCenter}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = countdown,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (notice != null) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            if (notice != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenNotices)
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Campaign,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (notice.isPinned) {
                                stringResource(R.string.notices_pinned)
                            } else {
                                stringResource(R.string.latest_notice)
                            },
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = notice.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = notice.body,
                        maxLines = 2,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
