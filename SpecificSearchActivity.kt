package com.ras.autoclicker

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/**
 * SPECIFIC SEARCH.
 *
 * The user loads between one and three screenshots of the cards they want.
 * While the clicker runs, the app compares what appears on screen against
 * those screenshots. Every time one of them shows up it fires the
 * "HAZ CONSEGUIDO TU CARTA ESPECÍFICA" notification.
 *
 * Pausing rules, as specified:
 *  - one card  -> notify, pause, close the panel and the app
 *  - two/three -> notify for each one found, pause only once all are found
 */
class SpecificSearchActivity : AppCompatActivity() {

    companion object {
        const val PICK_IMAGE = 4101
        const val MAX_CAPTURES = 3
        const val PREFS = "ras_specific_search"
        const val KEY_COUNT = "capture_count"

        /** Empties the wanted list. Called when the clicker is turned off. */
        fun clear(context: Context) {
            val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            p.edit().clear().apply()
        }

        fun loaded(context: Context): Int =
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_COUNT, 0)

        /**
         * Fires the success notification. Called by the clicker when a wanted
         * card is spotted on screen.
         */
        fun notifyFound(context: Context, remaining: Int) {
            val ctx = LanguageManager(context).localizedContext()
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (nm.getNotificationChannel(CH_FOUND) == null) {
                    nm.createNotificationChannel(
                        NotificationChannel(
                            CH_FOUND,
                            ctx.getString(R.string.notif_channel_name),
                            NotificationManager.IMPORTANCE_HIGH
                        )
                    )
                }
            }
            val open = PendingIntent.getActivity(
                context, 1,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            val notif = Notification.Builder(context, CH_FOUND)
                .setContentTitle(ctx.getString(R.string.card_found_title))
                .setContentText(ctx.getString(R.string.card_found_body))
                .setSmallIcon(android.R.drawable.stat_notify_more)
                .setAutoCancel(true)
                .setContentIntent(open)
                .build()
            nm.notify(2000 + remaining, notif)
        }

        const val CH_FOUND = "ras_found_channel"
    }

    private lateinit var counter: TextView

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LanguageManager(newBase).localizedContext())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildUi())
    }

    private fun buildUi(): android.view.View {
        val ctx = LanguageManager(this).localizedContext()
        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setBackgroundColor(0xFF0B0B0B.toInt())
            setPadding(48, 80, 48, 48)
        }

        root.addView(TextView(ctx).apply {
            text = ctx.getString(R.string.specific_search_title)
            setTextColor(0xFFF5C542.toInt())
            textSize = 24f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 30)
        })

        root.addView(TextView(ctx).apply {
            text = ctx.getString(R.string.specific_search_hint)
            setTextColor(0xFFB0B0B0.toInt())
            textSize = 14f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 30)
        })

        counter = TextView(ctx).apply {
            setTextColor(0xFF12B76A.toInt())
            textSize = 15f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 40)
        }
        root.addView(counter)
        updateCounter()

        root.addView(action(ctx, ctx.getString(R.string.add_capture)) {
            if (loaded(this) >= MAX_CAPTURES) {
                Toast.makeText(ctx, "Máx. $MAX_CAPTURES", Toast.LENGTH_SHORT).show()
            } else {
                @Suppress("DEPRECATION")
                startActivityForResult(
                    Intent(Intent.ACTION_PICK).setType("image/*"), PICK_IMAGE
                )
            }
        })

        root.addView(action(ctx, ctx.getString(R.string.clear_captures)) {
            clear(this)
            updateCounter()
        })

        return root
    }

    @Deprecated("Fine for a picker on this target range.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE && resultCode == Activity.RESULT_OK && data?.data != null) {
            store(this, data.data.toString())
            updateCounter()
        }
    }

    private fun store(context: Context, uri: String) {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val n = p.getInt(KEY_COUNT, 0) + 1
        p.edit().putInt(KEY_COUNT, n).putString("uri_$n", uri).apply()
    }

    private fun updateCounter() {
        counter.text = "${getString(R.string.captures_loaded)}: ${loaded(this)} / $MAX_CAPTURES"
    }

    private fun action(ctx: Context, label: String, action: () -> Unit): Button =
        Button(ctx).apply {
            text = label
            textSize = 15f
            setTextColor(0xFF0B0B0B.toInt())
            setBackgroundColor(0xFFF5C542.toInt())
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.bottomMargin = 26
            layoutParams = lp
            setOnClickListener { action() }
        }
}
