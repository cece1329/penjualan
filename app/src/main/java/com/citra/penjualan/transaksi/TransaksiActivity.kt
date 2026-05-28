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
import androidx.core.content.ContextCompat
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
import android.widget.ImageView
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
    private val cartMap = HashMap<String, Pair<ModelProduk, Int>>() 
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

    private data class PickerOption(
        val title: String,
        val info1: String, 
        val info2: String, 
        val info3: String, 
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
        dbProduk.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(prodSnapshot: DataSnapshot) {
                produkList.clear()
                for (data in prodSnapshot.children) {
                    val produk = data.getValue(ModelProduk::class.java)
                    if (produk != null) {
                        produk.idProduk = data.key
                        produkList.add(produk)
                    }
                }

                dbKategori.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(katSnapshot: DataSnapshot) {
                        binding.chipGroupKategori.removeAllViews()
                        val counts = produkList.groupingBy { it.namaKategori }.eachCount()
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
        chip.tag = categoryName
        chip.isCheckable = true
        chip.isChecked = categoryName == selectedCategory
        chip.chipCornerRadius = dp(16).toFloat()
        chip.chipMinHeight = dp(40).toFloat()
        chip.chipStrokeWidth = 1.5f
        
        applyChipColors(chip, categoryName == selectedCategory)

        chip.setOnClickListener {
            selectedCategory = categoryName
            filterProduk()
            refreshCategoryChips()
        }
        binding.chipGroupKategori.addView(chip)
    }

    private fun applyChipColors(chip: Chip, isSelected: Boolean) {
        if (isSelected) {
            chip.setTextColor(ContextCompat.getColor(this, R.color.chipTextSelected))
            chip.chipStrokeColor = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.chipStrokeSelected))
            chip.chipBackgroundColor = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.chipBgSelected))
        } else {
            chip.setTextColor(ContextCompat.getColor(this, R.color.chipText))
            chip.chipStrokeColor = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.chipStroke))
            chip.chipBackgroundColor = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.chipBg))
        }
    }

    private fun refreshCategoryChips() {
        for (i in 0 until binding.chipGroupKategori.childCount) {
            val chip = binding.chipGroupKategori.getChildAt(i) as? Chip ?: continue
            val isSelected = chip.tag.toString() == selectedCategory
            chip.isChecked = isSelected
            applyChipColors(chip, isSelected)
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
                        searchableText = "tambah baru add new customer", action = { startActivity(Intent(this@TransaksiActivity, TambahPelangganActivity::class.java)) })
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
        val etSearch = view.findViewById<EditText>(R.id.etSearchPicker)

        tvTitle?.text = title
        tvSubtitle?.text = subtitle

        fun renderOptions(filter: String = "") {
            container?.removeAllViews()
            options.filter { 
                filter.isEmpty() || it.searchableText.contains(filter, ignoreCase = true) || it.title.contains(filter, ignoreCase = true)
            }.forEach { option ->
                val itemView = layoutInflater.inflate(R.layout.item_picker_card, container, false)
                itemView.findViewById<TextView>(R.id.tvOptionIcon)?.let { it.text = option.icon }
                itemView.findViewById<TextView>(R.id.tvOptionTitle)?.let { it.text = option.title }
                itemView.findViewById<TextView>(R.id.tvPickerJabatan)?.let { it.text = option.info1 }
                itemView.findViewById<TextView>(R.id.tvPickerInfo)?.let { it.text = option.info2 }
                itemView.findViewById<TextView>(R.id.tvPickerTelp)?.let { it.text = option.info3 }
                itemView.findViewById<ImageView>(R.id.ivPickerIconInfo)?.setImageResource(option.typeIcon)
            
                // Fix: Correct way to set click listener for dynamic view in BottomSheet
                val clickEffect = itemView.findViewById<View>(R.id.viewClickEffect)
                clickEffect?.setOnClickListener {
                    sheet.dismiss()
                    option.action()
                }
                // Fallback for root click if overlay is not found
                if (clickEffect == null) {
                    itemView.setOnClickListener {
                        sheet.dismiss()
                        option.action()
                    }
                }

                container?.addView(itemView)
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
                    if (!namaPemilik.isNullOrBlank() && KasirSessionHelper.selectedKasir == null) {
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
                            binding.tvInfoKasirTransaksi.text = "Pemilik - Semua Cabang"
                        }
                    )
                )
                options.addAll(pegawaiList.map { kasir ->
                    PickerOption(
                        title = kasir.namaPegawai ?: "Kasir",
                        info1 = kasir.jabatanPegawai ?: "Karyawan",
                        info2 = kasir.cabangPegawai ?: "Pusat",
                        info3 = kasir.teleponPegawai ?: "-", 
                        icon = (kasir.namaPegawai?.take(1)?.uppercase() ?: "K"),
                        searchableText = "${kasir.idPegawai} ${kasir.namaPegawai} ${kasir.jabatanPegawai} ${kasir.cabangPegawai}"
                    ) {
                        selectedKasir = kasir
                        idKasir = kasir.idPegawai
                        namaKasir = kasir.namaPegawai ?: "Kasir"
                        jabatanKasir = kasir.jabatanPegawai
                        cabangKasir = kasir.cabangPegawai
                        binding.tvKasirTransaksi.text = "Kasir: $namaKasir"
                        binding.tvInfoKasirTransaksi.text = "${jabatanKasir} - Cabang: ${cabangKasir ?: "Pusat"}"
                    }
                })

                showPickerSheet(
                    title = "Pilih Kasir",
                    subtitle = if (pegawaiList.isEmpty()) "Belum ada pegawai berjabatan Kasir" else "Pilih kasir yang bertugas",
                    options = options
                )
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun isJabatanKasir(jabatan: String?): Boolean {
        return jabatan?.contains("kasir", ignoreCase = true) == true
    }

    private fun formatNumber(amount: Int): String {
        return String.format("%,d", amount).replace(",", ".")
    }

    private fun showMetodePembayaranSheet() {
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
            tvCashSectionLabel?.visibility = View.GONE
            etUangDiterima?.visibility = View.GONE
            tvPreviewKembalian?.visibility = View.GONE
            tvGopaySectionLabel?.visibility = View.GONE
            etNoGopay?.visibility = View.GONE
            tvPreviewNota?.visibility = View.GONE
            tvPreviewNota?.let { it.text = "" }
            btnPrint?.visibility = View.GONE
        }

        btnCash?.setOnClickListener {
            metodePembayaran = "Cash"
            resetAllInput()
            tvCashSectionLabel?.visibility = View.VISIBLE
            etUangDiterima?.visibility = View.VISIBLE
            etUangDiterima?.requestFocus()
        }

        btnQris?.setOnClickListener {
            metodePembayaran = "QRIS"
            resetAllInput()
            tvPreviewNota?.let { it.text = "Nota siap dicetak (QRIS)" }
            tvPreviewNota?.visibility = View.VISIBLE
            btnPrint?.visibility = View.VISIBLE
        }

        btnGopay?.setOnClickListener {
            metodePembayaran = "GoPay"
            resetAllInput()
            tvGopaySectionLabel?.visibility = View.VISIBLE
            etNoGopay?.visibility = View.VISIBLE
            etNoGopay?.requestFocus()
        }

        fun updatePreview() {
            if (metodePembayaran == "Cash") {
                val input = etUangDiterima?.text.toString().toIntOrNull() ?: 0
                if (input >= totalHarga) {
                    uangDiterima = input
                    kembalian = input - totalHarga
                    tvPreviewKembalian?.let { it.text = "Kembalian: Rp ${formatNumber(kembalian!!)}" }
                    tvPreviewKembalian?.visibility = View.VISIBLE
                    tvPreviewNota?.let { it.text = "Nota siap dicetak (Cash)" }
                    tvPreviewNota?.visibility = View.VISIBLE
                    btnPrint?.visibility = View.VISIBLE
                } else {
                    tvPreviewKembalian?.visibility = View.GONE
                    tvPreviewNota?.visibility = View.GONE
                    btnPrint?.visibility = View.GONE
                }
            } else if (metodePembayaran == "GoPay") {
                val input = etNoGopay?.text.toString()
                if (input.length >= 10) {
                    noGopay = input
                    tvPreviewNota?.let { it.text = "Nota siap (GoPay: $noGopay)" }
                    tvPreviewNota?.visibility = View.VISIBLE
                    btnPrint?.visibility = View.VISIBLE
                } else {
                    tvPreviewNota?.visibility = View.GONE
                    btnPrint?.visibility = View.GONE
                }
            }
        }

        etUangDiterima?.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { updatePreview() }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        etNoGopay?.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { updatePreview() }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        btnKonfirmasi?.setOnClickListener {
            if (metodePembayaran == null) {
                Toast.makeText(this, "Pilih metode pembayaran", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            sheet.dismiss()
            saveTransaction(metodePembayaran!!, uangDiterima, kembalian, noGopay)
        }
        
        btnPrint?.setOnClickListener {
            Toast.makeText(this, "Silahkan Konfirmasi Pembayaran terlebih dahulu", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveTransaction(metode: String, received: Int?, change: Int?, gopay: String?) {
        val id = dbTransaksi.push().key ?: return
        val tanggal = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault()).format(Date())
        
        val items = cartMap.values.map { 
            ReceiptItem(it.first.namaProduk ?: "-", it.second, it.first.hargaProduk)
        }

        val receipt = ReceiptData(
            toko = namaToko,
            alamat = alamatToko,
            cabang = cabangKasir,
            kasir = namaKasir,
            tanggal = tanggal,
            idTransaksi = id,
            items = items,
            jumlah = cartMap.values.sumOf { it.second },
            totalHarga = totalHarga,
            metodePembayaran = metode,
            uangDiterima = received,
            kembalian = change,
            noGopay = gopay,
            namaPelanggan = selectedPelanggan?.namaPelanggan ?: "Pelanggan Umum",
            jenisPelanggan = selectedPelanggan?.jenisPelanggan ?: "Umum"
        )

        dbTransaksi.child(id).setValue(receipt).addOnSuccessListener {
            cartMap.forEach { (prodId, pair) ->
                dbProduk.child(prodId).child("stokProduk").setValue(pair.first.stokProduk - pair.second)
            }
            cartMap.clear()
            updateCartUI()
            val intent = Intent(this, NotaActivity::class.java).apply {
                putExtra("EXTRA_RECEIPT", receipt)
            }
            startActivity(intent)
            finish()
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
            tvHargaProduk.text = "Rp ${String.format("%,d", produk.hargaProduk).replace(",", ".")}"
            tvInfoProduk.text = "${produk.namaKategori} - Stok ${produk.stokProduk}"
            tvQty.text = qty.toString()
            val active = produk.statusProduk.equals("Aktif", ignoreCase = true) || produk.statusProduk.isBlank()
            tvStatus.text = if (active) "Aktif" else "Nonaktif"
            tvStatus.setTextColor(Color.parseColor(if (active) "#2E7D32" else "#C62828"))
            
            // Ensure buttons maintain correct visibility and responsiveness
            btnPlus.isEnabled = active
            btnPlus.alpha = if (active) 1f else 0.35f
            btnMinus.isEnabled = true
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
}

private object KasirSessionHelper {
    var selectedKasir: ModelPegawai? = null
}
