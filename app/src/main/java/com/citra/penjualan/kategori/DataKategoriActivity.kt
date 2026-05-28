package com.citra.penjualan.kategori

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.citra.penjualan.databinding.ActivityDataKategoriBinding
import com.citra.penjualan.model.ModelKategori
import android.content.res.ColorStateList
import android.graphics.Color
import com.google.android.material.chip.Chip
import com.citra.penjualan.model.ModelProduk
import com.citra.penjualan.viewmodel.DataKategoriViewModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class DataKategoriActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDataKategoriBinding
    private lateinit var adapter: KategoriAdapter
    private lateinit var viewModel: DataKategoriViewModel
    private val dbProduk = FirebaseDatabase.getInstance().getReference("produk")
    private var originalKategoriList: List<ModelKategori> = emptyList()
    private var productCounts: Map<String, Int> = emptyMap()
    private var selectedCategoryFilter = "Semua"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDataKategoriBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[DataKategoriViewModel::class.java]

        // PERBAIKAN: Inisialisasi adapter hanya membutuhkan parameter list data saja
        // Logika klik sudah dipindah ke dalam KategoriAdapter mirip ProdukAdapter
        adapter = KategoriAdapter(originalKategoriList, productCounts)

        binding.recyclerKategori.apply {
            layoutManager = LinearLayoutManager(this@DataKategoriActivity)
            adapter = this@DataKategoriActivity.adapter
        }

        loadProductCounts()

        viewModel.kategoriList.observe(this) { list ->
            originalKategoriList = list ?: emptyList()
            setupCategoryChips()
        }

        binding.fabTambahKategori.setOnClickListener {
            startActivity(Intent(this, TambahKategoriActivity::class.java))
        }

        binding.editSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                applyFilters()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.iconSearch.setOnClickListener {
            binding.editSearch.requestFocus()
            applyFilters()
        }
    }

    private fun loadProductCounts() {
        dbProduk.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val counts = mutableMapOf<String, Int>()
                for (data in snapshot.children) {
                    val produk = data.getValue(ModelProduk::class.java)
                    val katName = produk?.namaKategori
                    if (katName != null) {
                        counts[katName] = (counts[katName] ?: 0) + 1
                    }
                }
                productCounts = counts
                updateKategoriAdapter()
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun setupCategoryChips() {
        binding.chipGroupKategori.removeAllViews()
        addCategoryChip("Semua")
        originalKategoriList.forEach { kat ->
            kat.namaKategori?.let { addCategoryChip(it) }
        }
        updateKategoriAdapter()
    }

    private fun addCategoryChip(name: String) {
        val chip = Chip(this)
        chip.text = name
        chip.isCheckable = true
        chip.isChecked = name == selectedCategoryFilter
        
        val isSelected = name == selectedCategoryFilter
        chip.setTextColor(Color.parseColor(if (isSelected) "#4A2B66" else "#8E74A6"))
        chip.chipStrokeColor = ColorStateList.valueOf(Color.parseColor(if (isSelected) "#4A2B66" else "#F3E5F5"))
        chip.chipBackgroundColor = ColorStateList.valueOf(Color.parseColor(if (isSelected) "#EAD9F7" else "#FBF8FF"))

        chip.setOnClickListener {
            selectedCategoryFilter = name
            applyFilters()
            refreshCategoryChips()
        }
        binding.chipGroupKategori.addView(chip)
    }

    private fun refreshCategoryChips() {
        for (i in 0 until binding.chipGroupKategori.childCount) {
            val chip = binding.chipGroupKategori.getChildAt(i) as? Chip ?: continue
            val isSelected = chip.text.toString() == selectedCategoryFilter
            chip.isChecked = isSelected
            chip.setTextColor(Color.parseColor(if (isSelected) "#4A2B66" else "#8E74A6"))
            chip.chipStrokeColor = ColorStateList.valueOf(Color.parseColor(if (isSelected) "#4A2B66" else "#F3E5F5"))
            chip.chipBackgroundColor = ColorStateList.valueOf(Color.parseColor(if (isSelected) "#EAD9F7" else "#FBF8FF"))
        }
    }

    private fun applyFilters() {
        val query = binding.editSearch.text.toString()
        val filtered = originalKategoriList.filter { 
            (selectedCategoryFilter == "Semua" || it.namaKategori == selectedCategoryFilter) &&
            (query.isEmpty() || it.namaKategori?.contains(query, ignoreCase = true) == true)
        }
        if (::adapter.isInitialized) {
            adapter.updateData(filtered, productCounts)
        }
    }

    private fun updateKategoriAdapter() {
        applyFilters()
    }
}
