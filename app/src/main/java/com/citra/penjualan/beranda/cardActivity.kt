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

class cardActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_card)

        // Load Sales Estimation dynamically from Firebase
        loadSalesEstimation()

        // --- BUTTON CLICK LISTENERS ---

        // 1. Settings (Night Mode Toggle Dialog)
        findViewById<ImageView>(R.id.btnSettings).setOnClickListener {
            showThemeSelectionDialog()
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

    private fun loadSalesEstimation() {
        val dbTransaksi = FirebaseDatabase.getInstance().getReference("transaksi")
        dbTransaksi.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var totalPenjualan = 0
                for (data in snapshot.children) {
                    val price = data.child("totalHarga").getValue(Int::class.java) ?: 0
                    totalPenjualan += price
                }

                val btnEstimasi: TextView = findViewById(R.id.btnestimasi)
                btnEstimasi.text = "Rp $totalPenjualan"

                val targetPenjualan = 1000000
                val progressStatus = (totalPenjualan.toFloat() / targetPenjualan.toFloat() * 100).toInt()
                val progressBar: ProgressBar = findViewById(R.id.progressBar)
                progressBar.progress = if (progressStatus > 100) 100 else progressStatus
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun showThemeSelectionDialog() {
        val sharedPref = getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
        val currentMode = sharedPref.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)

        val options = arrayOf("Default Sistem", "Mode Terang (Light)", "Mode Gelap (Dark)")
        val checkedItem = when (currentMode) {
            AppCompatDelegate.MODE_NIGHT_NO -> 1
            AppCompatDelegate.MODE_NIGHT_YES -> 2
            else -> 0
        }

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Pilih Tema")
            .setSingleChoiceItems(options, checkedItem) { dialog, which ->
                val selectedMode = when (which) {
                    1 -> AppCompatDelegate.MODE_NIGHT_NO
                    2 -> AppCompatDelegate.MODE_NIGHT_YES
                    else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                }

                sharedPref.edit().putInt("theme_mode", selectedMode).apply()
                AppCompatDelegate.setDefaultNightMode(selectedMode)
                dialog.dismiss()

                Toast.makeText(this, "Tema berhasil diubah", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Batal") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
}