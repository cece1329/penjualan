package com.citra.penjualan.printer

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.citra.penjualan.databinding.ActivityPrinterBinding
import com.citra.penjualan.model.ModelTransaksi
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class PrinterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPrinterBinding
    private var isConnected = false
    
    private val dbTransaksi = FirebaseDatabase.getInstance().getReference("transaksi")
    private val transaksiList = ArrayList<ModelTransaksi>()
    private var selectedTransaksi: ModelTransaksi? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPrinterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnConnectPrinter.setOnClickListener {
            toggleConnection()
        }

        binding.btnPrintTest.setOnClickListener {
            if (selectedTransaksi != null) {
                Toast.makeText(this, "Menyiapkan PDF Struk...", Toast.LENGTH_SHORT).show()
                printSelectedTransaction()
            } else {
                Toast.makeText(this, "Pilih transaksi terlebih dahulu!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadTransaksi() {
        binding.tvPilihTransaksi.visibility = View.VISIBLE
        binding.spinnerTransaksi.visibility = View.VISIBLE
        
        dbTransaksi.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                transaksiList.clear()
                val displayList = ArrayList<String>()

                for (data in snapshot.children) {
                    val transaksi = data.getValue(ModelTransaksi::class.java)
                    if (transaksi != null) {
                        transaksi.idTransaksi = data.key
                        transaksiList.add(transaksi)
                        displayList.add("${transaksi.tanggal} - ${transaksi.namaProduk} (Rp ${transaksi.totalHarga})")
                    }
                }

                if (transaksiList.isEmpty()) {
                    displayList.add("Belum ada transaksi")
                    binding.btnPrintTest.isEnabled = false
                } else {
                    binding.btnPrintTest.isEnabled = true
                }

                val adapter = ArrayAdapter(this@PrinterActivity, android.R.layout.simple_spinner_item, displayList)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.spinnerTransaksi.adapter = adapter

                binding.spinnerTransaksi.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                        if (transaksiList.isNotEmpty() && position < transaksiList.size) {
                            selectedTransaksi = transaksiList[position]
                        }
                    }
                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@PrinterActivity, "Gagal memuat transaksi: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
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
                    android.content.res.ColorStateList.valueOf(Color.parseColor("#EF5350"))
                )

                binding.tvStatusPrinter.text = "Status: Siap Mencetak PDF"
                binding.imgPrinterState.setColorFilter(Color.parseColor("#66BB6A"))
                
                // Load and show transactions when connected
                loadTransaksi()

                Toast.makeText(this, "Sistem pencetak PDF siap digunakan!", Toast.LENGTH_SHORT).show()
            }, 1500)
        } else {
            isConnected = false
            binding.btnConnectPrinter.text = "Hubungkan Sistem Pencetak"
            binding.btnConnectPrinter.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(Color.parseColor("#BA68C8"))
            )

            binding.tvStatusPrinter.text = "Status: Terputus"
            binding.imgPrinterState.setColorFilter(Color.parseColor("#EF5350"))
            binding.btnPrintTest.isEnabled = false
            
            // Hide transactions
            binding.tvPilihTransaksi.visibility = View.GONE
            binding.spinnerTransaksi.visibility = View.GONE
            selectedTransaksi = null

            Toast.makeText(this, "Sistem pencetakan dinonaktifkan.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun printSelectedTransaction() {
        val transaksi = selectedTransaksi ?: return
        
        try {
            val printer = ReceiptPdfPrinter(this)
            val receipt = ReceiptPdfPrinter.ReceiptData(
                toko = "Toko Citra",
                tanggal = transaksi.tanggal ?: "-",
                idTransaksi = transaksi.idTransaksi,
                namaProduk = transaksi.namaProduk ?: "-",
                jumlah = transaksi.jumlah,
                totalHarga = transaksi.totalHarga
            )
            printer.printToPdf(receipt)
        } catch (e: Exception) {
            Toast.makeText(this, "Gagal memproses struk PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
