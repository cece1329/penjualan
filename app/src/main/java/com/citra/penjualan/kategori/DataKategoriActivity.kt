package com.citra.penjualan.kategori

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.citra.penjualan.databinding.ActivityDataKategoriBinding
import com.citra.penjualan.model.ModelKategori
import com.citra.penjualan.viewmodel.DataKategoriViewModel

class DataKategoriActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDataKategoriBinding
    private lateinit var adapter: KategoriAdapter
    private lateinit var viewModel: DataKategoriViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDataKategoriBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[DataKategoriViewModel::class.java]

        // PERBAIKAN: Inisialisasi adapter hanya membutuhkan parameter list data saja
        // Logika klik sudah dipindah ke dalam KategoriAdapter mirip ProdukAdapter
        adapter = KategoriAdapter(arrayListOf())

        binding.recyclerKategori.apply {
            layoutManager = LinearLayoutManager(this@DataKategoriActivity)
            adapter = this@DataKategoriActivity.adapter
        }

        viewModel.kategoriList.observe(this) { list ->
            if (list != null) {
                adapter.updateData(list)
            }
        }

        binding.fabTambahKategori.setOnClickListener {
            startActivity(Intent(this, TambahKategoriActivity::class.java))
        }

        binding.editSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.filter(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }
}