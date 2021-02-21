package com.example.weeklymileagewidget

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

class AuthActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)
    }

    override fun onResume()
    {
        super.onResume()
        val uri = this.intent?.data
        if (uri != null && uri.toString().startsWith("wmw://com.example.weeklymileagewidget")) {
            val access_token = uri.getQueryParameter("code")
            print(access_token)

            finish()

        }
    }
}
