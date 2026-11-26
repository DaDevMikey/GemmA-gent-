package com.kairo.agent.data

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

/**
 * Applies and persists the user's chosen App UI language across ComponentActivity & Compose.
 */
object LocaleHelper {
    fun getSavedAppLanguage(context: Context): String {
        val prefs = context.applicationContext.getSharedPreferences("kairo_settings", Context.MODE_PRIVATE)
        return prefs.getString("app_language", "en") ?: "en"
    }

    fun wrapContext(base: Context): Context {
        val lang = getSavedAppLanguage(base)
        val locale = Locale.forLanguageTag(lang)
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return base.createConfigurationContext(config)
    }

    fun applyAppLanguage(context: Context, langCode: String, recreateActivity: Boolean = true) {
        context.applicationContext
            .getSharedPreferences("kairo_settings", Context.MODE_PRIVATE)
            .edit()
            .putString("app_language", langCode)
            .apply()

        val locale = Locale.forLanguageTag(langCode)
        Locale.setDefault(locale)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            runCatching {
                context.getSystemService(LocaleManager::class.java)?.applicationLocales =
                    LocaleList.forLanguageTags(langCode)
            }
        }
        runCatching {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(langCode))
        }

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        @Suppress("DEPRECATION")
        context.resources.updateConfiguration(config, context.resources.displayMetrics)

        if (recreateActivity) {
            (context as? Activity)?.recreate()
        }
    }
}
