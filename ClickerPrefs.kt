package com.ras.autoclicker

import android.content.Context

/**
 * All tunable values of the clicker in one place.
 *
 * Coordinates are stored as FRACTIONS of the screen (0.0 - 1.0) rather than
 * pixels, so the loop survives rotation, density changes and resolution
 * changes — one of the main reasons clickers freeze on Samsung devices.
 */
class ClickerPrefs(private val context: Context) {

    private val p = context.getSharedPreferences("ras_clicker", Context.MODE_PRIVATE)

    // ---- Modes -----------------------------------------------------------
    var sellAll: Boolean
        get() = p.getBoolean("sell_all", false)
        set(v) = p.edit().putBoolean("sell_all", v).apply()

    var sellMulti: Boolean
        get() = p.getBoolean("sell_multi", false)
        set(v) = p.edit().putBoolean("sell_multi", v).apply()

    var pointersHidden: Boolean
        get() = p.getBoolean("pointers_hidden", false)
        set(v) = p.edit().putBoolean("pointers_hidden", v).apply()

    var panelRunning: Boolean
        get() = p.getBoolean("panel_running", false)
        set(v) = p.edit().putBoolean("panel_running", v).apply()

    // ---- Timing ----------------------------------------------------------
    /** Interval between gestures, in milliseconds. */
    var intervalMs: Long
        get() = p.getLong("interval_ms", 900L)
        set(v) = p.edit().putLong("interval_ms", v).apply()

    /** How long the shape check waits for a button to appear, in ms. */
    var shapeTimeoutMs: Long
        get() = p.getLong("shape_timeout_ms", 4000L)
        set(v) = p.edit().putLong("shape_timeout_ms", v).apply()

    // ---- Anchors (fractions) --------------------------------------------
    var saveAllX: Float
        get() = p.getFloat("save_all_x", 0.62f)
        set(v) = p.edit().putFloat("save_all_x", v).apply()
    var saveAllY: Float
        get() = p.getFloat("save_all_y", 0.924f)
        set(v) = p.edit().putFloat("save_all_y", v).apply()

    var sellAllX: Float
        get() = p.getFloat("sell_all_x", 0.233f)
        set(v) = p.edit().putFloat("sell_all_x", v).apply()
    var sellAllY: Float
        get() = p.getFloat("sell_all_y", 0.924f)
        set(v) = p.edit().putFloat("sell_all_y", v).apply()

    var sellMultiX: Float
        get() = p.getFloat("sell_multi_x", 0.428f)
        set(v) = p.edit().putFloat("sell_multi_x", v).apply()
    var sellMultiY: Float
        get() = p.getFloat("sell_multi_y", 0.924f)
        set(v) = p.edit().putFloat("sell_multi_y", v).apply()

    var nextPackX: Float
        get() = p.getFloat("next_pack_x", 0.62f)
        set(v) = p.edit().putFloat("next_pack_x", v).apply()
    var nextPackY: Float
        get() = p.getFloat("next_pack_y", 0.910f)
        set(v) = p.edit().putFloat("next_pack_y", v).apply()

    /** Left and right candidates of the BONUS dialog (it swaps sides). */
    var bonusLeftX: Float
        get() = p.getFloat("bonus_left_x", 0.216f)
        set(v) = p.edit().putFloat("bonus_left_x", v).apply()
    var bonusRightX: Float
        get() = p.getFloat("bonus_right_x", 0.71f)
        set(v) = p.edit().putFloat("bonus_right_x", v).apply()
    var bonusY: Float
        get() = p.getFloat("bonus_y", 0.775f)
        set(v) = p.edit().putFloat("bonus_y", v).apply()

    // ---- Session counters (reset when the clicker is turned off) ---------
    var sessionPacks: Int
        get() = p.getInt("session_packs", 0)
        set(v) = p.edit().putInt("session_packs", v).apply()

    var sessionStartMs: Long
        get() = p.getLong("session_start", 0L)
        set(v) = p.edit().putLong("session_start", v).apply()

    var accumulatedMs: Long
        get() = p.getLong("accumulated_ms", 0L)
        set(v) = p.edit().putLong("accumulated_ms", v).apply()

    fun resetSession() {
        sessionPacks = 0
        accumulatedMs = 0L
        sessionStartMs = 0L
    }
}
