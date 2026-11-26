package `in`.campusone.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Remove
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
import `in`.campusone.app.ui.theme.CampusOneBrandGreen
import `in`.campusone.app.ui.theme.C1Height
import `in`.campusone.app.ui.theme.C1Radius
import `in`.campusone.app.ui.theme.C1Spacing
import `in`.campusone.app.ui.theme.componentColors
import `in`.campusone.app.ui.theme.extendedColors
import `in`.campusone.app.ui.theme.windowSize
import `in`.campusone.app.ui.components.C1Button
import `in`.campusone.app.ui.components.C1FilterChip
import `in`.campusone.app.ui.components.C1StatusChip
import `in`.campusone.app.ui.components.C1Tone
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
    showActions: Boolean = true,
    showNotifications: Boolean = true
) {
    val ws = windowSize
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = C1Spacing.pagePadding(ws), vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(C1Radius.tile)
                .background(CampusOneBrandGreen),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.foundation.Image(
                painter = painterResource(R.drawable.ic_campusone_c_mark),
                contentDescription = "CampusOne",
                modifier = Modifier.size(23.dp),
                colorFilter = ColorFilter.tint(Color.White)
            )
        }
        Spacer(Modifier.width(C1Spacing.sm))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "CampusOne",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = FlagshipBrand
                )
                Spacer(Modifier.width(6.dp))
                Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = C1Radius.pill) {
                    Text(
                        "HNBGU",
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Text(
                campusName.ifBlank { "Birla & Chauras Campuses" },
                style = MaterialTheme.typography.labelSmall,
                color = FlagshipMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (showActions) {
            if (showNotifications) {
                IconButton(onClick = onNotifications, modifier = Modifier.size(C1Height.touch)) {
                    Icon(
                        Icons.Default.Notifications,
                        contentDescription = "Notifications",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(23.dp)
                    )
                }
            }
            IconButton(onClick = onProfile, modifier = Modifier.size(C1Height.touch)) {
                Surface(shape = CircleShape, color = FlagshipBrand, modifier = Modifier.size(32.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        if (userName.isNotBlank()) {
                            Text(
                                userName.trim().take(1).uppercase(),
                                color = MaterialTheme.colorScheme.onPrimary,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = "Profile",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FlagshipSubHeader(
    title: String,
    onBack: () -> Unit,
    subtitle: String? = null,
    actions: (@Composable androidx.compose.foundation.layout.RowScope.() -> Unit)? = null
) {
    val ws = windowSize
    Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .padding(start = C1Spacing.pagePadding(ws) - 8.dp, end = C1Spacing.pagePadding(ws) - 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(C1Height.touch)) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Column(modifier = Modifier.weight(1f).padding(start = 4.dp, end = 8.dp), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = FlagshipInk,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = FlagshipMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            actions?.let { it() }
        }
        androidx.compose.material3.HorizontalDivider(color = FlagshipLine)
    }
}

@Composable
fun FlagshipPageTitle(
    title: String,
    subtitle: String,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = FlagshipInk, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = FlagshipMuted, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        trailing?.let { Box(modifier = Modifier.padding(start = 8.dp), contentAlignment = Alignment.CenterEnd) { it() } }
    }
}

@Composable
fun FlagshipCard(
    modifier: Modifier = Modifier,
    containerColor: Color = componentColors.cardBackground,
    contentPadding: PaddingValues = PaddingValues(C1Spacing.lg),
    onClick: (() -> Unit)? = null,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = C1Radius.card,
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
    Surface(shape = C1Radius.pill, color = background) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = foreground,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
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
            Text(title, style = MaterialTheme.typography.titleMedium, color = FlagshipInk, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (!subtitle.isNullOrBlank()) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = FlagshipMuted, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        if (action != null && onAction != null) TextButton(onClick = onAction, modifier = Modifier.heightIn(min = C1Height.touch)) {
            Text(action, color = FlagshipBrand, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun FlagshipPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    C1Button(
        text = text,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled
    )
}

@OptIn(ExperimentalLayoutApi::class)
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
        val ws = windowSize
        androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = C1Spacing.pagePadding(ws),
                end = C1Spacing.pagePadding(ws),
                top = C1Spacing.lg,
                bottom = C1Spacing.xxxl
            ),
            verticalArrangement = Arrangement.spacedBy(C1Spacing.lg)
        ) {
            item {
                FlagshipPageTitle(
                    "Attendance",
                    (uiState.profile.courseName.ifBlank { "Your programme" }) + " · Semester " + uiState.profile.semester
                )
            }
            item {
                val statusTone = when {
                    insight.classesCanMissSafely > 0 -> C1Tone.SUCCESS
                    insight.classesMustAttendToRecover > 0 -> C1Tone.WARNING
                    else -> C1Tone.NEUTRAL
                }
                val statusLabel = when {
                    insight.classesCanMissSafely > 0 -> "Safe zone"
                    insight.classesMustAttendToRecover > 0 -> "Recovery needed"
                    else -> "Tracking"
                }
                FlagshipCard(contentPadding = PaddingValues(C1Spacing.lg)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(C1Spacing.xs)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(C1Spacing.sm)
                            ) {
                                Text(
                                    "Overall attendance",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = FlagshipInk
                                )
                                C1StatusChip(text = statusLabel, tone = statusTone)
                            }
                            Text(
                                String.format(Locale.getDefault(), "%.1f%%", insight.currentPercentage),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = FlagshipInk
                            )
                            Text(
                                insight.attended.toString() + " of " + insight.totalConducted + " classes attended",
                                style = MaterialTheme.typography.bodySmall,
                                color = FlagshipMuted
                            )
                            Text(
                                "Required: " + target + "%",
                                style = MaterialTheme.typography.labelSmall,
                                color = FlagshipMuted
                            )
                        }
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(84.dp)) {
                            CircularProgressIndicator(
                                progress = { (insight.currentPercentage / 100.0).toFloat().coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxSize(),
                                strokeWidth = 8.dp,
                                color = FlagshipBrand,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                            Text(
                                if (insight.totalConducted > 0) insight.currentPercentage.toInt().toString() else "—",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = FlagshipInk
                            )
                        }
                    }
                    androidx.compose.material3.HorizontalDivider(
                        modifier = Modifier.padding(vertical = C1Spacing.md),
                        color = FlagshipLine
                    )
                    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(C1Spacing.sm)) {
                        Icon(
                            if (insight.classesCanMissSafely > 0) Icons.Default.CheckCircle else Icons.Default.Info,
                            contentDescription = null,
                            tint = if (insight.classesCanMissSafely > 0) FlagshipGreen else MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                if (insight.classesCanMissSafely > 0) "You can miss " + insight.classesCanMissSafely + " classes" else if (insight.classesMustAttendToRecover > 0) "Attendance recovery required" else "Attendance position is being calculated",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = FlagshipInk
                            )
                            Text(
                                if (insight.classesCanMissSafely > 0) "Your attendance is above the " + target + "% requirement."
                                else if (insight.classesMustAttendToRecover > 0) "Attend " + insight.classesMustAttendToRecover + " consecutive classes to recover to the target."
                                else "Attendance appears here as classes are recorded.",
                                style = MaterialTheme.typography.bodySmall,
                                color = FlagshipMuted
                            )
                        }
                    }
                    Spacer(Modifier.height(C1Spacing.md))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Target attendance",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = FlagshipInk
                        )
                        Text(
                            target.toString() + "%",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = FlagshipBrand
                        )
                    }
                    androidx.compose.material3.Slider(
                        value = target.toFloat(),
                        onValueChange = { onTargetPctChange(it.toInt()) },
                        valueRange = 60f..90f,
                        steps = 5,
                        modifier = Modifier.heightIn(min = C1Height.touch)
                    )
                }
            }
            item {
                FlagshipCard {
                    Text("Attendance simulator", style = MaterialTheme.typography.titleMedium, color = FlagshipInk, fontWeight = FontWeight.Bold)
                    Text("Adjust upcoming classes to project your standing", style = MaterialTheme.typography.bodySmall, color = FlagshipMuted)
                    Spacer(Modifier.height(C1Spacing.md))
                    Row(horizontalArrangement = Arrangement.spacedBy(C1Spacing.md)) {
                        ProjectionStepper("Future missed classes", futureMissed, { futureMissed = (futureMissed - 1).coerceAtLeast(0) }, { futureMissed = (futureMissed + 1).coerceAtMost(30) }, Modifier.weight(1f))
                        ProjectionStepper("Future attended classes", futureAttended, { futureAttended = (futureAttended - 1).coerceAtLeast(0) }, { futureAttended = (futureAttended + 1).coerceAtMost(30) }, Modifier.weight(1f))
                    }
                    androidx.compose.material3.HorizontalDivider(
                        modifier = Modifier.padding(vertical = C1Spacing.md),
                        color = FlagshipLine
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("Projected standing", style = MaterialTheme.typography.labelMedium, color = FlagshipMuted)
                            Text(
                                if (projection.totalConducted == 0) "No classes recorded" else projection.attended.toString() + " of " + projection.totalConducted + " attended",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = FlagshipInk
                            )
                        }
                        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                String.format(Locale.getDefault(), "%.1f%%", projection.currentPercentage),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (projection.currentPercentage >= target) FlagshipGreen else extendedColors.warning
                            )
                            Text(if (projection.currentPercentage >= target) "Above target" else "Below target", style = MaterialTheme.typography.labelSmall, color = FlagshipMuted)
                        }
                    }
                }
            }
            item {
                FlagshipCard {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("4-week rhythm", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = FlagshipInk)
                        Text("Recorded lecture consistency over the last 28 days", style = MaterialTheme.typography.bodySmall, color = FlagshipMuted)
                    }
                    Spacer(Modifier.height(C1Spacing.md))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("M", "T", "W", "T", "F", "S", "S").forEach {
                            Text(it, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall, color = FlagshipMuted)
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
                                    modifier = Modifier.weight(1f).height(36.dp).clip(C1Radius.tile).background(dayBackground),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(if (entries.isEmpty()) "" else date.dayOfMonth.toString(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = dayForeground)
                                }
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                    Spacer(Modifier.height(C1Spacing.sm))
                    Row(horizontalArrangement = Arrangement.spacedBy(C1Spacing.md), verticalAlignment = Alignment.CenterVertically) {
                        RhythmLegend("Present", extendedColors.success)
                        RhythmLegend("Missed", extendedColors.warning)
                        RhythmLegend("No record", MaterialTheme.colorScheme.surfaceVariant)
                    }
                }
            }
            item {
                FlagshipSectionTitle("Subjects", uiState.profile.courseName + " · Semester " + uiState.profile.semester)
            }
            if (uiState.subjectInsights.isEmpty()) {
                item {
                    FlagshipCard {
                        Text("No attendance records yet", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = FlagshipInk)
                        Spacer(Modifier.height(2.dp))
                        Text("Your subjects will appear here when your academic schedule is available.", style = MaterialTheme.typography.bodySmall, color = FlagshipMuted)
                    }
                }
            } else {
                items(uiState.subjectInsights.size) { index ->
                    val subject = uiState.subjectInsights[index]
                    val summary = subject.summary
                    val percent = subject.insight.currentPercentage
                    val healthy = subject.insight.health == AttendanceHealth.SAFE || subject.insight.health == AttendanceHealth.ON_EDGE
                    FlagshipCard(contentPadding = PaddingValues(C1Spacing.md)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(summary.subjectCode, style = MaterialTheme.typography.labelSmall, color = FlagshipBrand, fontWeight = FontWeight.SemiBold)
                                Text(summary.subjectName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = FlagshipInk)
                            }
                            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    String.format(Locale.getDefault(), "%.0f%%", percent),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (healthy) FlagshipBrand else extendedColors.warning
                                )
                                Text(summary.attendedCount.toString() + " of " + summary.totalClasses, style = MaterialTheme.typography.labelSmall, color = FlagshipMuted)
                            }
                        }
                        Spacer(Modifier.height(C1Spacing.sm))
                        LinearProgressIndicator(
                            progress = { (percent / 100.0).toFloat().coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                            color = if (healthy) FlagshipBrand else extendedColors.warning,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Spacer(Modifier.height(C1Spacing.sm))
                        Text(
                            if (subject.insight.classesCanMissSafely > 0) "Can miss " + subject.insight.classesCanMissSafely + " more classes" else if (subject.insight.classesMustAttendToRecover > 0) "Attend " + subject.insight.classesMustAttendToRecover + " more classes to recover" else "On target",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (healthy) FlagshipGreen else extendedColors.warning,
                            fontWeight = FontWeight.Medium
                        )
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(C1Spacing.xs)) {
                            TextButton(
                                onClick = { haptics.performHapticFeedback(HapticFeedbackType.LongPress); onMarkAttendance(summary.subjectId, AttendanceStatus.PRESENT) },
                                modifier = Modifier.heightIn(min = C1Height.touch)
                            ) { Text("Present", style = MaterialTheme.typography.labelLarge, color = FlagshipGreen) }
                            TextButton(
                                onClick = { haptics.performHapticFeedback(HapticFeedbackType.LongPress); onMarkAttendance(summary.subjectId, AttendanceStatus.ABSENT) },
                                modifier = Modifier.heightIn(min = C1Height.touch)
                            ) { Text("Missed", style = MaterialTheme.typography.labelLarge, color = extendedColors.warning) }
                            TextButton(
                                onClick = { onMarkAttendance(summary.subjectId, AttendanceStatus.CANCELLED) },
                                modifier = Modifier.heightIn(min = C1Height.touch)
                            ) { Text("Cancelled", style = MaterialTheme.typography.labelLarge, color = FlagshipMuted) }
                        }
                    }
                }
            }
            item {
                FlagshipCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Attendance history", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = FlagshipInk)
                        TextButton(
                            onClick = { showHistory = !showHistory },
                            modifier = Modifier.heightIn(min = C1Height.touch)
                        ) { Text(if (showHistory) "Hide" else "View all", style = MaterialTheme.typography.labelLarge, color = FlagshipBrand) }
                    }
                    if (showHistory) {
                        val logs = uiState.recentAttendanceLogs.take(20)
                        if (logs.isEmpty()) {
                            Text("No attendance activity has been recorded.", style = MaterialTheme.typography.bodySmall, color = FlagshipMuted)
                        }
                        logs.forEachIndexed { index, log ->
                            if (index > 0) {
                                androidx.compose.material3.HorizontalDivider(color = FlagshipLine)
                            }
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = C1Spacing.sm), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(names[log.subjectId] ?: log.subjectId, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = FlagshipInk, maxLines = 1)
                                    Text(log.dateIso, style = MaterialTheme.typography.labelSmall, color = FlagshipMuted)
                                }
                                C1StatusChip(
                                    text = when (log.status) {
                                        AttendanceStatus.PRESENT -> "Present"
                                        AttendanceStatus.ABSENT -> "Missed"
                                        AttendanceStatus.CANCELLED -> "Cancelled"
                                    },
                                    tone = when (log.status) {
                                        AttendanceStatus.PRESENT -> C1Tone.SUCCESS
                                        AttendanceStatus.ABSENT -> C1Tone.WARNING
                                        AttendanceStatus.CANCELLED -> C1Tone.NEUTRAL
                                    }
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
private fun ProjectionStepper(label: String, value: Int, onMinus: () -> Unit, onPlus: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceContainer, C1Radius.control).padding(C1Spacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(C1Spacing.xs)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = FlagshipMuted, minLines = 2, maxLines = 2, textAlign = TextAlign.Center)
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onMinus,
                modifier = Modifier.size(C1Height.touch).background(MaterialTheme.colorScheme.surface, CircleShape)
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Decrease " + label, tint = FlagshipBrand, modifier = Modifier.size(18.dp))
            }
            Text(value.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = FlagshipInk, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            IconButton(
                onClick = onPlus,
                modifier = Modifier.size(C1Height.touch).background(MaterialTheme.colorScheme.surface, CircleShape)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Increase " + label, tint = FlagshipBrand, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun RhythmLegend(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Text(label, style = MaterialTheme.typography.labelSmall, color = FlagshipMuted)
    }
}

@Composable
fun FlagshipNotificationsScreen(
    viewModel: CommunityViewModel,
    onBack: () -> Unit,
    onNavigate: (targetType: String, targetId: String) -> Unit,
    onOpenProfile: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val ws = windowSize
    var filter by rememberSaveable { mutableStateOf("ALL") }
    LaunchedEffect(Unit) { viewModel.refreshNotifications() }
    val visible = uiState.notifications.filter { item ->
        when (filter) {
            "ACADEMIC" -> item.type.contains("class", true) || item.type.contains("academic", true) || item.type.contains("assignment", true) || item.type.contains("exam", true)
            "NOTICES" -> item.type.contains("notice", true) || item.type.contains("circular", true)
            "DEADLINES" -> item.type.contains("deadline", true) || item.type.contains("assignment", true)
            "REQUESTS" -> item.type.contains("role_request", true)
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
        FlagshipSubHeader(
            title = "Notifications",
            onBack = onBack,
            subtitle = unread.toString() + " unread · " + uiState.notifications.size + " total",
            actions = {
                if (unread > 0) {
                    TextButton(
                        onClick = viewModel::markAllNotificationsRead,
                        modifier = Modifier.heightIn(min = C1Height.touch)
                    ) {
                        Text("Mark all read", style = MaterialTheme.typography.labelLarge, color = FlagshipBrand, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false)
                    }
                }
            }
        )
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = C1Spacing.pagePadding(ws), vertical = C1Spacing.md), verticalArrangement = Arrangement.spacedBy(C1Spacing.md)) {
            androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(C1Spacing.xs)) {
                items(listOf("ALL", "ACADEMIC", "NOTICES", "DEADLINES", "REQUESTS").size) { idx ->
                    val key = listOf("ALL", "ACADEMIC", "NOTICES", "DEADLINES", "REQUESTS")[idx]
                    val label = listOf("All (" + uiState.notifications.size + ")", "Academic & Classes", "Notices & Circulars", "Deadlines", "Role requests")[idx]
                    C1FilterChip(label = label, selected = filter == key, onClick = { filter = key })
                }
            }
        }
        androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = C1Spacing.pagePadding(ws), vertical = C1Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(C1Spacing.sm)
        ) {
            if (visible.isEmpty()) {
                item {
                    FlagshipCard {
                        Text("You're all caught up", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = FlagshipInk)
                        Spacer(Modifier.height(2.dp))
                        Text("New academic updates, notices and deadlines will appear here.", style = MaterialTheme.typography.bodySmall, color = FlagshipMuted)
                    }
                }
            }
            order.forEach { group ->
                val entries = grouped[group].orEmpty()
                if (entries.isNotEmpty()) {
                    item {
                        Row(modifier = Modifier.fillMaxWidth().padding(top = C1Spacing.xs), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(group.uppercase(), style = MaterialTheme.typography.labelSmall, color = FlagshipMuted, fontWeight = FontWeight.Bold)
                            Text(entries.size.toString() + " alerts", style = MaterialTheme.typography.labelSmall, color = FlagshipMuted)
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
        contentPadding = PaddingValues(C1Spacing.md)
    ) {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(C1Spacing.md)) {
            Box(Modifier.size(C1Height.touch).clip(C1Radius.tile).background(iconBg), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(C1Spacing.xs)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(notification.type.replace('_', ' ').ifBlank { "Campus update" }, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Text(
                        Instant.ofEpochMilli(notification.createdAtMillis).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("hh:mm a", Locale.getDefault())),
                        style = MaterialTheme.typography.labelSmall, color = FlagshipMuted
                    )
                }
                Text(notification.title, style = MaterialTheme.typography.titleSmall, color = FlagshipInk, fontWeight = FontWeight.SemiBold)
                if (notification.body.isNotBlank()) Text(notification.body, style = MaterialTheme.typography.bodySmall, color = FlagshipMuted, lineHeight = 18.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (notification.isRead) "Read" else "New · Tap to open", style = MaterialTheme.typography.labelSmall, color = if (notification.isRead) FlagshipMuted else MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.weight(1f))
                    Icon(Icons.Default.ArrowForward, contentDescription = "Open notification", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}
