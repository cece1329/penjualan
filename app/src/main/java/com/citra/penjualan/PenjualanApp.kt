package com.citra.penjualan

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import java.util.*

class PenjualanApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // Load saved theme preference
        val sharedPrefTheme = getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
        val themeMode = sharedPrefTheme.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        AppCompatDelegate.setDefaultNightMode(themeMode)
    }

    override fun attachBaseContext(base: Context) {
        val sharedPrefLang = base.getSharedPreferences("lang_prefs", Context.MODE_PRIVATE)
        val langCode = sharedPrefLang.getString("lang_mode", "id") ?: "id"
        
        val locale = if (langCode == "system") {
            Configuration(base.resources.configuration).locales[0]
        } else {
            Locale(langCode)
        }
        
        val context = updateResources(base, locale)
        super.attachBaseContext(context)
    }

    private fun updateResources(context: Context, locale: Locale): Context {
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }
}
