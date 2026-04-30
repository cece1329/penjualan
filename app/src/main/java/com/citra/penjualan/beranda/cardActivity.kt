package com.citra.penjualan.beranda

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.citra.penjualan.R
import com.citra.penjualan.kategori.DataKategoriActivity
import com.citra.penjualan.produk.DataProdukActivity // 1. IMPORT WAJIB BIAR NGGAK MERAH

class cardActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_card)

        // Inisialisasi View
        val btnEstimasi: TextView = findViewById(R.id.btnestimasi)
        val progressBar: ProgressBar = findViewById(R.id.progressBar)

        // Data dummy untuk Progress Bar
        val totalPenjualan = 150000
        val targetPenjualan = 1000000
        btnEstimasi.text = "Rp $totalPenjualan"

        val progressStatus = (totalPenjualan.toFloat() / targetPenjualan.toFloat() * 100).toInt()
        progressBar.progress = progressStatus

        // --- LOGIKA TOMBOL (CLICK LISTENERS) ---

        // 1. TOMBOL PRODUK (SUDAH AKTIF PINDAH HALAMAN)
        findViewById<ImageView>(R.id.btnproduct).setOnClickListener {
            val intent = Intent(this, DataProdukActivity::class.java)
            startActivity(intent)
        }

        // 2. TOMBOL KATEGORI (PINDAH HALAMAN)
        findViewById<ImageView>(R.id.btnkategori).setOnClickListener {
            val intent = Intent(this, DataKategoriActivity::class.java)
            startActivity(intent)
        }

        // --- MENU LAINNYA (BISA KAMU ISI NANTI) ---

        findViewById<ImageView>(R.id.btnSettings).setOnClickListener {
            Toast.makeText(this, "Membuka Pengaturan", Toast.LENGTH_SHORT).show()
        }

        findViewById<ImageView>(R.id.btntransaction).setOnClickListener {
            Toast.makeText(this, "Mulai Transaksi Baru", Toast.LENGTH_SHORT).show()
        }

        findViewById<ImageView>(R.id.btncust).setOnClickListener {
            Toast.makeText(this, "Daftar Pelanggan", Toast.LENGTH_SHORT).show()
        }

        findViewById<ImageView>(R.id.btnreport).setOnClickListener {
            Toast.makeText(this, "Laporan Penjualan", Toast.LENGTH_SHORT).show()
        }

        findViewById<ImageView>(R.id.btnacc).setOnClickListener {
            Toast.makeText(this, "Profil Akun", Toast.LENGTH_SHORT).show()
        }

        findViewById<ImageView>(R.id.employee).setOnClickListener {
            Toast.makeText(this, "Manajemen Pegawai", Toast.LENGTH_SHORT).show()
        }

        findViewById<ImageView>(R.id.btnbranch).setOnClickListener {
            Toast.makeText(this, "Informasi Cabang", Toast.LENGTH_SHORT).show()
        }

        findViewById<ImageView>(R.id.btnprinter).setOnClickListener {
            Toast.makeText(this, "Cek Koneksi Printer", Toast.LENGTH_SHORT).show()
        }
    }
}