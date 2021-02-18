package com.example.weeklymileagewidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import android.widget.RemoteViews
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import java.text.DateFormat
import java.util.*


// convert widget size in dp to bitmap px
fun Int.toPx(): Int = (this * Resources.getSystem().displayMetrics.density).toInt()


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

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        widgetInfo: Bundle
    ) {
        updateAppWidget(context, appWidgetManager, appWidgetId)
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

        val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
        val h = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT).toPx()
        val w = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH).toPx()
        val list1 = listOf(4.65, 10.4, 20.41, 29.94, 38.45, 47.56, 56.23)
        val list2 = listOf(6.25, 13.84, 15.87)

        val chart = LineChart(context)


        val entries1 = ArrayList<Entry>()
        entries1.add(Entry(0f, 0f))
        for (i in list1.indices) entries1.add(Entry(i.toFloat()+1, list1[i].toFloat()))
        val dataSet1 = LineDataSet(entries1, "a")
        dataSet1.axisDependency = YAxis.AxisDependency.LEFT


        val entries2 = ArrayList<Entry>()
        entries2.add(Entry(0f, 0f))
        for (i in list2.indices) entries2.add(Entry(i.toFloat()+1, list2[i].toFloat()))
        val dataSet2 = LineDataSet(entries2, "b")
        dataSet2.axisDependency = YAxis.AxisDependency.LEFT


        val lineData = LineData(dataSet1, dataSet2)
        chart.data = lineData
        chart.layout(0, 0, w, h)

        val chartBitmap = chart.chartBitmap

        val views =
            RemoteViews(context.packageName, R.layout.weekly_mileage_widget)

        views.setTextViewText(R.id.appwidget_update, w.toString());

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

        // Androidplot stuff
        views.setImageViewBitmap(R.id.imgView, chartBitmap);


        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}
