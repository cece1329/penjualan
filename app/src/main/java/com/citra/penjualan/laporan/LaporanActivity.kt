package com.citra.penjualan.laporan

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.citra.penjualan.R
import com.citra.penjualan.databinding.ActivityLaporanBinding
import com.google.android.material.datepicker.MaterialDatePicker
import com.citra.penjualan.model.ModelTransaksi
import com.citra.penjualan.printer.ReceiptPdfPrinter
import com.citra.penjualan.printer.ReportTransaction
import com.citra.penjualan.printer.ReportSummary
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

// Data class lokal agar LaporanActivity mandiri dan tidak error jika model global belum diupdate
data class LaporanItem(
    val namaProduk: String? = null,
    val jumlah: Int = 0,
    val harga: Int = 0,
    val hargaBeli: Int? = null
)

data class LaporanTransaksi(
    var idTransaksi: String? = null,
    val tanggal: String? = null,
    val totalHarga: Int = 0,
    val metodePembayaran: String? = null,
    val items: List<LaporanItem>? = null
)

class LaporanActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLaporanBinding
    private val dbTransaksi = FirebaseDatabase.getInstance().getReference("transaksi")
    private var allTransactions = mutableListOf<LaporanTransaksi>()
    private var filteredTransactions = mutableListOf<LaporanTransaksi>()
    private lateinit var adapter: LaporanAdapter

    private var currentOmzet = 0
    private var currentLaba = 0
    private var currentProductMap = mutableMapOf<String, Int>()
    private var currentPaymentMap = mutableMapOf<String, Int>()
    private var currentFilterLabel = "Hari Ini"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLaporanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }

        // Perbaikan ukuran ikon arrow agar tetap 24dp (tidak kegedean)
        binding.toolbar.post {
            val icon = binding.toolbar.navigationIcon
            icon?.let {
                it.setBounds(0, 0, dp(24), dp(24))
                binding.toolbar.navigationIcon = it
            }
        }
        
        // Menghilangkan tint default agar logo CSV tampil dengan warna aslinya/asli drawable
        binding.fabExport.iconTint = null

        setupFilters()
        fetchData()

        // Fix: Gunakan ID fabExport langsung untuk cetak PDF
        binding.fabExport.setOnClickListener { printLaporanPDF() }
    }

    private fun setupFilters() {
        binding.chipGroupFilter.setOnCheckedChangeListener { group, checkedId ->
            when (checkedId) {
                R.id.chipToday -> filterByDateRange(0)
                R.id.chipWeekly -> filterByDateRange(7)
                R.id.chipMonthly -> filterByDateRange(30)
                R.id.chipCustom -> showDatePicker()
            }
        }
        binding.chipToday.isChecked = true
    }

    private fun fetchData() {
        dbTransaksi.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                allTransactions.clear()
                for (data in snapshot.children) {
                    val t = data.getValue(LaporanTransaksi::class.java)
                    if (t != null) {
                        t.idTransaksi = data.key
                        allTransactions.add(t)
                    }
                }
                applyCurrentFilter()
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun filterByDateRange(days: Int) {
        val calendar = Calendar.getInstance()
        val end = calendar.timeInMillis
        if (days > 0) calendar.add(Calendar.DAY_OF_YEAR, -days)
        val start = if (days == 0) getStartOfDay() else calendar.timeInMillis
        
        processFilter(start, end)
    }

    private fun processFilter(start: Long, end: Long) {
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        currentFilterLabel = "${sdf.format(Date(start))} - ${sdf.format(Date(end))}"
        filteredTransactions = allTransactions.filter { 
            val date = parseDate(it.tanggal) ?: 0L
            date in start..end
        }.toMutableList()
        updateUI()
    }

    private fun updateUI() {
        var omzet = 0
        var laba = 0
        val productMap = mutableMapOf<String, Int>()
        val paymentMap = mutableMapOf<String, Int>()

        filteredTransactions.forEach { t ->
            omzet += t.totalHarga
            val methodKey = t.metodePembayaran ?: "Lainnya"
            paymentMap[methodKey] = (paymentMap[methodKey] ?: 0) + t.totalHarga
            
            t.items?.forEach { item ->
                val productKey = item.namaProduk ?: "Unknown"
                productMap[productKey] = (productMap[productKey] ?: 0) + item.jumlah
                val hBeli = item.hargaBeli ?: 0
                laba += (item.harga - hBeli) * item.jumlah
            }
        }

        this.currentOmzet = omzet
        this.currentLaba = laba
        this.currentProductMap = productMap
        this.currentPaymentMap = paymentMap

        binding.tvTotalOmzet.text = "Rp ${formatNumber(omzet)}"
        binding.tvTotalLaba.text = "Rp ${formatNumber(laba)}"
        
        renderTopSelling(productMap)
        renderPaymentSummary(paymentMap)

        // Setup/Update list transaksi di bagian bawah
        if (!::adapter.isInitialized) {
            adapter = LaporanAdapter(arrayListOf())
            binding.rvTransactions.layoutManager = LinearLayoutManager(this)
            binding.rvTransactions.adapter = adapter
        }

        // Konversi data lokal ke ModelTransaksi yang diharapkan Adapter
        val adapterData = filteredTransactions.map { t ->
            ModelTransaksi().apply {
                idTransaksi = t.idTransaksi
                tanggal = t.tanggal
                totalHarga = t.totalHarga
                jumlah = t.items?.sumOf { it.jumlah } ?: 0
                namaProduk = t.items?.joinToString(", ") { it.namaProduk ?: "" }
                metodePembayaran = t.metodePembayaran
            }
        }
        
        adapter.updateData(adapterData)
    }

    private fun renderTopSelling(map: Map<String, Int>) {
        binding.containerTopSelling.removeAllViews()
        map.toList().sortedByDescending { it.second }.take(5).forEach {
            val view = layoutInflater.inflate(R.layout.item_simple_row, binding.containerTopSelling, false)
            view.findViewById<TextView>(R.id.tvLabel).text = it.first
            view.findViewById<TextView>(R.id.tvValue).text = "${it.second} terjual"
            binding.containerTopSelling.addView(view)
        }
    }

    private fun renderPaymentSummary(map: Map<String, Int>) {
        binding.containerPaymentSummary.removeAllViews()
        map.forEach { (method, total) ->
            val view = layoutInflater.inflate(R.layout.item_simple_row, binding.containerPaymentSummary, false)
            view.findViewById<TextView>(R.id.tvLabel).text = method
            view.findViewById<TextView>(R.id.tvValue).text = "Rp ${formatNumber(total)}"
            binding.containerPaymentSummary.addView(view)
        }
    }

    private fun printLaporanPDF() {
        if (filteredTransactions.isEmpty()) {
            Toast.makeText(this, "Tidak ada data untuk dicetak", Toast.LENGTH_SHORT).show()
            return
        }

        val reportTransactions = filteredTransactions.map {
            ReportTransaction(
                id = it.idTransaksi ?: "-",
                date = it.tanggal ?: "-",
                total = it.totalHarga,
                method = it.metodePembayaran ?: "Cash",
                items = it.items?.joinToString(", ") { item -> item.namaProduk ?: "" } ?: ""
            )
        }

        val report = ReportSummary(
            title = "Laporan Ringkasan Penjualan",
            period = currentFilterLabel,
            omzet = currentOmzet,
            laba = currentLaba,
            topSelling = currentProductMap.toList().sortedByDescending { it.second }.take(5),
            paymentSummary = currentPaymentMap.toList(),
            transactions = reportTransactions
        )
        ReceiptPdfPrinter(this).printReport(report)
    }

    private fun showDatePicker() {
        val picker = MaterialDatePicker.Builder.dateRangePicker().build()
        picker.addOnPositiveButtonClickListener { range ->
            processFilter(range.first, range.second)
        }
        picker.show(supportFragmentManager, "range")
    }

    private fun parseDate(dateStr: String?): Long? {
        return try {
            SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault()).parse(dateStr ?: "")?.time
        } catch (e: Exception) { null }
    }

    private fun getStartOfDay(): Long {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }.timeInMillis
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private fun formatNumber(amount: Int): String {
        return "%,d".format(amount).replace(",", ".")
    }

    private fun applyCurrentFilter() {
        val id = binding.chipGroupFilter.checkedChipId
        if (id == View.NO_ID) filterByDateRange(0) 
        else when(id) {
            R.id.chipToday -> filterByDateRange(0)
            R.id.chipWeekly -> filterByDateRange(7)
            R.id.chipMonthly -> filterByDateRange(30)
        }
    }
}