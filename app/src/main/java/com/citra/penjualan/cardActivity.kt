package com.citra.penjualan

import android.os.Bundle
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class cardActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_card)


        val btnEstimasi: TextView = findViewById(R.id.btnestimasi)
        val progressBar: ProgressBar = findViewById(R.id.progressBar)

        val totalPenjualan = 150000
        val targetPenjualan = 1000000


        btnEstimasi.text = "Rp $totalPenjualan"


        val progressStatus = (totalPenjualan.toFloat() / targetPenjualan.toFloat() * 100).toInt()
        progressBar.progress = progressStatus


        // SETTING
        findViewById<ImageView>(R.id.btnSettings).setOnClickListener {
            Toast.makeText(this, "Membuka Pengaturan", Toast.LENGTH_SHORT).show()
        }


        // MENU BARIS ATAS (Dashboard Utama)
        findViewById<ImageView>(R.id.btntransaction).setOnClickListener {
            Toast.makeText(this, "Mulai Transaksi Baru", Toast.LENGTH_SHORT).show()
        }

        findViewById<ImageView>(R.id.btncust).setOnClickListener {
            Toast.makeText(this, "Daftar Pelanggan", Toast.LENGTH_SHORT).show()
        }

        findViewById<ImageView>(R.id.btnreport).setOnClickListener {
            Toast.makeText(this, "Laporan Penjualan", Toast.LENGTH_SHORT).show()
        }


        // MENU BARIS TENGAH
        findViewById<ImageView>(R.id.btnacc).setOnClickListener {
            Toast.makeText(this, "Profil Akun", Toast.LENGTH_SHORT).show()
        }

        findViewById<ImageView>(R.id.btnproduct).setOnClickListener {
            Toast.makeText(this, "Daftar Produk", Toast.LENGTH_SHORT).show()
        }

        findViewById<ImageView>(R.id.btnplus).setOnClickListener {
            Toast.makeText(this, "Menu Tambahan", Toast.LENGTH_SHORT).show()
        }


        //  MENU BARIS BAWAH
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