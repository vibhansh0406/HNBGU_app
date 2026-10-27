package com.campusone.app.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent

object CampusWidgetUpdater {
    fun refreshAllWidgets(context: Context) {
        try {
            val appContext = context.applicationContext
            val manager = AppWidgetManager.getInstance(appContext)

            val nextClassIds = manager.getAppWidgetIds(
                ComponentName(appContext, NextClassWidgetReceiver::class.java)
            )
            if (nextClassIds.isNotEmpty()) {
                val intent = Intent(appContext, NextClassWidgetReceiver::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, nextClassIds)
                }
                appContext.sendBroadcast(intent)
            }

            val attendanceIds = manager.getAppWidgetIds(
                ComponentName(appContext, AttendanceWidgetReceiver::class.java)
            )
            if (attendanceIds.isNotEmpty()) {
                val intent = Intent(appContext, AttendanceWidgetReceiver::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, attendanceIds)
                }
                appContext.sendBroadcast(intent)
            }
        } catch (_: Exception) {
        }
    }
}
