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
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class DataKategori : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var editSearch: EditText
    private lateinit var iconTambah: ImageView
    private lateinit var adapter: KategoriAdapter

    private val db = FirebaseDatabase.getInstance().getReference("kategori")
    private var kategoriList = mutableListOf<Kategori>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_data_kategori)

        recyclerView = findViewById(R.id.recyclerKategori)
        editSearch = findViewById(R.id.editSearch)
        iconTambah = findViewById(R.id.iconTambah)

        // Setup Adapter
        adapter = KategoriAdapter(kategoriList) { kategori ->
            // Menuju halaman edit/detail (SUDAH DIGANTI KE ModKategori)
            val intent = Intent(this, ModKategori::class.java)
            intent.putExtra("id", kategori.id)
            intent.putExtra("nama", kategori.nama)
            intent.putExtra("status", kategori.status)
            startActivity(intent)
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // Klik tombol tambah (SUDAH DIGANTI KE ModKategori)
        iconTambah.setOnClickListener {
            startActivity(Intent(this, ModKategori::class.java))
        }

        // Muat data
        loadData()

        // Fungsi pencarian
        editSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filter(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun loadData() {
        db.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                kategoriList.clear()
                for (data in snapshot.children) {
                    val kategori = data.getValue(Kategori::class.java)
                    if (kategori != null) {
                        kategoriList.add(kategori)
                    }
                }
                adapter.updateData(kategoriList)
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun filter(text: String) {
        val filtered = kategoriList.filter {
            it.nama.lowercase().contains(text.lowercase())
        }
        adapter.updateData(filtered)
    }
}