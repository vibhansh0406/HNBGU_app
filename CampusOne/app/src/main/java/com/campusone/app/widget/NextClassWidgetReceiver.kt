package com.campusone.app.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import com.campusone.app.R

class NextClassWidgetReceiver : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (widgetId in appWidgetIds) {
            try {
                val views = RemoteViews(context.packageName, R.layout.widget_next_class)
                views.setTextViewText(R.id.widget_header, context.getString(R.string.widget_title))
                views.setTextViewText(R.id.widget_subject, "CSE-401 • Design & Analysis of Algorithms")
                views.setTextViewText(R.id.widget_meta, "09:30 - 10:30 • CSE Block 102")
                appWidgetManager.updateAppWidget(widgetId, views)
            } catch (_: Exception) {
            }
        }
    }
}
