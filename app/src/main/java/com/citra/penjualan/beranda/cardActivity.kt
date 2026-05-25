package com.citra.penjualan.beranda

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.citra.penjualan.R
import com.citra.penjualan.akun.AkunActivity
import com.citra.penjualan.cabang.CabangActivity
import com.citra.penjualan.kategori.DataKategoriActivity
import com.citra.penjualan.laporan.LaporanActivity
import com.citra.penjualan.pegawai.PegawaiActivity
import com.citra.penjualan.pelanggan.PelangganActivity
import com.citra.penjualan.printer.PrinterActivity
import com.citra.penjualan.produk.DataProdukActivity
import com.citra.penjualan.transaksi.TransaksiActivity
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class cardActivity : AppCompatActivity() {

    private val dbSettings = FirebaseDatabase.getInstance().getReference("settings")
    private val dbTransaksi = FirebaseDatabase.getInstance().getReference("transaksi")
    private val dbProfil = FirebaseDatabase.getInstance().getReference("profil")
    private var targetPenjualanHarian = 1000000
    private var totalPenjualanHariIni = 0
    private var totalTransaksiHariIni = 0
    private var namaToko = "Citra Penjualan"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_card)

        // Set dynamic today's date in Indonesian
        try {
            val calendar = java.util.Calendar.getInstance()
            val dateFormat = java.text.SimpleDateFormat("dd MMMM yyyy", java.util.Locale("id", "ID"))
            val todayDate = dateFormat.format(calendar.time)
            findViewById<TextView>(R.id.tvDate).text = todayDate
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Check user session
        val sharedPref = getSharedPreferences("user_session", Context.MODE_PRIVATE)
        val role = sharedPref.getString("user_role", "pemilik") ?: "pemilik"
        val name = sharedPref.getString("user_name", "Citra") ?: "Citra"
        val cabang = sharedPref.getString("user_cabang", "") ?: ""

        // Set dynamic welcome message
        val tvWelcome: TextView = findViewById(R.id.tvWelcome)
        if (role == "karyawan") {
            tvWelcome.text = "Selamat datang, $name"
            findViewById<TextView>(R.id.tvWorkInfo)?.apply {
                visibility = android.view.View.VISIBLE
                text = "Anda sekarang bekerja di toko $namaToko cabang ${cabang.ifBlank { "-" }}"
            }
            
            // Hide Owner-only widgets
            findViewById<android.view.View>(R.id.rowEstimasi)?.visibility = android.view.View.GONE
            findViewById<android.view.View>(R.id.containerTargetPenjualan)?.visibility = android.view.View.GONE
            findViewById<android.view.View>(R.id.containerReport)?.visibility = android.view.View.GONE
            findViewById<android.view.View>(R.id.containerEmployee)?.visibility = android.view.View.GONE
            findViewById<android.view.View>(R.id.containerBranch)?.visibility = android.view.View.GONE
        } else {
            tvWelcome.text = getString(R.string.welcome_day) + ", " + name
        }
        loadStoreProfile(role, name, cabang)

        // Load Sales Estimation dynamically from Firebase (only for owner)
        if (role == "pemilik") {
            loadSalesEstimation()
            findViewById<android.view.View>(R.id.btnSetTarget)?.setOnClickListener {
                showTargetDialog()
            }
        }

        // --- BUTTON CLICK LISTENERS ---

        // 1. Settings (Language + Theme)
        findViewById<ImageView>(R.id.btnSettings).setOnClickListener {
            showSettingsDialog()
        }


        // 2. Transaksi
        findViewById<ImageView>(R.id.btntransaction).setOnClickListener {
            startActivity(Intent(this, TransaksiActivity::class.java))
        }

        // 3. Pelanggan
        findViewById<ImageView>(R.id.btncust).setOnClickListener {
            startActivity(Intent(this, PelangganActivity::class.java))
        }

        // 4. Laporan
        findViewById<ImageView>(R.id.btnreport).setOnClickListener {
            startActivity(Intent(this, LaporanActivity::class.java))
        }

        // 5. Akun
        findViewById<ImageView>(R.id.btnacc).setOnClickListener {
            startActivity(Intent(this, AkunActivity::class.java))
        }

        // 6. Produk
        findViewById<ImageView>(R.id.btnproduct).setOnClickListener {
            startActivity(Intent(this, DataProdukActivity::class.java))
        }

        // 7. Kategori
        findViewById<ImageView>(R.id.btnkategori).setOnClickListener {
            startActivity(Intent(this, DataKategoriActivity::class.java))
        }

        // 8. Pegawai
        findViewById<ImageView>(R.id.employee).setOnClickListener {
            startActivity(Intent(this, PegawaiActivity::class.java))
        }

        // 9. Cabang
        findViewById<ImageView>(R.id.btnbranch).setOnClickListener {
            startActivity(Intent(this, CabangActivity::class.java))
        }

        // 10. Printer
        findViewById<ImageView>(R.id.btnprinter).setOnClickListener {
            startActivity(Intent(this, PrinterActivity::class.java))
        }
    }

    private fun loadStoreProfile(role: String, fallbackName: String, cabang: String) {
        dbProfil.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val profileOwner = snapshot.child("namaPemilik").value?.toString().orEmpty()
                namaToko = snapshot.child("namaToko").value?.toString()?.takeIf { it.isNotBlank() } ?: "Citra Penjualan"

                if (role == "pemilik") {
                    findViewById<TextView>(R.id.tvWelcome)?.text =
                        getString(R.string.welcome_day) + ", " + profileOwner.ifBlank { fallbackName }
                    findViewById<TextView>(R.id.tvWorkInfo)?.visibility = android.view.View.GONE
                } else {
                    findViewById<TextView>(R.id.tvWelcome)?.text = "Selamat datang, $fallbackName"
                    findViewById<TextView>(R.id.tvWorkInfo)?.apply {
                        visibility = android.view.View.VISIBLE
                        text = "Anda sekarang bekerja di toko $namaToko cabang ${cabang.ifBlank { "-" }}"
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun loadSalesEstimation() {
        dbSettings.child("targetPenjualanHarian").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                targetPenjualanHarian = snapshot.getValue(Int::class.java) ?: 1000000
                updateSalesTargetUi()
            }

            override fun onCancelled(error: DatabaseError) {}
        })

        dbTransaksi.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var totalPenjualan = 0
                var countTransaksi = 0
                for (data in snapshot.children) {
                    val price = data.child("totalHarga").getValue(Int::class.java) ?: 0
                    val tanggal = data.child("tanggal").getValue(String::class.java).orEmpty()
                    if (isToday(tanggal)) {
                        totalPenjualan += price
                        countTransaksi++
                    }
                }

                totalPenjualanHariIni = totalPenjualan
                totalTransaksiHariIni = countTransaksi
                updateSalesTargetUi()
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun updateSalesTargetUi() {
        val safeTarget = if (targetPenjualanHarian <= 0) 1 else targetPenjualanHarian
        val progressStatus = ((totalPenjualanHariIni.toFloat() / safeTarget.toFloat()) * 100).toInt()

        findViewById<TextView>(R.id.btnestimasi)?.text = "Rp ${formatNumber(totalPenjualanHariIni)}"
        findViewById<TextView>(R.id.tvProgressPercent)?.text = "${progressStatus.coerceAtMost(100)}% Tercapai"
        findViewById<TextView>(R.id.tvTargetAmount)?.text = "Target: Rp ${formatNumber(targetPenjualanHarian)}"
        findViewById<ProgressBar>(R.id.progressBar)?.progress = progressStatus.coerceIn(0, 100)
        
        // Jika ada TextView untuk jumlah transaksi, bisa diisi di sini
        // findViewById<TextView>(R.id.tvTotalTransaksi)?.text = "${formatNumber(totalTransaksiHariIni)} Transaksi"
    }

    private fun showTargetDialog() {
        val input = android.widget.EditText(this)
        input.inputType = android.text.InputType.TYPE_CLASS_NUMBER
        input.hint = "Contoh: 1000000"
        input.setText(targetPenjualanHarian.toString())
        input.setSelection(input.text?.length ?: 0)
        input.setPadding(32, 18, 32, 18)

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Atur Target Harian")
            .setView(input)
            .setPositiveButton("Simpan") { _, _ ->
                val target = input.text?.toString()?.trim()?.toIntOrNull() ?: 0
                if (target <= 0) {
                    Toast.makeText(this, "Target harus lebih dari 0", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                dbSettings.child("targetPenjualanHarian").setValue(target).addOnSuccessListener {
                    Toast.makeText(this, "Target berhasil diperbarui", Toast.LENGTH_SHORT).show()
                }.addOnFailureListener {
                    Toast.makeText(this, getString(R.string.msg_failed, it.message), Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(getString(R.string.btn_cancel), null)
            .show()
    }

    private fun isToday(tanggal: String): Boolean {
        val transaksiDate = parseTransactionDate(tanggal) ?: return false
        val transaksiCal = Calendar.getInstance().apply { time = transaksiDate }
        val todayCal = Calendar.getInstance()
        return transaksiCal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR) &&
            transaksiCal.get(Calendar.DAY_OF_YEAR) == todayCal.get(Calendar.DAY_OF_YEAR)
    }

    private fun parseTransactionDate(tanggal: String): java.util.Date? {
        val locale = Locale.getDefault()
        val formats = listOf(
            SimpleDateFormat("dd-MM-yyyy HH:mm:ss", locale),
            SimpleDateFormat("dd/MM/yyyy HH:mm:ss", locale),
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", locale)
        )
        return formats.firstNotNullOfOrNull { format ->
            try {
                format.parse(tanggal)
            } catch (_: Exception) {
                null
            }
        }
    }

    private fun formatNumber(amount: Int): String {
        val s = amount.toString()
        val sb = StringBuilder()
        var count = 0
        for (i in s.length - 1 downTo 0) {
            if (count > 0 && count % 3 == 0) sb.insert(0, ".")
            sb.insert(0, s[i])
            count++
        }
        return sb.toString()
    }

    private fun showSettingsDialog() {
        // Dialog bahasa saja, tombol Settings juga tetap bisa mengakses tema lewat dialog berikutnya.
        val sharedPrefLang = getSharedPreferences("lang_prefs", Context.MODE_PRIVATE)
        val currentLang = sharedPrefLang.getString("lang_mode", "id") ?: "id"

        val languageOptions = arrayOf(
            getString(R.string.language_option_default),
            getString(R.string.language_option_id),
            getString(R.string.language_option_en)
        )

        val languageCheckedItem = when (currentLang) {
            "id" -> 1
            "en" -> 2
            else -> 0
        }

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(getString(R.string.language_dialog_title))
            .setSingleChoiceItems(languageOptions, languageCheckedItem) { dialog, which ->
                val selectedLang = when (which) {
                    1 -> "id"
                    2 -> "en"
                    else -> "system"
                }

                if (selectedLang == "system") {
                    sharedPrefLang.edit().remove("lang_mode").apply()
                } else {
                    sharedPrefLang.edit().putString("lang_mode", selectedLang).apply()
                }

                applyAppLanguage(selectedLang)
                Toast.makeText(this, getString(R.string.language_changed_toast), Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .setPositiveButton(getString(R.string.theme_dialog_title)) { dialog, _ ->
                dialog.dismiss()
                showThemeSelectionDialog()
            }
            .setNegativeButton(getString(R.string.language_cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun showThemeSelectionDialog() {
        val sharedPrefTheme = getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
        val currentMode = sharedPrefTheme.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)

        val themeOptions = arrayOf(
            getString(R.string.theme_option_default),
            getString(R.string.theme_option_light),
            getString(R.string.theme_option_dark)
        )

        val themeCheckedItem = when (currentMode) {
            AppCompatDelegate.MODE_NIGHT_NO -> 1
            AppCompatDelegate.MODE_NIGHT_YES -> 2
            else -> 0
        }

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(getString(R.string.theme_dialog_title))
            .setSingleChoiceItems(themeOptions, themeCheckedItem) { dialog, which ->
                val selectedMode = when (which) {
                    1 -> AppCompatDelegate.MODE_NIGHT_NO
                    2 -> AppCompatDelegate.MODE_NIGHT_YES
                    else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                }

                sharedPrefTheme.edit().putInt("theme_mode", selectedMode).apply()
                AppCompatDelegate.setDefaultNightMode(selectedMode)
                Toast.makeText(this, getString(R.string.theme_changed_toast), Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.theme_cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun applyAppLanguage(selectedLang: String) {
        val langCode = when (selectedLang) {
            "id" -> "id"
            "en" -> "en"
            else -> null
        } ?: return

        val locale = java.util.Locale(langCode)
        java.util.Locale.setDefault(locale)

        val config = resources.configuration
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)

        recreate()
    }

}
