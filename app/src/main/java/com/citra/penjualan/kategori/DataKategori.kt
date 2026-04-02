package com.citra.penjualan.kategori

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.viewModels // <--- Import ini jangan sampai hilang
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.citra.penjualan.R
import com.citra.penjualan.viewmodel.DataKategoriViewModel

class DataKategori : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var editSearch: EditText
    private lateinit var iconTambah: ImageView
    private lateinit var adapter: KategoriAdapter

    // Ini bakal normal (nggak merah) kalau Gradle sudah di-Sync
    private val viewModel: DataKategoriViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_data_kategori)

        recyclerView = findViewById(R.id.recyclerKategori)
        editSearch = findViewById(R.id.editSearch)
        iconTambah = findViewById(R.id.iconTambah)

        adapter = KategoriAdapter(arrayListOf()) { kategori ->
            val intent = Intent(this, ModKategori::class.java)
            intent.putExtra("id", kategori.id)
            intent.putExtra("nama", kategori.nama)
            intent.putExtra("status", kategori.status)
            startActivity(intent)
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        viewModel.kategoriList.observe(this) { list ->
            adapter.updateData(list)
        }

        iconTambah.setOnClickListener {
            startActivity(Intent(this, ModKategori::class.java))
        }

        editSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.filter(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }
}