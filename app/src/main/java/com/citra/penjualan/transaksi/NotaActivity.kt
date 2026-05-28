package com.citra.penjualan.transaksi

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.citra.penjualan.databinding.ActivityNotaBinding
import com.citra.penjualan.printer.ReceiptData
import com.citra.penjualan.printer.ReceiptPdfPrinter

class NotaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNotaBinding
    private var receiptData: ReceiptData? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        receiptData = intent.getParcelableExtra("EXTRA_RECEIPT")

        if (receiptData == null) {
            Toast.makeText(this, "Data nota tidak ditemukan", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupUI()

        binding.btnBack.setOnClickListener { finish() }
        
        binding.btnShare.setOnClickListener {
            receiptData?.let { ReceiptPdfPrinter(this).shareAsPdf(it) }
        }

        // Fix: Langsung hubungkan ke printer Bluetooth saat tombol Cetak ditekan
        binding.btnPrint.setOnClickListener {
            receiptData?.let { 
                ReceiptPdfPrinter(this).printToBluetooth(it) 
            }
        }
    }

    private fun setupUI() {
        receiptData?.let {
            val printer = ReceiptPdfPrinter(this)
            binding.tvNotaContent.text = printer.buildTextReceipt(it)
        }
    }
}
