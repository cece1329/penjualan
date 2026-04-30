package com.citra.penjualan.produk

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.citra.penjualan.databinding.ActivityDataProdukBinding
import com.citra.penjualan.viewmodel.ProdukViewModel

class DataProdukActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDataProdukBinding
    private lateinit var adapter: ProdukAdapter
    private lateinit var viewModel: ProdukViewModel

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
            if (list != null) {
                adapter.updateData(list)
            }
        }

        viewModel.fetchProduk()

        // FAB Tambah Produk
        binding.fabTambahProduk.setOnClickListener {
            startActivity(Intent(this, TambahProdukActivity::class.java))
        }
    }
}