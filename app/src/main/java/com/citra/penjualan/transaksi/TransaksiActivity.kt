package com.citra.penjualan.transaksi

import android.os.Bundle
import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.citra.penjualan.databinding.ActivityTransaksiBinding
import com.citra.penjualan.databinding.ItemTransaksiProdukBinding
import com.citra.penjualan.model.ModelProduk
import com.citra.penjualan.R
import com.citra.penjualan.model.ModelKategori
import com.citra.penjualan.model.ModelPelanggan
import com.citra.penjualan.model.ModelPegawai
import com.citra.penjualan.pelanggan.TambahPelangganActivity
import com.citra.penjualan.printer.ReceiptPdfPrinter
import com.citra.penjualan.printer.ReceiptData
import com.citra.penjualan.printer.ReceiptItem
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.android.material.bottomsheet.BottomSheetDialog
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale



class TransaksiActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTransaksiBinding
    private val dbProduk = FirebaseDatabase.getInstance().getReference("produk")
    private val dbTransaksi = FirebaseDatabase.getInstance().getReference("transaksi")
    private val dbKategori = FirebaseDatabase.getInstance().getReference("kategori")
    private val dbPelanggan = FirebaseDatabase.getInstance().getReference("pelanggan")
    private val dbPegawai = FirebaseDatabase.getInstance().getReference("pegawai")
    private val dbProfil = FirebaseDatabase.getInstance().getReference("profil")

    private val produkList = ArrayList<ModelProduk>()
    private val filteredProdukList = ArrayList<ModelProduk>()
    private val cartMap = HashMap<String, Pair<ModelProduk, Int>>() // ID Produk -> (Data, Qty)
    private lateinit var produkAdapter: TransaksiProdukAdapter
    private var totalHarga = 0
    private var selectedCategory = "Semua"
    private var selectedPelanggan: ModelPelanggan? = null
    private var selectedKasir: ModelPegawai? = null
    private var roleLogin = "pemilik"
    private var namaToko = "Citra Penjualan"
    private var alamatToko = ""
    private var namaKasir = "Admin"
    private var idKasir: String? = null
    private var jabatanKasir: String? = null
    private var cabangKasir: String? = null
    private var currentReceiptData: ReceiptData? = null

    private data class PickerOption(
        val title: String,
        val info1: String, // Jabatan / Tipe
        val info2: String, // Alamat / Cabang
        val info3: String, // Telp
        val icon: String,
        val typeIcon: Int = R.drawable.location,
        val searchableText: String = "",
        val action: () -> Unit
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTransaksiBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupKasirFromSession()
        loadStoreProfile()
        loadProdukData()

        // Samakan ukuran arrow icon dengan standar (24dp) agar tidak terlalu besar
        binding.imgArrowKasir.layoutParams.width = dp(24)
        binding.imgArrowKasir.layoutParams.height = dp(24)

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                filterSearch(s.toString())
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        binding.btnCheckout.setOnClickListener {
            showMetodePembayaranSheet()
        }

        binding.btnPilihPelanggan.setOnClickListener {
            showPilihPelangganDialog()
        }

        binding.btnPilihKasir.setOnClickListener {
            if (roleLogin == "pemilik") {
                showPilihKasirDialog()
            } else {
                Toast.makeText(this, "Kasir otomatis sesuai akun login", Toast.LENGTH_SHORT).show()
            }
        }
        
        binding.btnBack.setOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        produkAdapter = TransaksiProdukAdapter(
            filteredProdukList,
            getQty = { produk -> cartMap[produk.idProduk.orEmpty()]?.second ?: 0 },
            onMinus = { produk -> decreaseQty(produk) },
            onPlus = { produk -> increaseQty(produk) }
        )
        binding.rvProduk.layoutManager = LinearLayoutManager(this)
        binding.rvProduk.adapter = produkAdapter
    }

    private fun increaseQty(produk: ModelProduk) {
        if (!produk.statusProduk.equals("Aktif", ignoreCase = true) && produk.statusProduk.isNotBlank()) {
            Toast.makeText(this, "Produk tidak aktif", Toast.LENGTH_SHORT).show()
            return
        }

        val id = produk.idProduk.orEmpty()
        if (id.isBlank()) return

        val currentQty = cartMap[id]?.second ?: 0
        if (currentQty >= produk.stokProduk) {
            Toast.makeText(this, getString(R.string.transaksi_stock_insufficient, produk.stokProduk), Toast.LENGTH_SHORT).show()
            return
        }

        cartMap[id] = Pair(produk, currentQty + 1)
        updateCartUI()
    }

    private fun decreaseQty(produk: ModelProduk) {
        val id = produk.idProduk.orEmpty()
        val currentQty = cartMap[id]?.second ?: 0
        if (currentQty <= 0) return

        if (currentQty == 1) {
            cartMap.remove(id)
        } else {
            cartMap[id] = Pair(produk, currentQty - 1)
        }
        updateCartUI()
    }

    private fun updateCartUI() {
        totalHarga = cartMap.values.sumOf { it.first.hargaProduk * it.second }
        val totalItems = cartMap.values.sumOf { it.second }
        
        binding.tvTotalHarga.text = "Rp ${formatNumber(totalHarga)}"
        binding.tvItemTerpilih.text = "${formatNumber(totalItems)} Item Terpilih"
        binding.btnCheckout.isEnabled = cartMap.isNotEmpty()
        produkAdapter.refreshQuantities()
    }

    private fun loadProdukData() {
        // Ambil produk dulu agar bisa menghitung jumlah per kategori
        dbProduk.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(prodSnapshot: DataSnapshot) {
                produkList.clear()
                for (data in prodSnapshot.children) {
                    val produk = data.getValue(ModelProduk::class.java)
                    if (produk != null) {
                        produk.idProduk = data.key
                        produkList.add(produk)
                    }
                }

                // Setelah produk didapat, baru ambil kategori
                dbKategori.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(katSnapshot: DataSnapshot) {
                        binding.chipGroupKategori.removeAllViews()
                        
                        // Hitung jumlah produk per kategori
                        val counts = produkList.groupingBy { it.namaKategori }.eachCount()
                        
                        // Tambah chip "Semua"
                        addCategoryChip("Semua", produkList.size)
                        
                        for (data in katSnapshot.children) {
                            val kat = data.getValue(ModelKategori::class.java)
                            val name = kat?.namaKategori ?: continue
                            addCategoryChip(name, counts[name] ?: 0)
                        }
                        filterProdukByCategory(selectedCategory)
                    }
                    override fun onCancelled(error: DatabaseError) {}
                })
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun addCategoryChip(categoryName: String, count: Int) {
        val chip = Chip(this)
        chip.text = "$categoryName ($count)"
        chip.tag = categoryName // Gunakan tag untuk menyimpan nama asli kategori
        chip.isCheckable = true
        chip.isChecked = categoryName == selectedCategory
        chip.chipCornerRadius = dp(16).toFloat()
        chip.chipMinHeight = dp(40).toFloat()
        chip.chipStrokeWidth = 1.5f
        
        val isSelected = categoryName == selectedCategory
        chip.setTextColor(Color.parseColor(if (isSelected) "#4A2B66" else "#8E74A6"))
        chip.chipStrokeColor = ColorStateList.valueOf(Color.parseColor(if (isSelected) "#4A2B66" else "#F3E5F5"))
        chip.chipBackgroundColor = ColorStateList.valueOf(Color.parseColor(if (isSelected) "#EAD9F7" else "#FBF8FF"))

        chip.setOnClickListener {
            selectedCategory = categoryName
            filterProduk()
            refreshCategoryChips()
        }
        binding.chipGroupKategori.addView(chip)
    }

    private fun refreshCategoryChips() {
        for (i in 0 until binding.chipGroupKategori.childCount) {
            val chip = binding.chipGroupKategori.getChildAt(i) as? Chip ?: continue
            val isSelected = chip.tag.toString() == selectedCategory
            chip.isChecked = isSelected
            
            chip.setTextColor(Color.parseColor(if (isSelected) "#4A2B66" else "#8E74A6"))
            chip.chipStrokeColor = ColorStateList.valueOf(Color.parseColor(if (isSelected) "#4A2B66" else "#F3E5F5"))
            chip.chipBackgroundColor = ColorStateList.valueOf(Color.parseColor(if (isSelected) "#EAD9F7" else "#FBF8FF"))
        }
    }

    private fun filterProdukByCategory(category: String) {
        selectedCategory = category
        filterProduk()
        refreshCategoryChips()
    }

    private fun filterProduk() {
        val query = binding.etSearch.text?.toString()?.trim().orEmpty()
        filteredProdukList.clear()
        val filtered = produkList.filter { produk ->
            val matchesCategory = selectedCategory == "Semua" || produk.namaKategori.equals(selectedCategory, ignoreCase = true)
            val matchesSearch = query.isBlank() ||
                produk.namaProduk.contains(query, ignoreCase = true) ||
                produk.namaKategori.contains(query, ignoreCase = true) ||
                produk.cabangProduk.contains(query, ignoreCase = true)
            matchesCategory && matchesSearch
        }
        filteredProdukList.addAll(filtered)
        produkAdapter.updateData(filteredProdukList)
        binding.tvEmpty.visibility = if (filteredProdukList.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun filterSearch(query: String) {
        filterProduk()
    }

    private fun showPilihPelangganDialog() {
        dbPelanggan.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val pelangganList = ArrayList<ModelPelanggan>()
                for (data in snapshot.children) {
                    val pelanggan = data.getValue(ModelPelanggan::class.java)
                    if (pelanggan != null) {
                        pelanggan.idPelanggan = data.key
                        pelangganList.add(pelanggan)
                    }
                }

                val options = mutableListOf(
                    PickerOption(
                        title = "Pelanggan Umum",
                        info1 = "Umum",
                        info2 = "Pusat",
                        info3 = "-",
                        icon = "U",
                        searchableText = "umum general default", action = { setSelectedPelanggan(null) }) ,
                    PickerOption(
                        title = "Tambah Pelanggan Baru",
                        info1 = "Baru",
                        info2 = "Pusat",
                        info3 = "-",
                        icon = "+",
                        searchableText = "tambah baru add new customer", action = { startActivity(android.content.Intent(this@TransaksiActivity, TambahPelangganActivity::class.java)) })
                )
                options.addAll(pelangganList.map { pelanggan ->
                    PickerOption(
                        title = (pelanggan.namaPelanggan ?: "-").uppercase(),
                        info1 = pelanggan.jenisPelanggan ?: "Umum",
                        info2 = pelanggan.alamatPelanggan ?: "-",
                        info3 = pelanggan.teleponPelanggan ?: "-",
                        typeIcon = R.drawable.telp,
                        icon = (pelanggan.namaPelanggan?.take(1)?.uppercase() ?: "P"),
                        searchableText = "${pelanggan.idPelanggan} ${pelanggan.namaPelanggan} ${pelanggan.teleponPelanggan} ${pelanggan.alamatPelanggan} ${pelanggan.jenisPelanggan}"
                    ) {
                        setSelectedPelanggan(pelanggan)
                    }
                })

                showPickerSheet(
                    title = "Pilih Pelanggan",
                    subtitle = "Opsional, boleh tetap pakai pelanggan umum",
                    options = options
                )
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@TransaksiActivity, "Gagal memuat pelanggan: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showPickerSheet(title: String, subtitle: String, options: List<PickerOption>) {
        val sheet = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_picker, null)
        val tvTitle = view.findViewById<TextView>(R.id.tvPickerTitle)
        val tvSubtitle = view.findViewById<TextView>(R.id.tvPickerSubtitle)
        val container = view.findViewById<LinearLayout>(R.id.containerPickerOptions)
        val etSearch = view.findViewById<EditText>(R.id.etSearchPicker) // Pastikan ID ini ada di XML

        tvTitle.text = title
        tvSubtitle.text = subtitle

        fun renderOptions(filter: String = "") {
            container.removeAllViews()
            options.filter { 
                filter.isEmpty() || it.searchableText.contains(filter, ignoreCase = true) || it.title.contains(filter, ignoreCase = true)
            }.forEach { option ->
            val itemView = layoutInflater.inflate(R.layout.item_picker_card, container, false)
                itemView.findViewById<TextView>(R.id.tvOptionIcon).text = option.icon
                itemView.findViewById<TextView>(R.id.tvOptionTitle).text = option.title

                itemView.findViewById<TextView>(R.id.tvPickerJabatan).text = option.info1
                itemView.findViewById<TextView>(R.id.tvPickerInfo).text = option.info2
                itemView.findViewById<TextView>(R.id.tvPickerTelp).text = option.info3

                // Set Ikon Dinamis
                itemView.findViewById<android.widget.ImageView>(R.id.ivPickerIconInfo).setImageResource(option.typeIcon)
            
            // Karena kita menggunakan MaterialCardView, kita pasang klik di root-nya atau view khusus
            itemView.findViewById<View>(R.id.viewClickEffect).setOnClickListener {
                    sheet.dismiss()
                    option.action()
                }
                container.addView(itemView)
            }
        }

        etSearch?.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { renderOptions(s.toString()) }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        renderOptions()
        sheet.setContentView(view)
        sheet.show()
    }

    private fun setSelectedPelanggan(pelanggan: ModelPelanggan?) {
        selectedPelanggan = pelanggan
        if (pelanggan == null) {
            binding.tvPelangganTransaksi.text = "Pelanggan Umum"
            binding.tvJenisPelangganTransaksi.text = "Opsional"
        } else {
            binding.tvPelangganTransaksi.text = pelanggan.namaPelanggan ?: "Pelanggan"
            val phone = pelanggan.teleponPelanggan?.takeIf { it.isNotBlank() } ?: "-"
            binding.tvJenisPelangganTransaksi.text = "${pelanggan.jenisPelanggan ?: "Umum"} - $phone"
        }
    }

    private fun setupKasirFromSession() {
        val session = getSharedPreferences("user_session", Context.MODE_PRIVATE)
        roleLogin = session.getString("user_role", "pemilik") ?: "pemilik"
        namaKasir = session.getString("user_name", "Admin") ?: "Admin"
        idKasir = session.getString("user_id_pegawai", "")?.takeIf { it.isNotBlank() }
        jabatanKasir = session.getString("user_jabatan", roleLogin) ?: roleLogin
        cabangKasir = session.getString("user_cabang", "")?.takeIf { it.isNotBlank() }

        if (roleLogin == "pemilik") {
            binding.tvKasirTransaksi.text = "Kasir: $namaKasir"
            binding.tvInfoKasirTransaksi.text = "Pemilik dapat memilih kasir"
            binding.imgArrowKasir.visibility = View.VISIBLE
        } else {
            binding.tvKasirTransaksi.text = "Kasir: $namaKasir"
            binding.tvInfoKasirTransaksi.text = "${jabatanKasir ?: "Karyawan"} - Cabang: ${cabangKasir ?: "-"}"
            binding.imgArrowKasir.visibility = View.GONE
        }
    }

    private fun loadStoreProfile() {
        dbProfil.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                namaToko = snapshot.child("namaToko").value?.toString()?.takeIf { it.isNotBlank() } ?: "Citra Penjualan"
                alamatToko = snapshot.child("alamatToko").value?.toString() ?: ""
                if (roleLogin == "pemilik") {
                    val namaPemilik = snapshot.child("namaPemilik").value?.toString()
                    if (!namaPemilik.isNullOrBlank() && selectedKasir == null) {
                        namaKasir = namaPemilik
                        binding.tvKasirTransaksi.text = "Kasir: $namaKasir"
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun showPilihKasirDialog() {
        dbPegawai.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val pegawaiList = ArrayList<ModelPegawai>()
                for (data in snapshot.children) {
                    val pegawai = data.getValue(ModelPegawai::class.java)
                    if (pegawai != null && isJabatanKasir(pegawai.jabatanPegawai)) {
                        pegawai.idPegawai = data.key
                        pegawai.cabangPegawai = firstNotBlank(
                            pegawai.cabangPegawai,
                            data.child("cabangPegawai").value?.toString(),
                            data.child("cabang").value?.toString(),
                            data.child("namaCabang").value?.toString()
                        )
                        pegawaiList.add(pegawai)
                    }
                }

                val options = mutableListOf(
                    PickerOption(
                        title = "Pemilik / Admin",
                        info1 = "Pemilik",
                        info2 = "Semua Cabang",
                        info3 = "-",
                        icon = "A",
                        searchableText = "admin pemilik owner",
                        action = { 
                            selectedKasir = null
                            idKasir = null
                            jabatanKasir = "Pemilik"
                            cabangKasir = null
                            val sessionName = getSharedPreferences("user_session", Context.MODE_PRIVATE).getString("user_name", "Admin") ?: "Admin"
                            namaKasir = sessionName
                            binding.tvKasirTransaksi.text = "Kasir: $namaKasir"
                            binding.tvInfoKasirTransaksi.text = "${jabatanKasir ?: "-"} - Cabang: ${displayCabang(cabangKasir)}"
                        }
                    )
                )
                options.addAll(pegawaiList.map { kasir ->
                    PickerOption(
                        title = kasir.namaPegawai ?: "Kasir",
                        info1 = kasir.jabatanPegawai ?: "Karyawan",
                        info2 = displayCabang(kasir.cabangPegawai),
                        info3 = kasir.teleponPegawai ?: "-",
                        icon = (kasir.namaPegawai?.take(1)?.uppercase() ?: "K"),
                        searchableText = "${kasir.idPegawai} ${kasir.namaPegawai} ${kasir.jabatanPegawai} ${kasir.cabangPegawai} ${kasir.teleponPegawai}"
                    ) {
                        selectedKasir = kasir
                        idKasir = kasir.idPegawai
                        namaKasir = kasir.namaPegawai ?: "Kasir"
                        jabatanKasir = kasir.jabatanPegawai
                        cabangKasir = kasir.cabangPegawai
                        binding.tvKasirTransaksi.text = "Kasir: $namaKasir"
                        binding.tvInfoKasirTransaksi.text = "${jabatanKasir ?: "-"} - Cabang: ${displayCabang(cabangKasir)}"
                    }
                })

                showPickerSheet(
                    title = "Pilih Kasir",
                    subtitle = if (pegawaiList.isEmpty()) "Belum ada pegawai dengan jabatan Kasir" else "Hanya pegawai berjabatan Kasir yang ditampilkan",
                    options = options
                )
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@TransaksiActivity, "Gagal memuat kasir: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun isJabatanKasir(jabatan: String?): Boolean {
        return jabatan?.contains("kasir", ignoreCase = true) == true
    }

    private fun displayCabang(cabang: String?): String {
        return cabang?.takeIf { it.isNotBlank() } ?: "Belum dipilih"
    }

    private fun firstNotBlank(vararg values: String?): String {
        return values.firstOrNull { !it.isNullOrBlank() }.orEmpty()
    }

    private fun formatNumber(amount: Int): String {
        val s = amount.toString()
        val sb = StringBuilder()
        var count = 0
        for (i in s.length - 1 downTo 0) {
            if (count > 0 && count % 3 == 0) sb.insert(0, ".")
            sb.insert(0, s[i])
            count++
        }
        return sb.toString()
    }

    private fun showMetodePembayaranSheet() {
        if (cartMap.isEmpty()) {
            Toast.makeText(this, getString(R.string.transaksi_cart_empty), Toast.LENGTH_SHORT).show()
            return
        }

        val sheet = BottomSheetDialog(this)
        val sheetView = layoutInflater.inflate(R.layout.bottom_sheet_metode_pembayaran, null)
        sheet.setContentView(sheetView)
        sheet.show()

        val btnCash = sheetView.findViewById<MaterialButton>(R.id.btnCash)
        val btnQris = sheetView.findViewById<MaterialButton>(R.id.btnQris)
        val btnGopay = sheetView.findViewById<MaterialButton>(R.id.btnGopay)

        val tvCashSectionLabel = sheetView.findViewById<TextView>(R.id.tvCashSectionLabel)
        val etUangDiterima = sheetView.findViewById<EditText>(R.id.etUangDiterima)
        val tvPreviewKembalian = sheetView.findViewById<TextView>(R.id.tvPreviewKembalian)
        val tvGopaySectionLabel = sheetView.findViewById<TextView>(R.id.tvGopaySectionLabel)
        val etNoGopay = sheetView.findViewById<EditText>(R.id.etNoGopay)
        val tvPreviewNota = sheetView.findViewById<TextView>(R.id.tvPreviewNota)
        val btnKonfirmasi = sheetView.findViewById<MaterialButton>(R.id.btnKonfirmasiBayar)
        val btnPrint = sheetView.findViewById<MaterialButton>(R.id.btnPrint)

        var metodePembayaran: String? = null
        var uangDiterima: Int? = null
        var kembalian: Int? = null
        var noGopay: String? = null

        fun resetAllInput() {
            tvCashSectionLabel.visibility = View.GONE
            etUangDiterima.visibility = View.GONE
            tvPreviewKembalian.visibility = View.GONE
            tvGopaySectionLabel.visibility = View.GONE
            etNoGopay.visibility = View.GONE
            tvPreviewNota.visibility = View.GONE
            tvPreviewNota.text = ""

            btnPrint.visibility = View.GONE
            etUangDiterima.text?.clear()
            etNoGopay.text?.clear()
        }

        fun setCashMode(enabled: Boolean) {
            resetAllInput()
            tvCashSectionLabel.visibility = if (enabled) View.VISIBLE else View.GONE
            etUangDiterima.visibility = if (enabled) View.VISIBLE else View.GONE
            if (enabled) etUangDiterima.requestFocus() else etUangDiterima.text?.clear()
        }

        fun setGopayMode(enabled: Boolean) {
            resetAllInput()
            tvGopaySectionLabel.visibility = if (enabled) View.VISIBLE else View.GONE
            etNoGopay.visibility = if (enabled) View.VISIBLE else View.GONE
            if (enabled) etNoGopay.requestFocus() else etNoGopay.text?.clear()
        }

        btnCash.setOnClickListener {
            metodePembayaran = "Cash"
            noGopay = null
            uangDiterima = null
            kembalian = null
            setCashMode(true)
        }

        btnQris.setOnClickListener {
            metodePembayaran = "QRIS"
            noGopay = null
            uangDiterima = null
            kembalian = null
            resetAllInput()
            tvPreviewNota.text = "Nota siap dicetak ($metodePembayaran)"
            tvPreviewNota.visibility = View.VISIBLE
            btnPrint.visibility = View.VISIBLE
        }

        btnGopay.setOnClickListener {
            metodePembayaran = "GoPay"
            noGopay = null
            uangDiterima = null
            kembalian = null
            setGopayMode(true)
        }

        fun updatePreviewNota() {
            if (metodePembayaran.isNullOrBlank()) return

            when (metodePembayaran) {
                "Cash" -> {
                    // Nota preview bisa muncul setelah input cash valid
                    val uangStr = etUangDiterima.text?.toString()?.trim().orEmpty()
                    val uang = uangStr.toIntOrNull() ?: 0
                    if (uang <= 0 || uang < totalHarga) {
                        tvPreviewKembalian.visibility = View.GONE
                        tvPreviewNota.visibility = View.GONE
                        btnPrint.visibility = View.GONE
                        return
                    }
                    val change = uang - totalHarga
                    uangDiterima = uang
                    kembalian = change
                    tvPreviewKembalian.text = "Kembalian: Rp $change"
                    tvPreviewKembalian.visibility = View.VISIBLE
                    tvPreviewNota.text = "Nota siap dicetak (Cash)"
                    tvPreviewNota.visibility = View.VISIBLE
                    btnPrint.visibility = View.VISIBLE
                }

                "GoPay" -> {
                    val noStr = etNoGopay.text?.toString()?.trim().orEmpty()
                    if (noStr.length < 10 || !noStr.all { it.isDigit() }) {
                        tvPreviewNota.visibility = View.GONE
                        btnPrint.visibility = View.GONE
                        return
                    }
                    noGopay = noStr
                    tvPreviewNota.text = "Nota siap dicetak (Gopay: $noGopay)"
                    tvPreviewNota.visibility = View.VISIBLE
                    btnPrint.visibility = View.VISIBLE
                }

                else -> {
                    // QRIS: langsung bisa print setelah pilih
                    tvPreviewNota.text = "Nota siap dicetak ($metodePembayaran)"
                    tvPreviewNota.visibility = View.VISIBLE
                    btnPrint.visibility = View.VISIBLE
                }
            }
        }

        // Listener input
        val commonWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { updatePreviewNota() }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }
        etUangDiterima.addTextChangedListener(commonWatcher)
        etNoGopay.addTextChangedListener(commonWatcher)
        btnPrint.setOnClickListener {
            Toast.makeText(this, "Konfirmasi pembayaran dulu untuk membuat nota", Toast.LENGTH_SHORT).show()
        }

        // Inisialisasi awal
        resetAllInput()

        btnKonfirmasi.setOnClickListener {
            if (metodePembayaran.isNullOrBlank()) {
                Toast.makeText(this, "Pilih metode pembayaran", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (metodePembayaran == "Cash") {
                val uangStr = etUangDiterima.text?.toString()?.trim().orEmpty()
                val uang = uangStr.toIntOrNull() ?: 0
                if (uang <= 0) {
                    Toast.makeText(this, "Masukkan uang diterima", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (uang < totalHarga) {
                    Toast.makeText(this, "Uang diterima kurang", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                uangDiterima = uang
                kembalian = uang - totalHarga
            }

            if (metodePembayaran == "GoPay") {
                val noStr = etNoGopay.text?.toString()?.trim().orEmpty()
                if (noStr.length < 10 || !noStr.all { it.isDigit() }) {
                    Toast.makeText(this, "Masukkan No GoPay minimal 10 digit", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                noGopay = noStr
            }

            // Proses simpan, lalu tampilkan pilihan cetak nota.
            sheet.dismiss()
            saveTransaction(
                metodePembayaran = metodePembayaran!!,
                uangDiterima = uangDiterima,
                kembalian = kembalian,
                noGopay = noGopay
            )
        }
    }

    private fun saveTransaction(
        metodePembayaran: String,
        uangDiterima: Int?,
        kembalian: Int?,
        noGopay: String?
    ) {
        val id = dbTransaksi.push().key
        val tanggal = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault()).format(Date())

        val listItems = cartMap.values.map {
            hashMapOf(
                "idProduk" to it.first.idProduk,
                "namaProduk" to it.first.namaProduk,
                "harga" to it.first.hargaProduk,
                "jumlah" to it.second,
                "subtotal" to (it.first.hargaProduk * it.second)
            )
        }
        
        // Detail produk untuk Nota (menggabungkan semua item agar lebih detail)
        val detailProdukNota = cartMap.values.joinToString("\n") { 
            "${it.first.namaProduk} (${it.second}x)" 
        }
        val totalItems = cartMap.values.sumOf { it.second }

        val data = hashMapOf(
            "idTransaksi" to id,
            "namaProduk" to detailProdukNota,
            "jumlah" to totalItems,
            "items" to listItems,
            "totalHarga" to totalHarga,
            "tanggal" to tanggal,
            "metodePembayaran" to metodePembayaran,
            "uangDiterima" to uangDiterima,
            "kembalian" to kembalian,
            "noGopay" to noGopay,
            "idPelanggan" to selectedPelanggan?.idPelanggan,
            "namaPelanggan" to (selectedPelanggan?.namaPelanggan ?: "Pelanggan Umum"),
            "teleponPelanggan" to selectedPelanggan?.teleponPelanggan,
            "jenisPelanggan" to (selectedPelanggan?.jenisPelanggan ?: "Umum"),
            "idKasir" to idKasir,
            "namaKasir" to namaKasir,
            "jabatanKasir" to jabatanKasir,
            "cabangKasir" to cabangKasir,
            "namaToko" to namaToko,
            "alamatToko" to alamatToko
        )


        if (id != null) {
            dbTransaksi.child(id).setValue(data).addOnSuccessListener {
                // Update Stok untuk semua barang di keranjang
                cartMap.forEach { (idProd, pair) ->
                    val newStok = pair.first.stokProduk - pair.second
                    dbProduk.child(idProd).child("stokProduk").setValue(newStok)
                }

                Toast.makeText(this, "Transaksi Berhasil Disimpan", Toast.LENGTH_SHORT).show()

                val receipt = ReceiptData(
                    toko = namaToko,
                    alamat = alamatToko,
                    cabang = cabangKasir,
                    kasir = namaKasir,
                    tanggal = tanggal,
                    idTransaksi = id ?: "",
                    items = cartMap.values.map { 
                        ReceiptItem(it.first.namaProduk, it.second, it.first.hargaProduk)
                    },
                    jumlah = totalItems,
                    totalHarga = totalHarga,
                    metodePembayaran = metodePembayaran,
                    uangDiterima = uangDiterima,
                    kembalian = kembalian,
                    noGopay = noGopay,
                    namaPelanggan = selectedPelanggan?.namaPelanggan ?: "Pelanggan Umum",
                    jenisPelanggan = selectedPelanggan?.jenisPelanggan ?: "Umum"
                )
                cartMap.clear()
                updateCartUI()
                currentReceiptData = receipt
                showReceiptPrintOptions(receipt)
            }.addOnFailureListener {
                Toast.makeText(this, getString(R.string.transaksi_failed, it.message), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showReceiptPrintOptions(receipt: ReceiptData) {
        // Langsung pindah ke NotaActivity (Full Layout)
        val intent = Intent(this, NotaActivity::class.java).apply {
            putExtra("EXTRA_RECEIPT", receipt)
        }
        startActivity(intent)
        finish() // Menutup halaman transaksi agar tidak bisa 'back' ke keranjang yang sudah kosong
    }
    private fun printReceipt(receipt: ReceiptData) {
        try {
            ReceiptPdfPrinter(this).printToPdf(receipt)
        } catch (e: Exception) {
            Toast.makeText(this, "Gagal membuka cetak nota: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

}

private class TransaksiProdukAdapter(
    private var list: List<ModelProduk>,
    private val getQty: (ModelProduk) -> Int,
    private val onMinus: (ModelProduk) -> Unit,
    private val onPlus: (ModelProduk) -> Unit
) : RecyclerView.Adapter<TransaksiProdukAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemTransaksiProdukBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemTransaksiProdukBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val produk = list[position]
        val qty = getQty(produk)

        with(holder.binding) {
            imgProduk.setImageResource(R.drawable.product)
            tvNamaProduk.text = produk.namaProduk
            tvHargaProduk.text = "Rp ${formatRupiah(produk.hargaProduk)}"
            tvInfoProduk.text = "${produk.namaKategori.ifBlank { "Kategori" }} - Stok ${produk.stokProduk}"
            tvQty.text = qty.toString()
            tvStatus.text = produk.statusProduk.ifBlank { "Aktif" }

            val active = produk.statusProduk.equals("Aktif", ignoreCase = true) || produk.statusProduk.isBlank()
            tvStatus.setTextColor(Color.parseColor(if (active) "#2E7D32" else "#C62828"))
            btnPlus.isEnabled = active
            btnPlus.alpha = if (active) 1f else 0.35f
            btnMinus.alpha = if (qty > 0) 1f else 0.35f

            btnMinus.setOnClickListener { onMinus(produk) }
            btnPlus.setOnClickListener { onPlus(produk) }
            root.setOnClickListener { onPlus(produk) }
        }
    }

    override fun getItemCount(): Int = list.size

    fun updateData(newList: List<ModelProduk>) {
        list = newList
        notifyDataSetChanged()
    }

    fun refreshQuantities() {
        notifyDataSetChanged()
    }

    private fun formatRupiah(amount: Int): String {
        return "%,d".format(amount).replace(",", ".")
    }
}
