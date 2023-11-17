package com.example.backgroundscreenshot

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import com.example.backgroundscreenshot.services.ScreenshotService

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (!isAccessibilityServiceEnabled(this, ScreenshotService::class.java)) {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
    }

    override fun onResume() {
        super.onResume()
        startService(Intent(this, ScreenshotService::class.java))
    }

    /*override fun onPause() {
        super.onPause()

        // Stop the screenshot service
        stopService(Intent(this, ScreenshotService::class.java))
    }*/

    private fun isAccessibilityServiceEnabled(
        context: Context,
        accessibilityService: Class<*>
    ): Boolean {
        val expectedComponentName = ComponentName(context, accessibilityService)
        val enabledServicesSetting = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        return enabledServicesSetting?.contains(expectedComponentName.flattenToString()) == true
    }
}
