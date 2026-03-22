package com.taskify.app.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.taskify.app.R

/**
 * Home screen widget showing today's tasks.
 *
 * For Jetpack Glance (modern Compose-based widgets), migrate this to
 * GlanceAppWidget + GlanceAppWidgetReceiver. The RemoteViews approach
 * used here is compatible with all Android versions.
 */
class TaskifyWidgetReceiver : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { appWidgetId ->
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_taskify)
            // In a full implementation: populate with today's tasks via RemoteViews adapter
            // and set up a PendingIntent for the quick-add button
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
