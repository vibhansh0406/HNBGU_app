package com.campusone.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.campusone.app.MainActivity
import com.campusone.app.R
import com.campusone.app.data.local.CampusOneDatabase
import com.campusone.app.domain.model.TimetableSlot
import com.campusone.app.domain.usecase.TimetableEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

class NextClassWidgetReceiver : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = CampusOneDatabase.getInstance(context)
                val entities = db.coreDao().getAllTimetableSlotsOnce()
                val slots = entities.map {
                    TimetableSlot(
                        id = it.id,
                        subjectId = it.subjectId,
                        subjectName = it.subjectName,
                        subjectCode = it.subjectCode,
                        dayOfWeek = it.dayOfWeek,
                        startTime = it.startTime,
                        endTime = it.endTime,
                        room = it.room,
                        teacherName = it.teacherName,
                        semester = it.semester,
                        section = it.section,
                        colorHex = it.colorHex
                    )
                }
                val todayDow = LocalDate.now().dayOfWeek.value
                val nowMinutes = LocalTime.now().let { it.hour * 60 + it.minute }
                val nextSlot = TimetableEngine.findNextClassToday(slots, todayDow, nowMinutes)

                for (widgetId in appWidgetIds) {
                    val views = RemoteViews(context.packageName, R.layout.widget_next_class)
                    views.setTextViewText(R.id.widget_header, context.getString(R.string.widget_title))
                    if (nextSlot != null) {
                        views.setTextViewText(
                            R.id.widget_subject,
                            "${nextSlot.subjectCode} • ${nextSlot.subjectName}"
                        )
                        views.setTextViewText(
                            R.id.widget_meta,
                            "${nextSlot.startTime} - ${nextSlot.endTime} • ${nextSlot.room}"
                        )
                    } else {
                        views.setTextViewText(
                            R.id.widget_subject,
                            context.getString(R.string.no_classes_today)
                        )
                        views.setTextViewText(
                            R.id.widget_meta,
                            context.getString(R.string.widget_tap_to_open)
                        )
                    }

                    val launchIntent = Intent(context, MainActivity::class.java).apply {
                        putExtra("open_route", "timetable")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                    val pi = PendingIntent.getActivity(
                        context,
                        101,
                        launchIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_root, pi)
                    appWidgetManager.updateAppWidget(widgetId, views)
                }
            } catch (_: Exception) {
            } finally {
                pendingResult.finish()
            }
        }
    }
}
