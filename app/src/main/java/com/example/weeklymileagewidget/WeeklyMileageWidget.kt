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
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.utils.Transformer
import com.github.mikephil.charting.utils.Utils
import com.github.mikephil.charting.utils.ViewPortHandler
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
        context.getString(R.string.letter_sunday)
    )

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
        val list2 = listOf(6.25, 13.84, 15.87, 30.0, 40.0, 50.0, 59.0)

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
        dataSet1.setCircleColor(argb(100, 255, 255, 255))
        dataSet1.color = argb(100, 255, 255, 255)
        dataSet1.setDrawValues(false)
        dataSet1.setDrawCircleHole(false)
        dataSet1.lineWidth = 4f
        dataSet1.circleRadius = 5f

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
        dataSet2.lineWidth = 5f
        dataSet2.setDrawCircleHole(true)
        dataSet2.circleHoleColor = Color.WHITE
        dataSet2.circleHoleRadius = 5f
        dataSet2.circleRadius = 10f
        val labelColorList = MutableList<Int>(list2.size - 1) {Color.TRANSPARENT}
        labelColorList.add(Color.WHITE)
        dataSet2.setValueTextColors(labelColorList)
        dataSet2.valueTextSize = 24f
        val circleColorList = MutableList<Int>(list2.size - 1) {Color.TRANSPARENT}
        circleColorList.add(argb(80, 255, 255, 255))
        dataSet2.circleColors = circleColorList



        val lineData = LineData(dataSet1, dataSet2)
        chart.data = lineData
        chart.layout(0, 0, w, h)
        chart.setBackgroundColor(Color.TRANSPARENT)
        chart.setDrawGridBackground(false);
        chart.xAxis.valueFormatter = XAxisWeekFormatter(context)
        chart.xAxis.setDrawGridLines(false)
        chart.xAxis.setDrawAxisLine(false)
        chart.xAxis.textColor = Color.WHITE
        chart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        chart.xAxis.axisMinimum = -0.3f
        chart.xAxis.axisMaximum = 6.3f
        chart.xAxis.textSize = 14f
        chart.extraBottomOffset = 10f
        chart.axisRight.isEnabled = false
        chart.axisLeft.setDrawGridLines(false)
        chart.axisLeft.setDrawAxisLine(false)
        chart.axisLeft.axisMinimum = 0f
        val maxVal = (list1+list2+listOf(60.0)).max()!!.toFloat()
        chart.axisLeft.axisMaximum = maxVal
        chart.axisLeft.setDrawLabels(false)
        chart.axisLeft.setDrawZeroLine(true)
        chart.axisLeft.zeroLineColor = Color.WHITE
        chart.axisLeft.yOffset

        val ll = LimitLine(60f, "60")
        ll.lineColor = argb(100, 255, 255, 255)
        ll.lineWidth = 2f
        ll.textColor = argb(100, 255, 255, 255)
        ll.enableDashedLine(25f, 15f, 0f)
        ll.labelPosition = LimitLine.LimitLabelPosition.LEFT_BOTTOM
        ll.textSize = 16f
        chart.axisLeft.addLimitLine(ll)
        chart.legend.isEnabled = false
        chart.description.isEnabled = false

        // make space for value label at the top
        // but only as much as necessary
        val spaceTopNeeded = 36.5f
        val yTopMost = chart.getPixelForValues(
            0f,
            maxVal,
            YAxis.AxisDependency.LEFT
        ).y
        val yTopActual = chart.getPixelForValues(
            (list2.size - 1).toFloat(),
            list2.last().toFloat(),
            YAxis.AxisDependency.LEFT
        ).y
        val spaceTopExtraPixels = yTopActual - yTopMost
        val spaceTopExtraDP = Utils.convertPixelsToDp(spaceTopExtraPixels.toFloat())
        chart.extraTopOffset = spaceTopNeeded-spaceTopExtraDP

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
