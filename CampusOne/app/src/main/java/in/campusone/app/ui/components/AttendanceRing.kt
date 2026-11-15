package `in`.campusone.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import `in`.campusone.app.domain.usecase.AttendanceHealth
import `in`.campusone.app.ui.theme.extendedColors
import java.util.Locale

@Composable
fun AnimatedAttendanceRing(
    percentage: Double,
    health: AttendanceHealth,
    size: Dp = 84.dp,
    strokeWidth: Dp = 9.dp,
    modifier: Modifier = Modifier,
    showLabel: Boolean = true,
    label: String? = null
) {
    val clamped = (percentage.toFloat() / 100f).coerceIn(0f, 1f)
    val animatedSweep by animateFloatAsState(
        targetValue = clamped * 360f,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "attendanceRingSweep"
    )

    val extended = extendedColors
    val ringColor = when (health) {
        AttendanceHealth.SAFE -> extended.success
        AttendanceHealth.ON_EDGE, AttendanceHealth.WARNING -> extended.warning
        AttendanceHealth.CRITICAL -> MaterialTheme.colorScheme.error
        AttendanceHealth.NO_DATA -> MaterialTheme.colorScheme.outline
    }
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val formattedPct = if (health == AttendanceHealth.NO_DATA) "--" else String.format(Locale.US, "%.0f%%", percentage)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .semantics {
                contentDescription = "Attendance percentage $formattedPct, status ${health.name}"
            }
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val strokePx = strokeWidth.toPx()
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
            drawArc(
                color = ringColor,
                startAngle = -90f,
                sweepAngle = animatedSweep,
                useCenter = false,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
        }
        if (showLabel) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(2.dp)) {
                Text(
                    text = formattedPct,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                label?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/** Compact attendance ring for list items and smaller spaces. */
@Composable
fun CompactAttendanceRing(
    percentage: Double,
    health: AttendanceHealth,
    size: Dp = 48.dp,
    strokeWidth: Dp = 5.dp,
    modifier: Modifier = Modifier
) {
    val clamped = (percentage.toFloat() / 100f).coerceIn(0f, 1f)
    val animatedSweep by animateFloatAsState(
        targetValue = clamped * 360f,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "compactAttendanceRingSweep"
    )

    val extended = extendedColors
    val ringColor = when (health) {
        AttendanceHealth.SAFE -> extended.success
        AttendanceHealth.ON_EDGE, AttendanceHealth.WARNING -> extended.warning
        AttendanceHealth.CRITICAL -> MaterialTheme.colorScheme.error
        AttendanceHealth.NO_DATA -> MaterialTheme.colorScheme.outline
    }
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val formattedPct = if (health == AttendanceHealth.NO_DATA) "--" else String.format(Locale.US, "%.0f%%", percentage)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .semantics {
                contentDescription = "Attendance $formattedPct"
            }
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val strokePx = strokeWidth.toPx()
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
            drawArc(
                color = ringColor,
                startAngle = -90f,
                sweepAngle = animatedSweep,
                useCenter = false,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
        }
        Text(
            text = formattedPct,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}