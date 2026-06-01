package com.citra.penjualan

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

open class BaseActivity : AppCompatActivity() {

    private var currentLangCode: String = "id"

    override fun attachBaseContext(newBase: Context) {
        val sharedPrefLang = newBase.getSharedPreferences("lang_prefs", Context.MODE_PRIVATE)
        val langCode = sharedPrefLang.getString("lang_mode", "id") ?: "id"
        
        val locale = if (langCode == "system") {
            Configuration(newBase.resources.configuration).locales[0]
        } else {
            Locale(langCode)
        }
        
        Locale.setDefault(locale)
        val config = Configuration(newBase.resources.configuration)
        config.setLocale(locale)
        
        val context = newBase.createConfigurationContext(config)
        super.attachBaseContext(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sharedPrefLang = getSharedPreferences("lang_prefs", Context.MODE_PRIVATE)
        currentLangCode = sharedPrefLang.getString("lang_mode", "id") ?: "id"
    }

    override fun onResume() {
        super.onResume()
        val sharedPrefLang = getSharedPreferences("lang_prefs", Context.MODE_PRIVATE)
        val savedLangCode = sharedPrefLang.getString("lang_mode", "id") ?: "id"
        if (currentLangCode != savedLangCode) {
            recreate()
        }
    }

    fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    // Helper functions untuk role-based access control
    fun getUserRole(): String {
        return getSharedPreferences("user_session", Context.MODE_PRIVATE)
            .getString("user_role", "pemilik") ?: "pemilik"
    }

    fun getUserJabatan(): String {
        return getSharedPreferences("user_session", Context.MODE_PRIVATE)
            .getString("user_jabatan", "Pemilik") ?: "Pemilik"
    }

    fun isPemilik(): Boolean = getUserRole() == "pemilik"
    fun isAdmin(): Boolean = getUserJabatan().equals("admin", ignoreCase = true)
    fun isSupervisor(): Boolean = getUserJabatan().equals("supervisor", ignoreCase = true)
    fun isKasir(): Boolean = getUserJabatan().contains("kasir", ignoreCase = true)
    fun isGudang(): Boolean = getUserJabatan().contains("gudang", ignoreCase = true)

    // Hak akses per fitur
    fun canAccessTransaksi(): Boolean {
        return isPemilik() || isKasir()
    }

    fun canAccessProdukCRUD(): Boolean {
        return isPemilik() || isAdmin() || isGudang()
    }

    fun canAccessProdukReadOnly(): Boolean {
        return isSupervisor()
    }

    fun canAccessKategoriCRUD(): Boolean {
        return isPemilik() || isAdmin() || isGudang()
    }

    fun canAccessKategoriReadOnly(): Boolean {
        return isSupervisor()
    }

    fun canAccessPelangganCRUD(): Boolean {
        return isPemilik() || isAdmin() || isSupervisor() || isKasir()
    }

    fun canAccessLaporan(): Boolean {
        return isPemilik() || isAdmin() || isSupervisor()
    }

    fun canAccessPegawai(): Boolean {
        return isPemilik() || isAdmin()
    }

    fun canAccessCabang(): Boolean {
        return isPemilik() || isAdmin()
    }

    fun canAccessSettings(): Boolean {
        return isPemilik()
    }
}
