package com.ras.autoclicker

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.content.Context
import androidx.appcompat.app.AppCompatActivity

/**
 * Home screen: the logo, the two main sections and the system tweaks that
 * keep the clicker alive on aggressive OEMs.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var lm: LanguageManager

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LanguageManager(newBase).localizedContext())
    }

    @Override
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lm = LanguageManager(this)

        // First launch -> ask for the language before anything else.
        if (!lm.prefsHasLanguage()) {
            startActivity(Intent(this, LanguageActivity::class.java))
        }

        setContentView(buildUi())
    }

    override fun onResume() {
        super.onResume()
        setContentView(buildUi())
        PanelManager.syncState()
    }

    private fun buildUi(): android.view.View {
        val ctx = LanguageManager(this).localizedContext()
        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setBackgroundColor(0xFF0B0B0B.toInt())
            setPadding(48, 72, 48, 48)
        }

        val logo = ImageView(ctx).apply {
            setImageResource(R.mipmap.ic_launcher)
            layoutParams = LinearLayout.LayoutParams(420, 420)
        }
        root.addView(logo)

        root.addView(TextView(ctx).apply {
            text = ctx.getString(R.string.app_name)
            setTextColor(0xFFF5C542.toInt())
            gravity = Gravity.CENTER
            textSize = 22f
            setPadding(0, 24, 0, 48)
        })

        root.addView(bigButton(ctx, ctx.getString(R.string.channels)) {
            startActivity(Intent(this, ChannelsActivity::class.java))
        })

        root.addView(bigButton(ctx, ctx.getString(R.string.specific_search)) {
            startActivity(Intent(this, SpecificSearchActivity::class.java))
        })

        root.addView(bigButton(ctx, ctx.getString(R.string.start_service)) {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        })

        root.addView(subButton(ctx, ctx.getString(R.string.overlay_permission) + ": " +
            if (Settings.canDrawOverlays(this)) ctx.getString(R.string.granted) else ctx.getString(R.string.not_granted)) {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
        })

        root.addView(subButton(ctx, ctx.getString(R.string.battery_settings)) {
            openBatterySettings()
        })

        root.addView(subButton(ctx, "ES / EN") {
            startActivity(Intent(this, LanguageActivity::class.java))
        })

        return root
    }

    /**
     * Opens the "no restrictions" battery screen. On One UI this is the exact
     * setting that stops Samsung from freezing the clicker after a few
     * minutes — the single most effective anti-freeze step for the user.
     */
    private fun openBatterySettings() {
        val candidates = listOf(
            Intent().setAction("android.settings.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS")
                .setData(Uri.parse("package:$packageName")),
            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS),
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).setData(Uri.parse("package:$packageName"))
        )
        for (i in candidates) {
            try { startActivity(i); return } catch (_: Exception) { }
        }
        Toast.makeText(this, "Open Settings > Battery manually", Toast.LENGTH_LONG).show()
    }

    private fun bigButton(ctx: Context, label: String, action: () -> Unit): Button =
        Button(ctx).apply {
            text = label
            textSize = 15f
            setTextColor(0xFF0B0B0B.toInt())
            setBackgroundColor(0xFFF5C542.toInt())
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.bottomMargin = 26
            layoutParams = lp
            setOnClickListener { action() }
        }

    private fun subButton(ctx: Context, label: String, action: () -> Unit): Button =
        Button(ctx).apply {
            text = label
            textSize = 13f
            setTextColor(0xFFF5C542.toInt())
            setBackgroundColor(0xFF141414.toInt())
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.bottomMargin = 18
            layoutParams = lp
            setOnClickListener { action() }
        }
}
