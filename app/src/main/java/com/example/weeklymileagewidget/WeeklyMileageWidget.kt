package com.example.weeklymileagewidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Color.argb
import android.os.Bundle
import android.widget.RemoteViews
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.*
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import java.text.DateFormat
import java.util.*


// convert widget size in dp to bitmap px
fun Int.toPx(): Int = (this * Resources.getSystem().displayMetrics.density).toInt()

class XAxisWeekFormatter(context: Context) : ValueFormatter() {
    private val days = arrayOf(
        context.getString(R.string.letter_monday),
        context.getString(R.string.letter_tuesday),
        context.getString(R.string.letter_wednesday),
        context.getString(R.string.letter_thursday),
        context.getString(R.string.letter_friday),
        context.getString(R.string.letter_saturday),
        context.getString(R.string.letter_sunday))

    override fun getAxisLabel(value: Float, axis: AxisBase?): String {
        return days.getOrNull(value.toInt()) ?: value.toString()
    }
}

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
        val list2 = listOf(6.25)//, 13.84, 15.87, 30.0)

        val chart = LineChart(context)


        val entries1 = ArrayList<Entry>()
        for (i in list1.indices) entries1.add(
            Entry(
                i.toFloat(),
                list1[i].toFloat()
            )
        )
        val dataSet1 = LineDataSet(entries1, "a")
        dataSet1.axisDependency = YAxis.AxisDependency.LEFT
        dataSet1.setCircleColor(argb(130, 255, 255, 255))
        dataSet1.color = argb(130, 255, 255, 255)
        dataSet1.setDrawValues(false)
        dataSet1.setDrawCircleHole(false)
        dataSet1.lineWidth = 2f

        val entries2 = ArrayList<Entry>()
        for (i in list2.indices) entries2.add(
            Entry(
                i.toFloat(),
                list2[i].toFloat()
            )
        )
        val dataSet2 = LineDataSet(entries2, "b")
        dataSet2.axisDependency = YAxis.AxisDependency.LEFT
        dataSet2.color = Color.WHITE
        dataSet2.setCircleColor(Color.WHITE)
        dataSet2.setDrawCircleHole(false)
        dataSet2.lineWidth = 3f
        dataSet2.setDrawCircleHole(true)
        dataSet2.circleHoleColor = Color.WHITE
        dataSet2.circleHoleRadius = 3f
        dataSet2.circleRadius = 8f
        val labelColorList = MutableList<Int>(list2.size-1) {Color.TRANSPARENT}
        labelColorList.add(Color.WHITE)
        dataSet2.setValueTextColors(labelColorList)
        dataSet2.valueTextSize = 16f
        val circleColorList = MutableList<Int>(list2.size-1) {Color.TRANSPARENT}
        circleColorList.add(argb(120, 255, 255,255))
        dataSet2.circleColors = circleColorList



        val lineData = LineData(dataSet1, dataSet2)

        chart.data = lineData
        chart.layout(0, 0, w, h)
        chart.setBackgroundColor(Color.TRANSPARENT)
        chart.setDrawGridBackground(false);
        chart.xAxis.valueFormatter = XAxisWeekFormatter(context)
        chart.xAxis.setDrawGridLines(false)
        chart.xAxis.setDrawAxisLine(true)
        chart.xAxis.axisLineColor = Color.WHITE
        chart.xAxis.textColor = Color.WHITE
        chart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        chart.xAxis.axisMinimum = -0.3f
        chart.xAxis.axisMaximum = 6.3f
        chart.xAxis.textSize = 12f
        chart.axisRight.isEnabled = false
        chart.axisLeft.setDrawGridLines(false)
        chart.axisLeft.setDrawAxisLine(false)
        chart.axisLeft.axisMinimum = 0f
        chart.axisLeft.setDrawLabels(false)


        val ll = LimitLine(50f, "50")
        ll.lineColor = argb(80, 255, 255,255)
        ll.lineWidth = 2f
        ll.textColor = argb(80, 255, 255,255)
        ll.enableDashedLine(25f, 15f, 0f)
        ll.labelPosition = LimitLine.LimitLabelPosition.LEFT_BOTTOM
        ll.textSize = 12f
        chart.axisLeft.addLimitLine(ll)
        chart.legend.isEnabled = false
        chart.description.isEnabled = false
        chart.invalidate()

        // chart.getChartBitmap() returns RGB_565 but we need ARGB_8888
        // Define a bitmap with the same size as the view
        val returnedBitmap =
            Bitmap.createBitmap(
                chart.width,
                chart.height,
                Bitmap.Config.ARGB_8888
            )
        // Bind a canvas to it
        val canvas = Canvas(returnedBitmap)
        chart.draw(canvas)

        val views =
            RemoteViews(context.packageName, R.layout.weekly_mileage_widget)

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
        views.setImageViewBitmap(R.id.imgView, returnedBitmap);


        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}
