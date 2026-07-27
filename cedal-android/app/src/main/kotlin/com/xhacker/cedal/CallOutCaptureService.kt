package com.xhacker.cedal

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import java.io.ByteArrayOutputStream

// Settings > AI > "Call Out (Screen Capture)" - a single ON-DEMAND
// screenshot (not continuous "video" watching - that would need this same
// foreground-service infra running indefinitely, a much bigger, more
// battery-hungry commitment; see MemberSettingsScreen's explicit battery
// warning on this toggle). Started with the MediaProjection grant Intent
// from CornealChatScreen's permission launcher, captures exactly one frame
// via VirtualDisplay + ImageReader, hands the PNG bytes back through the
// static callback below, then tears everything down and stops itself -
// Android 14+ requires a real foreground service (not just an in-memory
// capture call) for MediaProjection to work at all, hence this being a
// Service rather than a plain helper class.
class CallOutCaptureService : Service() {
    companion object {
        private const val CHANNEL_ID = "call_out_capture"
        private const val NOTIFICATION_ID = 9201
        const val EXTRA_RESULT_CODE = "resultCode"
        const val EXTRA_RESULT_DATA = "resultData"

        // In-process only - set by the launching composable right before
        // starting this service, consumed exactly once per capture.
        var onCaptured: ((ByteArray?) -> Unit)? = null
    }

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification())
        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED) ?: Activity.RESULT_CANCELED
        @Suppress("DEPRECATION")
        val resultData = intent?.getParcelableExtra<Intent>(EXTRA_RESULT_DATA)
        if (resultCode != Activity.RESULT_OK || resultData == null) {
            finish(null)
            return START_NOT_STICKY
        }

        val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val projection = projectionManager.getMediaProjection(resultCode, resultData)
        if (projection == null) {
            finish(null)
            return START_NOT_STICKY
        }
        mediaProjection = projection

        val metrics = DisplayMetrics()
        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getRealMetrics(metrics)
        val width = metrics.widthPixels
        val height = metrics.heightPixels
        val density = metrics.densityDpi

        val reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        imageReader = reader
        reader.setOnImageAvailableListener({ r ->
            val image = r.acquireLatestImage()
            if (image == null) {
                finish(null)
                return@setOnImageAvailableListener
            }
            try {
                finish(imageToPng(image, width, height))
            } finally {
                image.close()
            }
        }, null)

        virtualDisplay = projection.createVirtualDisplay(
            "call-out-capture", width, height, density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, reader.surface, null, null,
        )
        return START_NOT_STICKY
    }

    private fun imageToPng(image: Image, width: Int, height: Int): ByteArray {
        val plane = image.planes[0]
        val buffer = plane.buffer
        val pixelStride = plane.pixelStride
        val rowStride = plane.rowStride
        val rowPadding = rowStride - pixelStride * width
        val bitmap = Bitmap.createBitmap(width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888)
        bitmap.copyPixelsFromBuffer(buffer)
        val cropped = Bitmap.createBitmap(bitmap, 0, 0, width, height)
        val out = ByteArrayOutputStream()
        cropped.compress(Bitmap.CompressFormat.PNG, 100, out)
        bitmap.recycle()
        cropped.recycle()
        return out.toByteArray()
    }

    // Guards against the ImageReader listener AND a failed/cancelled start
    // both trying to tear down and callback more than once.
    private var finished = false
    private fun finish(bytes: ByteArray?) {
        if (finished) return
        finished = true
        onCaptured?.invoke(bytes)
        onCaptured = null
        virtualDisplay?.release()
        imageReader?.close()
        mediaProjection?.stop()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun buildNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Call Out screen capture", NotificationManager.IMPORTANCE_LOW)
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Corneal is looking at your screen")
            .setContentText("Call Out - one-time screenshot for Corneal")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        virtualDisplay?.release()
        imageReader?.close()
        mediaProjection?.stop()
        super.onDestroy()
    }
}
