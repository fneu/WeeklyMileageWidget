package com.example.weeklymileagewidget

//import com.androidplot.demos.R;


import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Bundle
import android.widget.RemoteViews
import com.androidplot.ui.Anchor
import com.androidplot.ui.HorizontalPositioning
import com.androidplot.ui.Size
import com.androidplot.ui.VerticalPositioning
import com.androidplot.util.PixelUtils
import com.androidplot.xy.*
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

        // androidplot stuff 1
        val plot = XYPlot(context, "Widget Example")

        //val h = context.resources
        //    .getDimension(R.dimen.sample_widget_height).toInt()
        //val w = context.resources
        //    .getDimension(R.dimen.sample_widget_width).toInt()

        val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
        val h = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT).toPx()
        val w = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH).toPx()

        plot.graph.setMargins(0f, 0f, 0f, 0f)
        plot.graph.setPadding(0f, 0f, 0f, 0f)

        plot.graph.position(
            0f, HorizontalPositioning.ABSOLUTE_FROM_LEFT, 0f,
            VerticalPositioning.ABSOLUTE_FROM_TOP, Anchor.LEFT_TOP
        )

        plot.graph.size = Size.FILL

        plot.layoutManager.moveToTop(plot.title)

        plot.graph.setLineLabelEdges(
            XYGraphWidget.Edge.LEFT,
            XYGraphWidget.Edge.BOTTOM
        )
        plot.graph.lineLabelInsets.left = PixelUtils.dpToPix(16f)
        plot.graph.lineLabelInsets.bottom = PixelUtils.dpToPix(4f)
        plot.graph.getLineLabelStyle(XYGraphWidget.Edge.LEFT).paint.color =
            Color.RED
        plot.graph.gridInsets.top = PixelUtils.dpToPix(12f)
        plot.graph.gridInsets.right = PixelUtils.dpToPix(12f)
        plot.graph.gridInsets.left = PixelUtils.dpToPix(36f)
        plot.graph.gridInsets.bottom = PixelUtils.dpToPix(16f)

        plot.measure(w, h)
        plot.layout(0, 0, w, h)

        // Turn the above arrays into XYSeries':
        val series1: XYSeries = SimpleXYSeries(
            listOf(
                1,
                4,
                2,
                8,
                4,
                16,
                8,
                32,
                16,
                64
            ),  // SimpleXYSeries takes a List so turn our array into a List
            SimpleXYSeries.ArrayFormat.Y_VALS_ONLY,  // Y_VALS_ONLY means use the element index as the x value
            "Series1"
        ) // Set the display title of the series


        // same as above

        // same as above
        val series2: XYSeries = SimpleXYSeries(
            listOf(5, 2, 10, 5, 20, 10, 40, 20, 80, 40),
            SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, "Series2"
        )

        // Create a formatter to use for drawing a series using LineAndPointRenderer:

        // Create a formatter to use for drawing a series using LineAndPointRenderer:
        val series1Format = LineAndPointFormatter(
            Color.rgb(0, 200, 0),  // line color
            Color.rgb(0, 100, 0),  // point color
            null, null
        ) // fill color (none)


        // add a new series' to the xyplot:

        // add a new series' to the xyplot:
        plot.addSeries(series1, series1Format)

        // same as above:

        // same as above:
        plot.addSeries(
            series2,
            LineAndPointFormatter(
                Color.rgb(0, 0, 200),
                Color.rgb(0, 0, 100),
                null,
                null
            )
        )


        // reduce the number of range labels


        // reduce the number of range labels
        plot.linesPerRangeLabel = 3
        plot.linesPerDomainLabel = 2

        // hide the legend:

        // hide the legend:
        plot.legend.isVisible = false

        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        plot.draw(Canvas(bitmap))



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
        views.setImageViewBitmap(R.id.imgView, bitmap);


        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}
