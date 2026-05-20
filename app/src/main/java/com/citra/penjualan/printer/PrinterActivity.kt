package com.citra.penjualan.printer

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.citra.penjualan.R
import com.citra.penjualan.databinding.ActivityPrinterBinding

class PrinterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPrinterBinding
    private var isConnected = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPrinterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnConnectPrinter.setOnClickListener {
            toggleConnection()
        }

        binding.btnPrintTest.setOnClickListener {
            Toast.makeText(this, "Sedang mencetak struk percobaan...", Toast.LENGTH_SHORT).show()
        }
    }

    private fun toggleConnection() {
        if (!isConnected) {
            binding.btnConnectPrinter.text = "Menghubungkan..."
            binding.btnConnectPrinter.isEnabled = false

            Handler(Looper.getMainLooper()).postDelayed({
                isConnected = true
                binding.btnConnectPrinter.text = "Putuskan Koneksi"
                binding.btnConnectPrinter.isEnabled = true
                binding.btnConnectPrinter.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(Color.parseColor("#F44336"))
                )

                binding.tvStatusPrinter.text = "Status: Terhubung (Bluetooth)"
                binding.imgPrinterState.setColorFilter(Color.parseColor("#4CAF50"))
                binding.btnPrintTest.isEnabled = true

                Toast.makeText(this, "Printer berhasil terhubung!", Toast.LENGTH_SHORT).show()
            }, 1500)
        } else {
            isConnected = false
            binding.btnConnectPrinter.text = "Hubungkan Printer"
            binding.btnConnectPrinter.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(Color.parseColor("#2196F3"))
            )

            binding.tvStatusPrinter.text = "Status: Terputus"
            binding.imgPrinterState.setColorFilter(Color.parseColor("#F44336"))
            binding.btnPrintTest.isEnabled = false

            Toast.makeText(this, "Koneksi printer terputus.", Toast.LENGTH_SHORT).show()
        }
    }
}
