package com.example.weeklymileagewidget

import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.*
import com.android.volley.*
import com.android.volley.toolbox.*
import org.json.JSONException
import org.json.JSONObject
import java.io.UnsupportedEncodingException
import com.rarepebble.colorpicker.ColorPreference


class AuthActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)
    }

    override fun onResume()
    {
        super.onResume()
        val uri = this.intent?.data
        if (uri != null && uri.toString().startsWith("weeklymileagewidget://weeklymileagewidget.com")) {
            // return early in case of user abort
            val error = uri.getQueryParameter("error")
            if( error == "access_denied"){
                finish()
            }

            // if authorized we get code and scope
            val scope: String = uri.getQueryParameter("scope")?.toString() ?:""
            if( "activity:read" !in scope){
                finish()
            }
            val code = uri.getQueryParameter("code")

            val requestQueue: RequestQueue = Volley.newRequestQueue(this)
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
                    val athlete = response["athlete"]

                    val prefs: SharedPreferences =
                        PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                    val editor = prefs.edit()
                    editor.putString("strava_access_token", access_token.toString())
                    editor.putString("strava_refresh_token", refresh_token.toString())
                    editor.putString("strava_expires_at", expires_at.toString())
                    editor.apply()

                    var a = prefs.getString("strava_access_token", "keines da")
                    finish()
                },
                Response.ErrorListener { // Do something when error occurred
                    finish()
                }
            ) {
                override fun getBody(): ByteArray {
                    val parameters = HashMap<String, String>()
                    parameters["client_id"] = BuildConfig.STRAVA_KEY
                    parameters["client_secret"] = BuildConfig.STRAVA_SECRET
                    parameters["code"] = code.toString()
                    parameters["grant_type"] = "authorization_code"

                    return JSONObject(parameters.toString()).toString().toByteArray()
                }
            }
            requestQueue.add(jsonObjectRequest)

        }
    }
}
