package com.citra.penjualan.produk

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
// PASTIKAN IMPORT INI BENER (Sesuai nama file XML kamu)
import com.citra.penjualan.databinding.ActivityDataProdukBinding
import com.citra.penjualan.viewmodel.ProdukViewModel

class DataProdukActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDataProdukBinding
    private lateinit var adapter: ProdukAdapter
    private lateinit var viewModel: ProdukViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Inisialisasi View Binding
        binding = ActivityDataProdukBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. Setup ViewModel
        viewModel = ViewModelProvider(this)[ProdukViewModel::class.java]

        // 2. Setup RecyclerView (ID sesuai XML: recyclerProduk)
        adapter = ProdukAdapter(arrayListOf())
        binding.recyclerProduk.layoutManager = LinearLayoutManager(this)
        binding.recyclerProduk.adapter = adapter

        // 3. Ambil Data
        viewModel.listProduk.observe(this) { list ->
            if (list != null) {
                adapter.updateData(list)
            }
        }
        viewModel.fetchProduk()

        // 4. Klik Tombol Tambah (ID sesuai XML: fabTambahProduk)
        binding.fabTambahProduk.setOnClickListener {
            val intent = Intent(this, TambahProdukActivity::class.java)
            startActivity(intent)
        }

        // CATATAN: Jangan panggil binding.btnBack kalau di XML sudah dihapus!
    }
}