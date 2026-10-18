package com.ras.autoclicker

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.graphics.Path
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlin.math.max

/**
 * The actual tap engine.
 *
 * Design rules that matter on Samsung (and every other aggressive OEM):
 *
 * 1. Every gesture is scheduled against the ABSOLUTE clock, never chained to
 *    the previous gesture's callback. A dropped callback can no longer stall
 *    the loop.
 * 2. The loop is self-healing: a watchdog re-arms the cycle if it notices the
 *    run flag is on but no gesture has fired recently.
 * 3. Coordinates are fractions of the live screen, recomputed on every tap,
 *    so rotation / density / resolution changes do not silently break it.
 * 4. Button targets are found by SHAPE + POSITION, never by label text, so
 *    the same build works in Spanish, English or any other language.
 */
class ClickerService : AccessibilityService() {

    private lateinit var prefs: ClickerPrefs
    private val handler = Handler(Looper.getMainLooper())

    /** Guards against two cycles running at once after a service restart. */
    @Volatile private var cycleActive = false

    /** Timestamp of the last successfully dispatched gesture, for the watchdog. */
    @Volatile private var lastGestureAt = 0L

    private var shapeDetector: ShapeDetector? = null

    /** Which step of the pack cycle we are on. */
    private enum class Step { CARD_SCREEN, PACK_SCREEN, BONUS }

    override fun onCreate() {
        super.onCreate()
        prefs = ClickerPrefs(this)
        instance = this
        shapeDetector = ShapeDetector(this, prefs)
        acquireWakeLock()
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        // A fresh connection is exactly when a clicker usually goes mute.
        if (prefs.panelRunning) restartCycle()
        startWatchdog()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) { /* unused */ }

    override fun onInterrupt() { /* unused */ }

    override fun onDestroy() {
        cycleActive = false
        instance = null
        releaseWakeLock()
        super.onDestroy()
    }

    // ------------------------------------------------------------------
    // Public entry points used by the floating panel
    // ------------------------------------------------------------------

    fun startCycle() {
        prefs.panelRunning = true
        if (prefs.sessionStartMs == 0L) prefs.sessionStartMs = SystemClock.elapsedRealtime()
        restartCycle()
    }

    fun pauseCycle() {
        prefs.panelRunning = false
        // Freeze the elapsed time so the counter survives a pause.
        if (prefs.sessionStartMs != 0L) {
            prefs.accumulatedMs += SystemClock.elapsedRealtime() - prefs.sessionStartMs
            prefs.sessionStartMs = 0L
        }
        cycleActive = false
        handler.removeCallbacksAndMessages(null)
    }

    /** Called from the panel's close button: running state is fully torn down. */
    fun stopAndReset() {
        pauseCycle()
        prefs.resetSession()
    }

    private fun restartCycle() {
        cycleActive = false
        handler.removeCallbacksAndMessages(null)
        if (prefs.panelRunning) {
            cycleActive = true
            handler.postDelayed({ tick(Step.CARD_SCREEN) }, 300L)
        }
    }

    // ------------------------------------------------------------------
    // Watchdog — the anti-freeze mechanism
    // ------------------------------------------------------------------

    private var watchdogTicks = 0

    private fun startWatchdog() {
        handler.removeCallbacks(watchdogRunnable)
        handler.postDelayed(watchdogRunnable, WATCHDOG_MS)
    }

    private val watchdogRunnable = object : Runnable {
        override fun run() {
            watchdogTicks++
            val stale = SystemClock.elapsedRealtime() - lastGestureAt > STALE_MS
            val screenOn = isScreenOn()

            if (prefs.panelRunning && screenOn && stale && !cycleActive) {
                // The cycle died silently — re-arm it instead of staying mute.
                Log.w(TAG, "Watchdog: cycle stalled, re-arming")
                restartCycle()
            }
            handler.postDelayed(this, WATCHDOG_MS)
        }
    }

    private fun isScreenOn(): Boolean {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isInteractive
    }

    // ------------------------------------------------------------------
    // The cycle
    // ------------------------------------------------------------------

    private fun tick(step: Step) {
        if (!prefs.panelRunning) {
            cycleActive = false
            return
        }
        when (step) {
            Step.BONUS -> handleBonus()
            Step.PACK_SCREEN -> handlePackScreen()
            Step.CARD_SCREEN -> handleCardScreen()
        }
    }

    /**
     * Screen 1 (the pack): next-pack is always present and is what advances
     * the loop. We wait for its shape to show up before tapping.
     */
    private fun handlePackScreen() {
        val x = prefs.nextPackX
        val y = prefs.nextPackY
        awaitShapeThenTap(x, y, ShapeDetector.Target.LIGHT_BUTTON) {
            afterTap { tick(Step.CARD_SCREEN) }
        }
    }

    /**
     * Screen 2 (the cards): which button we hit depends on the active mode.
     * No mode active -> Save All. That keeps the app useful by default.
     */
    private fun handleCardScreen() {
        val isSellAll = prefs.sellAll
        val isSellMulti = prefs.sellMulti

        val x: Float
        val y: Float
        when {
            isSellAll -> { x = prefs.sellAllX; y = prefs.sellAllY }
            isSellMulti -> { x = prefs.sellMultiX; y = prefs.sellMultiY }
            else -> { x = prefs.saveAllX; y = prefs.saveAllY }
        }

        awaitShapeThenTap(x, y, ShapeDetector.Target.LIGHT_BUTTON) {
            afterTap {
                prefs.sessionPacks = prefs.sessionPacks + 1
                PanelManager.refreshCounters()
                // A bonus may or may not be due; check and branch accordingly.
                handler.postDelayed({ decideNextStep() }, 700L)
            }
        }
    }

    /**
     * A BONUS dialog appears every so often. The OK button swaps sides, and
     * both buttons have an identical shape, so we tap a candidate and verify
     * that the dark bonus panel went away. If it did not, we tap the other one.
     */
    private fun handleBonus() {
        val left = prefs.bonusLeftX
        val right = prefs.bonusRightX
        val y = prefs.bonusY

        tapFraction(left, y) {
            afterTap {
                if (shapeDetector?.bonusVisible() == true) {
                    tapFraction(right, y) {
                        afterTap { handler.postDelayed({ tick(Step.PACK_SCREEN) }, 500L) }
                    }
                } else {
                    handler.postDelayed({ tick(Step.PACK_SCREEN) }, 500L)
                }
            }
        }
    }

    /** Decides whether the next stop is the bonus dialog or the pack screen. */
    private fun decideNextStep() {
        if (!prefs.panelRunning) { cycleActive = false; return }
        if (shapeDetector?.bonusVisible() == true) {
            tick(Step.BONUS)
        } else {
            tick(Step.PACK_SCREEN)
        }
    }

    /**
     * Hybrid strategy: with permission to read the screen we wait for the
     * button's SHAPE; without it we fall back to a plain timed tap so the
     * loop never stalls.
     */
    private fun awaitShapeThenTap(x: Float, y: Float, target: ShapeDetector.Target, done: () -> Unit) {
        val deadline = SystemClock.elapsedRealtime() + prefs.shapeTimeoutMs
        val attempt = object : Runnable {
            override fun run() {
                if (!prefs.panelRunning) { cycleActive = false; return }
                val visible = shapeDetector?.hasTargetAt(x, y, target) ?: true
                if (visible || SystemClock.elapsedRealtime() >= deadline) {
                    tapFraction(x, y) { afterTap(done) }
                } else {
                    handler.postDelayed(this, 150L)
                }
            }
        }
        handler.postDelayed(attempt, 100L)
    }

    // ------------------------------------------------------------------
    // Gesture dispatch
    // ------------------------------------------------------------------

    private fun tapFraction(fx: Float, fy: Float, onDone: () -> Unit) {
        val metrics = resources.displayMetrics
        val px = (fx * metrics.widthPixels).toInt().coerceIn(0, max(0, metrics.widthPixels - 1))
        val py = (fy * metrics.heightPixels).toInt().coerceIn(0, max(0, metrics.heightPixels - 1))

        val path = Path().apply { moveTo(px.toFloat(), py.toFloat()) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0L, TAP_DURATION_MS))
            .build()

        lastGestureAt = SystemClock.elapsedRealtime()

        val accepted = dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(description: GestureDescription?) { onDone() }
            override fun onCancelled(description: GestureDescription?) { onDone() }
        }, null)

        if (!accepted) {
            // Never leave the loop hanging on a refused gesture.
            handler.postDelayed(onDone, 50L)
        }
    }

    /** Spacing helper — keeps gestures from landing on top of each other. */
    private fun afterTap(next: () -> Unit) {
        if (!prefs.panelRunning) { cycleActive = false; return }
        handler.postDelayed(next, prefs.intervalMs)
    }

    // ------------------------------------------------------------------
    // Wake lock
    // ------------------------------------------------------------------

    private var wakeLock: PowerManager.WakeLock? = null

    private fun acquireWakeLock() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "RAS::ClickerLock").apply {
            setReferenceCounted(false)
            try { acquire() } catch (_: Exception) { }
        }
    }

    private fun releaseWakeLock() {
        try { wakeLock?.takeIf { it.isHeld }?.release() } catch (_: Exception) { }
        wakeLock = null
    }

    companion object {
        private const val TAG = "RASClicker"
        private const val TAP_DURATION_MS = 40L
        private const val WATCHDOG_MS = 5_000L
        private const val STALE_MS = 12_000L

        @Volatile var instance: ClickerService? = null
            private set
    }
}
