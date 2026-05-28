package com.citra.penjualan.beranda

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
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
import com.citra.penjualan.akun.LoginActivity
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

        setupDate()
        
        val sharedPref = getSharedPreferences("user_session", Context.MODE_PRIVATE)
        val role = sharedPref.getString("user_role", "pemilik") ?: "pemilik"
        val name = sharedPref.getString("user_name", "Citra") ?: "Citra"
        val cabang = sharedPref.getString("user_cabang", "") ?: ""

        setupWelcomeHeader(role, name, cabang)
        loadStoreProfile(role, name, cabang)

        if (role == "pemilik") {
            loadSalesEstimation()
            findViewById<android.view.View>(R.id.btnSetTarget)?.setOnClickListener {
                showTargetDialog()
            }
        }

        setupClickListeners()
    }

    private fun setupDate() {
        try {
            val calendar = Calendar.getInstance()
            // Ensure date is always in system locale or preferred locale
            val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
            findViewById<TextView>(R.id.tvDate).text = dateFormat.format(calendar.time)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupWelcomeHeader(role: String, name: String, cabang: String) {
        val tvWelcome: TextView = findViewById(R.id.tvWelcome)
        if (role == "karyawan") {
            tvWelcome.text = "Selamat datang, $name"
            findViewById<TextView>(R.id.tvWorkInfo)?.apply {
                visibility = android.view.View.VISIBLE
                text = "Bekerja di $namaToko cabang ${cabang.ifBlank { "-" }}"
            }
            
            findViewById<android.view.View>(R.id.rowEstimasi)?.visibility = android.view.View.GONE
            findViewById<android.view.View>(R.id.containerTargetPenjualan)?.visibility = android.view.View.GONE
            findViewById<android.view.View>(R.id.containerReport)?.visibility = android.view.View.GONE
            findViewById<android.view.View>(R.id.containerEmployee)?.visibility = android.view.View.GONE
            findViewById<android.view.View>(R.id.containerBranch)?.visibility = android.view.View.GONE
        } else {
            tvWelcome.text = getString(R.string.welcome_day) + ", " + name
        }
    }

    private fun setupClickListeners() {
        // Updated: Open SettingsActivity instead of Dialog
        findViewById<ImageView>(R.id.btnSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        findViewById<android.view.View>(R.id.btntransaction).setOnClickListener {
            startActivity(Intent(this, TransaksiActivity::class.java))
        }

        findViewById<android.view.View>(R.id.btncust).setOnClickListener {
            startActivity(Intent(this, PelangganActivity::class.java))
        }

        findViewById<android.view.View>(R.id.btnreport).setOnClickListener {
            startActivity(Intent(this, LaporanActivity::class.java))
        }

        findViewById<android.view.View>(R.id.btnacc_container)?.setOnClickListener {
            startActivity(Intent(this, AkunActivity::class.java))
        } ?: findViewById<ImageView>(R.id.btnacc).setOnClickListener {
            startActivity(Intent(this, AkunActivity::class.java))
        }

        findViewById<android.view.View>(R.id.btnproduct_container)?.setOnClickListener {
            startActivity(Intent(this, DataProdukActivity::class.java))
        } ?: findViewById<ImageView>(R.id.btnproduct).setOnClickListener {
            startActivity(Intent(this, DataProdukActivity::class.java))
        }

        findViewById<android.view.View>(R.id.btnkategori_container)?.setOnClickListener {
            startActivity(Intent(this, DataKategoriActivity::class.java))
        } ?: findViewById<ImageView>(R.id.btnkategori).setOnClickListener {
            startActivity(Intent(this, DataKategoriActivity::class.java))
        }

        findViewById<android.view.View>(R.id.containerEmployee)?.setOnClickListener {
            startActivity(Intent(this, PegawaiActivity::class.java))
        } ?: findViewById<ImageView>(R.id.employee).setOnClickListener {
            startActivity(Intent(this, PegawaiActivity::class.java))
        }

        findViewById<android.view.View>(R.id.containerBranch)?.setOnClickListener {
            startActivity(Intent(this, CabangActivity::class.java))
        } ?: findViewById<ImageView>(R.id.btnbranch).setOnClickListener {
            startActivity(Intent(this, CabangActivity::class.java))
        }

        findViewById<android.view.View>(R.id.btnprinter_container)?.setOnClickListener {
            startActivity(Intent(this, PrinterActivity::class.java))
        } ?: findViewById<ImageView>(R.id.btnprinter).setOnClickListener {
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
                } else {
                    findViewById<TextView>(R.id.tvWelcome)?.text = "Selamat datang, $fallbackName"
                    findViewById<TextView>(R.id.tvWorkInfo)?.text = "Bekerja di $namaToko cabang ${cabang.ifBlank { "-" }}"
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
    }

    private fun showTargetDialog() {
        val input = android.widget.EditText(this)
        input.inputType = android.text.InputType.TYPE_CLASS_NUMBER
        input.setText(targetPenjualanHarian.toString())
        input.setPadding(32, 18, 32, 18)

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Atur Target Harian")
            .setView(input)
            .setPositiveButton("Simpan") { _, _ ->
                val target = input.text?.toString()?.trim()?.toIntOrNull() ?: 0
                if (target > 0) {
                    dbSettings.child("targetPenjualanHarian").setValue(target)
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun isToday(tanggal: String): Boolean {
        val formats = listOf("dd-MM-yyyy HH:mm:ss", "dd/MM/yyyy HH:mm:ss", "yyyy-MM-dd HH:mm:ss")
        val today = Calendar.getInstance()
        return formats.any { fmt ->
            try {
                val date = SimpleDateFormat(fmt, Locale.getDefault()).parse(tanggal)
                val cal = Calendar.getInstance().apply { time = date }
                cal.get(Calendar.YEAR) == today.get(Calendar.YEAR) && 
                cal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
            } catch (_: Exception) { false }
        }
    }

    private fun formatNumber(amount: Int): String {
        return String.format("%,d", amount).replace(",", ".")
    }
}
