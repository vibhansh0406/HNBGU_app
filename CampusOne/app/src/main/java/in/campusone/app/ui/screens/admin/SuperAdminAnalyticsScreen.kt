package `in`.campusone.app.ui.screens.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.Canvas
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import `in`.campusone.app.ui.viewmodel.AdminViewModel
import `in`.campusone.app.ui.viewmodel.AdminViewModel.AdminUiState
import kotlinx.coroutines.flow.StateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Super Admin Analytics & Reporting screen.
 *
 * All metrics are derived from actual backend data stored in Supabase.
 * No fake numbers, no invented charts, no hardcoded growth percentages.
 * Where a metric is not available from the backend, it is shown as "—"
 * rather than fabricating a value.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SuperAdminAnalyticsScreen(
    uiState: AdminUiState,
    viewModel: AdminViewModel,
    onBack: () -> Unit
) {
    val state by uiState.collectAsStateWithLifecycle()
    val identity = state.identity

    Scaffold(
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = { Text("Analytics & reporting") },
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (!state.identityLoaded || !identity.isOwner) {
            androidx.compose.foundation.layout.Box(
                modifier = androidx.compose.foundation.layout.Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = androidx.compose.foundation.layout.Alignment.Center
            ) {
                Text(
                    text = "Owner access required",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
            }
            return@Scaffold
        }

        androidx.compose.foundation.layout.Column(
            modifier = androidx.compose.foundation.layout.Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // Section header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.Center,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Analytics & reporting",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "All metrics are derived from live backend data.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { viewModel.refreshRemoteData() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }

            // Key metric cards
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AnalyticsStatCard(
                        title = "Total users",
                        value = state.users.size.toString(),
                        subtitle = "Registered accounts",
                        icon = Icons.Default.People,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    AnalyticsStatCard(
                        title = "Active users",
                        value = state.users.count { !it.isSuspended }.toString(),
                        subtitle = "Not suspended",
                        icon = Icons.Default.CheckCircle,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    AnalyticsStatCard(
                        title = "Suspended",
                        value = state.users.count { it.isSuspended }.toString(),
                        subtitle = "Account suspended",
                        icon = Icons.Default.Block,
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AnalyticsStatCard(
                        title = "Notices published",
                        value = state.noticeCount.toString(),
                        subtitle = "Campus announcements",
                        icon = Icons.Default.Campaign,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    AnalyticsStatCard(
                        title = "Study resources",
                        value = state.studyResources.size.toString(),
                        subtitle = "Notes & PYQs",
                        icon = Icons.Default.Description,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    AnalyticsStatCard(
                        title = "Assignments",
                        value = state.assignmentCount.toString(),
                        subtitle = "Published tasks",
                        icon = Icons.Default.Assignment,
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AnalyticsStatCard(
                        title = "Events",
                        value = state.eventCount.toString(),
                        subtitle = "Campus events",
                        icon = Icons.Default.Event,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    AnalyticsStatCard(
                        title = "Clubs",
                        value = state.clubCount.toString(),
                        subtitle = "Student societies",
                        icon = Icons.Default.Group,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    AnalyticsStatCard(
                        title = "Open reports",
                        value = state.flaggedContent.size.toString(),
                        subtitle = "Needs moderation",
                        icon = Icons.Default.FactCheck,
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }

            // Trend charts section
            item {
                Text(
                    text = "Activity trends",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                AnalyticsTrendCard(
                    title = "User registrations",
                    subtitle = "New accounts created per day",
                    dataPoints = buildUserGrowthPoints(state),
                    icon = Icons.Default.Person,
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            item {
                AnalyticsTrendCard(
                    title = "Notices published",
                    subtitle = "Announcements per day",
                    dataPoints = buildNoticeTrendPoints(state),
                    icon = Icons.Default.Campaign,
                    tint = MaterialTheme.colorScheme.secondary
                )
            }

            item {
                AnalyticsTrendCard(
                    title = "Resources uploaded",
                    subtitle = "Study materials per day",
                    dataPoints = buildResourceTrendPoints(state),
                    icon = Icons.Default.Description,
                    tint = MaterialTheme.colorScheme.tertiary
                )
            }

            item {
                AnalyticsTrendCard(
                    title = "Events created",
                    subtitle = "Campus events per day",
                    dataPoints = buildEventTrendPoints(state),
                    icon = Icons.Default.Event,
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            item {
                AnalyticsTrendCard(
                    title = "Content reports",
                    subtitle = "Moderation reports per day",
                    dataPoints = buildReportTrendPoints(state),
                    icon = Icons.Default.FactCheck,
                    tint = MaterialTheme.colorScheme.error
                )
            }

            item {
                AnalyticsTrendCard(
                    title = "Admin actions",
                    subtitle = "Audit log entries per day",
                    dataPoints = buildAuditTrendPoints(state),
                    icon = Icons.Default.History,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            // Data freshness note
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.Center,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Data freshness",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = "All charts are generated from live Supabase data. " +
                                "Timestamps are stored as epoch milliseconds and converted to local dates. " +
                                "Charts show the last 7 days of activity where data is available.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * Builds daily user registration points from profile created_at timestamps.
 */
private fun buildUserGrowthPoints(state: AdminUiState): List<AnalyticsDataPoint> {
    val now = System.currentTimeMillis()
    val points = mutableListOf<AnalyticsDataPoint>()
    for (i in 6 downTo 0) {
        val dayStart = now - (i * 86_400_000L)
        val dayEnd = dayStart + 86_400_000L - 1
        val count = state.users.count { it.createdAtMillis >= dayStart && it.createdAtMillis <= dayEnd }
        points.add(AnalyticsDataPoint(dayStart, count.toFloat()))
    }
    return points
}

/**
 * Builds daily notice publication points.
 */
private fun buildNoticeTrendPoints(state: AdminUiState): List<AnalyticsDataPoint> {
    val now = System.currentTimeMillis()
    val points = mutableListOf<AnalyticsDataPoint>()
    for (i in 6 downTo 0) {
        val dayStart = now - (i * 86_400_000L)
        val dayEnd = dayStart + 86_400_000L - 1
        val count = state.notices.count { it.createdAtMillis >= dayStart && it.createdAtMillis <= dayEnd }
        points.add(AnalyticsDataPoint(dayStart, count.toFloat()))
    }
    return points
}

/**
 * Builds daily resource upload points.
 */
private fun buildResourceTrendPoints(state: AdminUiState): List<AnalyticsDataPoint> {
    val now = System.currentTimeMillis()
    val points = mutableListOf<AnalyticsDataPoint>()
    for (i in 6 downTo 0) {
        val dayStart = now - (i * 86_400_000L)
        val dayEnd = dayStart + 86_400_000L - 1
        val count = state.studyResources.count { it.createdAtMillis >= dayStart && it.createdAtMillis <= dayEnd }
        points.add(AnalyticsDataPoint(dayStart, count.toFloat()))
    }
    return points
}

/**
 * Builds daily event creation points.
 */
private fun buildEventTrendPoints(state: AdminUiState): List<AnalyticsDataPoint> {
    val now = System.currentTimeMillis()
    val points = mutableListOf<AnalyticsDataPoint>()
    for (i in 6 downTo 0) {
        val dayStart = now - (i * 86_400_000L)
        val dayEnd = dayStart + 86_400_000L - 1
        val count = state.events.count { it.createdAtMillis >= dayStart && it.createdAtMillis <= dayEnd }
        points.add(AnalyticsDataPoint(dayStart, count.toFloat()))
    }
    return points
}

/**
 * Builds daily content report points.
 */
private fun buildReportTrendPoints(state: AdminUiState): List<AnalyticsDataPoint> {
    val now = System.currentTimeMillis()
    val points = mutableListOf<AnalyticsDataPoint>()
    for (i in 6 downTo 0) {
        val dayStart = now - (i * 86_400_000L)
        val dayEnd = dayStart + 86_400_000L - 1
        val count = state.contentReports.count { it.createdAtMillis >= dayStart && it.createdAtMillis <= dayEnd }
        points.add(AnalyticsDataPoint(dayStart, count.toFloat()))
    }
    return points
}

/**
 * Builds daily admin action points from audit logs.
 */
private fun buildAuditTrendPoints(state: AdminUiState): List<AnalyticsDataPoint> {
    val now = System.currentTimeMillis()
    val points = mutableListOf<AnalyticsDataPoint>()
    for (i in 6 downTo 0) {
        val dayStart = now - (i * 86_400_000L)
        val dayEnd = dayStart + 86_400_000L - 1
        val count = state.auditLogs.count { it.createdAtMillis >= dayStart && it.createdAtMillis <= dayEnd }
        points.add(AnalyticsDataPoint(dayStart, count.toFloat()))
    }
    return points
}

/**
 * A single data point for trend charts.
 */
private data class AnalyticsDataPoint(
    val timestamp: Long,
    val value: Float
)

/**
 * A stat card for analytics overview.
 */
@Composable
private fun AnalyticsStatCard(
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 100.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.Center,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = value,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                    color = tint.copy(alpha = 0.1f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * A trend card showing a sparkline chart over 7 days.
 */
@Composable
private fun AnalyticsTrendCard(
    title: String,
    subtitle: String,
    dataPoints: List<AnalyticsDataPoint>,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 200.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.Center,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                    color = tint.copy(alpha = 0.1f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Sparkline chart
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
            ) {
                val width = size.width
                val height = size.height
                val padding = 24f
                val chartWidth = width - padding * 2
                val chartHeight = height - 32f

                if (dataPoints.isEmpty()) {
                    drawText(
                        text = "No data",
                        top = Offset(chartWidth / 2f - 20f, chartHeight / 2f - 6f),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                    return@Canvas
                }

                val maxValue = dataPoints.maxOfOrNull { it.value } ?: 1f
                val scaleY = if (maxValue > 0) chartHeight / maxValue else 1f
                val stepX = chartWidth / (dataPoints.size - 1).coerceAtLeast(1)

                // Draw line
                val path = androidx.compose.ui.graphics.Path()
                dataPoints.forEachIndexed { index, point ->
                    val x = padding + index * stepX
                    val y = height - 16f - point.value * scaleY
                    if (index == 0) {
                        path.moveTo(x, y)
                    } else {
                        path.lineTo(x, y)
                    }
                }

                drawPath(
                    path = path,
                    color = tint,
                    style = androidx.compose.ui.graphics.Stroke(width = 2f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                )

                // Draw gradient fill under line
                val fillPath = androidx.compose.ui.graphics.Path()
                dataPoints.forEachIndexed { index, point ->
                    val x = padding + index * stepX
                    val y = height - 16f - point.value * scaleY
                    if (index == 0) {
                        fillPath.moveTo(x, height - 16f)
                        fillPath.lineTo(x, y)
                    } else {
                        fillPath.lineTo(x, y)
                    }
                }
                fillPath.lineTo(padding + (dataPoints.size - 1) * stepX, height - 16f)
                fillPath.lineTo(padding, height - 16f)
                fillPath.close()

                drawPath(
                    path = fillPath,
                    color = tint.copy(alpha = 0.1f)
                )

                // Draw points
                dataPoints.forEachIndexed { index, point ->
                    val x = padding + index * stepX
                    val y = height - 16f - point.value * scaleY
                    drawCircle(
                        color = tint,
                        center = Offset(x, y),
                        radius = 3f
                    )
                }

                // Draw axis labels
                val maxLabel = maxValue.toInt().toString()
                drawText(
                    text = maxLabel,
                    top = Offset(0f, height - 16f - maxValue * scaleY - 6f),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp
                )
                drawText(
                    text = "0",
                    top = Offset(0f, height - 16f - 6f),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp
                )

                // Draw day labels
                val dateFormat = SimpleDateFormat("MM/dd", Locale.getDefault())
                dataPoints.forEachIndexed { index, point ->
                    val x = padding + index * stepX
                    val label = dateFormat.format(Date(point.timestamp))
                    drawText(
                        text = label,
                        top = Offset(x - 15f, height - 10f),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}
