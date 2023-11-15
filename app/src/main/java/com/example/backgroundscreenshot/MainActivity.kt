package com.example.backgroundscreenshot

import android.Manifest
import android.app.Activity
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.backgroundscreenshot.services.ScreenshotService

class MainActivity : AppCompatActivity() {
    private val REQUEST_MEDIA_PROJECTION = 1
    private val REQUEST_CODE_SCREENSHOT= 1
    private val mediaProjectionManager: MediaProjectionManager by lazy {
        application.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    }
    private lateinit var startServiceButton: Button
    private lateinit var serviceIntent: Intent

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        startServiceButton = findViewById(R.id.startServiceButton)
        serviceIntent = Intent(this, ScreenshotService::class.java)
//        ContextCompat.startForegroundService(this, serviceIntent)
        startServiceButton.setOnClickListener {

            startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), REQUEST_CODE_SCREENSHOT)

        }

    }

    private fun checkPermissions(): Boolean {
        return (ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED)
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
            REQUEST_MEDIA_PROJECTION
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_MEDIA_PROJECTION) {
            if (checkPermissions() && resultCode == Activity.RESULT_OK) {
                startScreenshotService(resultCode, data!!)
            } else {
                requestPermissions()
                if (resultCode == Activity.RESULT_CANCELED) {
                    Toast.makeText(this, "Screen Cast Permission Denied", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Log.e(TAG, "Unknown request code: $requestCode")
        }
        super.onActivityResult(requestCode, resultCode, data)
    }


    private fun startScreenshotService(resultCode: Int, data: Intent) {
        val serviceIntent = Intent(this, ScreenshotService::class.java).apply {
            putExtra("resultCode", resultCode)
            putExtra("data", data)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        ContextCompat.startForegroundService(this, serviceIntent)
        }
        else{
            startService(serviceIntent)
        }
    }


}
