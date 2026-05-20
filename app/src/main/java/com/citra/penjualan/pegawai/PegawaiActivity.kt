package com.citra.penjualan.pegawai

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.citra.penjualan.databinding.ActivityPegawaiBinding
import com.citra.penjualan.model.ModelPegawai
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class PegawaiActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPegawaiBinding
    private lateinit var adapter: PegawaiAdapter
    private val db = FirebaseDatabase.getInstance().getReference("pegawai")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPegawaiBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = PegawaiAdapter(arrayListOf())
        binding.recyclerPegawai.apply {
            layoutManager = LinearLayoutManager(this@PegawaiActivity)
            adapter = this@PegawaiActivity.adapter
        }

        binding.fabTambahPegawai.setOnClickListener {
            startActivity(Intent(this, TambahPegawaiActivity::class.java))
        }

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
                adapter.updateData(list)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@PegawaiActivity, "Gagal memuat pegawai: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
