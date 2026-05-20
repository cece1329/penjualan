package com.citra.penjualan

import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

class PenjualanApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // Load saved theme preference
        val sharedPref = getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
        val themeMode = sharedPref.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        AppCompatDelegate.setDefaultNightMode(themeMode)
    }
}
