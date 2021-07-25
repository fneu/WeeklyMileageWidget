package com.example.weeklymileagewidget

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Color.argb
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import android.widget.TextView
import androidx.preference.PreferenceManager
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.utils.Utils
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit


// convert widget size in dp to bitmap px
fun Int.toPx(): Int = (this * Resources.getSystem().displayMetrics.density).toInt()

class XAxisWeekFormatter(context: Context, prefs: SharedPreferences) : ValueFormatter() {
    private val days = when {
        prefs.getString("timeFrame", "week") == "month" -> {
            arrayOf(
                "1", "2","3","4","5","6","7","8","9",
                "10","11", "12","13","14","15","16","17","18","19",
                "20","21", "22","23","24","25","26","27","28","29",
                "30", "31"
            )
        }
        prefs.getString("weekStart", "monday") == "sunday" -> {
            arrayOf(
                context.getString(R.string.letter_sunday),
                context.getString(R.string.letter_monday),
                context.getString(R.string.letter_tuesday),
                context.getString(R.string.letter_wednesday),
                context.getString(R.string.letter_thursday),
                context.getString(R.string.letter_friday),
                context.getString(R.string.letter_saturday)
            )
        }
        else -> {
            arrayOf(
                context.getString(R.string.letter_monday),
                context.getString(R.string.letter_tuesday),
                context.getString(R.string.letter_wednesday),
                context.getString(R.string.letter_thursday),
                context.getString(R.string.letter_friday),
                context.getString(R.string.letter_saturday),
                context.getString(R.string.letter_sunday)
            )
        }
    }

    override fun getAxisLabel(value: Float, axis: AxisBase?): String {
        return days.getOrNull(value.toInt()) ?: value.toString()
    }
}

/**
 * Implementation of App Widget functionality.
 */
class WeeklyMileageWidget : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (context.getString(R.string.updateIntentString) == intent.action)
        {
            externalUpdate(context)
        }
    }

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

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        widgetInfo: Bundle
    ) {
        updateAppWidget(context, appWidgetManager, appWidgetId)
    }

    private fun externalUpdate(context: Context){
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(
            ComponentName(
                context,
                this.javaClass
            )
        )
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {

        val prefs: SharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(context)

        // check and optionally refresh access token
        val requestQueue: RequestQueue = Volley.newRequestQueue(context)

        when {
            prefs.getString("strava_access_token", "") == "" -> {
                plotOnlyStravaMessage(prefs, context, appWidgetManager, appWidgetId)
            }
            prefs.getString("strava_expires_at", "0")!!.toInt() < TimeUnit.MILLISECONDS.toSeconds(
                System.currentTimeMillis()
            ) -> {

                val jsonObjectRequest = object : JsonObjectRequest(
                    Request.Method.POST,
                    "https://www.strava.com/oauth/token",
                    null,
                    Response.Listener { response ->
                        val token_type = response["token_type"]
                        val expires_at = response["expires_at"]
                        val expires_in = response["expires_in"]
                        val refresh_token = response["refresh_token"]
                        val access_token = response["access_token"]

                        val editor = prefs.edit()
                        editor.putString("strava_access_token", access_token.toString())
                        editor.putString("strava_refresh_token", refresh_token.toString())
                        editor.putString("strava_expires_at", expires_at.toString())
                        editor.commit()

                        fetchDataAndPlot(
                            prefs,
                            context,
                            appWidgetManager,
                            appWidgetId,
                            requestQueue
                        )
                    },
                    Response.ErrorListener { // Do something when error occurred

                    }
                ) {
                    override fun getBody(): ByteArray {
                        val parameters = HashMap<String, String>()
                        parameters["client_id"] = BuildConfig.STRAVA_KEY
                        parameters["client_secret"] = BuildConfig.STRAVA_SECRET
                        parameters["refresh_token"] = prefs.getString("strava_refresh_token", "").toString()
                        parameters["grant_type"] = "refresh_token"

                        return JSONObject(parameters.toString()).toString().toByteArray()
                    }
                }
                requestQueue.add(jsonObjectRequest)

            }
            else -> {
                fetchDataAndPlot(prefs, context, appWidgetManager, appWidgetId, requestQueue)
            }
        }

    }

    private fun getEarliestDate(prefs: SharedPreferences): Long {
        val cStart = Calendar.getInstance()
        if (prefs.getString("timeFrame", "week") == "month") {
            cStart.add(Calendar.MONTH, -1) // go back one month
            cStart.set(Calendar.DAY_OF_MONTH, 1) // set date to first day of that month
            cStart.set(Calendar.HOUR, 0)
            cStart.set(Calendar.MINUTE, 0)
            cStart.set(Calendar.SECOND, 0)
            cStart.set(Calendar.MILLISECOND, 0)
            cStart.add(Calendar.DATE, -1) // extra day to prevent off-by-ones
            return TimeUnit.MILLISECONDS.toSeconds(cStart.timeInMillis)
        } else {
            if (prefs.getString("weekStart", "monday") == "sunday") {
                cStart.firstDayOfWeek = Calendar.SUNDAY  // make sunday first of week
                cStart.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY) // set date to sunday this week
            } else {
                cStart.firstDayOfWeek = Calendar.MONDAY  // make monday first of week
                cStart.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY) // set date to monday this week
            }
            cStart.set(Calendar.HOUR, 0)
            cStart.set(Calendar.MINUTE, 0)
            cStart.set(Calendar.SECOND, 0)
            cStart.set(Calendar.MILLISECOND, 0)
            cStart.add(Calendar.DATE, -7) // go to previous week
            cStart.add(Calendar.DATE, -1) // extra day to prevent off-by-ones
            return TimeUnit.MILLISECONDS.toSeconds(cStart.timeInMillis)
        }
    }

    private fun fetchDataAndPlot(
        prefs: SharedPreferences,
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        requestQueue: RequestQueue
    ){

        val earliestPossibleActivity = getEarliestDate(prefs)
        val url = Uri.parse("https://www.strava.com/api/v3/athlete/activities")
            .buildUpon()
            .appendQueryParameter("per_page", "100")
            .appendQueryParameter("after", earliestPossibleActivity.toString())
            .build()
            .toString()

        val jsonArrayRequest = @SuppressLint("SimpleDateFormat")
        object : JsonArrayRequest(
            Request.Method.GET,
            url,
            null,
            Response.Listener { response ->
                val format = SimpleDateFormat("yyyy-MM-dd'T'kk:mm:ss'Z'")  // 2018-04-30T12:35:51Z"
                //format.timeZone = TimeZone.getTimeZone("UTC");

                val cToday = Calendar.getInstance()
                val dayOfWeek = cToday.get(Calendar.DAY_OF_WEEK)
                //calculate time stuffs
                val c = Calendar.getInstance()
                if (prefs.getString("timeFrame", "week") == "month") {
                    c.add(Calendar.MONTH, -1) // go back one month
                    c.set(Calendar.DAY_OF_MONTH, 1) // set date to first day of that month
                } else {
                    if (prefs.getString("weekStart", "monday") == "sunday") {
                        c.firstDayOfWeek = Calendar.SUNDAY  // make sunday first of week
                        c.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY) // set date to sunday this week
                    } else {
                        c.firstDayOfWeek = Calendar.MONDAY  // make monday first of week
                        c.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY) // set date to monday this week
                    }
                    c.add(Calendar.DATE, -7) // go to previous week
                }

                val weekDayToWeekLengthMap = if (prefs.getString("weekStart", "monday") == "sunday") {
                    listOf(0, 1, 2, 3, 4, 5, 6, 7)
                } else {
                    listOf(0, 7, 1, 2, 3, 4, 5, 6)
                }
                val lengthOfThisWeek = weekDayToWeekLengthMap[dayOfWeek]

                val list1 = if (prefs.getString("timeFrame", "week") == "month") {
                    DoubleArray(c.getActualMaximum(Calendar.DAY_OF_MONTH))
                } else {
                    DoubleArray(7)
                }

                val list2 = if (prefs.getString("timeFrame", "week") == "month") {
                    DoubleArray(cToday.get(Calendar.DAY_OF_MONTH))
                } else {
                    DoubleArray(lengthOfThisWeek)
                }

                c.add(Calendar.DATE, -1) // go one back so we can advance in each iteration
                for (i in list1.indices) {
                    c.add(Calendar.DATE, 1) // next day
                    val dateOfTheDay = format.format(c.time).slice(0..9)
                    for (ii in 0 until response.length()) {
                        val activity = response.getJSONObject(ii)
                        if (prefs.getString("activities", "all") == "runs" && activity.getString("type") !in setOf("Run", "VirtualRun")) {
                            continue
                        }
                        if (prefs.getString("activities", "all") == "rides" && activity.getString("type") !in setOf("Ride", "VirtualRide")) {
                            continue
                        }

                        val dist = if (prefs.getString("units", "kilometers") == "miles") {
                            activity.getDouble("distance") / 1609.34
                        } else {
                            activity.getDouble("distance") / 1000
                        }
                        val date = activity.getString("start_date_local").slice(0..9)
                        if (date == dateOfTheDay) {
                            for (iii in i until list1.size) {
                                list1[iii] += dist
                            }
                        }
                    }
                }

                for (i in list2.indices) {
                    c.add(Calendar.DATE, 1) // next day
                    val dateOfTheDay = format.format(c.time).slice(0..9)
                    for (ii in 0 until response.length()) {
                        val activity = response.getJSONObject(ii)
                        if (prefs.getString("activities", "all") == "runs" && activity.getString("type") !in setOf("Run", "VirtualRun")) {
                            continue
                        }
                        if (prefs.getString("activities", "all") == "rides" && activity.getString("type") !in setOf("Ride", "VirtualRide")) {
                            continue
                        }

                        val dist = if (prefs.getString("units", "kilometers") == "miles") {
                            activity.getDouble("distance") / 1609.34
                        } else {
                            activity.getDouble("distance") / 1000
                        }
                        val date = activity.getString("start_date_local").slice(0..9)
                        if (date == dateOfTheDay) {
                            for (iii in i until list2.size) {
                                list2[iii] += dist
                            }
                        }
                    }
                }

                plotStuff(prefs, context, appWidgetManager, appWidgetId, list1, list2)

            },
            Response.ErrorListener { // Do something when error occurred
                val e = it
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                val token = prefs.getString("strava_access_token", "").toString()
                headers["Authorization"] = "Bearer $token"
                return headers
            }
        }
        requestQueue.add(jsonArrayRequest)
    }

    private fun plotStuff(
        prefs: SharedPreferences,
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        list1: DoubleArray,
        list2: DoubleArray
    ){
        val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
        val h = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT).toPx()
        val w = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH).toPx()


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
        dataSet1.setCircleColor(prefs.getInt("color_last_week", 0))
        dataSet1.color = prefs.getInt("color_last_week", 0)
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
        dataSet2.color = prefs.getInt("color_this_week", 0)
        dataSet2.setCircleColor(prefs.getInt("color_this_week", 0))
        dataSet2.setDrawCircleHole(false)
        dataSet2.lineWidth = 5f
        dataSet2.setDrawCircleHole(true)
        dataSet2.circleHoleColor = prefs.getInt("color_this_week", 0)
        dataSet2.circleHoleRadius = 5f
        dataSet2.circleRadius = 10f
        val labelColorList = MutableList(list2.size - 1) {Color.TRANSPARENT}
        labelColorList.add(prefs.getInt("color_this_week", 0))
        dataSet2.setValueTextColors(labelColorList)
        dataSet2.valueTextSize = 24f
        val circleColorList = MutableList(list2.size - 1) {Color.TRANSPARENT}
        // alpha halved, others stay the same
        circleColorList.add(
            argb(
                Color.alpha(prefs.getInt("color_this_week", 0)) / 2,
                Color.red(prefs.getInt("color_this_week", 0)),
                Color.green(prefs.getInt("color_this_week", 0)),
                Color.blue(prefs.getInt("color_this_week", 0))
            )
        )
        dataSet2.circleColors = circleColorList

        val lineData = LineData(dataSet1, dataSet2)
        chart.data = lineData
        chart.layout(0, 0, w, h)
        chart.setBackgroundColor(prefs.getInt("color_background", 0))
        chart.setDrawGridBackground(false)
        chart.xAxis.valueFormatter = XAxisWeekFormatter(context, prefs)
        chart.xAxis.setDrawGridLines(false)
        chart.xAxis.setDrawAxisLine(false)
        chart.xAxis.textColor = prefs.getInt("color_x_axis", 0)
        chart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        chart.xAxis.axisMinimum = -0.3f
        val test = (listOf(list1.size,  list2.size)).max()!!.toFloat() - 0.7f
        chart.xAxis.axisMaximum = (listOf(list1.size,  list2.size)).max()!!.toFloat() - 0.7f
        chart.xAxis.textSize = 14f
        chart.extraBottomOffset = 10f
        chart.axisRight.isEnabled = false
        chart.axisLeft.setDrawGridLines(false)
        chart.axisLeft.setDrawAxisLine(false)
        chart.axisLeft.axisMinimum = 0f

        val hasGoal = (prefs.getString("goal", "") != "")

        val maxVal: Float = if (hasGoal) {
            (list1 + list2 + listOf(prefs.getString("goal", "30")!!.toDouble())).max()!!
                .toFloat()
        } else {
            (list1 + list2).max()!!.toFloat()
        }
        chart.axisLeft.axisMaximum = maxVal
        chart.axisLeft.setDrawLabels(false)
        chart.axisLeft.setDrawZeroLine(true)
        chart.axisLeft.zeroLineColor = prefs.getInt("color_x_axis", 0)
        chart.axisLeft.yOffset

        if (hasGoal) {
            val ll =
                LimitLine(prefs.getString("goal", "30")!!.toFloat(), prefs.getString("goal", "30"))
            ll.lineColor = prefs.getInt("color_goal", 0)
            ll.lineWidth = 2f
            ll.textColor = prefs.getInt("color_goal", 0)
            ll.enableDashedLine(25f, 15f, 0f)
            ll.labelPosition = LimitLine.LimitLabelPosition.LEFT_BOTTOM
            ll.textSize = 16f
            chart.axisLeft.addLimitLine(ll)
        }
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

        // TODO: why would w be zero anyway?
        if (w>0) {
            // chart.getChartBitmap() returns RGB_565 but we need ARGB_8888
            // Define a bitmap with the same size as the view
            val returnedBitmap =
                Bitmap.createBitmap(
                    w,
                    h,
                    Bitmap.Config.ARGB_8888
                )
            // Bind a canvas to it
            val canvas = Canvas(returnedBitmap)
            chart.draw(canvas)

            val views =
                RemoteViews(
                    context.packageName,
                    R.layout.weekly_mileage_widget
                )

            val intentUpdate = Intent(context, WeeklyMileageWidget::class.java)
            intentUpdate.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE

            val idArray = intArrayOf(appWidgetId)
            intentUpdate.putExtra(
                AppWidgetManager.EXTRA_APPWIDGET_IDS,
                idArray
            )

            val pendingUpdate = PendingIntent.getBroadcast(
                context, appWidgetId, intentUpdate,
                PendingIntent.FLAG_UPDATE_CURRENT
            )

            views.setOnClickPendingIntent(
                R.id.widget_layout_id,
                pendingUpdate
            )

            views.setImageViewBitmap(R.id.imgView, returnedBitmap)

            views.setViewVisibility(R.id.textView, View.GONE)

            // Instruct the widget manager to update the widget
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    private fun plotOnlyStravaMessage(
        prefs: SharedPreferences,
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
        val h = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT).toPx()
        val w = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH).toPx()

        if (w>0) {
            // chart.getChartBitmap() returns RGB_565 but we need ARGB_8888
            // Define a bitmap with the same size as the view
            val returnedBitmap =
                Bitmap.createBitmap(
                    w,
                    h,
                    Bitmap.Config.ARGB_8888
                )

            val canvas = Canvas(returnedBitmap)
            canvas.drawColor(prefs.getInt("color_background", 0))

            val views =
                RemoteViews(
                    context.packageName,
                    R.layout.weekly_mileage_widget
                )

            val intentUpdate = Intent(context, WeeklyMileageWidget::class.java)
            intentUpdate.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE

            val idArray = intArrayOf(appWidgetId)
            intentUpdate.putExtra(
                AppWidgetManager.EXTRA_APPWIDGET_IDS,
                idArray
            )

            val pendingUpdate = PendingIntent.getBroadcast(
                context, appWidgetId, intentUpdate,
                PendingIntent.FLAG_UPDATE_CURRENT
            )

            views.setOnClickPendingIntent(
                R.id.widget_layout_id,
                pendingUpdate
            )

            views.setImageViewBitmap(R.id.imgView, returnedBitmap)

            views.setViewVisibility(R.id.textView, View.VISIBLE)

            // Instruct the widget manager to update the widget
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
