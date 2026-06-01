package com.citra.penjualan.beranda

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import com.citra.penjualan.BaseActivity
import com.citra.penjualan.R
import com.citra.penjualan.databinding.ActivitySettingsBinding
import java.util.*

class SettingsActivity : BaseActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener { finish() }

        // Theme Persistence
        val sharedPrefTheme = getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
        val currentMode = sharedPrefTheme.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)

        when (currentMode) {
            AppCompatDelegate.MODE_NIGHT_NO -> binding.rbThemeLight.isChecked = true
            AppCompatDelegate.MODE_NIGHT_YES -> binding.rbThemeDark.isChecked = true
            else -> binding.rbThemeSystem.isChecked = true
        }

        binding.rgTheme.setOnCheckedChangeListener { _, checkedId ->
            val selectedMode = when (checkedId) {
                R.id.rbThemeLight -> AppCompatDelegate.MODE_NIGHT_NO
                R.id.rbThemeDark -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
            sharedPrefTheme.edit().putInt("theme_mode", selectedMode).apply()
            AppCompatDelegate.setDefaultNightMode(selectedMode)
            Toast.makeText(this, getString(R.string.theme_changed_toast), Toast.LENGTH_SHORT).show()
        }

        // Language Persistence
        val sharedPrefLang = getSharedPreferences("lang_prefs", Context.MODE_PRIVATE)
        val currentLang = sharedPrefLang.getString("lang_mode", "id") ?: "id"

        when (currentLang) {
            "id" -> binding.rbLangId.isChecked = true
            "en" -> binding.rbLangEn.isChecked = true
            else -> binding.rbLangSystem.isChecked = true
        }

        binding.rgLanguage.setOnCheckedChangeListener { _, checkedId ->
            val selectedLang = when (checkedId) {
                R.id.rbLangId -> "id"
                R.id.rbLangEn -> "en"
                else -> "system"
            }

            if (selectedLang == "system") {
                sharedPrefLang.edit().remove("lang_mode").apply()
            } else {
                sharedPrefLang.edit().putString("lang_mode", selectedLang).apply()
            }

            applyAppLanguage(selectedLang)
            Toast.makeText(this, getString(R.string.language_changed_toast), Toast.LENGTH_SHORT).show()
        }
    }

    private fun applyAppLanguage(selectedLang: String) {
        val locale = if (selectedLang == "system") {
            Configuration(resources.configuration).locales[0]
        } else {
            Locale(selectedLang)
        }

        Locale.setDefault(locale)
        recreate() // Reload activity to apply changes
    }
}
