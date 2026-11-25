package `in`.campusone.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.campusone.app.R
import `in`.campusone.app.domain.model.AttendanceStatus
import `in`.campusone.app.domain.model.AppNotification
import `in`.campusone.app.domain.usecase.AttendanceCalculator
import `in`.campusone.app.domain.usecase.AttendanceHealth
import `in`.campusone.app.ui.viewmodel.CoreUiState
import `in`.campusone.app.ui.theme.extendedColors
import `in`.campusone.app.ui.viewmodel.CommunityViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.style.TextAlign
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

private val FlagshipBrand: Color
    @Composable get() = MaterialTheme.colorScheme.primary
private val FlagshipGreen: Color
    @Composable get() = extendedColors.success
private val FlagshipGreenSoft: Color
    @Composable get() = extendedColors.successContainer
private val FlagshipMint: Color
    @Composable get() = MaterialTheme.colorScheme.secondaryContainer
private val FlagshipInfo: Color
    @Composable get() = extendedColors.infoContainer
private val FlagshipOchre: Color
    @Composable get() = extendedColors.warningContainer
private val FlagshipRed: Color
    @Composable get() = MaterialTheme.colorScheme.errorContainer
private val FlagshipInk: Color
    @Composable get() = MaterialTheme.colorScheme.onSurface
private val FlagshipMuted: Color
    @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
private val FlagshipCanvas: Color
    @Composable get() = MaterialTheme.colorScheme.background
private val FlagshipLine: Color
    @Composable get() = MaterialTheme.colorScheme.outlineVariant

@Composable
fun FlagshipAppHeader(
    campusName: String,
    userName: String = "",
    onNotifications: () -> Unit = {},
    onProfile: () -> Unit = {},
    showActions: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.98f))
            .padding(horizontal = 16.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        androidx.compose.foundation.Image(
            painter = painterResource(R.drawable.ic_campusone_c_mark),
            contentDescription = "CampusOne",
            modifier = Modifier.size(32.dp),
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary)
        )
        Spacer(Modifier.width(9.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("CampusOne", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = FlagshipBrand)
                Spacer(Modifier.width(5.dp))
                Box(Modifier.size(6.dp).clip(CircleShape).background(extendedColors.success))
                Spacer(Modifier.width(6.dp))
                Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(50)) {
                    Text("HNBGU", modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp), fontSize = 10.sp, color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.SemiBold)
                }
            }
            Text(
                campusName.ifBlank { "Birla & Chauras Campuses" },
                fontSize = 11.sp,
                color = FlagshipMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (showActions) {
            IconButton(onClick = onNotifications, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Default.Notifications, contentDescription = "Notifications", tint = MaterialTheme.colorScheme.onSurface)
            }
            IconButton(onClick = onProfile, modifier = Modifier.size(40.dp)) {
                Surface(shape = CircleShape, color = FlagshipBrand, modifier = Modifier.size(31.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        if (userName.isNotBlank()) {
                            Text(userName.trim().take(1).uppercase(), color = MaterialTheme.colorScheme.onPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Default.Person, contentDescription = "Profile", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FlagshipPageTitle(
    title: String,
    subtitle: String,
    eyebrow: String? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            if (!eyebrow.isNullOrBlank()) {
                Text(eyebrow.uppercase(), fontSize = 10.sp, color = FlagshipBrand, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = FlagshipInk)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = FlagshipMuted)
        }
        trailing?.invoke()
    }
}

@Composable
fun FlagshipCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentPadding: PaddingValues = PaddingValues(15.dp),
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = containerColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, FlagshipLine),
        shadowElevation = 0.dp
    ) {
        Column(modifier = Modifier.padding(contentPadding), content = content)
    }
}

@Composable
fun FlagshipPill(
    text: String,
    background: Color = MaterialTheme.colorScheme.primaryContainer,
    foreground: Color = MaterialTheme.colorScheme.onPrimaryContainer
) {
    Surface(shape = RoundedCornerShape(50), color = background) {
        Text(text, modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp), fontSize = 10.sp, color = foreground, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun FlagshipSectionTitle(
    title: String,
    subtitle: String? = null,
    action: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = FlagshipInk, fontWeight = FontWeight.Bold)
            if (!subtitle.isNullOrBlank()) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = FlagshipMuted)
        }
        if (action != null && onAction != null) TextButton(onClick = onAction) { Text(action, color = FlagshipBrand, fontWeight = FontWeight.SemiBold) }
    }
}

@Composable
fun FlagshipPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(46.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = FlagshipBrand, contentColor = MaterialTheme.colorScheme.onPrimary)
    ) {
        Text(text, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun FlagshipAttendanceScreen(
    uiState: CoreUiState,
    onMarkAttendance: (subjectId: String, status: AttendanceStatus) -> Unit,
    onTargetPctChange: (Int) -> Unit,
    onOpenNotifications: () -> Unit = {},
    onOpenProfile: () -> Unit = {}
) {
    val haptics = LocalHapticFeedback.current
    var futureMissed by rememberSaveable { mutableIntStateOf(1) }
    var futureAttended by rememberSaveable { mutableIntStateOf(0) }
    var showHistory by rememberSaveable { mutableStateOf(false) }
    val insight = uiState.overallInsight
    val target = uiState.profile.targetAttendancePct
    val projection = remember(insight, futureMissed, futureAttended, target) {
        AttendanceCalculator.calculate(
            attended = insight.attended + futureAttended,
            totalConducted = insight.totalConducted + futureMissed + futureAttended,
            cancelled = insight.cancelled,
            targetPercentage = target
        )
    }
    val names = uiState.subjectInsights.associate { it.summary.subjectId to it.summary.subjectName }
    val last28 = remember(uiState.recentAttendanceLogs) {
        (27 downTo 0).map { LocalDate.now().minusDays(it.toLong()) }
    }
    val grouped = remember(uiState.recentAttendanceLogs) {
        uiState.recentAttendanceLogs.groupBy { runCatching { LocalDate.parse(it.dateIso) }.getOrNull() }
    }

    Column(modifier = Modifier.fillMaxSize().background(FlagshipCanvas)) {
        FlagshipAppHeader(
            campusName = uiState.profile.collegeName,
            userName = uiState.profile.name,
            onNotifications = onOpenNotifications,
            onProfile = onOpenProfile
        )
        androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp)
        ) {
            item {
                FlagshipCard(
                    containerColor = extendedColors.successContainer,
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("AGGREGATE STANDING", fontSize = 11.sp, color = FlagshipGreen, fontWeight = FontWeight.Bold, letterSpacing = 0.4.sp)
                                FlagshipPill(
                                    if (insight.classesCanMissSafely > 0) "Safe Zone" else if (insight.currentPercentage >= target) "On edge" else "Recovery",
                                    background = if (insight.currentPercentage >= target) FlagshipGreenSoft else FlagshipOchre,
                                    foreground = FlagshipGreen
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                String.format(Locale.getDefault(), "%.1f%%", insight.currentPercentage),
                                fontSize = 29.sp, lineHeight = 34.sp, fontWeight = FontWeight.ExtraBold, color = FlagshipInk
                            )
                            Text(insight.attended.toString() + " / " + insight.totalConducted + " Attended", fontSize = 12.sp, color = FlagshipMuted)
                            Text("Statutory threshold: " + target + "%", fontSize = 11.sp, color = FlagshipMuted)
                        }
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(72.dp)) {
                            CircularProgressIndicator(
                                progress = { (insight.currentPercentage / 100.0).toFloat().coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxSize(),
                                strokeWidth = 7.dp,
                                color = FlagshipGreen,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                            Text(if (insight.totalConducted > 0) insight.currentPercentage.toInt().toString() else "—", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = FlagshipGreen)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(13.dp)) {
                        Row(modifier = Modifier.padding(11.dp), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = FlagshipGreen, modifier = Modifier.size(17.dp))
                            Column {
                                Text(if (insight.classesCanMissSafely > 0) "Safe Zone: You can miss up to " + insight.classesCanMissSafely + " classes" else if (insight.classesMustAttendToRecover > 0) "Attendance recovery required" else "Attendance position is being calculated", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = FlagshipGreen)
                                Text(
                                    if (insight.classesCanMissSafely > 0) "Your current attendance is above the " + target + "% institutional requirement."
                                    else if (insight.classesMustAttendToRecover > 0) "Attend " + insight.classesMustAttendToRecover + " consecutive classes to recover to the target."
                                    else "Attendance appears here as classes are recorded.",
                                    fontSize = 11.sp, color = FlagshipMuted
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text("Target attendance: " + target + "%", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = FlagshipInk)
                    androidx.compose.material3.Slider(
                        value = target.toFloat(),
                        onValueChange = { onTargetPctChange(it.toInt()) },
                        valueRange = 60f..90f,
                        steps = 5
                    )
                }
            }
            item {
                FlagshipCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Interactive Bunk Simulator", style = MaterialTheme.typography.titleMedium, color = FlagshipInk, fontWeight = FontWeight.Bold)
                            Text("Real-time projection based on your recorded classes", style = MaterialTheme.typography.bodySmall, color = FlagshipMuted)
                        }
                        FlagshipPill("Dynamic", FlagshipInfo, extendedColors.onInfoContainer)
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        ProjectionStepper("Future missed classes", futureMissed, { futureMissed = (futureMissed - 1).coerceAtLeast(0) }, { futureMissed = (futureMissed + 1).coerceAtMost(30) }, Modifier.weight(1f))
                        ProjectionStepper("Future attended classes", futureAttended, { futureAttended = (futureAttended - 1).coerceAtLeast(0) }, { futureAttended = (futureAttended + 1).coerceAtMost(30) }, Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(10.dp))
                    Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = RoundedCornerShape(13.dp)) {
                        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Projected standing", fontSize = 11.sp, color = FlagshipMuted)
                                Text(
                                    if (projection.totalConducted == 0) "No classes recorded" else projection.attended.toString() + " / " + projection.totalConducted + " attended",
                                    fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = FlagshipInk
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(String.format(Locale.getDefault(), "%.1f%%", projection.currentPercentage), fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = if (projection.currentPercentage >= target) extendedColors.success else extendedColors.warning)
                                Text(if (projection.currentPercentage >= target) "Above target" else "Below target", fontSize = 10.sp, color = FlagshipMuted)
                            }
                        }
                    }
                }
            }
            item {
                FlagshipCard {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("4-Week Attendance Rhythm", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = FlagshipInk)
                            Text("Recorded lecture consistency", style = MaterialTheme.typography.bodySmall, color = FlagshipMuted)
                        }
                        FlagshipPill("Last 28 days")
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("M", "T", "W", "T", "F", "S", "S").forEach {
                            Text(it, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontSize = 10.sp, color = FlagshipMuted)
                        }
                    }
                    last28.chunked(7).forEach { week ->
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            week.forEach { date ->
                                val entries = grouped[date].orEmpty()
                                val dayBackground = when {
                                    entries.isEmpty() || entries.all { it.status == AttendanceStatus.CANCELLED } -> MaterialTheme.colorScheme.surfaceVariant
                                    entries.any { it.status == AttendanceStatus.ABSENT } -> extendedColors.warningContainer
                                    else -> extendedColors.successContainer
                                }
                                val dayForeground = when {
                                    entries.isEmpty() || entries.all { it.status == AttendanceStatus.CANCELLED } -> MaterialTheme.colorScheme.onSurfaceVariant
                                    entries.any { it.status == AttendanceStatus.ABSENT } -> extendedColors.onWarningContainer
                                    else -> extendedColors.onSuccessContainer
                                }
                                Box(
                                    modifier = Modifier.weight(1f).height(31.dp).clip(RoundedCornerShape(7.dp)).background(dayBackground),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(if (entries.isEmpty()) "" else date.dayOfMonth.toString(), fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = dayForeground)
                                }
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        RhythmLegend("Present", extendedColors.success)
                        RhythmLegend("Missed", extendedColors.warning)
                        RhythmLegend("No record", MaterialTheme.colorScheme.surfaceVariant)
                    }
                }
            }
            item {
                FlagshipSectionTitle("Curriculum Breakdown", uiState.profile.courseName + " · Semester " + uiState.profile.semester)
            }
            if (uiState.subjectInsights.isEmpty()) {
                item {
                    FlagshipCard {
                        Text("No attendance records yet", fontWeight = FontWeight.Bold, color = FlagshipInk)
                        Text("Your subjects will appear here when your academic schedule is available.", color = FlagshipMuted, fontSize = 12.sp)
                    }
                }
            } else {
                items(uiState.subjectInsights.size) { index ->
                    val subject = uiState.subjectInsights[index]
                    val summary = subject.summary
                    val percent = subject.insight.currentPercentage
                    val healthy = subject.insight.health == AttendanceHealth.SAFE || subject.insight.health == AttendanceHealth.ON_EDGE
                    FlagshipCard(contentPadding = PaddingValues(14.dp), containerColor = if (healthy) extendedColors.successContainer else extendedColors.warningContainer) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(summary.subjectCode, fontSize = 10.sp, color = extendedColors.info, fontWeight = FontWeight.Bold)
                                Text(summary.subjectName, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = FlagshipInk)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(String.format(Locale.getDefault(), "%.0f%%", percent), fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = if (healthy) extendedColors.success else extendedColors.warning)
                                Text(summary.attendedCount.toString() + " / " + summary.totalClasses + " attended", fontSize = 10.sp, color = FlagshipMuted)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { (percent / 100.0).toFloat().coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().height(7.dp).clip(CircleShape),
                            color = if (healthy) FlagshipGreen else extendedColors.warning,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Spacer(Modifier.height(7.dp))
                        Text(
                            if (subject.insight.classesCanMissSafely > 0) "Safe (+ " + subject.insight.classesCanMissSafely + " leeway)" else if (subject.insight.classesMustAttendToRecover > 0) "Shortfall: attend " + subject.insight.classesMustAttendToRecover + " consecutive classes" else "On target",
                            fontSize = 11.sp, color = if (healthy) FlagshipGreen else extendedColors.warning, fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.height(9.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            TextButton(onClick = { haptics.performHapticFeedback(HapticFeedbackType.LongPress); onMarkAttendance(summary.subjectId, AttendanceStatus.PRESENT) }) { Text("Present", fontSize = 11.sp, color = FlagshipGreen) }
                            TextButton(onClick = { haptics.performHapticFeedback(HapticFeedbackType.LongPress); onMarkAttendance(summary.subjectId, AttendanceStatus.ABSENT) }) { Text("Missed", fontSize = 11.sp, color = extendedColors.warning) }
                            TextButton(onClick = { onMarkAttendance(summary.subjectId, AttendanceStatus.CANCELLED) }) { Text("Cancelled", fontSize = 11.sp, color = FlagshipMuted) }
                        }
                    }
                }
            }
            item {
                FlagshipCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Attendance History", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = FlagshipInk)
                        TextButton(onClick = { showHistory = !showHistory }) { Text(if (showHistory) "Hide" else "View history", color = FlagshipGreen) }
                    }
                    if (showHistory) {
                        val logs = uiState.recentAttendanceLogs.take(20)
                        if (logs.isEmpty()) Text("No attendance activity has been recorded.", fontSize = 12.sp, color = FlagshipMuted)
                        logs.forEach { log ->
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(names[log.subjectId] ?: log.subjectId, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = FlagshipInk)
                                    Text(log.dateIso, fontSize = 10.sp, color = FlagshipMuted)
                                }
                                FlagshipPill(log.status.name.lowercase().replaceFirstChar { it.uppercase() }, when (log.status) {
                                    AttendanceStatus.PRESENT -> FlagshipGreenSoft
                                    AttendanceStatus.ABSENT -> FlagshipOchre
                                    AttendanceStatus.CANCELLED -> MaterialTheme.colorScheme.surfaceVariant
                                }, when (log.status) {
                                    AttendanceStatus.PRESENT -> FlagshipGreen
                                    AttendanceStatus.ABSENT -> extendedColors.warning
                                    AttendanceStatus.CANCELLED -> FlagshipMuted
                                })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProjectionStepper(label: String, value: Int, onMinus: () -> Unit, onPlus: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(13.dp)).padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 10.sp, color = FlagshipMuted, minLines = 2, textAlign = TextAlign.Center)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            IconButton(onClick = onMinus, modifier = Modifier.size(30.dp)) { Text("−", color = FlagshipGreen, fontSize = 18.sp, fontWeight = FontWeight.Bold) }
            Text(value.toString(), fontSize = 21.sp, fontWeight = FontWeight.Bold, color = FlagshipInk, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            IconButton(onClick = onPlus, modifier = Modifier.size(30.dp)) { Text("+", color = FlagshipGreen, fontSize = 18.sp, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun RhythmLegend(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.size(7.dp).clip(CircleShape).background(color))
        Text(label, fontSize = 9.sp, color = FlagshipMuted)
    }
}

@Composable
fun FlagshipNotificationsScreen(
    viewModel: CommunityViewModel,
    onBack: () -> Unit,
    onNavigate: (targetType: String, targetId: String) -> Unit,
    onOpenNotifications: () -> Unit = {},
    onOpenProfile: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var filter by rememberSaveable { mutableStateOf("ALL") }
    LaunchedEffect(Unit) { viewModel.refreshNotifications() }
    val visible = uiState.notifications.filter { item ->
        when (filter) {
            "ACADEMIC" -> item.type.contains("class", true) || item.type.contains("academic", true) || item.type.contains("assignment", true) || item.type.contains("exam", true)
            "NOTICES" -> item.type.contains("notice", true) || item.type.contains("circular", true)
            "DEADLINES" -> item.type.contains("deadline", true) || item.type.contains("assignment", true)
            else -> true
        }
    }.sortedByDescending { it.createdAtMillis }
    val unread = uiState.notifications.count { !it.isRead }
    val grouped = visible.groupBy { notification ->
        val date = Instant.ofEpochMilli(notification.createdAtMillis).atZone(ZoneId.systemDefault()).toLocalDate()
        when (ChronoUnit.DAYS.between(date, LocalDate.now())) {
            0L -> "Today"
            1L -> "Yesterday"
            in 2L..6L -> "Earlier this week"
            else -> "Earlier"
        }
    }
    val order = listOf("Today", "Yesterday", "Earlier this week", "Earlier")

    Column(modifier = Modifier.fillMaxSize().background(FlagshipCanvas)) {
        FlagshipAppHeader(uiState.profile.collegeName, uiState.profile.name, onNotifications = onOpenNotifications, onProfile = onOpenProfile)
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            FlagshipPageTitle(
                "Activity Feed",
                unread.toString() + " unread · " + uiState.notifications.size + " total",
                trailing = {
                    if (unread > 0) TextButton(onClick = viewModel::markAllNotificationsRead) {
                        Text("Mark all read", color = FlagshipGreen, fontWeight = FontWeight.SemiBold)
                    }
                }
            )
            androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                items(listOf("ALL", "ACADEMIC", "NOTICES", "DEADLINES").size) { idx ->
                    val key = listOf("ALL", "ACADEMIC", "NOTICES", "DEADLINES")[idx]
                    val label = listOf("All (" + uiState.notifications.size + ")", "Academic & Classes", "Notices & Circulars", "Deadlines")[idx]
                    FilterChip(selected = filter == key, onClick = { filter = key }, label = { Text(label, fontSize = 11.sp) })
                }
            }
        }
        androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 15.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            if (visible.isEmpty()) {
                item {
                    FlagshipCard {
                        Text("You're all caught up", fontWeight = FontWeight.Bold, color = FlagshipInk)
                        Text("New academic updates, notices and deadlines will appear here.", fontSize = 12.sp, color = FlagshipMuted)
                    }
                }
            }
            order.forEach { group ->
                val entries = grouped[group].orEmpty()
                if (entries.isNotEmpty()) {
                    item {
                        Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(group.uppercase(), fontSize = 10.sp, letterSpacing = 1.sp, color = FlagshipMuted, fontWeight = FontWeight.Bold)
                            Text(entries.size.toString() + " alerts", fontSize = 10.sp, color = FlagshipMuted)
                        }
                    }
                    items(entries.size, key = { entries[it].id }) { index ->
                        val notification = entries[index]
                        FlagshipNotificationCard(notification) {
                            viewModel.markNotificationRead(notification.id)
                            onNavigate(notification.targetType, notification.targetId)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FlagshipNotificationCard(notification: AppNotification, onClick: () -> Unit) {
    val type = notification.type.lowercase()
    val iconBg = when {
        type.contains("assignment") || type.contains("deadline") -> FlagshipOchre
        type.contains("notice") || type.contains("exam") -> FlagshipInfo
        type.contains("attendance") -> FlagshipGreenSoft
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    FlagshipCard(
        modifier = Modifier.clickable(onClick = onClick),
        contentPadding = PaddingValues(14.dp)
    ) {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(38.dp).clip(RoundedCornerShape(11.dp)).background(iconBg), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(19.dp))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(notification.type.replace('_', ' ').ifBlank { "Campus update" }, fontSize = 10.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Text(
                        Instant.ofEpochMilli(notification.createdAtMillis).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("hh:mm a", Locale.getDefault())),
                        fontSize = 10.sp, color = FlagshipMuted
                    )
                }
                Text(notification.title, fontSize = 14.sp, color = FlagshipInk, fontWeight = FontWeight.SemiBold)
                if (notification.body.isNotBlank()) Text(notification.body, fontSize = 12.sp, lineHeight = 18.sp, color = FlagshipMuted)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (notification.isRead) "Read" else "New · Tap to open", fontSize = 10.sp, color = if (notification.isRead) FlagshipMuted else MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.weight(1f))
                    Icon(Icons.Default.ArrowForward, contentDescription = "Open notification", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
