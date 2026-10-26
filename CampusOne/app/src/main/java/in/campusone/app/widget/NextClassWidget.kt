package `in`.campusone.app.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import `in`.campusone.app.MainActivity
import `in`.campusone.app.data.local.HnbguSeedData
import `in`.campusone.app.domain.model.TimetableSlot
import `in`.campusone.app.domain.usecase.TimetableEngine
import java.time.LocalDateTime

class NextClassWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // Offline-first fallback using HNBGU default timetable or cached slots
        val slots = HnbguSeedData.defaultTimetable().map {
            TimetableSlot(
                id = it.id,
                subjectId = it.subjectId,
                subjectName = it.subjectName,
                subjectCode = it.subjectCode,
                teacherName = it.teacherName,
                roomNumber = it.roomNumber,
                dayOfWeek = it.dayOfWeek,
                startTime = it.startTime,
                endTime = it.endTime,
                isElective = it.isElective
            )
        }
        val nextResult = TimetableEngine.findNextOrOngoingClass(slots, LocalDateTime.now())

        provideContent {
            GlanceTheme {
                WidgetContent(nextResult = nextResult)
            }
        }
    }

    @Composable
    private fun WidgetContent(
        nextResult: `in`.campusone.app.domain.usecase.NextClassResult?
    ) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.primaryContainer)
                .clickable(actionStartActivity<MainActivity>())
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(modifier = GlanceModifier.fillMaxWidth()) {
                Text(
                    text = if (nextResult?.isOngoing == true) "CAMPUSONE • ONGOING" else "CAMPUSONE • NEXT CLASS",
                    style = TextStyle(
                        color = GlanceTheme.colors.onPrimaryContainer,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
            Spacer(modifier = GlanceModifier.height(6.dp))
            if (nextResult != null) {
                Text(
                    text = nextResult.slot.subjectName,
                    style = TextStyle(
                        color = GlanceTheme.colors.onPrimaryContainer,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = GlanceModifier.height(4.dp))
                Text(
                    text = "${nextResult.slot.startTime} - ${nextResult.slot.endTime} • ${nextResult.slot.roomNumber}",
                    style = TextStyle(
                        color = GlanceTheme.colors.onPrimaryContainer,
                        fontSize = 13.sp
                    )
                )
            } else {
                Text(
                    text = "No more classes scheduled today! 🎉",
                    style = TextStyle(
                        color = GlanceTheme.colors.onPrimaryContainer,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }
    }
}

class NextClassWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NextClassWidget()
}
