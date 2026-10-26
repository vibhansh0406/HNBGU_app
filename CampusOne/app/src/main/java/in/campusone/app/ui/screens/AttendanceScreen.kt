package `in`.campusone.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import `in`.campusone.app.R
import `in`.campusone.app.domain.model.AttendanceStatus
import `in`.campusone.app.domain.usecase.AttendanceHealth
import `in`.campusone.app.domain.usecase.AttendanceInsight
import `in`.campusone.app.ui.components.AnimatedAttendanceRing
import `in`.campusone.app.ui.theme.DangerRed
import `in`.campusone.app.ui.theme.EdgeAmber
import `in`.campusone.app.ui.theme.SafeGreen
import `in`.campusone.app.ui.viewmodel.CoreUiState
import `in`.campusone.app.ui.viewmodel.SubjectWithInsight
import java.time.LocalDate

@Composable
fun AttendanceScreen(
    uiState: CoreUiState,
    onMarkAttendance: (subjectId: String, status: AttendanceStatus) -> Unit,
    onTargetPctChange: (Int) -> Unit
) {
    val haptics = LocalHapticFeedback.current
    var historyWindowDays by rememberSaveable { mutableStateOf(7) } // 7 = Weekly, 30 = Monthly

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            // Personal tracker disclaimer banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.attendance_disclaimer),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }

        // Overall Attendance Summary + Configurable Target Slider
        item {
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.attendance_overall),
                                style = MaterialTheme.typography.titleLarge
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${uiState.overallInsight.attended} / ${uiState.overallInsight.totalConducted} classes attended (${uiState.overallInsight.cancelled} cancelled)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            SmartCalculatorBanner(insight = uiState.overallInsight)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        AnimatedAttendanceRing(
                            percentage = uiState.overallInsight.currentPercentage,
                            health = uiState.overallInsight.health,
                            size = 92.dp,
                            strokeWidth = 10.dp
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = stringResource(
                            R.string.attendance_target,
                            uiState.profile.targetAttendancePct
                        ),
                        style = MaterialTheme.typography.labelLarge
                    )
                    Slider(
                        value = uiState.profile.targetAttendancePct.toFloat(),
                        onValueChange = { onTargetPctChange(it.toInt()) },
                        valueRange = 60f..90f,
                        steps = 5
                    )
                }
            }
        }

        // Per-Subject Cards with Present / Absent / Cancelled buttons
        items(uiState.subjectInsights, key = { it.summary.subjectId }) { item ->
            SubjectAttendanceCard(
                item = item,
                onMark = { status ->
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onMarkAttendance(item.summary.subjectId, status)
                }
            )
        }

        // Weekly / Monthly History Section
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.History, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Attendance Log History",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = historyWindowDays == 7,
                        onClick = { historyWindowDays = 7 },
                        label = { Text("Weekly (7d)") }
                    )
                    FilterChip(
                        selected = historyWindowDays == 30,
                        onClick = { historyWindowDays = 30 },
                        label = { Text("Monthly (30d)") }
                    )
                }
            }
        }

        val cutoffEpochDay = LocalDate.now().minusDays(historyWindowDays.toLong()).toEpochDay()
        val filteredLogs = uiState.recentAttendanceLogs.filter { it.dateEpochDay >= cutoffEpochDay }

        if (filteredLogs.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.no_classes_recorded),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }
        } else {
            items(filteredLogs.take(20), key = { it.id }) { log ->
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "${log.subjectId} • ${log.dateIso}",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = if (log.isSynced) "Synced to Cloud" else "Saved Offline (Room DB)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        AssistChip(
                            onClick = {},
                            label = { Text(log.status.name) }
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(28.dp)) }
    }
}

@Composable
fun SubjectAttendanceCard(
    item: SubjectWithInsight,
    onMark: (AttendanceStatus) -> Unit
) {
    val summary = item.summary
    val insight = item.insight

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = summary.subjectName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${summary.subjectCode} • Attended: ${summary.attendedCount}/${summary.totalClasses} (Cancelled: ${summary.cancelledCount})",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    SmartCalculatorBanner(insight = insight)
                }
                Spacer(modifier = Modifier.width(12.dp))
                AnimatedAttendanceRing(
                    percentage = insight.currentPercentage,
                    health = insight.health,
                    size = 72.dp,
                    strokeWidth = 8.dp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = { onMark(AttendanceStatus.PRESENT) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.mark_present))
                }
                OutlinedButton(
                    onClick = { onMark(AttendanceStatus.ABSENT) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Cancel, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.mark_absent))
                }
                OutlinedButton(
                    onClick = { onMark(AttendanceStatus.CANCELLED) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.RemoveCircleOutline, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.mark_cancelled))
                }
            }
        }
    }
}

@Composable
fun SmartCalculatorBanner(insight: AttendanceInsight) {
    val (text, color) = when (insight.health) {
        AttendanceHealth.NO_DATA ->
            stringResource(R.string.no_classes_recorded) to MaterialTheme.colorScheme.onSurfaceVariant
        AttendanceHealth.SAFE ->
            stringResource(
                R.string.bunk_safe,
                insight.classesCanMissSafely,
                insight.targetPercentage
            ) to SafeGreen
        AttendanceHealth.ON_EDGE ->
            stringResource(
                R.string.bunk_on_edge,
                insight.targetPercentage
            ) to EdgeAmber
        AttendanceHealth.WARNING, AttendanceHealth.CRITICAL ->
            stringResource(
                R.string.bunk_recover,
                insight.classesMustAttendToRecover,
                insight.targetPercentage
            ) to DangerRed
    }
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.SemiBold,
        color = color
    )
}
