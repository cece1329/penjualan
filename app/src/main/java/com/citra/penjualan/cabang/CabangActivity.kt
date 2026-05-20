package com.citra.penjualan.cabang

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.citra.penjualan.databinding.ActivityCabangBinding
import com.citra.penjualan.model.ModelCabang
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class CabangActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCabangBinding
    private lateinit var adapter: CabangAdapter
    private val db = FirebaseDatabase.getInstance().getReference("cabang")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCabangBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = CabangAdapter(arrayListOf())
        binding.recyclerCabang.apply {
            layoutManager = LinearLayoutManager(this@CabangActivity)
            adapter = this@CabangActivity.adapter
        }

        binding.fabTambahCabang.setOnClickListener {
            startActivity(Intent(this, TambahCabangActivity::class.java))
        }

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
                adapter.updateData(list)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@CabangActivity, "Gagal memuat cabang: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
