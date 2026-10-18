package com.ras.autoclicker

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * Language picker. Two flags: United States -> English, Argentina -> Spanish.
 * The choice is stored and every screen resolves its strings from it, so the
 * UI language is independent of the phone's system locale.
 */
class LanguageActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LanguageManager(newBase).localizedContext())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val lm = LanguageManager(this)
        val ctx = lm.localizedContext()

        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(0xFF0B0B0B.toInt())
            setPadding(60, 60, 60, 60)
        }

        root.addView(TextView(ctx).apply {
            text = ctx.getString(R.string.choose_language)
            setTextColor(0xFFF5C542.toInt())
            textSize = 22f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 60)
        })

        root.addView(flagButton(ctx, "🇺🇸  ENGLISH", LanguageManager.LANG_EN) {
            lm.save(LanguageManager.LANG_EN)
            goHome()
        })

        root.addView(flagButton(ctx, "🇦🇷  ESPAÑOL", LanguageManager.LANG_ES) {
            lm.save(LanguageManager.LANG_ES)
            goHome()
        })

        setContentView(root)
    }

    private fun goHome() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun flagButton(ctx: Context, label: String, lang: String, action: () -> Unit): TextView =
        TextView(ctx).apply {
            text = label
            textSize = 26f
            gravity = Gravity.CENTER
            setTextColor(Color.BLACK)
            setBackgroundColor(0xFFF5C542.toInt())
            setPadding(40, 46, 40, 46)
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.bottomMargin = 34
            layoutParams = lp
            setOnClickListener { action() }
        }
}
