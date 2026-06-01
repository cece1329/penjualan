package com.citra.penjualan.cabang

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import com.citra.penjualan.BaseActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.citra.penjualan.R
import com.citra.penjualan.databinding.ActivityCabangBinding
import com.citra.penjualan.model.ModelCabang
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class CabangActivity : BaseActivity() {

    private lateinit var binding: ActivityCabangBinding
    private lateinit var adapter: CabangAdapter
    private val db = FirebaseDatabase.getInstance().getReference("cabang")
    
    private val listCabangFull = ArrayList<ModelCabang>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCabangBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Cek hak akses: hanya Pemilik, Admin yang bisa akses
        if (!canAccessCabang()) {
            Toast.makeText(this, "Akses ditolak: Anda tidak memiliki akses ke menu ini", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        adapter = CabangAdapter(arrayListOf())
        binding.recyclerCabang.apply {
            layoutManager = LinearLayoutManager(this@CabangActivity)
            adapter = this@CabangActivity.adapter
        }

        binding.fabTambahCabang.setOnClickListener {
            startActivity(Intent(this, TambahCabangActivity::class.java))
        }

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filter(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        fetchCabang()
    }

    private fun fetchCabang() {
        db.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = ArrayList<ModelCabang>()
                for (data in snapshot.children) {
                    val c = data.getValue(ModelCabang::class.java)
                    if (c != null) {
                        c.idCabang = data.key
                        list.add(c)
                    }
                }
                listCabangFull.clear()
                listCabangFull.addAll(list)
                filter(binding.etSearch.text.toString())
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@CabangActivity, getString(R.string.cabang_load_failed, error.message), Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun filter(query: String) {
        val filteredList = if (query.trim().isEmpty()) {
            listCabangFull
        } else {
            listCabangFull.filter {
                it.namaCabang?.contains(query, ignoreCase = true) == true ||
                it.kotaCabang?.contains(query, ignoreCase = true) == true ||
                it.alamatCabang?.contains(query, ignoreCase = true) == true
            }
        }
        adapter.updateData(filteredList)
    }
}
