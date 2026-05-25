package com.citra.penjualan.printer

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.citra.penjualan.R
import com.citra.penjualan.databinding.ActivityPrinterBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.appcompat.app.AppCompatActivity

class PrinterActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPrinterBinding
    private var bluetoothAdapter: BluetoothAdapter? = null

    // Request permission launcher untuk menangani izin Bluetooth di Android 12+
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            updateStatusUI()
        } else {
            Toast.makeText(this, "Izin Bluetooth diperlukan untuk fitur ini", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 1. Inisialisasi View Binding (PENTING: ini akan menghapus error 'Unresolved reference')
        binding = ActivityPrinterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 2. Inisialisasi Bluetooth Adapter
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        setupUI()
        updateStatusUI()
    }

    private fun setupUI() {
        // Setup Title
        binding.tvTitle.text = "Pengaturan Printer"

        // Tombol Kembali
        binding.btnBack.setOnClickListener { finish() }

        // Tombol Hubungkan Printer
        binding.btnConnectPrinter.setOnClickListener {
            if (checkBluetoothPermissions()) {
                openBluetoothSettings()
            }
        }

        // Tombol Test Print
        binding.btnPrintTest.setOnClickListener {
            if (checkBluetoothPermissions()) {
                val dummyData = ReceiptData(
                    toko = "TOKO TEST PRINT",
                    alamat = "Jl. Contoh Alamat No. 123",
                    cabang = "Cabang Pusat",
                    kasir = "Admin",
                    tanggal = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()).format(Date()),
                    idTransaksi = "TEST-001",
                    jumlah = 1,
                    totalHarga = 0
                )
                ReceiptPdfPrinter(this).printToBluetooth(dummyData)
            }
        }
    }

    private fun openBluetoothSettings() {
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth tidak didukung di perangkat ini", Toast.LENGTH_SHORT).show()
            return
        }

        if (!bluetoothAdapter!!.isEnabled) {
            Toast.makeText(this, "Silakan aktifkan Bluetooth", Toast.LENGTH_SHORT).show()
        }

        // Membuka pengaturan Bluetooth HP agar user bisa pairing printer
        try {
            startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
            Toast.makeText(this, "Pasangkan (Pair) printer Anda di menu ini", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Gagal membuka pengaturan: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkBluetoothPermissions(): Boolean {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            arrayOf(Manifest.permission.BLUETOOTH, Manifest.permission.ACCESS_FINE_LOCATION)
        }

        val notGranted = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        return if (notGranted.isNotEmpty()) {
            requestPermissionLauncher.launch(notGranted.toTypedArray())
            false
        } else true
    }

    private fun updateStatusUI() {
        if (bluetoothAdapter?.isEnabled == true) {
            binding.tvStatusPrinter.text = "Bluetooth Aktif"
            binding.tvStatusPrinter.setTextColor(ContextCompat.getColor(this, android.R.color.black))
            binding.imgPrinterState.setImageResource(R.drawable.printer)
        } else {
            binding.tvStatusPrinter.text = "Bluetooth Tidak Aktif"
            binding.tvStatusPrinter.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
        }
    }
}