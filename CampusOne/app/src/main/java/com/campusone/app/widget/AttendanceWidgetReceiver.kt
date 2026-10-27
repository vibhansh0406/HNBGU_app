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
import com.campusone.app.domain.usecase.AttendanceCalculator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AttendanceWidgetReceiver : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = CampusOneDatabase.getInstance(context)
                val subjects = db.coreDao().getAllSubjectsOnce()
                val totalAttended = subjects.sumOf { it.attendedCount }
                val totalConducted = subjects.sumOf { it.totalClassesCount }
                val overallAdvice = AttendanceCalculator.evaluate(totalAttended, totalConducted, 75)

                val lowestSubject = subjects
                    .filter { it.totalClassesCount > 0 }
                    .minByOrNull {
                        (it.attendedCount.toDouble() * 100.0) / it.totalClassesCount.toDouble()
                    }

                for (widgetId in appWidgetIds) {
                    val views = RemoteViews(context.packageName, R.layout.widget_attendance)
                    views.setTextViewText(
                        R.id.widget_att_header,
                        context.getString(R.string.widget_attendance_title)
                    )
                    if (subjects.isEmpty() || totalConducted == 0) {
                        views.setTextViewText(
                            R.id.widget_att_overall,
                            context.getString(R.string.attendance_no_classes_logged)
                        )
                        views.setTextViewText(
                            R.id.widget_att_lowest,
                            context.getString(R.string.widget_tap_to_open)
                        )
                    } else {
                        views.setTextViewText(
                            R.id.widget_att_overall,
                            String.format("Overall: %.1f%% (%d/%d)", overallAdvice.currentPercentage, totalAttended, totalConducted)
                        )
                        if (lowestSubject != null) {
                            val lowAdv = AttendanceCalculator.evaluate(
                                lowestSubject.attendedCount,
                                lowestSubject.totalClassesCount,
                                lowestSubject.targetPercentage
                            )
                            views.setTextViewText(
                                R.id.widget_att_lowest,
                                String.format("Lowest: %s (%.1f%%)", lowestSubject.code, lowAdv.currentPercentage)
                            )
                        } else {
                            views.setTextViewText(
                                R.id.widget_att_lowest,
                                context.getString(R.string.widget_tap_to_open)
                            )
                        }
                    }

                    val launchIntent = Intent(context, MainActivity::class.java).apply {
                        putExtra("open_route", "attendance")
                        if (lowestSubject != null) {
                            putExtra("open_subject_id", lowestSubject.id)
                        }
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                    val pi = PendingIntent.getActivity(
                        context,
                        102,
                        launchIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_att_root, pi)
                    appWidgetManager.updateAppWidget(widgetId, views)
                }
            } catch (_: Exception) {
            } finally {
                pendingResult.finish()
            }
        }
    }
}
