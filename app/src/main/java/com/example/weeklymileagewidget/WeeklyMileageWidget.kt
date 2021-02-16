package com.example.weeklymileagewidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import java.text.DateFormat
import java.util.*


/**
 * Implementation of App Widget functionality.
 */
class WeeklyMileageWidget : AppWidgetProvider() {
    private val mSharedPrefFile =
        "com.example.weeklymileagewidget"
    private val COUNT_KEY = "count"
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // There may be multiple widgets active, so update all of them
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onEnabled(context: Context) {
        // Enter relevant functionality for when the first widget is created
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val prefs = context.getSharedPreferences(
            mSharedPrefFile, 0
        )
        var count = prefs.getInt(COUNT_KEY + appWidgetId, 0)
        count++
        val dateString: String =
            DateFormat.getTimeInstance(DateFormat.SHORT).format(Date())

        val views =
            RemoteViews(context.packageName, R.layout.weekly_mileage_widget)
        views.setTextViewText(R.id.appwidget_id, appWidgetId.toString())

        views.setTextViewText(R.id.appwidget_update,
            context.resources.getString(
                R.string.date_count_format, count, dateString));

        val prefEditor = prefs.edit()
        prefEditor.putInt(COUNT_KEY + appWidgetId, count)
        prefEditor.apply()

        val intentUpdate = Intent(context, WeeklyMileageWidget::class.java)
        intentUpdate.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE

        val idArray = intArrayOf(appWidgetId)
        intentUpdate.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, idArray);

        val pendingUpdate = PendingIntent.getBroadcast(
            context, appWidgetId, intentUpdate,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        views.setOnClickPendingIntent(R.id.widget_layout_id, pendingUpdate);

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}
