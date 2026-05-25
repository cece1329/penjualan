package com.citra.penjualan.produk

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.appcompat.app.AppCompatActivity
import android.content.res.ColorStateList
import android.graphics.Color
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.citra.penjualan.databinding.ActivityDataProdukBinding
import com.citra.penjualan.model.ModelKategori
import com.citra.penjualan.model.ModelProduk
import com.citra.penjualan.viewmodel.ProdukViewModel
import com.google.android.material.chip.Chip
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class DataProdukActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDataProdukBinding
    private lateinit var adapter: ProdukAdapter
    private lateinit var viewModel: ProdukViewModel
    private val dbKategori = FirebaseDatabase.getInstance().getReference("kategori")
    private var originalProdukList: List<ModelProduk> = ArrayList()
    private var selectedCategoryFilter = "Semua"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDataProdukBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup ViewModel
        viewModel = ViewModelProvider(this)[ProdukViewModel::class.java]

        // Setup RecyclerView
        adapter = ProdukAdapter(arrayListOf())
        binding.recyclerProduk.apply {
            layoutManager = LinearLayoutManager(this@DataProdukActivity)
            adapter = this@DataProdukActivity.adapter
        }

        // Observe Data
        viewModel.listProduk.observe(this) { list ->
            originalProdukList = list ?: emptyList()
            setupCategoryChips()
        }

        viewModel.fetchProduk()


        // Search: filter produk saat user mengetik
        binding.editSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                applyFilters()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Opsional: tekan icon untuk mem-fokuskan / trigger refresh
        binding.iconSearch.setOnClickListener {
            binding.editSearch.requestFocus()
            applyFilters()
        }

        // FAB Tambah Produk
        binding.fabTambahProduk.setOnClickListener {
            startActivity(Intent(this, TambahProdukActivity::class.java))
        }
    }

    private fun setupCategoryChips() {
        // Tambahkan "Semua" secara default sebelum fetch data kategori
        binding.chipGroupKategori.removeAllViews()
        addCategoryChip("Semua")
        
        // Sinkronisasi kategori dari Firebase untuk filter produk
        dbKategori.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (data in snapshot.children) {
                    val kat = data.getValue(ModelKategori::class.java)
                    kat?.namaKategori?.let { addCategoryChip(it) }
                }
                updateProdukAdapter()
            }
            override fun onCancelled(error: DatabaseError) {}
        })
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
        adapter.filterList(query, selectedCategoryFilter, originalProdukList)
    }

    private fun updateProdukAdapter() {
        applyFilters()
    }
}