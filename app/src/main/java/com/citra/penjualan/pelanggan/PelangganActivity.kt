package com.citra.penjualan.pelanggan

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.citra.penjualan.databinding.ActivityPelangganBinding
import com.citra.penjualan.model.ModelPelanggan
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class PelangganActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPelangganBinding
    private lateinit var adapter: PelangganAdapter
    private val db = FirebaseDatabase.getInstance().getReference("pelanggan")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPelangganBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = PelangganAdapter(arrayListOf())
        binding.recyclerPelanggan.apply {
            layoutManager = LinearLayoutManager(this@PelangganActivity)
            adapter = this@PelangganActivity.adapter
        }

        binding.fabTambahPelanggan.setOnClickListener {
            startActivity(Intent(this, TambahPelangganActivity::class.java))
        }

        fetchPelanggan()
    }

    private fun fetchPelanggan() {
        db.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = ArrayList<ModelPelanggan>()
                for (data in snapshot.children) {
                    val p = data.getValue(ModelPelanggan::class.java)
                    if (p != null) {
                        p.idPelanggan = data.key
                        list.add(p)
                    }
                }
                adapter.updateData(list)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@PelangganActivity, "Gagal memuat pelanggan: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
