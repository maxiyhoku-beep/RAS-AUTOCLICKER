package com.ras.autoclicker

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

/**
 * The floating panel.
 *
 * Two states, exactly as specified:
 *  - IDLE  (clicker off)  -> START / HIDE POINTERS / SELL ALL / SELL MULTI / CLOSE PANEL
 *  - RUNNING (clicker on) -> PAUSE / HIDE POINTERS / CLOSE PANEL, plus the
 *    timer and the packs-opened counter
 *
 * Closing the panel never touches accessibility permissions: the service
 * lives at system level and stays enabled.
 */
object PanelManager {

    private var view: View? = null
    private var wm: WindowManager? = null

    private var btnStart: TextView? = null
    private var btnHide: TextView? = null
    private var btnSellAll: TextView? = null
    private var btnSellMulti: TextView? = null
    private var btnClose: TextView? = null
    private var counters: TextView? = null
    private var collapseRow: LinearLayout? = null
    private var fullRow: LinearLayout? = null

    private val handler = Handler(Looper.getMainLooper())
    private var ticker: Runnable? = null

    @SuppressLint("InflateParams", "SetTextI18n")
    fun show(context: Context) {
        if (view != null) { refreshCounters(); return }

        val ctx = LanguageManager(context).localizedContext()
        wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val root = buildView(ctx)

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 24
            y = 160
        }
        root.setOnTouchListener(DragListener(wm!!, params))
        try {
            wm?.addView(root, params)
            view = root
            syncState()
            startTicker()
        } catch (e: Exception) {
            Toast.makeText(context, "Overlay permission missing", Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun buildView(ctx: Context): View {
        val prefs = ClickerPrefs(ctx)
        val wrap = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xEE0B0B0B.toInt())
            setPadding(28, 22, 28, 22)
            layoutParams = LinearLayout.LayoutParams(dp(ctx, 250), LinearLayout.LayoutParams.WRAP_CONTENT)
        }

        counters = TextView(ctx).apply {
            setTextColor(0xFFF5C542.toInt())
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }
        wrap.addView(counters)

        // ---- IDLE state: the full set of options -------------------------
        fullRow = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        btnStart = button(ctx, LanguageManager.string(ctx, R.string.panel_start)) {
            val svc = ClickerService.instance
            if (svc == null) {
                Toast.makeText(ctx, LanguageManager.string(ctx, R.string.start_service), Toast.LENGTH_SHORT).show()
            } else {
                ClickerForegroundService.start(ctx)
                svc.startCycle()
                syncState()
            }
        }
        btnSellAll = button(ctx, LanguageManager.string(ctx, R.string.panel_sell_all)) {
            prefs.sellAll = !prefs.sellAll
            if (prefs.sellAll) prefs.sellMulti = false
            syncState()
        }
        btnSellMulti = button(ctx, LanguageManager.string(ctx, R.string.panel_sell_multi)) {
            prefs.sellMulti = !prefs.sellMulti
            if (prefs.sellMulti) prefs.sellAll = false
            syncState()
        }
        fullRow?.addView(btnStart)
        fullRow?.addView(btnSellAll)
        fullRow?.addView(btnSellMulti)
        wrap.addView(fullRow)

        // ---- RUNNING state: the collapsed set ---------------------------
        collapseRow = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        val btnPause = button(ctx, LanguageManager.string(ctx, R.string.panel_pause)) {
            ClickerService.instance?.pauseCycle()
            ClickerForegroundService.stop(ctx)
            syncState()
        }
        collapseRow?.addView(btnPause)
        wrap.addView(collapseRow)

        // ---- Always visible ----------------------------------------------
        btnHide = button(ctx, LanguageManager.string(ctx, R.string.panel_hide_pointers)) {
            prefs.pointersHidden = !prefs.pointersHidden
            syncState()
        }
        btnClose = button(ctx, LanguageManager.string(ctx, R.string.panel_close)) {
            ClickerService.instance?.stopAndReset()
            ClickerForegroundService.stop(ctx)
            hide()
        }
        wrap.addView(btnHide)
        wrap.addView(btnClose)
        return wrap
    }

    private fun button(ctx: Context, label: String, onClick: () -> Unit): TextView {
        return TextView(ctx).apply {
            text = label
            setTextColor(0xFF0B0B0B.toInt())
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setBackgroundColor(0xFFF5C542.toInt())
            setPadding(14, 20, 14, 20)
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.topMargin = dp(ctx, 10)
            layoutParams = lp
            setOnClickListener { onClick() }
        }
    }

    /** Repaints labels and swaps between the full and the collapsed layout. */
    @SuppressLint("SetTextI18n")
    fun syncState() {
        view?.post {
            val ctx = view?.context ?: return@post
            val prefs = ClickerPrefs(ctx)
            val running = prefs.panelRunning

            fullRow?.visibility = if (running) View.GONE else View.VISIBLE
            collapseRow?.visibility = if (running) View.VISIBLE else View.GONE

            btnSellAll?.text = prefix(prefs.sellAll, LanguageManager.string(ctx, R.string.panel_sell_all))
            btnSellMulti?.text = prefix(prefs.sellMulti, LanguageManager.string(ctx, R.string.panel_sell_multi))
            btnHide?.text = if (prefs.pointersHidden)
                LanguageManager.string(ctx, R.string.panel_show_pointers)
            else
                LanguageManager.string(ctx, R.string.panel_hide_pointers)

            btnSellAll?.setBackgroundColor(if (prefs.sellAll) 0xFF12B76A.toInt() else 0xFFF5C542.toInt())
            btnSellMulti?.setBackgroundColor(if (prefs.sellMulti) 0xFF12B76A.toInt() else 0xFFF5C542.toInt())
            refreshCounters()
        }
    }

    private fun prefix(active: Boolean, label: String): String = if (active) "• $label" else label

    /** Live timer + packs counter, refreshed once a second. */
    private fun startTicker() {
        stopTicker()
        ticker = object : Runnable {
            override fun run() {
                refreshCounters()
                handler.postDelayed(this, 1000L)
            }
        }
        handler.postDelayed(ticker!!, 1000L)
    }

    private fun stopTicker() {
        ticker?.let { handler.removeCallbacks(it) }
        ticker = null
    }

    @SuppressLint("SetTextI18n")
    fun refreshCounters() {
        val v = view ?: return
        val ctx = v.context
        val prefs = ClickerPrefs(ctx)
        val elapsed = prefs.accumulatedMs +
            if (prefs.sessionStartMs != 0L) SystemClock.elapsedRealtime() - prefs.sessionStartMs else 0L

        val totalSec = elapsed / 1000
        val h = totalSec / 3600
        val m = (totalSec % 3600) / 60
        val s = totalSec % 60
        val time = String.format("%02d:%02d:%02d", h, m, s)
        counters?.text = "${LanguageManager.string(ctx, R.string.panel_time)}: $time   " +
            "${LanguageManager.string(ctx, R.string.panel_packs)}: ${prefs.sessionPacks}"
    }

    fun hide() {
        stopTicker()
        view?.let { try { wm?.removeView(it) } catch (_: Exception) { } }
        view = null
        btnStart = null; btnHide = null; btnSellAll = null; btnSellMulti = null; btnClose = null
        counters = null; fullRow = null; collapseRow = null
    }

    private fun dp(ctx: Context, v: Int) = (v * ctx.resources.displayMetrics.density).toInt()
}
