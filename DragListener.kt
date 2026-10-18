package com.ras.autoclicker

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import kotlin.math.abs

/** Lets the user drag the floating panel anywhere on screen. */
class DragListener(
    private val wm: WindowManager,
    private val params: WindowManager.LayoutParams
) : View.OnTouchListener {

    private var startX = 0
    private var startY = 0
    private var touchX = 0f
    private var touchY = 0f
    private var dragged = false

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View, event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                startX = params.x
                startY = params.y
                touchX = event.rawX
                touchY = event.rawY
                dragged = false
                return false
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = (event.rawX - touchX).toInt()
                val dy = (event.rawY - touchY).toInt()
                if (abs(dx) > 12 || abs(dy) > 12) dragged = true
                if (dragged) {
                    params.x = startX + dx
                    params.y = startY + dy
                    try { wm.updateViewLayout(v, params) } catch (_: Exception) { }
                }
                return dragged
            }
            MotionEvent.ACTION_UP -> return dragged
        }
        return false
    }
}
