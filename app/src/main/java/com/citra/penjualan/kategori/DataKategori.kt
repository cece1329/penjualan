package com.citra.penjualan.kategori

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.citra.penjualan.R
import com.google.firebase.firestore.FirebaseFirestore

class DataKategori : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var editSearch: EditText
    private lateinit var iconTambah: ImageView
    private lateinit var adapter: KategoriAdapter
    private val db = FirebaseFirestore.getInstance()
    private var kategoriList = mutableListOf<Kategori>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_data_kategori)

        recyclerView = findViewById(R.id.recyclerKategori)
        editSearch = findViewById(R.id.editSearch)
        iconTambah = findViewById(R.id.iconTambah)

        adapter = KategoriAdapter(kategoriList) { kategori ->
            val intent = Intent(this, TambahKategoriActivity::class.java)
            intent.putExtra("id", kategori.id)
            intent.putExtra("nama", kategori.nama)
            intent.putExtra("status", kategori.status)
            startActivity(intent)
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        iconTambah.setOnClickListener {
            startActivity(Intent(this, TambahKategoriActivity::class.java))
        }

        loadData()

        editSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filter(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun loadData() {
        db.collection("kategori")
            .get()
            .addOnSuccessListener { result ->
                kategoriList.clear()
                for (doc in result) {
                    val kategori = Kategori(
                        id = doc.id,
                        nama = doc.getString("nama") ?: "",
                        status = doc.getString("status") ?: "Aktif"
                    )
                    kategoriList.add(kategori)
                }
                adapter.updateData(kategoriList)
            }
    }

    private fun filter(text: String) {
        val filtered = kategoriList.filter {
            it.nama.lowercase().contains(text.lowercase())
        }
        adapter.updateData(filtered)
    }

    override fun onResume() {
        super.onResume()
        loadData()
    }
}