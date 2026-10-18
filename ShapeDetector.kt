package com.ras.autoclicker

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Log

/**
 * Looks at a small window of the live screen and answers a yes/no question
 * about what is there.
 *
 * IMPORTANT: this class never reads TEXT. It compares pixel brightness and
 * the proportion of light pixels, which is what makes the app work in every
 * language and is also what lets us recognise the BONUS dialog without
 * depending on its wording.
 *
 * When screen capture is unavailable (permission not granted, or the game
 * sets FLAG_SECURE) every query returns null, and the clicker falls back to
 * timed taps so the loop keeps running.
 */
class ShapeDetector(
    private val context: Context,
    private val prefs: ClickerPrefs
) {

    enum class Target { LIGHT_BUTTON, DARK_PANEL }

    /** Set by CaptureService while a MediaProjection session is live. */
    @Volatile var frameProvider: (() -> Bitmap?)? = null

    /**
     * True when a light, rounded button occupies the sampled window around
     * the given fraction of the screen.
     *
     * Returns null when we cannot see the screen at all, which the caller
     * interprets as "do not block the tap".
     */
    fun hasTargetAt(fx: Float, fy: Float, target: Target): Boolean? {
        val frame = frameProvider?.invoke() ?: return null
        val metrics = context.resources.displayMetrics
        val cx = (fx * metrics.widthPixels).toInt()
        val cy = (fy * metrics.heightPixels).toInt()

        val halfW = (metrics.widthPixels * SAMPLE_W).toInt()
        val halfH = (metrics.heightPixels * SAMPLE_H).toInt()
        val rect = Rect(
            (cx - halfW).coerceAtLeast(0),
            (cy - halfH).coerceAtLeast(0),
            (cx + halfW).coerceAtMost(frame.width),
            (cy + halfH).coerceAtMost(frame.height)
        )
        if (rect.width() <= 0 || rect.height() <= 0) return false

        var light = 0
        var total = 0
        var x = rect.left
        while (x < rect.right) {
            var y = rect.top
            while (y < rect.bottom) {
                val p = frame.getPixel(x, y)
                val r = (p shr 16) and 0xFF
                val g = (p shr 8) and 0xFF
                val b = p and 0xFF
                val lum = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0
                if (lum > LIGHT_THRESHOLD) light++
                total++
                y += 3
            }
            x += 3
        }
        if (total == 0) return false

        val ratio = light.toDouble() / total
        return when (target) {
            Target.LIGHT_BUTTON -> ratio > LIGHT_RATIO
            Target.DARK_PANEL -> ratio < DARK_RATIO
        }
    }

    /**
     * True when the dark BONUS panel is on screen.
     *
     * We sample the band just above the two buttons: in the bonus dialog that
     * area is the flat dark panel, while on the normal pack screen it is the
     * blue game background. Colour-independent enough to spot the dialog, and
     * completely text-free.
     */
    fun bonusVisible(): Boolean? {
        val frame = frameProvider?.invoke() ?: return null
        val metrics = context.resources.displayMetrics
        val y = (prefs.bonusY * metrics.heightPixels).toInt() - (metrics.heightPixels * 0.10f).toInt()
        val left = (metrics.widthPixels * 0.15f).toInt()
        val right = (metrics.widthPixels * 0.85f).toInt()
        if (y < 0 || y >= frame.height) return null

        var dark = 0
        var total = 0
        var x = left
        while (x < right) {
            val p = frame.getPixel(x.coerceIn(0, frame.width - 1), y)
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            val b = p and 0xFF
            val lum = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0
            if (lum < 0.25) dark++
            total++
            x += 4
        }
        if (total == 0) return null
        return (dark.toDouble() / total) > 0.7
    }

    companion object {
        private const val TAG = "RASShape"
        private const val SAMPLE_W = 0.06f
        private const val SAMPLE_H = 0.018f
        private const val LIGHT_THRESHOLD = 0.72
        private const val LIGHT_RATIO = 0.45
        private const val DARK_RATIO = 0.35
    }
}
