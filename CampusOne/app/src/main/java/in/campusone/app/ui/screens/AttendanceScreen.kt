package `in`.campusone.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import `in`.campusone.app.R
import `in`.campusone.app.domain.model.AttendanceStatus
import `in`.campusone.app.domain.usecase.AttendanceHealth
import `in`.campusone.app.domain.usecase.AttendanceInsight
import `in`.campusone.app.ui.components.AnimatedAttendanceRing
import `in`.campusone.app.ui.components.C1Banner
import `in`.campusone.app.ui.components.C1Button
import `in`.campusone.app.ui.components.C1ButtonIntent
import `in`.campusone.app.ui.components.C1EmptyState
import `in`.campusone.app.ui.components.C1SurfaceCard
import `in`.campusone.app.ui.components.C1SectionHeader
import `in`.campusone.app.ui.components.C1StatusChip
import `in`.campusone.app.ui.components.C1Tone
import `in`.campusone.app.ui.theme.C1Radius
import `in`.campusone.app.ui.theme.C1Spacing
import `in`.campusone.app.ui.theme.extendedColors
import `in`.campusone.app.ui.viewmodel.CoreUiState
import `in`.campusone.app.ui.viewmodel.SubjectWithInsight
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun AttendanceScreen(
    uiState: CoreUiState,
    onMarkAttendance: (subjectId: String, status: AttendanceStatus) -> Unit,
    onTargetPctChange: (Int) -> Unit
) {
    val haptics = LocalHapticFeedback.current
    var historyWindowDays by rememberSaveable { mutableIntStateOf(7) }
    val subjectNames = uiState.subjectInsights.associate { it.summary.subjectId to it.summary.subjectName }
    val cutoffEpochDay = LocalDate.now().minusDays(historyWindowDays.toLong()).toEpochDay()
    val filteredLogs = uiState.recentAttendanceLogs.filter { it.dateEpochDay >= cutoffEpochDay }
    val presentCount = filteredLogs.count { it.status == AttendanceStatus.PRESENT }
    val absentCount = filteredLogs.count { it.status == AttendanceStatus.ABSENT }
    val cancelledCount = filteredLogs.count { it.status == AttendanceStatus.CANCELLED }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = C1Spacing.lg,
            end = C1Spacing.lg,
            top = C1Spacing.lg,
            bottom = C1Spacing.xxl
        ),
        verticalArrangement = Arrangement.spacedBy(C1Spacing.lg)
    ) {
        item {
            C1Banner(
                text = stringResource(R.string.attendance_disclaimer),
                tone = C1Tone.INFO,
                icon = Icons.Default.Info
            )
        }

        item {
            OverallAttendanceCard(
                insight = uiState.overallInsight,
                targetPct = uiState.profile.targetAttendancePct,
                onTargetPctChange = onTargetPctChange
            )
        }

        item {
            C1SectionHeader(title = stringResource(R.string.attendance_subjects))
        }

        if (uiState.subjectInsights.isEmpty()) {
            item {
                C1SurfaceCard(
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ) {
                    C1EmptyState(
                        icon = Icons.Default.Info,
                        title = stringResource(R.string.no_classes_recorded),
                        message = stringResource(R.string.attendance_disclaimer)
                    )
                }
            }
        } else {
            items(uiState.subjectInsights, key = { it.summary.subjectId }) { item ->
                SubjectAttendanceCard(
                    item = item,
                    onMark = { status ->
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onMarkAttendance(item.summary.subjectId, status)
                    }
                )
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(C1Spacing.sm)) {
                C1SectionHeader(title = stringResource(R.string.attendance_history))
                Row(horizontalArrangement = Arrangement.spacedBy(C1Spacing.sm)) {
                    FilterChip(
                        selected = historyWindowDays == 7,
                        onClick = { historyWindowDays = 7 },
                        label = { Text(stringResource(R.string.history_weekly)) }
                    )
                    FilterChip(
                        selected = historyWindowDays == 30,
                        onClick = { historyWindowDays = 30 },
                        label = { Text(stringResource(R.string.history_monthly)) }
                    )
                }
                Text(
                    text = stringResource(
                        R.string.attendance_window_summary,
                        presentCount,
                        absentCount,
                        cancelledCount
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (filteredLogs.isEmpty()) {
            item {
                C1SurfaceCard(
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ) {
                    C1EmptyState(
                        icon = Icons.Default.History,
                        title = stringResource(R.string.no_classes_recorded),
                        message = stringResource(R.string.attendance_history_hint)
                    )
                }
            }
        } else {
            items(filteredLogs.take(30), key = { it.id }) { log ->
                AttendanceLogRow(
                    subjectName = subjectNames[log.subjectId] ?: log.subjectId,
                    dateIso = log.dateIso,
                    isSynced = log.isSynced,
                    status = log.status
                )
            }
        }
    }
}

@Composable
private fun OverallAttendanceCard(
    insight: AttendanceInsight,
    targetPct: Int,
    onTargetPctChange: (Int) -> Unit
) {
    val extended = extendedColors
    val absentCount = (insight.totalConducted - insight.attended).coerceAtLeast(0)
    val belowTarget = insight.health == AttendanceHealth.WARNING || insight.health == AttendanceHealth.CRITICAL

    C1SurfaceCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(C1Spacing.lg),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(modifier = Modifier.padding(C1Spacing.xl)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.attendance_overall),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(C1Spacing.xs))
                    Text(
                        text = stringResource(
                            R.string.attendance_attended_of,
                            insight.attended,
                            insight.totalConducted
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(C1Spacing.sm))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(C1Spacing.sm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        C1StatusChip(
                            text = stringResource(R.string.attendance_target, targetPct),
                            tone = C1Tone.NEUTRAL
                        )
                        if (belowTarget) {
                            C1StatusChip(
                                text = stringResource(R.string.attendance_below_target, targetPct),
                                tone = if (insight.health == AttendanceHealth.CRITICAL) C1Tone.DANGER else C1Tone.WARNING
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.width(C1Spacing.lg))
                AnimatedAttendanceRing(
                    percentage = insight.currentPercentage,
                    health = insight.health,
                    size = 92.dp,
                    strokeWidth = 10.dp
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = C1Spacing.lg),
                color = extended.hairline
            )

            Row(modifier = Modifier.fillMaxWidth()) {
                AttendanceStat(
                    value = insight.attended,
                    label = stringResource(R.string.mark_present),
                    valueColor = extended.success,
                    modifier = Modifier.weight(1f)
                )
                AttendanceStat(
                    value = absentCount,
                    label = stringResource(R.string.mark_absent),
                    valueColor = MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f)
                )
                AttendanceStat(
                    value = insight.cancelled,
                    label = stringResource(R.string.mark_cancelled),
                    valueColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                AttendanceStat(
                    value = insight.totalConducted,
                    label = stringResource(R.string.attendance_total_lectures),
                    valueColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(C1Spacing.md))
            SmartCalculatorBanner(insight = insight)

            Spacer(modifier = Modifier.height(C1Spacing.md))
            Slider(
                value = targetPct.toFloat(),
                onValueChange = { onTargetPctChange(it.toInt()) },
                valueRange = 60f..90f,
                steps = 5
            )
        }
    }
}

@Composable
private fun AttendanceStat(
    value: Int,
    label: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SubjectAttendanceCard(
    item: SubjectWithInsight,
    onMark: (AttendanceStatus) -> Unit
) {
    val summary = item.summary
    val insight = item.insight
    val extended = extendedColors
    val belowTarget = insight.health == AttendanceHealth.WARNING || insight.health == AttendanceHealth.CRITICAL

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = C1Radius.md,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(modifier = Modifier.padding(C1Spacing.lg)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = summary.subjectName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = summary.subjectCode,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(C1Spacing.xs))
                    Text(
                        text = stringResource(
                            R.string.attendance_attended_of,
                            summary.attendedCount,
                            summary.totalClasses
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(C1Spacing.md))
                AnimatedAttendanceRing(
                    percentage = insight.currentPercentage,
                    health = insight.health,
                    size = 64.dp,
                    strokeWidth = 7.dp
                )
            }

            Spacer(modifier = Modifier.height(C1Spacing.sm))
            Row(
                horizontalArrangement = Arrangement.spacedBy(C1Spacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (belowTarget) {
                    C1StatusChip(
                        text = stringResource(R.string.attendance_below_target, insight.targetPercentage),
                        tone = if (insight.health == AttendanceHealth.CRITICAL) C1Tone.DANGER else C1Tone.WARNING
                    )
                }
                if (summary.cancelledCount > 0) {
                    C1StatusChip(
                        text = stringResource(R.string.attendance_cancelled_short, summary.cancelledCount),
                        tone = C1Tone.NEUTRAL
                    )
                }
            }

            Spacer(modifier = Modifier.height(C1Spacing.xs))
            SmartCalculatorBanner(insight = insight)

            Spacer(modifier = Modifier.height(C1Spacing.md))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(C1Spacing.sm)
            ) {
                C1Button(
                    text = stringResource(R.string.mark_present),
                    onClick = { onMark(AttendanceStatus.PRESENT) },
                    intent = C1ButtonIntent.PRIMARY,
                    modifier = Modifier.weight(1f)
                )
                C1Button(
                    text = stringResource(R.string.mark_absent),
                    onClick = { onMark(AttendanceStatus.ABSENT) },
                    intent = C1ButtonIntent.OUTLINED,
                    modifier = Modifier.weight(1f)
                )
                C1Button(
                    text = stringResource(R.string.mark_cancelled),
                    onClick = { onMark(AttendanceStatus.CANCELLED) },
                    intent = C1ButtonIntent.GHOST,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun AttendanceLogRow(
    subjectName: String,
    dateIso: String,
    isSynced: Boolean,
    status: AttendanceStatus
) {
    val formatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }
    val displayDate = remember(dateIso) {
        runCatching { LocalDate.parse(dateIso).format(formatter) }.getOrDefault(dateIso)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = C1Radius.sm,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = C1Spacing.lg, vertical = C1Spacing.md),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = subjectName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                Text(
                    text = displayDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!isSynced) {
                    Text(
                        text = stringResource(R.string.saved_on_device),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.width(C1Spacing.sm))
            C1StatusChip(
                text = stringResource(statusLabelRes(status)),
                tone = statusTone(status)
            )
        }
    }
}

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

private fun statusLabelRes(status: AttendanceStatus): Int = when (status) {
    AttendanceStatus.PRESENT -> R.string.mark_present
    AttendanceStatus.ABSENT -> R.string.mark_absent
    AttendanceStatus.CANCELLED -> R.string.mark_cancelled
}

private fun statusTone(status: AttendanceStatus): C1Tone = when (status) {
    AttendanceStatus.PRESENT -> C1Tone.SUCCESS
    AttendanceStatus.ABSENT -> C1Tone.DANGER
    AttendanceStatus.CANCELLED -> C1Tone.NEUTRAL
}
