package com.example.backgroundscreenshot.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.MediaScannerConnection
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Build.VERSION_CODES.O
import android.os.Environment
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.MediaStore
import android.util.DisplayMetrics
import android.util.Log
import android.util.SparseIntArray
import android.view.Surface
import android.view.WindowManager
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.example.backgroundscreenshot.MainActivity
import com.example.backgroundscreenshot.R
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer

class ScreenshotService : Service() {
    private lateinit var mediaProjectionManager: MediaProjectionManager
    private lateinit var mediaProjection: MediaProjection
    private lateinit var imageReader: ImageReader
    private lateinit var virtualDisplay: VirtualDisplay
    private lateinit var displayMetrics : DisplayMetrics
    private val handler = Handler(Looper.getMainLooper())
    private var screenDensity = 320
    private val NOTIFICATION_ID = 1
    private val NOTIFICATION_CHANNEL_ID = "ScreenshotServiceChannel"

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        initRunningTipNotification()
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )
        } else startForeground(
            NOTIFICATION_ID,
            notification
        )
        if (intent != null) {
            val resultCode = intent.getIntExtra("resultCode", 0)
            val data = intent.getParcelableExtra<Intent>("data")
            init(resultCode, data)
        }
        return START_NOT_STICKY
    }

    private fun buildNotification(): Notification {
        val channelId =
            createNotificationChannel("my_service", "Capture Service")
        val intent = Intent(this, MainActivity::class.java).apply {
            flags =
                Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT and Intent.FLAG_ACTIVITY_SINGLE_TOP and Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
        val notification = notificationBuilder.setOngoing(true)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setContentText(
                "Service is running"
            )
            .setContentIntent(pendingIntent)
            .setCategory(Notification.CATEGORY_STATUS)
            .build()
        return notification
    }


    private fun createNotificationChannel(channelId: String, channelName: String): String {
        val chan = if (Build.VERSION.SDK_INT >= O) {
            NotificationChannel(
                channelId,
                channelName, NotificationManager.IMPORTANCE_NONE
            )
        } else {
            return channelId
        }
        chan.lightColor = Color.BLUE
        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(chan)
        return channelId
    }

    private fun initRunningTipNotification() {
        val builder: Notification.Builder
        var notificationManager: NotificationManager
        if (Build.VERSION.SDK_INT >= O) {
            val channel = NotificationChannel(
                "running",
                "Running Notification",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            builder = Notification.Builder(this, channel.id)
            notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        } else {
            builder = Notification.Builder(this)
        }

        builder.setContentText("Running Notification")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        startForeground(100, builder.build())
    }

    private fun init(resultCode: Int, data: Intent?) {
        mediaProjectionManager =
            getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection =
            data?.let { mediaProjectionManager.getMediaProjection(resultCode, it) }!!
        displayMetrics = DisplayMetrics()
        (getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.getMetrics(displayMetrics)
        imageReader = ImageReader.newInstance(displayMetrics.widthPixels, displayMetrics.heightPixels, PixelFormat.RGBA_8888, 2)
        startCapture()
    }

    private fun startCapture() {
        virtualDisplay = mediaProjection.createVirtualDisplay(
            "ScreenshotService",
            displayMetrics.widthPixels , displayMetrics.heightPixels, screenDensity,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader.surface, null, null
        )
        captureScreenshot()
        handler.postDelayed({
            captureScreenshot()
            virtualDisplay.release()
            startCapture()
        }, CAPTURE_INTERVAL.toLong())
    }

    private fun captureScreenshot() {
        val image : Image = imageReader.acquireLatestImage()
        if (image != null) {
            imageToBitmap(image)
            image.close()
        }
    }

    private fun imageToBitmap(image: Image) {
        try {
            val buffer: ByteBuffer = image.planes[0].buffer
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)

            val filename = "${System.currentTimeMillis()}.jpg"
            Log.d("FileName", "FileName: $filename")
            val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
            val imageFile = File(imagesDir, filename)
            Log.e("FilePath", ":${imageFile.absolutePath}")

            FileOutputStream(imageFile).use { outputStream ->
                outputStream.write(bytes)
            }

            MediaScannerConnection.scanFile(
                this,
                arrayOf(imageFile.absolutePath),
                arrayOf("image/jpeg"),
                null
            )

            showToast("Image saved: ${imageFile.absolutePath}")
        } catch (e: Exception) {
            Log.e("CaptureScreenshot", "Error saving image: ${e.message}")
            e.printStackTrace()
            showToast("Error saving image")
        }
    }


    private fun showToast(message: String) {
        handler.post {
            Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
        }
    }


    companion object {
        private const val CAPTURE_INTERVAL = 60 * 1000 // 1 minute
        private val ORIENTATION = SparseIntArray()

        init {
            ORIENTATION.append(Surface.ROTATION_0, 90)
            ORIENTATION.append(Surface.ROTATION_90, 0)
            ORIENTATION.append(Surface.ROTATION_180, 270)
            ORIENTATION.append(Surface.ROTATION_270, 180)
        }
    }
}

