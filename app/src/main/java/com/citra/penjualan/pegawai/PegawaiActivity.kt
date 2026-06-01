package com.citra.penjualan.pegawai

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import com.citra.penjualan.BaseActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.citra.penjualan.R
import com.citra.penjualan.databinding.ActivityPegawaiBinding
import com.citra.penjualan.model.ModelPegawai
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class PegawaiActivity : BaseActivity() {

    private lateinit var binding: ActivityPegawaiBinding
    private lateinit var adapter: PegawaiAdapter
    private val db = FirebaseDatabase.getInstance().getReference("pegawai")
    private val listPegawaiFull = ArrayList<ModelPegawai>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPegawaiBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Cek hak akses: hanya Pemilik, Admin yang bisa akses
        if (!canAccessPegawai()) {
            Toast.makeText(this, "Akses ditolak: Anda tidak memiliki akses ke menu ini", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        adapter = PegawaiAdapter(arrayListOf())
        binding.recyclerPegawai.apply {
            layoutManager = LinearLayoutManager(this@PegawaiActivity)
            adapter = this@PegawaiActivity.adapter
        }

        binding.fabTambahPegawai.setOnClickListener {
            startActivity(Intent(this, TambahPegawaiActivity::class.java))
        }

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filter(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        fetchPegawai()
    }

    private fun fetchPegawai() {
        db.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = ArrayList<ModelPegawai>()
                for (data in snapshot.children) {
                    val p = data.getValue(ModelPegawai::class.java)
                    if (p != null) {
                        p.idPegawai = data.key
                        list.add(p)
                    }
                }
                listPegawaiFull.clear()
                listPegawaiFull.addAll(list)
                filter(binding.etSearch.text?.toString() ?: "")
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@PegawaiActivity, getString(R.string.pegawai_load_failed, error.message), Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun filter(query: String) {
        val q = query.trim()
        val filteredList = if (q.isEmpty()) {
            listPegawaiFull
        } else {
            listPegawaiFull.filter {
                it.namaPegawai?.contains(q, ignoreCase = true) == true ||
                    it.jabatanPegawai?.contains(q, ignoreCase = true) == true ||
                    it.teleponPegawai?.contains(q, ignoreCase = true) == true ||
                    it.cabangPegawai?.contains(q, ignoreCase = true) == true ||
                    it.passwordPegawai?.contains(q, ignoreCase = true) == true
            }
        }
        adapter.updateData(filteredList)
    }
}
