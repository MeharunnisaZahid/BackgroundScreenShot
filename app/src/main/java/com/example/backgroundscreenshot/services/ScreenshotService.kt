package com.example.backgroundscreenshot.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import android.util.DisplayMetrics
import android.util.Log
import android.util.SparseIntArray
import android.view.Surface
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.example.backgroundscreenshot.MainActivity
import com.example.backgroundscreenshot.R
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer

class ScreenshotService : Service() {
    private lateinit var mediaProjectionManager: MediaProjectionManager
    private lateinit var mediaProjection: MediaProjection
    private lateinit var imageReader: ImageReader
    private lateinit var virtualDisplay: VirtualDisplay
    private val handler = Handler(Looper.getMainLooper())
    private var screenDensity = 0
    private val NOTIFICATION_ID = 1
    private val NOTIFICATION_CHANNEL_ID = "ScreenshotServiceChannel"

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            val resultCode = intent.getIntExtra("resultCode", 0)
            val data = intent.getParcelableExtra<Intent>("data")
            init(resultCode, data)
        }
        initRunningTipNotification()
        val notification = buildNotification()
        startForeground(NOTIFICATION_ID, notification)

        return START_NOT_STICKY
    }

    private fun buildNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Screenshot Service")
            .setContentText("Service is running")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setLargeIcon(
                BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher)
            )
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun initRunningTipNotification() {
        val builder: Notification.Builder
        var notificationManager: NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
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
        val metrics = DisplayMetrics()
        screenDensity = metrics.densityDpi
        mediaProjectionManager =
            getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = data?.let { mediaProjectionManager.getMediaProjection(resultCode, it) }!!
        createImageReader()
        startCapture()
    }

    private fun createImageReader() {
        val width = DISPLAY_WIDTH
        val height = DISPLAY_HEIGHT
        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
    }

    private fun startCapture() {
        virtualDisplay = mediaProjection.createVirtualDisplay(
            "ScreenshotService",
            DISPLAY_WIDTH, DISPLAY_HEIGHT, screenDensity,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader.surface, null, null
        )
        handler.postDelayed({
            captureScreenshot()
            virtualDisplay.release()
            startCapture()
        }, CAPTURE_INTERVAL.toLong())
    }

    private fun captureScreenshot() {
        val image = imageReader.acquireLatestImage()
        if (image != null) {
            val bitmap = imageToBitmap(image)
            image.close()
            saveScreenshot(bitmap)
        }
    }


    private fun imageToBitmap(image: Image): Bitmap {
        val buffer: ByteBuffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    private fun saveScreenshot(bitmap: Bitmap) {
        val filename = "${System.currentTimeMillis()}.jpg"
        Log.d("FileName", "FileName:$filename")
        val imagesDir =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
        val imageFile = File(imagesDir, filename)
        FileOutputStream(imageFile).use { outputStream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
        }

        MediaScannerConnection.scanFile(
            this,
            arrayOf(imageFile.absolutePath),
            arrayOf("image/jpeg"),
            null
        )
    }

    companion object {
        private const val DISPLAY_WIDTH = 720
        private const val DISPLAY_HEIGHT = 1280
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

