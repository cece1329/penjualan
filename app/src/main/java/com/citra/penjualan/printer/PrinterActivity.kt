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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.citra.penjualan.R
import com.citra.penjualan.databinding.ActivityPrinterBinding
import com.citra.penjualan.databinding.ItemLaporanBinding
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class PrinterActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPrinterBinding
    private var bluetoothAdapter: BluetoothAdapter? = null
    private val dbTransaksi = FirebaseDatabase.getInstance().getReference("transaksi")
    private val transaksiList = ArrayList<ReceiptData>()
    private lateinit var adapter: PrinterTransaksiAdapter

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            updateStatusUI()
        } else {
            Toast.makeText(this, "Izin Bluetooth diperlukan", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPrinterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        setupRecyclerView()
        setupUI()
        loadTransaksi()
        updateStatusUI()
    }

    private fun setupRecyclerView() {
        adapter = PrinterTransaksiAdapter(transaksiList, 
            onPrint = { receipt ->
                if (checkBluetoothPermissions()) {
                    ReceiptPdfPrinter(this).printToBluetooth(receipt)
                }
            },
            onDelete = { receipt ->
                showDeleteDialog(receipt)
            }
        )
        binding.rvTransactions.layoutManager = LinearLayoutManager(this)
        binding.rvTransactions.adapter = adapter
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener { finish() }
        binding.btnConnectPrinter.setOnClickListener {
            if (checkBluetoothPermissions()) {
                openBluetoothSettings()
            }
        }

        binding.btnPrintTest.setOnClickListener {
            if (checkBluetoothPermissions()) {
                val dummyData = ReceiptData(
                    toko = "BAKED LOVE",
                    alamat = "Halaman Uji Printer",
                    tanggal = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()).format(Date()),
                    idTransaksi = "TEST-PRINT",
                    jumlah = 1,
                    totalHarga = 0,
                    kasir = "Sistem",
                    items = listOf(ReceiptItem("Test Item", 1, 0))
                )
                ReceiptPdfPrinter(this).printToBluetooth(dummyData)
            }
        }
    }

    private fun loadTransaksi() {
        val today = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())
        
        dbTransaksi.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                transaksiList.clear()
                for (data in snapshot.children) {
                    val tanggalFull = data.child("tanggal").value?.toString() ?: ""
                    val isPrinted = data.child("isPrinted").getValue(Boolean::class.java) ?: false
                    
                    // Menampilkan transaksi hari ini yang belum dicetak
                    if (!isPrinted && tanggalFull.startsWith(today)) {
                        val id = data.child("idTransaksi").value?.toString() ?: data.key
                        val toko = data.child("namaToko").value?.toString() ?: "BAKED LOVE"
                        val alamat = data.child("alamatToko").value?.toString()
                        val kasir = data.child("namaKasir").value?.toString()
                        val total = data.child("totalHarga").value?.toString()?.toIntOrNull() ?: 0
                        val jumlah = data.child("jumlah").value?.toString()?.toIntOrNull() ?: 0
                        val metode = data.child("metodePembayaran").value?.toString()
                        val uangDiterima = data.child("uangDiterima").value?.toString()?.toIntOrNull()
                        val kembalian = data.child("kembalian").value?.toString()?.toIntOrNull()
                        val noGopay = data.child("noGopay").value?.toString()
                        val namaPlg = data.child("namaPelanggan").value?.toString()
                        val jenisPlg = data.child("jenisPelanggan").value?.toString()

                        val itemsList = ArrayList<ReceiptItem>()
                        data.child("items").children.forEach { itemSnap ->
                            itemsList.add(ReceiptItem(
                                namaProduk = itemSnap.child("namaProduk").value?.toString() ?: "-",
                                jumlah = itemSnap.child("jumlah").value?.toString()?.toIntOrNull() ?: 0,
                                harga = itemSnap.child("harga").value?.toString()?.toIntOrNull() ?: 0
                            ))
                        }

                        transaksiList.add(ReceiptData(
                            toko = toko, alamat = alamat, tanggal = tanggalFull, idTransaksi = id,
                            items = itemsList, jumlah = jumlah, totalHarga = total,
                            metodePembayaran = metode, uangDiterima = uangDiterima, kembalian = kembalian,
                            noGopay = noGopay, namaPelanggan = namaPlg, jenisPelanggan = jenisPlg,
                            kasir = kasir, isPrinted = isPrinted
                        ))
                    }
                }
                transaksiList.reverse()
                adapter.notifyDataSetChanged()
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun showDeleteDialog(receipt: ReceiptData) {
        AlertDialog.Builder(this)
            .setTitle("Hapus dari Antrean")
            .setMessage("Transaksi ini akan ditandai sebagai 'Sudah Dicetak' dan hilang dari daftar ini. Data laporan tetap tersimpan.")
            .setPositiveButton("Hapus") { _, _ ->
                receipt.idTransaksi?.let { id ->
                    dbTransaksi.child(id).child("isPrinted").setValue(true).addOnSuccessListener {
                        Toast.makeText(this, "Berhasil dihapus dari antrean", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun openBluetoothSettings() {
        try {
            startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
            Toast.makeText(this, "Pasangkan printer di menu Bluetooth HP", Toast.LENGTH_LONG).show()
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
            binding.tvStatusPrinter.setTextColor(ContextCompat.getColor(this, R.color.textColorTitle))
        } else {
            binding.tvStatusPrinter.text = "Bluetooth Tidak Aktif"
            binding.tvStatusPrinter.setTextColor(ContextCompat.getColor(this, R.color.errorColor))
        }
    }
}

private class PrinterTransaksiAdapter(
    private val list: List<ReceiptData>,
    private val onPrint: (ReceiptData) -> Unit,
    private val onDelete: (ReceiptData) -> Unit
) : RecyclerView.Adapter<PrinterTransaksiAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemLaporanBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemLaporanBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val data = list[position]
        with(holder.binding) {
            tvNamaProduk.text = "Nota #${data.idTransaksi?.takeLast(8)}"
            tvTotalHarga.text = "Rp ${String.format("%,d", data.totalHarga).replace(",", ".")}"
            tvQty.text = "${data.jumlah} Item • ${data.metodePembayaran ?: "Cash"}"
            tvTanggal.text = data.tanggal
            
            root.setOnClickListener { onPrint(data) }
            root.setOnLongClickListener {
                onDelete(data)
                true
            }
        }
    }

    override fun getItemCount(): Int = list.size
}
