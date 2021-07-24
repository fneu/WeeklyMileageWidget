package com.example.weeklymileagewidget

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.rarepebble.colorpicker.ColorPreference


class MySettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(
        savedInstanceState: Bundle?,
        rootKey: String?
    ) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        val colorSummaryProvider =
            Preference.SummaryProvider<ColorPreference> { preference ->
                '#' + Integer.toHexString(preference.color)
            }
        findPreference<ColorPreference>("color_background")?.summaryProvider =
            colorSummaryProvider
        findPreference<ColorPreference>("color_this_week")?.summaryProvider =
            colorSummaryProvider
        findPreference<ColorPreference>("color_last_week")?.summaryProvider =
            colorSummaryProvider
        findPreference<ColorPreference>("color_x_axis")?.summaryProvider =
            colorSummaryProvider
        findPreference<ColorPreference>("color_goal")?.summaryProvider =
            colorSummaryProvider

        findPreference<ListPreference>("activities")?.summaryProvider =
            ListPreference.SimpleSummaryProvider.getInstance()
        findPreference<ListPreference>("units")?.summaryProvider =
            ListPreference.SimpleSummaryProvider.getInstance()
        findPreference<ListPreference>("weekStart")?.summaryProvider =
            ListPreference.SimpleSummaryProvider.getInstance()
        findPreference<ListPreference>("style")?.summaryProvider =
            ListPreference.SimpleSummaryProvider.getInstance()
        findPreference<EditTextPreference>("strava_access_token")?.summaryProvider =
            EditTextPreference.SimpleSummaryProvider.getInstance()
        findPreference<EditTextPreference>("strava_refresh_token")?.summaryProvider =
            EditTextPreference.SimpleSummaryProvider.getInstance()

        val expiresPreference = findPreference<EditTextPreference>("strava_expires_at")
        expiresPreference?.summaryProvider =
            EditTextPreference.SimpleSummaryProvider.getInstance()
        expiresPreference?.setOnBindEditTextListener { editText ->
            editText.inputType = InputType.TYPE_CLASS_NUMBER
        }

        // logic around strava account
        val accessTokenPref = findPreference<EditTextPreference>("strava_access_token")
        accessTokenPref?.onPreferenceChangeListener = Preference.OnPreferenceChangeListener{ _, v -> accessTokenChanged(
            v
        )}
        if (accessTokenPref != null) {
            accessTokenChanged(accessTokenPref.text)
        }


        val goalPreference = findPreference<EditTextPreference>("goal")
        goalPreference?.summaryProvider =
            EditTextPreference.SimpleSummaryProvider.getInstance()
        goalPreference?.setOnBindEditTextListener { editText ->
            editText.inputType = InputType.TYPE_CLASS_NUMBER
        }

        // invalidate each other
        val preference = findPreference<ListPreference>("style")
        preference?.onPreferenceChangeListener = Preference.OnPreferenceChangeListener{ _, v -> styleChanged(
            v
        )}

        val bgPreference = findPreference<ColorPreference>("color_background")
        bgPreference?.onPreferenceChangeListener = Preference.OnPreferenceChangeListener{ _, _ -> colorChanged()}

        findPreference<ColorPreference>("color_background")?.onPreferenceChangeListener = Preference.OnPreferenceChangeListener{ _, _ -> colorChanged()}
        findPreference<ColorPreference>("color_this_week")?.onPreferenceChangeListener = Preference.OnPreferenceChangeListener{ _, _ -> colorChanged()}
        findPreference<ColorPreference>("color_last_week")?.onPreferenceChangeListener = Preference.OnPreferenceChangeListener{ _, _ -> colorChanged()}
        findPreference<ColorPreference>("color_x_axis")?.onPreferenceChangeListener = Preference.OnPreferenceChangeListener{ _, _ -> colorChanged()}
        findPreference<ColorPreference>("color_goal")?.onPreferenceChangeListener = Preference.OnPreferenceChangeListener{ _, _ -> colorChanged()}

        findPreference<Preference>("strava_connect")?.onPreferenceClickListener = Preference.OnPreferenceClickListener{ _ -> connectStrava()}
        findPreference<Preference>("strava_disconnect")?.onPreferenceClickListener = Preference.OnPreferenceClickListener{ _ -> disconnectStrava()}

    }

    private fun connectStrava():Boolean {
        val intentUri = Uri.parse("https://www.strava.com/oauth/mobile/authorize")
            .buildUpon()
            .appendQueryParameter("client_id", BuildConfig.STRAVA_KEY)
            .appendQueryParameter("redirect_uri", BuildConfig.STRAVA_CALLBACK)
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("approval_prompt", "auto")
            .appendQueryParameter("scope", "activity:read")
            .build()

        val intent = Intent(Intent.ACTION_VIEW, intentUri)
        startActivity(intent)
        return true
    }

    private fun disconnectStrava():Boolean {
        val access = findPreference<EditTextPreference>("strava_access_token")
        val refresh = findPreference<EditTextPreference>("strava_refresh_token")
        val expires = findPreference<EditTextPreference>("strava_expires_at")
        access!!.text=""
        refresh!!.text=""
        expires!!.text="0"
        accessTokenChanged("")
        return true
    }

    private fun accessTokenChanged(v: Any): Boolean{
        val connect = findPreference<Preference>("strava_connect")
        val disconnect = findPreference<Preference>("strava_disconnect")

        connect?.isVisible = (v == "")
        disconnect?.isVisible = (v != "")

        return true
    }

    private fun colorChanged(): Boolean{
        val style = findPreference<ListPreference>("style")
        style?.value = ""
        return true
    }

    private fun styleChanged(v: Any): Boolean{
        val bg = findPreference<ColorPreference>("color_background")
        val curr = findPreference<ColorPreference>("color_this_week")
        val last = findPreference<ColorPreference>("color_last_week")
        val x = findPreference<ColorPreference>("color_x_axis")
        val goal = findPreference<ColorPreference>("color_goal")
        when(v.toString()){
            "dark" -> {
                bg?.color = Color.parseColor("#ff282c34")
                curr?.color = Color.parseColor("#ffe06c75")
                last?.color = Color.parseColor("#ff91c1f8")
                x?.color = Color.parseColor("#ff5c6370")
                goal?.color = Color.parseColor("#ff5c6370")
            }
            "light" -> {
                bg?.color = Color.parseColor("#fffaf8f5")
                curr?.color = Color.parseColor("#ff065289")
                last?.color = Color.parseColor("#ff896724")
                x?.color = Color.parseColor("#ffb6ad9a")
                goal?.color = Color.parseColor("#ffd1cec7")
            }
            "transparent" -> {
                bg?.color = Color.parseColor("#00000000")
                curr?.color = Color.parseColor("#ffffffff")
                last?.color = Color.parseColor("#ffa8a8a8")
                x?.color = Color.parseColor("#ffffffff")
                goal?.color = Color.parseColor("#64ffffff")
            }
            "strava" -> {
                bg?.color = Color.parseColor("#ffffffff")
                curr?.color = Color.parseColor("#fffc5200")
                last?.color = Color.parseColor("#ff555555")
                x?.color = Color.parseColor("#ffaaaaaa")
                goal?.color = Color.parseColor("#ffcccccc")
            }
            "polar" -> {
                bg?.color = Color.parseColor("#fff2f2f2")
                curr?.color = Color.parseColor("#ffe2002b")
                last?.color = Color.parseColor("#ffc0c8c8")
                x?.color = Color.parseColor("#ff808080")
                goal?.color = Color.parseColor("#ffcecece")
            }
            else -> {}
        }
        return true
    }

    override fun onDisplayPreferenceDialog(preference: Preference) {
        if (preference is ColorPreference) {
            (preference).showDialog(this, 0)
        } else super.onDisplayPreferenceDialog(preference)
    }
}

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings_container, MySettingsFragment())
            .commit()
    }

    override fun onResume() {
        super.onResume()
        setContentView(R.layout.activity_main)
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings_container, MySettingsFragment())
            .commit()
    }

    override fun onStop() {
        super.onStop()
        val updateWidgetIntent = Intent(
            applicationContext,
            WeeklyMileageWidget::class.java
        )
        updateWidgetIntent.action = getString(R.string.updateIntentString)
        applicationContext.sendBroadcast(updateWidgetIntent)
    }
}
