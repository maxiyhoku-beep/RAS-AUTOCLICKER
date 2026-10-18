package com.ras.autoclicker

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/**
 * Resolves every user-facing string from the language the user picked with the
 * flags, independent of the system locale.
 */
class LanguageManager(private val context: Context) {

    fun current(): String = prefs().getString(KEY_LANG, LANG_ES) ?: LANG_ES

    fun save(lang: String) {
        prefs().edit().putString(KEY_LANG, lang).apply()
        applyToConfiguration()
    }

    fun isSpanish(): Boolean = current() == LANG_ES

    /** Wraps the context so resources resolve in the chosen language. */
    fun localizedContext(): Context {
        val locale = Locale(if (isSpanish()) "es" else "en")
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }

    private fun applyToConfiguration() {
        val locale = Locale(if (isSpanish()) "es" else "en")
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        @Suppress("DEPRECATION")
        context.resources.updateConfiguration(config, context.resources.displayMetrics)
    }

    private fun prefs() = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    companion object {
        const val PREFS = "ras_prefs"
        const val KEY_LANG = "app_language"
        const val LANG_ES = "es"
        const val LANG_EN = "en"

        /** Standalone lookup usable from a Service, which has no Activity base. */
        fun string(context: Context, resId: Int): String =
            LanguageManager(context).localizedContext().getString(resId)
    }
}
