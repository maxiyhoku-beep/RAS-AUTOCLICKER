package com.ras.autoclicker

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.util.Log

/**
 * Wraps a MediaProjection session so the shape detector can look at the
 * screen in real time.
 *
 * If the game blocks capture (FLAG_SECURE) the frames come back blank; the
 * detector treats that as "cannot see" and the clicker keeps working on
 * timing alone, so the app never dead-ends.
 */
object CaptureService {

    private const val TAG = "RASCapture"

    private var projection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var thread: HandlerThread? = null
    private var handler: Handler? = null

    @Volatile private var latest: Bitmap? = null

    val isActive: Boolean get() = projection != null

    /** Kicks off the system permission dialog; call from an Activity. */
    fun requestCapture(activity: Activity, requestCode: Int) {
        val mgr = activity.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        activity.startActivityForResult(mgr.createScreenCaptureIntent(), requestCode)
    }

    /** Call from onActivityResult with the user's answer. */
    fun onResult(context: Context, resultCode: Int, data: Intent?): Boolean {
        if (resultCode != Activity.RESULT_OK || data == null) return false
        val mgr = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        return try {
            start(mgr.getMediaProjection(resultCode, data), context)
            true
        } catch (e: Exception) {
            Log.w(TAG, "Could not start projection", e)
            false
        }
    }

    private fun start(mp: MediaProjection, context: Context) {
        stop()
        projection = mp
        val dm = context.resources.displayMetrics
        val width = dm.widthPixels
        val height = dm.heightPixels
        val density = dm.densityDpi

        thread = HandlerThread("ras-capture").also { it.start() }
        handler = Handler(thread!!.looper)

        imageReader = ImageReader.newInstance(width, height, android.graphics.PixelFormat.RGBA_8888, 2)
        imageReader?.setOnImageAvailableListener({ reader ->
            try {
                val image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener
                val plane = image.planes[0]
                val buffer = plane.buffer
                val pixelStride = plane.pixelStride
                val rowStride = plane.rowStride
                val rowPadding = rowStride - pixelStride * width
                val bmp = Bitmap.createBitmap(
                    width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888
                )
                bmp.copyPixelsFromBuffer(buffer)
                latest = Bitmap.createBitmap(bmp, 0, 0, width, height)
                image.close()
            } catch (_: Exception) { }
        }, handler)

        virtualDisplay = mp.createVirtualDisplay(
            "ras-capture",
            width, height, density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface, null, handler
        )
    }

    fun currentFrame(): Bitmap? = latest

    fun stop() {
        try { virtualDisplay?.release() } catch (_: Exception) { }
        try { projection?.stop() } catch (_: Exception) { }
        try { imageReader?.close() } catch (_: Exception) { }
        try { thread?.quitSafely() } catch (_: Exception) { }
        virtualDisplay = null
        projection = null
        imageReader = null
        thread = null
        handler = null
        latest = null
    }
}
