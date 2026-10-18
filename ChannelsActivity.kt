package com.ras.autoclicker

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/**
 * The four creator channels. Each button opens the link in the YouTube app
 * when it is installed, and falls back to the browser otherwise.
 */
class ChannelsActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LanguageManager(newBase).localizedContext())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val ctx = LanguageManager(this).localizedContext()

        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setBackgroundColor(0xFF0B0B0B.toInt())
            setPadding(48, 80, 48, 48)
        }

        root.addView(TextView(ctx).apply {
            text = ctx.getString(R.string.channels_title)
            setTextColor(0xFFF5C542.toInt())
            textSize = 24f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 56)
        })

        root.addView(channel(ctx, R.string.channel_felcor, "https://youtube.com/@felcor"))
        root.addView(channel(ctx, R.string.channel_comer305, "https://youtube.com/@comer305"))
        root.addView(channel(ctx, R.string.channel_dylanfut, "https://youtube.com/@dylanfut2322"))
        root.addView(channel(ctx, R.string.channel_frykko, "https://youtube.com/@frykko57"))

        setContentView(root)
    }

    private fun channel(ctx: Context, labelRes: Int, url: String): Button =
        Button(ctx).apply {
            text = ctx.getString(labelRes)
            textSize = 15f
            setTextColor(0xFF0B0B0B.toInt())
            setBackgroundColor(0xFFF5C542.toInt())
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.bottomMargin = 28
            layoutParams = lp
            setOnClickListener { open(ctx, url) }
        }

    private fun open(ctx: Context, url: String) {
        val app = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            .setPackage("com.google.android.youtube")
        try {
            startActivity(app)
            return
        } catch (_: ActivityNotFoundException) { }
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: Exception) {
            Toast.makeText(ctx, ctx.getString(R.string.open_channel_failed), Toast.LENGTH_SHORT).show()
        }
    }
}
