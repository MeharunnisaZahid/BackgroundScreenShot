package com.example.backgroundscreenshot.services

import android.accessibilityservice.AccessibilityService
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.os.Environment
import android.os.Handler
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class ScreenshotService : AccessibilityService() {
    private lateinit var rootNode: AccessibilityNodeInfo
    private lateinit var rect: Rect
    private lateinit var bitmap: Bitmap
    private lateinit var canvas: Canvas
    private val handler = Handler()

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            takeScreenshot()
        }
    }

    private fun takeScreenshot() {
        rootNode = rootInActiveWindow
        rect = Rect()
        rootNode.getBoundsInScreen(rect)
        bitmap = Bitmap.createBitmap(rect.width(), rect.height(), Bitmap.Config.ARGB_8888)
        canvas = Canvas(bitmap)
        drawNodeRecursive(canvas, rootNode, rect)
        saveScreenshotToFile(bitmap)
    }

    private fun saveScreenshotToFile(bitmap: Bitmap) {
        val screenshotFile = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
            "screenshot_${System.currentTimeMillis()}.png"
        )
        try {
            FileOutputStream(screenshotFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        Log.d("FilePath", ":$screenshotFile")
    }

    private fun drawNodeRecursive(canvas: Canvas, node: AccessibilityNodeInfo, bounds: Rect) {
        val nodeBounds = Rect()
        node.getBoundsInScreen(nodeBounds)
        if (nodeBounds.width() > 0 && nodeBounds.height() > 0) {
            canvas.clipRect(bounds)
            canvas.save()
            canvas.translate(nodeBounds.left.toFloat(), nodeBounds.top.toFloat())
            if (node.childCount > 0) {
                for (i in 0 until node.childCount) {
                    val childNode = node.getChild(i)
                    drawNodeRecursive(canvas, childNode, bounds)
                }
            } /*else {
                drawNode(canvas, node, nodeBounds)
            }*/
            canvas.restore()
        }
    }

/*    private fun drawNode(canvas: Canvas, node: AccessibilityNodeInfo, bounds: Rect) {
        if (node.className == "android.view.View") {
            val bitmap = Bitmap.createBitmap(bounds.width(), bounds.height(), Bitmap.Config.ARGB_8888)
            canvas.drawBitmap(bitmap,bounds.left,bounds.top,null)//drawBitmap(node.window.decorHintBitmap, bounds.left, bounds.top, null)
        }
    }*/

    override fun onServiceConnected() {
        super.onServiceConnected()
        handler.postDelayed(screenshotRunnable, 60000) // 60,000 milliseconds = 1 minute
    }

    override fun onInterrupt() {
        // Implement if your service needs to handle interruptions
    }

    private val screenshotRunnable = object : Runnable {
        override fun run() {
            takeScreenshot()
            handler.postDelayed(this, 60000)
        }
    }
}
